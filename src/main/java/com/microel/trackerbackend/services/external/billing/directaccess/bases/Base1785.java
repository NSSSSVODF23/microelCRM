package com.microel.trackerbackend.services.external.billing.directaccess.bases;

import com.microel.trackerbackend.controllers.telegram.Utils;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.external.billing.directaccess.DirectBaseAccess;
import com.microel.trackerbackend.services.external.billing.directaccess.DirectBaseSession;
import com.microel.trackerbackend.services.external.billing.directaccess.Request;
import com.microel.trackerbackend.storage.entities.address.Address;
import com.microel.trackerbackend.storage.entities.team.util.Credentials;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.validation.constraints.NotBlank;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


public class Base1785 extends DirectBaseSession implements DirectBaseAccess {

    private Base1785(Credentials credentials) {
        super("http://10.50.0.17:85", credentials);
    }

    public static Base1785 create(Credentials credentials) {
        return new Base1785(credentials);
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
        try {
            request(Request.of("index.php", Map.of("act", "auth")));
        } catch (IOException e) {
            throw new ResponseException("Ошибка при выходе " + getHost());
        }
    }

    public String getFreeUserLogin() {
        try {
            Connection.Response response = request(
                    Request.of(
                            "index.php",
                            Map.of("act", "user_add")
                    )
            );
            authSuccessfulCheck(response);
            Document document = response.bufferUp().parse();
            return document.select("input[name=login]").val();
        } catch (IOException e) {
            throw new ResponseException("Ошибка при получении свободного логина пользователя в " + getHost() + " " + getCredentials());
        }
    }

    public String createLogin(CreateUserForm form) {
        String freeUserLogin = getFreeUserLogin();
        try {
            Connection.Response response = request(
                    Request.of(
                            "index.php",
                            Map.of("act", "user_add"),
                            form.toRequestBody(freeUserLogin),
                            Connection.Method.POST
                    )
            );
            authSuccessfulCheck(response);
            if (form.getUserType() == UserType.ORG)
                return "BIZ" + freeUserLogin;
            return freeUserLogin;
        } catch (IOException e) {
            throw new ResponseException("Ошибка при создании пользователя в " + getHost() + " " + getCredentials());
        }
    }

    public void selectTargetUser(@NotBlank String login) {
        try {
            Connection.Response response = request(
                    Request.of(
                            "index.php",
                            Map.of("act", "user_select"),
                            Map.of("str_user", login),
                            Connection.Method.POST
                    )
            );
            authSuccessfulCheck(response);
        } catch (IOException e) {
            throw new ResponseException("Не удалось выбрать целевого пользователя");
        }
    }

    public void unfreeze(@NotBlank String login) {
        try {
            selectTargetUser(login);
            Connection.Response response = request(
                    Request.of(
                            "index.php",
                            Map.of("act", "auth_control"),
                            Map.of("DoUnFreeze", "Выполнить"),
                            Connection.Method.POST
                    )
            );
            authSuccessfulCheck(response);
        } catch (IOException e) {
            throw new ResponseException("Не удалось разморозить пользователя");
        }
    }

    public List<UserTariff> getTariffList(@NotBlank String login) {
        selectTargetUser(login);
        try {
            Connection.Response response = request(
                    Request.of(
                            "index.php",
                            Map.of("act", "tarif"),
                            Map.of("Change", "Установить тариф"),
                            Connection.Method.POST
                    )
            );
            authSuccessfulCheck(response);
            List<UserTariff> userTariffs = parseUserTariffs(response.bufferUp().parse(), "2");
            if (userTariffs.size() == 1) return new ArrayList<>();
            if (userTariffs.isEmpty()) {
                request(
                        Request.of(
                                "index.php",
                                Map.of("act", "auth_control"),
                                Map.of("DoUnFreeze", "Выполнить"),
                                Connection.Method.POST
                        )
                );
                Connection.Response emergencyResponse = request(
                        Request.of(
                                "index.php",
                                Map.of("act", "tarif"),
                                Map.of("New_tarif", "Установить тариф"),
                                Connection.Method.POST
                        )
                );
                authSuccessfulCheck(emergencyResponse);
                userTariffs = parseUserTariffs(emergencyResponse.bufferUp().parse(), "1");
            }
            return userTariffs;
        } catch (IOException e) {
            throw new ResponseException("Не удалось выбрать целевого пользователя");
        }
    }

