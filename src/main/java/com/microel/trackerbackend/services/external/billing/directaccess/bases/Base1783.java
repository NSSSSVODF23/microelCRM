package com.microel.trackerbackend.services.external.billing.directaccess.bases;

import com.microel.billing.transport.dataobject.EditUserForm;
import com.microel.trackerbackend.modules.transport.DateRange;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.api.controllers.BillingRequestController;
import com.microel.trackerbackend.services.external.billing.directaccess.DirectBaseAccess;
import com.microel.trackerbackend.services.external.billing.directaccess.DirectBaseSession;
import com.microel.trackerbackend.services.external.billing.directaccess.Request;
import com.microel.trackerbackend.storage.entities.team.util.Credentials;
import lombok.Data;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import javax.validation.constraints.NotBlank;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Base1783 extends DirectBaseSession implements DirectBaseAccess {

    private Base1783(Credentials credentials) {
        super("http://10.50.0.17:83", credentials);
    }

    public static Base1783 create(Credentials credentials) {
        return new Base1783(credentials);
    }

    @Override
    public void login() {
        try {
            Connection.Response response = request(
                    Request.ofBody(
                            "auth/check",
                            Map.of(
                                    "_auth_login", getCredentials().getUsername(),
                                    "_auth_pw", getCredentials().getPassword()),
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
            request(Request.of("auth/logout"));
        } catch (IOException e) {
            throw new ResponseException("Ошибка при выходе " + getHost());
        }
    }

    public UserInfo selectTargetUser(@NotBlank String login) {
        try {
            Connection.Response setRoleResponse = request(
                    Request.of("main/set_role/role/admin/rolename/Админы")
            );
            authSuccessfulCheck(setRoleResponse);
            Connection.Response response = request(
                    Request.of("oldstat/show_user/rq.uname/"+login)
            );
            return UserInfo.from(response.bufferUp().parse());
        } catch (IOException e) {
            throw new ResponseException("Не удалось выбрать целевого пользователя");
        }
    }

    public String getUserPassword(@NotBlank String login) {
        try {
            Connection.Response response = request(
                    Request.of("oldstat/show_logins/rq.uname/"+login)
            );
            Document document = response.bufferUp().parse();
            if(document.body().children().isEmpty() || document.body().text().contains("Произошел ТРАП на стороне сервера"))
                throw new ResponseException("Абонент не найден");

            Element passwordElement = document.selectFirst("#show_user > table:nth-child(9) > tbody > tr:nth-child(2) > td:nth-child(3)");

            if(passwordElement == null)
                throw new ResponseException("Не удалось получить пароль абонента");

            return passwordElement.text();
        } catch (IOException e) {
            throw new ResponseException("Ошибка получения пароля абонента");
        }
    }

    public void changeAddress(@NotBlank String login, @NotBlank String address) {
        UserInfo userInfo = selectTargetUser(login);
        UserInfo changedUserInfo = userInfo.copy();
        changedUserInfo.setAddress(address);
        commitUserInfoChanges(userInfo, changedUserInfo);
    }

    public void changeFullName(@NotBlank String login, @NotBlank String fullName) {
        UserInfo userInfo = selectTargetUser(login);
        UserInfo changedUserInfo = userInfo.copy();
        changedUserInfo.setFullName(fullName);
        commitUserInfoChanges(userInfo, changedUserInfo);
    }

    public void changePhone(@NotBlank String login, @NotBlank String phone) {
        UserInfo userInfo = selectTargetUser(login);
        UserInfo changedUserInfo = userInfo.copy();
        changedUserInfo.setPhone(phone);
        commitUserInfoChanges(userInfo, changedUserInfo);
    }

    public void changeComment(@NotBlank String login, @NotBlank String comment) {
        UserInfo userInfo = selectTargetUser(login);
        UserInfo changedUserInfo = userInfo.copy();
        changedUserInfo.setComment(comment);
        commitUserInfoChanges(userInfo, changedUserInfo);
    }

    public void commitUserInfoChanges(UserInfo oldUserInfo, UserInfo newUserInfo) {
        try {
            Connection.Response response = request(
                    Request.ofBody(
                            "ajax_c.php",
                            oldUserInfo.toRequestBody(newUserInfo),
                            Connection.Method.POST
                    )
            );
            authSuccessfulCheck(response);
        } catch (IOException e) {
            throw new ResponseException("Не удалось изменить данные абонента");
        }
    }

    public Page<LogItem> getLogs(LogsForm form) {
        try {
            Connection.Response setRoleResponse = request(
                    Request.of("main/set_role/role/admin/rolename/Админы")
            );
//            Connection.Response response = request(
//                    Request.ofBody(
//                            "ajax_c.php",
//                            form.toRequestBody(),
//                            Connection.Method.POST
//                    )
//            );
            Connection.Response tableResponse = request(
                    Request.ofBody(
                            "oldstat/stat",
                            form.toRequestBody(),
                            Connection.Method.POST
                    )
            );
//            authSuccessfulCheck(response);
//            Connection.Response tableResponse = request(
//                    Request.of(
//                            "oldstat/stat"
//                    )
//            );
            authSuccessfulCheck(tableResponse);
            Document document = tableResponse.bufferUp().parse();
            Element totalElement = document.selectFirst("#stat > pre");
            Elements hDateColumn = document.select("#stat > table:nth-child(3) > tbody > tr > td:nth-child(3)");
            Elements hTimeColumn = document.select("#stat > table:nth-child(3) > tbody > tr > td:nth-child(4)");
            Elements actionColumn = document.select("#stat > table:nth-child(3) > tbody > tr > td:nth-child(8)");
            Elements descriptionColumn = document.select("#stat > table:nth-child(3) > tbody > tr > td:nth-child(10)");
            Elements amountColumn = document.select("#stat > table:nth-child(3) > tbody > tr > td:nth-child(12)");
            Elements balanceColumn = document.select("#stat > table:nth-child(3) > tbody > tr > td:nth-child(13)");

            Pattern pattern = Pattern.compile("Всего строк (\\d+),");
            Matcher matcher = pattern.matcher(totalElement != null ? totalElement.text() : "");
            int totalLogs = 0;
            if(matcher.find())
                totalLogs = Integer.parseInt(matcher.group(1));

            List<LogItem> logItems = new ArrayList<>();
            for(int i = 0; i < hDateColumn.size(); i++){
                logItems.add(LogItem.of(
                        hDateColumn.get(i).text(),
                        hTimeColumn.get(i).text(),
                        actionColumn.get(i).text(),
                        descriptionColumn.get(i).text(),
                        amountColumn.get(i).text(),
                        balanceColumn.get(i).text()
                ));
            }
            return new PageImpl<>(logItems, PageRequest.of(form.getPage(), form.getPlen()), totalLogs);
        } catch (IOException e) {
            throw new ResponseException("Не удалось получить логи абонента");
        }
    }

    private void authSuccessfulCheck(Connection.Response response) throws IOException {
        Document document = response.bufferUp().parse();
        if (document.body().text().isEmpty() || document.body().text().contains("Код ошибки")) {
            throw new ResponseException("Не авторизованный запрос");
        }
    }

    public void userEdit(String login, EditUserForm form) {
        UserInfo userInfo = selectTargetUser(login);
        UserInfo changedUserInfo = userInfo.copy();
        changedUserInfo.setAddress(form.getAddress());
        changedUserInfo.setFullName(form.getFullName());
        changedUserInfo.setPhone(form.getPhone());
        changedUserInfo.setComment(form.getComment());
        commitUserInfoChanges(userInfo, changedUserInfo);
    }

    @Data
    public static class UserInfo{
        private String login;
        private String address;
        private String fullName;
        private String phone;
        private String state;
        private String comment;

        public static UserInfo from(Document document) {
            if(document.body().children().isEmpty() || document.body().text().contains("Произошел ТРАП на стороне сервера"))
                throw new ResponseException("Абонент не найден");

            Element loginElement = document.selectFirst("#show_user > table > tbody > tr:nth-child(1) > td:nth-child(2)");
            Element addressElement = document.selectFirst("#show_user > table > tbody > tr:nth-child(2) > td:nth-child(2) > input[type=text]");
            Element fullNameElement = document.selectFirst("#show_user > table > tbody > tr:nth-child(3) > td:nth-child(2) > input[type=text]");
            Element phoneElement = document.selectFirst("#show_user > table > tbody > tr:nth-child(4) > td:nth-child(2) > input[type=text]");
            Element stateElement = document.selectFirst("#show_user > table > tbody > tr:nth-child(5) > td:nth-child(2) > input[type=text]");
            Element commentElement = document.selectFirst("#show_user > table > tbody > tr:nth-child(7) > td:nth-child(2) > input[type=text]");

            if(loginElement == null || addressElement == null ||
                    fullNameElement == null || phoneElement == null ||
                    commentElement == null || stateElement == null)
                throw new ResponseException("Не удалось получить данные абонента");

            UserInfo userInfo = new UserInfo();

            userInfo.setLogin(loginElement.text());
            userInfo.setAddress(addressElement.val());
            userInfo.setFullName(fullNameElement.val());
            userInfo.setPhone(phoneElement.val());
            userInfo.setState(stateElement.val());
            userInfo.setComment(commentElement.val());

            return userInfo;
        }

        public UserInfo copy(){
            UserInfo newUserInfo = new UserInfo();
            newUserInfo.setLogin(login);
            newUserInfo.setAddress(address);
            newUserInfo.setFullName(fullName);
            newUserInfo.setPhone(phone);
            newUserInfo.setState(state);
            newUserInfo.setComment(comment);
            return newUserInfo;
        }

        public Map<String, String> toRequestBody(UserInfo changes){
            Map<String, String> body = new HashMap<>();

            body.put("rq.addr", changes.getAddress());
            body.put("rq.fio", changes.getFullName());
            body.put("rq.phone", changes.getPhone());
            body.put("rq.state", changes.getState());
            body.put("rq.coment", changes.getComment());

            body.put("rq.old_addr", address);
            body.put("rq.old_state", state);
            body.put("rq.old_coment", comment);
            body.put("rq.old_fio", fullName);
            body.put("rq.old_phone", phone);
            body.put("rq.uname", login);

            body.put("__act", "oldstat-SaveEditUser");
            return body;
        }
    }

    @Data
    public static class LogsForm {
        private DateRange dateRange;
        private Integer page;
        private Integer plen;
        private String login;

        public Map<String, String> toRequestBody(){
            Map<String, String> body = new HashMap<>();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            body.put("period.hdate", sdf.format(dateRange.start()));
            body.put("period.edate", sdf.format(dateRange.end()));
            body.put("filtr.__page", String.valueOf(page+1));
            body.put("filtr.__plen", plen.toString());
            body.put("filtr.__filtr_name", "stat");
            body.put("filtr.login", login);
            body.put("filtr._sel_login", "0");
            body.put("filtr._sel_hdate", "0");
            body.put("filtr._sel_htime", "0");
            body.put("filtr._sel_edate", "0");
            body.put("filtr._sel_etime", "0");
            body.put("filtr.service", "");
            body.put("filtr.cause", "0");
            body.put("filtr._sel_cid", "0");
            body.put("filtr.ip", "");
            body.put("__act", "oldstat-stat");

            return body;
        }
    }

    @Data
    public static class LogItem {
        private Timestamp timestamp;
        private String action;
        private String description;
        private Float amount;
        private Float balance;

        public static LogItem of(String date, String time, String action, String description, String amount, String balance) {
            LogItem logItem = new LogItem();
            logItem.setTimestamp(Timestamp.valueOf(date + " " + time));
            logItem.setAction(action);
            logItem.setDescription(description);
            logItem.setAmount(Float.parseFloat(amount));
            logItem.setBalance(Float.parseFloat(balance));
            return logItem;
        }
    }

}
