package com.microel.trackerbackend.controllers.telegram;

import org.springframework.lang.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CallbackData {
    private String prefix;
    private String data;

    public CallbackData(String prefix, String data) {
        this.prefix = prefix;
        this.data = data;
    }


    public static String create(String prefix, String data){
    	return "#"+prefix + ":" + data;
    }

    public static String create(String prefix, String... data){
    	return "#"+prefix + ":" + String.join(",", data);
    }

    public static CallbackData parse(String callbackData) throws IllegalArgumentException{
        Pattern pattern = Pattern.compile("#(?<prefix>[\\w_]+):(?<data>[\\s\\S]+)");
        Pattern patternWithoutData = Pattern.compile("#(?<prefix>[\\w_]+)");
        Matcher matcher = pattern.matcher(callbackData);
        Matcher matcherWithoutData = patternWithoutData.matcher(callbackData);
        if(matcher.find()){
            return new CallbackData(matcher.group("prefix"), matcher.group("data"));
        } else if (matcherWithoutData.find()) {
            return new CallbackData(matcherWithoutData.group("prefix"), null);
        }
        throw new IllegalArgumentException("Не верные данные для парсинга CallbackData");
    }

    public Boolean isPrefix(String prefix){
        return this.prefix.equals(prefix);
    }

    public Boolean isData(String data){
        return this.data.equals(data);
    }

    public String getString(){
        return data;
    }

    @Nullable
    public Integer getInt(){
        try {
            return Integer.parseInt(data);
        }catch (NumberFormatException e){
            return null;
        }
    }

    @Nullable
    public Long getLong(){
        try {
            return Long.parseLong(data);
        }catch (NumberFormatException e){
            return null;
        }
    }

    @Nullable
    public Float getFloat(){
        try {
            return Float.parseFloat(data);
        }catch (NumberFormatException e){
            return null;
        }
    }

    @Nullable
    public Double getDouble(){
        try {
            return Double.parseDouble(data);
        }catch (NumberFormatException e){
            return null;
        }
    }

    @Nullable
    public Boolean getBoolean(){
        return Boolean.parseBoolean(data);
    }

    @Nullable
    public List<String> getList(){
        return List.of(data.split(","));
    }

    @Override
    public String toString() {
        return "#"+prefix + ":" + data;
    }
}
