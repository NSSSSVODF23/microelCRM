package com.microel.trackerbackend.services.external.billing.directaccess.bases;

import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.external.billing.directaccess.DirectBaseAccess;
import com.microel.trackerbackend.services.external.billing.directaccess.DirectBaseSession;
import com.microel.trackerbackend.services.external.billing.directaccess.Request;
import com.microel.trackerbackend.services.external.billing.directaccess.Url;
import com.microel.trackerbackend.storage.entities.address.Address;
import com.microel.trackerbackend.storage.entities.team.util.Credentials;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.validation.constraints.NotBlank;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class Base781 extends DirectBaseSession implements DirectBaseAccess {

    private Base781(Credentials credentials) {
        super("http://10.50.0.17:85", credentials);
    }

    @Override
    public void login() {
        try {
            Connection.Response response = request(
                    Request.of(
                            "index.php",
                            Map.of("act", "main"),
                            Map.of("login", getCredentials().getUsername(), "passwd", getCredentials().getPassword()),
                            Connection.Method.POST
                    )
            );
            authSuccessfulCheck(response);
        } catch (HttpStatusException e) {
            throw new ResponseException("Ошибка авторизации в " + getHost() + " " + getCredentials());
        } catch (IOException e) {
            throw new ResponseException("Ошибка подключения к " + getHost());
        }
    }

    @Override
    public void logout() {
        String url = Url.create(Map.of("act", "auth"), getHost(), "index.php");
        try {
            Jsoup.connect(url).execute();
        } catch (IOException e) {
            throw new ResponseException("Ошибка при выходе " + getHost());
        }
    }

    private void authSuccessfulCheck(Connection.Response response) throws IOException {
        Document document = response.bufferUp().parse();
        if(document.body().children().isEmpty() || document.body().text().contains("adm_auth.operator_not_found")){
            throw new ResponseException("Не авторизованный запрос " + getHost() + " " + getCredentials());
        }
    }

    public static Base781 create(Credentials credentials) {
        return new Base781(credentials);
    }

    @Getter
    @Setter
    public static class CreateUserForm{

        @NonNull
        private Address address;
        @NotBlank
        private String fullName;
        @NotBlank
        private String phone;
        @NonNull
        private UserType userType;

        public String getPreparePhone(){
            String cleared = phone.replaceAll("[-() ]", "").trim();
            return cleared.substring(1, cleared.length() - 1);
        }

        public Map<String, String> toRequestBody(String login){
            Map<String, String> body = new HashMap<>();

            if(userType == UserType.ORG){
                body.put("login", "BIZ" + login);
            }else{
                body.put("login", login);
            }

            if(address.getStreet() == null)
                throw new ResponseException("Не указана улица для создания абонента");

            if(address.getStreet().getBillingAlias() == null || address.getStreet().getBillingAlias().isBlank())
                throw new ResponseException("Не указан псевдоним для улицы в биллинге");

            if(address.getHouseNamePart() == null || address.getHouseNamePart().isBlank())
                throw new ResponseException("Не указан адрес дома");

            String billingAlias = address.getStreet().getBillingAlias();

            String house = address.getHouseNamePart();

            String apartNum = "";
            if(address.getApartmentNum() != null)
                apartNum = address.getApartmentNum().toString();

            String entrance = "";
            if(address.getEntrance() != null)
                entrance = address.getEntrance().toString();

            String floor = "";
            if(address.getFloor() != null)
                floor = address.getFloor().toString();


            body.put("fio", fullName);
            body.put("phone", getPreparePhone());
            body.put("pwd", getPreparePhone());
            body.put("utype", userType.getValue());
            body.put("street", billingAlias);
            body.put("hause", house);
            body.put("kv", apartNum);
            body.put("pod", entrance);
            body.put("etj", floor);
            body.put("ndog", login);
            body.put("AddUser", "Добавить абонента");

            return body;
        }

        public static CreateUserForm of(Address address, String fullName, String phone, UserType userType){
            CreateUserForm form = new CreateUserForm();
            form.setAddress(address);
            form.setFullName(fullName);
            form.setPhone(phone);
            form.setUserType(userType);
            return form;
        }
    }

    public enum UserType {
        PHY("обычн."),
        ORG("орг.");

        private final String value;

        UserType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
