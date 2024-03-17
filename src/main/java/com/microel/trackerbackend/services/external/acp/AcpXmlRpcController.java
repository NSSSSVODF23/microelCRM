package com.microel.trackerbackend.services.external.acp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.external.billing.ApiBillingController;
import lombok.Data;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AcpXmlRpcController {
    private final String url = "http://acp.vdonsk.ru/acp.php";
    private final XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
    private final XmlRpcClient client = new XmlRpcClient();

    public AcpXmlRpcController() throws MalformedURLException {
        config.setServerURL(new URL(url));
        client.setConfig(config);
    }

    public AcpUserBrief getUserBrief(String login){
        if(login == null)
            throw new ResponseException("Логин не может быть пустым");

        try {
            return AcpUserBriefRaw.of(client.execute("billing.getUserBrief", new Object[]{login})).toAcpUserBrief();
        } catch (XmlRpcException e) {
            return AcpUserBriefRaw.empty().toAcpUserBrief();
        }
    }

    public Map<String, AcpUserBrief> getBulkUserBrief(Set<String> logins){
        return logins.stream().collect(Collectors.toMap(login -> login, this::getUserBrief));
    }

    @Data
    public static class AcpUserBrief {
        @Nullable
        private String address;
        @JsonIgnore
        @Nullable
        private Integer state;

        public String getStatusName(){
            return ApiBillingController.getUserStatusName(state, null);
        }

        public String getStatusColor(){
            return ApiBillingController.getUserStatusColor(state, null);
        }
    }

    @Data
    public static class AcpUserBriefRaw {
        @Nullable
        private Login login;
        @Nullable
        private Account account;
        @Nullable
        private Balance balance;
        @Nullable
        private List<Tariff> tariff;

        public static AcpUserBriefRaw of(Object data){
            if(data == null)
                return null;
            AcpUserBriefRaw acpUserBrief = new AcpUserBriefRaw();
            Map<String, Object> map = (Map<String, Object>) data;
            acpUserBrief.setLogin(Login.of(map.get("LOGIN")));
            acpUserBrief.setAccount(Account.of(map.get("ACCOUNT")));
            acpUserBrief.setBalance(Balance.of(map.get("BALANCE")));
            Object[] tariffList = (Object[]) map.get("TARIF");
            if(tariffList != null)
                acpUserBrief.setTariff(Stream.of(tariffList).map(Tariff::of).collect(Collectors.toList()));
            else
                acpUserBrief.setTariff(null);
            return acpUserBrief;
        }

        public static AcpUserBriefRaw empty() {
            return new AcpUserBriefRaw();
        }

        public AcpUserBrief toAcpUserBrief(){
            AcpUserBrief acpUserBrief = new AcpUserBrief();
            if(account != null)
                acpUserBrief.setAddress(account.getAddr());
            if(login != null)
                acpUserBrief.setState(login.getState());
            return acpUserBrief;
        }

        @Data
        public static class Login{
            private String login;
            private String uname;
            private String ipaddr;
            private String lastUse;
            private String ctime;
            private Integer state;

            public static Login of(Object data){
                if(data == null)
                    return null;
                Login loginObj = new Login();
                Map<String, Object> map = (Map<String, Object>) data;
                loginObj.setLogin((String) map.get("login"));
                loginObj.setUname((String) map.get("uname"));
                loginObj.setIpaddr((String) map.get("ipaddr"));
                loginObj.setLastUse((String) map.get("lastuse"));
                loginObj.setCtime((String) map.get("ctime"));
                loginObj.setState((Integer) map.get("state"));
                return loginObj;
            }
        }
        @Data
        public static class Account{
            private String uname;
            private String utype;
            private String ndog;
            private String login;
            private String addr;
            private String fio;
            private String comment;
            private String phone;

            public static Account of(Object data){
                if(data == null)
                    return null;
                Account accountObj = new Account();
                Map<String, Object> map = (Map<String, Object>) data;
                accountObj.setUname((String) map.get("uname"));
                accountObj.setUtype((String) map.get("utype"));
                accountObj.setNdog((String) map.get("ndog"));
                accountObj.setLogin((String) map.get("login"));
                accountObj.setAddr((String) map.get("addr"));
                accountObj.setFio((String) map.get("fio"));
                accountObj.setComment((String) map.get("coment"));
                accountObj.setPhone((String) map.get("phone"));
                return accountObj;
            }
        }
        @Data
        public static class Balance{
            private String bmoney;
            private String bcredit;
            private String btraf;
            private String btime;
            private Boolean deferredPay;

            public static Balance of(Object data){
                if(data == null)
                    return null;
                Balance balanceObj = new Balance();
                Map<String, Object> map = (Map<String, Object>) data;
                balanceObj.setBmoney((String) map.get("bmoney"));
                balanceObj.setBcredit((String) map.get("bcredit"));
                balanceObj.setBtraf((String) map.get("btraf"));
                balanceObj.setBtime((String) map.get("btime"));
                if(map.get("deferred_pay") instanceof Boolean) {
                    balanceObj.setDeferredPay((Boolean) map.get("deferred_pay"));
                }else{
                    balanceObj.setDeferredPay(true);
                }
                return balanceObj;
            }
        }
        @Data
        public static class Tariff{
            private Integer serviceN;
            private String service;
            private String uname;
            private String login;
            private Integer price;
            private String hdate;
            private String edate;
            private String adate;
            private String mdate;
            private Integer state;
            private String isMain;
            private String last;
            private String stype;
            private String iExt;

            public static Tariff of(Object data){
                if(data == null)
                    return null;
                Tariff tariffObj = new Tariff();
                Map<String, Object> map = (Map<String, Object>) data;
                tariffObj.setServiceN((Integer) map.get("service_n"));
                tariffObj.setService((String) map.get("service"));
                tariffObj.setUname((String) map.get("uname"));
                tariffObj.setLogin((String) map.get("login"));
                tariffObj.setPrice((Integer) map.get("price"));
                tariffObj.setHdate((String) map.get("hdate"));
                tariffObj.setEdate((String) map.get("edate"));
                tariffObj.setAdate((String) map.get("adate"));
                tariffObj.setMdate((String) map.get("mdate"));
                tariffObj.setState((Integer) map.get("state"));
                tariffObj.setIsMain((String) map.get("is_main"));
                tariffObj.setLast((String) map.get("last"));
                tariffObj.setStype((String) map.get("stype"));
                tariffObj.setIExt((String) map.get("i_ext"));
                return tariffObj;
            }
        }
    }
}
