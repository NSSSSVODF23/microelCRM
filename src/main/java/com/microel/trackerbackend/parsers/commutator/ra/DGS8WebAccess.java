package com.microel.trackerbackend.parsers.commutator.ra;

import com.microel.trackerbackend.parsers.commutator.AbstractRemoteAccess;
import com.microel.trackerbackend.parsers.commutator.CommutatorCredentials;
import com.microel.trackerbackend.parsers.commutator.exceptions.ParsingException;
import com.microel.trackerbackend.parsers.commutator.parsers.DlinkParser;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.storage.entities.acp.commutator.PortInfo;
import com.microel.trackerbackend.storage.entities.acp.commutator.SystemInfo;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DGS8WebAccess extends CommutatorCredentials implements AbstractRemoteAccess {

    String sessionPath = null;
    Map<String, String> sessionCookie = new HashMap<>();

    public DGS8WebAccess(String ip, String login, String password) {
        super(ip, login, password);
    }

    public DGS8WebAccess(String ip) {
        super(ip);
    }

    @Override
    public void auth() {
        try {
            Connection.Response sessionPathResponse = Jsoup.connect("http://" + getIp() + "/").method(Connection.Method.GET).execute();
            Pattern sessionPathPattern = Pattern.compile("RT='/([^/]+)/';");
            Matcher sessionPathMatcher = sessionPathPattern.matcher(sessionPathResponse.body());
            if(!sessionPathMatcher.find()) throw new ParsingException("Не найден путь сессии в DGS");
            sessionPath = sessionPathMatcher.group(1);
            Document document = Jsoup.connect("http://" + getIp()+"/"+sessionPath+"/login2.htm").header("Referer","http://"+getIp()+"/").get();
            boolean isPassCrypt = document.html().contains("CryptoJS");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5PassEncoded = md.digest(getPassword().getBytes(StandardCharsets.UTF_8));
            String pass = isPassCrypt ? HexFormat.of().formatHex(md5PassEncoded) : getPassword();
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/x-www-form-urlencoded");
            headers.put("Referer", "http://"+getIp()+"/"+sessionPath+"/login2.htm");

            Connection.Response loginResponse = Jsoup.connect("http://" + getIp() + "/cgi/login.cgi")
                    .data("pass", pass)
                    .headers(headers)
                    .method(Connection.Method.POST)
                    .execute();
            Pattern cookiePattern = Pattern.compile("document.cookie='(?<cname>\\w+)=(?<cval>[^;]+);path=/'");
            BufferedInputStream bufferedInputStream = loginResponse.bodyStream();
            StringBuilder textBuilder = new StringBuilder();
            try (Reader reader = new BufferedReader(new InputStreamReader
                    (bufferedInputStream, StandardCharsets.UTF_8))) {
                int c = 0;
                while ((c = reader.read()) != -1 && !textBuilder.toString().contains("</html>")) {
                    textBuilder.append((char) c);
                }
            }
            Matcher cookieMatcher = cookiePattern.matcher(textBuilder.toString());
            while (cookieMatcher.find()) {
                sessionCookie.put(cookieMatcher.group("cname"), cookieMatcher.group("cval"));
            }
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
            Connection.Response constInfoResponse = Jsoup.connect("http://" + getIp() + "/"+sessionPath+"/DS/const.js")
                    .cookies(sessionCookie).header("Referer","http://"+getIp()+"/").method(Connection.Method.GET).ignoreContentType(true).execute();
            Connection.Response switchInfoResponse = Jsoup.connect("http://" + getIp() + "/"+sessionPath+"/DS/Switch.js")
                    .cookies(sessionCookie).header("Referer","http://"+getIp()+"/").method(Connection.Method.GET).ignoreContentType(true).execute();

            return DlinkParser.parseSIDgs8(constInfoResponse.body(), switchInfoResponse.body());
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
            Connection.Response portInfoResponse = Jsoup.connect("http://" + getIp() + "/"+sessionPath+"/DS/Port.js")
                    .cookies(sessionCookie).header("Referer","http://"+getIp()+"/").method(Connection.Method.GET).ignoreContentType(true).execute();
            Connection.Response fdbInfoResponse = Jsoup.connect("http://" + getIp() + "/"+sessionPath+"/DS/DFT.js")
                    .cookies(sessionCookie).header("Referer","http://"+getIp()+"/").method(Connection.Method.GET).ignoreContentType(true).execute();
            String fdbInfoRaw = fdbInfoResponse.body();

            Pattern fdbPagesCountPattern = Pattern.compile("var ds_TotalPage ?= ?(\\d+);");
            Matcher fdbPagesCountMatcher = fdbPagesCountPattern.matcher(fdbInfoResponse.body());
            if (fdbPagesCountMatcher.find()) {
                int fdbPagesCount = Integer.parseInt(fdbPagesCountMatcher.group(1));
                for (int index = 2; index <= fdbPagesCount; index++) {
                    Jsoup.connect("http://" + getIp() + "/cgi/changePage.cgi")
                            .cookies(sessionCookie).method(Connection.Method.POST)
                            .data("portsel", "0")
                            .data("pagenum", String.valueOf(index))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .header("Referer","http://"+getIp()+"/")
                            .ignoreContentType(true)
                            .execute();
                    fdbInfoResponse = Jsoup.connect("http://" + getIp() + "/"+sessionPath+"/DS/DFT.js")
                            .cookies(sessionCookie).header("Referer","http://"+getIp()+"/").method(Connection.Method.GET).ignoreContentType(true).execute();
                    fdbInfoRaw += fdbInfoResponse.body();
                }
            }

            return DlinkParser.parsePortsDgs8(portInfoResponse.body(), fdbInfoRaw);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            Jsoup.connect("http://" + getIp() + "/cgi/logout.cgi")
                    .cookies(sessionCookie).method(Connection.Method.POST).ignoreContentType(true).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
