package com.microel.trackerbackend.services.external.billing.directaccess.bases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.external.billing.directaccess.DirectBaseAccess;
import com.microel.trackerbackend.services.external.billing.directaccess.DirectBaseSession;
import com.microel.trackerbackend.services.external.billing.directaccess.Request;
import com.microel.trackerbackend.services.external.billing.directaccess.Url;
import com.microel.trackerbackend.storage.entities.address.Address;
import com.microel.trackerbackend.storage.entities.team.util.Credentials;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.validation.constraints.NotBlank;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Base781 extends DirectBaseSession implements DirectBaseAccess {

    private Base781(Credentials credentials) {
        super("http://10.50.0.7:81", credentials);
    }

    @Override
    public void login() {
        try {
            Connection.Response response = request(
                    Request.ofBody(
                            "auth/check",
                            Map.of("_auth_login", getCredentials().getUsername(), "_auth_pw", getCredentials().getPassword()),
                            Connection.Method.POST
                    )
            );
            authSuccessfulCheck(response);
        } catch (HttpStatusException ignore) {
//            throw new ResponseException("Ошибка авторизации в " + getHost() + " " + getCredentials());
        } catch (IOException e) {
            throw new ResponseException("Ошибка подключения к " + getHost());
        }
    }

    private void selectUser(@NotBlank String login){
        try {
            request(Request.of("main/set_role/role/office/rolename/%D0%9E%D1%84%D0%B8%D1%81?"));
            request(Request.of("user_info/start"));
            Connection.Response response = request(Request.of("user_info/show_user", Map.of("rq.uname", login), Connection.Method.GET));
        } catch (IOException e) {
            throw new ResponseException("Ошибка при выборе абонента " + getHost());
        }
    }

    public void makePayment(@NotBlank String login, @NonNull PaymentForm form){
        selectUser(login);
        try {
            Connection.Response response = request(Request.ofBody("ajax_c.php", form.toRequestBody(), Connection.Method.POST));
            String errorMessage = getAjaxErrorMessage(response.body());
            if(errorMessage != null)
                throw new ResponseException(errorMessage);

            ObjectMapper mapper = new ObjectMapper();
            PaymentResponse paymentResponse = mapper.readValue(response.body(), PaymentResponse.class);

            Connection.Response checkIssuanceResponse = request(Request.ofBody("ajax_c.php", paymentResponse.toRequestBody(), Connection.Method.POST));
            errorMessage = getAjaxErrorMessage(checkIssuanceResponse.body());
            if(errorMessage != null)
                throw new ResponseException(errorMessage);
        } catch (IOException e) {
            throw new ResponseException("Ошибка при совершении платежа " + getHost());
        }
    }

    @Override
    public void logout() {
        try {
            request(Request.of("auth/logout"));
        } catch (IOException e) {
            throw new ResponseException("Ошибка при выходе " + getHost());
        }
    }

    private String getAjaxErrorMessage(String body){
        Pattern pattern = Pattern.compile("\\\"msg\\\":\\\"([^\\\"]+)");
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? utfConvert(matcher.group(1)) : null;
    }

    private String utfConvert(String str){
        return StringEscapeUtils.unescapeJava(str);
    }

    private void authSuccessfulCheck(Connection.Response response) throws IOException {
        Document document = response.bufferUp().parse();
        if(document.body().children().isEmpty() || document.body().text().contains("Авторизация завершилась с ошибкой")){
            throw new ResponseException("Не авторизованный запрос " + getHost() + " " + getCredentials());
        }
    }

    public static Base781 create(Credentials credentials) {
        return new Base781(credentials);
    }

    @Data
    public static class PaymentResponse{
        private String state;
        private String __msg;
        private KkmInfo kkm;
        @Data
        public static class KkmInfo{
            private String manager;
            private Integer cashin;
            private List<SalesInfoItem> sales;
            private XObject x;

            @Data
            public static class SalesInfoItem{
                private String product;
                private Integer ammount;
                private Integer price;
                private Integer group;
            }

            @Data
            public static class XObject{
                private Integer delivery;
                private Integer ptype;
                private String uname;
                private String tarif;
                private Integer sum;
                private Integer cashin;
                private String kkm_state;
                private Integer tid;
                private Integer seid;
            }
        }

        public Map<String, String> toRequestBody(){
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("kkm_state", kkm.x.kkm_state);
            requestBody.put("manager", kkm.manager);
            requestBody.put("sum_kkm", kkm.x.sum.toString());
            requestBody.put("cashin", kkm.x.cashin.toString());
            requestBody.put("__act", "kkm-saleKKM");
            return requestBody;
        }
    }

    @Getter
    @Setter
    public static class PaymentForm{
        @NonNull
        private Integer paymentType;
        @NonNull
        private Integer sum;
        @NotBlank
        private String comment;

        public Map<String, String> toRequestBody(){
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("rq.sum", sum.toString());
//            requestBody.put("rq.cashin", null);
            requestBody.put("rq.paytype", paymentType.toString());
            requestBody.put("rq.coment", comment);
//            requestBody.put("rq.msg", null);
            requestBody.put("__act", "user_info-regPay");
            return requestBody;
        }
    }

}
