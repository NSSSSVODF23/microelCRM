package com.microel.trackerbackend.parsers.addresses;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.microel.trackerbackend.misc.SimpleMessage;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.dispatchers.CityDispatcher;
import com.microel.trackerbackend.storage.dispatchers.HouseDispatcher;
import com.microel.trackerbackend.storage.dispatchers.StreetDispatcher;
import com.microel.trackerbackend.storage.entities.address.City;
import com.microel.trackerbackend.storage.entities.address.House;
import com.microel.trackerbackend.storage.entities.address.Street;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;

@Service
@Lazy
@Getter
@Setter
@Slf4j
// Предназначен для сбора адресов с гос.услуг
public class AddressParser {
    private final StompController stompController;
    private final CityDispatcher cityDispatcher;
    private final StreetDispatcher streetDispatcher;
    private final HouseDispatcher houseDispatcher;
    private final HashMap<String, LocalityIdentifier> cityIds = new HashMap<>();
    private Boolean isRunning = false;

    public AddressParser(StompController stompController, CityDispatcher cityDispatcher, StreetDispatcher streetDispatcher, HouseDispatcher houseDispatcher) {
        this.stompController = stompController;
        this.cityDispatcher = cityDispatcher;
        this.streetDispatcher = streetDispatcher;
        this.houseDispatcher = houseDispatcher;
        cityIds.put("Волгодонск", new LocalityIdentifier(null, "1a453dcd-8885-4999-923b-1bbaa5a1cec4", null));
        cityIds.put("Романовская", new LocalityIdentifier("31b577c3-afe5-4b18-bd5c-2cf68d6c88ec", null, "007082b3-df3d-428e-93cb-7a30d5099737"));
        cityIds.put("Цимлянск", new LocalityIdentifier("4541e1c4-8151-49a7-87fd-8be4026f10b8", "27095e32-973c-4d07-929a-c46f63076ea8", null));
    }

    public void startParse() {
        if(isRunning) return;
        isRunning = true;
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                cityIds.forEach((cityName, location) -> {
                    City cityEntity = cityDispatcher.createIfAbsent(cityName);
                    stompController.sendParserMessage(new SimpleMessage(SimpleMessage.Severity.INFO, "Собираем улицы города: " + cityName));
                    List<StreetResponse> streets = getStreets(location);
                    stompController.sendParserMessage(new SimpleMessage(SimpleMessage.Severity.INFO, "Получено улиц: " + streets.size()));
                    streets.forEach(street -> {
                        stompController.sendParserMessage(new SimpleMessage(SimpleMessage.Severity.INFO, "Собираем дома улицы: " + street.getOffName()));
                        Street streetEntity = streetDispatcher.createIfAbsent(street.getOffName(), street.getShortName(), cityEntity);
                        List<HouseResponse> houses = getHouses(location, street.getAoGuid());
                        stompController.sendParserMessage(new SimpleMessage(SimpleMessage.Severity.INFO, "Получено домов: " + houses.size()));
                        houses.forEach(house -> {
                            House parsedHouse = house.toHouse();
                            if(parsedHouse == null){
                                stompController.sendParserMessage(new SimpleMessage(SimpleMessage.Severity.ERROR, "Не верный адрес дома: "+cityName+" "+street.getOffName()+" "+house.getHouseTextAddress()));
                                return;
                            }
                            House houseEntity = houseDispatcher.createIfAbsent(parsedHouse, streetEntity);
                        });
                    });
                });
            }catch (Exception e){
                stompController.sendParserMessage(new SimpleMessage(SimpleMessage.Severity.ERROR, "Сбор адресов прерван "+e.getMessage()));
            }finally {
                isRunning = false;
                stompController.sendParserMessage(new SimpleMessage(SimpleMessage.Severity.INFO, "Сбор адресов завершен"));
            }
        });
    }

    private List<StreetResponse> getStreets(LocalityIdentifier localityIdentifier) {
        List<StreetResponse> streetResult = new ArrayList<>();

        RequestQueryFactory requestQueryFactory = new RequestQueryFactory(localityIdentifier);

        Integer currentPage = 1;
        while (true) {
            URL streetsGetterUrl = requestQueryFactory.createStreetsGetterUrl(currentPage);
            if (streetsGetterUrl == null) break;
            List<StreetResponse> streetResponses = request(streetsGetterUrl, StreetResponse.class);
            if (streetResponses == null) {
                break;
            }
            streetResult.addAll(streetResponses);
            currentPage++;
        }

        return streetResult;
    }

    private List<HouseResponse> getHouses(LocalityIdentifier localityIdentifier, String streetCode) {
        List<HouseResponse> houseResult = new ArrayList<>();

        RequestQueryFactory requestQueryFactory = new RequestQueryFactory(localityIdentifier);

        Integer currentPage = 1;
        while (true) {
            URL housesGetterUrl = requestQueryFactory.createHousesGetterUrl(currentPage, streetCode);
            if (housesGetterUrl == null) break;
            List<HouseResponse> houseResponses = request(housesGetterUrl, HouseResponse.class);
            if (houseResponses == null) {
                break;
            }
            houseResult.addAll(houseResponses);
            currentPage++;
        }

        return houseResult;
    }

    @Nullable
    private <T> List<T> request(URL url, Class<T> clazz) {
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // success
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                String responseString = response.toString();
                if (responseString.equals("[]") || responseString.isBlank()) {
                    return null;
                }
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, clazz);
                return objectMapper.readValue(responseString, collectionType);
            } else {
                return null;
            }
        } catch (IOException ignored) {
            return null;
        }
    }
}
