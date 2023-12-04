package com.microel.trackerbackend.services;

import com.microel.trackerbackend.misc.ListItem;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.storage.entities.team.util.PhyPhoneInfo;
import com.microel.trackerbackend.storage.repositories.PhyPhoneInfoRepository;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static java.lang.Thread.sleep;

@Service
public class PhyPhoneService {

    private final PhyPhoneInfoRepository phyPhoneInfoRepository;

    public PhyPhoneService(PhyPhoneInfoRepository phyPhoneInfoRepository) {
        this.phyPhoneInfoRepository = phyPhoneInfoRepository;
    }

    public void callUp(PhyPhoneInfo phoneInfo, CallUpRequest callUpRequest){
        String phoneNumber = callUpRequest.getPhoneNumber();
        phoneNumber = phoneNumber.replaceAll("[^0-9]", "");
        if(phoneNumber.length() == 10){
            phoneNumber = "8" + phoneNumber;
        }
        switch (phoneInfo.getModel()){
            case OLD -> {
                String login = phoneInfo.getLogin() + ":" + phoneInfo.getPassword();
                String base64login = Base64.getEncoder().encodeToString(login.getBytes());
                Map<String, String> headersMap = Map.of(
                        "Authorization", "Basic "+base64login,
                        "Content-Type", "application/octet-stream"
                        );
                // Звонок на указанный номер телефона
                try {
                    Jsoup.connect("http://" + phoneInfo.getIp() + "/hlPhone_ActionURL")
                            .method(Connection.Method.POST)
                            .headers(headersMap)
                            .requestBody("/hlPhone_ActionURL&Command=0&admin:admin&Number="+phoneNumber+"&Account=microel.mangosip.ru")
                            .execute();
                }catch (IOException ignore){
//                    throw new ResponseException("Не удалось позвонить на телефон");
                }
            }
            case NEW -> {
                try {
                    // Получение идентификатора сессии из cookie
                    Connection.Response sessionCookie = Jsoup.connect("http://" + phoneInfo.getIp()).method(Connection.Method.GET).execute();
                    Map<String, String> cookies = sessionCookie.cookies();

                    // Обновление идентификатора сессии
                    Document authDocument = Jsoup.connect("http://" + phoneInfo.getIp() + "/key==nonce?now=" + (Instant.now().toEpochMilli()))
                            .cookies(cookies)
                            .get();
                    String nonce = authDocument.text();
                    if(!nonce.isBlank()) {
                        cookies.put("auth", nonce);
                    } else if(cookies.containsKey("auth")) {
                        nonce = cookies.get("auth");
                    } else {
                        throw new ResponseException("Не удалось получить id сессии на телефоне");
                    }

                    // Хеширование данных запроса для авторизации
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    String encoded = phoneInfo.getLogin() + ":" + HexFormat.of().formatHex(md.digest((phoneInfo.getLogin() + ":" + phoneInfo.getPassword() + ":" + nonce).getBytes(StandardCharsets.UTF_8)));

                    // Авторизация на телефоне
                    Map<String, String> authBody = Map.of(
                            "encoded", encoded,
                            "CurLanguage", "ru",
                            "ReturnPage", "/"
                    );
                    try {
                        Jsoup.connect("http://" + phoneInfo.getIp() + "/").cookies(cookies).data(authBody).post();
                    }catch (HttpStatusException e){
                        throw new ResponseException("Не верный логин или пароль для авторизации на телефоне");
                    }catch (IOException e){
                        throw new ResponseException("Не удалось авторизоваться на телефоне");
                    }

                    // Звонок на указанный номер телефона
                    Map<String, String> callRequestMap = Map.of(
                        "PHB_AutoDialNumber", phoneNumber,
                        "ReturnPage", "/webdial.htm",
                        "AutoDialSubmit", "submit",
                        "PHB_AutoDialLine", "-1"
                    );
                    try {
                        Jsoup.connect("http://" + phoneInfo.getIp() + "/webdial.htm").method(Connection.Method.POST).cookies(cookies).data(callRequestMap).execute();
                    }catch (IOException e){
                        throw new ResponseException("Не удалось позвонить на телефон");
                    }

                    // Logout на телефоне
                    Map<String, String> logoutRequestMap = Map.of("DefaultLogout", "Выход");
                    Jsoup.connect("http://" + phoneInfo.getIp() + "/title.htm").method(Connection.Method.POST).cookies(cookies).data(logoutRequestMap).execute();

                } catch (IOException e) {
                    throw new ResponseException(e.getMessage());
                } catch (NoSuchAlgorithmException e) {
                    throw new ResponseException("Ошибка кодирования данных для авторизации на телефоне");
                }
            }
        }
    }

    public PhyPhoneInfo get(Long phyPhoneInfoId) {
        return phyPhoneInfoRepository.findById(phyPhoneInfoId).orElseThrow(()->new ResponseException("Не удалось найти телефон"));
    }

    public List<ListItem> getPhyPhoneList() {
        return phyPhoneInfoRepository.findAll().stream().map(PhyPhoneInfo::toListItem).toList();
    }

    @Getter
    @Setter
    public static class CallUpRequest {
        private String phoneNumber;
    }

}
