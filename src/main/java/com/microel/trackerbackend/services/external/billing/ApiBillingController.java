package com.microel.trackerbackend.services.external.billing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcospassos.phpserializer.Serializer;
import com.marcospassos.phpserializer.SerializerBuilder;
import com.microel.trackerbackend.controllers.configuration.Configuration;
import com.microel.trackerbackend.controllers.configuration.FailedToWriteConfigurationException;
import com.microel.trackerbackend.controllers.configuration.entity.BillingConf;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.entities.address.Address;
import com.microel.trackerbackend.storage.exceptions.EmptyResponse;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.CRC32;

@Component
@Slf4j
public class ApiBillingController {

    private final XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
    private final XmlRpcClient client = new XmlRpcClient();
    private final LinkedHashMap<Object, Object> rqMap = new LinkedHashMap<>();
    private final Map<String, Object> argsMap = new HashMap<>();
    private final Serializer serializer = new SerializerBuilder().build();
    private final Configuration configStorage;
    private final StompController stompController;
    private BillingConf billingConf;

    public ApiBillingController(Configuration configStorage, StompController stompController) throws MalformedURLException {
        this.configStorage = configStorage;
        billingConf = configStorage.loadOrDefault(BillingConf.class, new BillingConf());
        this.stompController = stompController;
        authenticate();
    }

    public static String getUserStatusName(Integer state, @Nullable Integer tstate) {
        if (state == null) return "Нет статуса";
        if (tstate == null) tstate = 0;
        switch (state) {
            case 1 -> {
                switch (tstate) {
                    case 1 -> {
                        return "Активный";
                    }
                    case 3 -> {
                        return "Отл. платеж";
                    }
                }
                return "Активный";
            }
            case 5 -> {
                return "Приостановлен";
            }
            case 7 -> {
                switch (tstate) {
                    case 8 -> {
                        return "Нет денег";
                    }
                    case 9 -> {
                        return "Отл. платеж просрочен";
                    }
                }
                return "Нет денег";
            }
            case 11 -> {
                return "Отключен>30дн";
            }
            default -> {
                return "Неактивный";
            }
        }
    }

    public static String getUserStatusColor(Integer state, @Nullable Integer tstate) {
        if (state == null) return "red";
        if (tstate == null) tstate = 0;
        switch (state) {
            case 1 -> {
                switch (tstate) {
                    case 1 -> {
                        return "#7CE065";
                    }
                    case 3 -> {
                        return "#78D3E3";
                    }
                }
                return "#7CE065";
            }
            case 5 -> {
                return "#A1A6E3";
            }
            case 7 -> {
                switch (tstate) {
                    case 8 -> {
                        return "#F2CC5A";
                    }
                    case 9 -> {
                        return "#D15151";
                    }
                }
                return "#F2CC5A";
            }
            default -> {
                return "#D15151";
            }
        }
    }

    private void authenticate() throws MalformedURLException {
        if (billingConf == null || !billingConf.isFilled()) {
            log.warn("Конфигурация биллинга не задана. Авторизация не возможна.");
            return;
        }
//        config.setServerURL(new URL("http://10.50.0.7:91/rpc_server.php"));
        config.setServerURL(new URL("http://" + billingConf.getHost() + ":" + billingConf.getPort() + "/rpc_server.php"));
        client.setConfig(config);
        Map<String, Map<String, String>> sysMap = Map.of("auth", Map.of("dname", billingConf.getDaemonName()));
        argsMap.put("method", "WDaemon2");
        argsMap.put("rq", rqMap);
        argsMap.put("_Sys", sysMap);
        argsMap.put("__hostname", billingConf.getLogin());
//        argsMap.put("__hostname", "sandbox");
    }

    private void prepareRequestBody(String functionName) {
        rqMap.clear();
        rqMap.put(0, "WDaemon.php");
        rqMap.put("__call", functionName);
        rqMap.put("__ip", billingConf.getSelfIp());
        rqMap.put("__person", "root:" + billingConf.getSelfIp());
    }

    private void setRequestProp(String name, String value) {
        rqMap.put(name, value);
    }

