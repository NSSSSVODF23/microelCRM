package com.microel.trackerbackend.storage.entities.team.util;

import com.microel.trackerbackend.misc.ListItem;
import com.microel.trackerbackend.services.FilesWatchService;
import com.microel.trackerbackend.services.api.ResponseException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.PatchMapping;

import javax.persistence.*;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Getter
@Setter
@Entity
@Table(name = "phy_phone_info")
public class PhyPhoneInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long phyPhoneInfoId;
    private String ip;
    private String login;
    private String password;
    @Enumerated(EnumType.STRING)
    private PhyPhoneModel model;

    private String description;

    public ListItem toListItem(){
        return new ListItem(getDescription(), getPhyPhoneInfoId());
    }

    public static PhyPhoneInfo from(Form form) {
        PhyPhoneInfo info = new PhyPhoneInfo();
        info.setIp(form.getIp());
        info.setLogin(form.getLogin());
        info.setPassword(form.getPassword());
        info.setModel(form.getModel());
        return info;
    }

    public void throwIfIncomplete(){
        Pattern ipPattern = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
        Matcher ipMatcher = ipPattern.matcher(ip);
        if(!ipMatcher.matches()) throw new ResponseException("IP адрес телефона не верный");
        if(ip == null || ip.isBlank()) throw new ResponseException("IP адрес телефона не задан");
        if(login == null || login.isBlank()) throw new ResponseException("Логин телефона не задан");
        if(password == null || password.isBlank()) throw new ResponseException("Пароль телефона не задан");
        if(model == null) throw new ResponseException("Модель телефона не задана");
    }

    @Getter
    @Setter
    public static class Form{
        private String employeeLogin;
        private String ip;
        private String login;
        private String password;
        private PhyPhoneModel model;
    }

    public enum PhyPhoneModel {
        X1S("X1S"),
        X3S("X3S"),
        OLD("OLD");

        private final String model;

        PhyPhoneModel(String model) {
            this.model = model;
        }

        public String getLabel() {
            return switch (this) {
                case OLD -> "Старый телефон";
                case X1S -> "X1S";
                case X3S -> "X3S";
            };
        }

        public static List<Map<String,String>> getList(){
            return Stream.of(PhyPhoneModel.values()).map(value->Map.of("label", value.getLabel(), "value", value.getValue())).toList();
        }

        public String getValue() {
            return model;
        }
    }

    @Getter
    @Setter
    public class BindToEmployeeForm {
        @Nullable
        private Long phyPhoneInfoId;
    }
}