    public List<UserTariff> getServiceList(@NotBlank String login) {
        selectTargetUser(login);
        try {
            Connection.Response response = request(
                    Request.of(
                            "index.php",
                            Map.of("act", "tarif"),
                            Map.of("ExtTarif", "Расшир.тариф"),
                            Connection.Method.POST
                    )
            );
            authSuccessfulCheck(response);
            List<UserTariff> userTariffs = parseUserTariffs(response.bufferUp().parse(), "2");
            Collections.reverse(userTariffs);
            return userTariffs;
        } catch (IOException e) {
            throw new ResponseException("Не удалось выбрать целевого пользователя");
        }
    }

    private List<UserTariff> parseUserTariffs(Document document, String childNumber) {
        Elements tariffElements = document.select("form>table:nth-of-type(" + childNumber + ")>tbody>tr:nth-child(odd)");
        List<UserTariff> userTariffs = tariffElements.stream().skip(1L).map(UserTariff::from).collect(Collectors.toList());
        Collections.reverse(userTariffs);
        return userTariffs;
    }

    public void appendService(@NotBlank String login, @NonNull Integer id) {
        selectTargetUser(login);
        try {
            Connection.Response response = request(
                    Request.of(
                            "index.php",
                            Map.of("act", "tarif"),
                            Map.of("Add_Ext", "Установить тариф", "t_sel", id.toString()),
                            Connection.Method.POST
                    )
            );
            authSuccessfulCheck(response);
        } catch (IOException e) {
            throw new ResponseException("Не удалось добавить услугу");
        }
    }

    public void removeService(@NotBlank String login, @NotBlank String name) {
        selectTargetUser(login);
        try {
            Connection.Response response = request(
                    Request.of(
                            "index.php",
                            Map.of("act", "tarif"),
                            Map.of("Drop_ext", "Установить тариф", "x_sel", name),
                            Connection.Method.POST
                    )
            );
            authSuccessfulCheck(response);
        } catch (IOException e) {
            throw new ResponseException("Не удалось удалить услугу");
        }
    }

    public void changeTariff(@NotBlank String login, @NonNull Integer id) {
        selectTargetUser(login);
        try {
            Connection.Response response = request(
                    Request.of(
                            "index.php",
                            Map.of("act", "tarif"),
                            Map.of("ChangeAction", "Установить тариф", "t_sel", id.toString()),
                            Connection.Method.POST
                    )
            );
            authSuccessfulCheck(response);
        } catch (IOException e) {
            throw new ResponseException("Не удалось изменить тариф");
        }
    }

    public void normalizeTariff(@NotBlank String login) {
        selectTargetUser(login);
        try {
            Connection.Response response = request(
                    Request.of(
                            "index.php",
                            Map.of("act", "tarif"),
                            Map.of("Set_Norm", "Нормализ."),
                            Connection.Method.POST
                    )
            );
            authSuccessfulCheck(response);
        } catch (IOException e) {
            throw new ResponseException("Не удалось изменить тариф");
        }
    }

    private void authSuccessfulCheck(Connection.Response response) throws IOException {
        Document document = response.bufferUp().parse();
        if (document.body().children().isEmpty() || document.body().text().contains("adm_auth.operator_not_found")) {
            throw new ResponseException("Не авторизованный запрос " + getHost() + " " + getCredentials());
        }
    }