    public List<UserItemData> getUsersByLogin(String login, Boolean isActive) {
        prepareRequestBody("UsersByLogin");
        setRequestProp("skey", login);
        setRequestProp("uname_live", isActive ? "1" : "0");
        calculateSign();
        try {
            return ((Map<String, Object>) execute().get("xdata")).values().stream().map(UserItemData::from).collect(Collectors.toList());
        } catch (ClassCastException e) {
            return new ArrayList<>();
        }
    }

    public List<UserItemData> getUsersByFio(String query, Boolean isActive) {

        prepareRequestBody("UsersByFio");
        setRequestProp("skey", query);
        setRequestProp("uname_live", isActive ? "1" : "0");
        calculateSign();

        try {
            return ((Map<String, Object>) execute().get("xdata")).values().stream().map(UserItemData::from).collect(Collectors.toList());
        } catch (ClassCastException e) {
            return new ArrayList<>();
        }
    }

    public List<UserItemData> getUsersByAddress(String address, Boolean isActive) {

        prepareRequestBody("UsersByAddr");
        setRequestProp("skey", address);
        setRequestProp("uname_live", isActive ? "1" : "0");
        calculateSign();

        try {
            return ((Map<String, Object>) execute().get("xdata")).values().stream().map(UserItemData::from).collect(Collectors.toList());
        } catch (ClassCastException e) {
            return new ArrayList<>();
        }
    }

    public List<UserItemData> getUserSuggestions(String stringQuery) {
        Pattern loginPattern = Pattern.compile("^(BIZ)?(\\d{8}|[A-z.\\d-@]+)");
        Pattern addressPattern = Pattern.compile("^((?<st>[А-я.\\d-]+) )?(?<ha>\\d{1,3}(/\\d{1,3})?[А-я]*(_\\d{1,3})?(-\\d{1,3})?)");
        Matcher loginMatcher = loginPattern.matcher(stringQuery);
        Matcher addressMatcher = addressPattern.matcher(stringQuery);
        if(addressMatcher.find()){
            String street = null;
            String house = null;
            try {
                street = addressMatcher.group("st");
            }catch (Exception ignored){}
            try {
                house = addressMatcher.group("ha");
            }catch (Exception ignored){}
            if(street != null && house != null){
                String finalHouse = house;
                List<UserItemData> usersByAddress = getUsersByAddress(street, false)
                        .stream()
                        .filter(f -> f.addr.contains(finalHouse))
                        .sorted(Comparator.comparing(UserItemData::getAddr))
                        .toList();
                if(!usersByAddress.isEmpty())
                    return usersByAddress;
            }
            if(street != null){
                List<UserItemData> usersByAddress = getUsersByAddress(street, false)
                        .stream()
                        .sorted(Comparator.comparing(UserItemData::getAddr))
                        .toList();
                if(!usersByAddress.isEmpty())
                    return usersByAddress;
            }
            if(house != null){
                List<UserItemData> usersByAddress = getUsersByAddress(house, false)
                        .stream()
                        .sorted(Comparator.comparing(UserItemData::getAddr))
                        .toList();
                if(!usersByAddress.isEmpty())
                    return usersByAddress;
            }
        }
        if(loginMatcher.find()) {
            return getUsersByLogin(stringQuery, false);
        }
        return List.of();
    }

    public String getCalculateCountingLives(CountingLivesForm form) {
        List<UserItemData> usersByAddress = getUsersByAddress(form.getAddress().getBillingAddress(), true);
        if (usersByAddress == null || usersByAddress.isEmpty()) {
            usersByAddress = getUsersByAddress(form.getAddress().getBillingAddress(true), true);
        }
        if (usersByAddress == null || usersByAddress.isEmpty()) {
            return "Живых пользователей нет";
        }
        List<UserItemData> lives = usersByAddress.stream().filter(UserItemData::getIsActive).filter(f -> f.isApartNumberInRange(form.getStartApart(), form.getEndApart())).toList();
        if (lives.isEmpty()) return "Живых пользователей нет";

        return "Живые: " + lives.stream().sorted((o1, o2) -> Comparator.nullsLast(Integer::compareTo).compare(o1.getApartNumber(), o2.getApartNumber()))
                .map(UserItemData::getAddressOrName).collect(Collectors.joining(", ")) + "  Кол-во: " + lives.size();
    }

    public TotalUserInfo getUserInfo(String login) {
        prepareRequestBody("getUserInfo");
        setRequestProp("uname", login);
        calculateSign();
        return TotalUserInfo.from(execute());
    }

