package com.microel.trackerbackend.parsers.commutator.ra;

import com.microel.trackerbackend.parsers.commutator.AbstractRemoteAccess;
import com.microel.trackerbackend.parsers.commutator.CommutatorCredentials;
import com.microel.trackerbackend.parsers.commutator.exceptions.ParsingException;
import com.microel.trackerbackend.parsers.commutator.parsers.DlinkParser;
import com.microel.trackerbackend.storage.entities.acp.commutator.PortInfo;
import com.microel.trackerbackend.storage.entities.acp.commutator.SystemInfo;
import org.apache.commons.codec.digest.Md5Crypt;
import org.apache.tomcat.util.security.MD5Encoder;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DES16WebAccess extends CommutatorCredentials implements AbstractRemoteAccess {

    Map<String, String> sessionCookie = new HashMap<>();

    public DES16WebAccess(String ip, String login, String password) {
        super(ip, login, password);
    }

    public DES16WebAccess(String ip) {
        super(ip);
    }

    @Override
    public void auth() {
        try {
            Document document = Jsoup.connect("http://" + getIp()+"/login2.htm").get();
            Element challengeElement = document.select("input[name=Challenge]").first();
            if(challengeElement == null) {
                throw new ParsingException("Не найдена соль для авторизации");
            }
            String challenge = challengeElement.attr("value");
            boolean isPassCrypt = document.body().html().contains("CryptoJS");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5PassEncoded = md.digest(getPassword().getBytes(StandardCharsets.UTF_8));
            String pass = isPassCrypt ? HexFormat.of().formatHex(md5PassEncoded) : getPassword();
            Connection.Response loginResponse = Jsoup.connect("http://" + getIp() + "/cgi/login.cgi?pass=" + pass + "&Challenge=" + challenge).method(Connection.Method.GET).execute();
            sessionCookie = loginResponse.cookies();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SystemInfo getSystemInfo() {
        if(sessionCookie.isEmpty()){
            throw new RuntimeException("Попытка не авторизованного запроса");
        }
        try {
            Connection.Response deviceInfoResponse = Jsoup.connect("http://" + getIp() + "/Device.js")
                    .cookies(sessionCookie).method(Connection.Method.GET).ignoreContentType(true).execute();

            return DlinkParser.parseSIDes16(deviceInfoResponse.body());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<PortInfo> getPorts() {
        if (sessionCookie.isEmpty()) {
            throw new RuntimeException("Попытка не авторизованного запроса");
        }
        try {
            Connection.Response portInfoResponse = Jsoup.connect("http://" + getIp() + "/PortSetting.js")
                    .cookies(sessionCookie).method(Connection.Method.GET).ignoreContentType(true).execute();
            Connection.Response fdbInfoResponse = Jsoup.connect("http://" + getIp() + "/DFD.js")
                    .cookies(sessionCookie).method(Connection.Method.GET).ignoreContentType(true).execute();
            String fdbInfoRaw = fdbInfoResponse.body();

            Pattern fdbPagesCountPattern = Pattern.compile("TotalPage = (\\d+);");
            Matcher fdbPagesCountMatcher = fdbPagesCountPattern.matcher(fdbInfoResponse.body());
            if (fdbPagesCountMatcher.find()) {
                int fdbPagesCount = Integer.parseInt(fdbPagesCountMatcher.group(1));
                for (int index = 2; index <= fdbPagesCount; index++) {
                    Jsoup.connect("http://" + getIp() + "/cgi/changePage.cgi?pagenum=" + index + "&selectpagenum=" + index)
                            .cookies(sessionCookie).method(Connection.Method.GET).ignoreContentType(true).execute();
                    fdbInfoResponse = Jsoup.connect("http://" + getIp() + "/DFD.js")
                            .cookies(sessionCookie).method(Connection.Method.GET).ignoreContentType(true).execute();
                    fdbInfoRaw += fdbInfoResponse.body();
                }
            }

            return DlinkParser.parsePortsDes16(portInfoResponse.body(), fdbInfoRaw);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {

    }
}