    public void balanceReset(String login, String comment) {
        selectTargetUser(login);
        try {
            Connection.Response response = request(
                    Request.of(
                            "index.php",
                            Map.of("act", "pay_action"),
                            Map.of(
                                    "rmoney","0",
                                    "ptype", "10",
                                    "smoney",  "0",
                                    "straf", "0",
                                    "stime", "0",
                                    "subm_pay", "Оплата",
                                    "cmt", comment
                            ),
                            Connection.Method.POST
                    )
            );
            authSuccessfulCheck(response);
        } catch (IOException e) {
            throw new ResponseException("Не удалось сбросить баланс");
        }
    }

    public Boolean isLoginEnable(String login) {
        selectTargetUser(login);
        try {
            Connection.Response response = request(
                    Request.of(
                            "index.php",
                            Map.of("act", "user_work"),
                            Connection.Method.GET
                    )
            );
            authSuccessfulCheck(response);
            Document document = response.bufferUp().parse();
            Element element = document.selectFirst("body > center > form > table.def_table > tbody > tr:nth-child(4) > td:nth-child(4)");
            return element != null && element.text().equals("1");
        } catch (IOException e) {
            throw new ResponseException("Не удалось сбросить баланс");
        }
    }

    public void enableLogin(String login) {
        selectTargetUser(login);
        try {
            Connection.Response response = request(
                    Request.of(
                            "index.php",
                            Map.of("act", "auth_control"),
                            Map.of("On_All", "Вкл.все"),
                            Connection.Method.POST
                    )
            );
            authSuccessfulCheck(response);
        } catch (IOException e) {
            throw new ResponseException("Не удалось активировать логин");
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

    @Getter
    @Setter
    public static class CreateUserForm {

        @NonNull
        private Address address;
        @NotBlank
        private String fullName;
        @NotBlank
        private String phone;
        @NonNull
        private UserType userType;

        public static CreateUserForm of(Address address, String fullName, String phone, UserType userType) {
            CreateUserForm form = new CreateUserForm();
            form.setAddress(address);
            form.setFullName(fullName);
            form.setPhone(phone);
            form.setUserType(userType);
            return form;
        }

        public String getPreparePhone() {
            String cleared = phone.replaceAll("[-() ]", "").trim();
            return cleared.substring(1);
        }

        public Map<String, String> toRequestBody(String login) {
            Map<String, String> body = new HashMap<>();

            if (userType == UserType.ORG) {
                body.put("login", "BIZ" + login);
            } else {
                body.put("login", login);
            }

            if (address.getStreet() == null)
                throw new ResponseException("Не указана улица для создания абонента");

            if (address.getStreet().getBillingAlias() == null || address.getStreet().getBillingAlias().isBlank())
                throw new ResponseException("Не указан псевдоним для улицы в биллинге");

            if (address.getHouseNamePart() == null || address.getHouseNamePart().isBlank())
                throw new ResponseException("Не указан адрес дома");

            String billingAlias = address.getStreet().getBillingAlias();

            String house = address.getHouseNamePart();

            String apartNum = "";
            if (address.getApartmentNum() != null)
                apartNum = address.getApartmentNum().toString();

            String entrance = "";
            if (address.getEntrance() != null)
                entrance = address.getEntrance().toString();

            String floor = "";
            if (address.getFloor() != null)
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
    }

    @Getter
    @Setter
    public static class UserTariff {
        private Integer id;
        private String name;
        private Float cost;
        private String description;

        private UserTariff() {
        }

        public static UserTariff from(Element element) {
            UserTariff userTariff = new UserTariff();

            Element idCol = element.child(0);
            Element nameCol = element.child(1);
            Element costCol = element.child(2);
            Element descCol = element.child(3);

            userTariff.setId(Integer.parseInt(idCol.text()));
            userTariff.setName(nameCol.text());
            userTariff.setCost(Float.parseFloat(costCol.text()));
            userTariff.setDescription(descCol.text());

            return userTariff;
        }
    }
}