    public UserEvents getUserEvents(String login) {
        prepareRequestBody("getUserEvents");
        setRequestProp("uname", login);
        calculateSign();
        return UserEvents.from(execute());
    }

    public void updateBalance(String login, Float sum, BillingPayType payType, String comment) {
        prepareRequestBody("regPay");
        setRequestProp("uname", login);
        setRequestProp("sum", String.valueOf(sum.intValue()));
        setRequestProp("paytype", String.valueOf(payType.getValue()));
        setRequestProp("coment", comment);
        calculateSign();
        execute();
        getUpdatedUserAndPushUpdate(login);
    }

    public void deferredPayment(String login) {
        prepareRequestBody("setDefer");
        setRequestProp("uname", login);
        calculateSign();
        execute();
        getUpdatedUserAndPushUpdate(login);
    }

    public void stopUserService(String login) {
        prepareRequestBody("setStop");
        setRequestProp("uname", login);
        calculateSign();
        execute();
        getUpdatedUserAndPushUpdate(login);
    }

    public void startUserService(String login) {
        prepareRequestBody("setStart");
        setRequestProp("uname", login);
        calculateSign();
        execute();
        getUpdatedUserAndPushUpdate(login);
    }

    public void getHelp() throws XmlRpcException {
        rqMap.clear();
        rqMap.put(0, "WDaemon.php");

//        rqMap.put("skey", "abc");
//        rqMap.put("uname_live", "1");
//        rqMap.put("__call", "UsersByLogin");
        rqMap.put("help", "");
//        rqMap.put("__ip", "10.1.3.150");
//        rqMap.put("__person", "root:10.1.3.150");

        calculateSign();
        System.out.println(argsMap);
        Map<String, Object> execute = (Map<String, Object>) client.execute("daemons.*", Collections.singletonList(argsMap));
        System.out.println(execute);
    }

    private void calculateSign() {
        if (billingConf == null || !billingConf.isFilled()) {
            throw new BillingAuthenticationException("Нет заданной конфигурации подключения к биллингу");
        }
        CRC32 sign = new CRC32();
//        sign.update((serializer.serialize(rqMap) + "130").getBytes());
        sign.update((serializer.serialize(rqMap) + billingConf.getPassword()).getBytes());
        argsMap.put("__sign", String.valueOf(sign.getValue()));
    }

    public BillingConf getConfiguration() {
        return billingConf;
    }

    public void setConfiguration(BillingConf billingConf) throws MalformedURLException, FailedToWriteConfigurationException {
        if (!billingConf.isFilled()) throw new IllegalFields("Конфигурация не заполнена");
        this.billingConf = billingConf;
        authenticate();
        configStorage.save(billingConf);
        stompController.changeBillingConfig(billingConf);
    }

    public void getUpdatedUserAndPushUpdate(String login) {
        TotalUserInfo userInfo = getUserInfo(login);
        stompController.updateBillingUser(userInfo);
    }

    private Map<String, Object> execute() {
        try {
            Map<String, Object> execute = (Map<String, Object>) client.execute("daemons.*", Collections.singletonList(argsMap));
            String state = execute.get("state").toString();
            if (state.equals("Error")) {
                throw new EmptyResponse(execute.get("__msg").toString());
            }
            if (state.equals("STrap") || state.equals("Trap")) {
                throw new EmptyResponse(execute.get("msg").toString());
            }
            return execute;
        } catch (XmlRpcException e) {
            throw new EntryNotFound("Запрос в XMLRPC не удался");
        }
    }

