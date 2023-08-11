package com.microel.trackerbackend.services.api.external.billing;

import com.marcospassos.phpserializer.Serializer;
import com.marcospassos.phpserializer.SerializerBuilder;
import com.microel.trackerbackend.controllers.configuration.ConfigurationStorage;
import com.microel.trackerbackend.controllers.configuration.FailedToWriteConfigurationException;
import com.microel.trackerbackend.controllers.configuration.entity.BillingConf;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.CRC32;

@Component
@Slf4j
public class BillingRequestController {

    private final XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
    private final XmlRpcClient client = new XmlRpcClient();
    private final LinkedHashMap<Object, Object> rqMap = new LinkedHashMap<>();
    private final Map<String, Object> argsMap = new HashMap<>();
    private final Serializer serializer = new SerializerBuilder().build();
    private final ConfigurationStorage configStorage;
    private final StompController stompController;
    private BillingConf billingConf;

    public BillingRequestController(ConfigurationStorage configStorage, StompController stompController) throws MalformedURLException {
        this.configStorage = configStorage;
        billingConf = configStorage.loadOrDefault(BillingConf.class, new BillingConf());
        this.stompController = stompController;
        authenticate();
    }

    private void authenticate() throws MalformedURLException {
        if(billingConf == null || !billingConf.isFilled()) {
            log.warn("Конфигурация биллинга не задана. Авторизация не возможна.");
            return;
        }
//        config.setServerURL(new URL("http://10.50.0.7:91/rpc_server.php"));
        config.setServerURL(new URL("http://"+billingConf.getHost()+":"+billingConf.getPort()+"/rpc_server.php"));
        client.setConfig(config);
        Map<String, Map<String, String>> sysMap = Map.of("auth", Map.of("dname", "daemon_x"));
        argsMap.put("method", "WDaemon2");
        argsMap.put("rq", rqMap);
        argsMap.put("_Sys", sysMap);
        argsMap.put("__hostname", billingConf.getLogin());
//        argsMap.put("__hostname", "sandbox");
    }

    @Nullable
    public List<UserItemData> getUsersByLogin(String login) {

        rqMap.clear();
        rqMap.put(0, "WDaemon.php");
        rqMap.put("skey", login);
        rqMap.put("__call", "UsersByLogin");
        rqMap.put("__ip", billingConf.getSelfIp());
        rqMap.put("__person", "root:"+billingConf.getSelfIp());
//        rqMap.put("uname_live", 1);
        calculateSign();

        try {
            Map<String, Object> execute = (Map<String, Object>) client.execute("daemons.*", Collections.singletonList(argsMap));
            Map<String, String> answer = (Map<String, String>) execute.get("answer");
            Map<String, UserItemData> xdata = (Map<String, UserItemData>) execute.get("xdata");
            return List.copyOf(xdata.values());
        } catch (ClassCastException e) {
            log.warn("Не найдено");
        } catch (XmlRpcException e) {
            throw new EntryNotFound("Ошибка запроса в XMLRPC");
        } catch (Exception e){
            throw new EmptyResponse("Пустой ответ от биллинга");
        }
        return null;
    }

    @Nullable
    public List<UserItemData> getUsersByFio(String query) {

        rqMap.clear();
        rqMap.put(0, "WDaemon.php");
        rqMap.put("skey", query);
        rqMap.put("__call", "UsersByFio");
        rqMap.put("__ip", billingConf.getSelfIp());
        rqMap.put("__person", "root:"+billingConf.getSelfIp());
//        rqMap.put("uname_live", 1);
        calculateSign();

        try {
            Map<String, Object> execute = (Map<String, Object>) client.execute("daemons.*", Collections.singletonList(argsMap));
            Map<String, String> answer = (Map<String, String>) execute.get("answer");
            Map<String, UserItemData> xdata = (Map<String, UserItemData>) execute.get("xdata");
            return List.copyOf(xdata.values());
        } catch (ClassCastException e) {
            log.warn("Не найдено");
        } catch (XmlRpcException e) {
            throw new EntryNotFound("Ошибка запроса в XMLRPC");
        }
        return null;
    }

    @Nullable
    public List<UserItemData> getUsersByAddress(Address address) {

        rqMap.clear();
        rqMap.put(0, "WDaemon.php");
        rqMap.put("skey", address.getBillingAddress());
        rqMap.put("__call", "UsersByAddr");
        rqMap.put("__ip", billingConf.getSelfIp());
        rqMap.put("__person", "root:"+billingConf.getSelfIp());
//        rqMap.put("uname_live", 1);
        calculateSign();

        try {
            Map<String, Object> execute = (Map<String, Object>) client.execute("daemons.*", Collections.singletonList(argsMap));
            Map<String, String> answer = (Map<String, String>) execute.get("answer");
            Map<String, UserItemData> xdata = (Map<String, UserItemData>) execute.get("xdata");

            return List.copyOf(xdata.values());
        } catch (ClassCastException e) {
            // FIXIT заглушка для поиска по адресу, если не находит адресов с дробью, ищет без неё
            rqMap.clear();
            rqMap.put(0, "WDaemon.php");
            rqMap.put("skey", address.getBillingAddress(false));
//              rqMap.put("uname_live", 1);
            rqMap.put("__call", "UsersByAddr");
            rqMap.put("__ip", "10.1.3.150");
            rqMap.put("__person", "root:10.1.3.150");
            calculateSign();
            try {
                Map<String, Object> execute = (Map<String, Object>) client.execute("daemons.*", Collections.singletonList(argsMap));
                Map<String, String> answer = (Map<String, String>) execute.get("answer");
                Map<String, UserItemData> xdata = (Map<String, UserItemData>) execute.get("xdata");
                return List.copyOf(xdata.values());
            } catch (ClassCastException e1) {
                log.warn("Не найдено");
            } catch (XmlRpcException e1) {
                throw new EntryNotFound("Ошибка запроса в XMLRPC");
            }
        } catch (XmlRpcException e) {
            throw new EntryNotFound("Ошибка запроса в XMLRPC");
        }

        return null;
    }


    public TotalUserInfo getUserInfo(String login) {
        rqMap.clear();
        rqMap.put(0, "WDaemon.php");
        rqMap.put("uname", login);
        rqMap.put("__call", "getUserInfo");
        rqMap.put("__ip", billingConf.getSelfIp());
        rqMap.put("__person", "root:"+billingConf.getSelfIp());
        calculateSign();

        try {
            Object execute = client.execute("daemons.*", Collections.singletonList(argsMap));
            return TotalUserInfo.from(execute);
        } catch (XmlRpcException e) {
            throw new EntryNotFound("Ошибка запроса в XMLRPC");
        } catch (ClassCastException e) {
            throw new EntryNotFound("Пользователь не найден");
        }
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
        if(billingConf == null || !billingConf.isFilled()) {
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
        if(!billingConf.isFilled()) throw new IllegalFields("Конфигурация не заполнена");
        this.billingConf = billingConf;
        authenticate();
        configStorage.save(billingConf);
        stompController.changeBillingConfig(billingConf);
    }

    @Getter
    @Setter
    public static class UserItemData {
        private String tarif;
        private String uname;
        private String last;
        private String phone;
        private String coment;
        private Integer utype;
        private Integer state;
        private String addr;
        private String fio;
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
}