    public TotalUserInfo testUser() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue("{\n" +
                    "    \"ibase\": {\n" +
                    "        \"addr\": \"Строителей 6--\",\n" +
                    "        \"coment\": \"\",\n" +
                    "        \"credit\": 0.0,\n" +
                    "        \"ddog\": \"2017-03-27T21:00:00.000+00:00\",\n" +
                    "        \"fio\": \"тестовый логин\",\n" +
                    "        \"money\": 0.0,\n" +
                    "        \"ndog\": \"test\",\n" +
                    "        \"phone\": \"-\"\n" +
                    "    },\n" +
                    "    \"karma\": 7,\n" +
                    "    \"newTarif\": {\n" +
                    "        \"cntDhcp\": \"0\",\n" +
                    "        \"cntVpn\": \"0\",\n" +
                    "        \"dstate\": \"2024-02-22 16:16:14\",\n" +
                    "        \"edate\": \"2024-02-22T12:32:23.000+00:00\",\n" +
                    "        \"endstate\": null,\n" +
                    "        \"extIp\": \"\",\n" +
                    "        \"hdate\": \"2024-01-22T12:32:23.000+00:00\",\n" +
                    "        \"intIp\": \"\",\n" +
                    "        \"ipDhcp\": \"\",\n" +
                    "        \"ipNat\": \"\",\n" +
                    "        \"ipUser\": \"\",\n" +
                    "        \"ipVpn\": \"\",\n" +
                    "        \"last\": \"1999-12-31T21:00:00.000+00:00\",\n" +
                    "        \"lastDhcp\": null,\n" +
                    "        \"lastVpn\": null,\n" +
                    "        \"ndog\": \"test\",\n" +
                    "        \"online\": 0,\n" +
                    "        \"pstate\": 11,\n" +
                    "        \"speed\": 100,\n" +
                    "        \"staj\": 20,\n" +
                    "        \"state\": 11,\n" +
                    "        \"tarif\": \"NewHomePromo3\",\n" +
                    "        \"tspeed\": 100,\n" +
                    "        \"tstate\": 10,\n" +
                    "        \"uname\": \"test\",\n" +
                    "        \"xservice\": \"\",\n" +
                    "        \"isPossibleEnableDeferredPayment\": false,\n" +
                    "        \"isServiceSuspended\": false,\n" +
                    "        \"userStatusName\": \"Отключен>30дн\",\n" +
                    "        \"userStatusColor\": \"#D15151\"\n" +
                    "    },\n" +
                    "    \"oldTarif\": [\n" +
                    "        {\n" +
                    "            \"adate\": null,\n" +
                    "            \"edate\": null,\n" +
                    "            \"hdate\": null,\n" +
                    "            \"mdate\": null,\n" +
                    "            \"price\": 0.0,\n" +
                    "            \"service\": \"\",\n" +
                    "            \"state\": 0,\n" +
                    "            \"stype\": 0,\n" +
                    "            \"iext\": 0\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"state\": \"OK\",\n" +
                    "    \"uname\": \"test\"\n" +
                    "}", TotalUserInfo.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Getter
    @Setter
    public static class CountingLivesForm {
        private Address address;
        private Integer startApart;
        private Integer endApart;

        public static CountingLivesForm of(Address address, Integer startApart, Integer endApart) {
            CountingLivesForm form = new CountingLivesForm();
            form.setAddress(address);
            form.setStartApart(startApart);
            form.setEndApart(endApart);
            return form;
        }
    }

    @Getter
    @Setter
    public static class UserItemData {
        private String tarif;
        private String uname;
        private Date last;
        private String phone;
        private String coment;
        private Integer utype;
        private Integer state;
        private String addr;
        private String fio;

        public static UserItemData from(Object o) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            UserItemData u = new UserItemData();
            Map<String, String> map = (Map<String, String>) o;
            u.setTarif(map.get("tarif"));
            u.setUname(map.get("uname"));
            try {
                u.setLast(format.parse(map.get("last")));
            } catch (ParseException | NullPointerException e) {
                u.setLast(null);
            }
            u.setPhone(map.get("phone"));
            u.setComent(map.get("coment"));
            try {
                u.setUtype(Integer.valueOf(map.get("utype")));
            } catch (NumberFormatException | NullPointerException e) {
                u.setUtype(null);
            }
            try {
                u.setState(Integer.valueOf(map.get("state")));
            } catch (NumberFormatException | NullPointerException e) {
                u.setState(null);
            }
            u.setAddr(map.get("addr"));
            u.setFio(map.get("fio"));

            return u;
        }

        @JsonIgnore
        public Boolean getIsActive() {
            return state == 1 || state == 7;
        }

        @JsonIgnore
        @Nullable
        public String getApartName() {
            Pattern apartPattern = Pattern.compile("[^-]+-(.+)");
            Matcher apartMatcher = apartPattern.matcher(addr);
            if (apartMatcher.find()) {
                return apartMatcher.group(1).replaceAll(" \\(\\d{0,2}\\.?\\d{0,2}\\)", "");
            }
            return null;
        }

        @JsonIgnore
        public String getAddressOrName() {
            String apartName = getApartName();
            if (apartName == null || apartName.isBlank()) return uname;
            return apartName;
        }

        @JsonIgnore
        @Nullable
        public Integer getApartNumber() {
            String apartName = getApartName();
            if (apartName == null) return null;
            Pattern digitPattern = Pattern.compile("(\\d+)");
            Matcher digitMatcher = digitPattern.matcher(apartName);
            if (digitMatcher.find()) {
                return Integer.parseInt(digitMatcher.group(1));
            }
            return null;
        }

        @JsonIgnore
        public Boolean isApartNumberInRange(int start, int end) {
            Integer apartNumber = getApartNumber();
            if (apartNumber != null) {
                return apartNumber >= start && apartNumber <= end;
            }
            return true;
        }

        public String getStateName() {
            return ApiBillingController.getUserStatusName(state, null);
        }

        public String getStateColor() {
            return ApiBillingController.getUserStatusColor(state, null);
        }
    }

    @Getter
    @Setter
    public static class UserMainInfo {
        private String addr;
        private String coment;
        private Float credit;
        private Date ddog;
        private String fio;
        private Float money;
        private String ndog;
        private String phone;

        public static UserMainInfo from(Object o) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            UserMainInfo u = new UserMainInfo();
            Map<String, String> map = (Map<String, String>) o;
            if (map == null) throw new IllegalFields("Информация о клиенте не найдена");
            u.addr = map.get("addr");
            u.coment = map.get("coment");
            try {

                u.credit = Float.valueOf(map.get("credit"));
            } catch (NullPointerException e) {
                u.credit = 0f;
            }
            try {
                u.ddog = format.parse(map.get("ddog"));
            } catch (ParseException | NullPointerException e) {
                u.ddog = null;
            }
            u.fio = map.get("fio");
            try {

                u.money = Float.valueOf(map.get("money"));
            } catch (NumberFormatException e) {
                u.money = 0f;
            }
            u.ndog = map.get("ndog");
            u.phone = map.get("phone");
            return u;
        }
    }

    @Getter
    @Setter
    public static class UserNewTarif {
        private String cntDhcp;
        private String cntVpn;
        private String dstate;
        private Date edate;
        private Date endstate;
        private String extIp;
        private Date hdate;
        private String intIp;
        private String ipDhcp;
        private String ipNat;
        private String ipUser;
        private String ipVpn;
        private Date last;
        private Date lastDhcp;
        private Date lastVpn;
        private String ndog;
        private Integer online;
        private Integer pstate;
        private Integer speed;
        private Integer staj;
        private Integer state;
        private String tarif;
        private Integer tspeed;
        private Integer tstate;
        private String uname;
        private String xservice;

        public static UserNewTarif from(Object o) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            UserNewTarif u = new UserNewTarif();
            Map<String, String> map = (Map<String, String>) o;
            u.cntDhcp = map.get("cnt_dhcp");
            u.cntVpn = map.get("cnt_vpn");
            u.dstate = map.get("dstate");
            try {
                u.edate = format.parse(map.get("edate"));
            } catch (ParseException e) {
                u.edate = null;
            }
            try {
                u.endstate = format.parse(map.get("endstate"));
            } catch (ParseException e) {
                u.endstate = null;
            }
            u.extIp = map.get("ext_ip");
            try {
                u.hdate = format.parse(map.get("hdate"));
            } catch (ParseException e) {
                u.hdate = null;
            }
            u.intIp = map.get("int_ip");
            u.ipDhcp = map.get("ip_dhcp");
            u.ipNat = map.get("ip_nat");
            u.ipUser = map.get("ip_user");
            u.ipVpn = map.get("ip_vpn");
            try {
                u.last = format.parse(map.get("last"));
            } catch (ParseException | NullPointerException e) {
                u.last = null;
            }
            try {
                u.lastDhcp = format.parse(map.get("last_dhcp"));
            } catch (ParseException | NullPointerException e) {
                u.lastDhcp = null;
            }
            try {
                u.lastVpn = format.parse(map.get("last_vpn"));
            } catch (ParseException | NullPointerException e) {
                u.lastVpn = null;
            }
            u.ndog = map.get("ndog");
            try {
                u.online = Integer.valueOf(map.get("online"));
            } catch (NumberFormatException e) {
                u.online = 0;
            }
            try {
                u.pstate = Integer.valueOf(map.get("pstate"));
            } catch (NumberFormatException e) {
                u.pstate = 0;
            }
            try {
                u.speed = Integer.valueOf(map.get("speed"));
            } catch (NumberFormatException e) {
                u.speed = 0;
            }
            try {
                u.staj = Integer.valueOf(map.get("staj"));
            } catch (NumberFormatException e) {
                u.staj = 0;
            }
            try {
                u.state = Integer.valueOf(map.get("state"));
            } catch (NumberFormatException e) {
                u.state = 0;
            }
            u.tarif = map.get("tarif");
            try {
                u.tspeed = Integer.valueOf(map.get("tspeed"));
            } catch (NumberFormatException e) {
                u.tspeed = 0;
            }
            try {
                u.tstate = Integer.valueOf(map.get("tstate"));
            } catch (NumberFormatException e) {
                u.tstate = 0;
            }
            u.uname = map.get("uname");
            u.xservice = map.get("xservice");

            return u;
        }

        public String getUserStatusName() {
            return ApiBillingController.getUserStatusName(state, tstate);
        }

        public String getUserStatusColor() {
            return ApiBillingController.getUserStatusColor(state, tstate);
        }

        public Boolean getIsPossibleEnableDeferredPayment() {
            return Objects.equals(state, 7) && Objects.equals(tstate, 8);
        }

        public Boolean getIsServiceSuspended() {
            return Objects.equals(state, 5);
        }
    }

    @Getter
    @Setter
    public static class OldTarifItem {
        private Date adate;
        private Date edate;
        private Date hdate;
        private Integer iExt;
        private Date mdate;
        private Float price;
        private String service;
        private Integer state;
        private Integer stype;

        public static OldTarifItem from(Object o) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

            OldTarifItem u = new OldTarifItem();
            Map<String, String> map = (Map<String, String>) o;
            try {
                u.adate = format.parse(map.get("adate"));
            } catch (ParseException | NullPointerException e) {
                u.adate = null;
            }
            try {
                u.edate = format.parse(map.get("edate"));
            } catch (ParseException | NullPointerException e) {
                u.edate = null;
            }
            try {
                u.hdate = format.parse(map.get("hdate"));
            } catch (ParseException | NullPointerException e) {
                u.hdate = null;
            }
            try {
                u.iExt = Integer.valueOf(map.get("i_ext"));
            } catch (NumberFormatException | ClassCastException e) {
                u.iExt = 0;
            }
            try {
                u.mdate = format.parse(map.get("mdate"));
            } catch (ParseException | NullPointerException e) {
                u.mdate = null;
            }
            try {
                u.price = Float.valueOf(map.get("price"));
            } catch (NumberFormatException e) {
                u.price = 0f;
            }
            u.service = map.get("service");
            try {
                u.state = Integer.valueOf(map.get("state"));
            } catch (NumberFormatException e) {
                u.state = 0;
            }
            try {
                u.stype = Integer.valueOf(map.get("stype"));
            } catch (NumberFormatException e) {
                u.stype = 0;
            }
            return u;
        }
    }

    @Getter
    @Setter
    public static class TotalUserInfo {
        private UserMainInfo ibase;
        private Integer karma;
        private UserNewTarif newTarif;
        private List<OldTarifItem> oldTarif;
        private String state;
        private String uname;

        public static TotalUserInfo from(Object o) {
            TotalUserInfo u = new TotalUserInfo();
            Map<String, Object> map = (Map<String, Object>) o;
            u.ibase = UserMainInfo.from(map.get("ibase"));
            try {
                u.karma = Integer.valueOf((String) map.get("karma"));
            } catch (NumberFormatException e) {
                u.karma = null;
            }
            u.newTarif = UserNewTarif.from(map.get("new_tarif"));
            Object[] oTarifs = (Object[]) map.get("old_tarif");
            u.oldTarif = Stream.of(oTarifs).map(OldTarifItem::from).collect(Collectors.toList());
            u.state = (String) map.get("state");
            u.uname = (String) map.get("uname");
            return u;
        }
    }

    @Getter
    @Setter
    public static class UserEvents {
        private String uname;
        private String fromDate;
        private List<UserEventLog> events;
        private List<UserPaysLog> pays;
        private List<UserTariffLog> tarifs;

        public static UserEvents from(Object o) {
            UserEvents u = new UserEvents();
            Map<String, Object> map = (Map<String, Object>) o;
            u.uname = (String) map.get("uname");
            u.fromDate = (String) map.get("from_date");
            u.events = Stream.of(((Object[]) map.get("events"))).map(UserEventLog::from).collect(Collectors.toList());
            u.pays = Stream.of(((Object[]) map.get("pays"))).map(UserPaysLog::from).collect(Collectors.toList());
            u.tarifs = Stream.of(((Object[]) map.get("tarifs"))).map(UserTariffLog::from).collect(Collectors.toList());
            return u;
        }
    }

    @Getter
    @Setter
    public static class UserEventLog {
        private String evdate;
        private String evtime;
        private Date evTimeStamp;
        private String xtype;
        private String lastuse;
        private String uname;
        private Float money;
        private String coment;
        private Float price;
        private String event;
        private Date edate;
        private Date hdate;
        private String info;

        public static UserEventLog from(Object o) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            UserEventLog u = new UserEventLog();
            Map<String, String> map = (Map<String, String>) o;
            u.evdate = map.get("evdate");
            u.evtime = map.get("evtime");
            try {
                u.evTimeStamp = format.parse(u.evdate + " " + u.evtime);
            } catch (ParseException | NullPointerException e) {
                u.evTimeStamp = null;
            }
            u.xtype = map.get("xtype");
            u.lastuse = map.get("lastuse");
            u.uname = map.get("uname");
            try {
                u.money = Float.valueOf(map.get("money"));
            } catch (NumberFormatException e) {
                u.money = null;
            }
            u.coment = map.get("coment");
            try {
                u.price = Float.valueOf(map.get("price"));
            } catch (NumberFormatException e) {
                u.price = null;
            }
            u.event = map.get("event");
            try {
                u.edate = format.parse(map.get("edate"));
            } catch (ParseException | NullPointerException e) {
                u.edate = null;
            }
            try {
                u.hdate = format.parse(map.get("hdate"));
            } catch (ParseException | NullPointerException e) {
                u.hdate = null;
            }
            u.info = map.get("info");
            return u;
        }

        public String getEventName() {
            switch (event) {
                case "A_NEXT", "A_NEXT_X", "A_NEXT_KTV" -> {
                    return "Начало тарифа";
                }
                case "A_END", "A_END_X", "A_END_KTV" -> {
                    return "Окончание тарифа";
                }
                case "A_CHANGE" -> {
                    return "Смена тарифа";
                }
                case "P_FPAY" -> {
                    if (money < 0) {
                        return "Списание";
                    } else {
                        return "Пополнение";
                    }
                }
                case "P_TPAY" -> {
                    return "Пополнение счета";
                }
                case "P_RETM" -> {
                    return "Перерасчет";
                }
                default -> {
                    return event;
                }
            }
        }

        public String getEventColor() {
            switch (event) {
                case "A_NEXT", "A_NEXT_X", "A_NEXT_KTV" -> {
                    return "green";
                }
                case "A_END", "A_END_X", "A_END_KTV" -> {
                    return "red";
                }
                case "A_CHANGE" -> {
                    return "orange";
                }
                case "P_FPAY" -> {
                    if (money < 0) {
                        return "red";
                    } else {
                        return "green";
                    }
                }
                case "P_TPAY" -> {
                    return "blue";
                }
                case "P_RETM" -> {
                    return "purple";
                }
                default -> {
                    return "black";
                }
            }
        }

        public Integer getMoneyDirection() {
            switch (event) {
                case "A_END", "A_END_X", "A_END_KTV" -> {
                    return -1;
                }
                case "P_FPAY", "P_TPAY", "P_RETM" -> {
                    if (money < 0) return -1;
                    return 1;
                }
                default -> {
                    return 0;
                }
            }
        }
    }

    @Getter
    @Setter
    public static class UserPaysLog {
        private Float bmoney;
        private String uname;
        private Float money;
        private Date pdate;
        private Integer ptype;
        private String cmt;
        private String who;

        public static UserPaysLog from(Object o) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            UserPaysLog u = new UserPaysLog();
            Map<String, String> map = (Map<String, String>) o;
            try {
                u.bmoney = Float.valueOf(map.get("bmoney"));
            } catch (NumberFormatException e) {
                u.bmoney = null;
            }
            u.uname = map.get("uname");
            try {
                u.money = Float.valueOf(map.get("money"));
            } catch (NumberFormatException e) {
                u.money = null;
            }
            try {
                u.pdate = format.parse(map.get("pdate"));
            } catch (ParseException | NullPointerException e) {
                u.pdate = null;
            }
            try {
                u.ptype = Integer.valueOf(map.get("ptype"));
            } catch (NumberFormatException e) {
                u.ptype = null;
            }
            u.cmt = map.get("cmt");
            u.who = map.get("who");
            return u;
        }

        public String getPtypeName() {
            switch (ptype) {
                case 1 -> {
                    return "Касса";
                }
                case 25 -> {
                    return "Карта";
                }
                case 2 -> {
                    return "Банк";
                }
                case 3 -> {
                    return "Возврат";
                }
                case 4 -> {
                    return "Кредит";
                }
                case 9 -> {
                    return "Внешний";
                }
                case 10 -> {
                    return "Заморозка";
                }
                case 11 -> {
                    return "Служебный";
                }
                case 12 -> {
                    return "Перерасчет";
                }
            }
            return "Неизвестный";
        }

        public String getPtypeColor() {
            switch (ptype) {
                case 1, 12 -> {
                    return "green";
                }
                case 25 -> {
                    return "blue";
                }
                case 2 -> {
                    return "grey";
                }
                case 3 -> {
                    return "yellow";
                }
                case 4 -> {
                    return "red";
                }
                case 9 -> {
                    return "orange";
                }
                case 10 -> {
                    return "#53AAFC";
                }
                case 11 -> {
                    return "purple";
                }
            }
            return "black";
        }
    }

    @Getter
    @Setter
    public static class UserTariffLog {
        private Date lasttime;
        private String uname;
        private Date mdate;
        private String service;
        private Float price;
        private Integer stype;
        private Date adate;
        private Integer state;
        private Date edate;
        private Date hdate;
        private Integer iExt;

        public static UserTariffLog from(Object o) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            UserTariffLog u = new UserTariffLog();
            Map<String, String> map = (Map<String, String>) o;
            try {
                u.lasttime = format.parse(map.get("lasttime"));
            } catch (ParseException | NullPointerException e) {
                u.lasttime = null;
            }
            u.uname = map.get("uname");
            try {
                u.mdate = format.parse(map.get("mdate"));
            } catch (ParseException | NullPointerException e) {
                u.mdate = null;
            }
            u.service = map.get("service");
            try {
                u.price = Float.valueOf(map.get("price"));
            } catch (NumberFormatException e) {
                u.price = null;
            }
            try {
                u.stype = Integer.valueOf(map.get("stype"));
            } catch (NumberFormatException e) {
                u.stype = null;
            }
            try {
                u.adate = format.parse(map.get("adate"));
            } catch (ParseException | NullPointerException e) {
                u.adate = null;
            }
            try {
                u.state = Integer.valueOf(map.get("state"));
            } catch (NumberFormatException e) {
                u.state = null;
            }
            try {
                u.edate = format.parse(map.get("edate"));
            } catch (ParseException | NullPointerException e) {
                u.edate = null;
            }
            try {
                u.hdate = format.parse(map.get("hdate"));
            } catch (ParseException | NullPointerException e) {
                u.hdate = null;
            }
            try {
                u.iExt = Integer.valueOf(map.get("iExt"));
            } catch (NumberFormatException e) {
                u.iExt = null;
            }
            return u;
        }
    }

    @Getter
    @Setter
    public static class UpdateBalanceForm {
        private Float sum;
        private BillingPayType payType;
        private String comment;

        public void validate() {
            if (sum == null || sum == 0f) {
                throw new ResponseException("Не указана сумма оплаты");
            }
            if (payType == null) {
                throw new ResponseException("Тип оплаты не указан");
            }
            if (comment == null || comment.isBlank()) {
                throw new ResponseException("Комментарий к платежу не указан");
            }
        }
    }
}
