package com.microel.trackerbackend.services;

import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.services.api.controllers.ProxyRemoteConnectionController;
import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.Data;
import org.apache.commons.net.telnet.EchoOptionHandler;
import org.apache.commons.net.telnet.SuppressGAOptionHandler;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TerminalTypeOptionHandler;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class RemoteTelnetService {

    private final StompController stompController;
    private final Map<String, TelnetClient> telnetSessions = new ConcurrentHashMap<>();
    private final Map<String, OutputStream> telnetOutput = new ConcurrentHashMap<>();

    public RemoteTelnetService(StompController stompController) {
        this.stompController = stompController;
    }

    public void createTelnetSession(String ip, String sessionId, UUID stompSessionId, String stompSubId) {
        if (telnetOutput.containsKey(sessionId)) return;

        TelnetClient telnetClient = new TelnetClient();
        telnetClient.setConnectTimeout(10000);
        TerminalTypeOptionHandler ttopt = new TerminalTypeOptionHandler("xterm", true, true, false, false);
        EchoOptionHandler echoopt = new EchoOptionHandler(false, false, true, true);
        SuppressGAOptionHandler gaopt = new SuppressGAOptionHandler(false, false, true, true);
        try {
            telnetClient.addOptionHandler(ttopt);
            telnetClient.addOptionHandler(echoopt);
            telnetClient.addOptionHandler(gaopt);
            telnetClient.connect(ip);
        } catch (Exception e) {
            stompController.outputTelnetStream(OutputFrame.connectionError(), ip, sessionId);
            return;
        }
        telnetSessions.put(stompSessionId + stompSubId, telnetClient);
        OutputStream outputStream = telnetClient.getOutputStream();
        telnetOutput.put(sessionId, outputStream);

        ExecutorService threadExecutor = Executors.newSingleThreadExecutor();
        threadExecutor.execute(() -> {
            Thread.currentThread().setName("TELNET_RECEIVER_"+ip);
            InputStream inputStream = telnetClient.getInputStream();
            byte[] buffer = new byte[1024];
            int i;
            try (inputStream) {
                while ((i = inputStream.read(buffer)) != -1) {
                    byte[] bi = Arrays.copyOfRange(buffer, 0, i);
                    stompController.outputTelnetStream(OutputFrame.of(bi), ip, sessionId);
                }
            } catch (IOException e) {
                System.out.println("Ошибка отправки данных на удаленный хост");
//                throw new RuntimeException(e);
            }/*finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }*/
            stompController.outputTelnetStream(OutputFrame.disconnect(), ip, sessionId);
            telnetSessions.remove(stompSessionId + stompSubId);
            telnetOutput.remove(sessionId);
            System.out.println("End thread for "+ip+" sessionId "+sessionId+" stompSessionId "+stompSessionId+" stompSubId "+stompSubId);
        });

        ExecutorService autoLoginExecutor = Executors.newSingleThreadExecutor();

        autoLoginExecutor.execute(() -> {
            Thread.currentThread().setName("AUTO_LOGIN_"+ip);
            try {
                Thread.sleep(1000);
                final String LOGIN = "admin\n";
                final String PASSWORD = "gjkjcfnsq\n";
                outputStream.write(LOGIN.getBytes());
                outputStream.write(PASSWORD.getBytes());
                outputStream.flush();
            } catch (Exception ignored) {
            }
        });
        autoLoginExecutor.shutdown();

        threadExecutor.shutdown();
    }

    public void sendData(String data, String sessionId) {
        OutputStream outputStream = telnetOutput.get(sessionId);
        if (outputStream == null)
            throw new ResponseException("Telnet сессия не найдена");
        try {
            outputStream.write(data.getBytes());
            outputStream.flush();
        } catch (IOException e) {
            throw new ResponseException("Не удалось отправить данные на удаленный хост");
        }
    }

    public void releaseTelnetSession(String stompSubscriptionId) {
        TelnetClient telnetClient = telnetSessions.get(stompSubscriptionId);
        if (telnetClient == null || !telnetClient.isConnected())
            return;
        try {
            telnetClient.disconnect();
        } catch (IOException e) {
            throw new ResponseException("Не удалось закрыть соединение с удаленным хостом");
        }
        telnetOutput.entrySet().stream()
                .filter(entry -> entry.getValue().equals(telnetClient.getOutputStream()))
                .map(Map.Entry::getKey)
                .findFirst().ifPresent(telnetOutput::remove);
        telnetSessions.remove(stompSubscriptionId);
    }

    public void releaseTelnetSessionBySessionId(String sessionId) {
        telnetSessions.keySet().stream().filter(key->key.contains(sessionId)).forEach(this::releaseTelnetSession);
    }

    public void connectMessage(ProxyRemoteConnectionController.ConnectionCredentials credentials, Employee employee) {
        this.stompController.telnetConnectionMessage(credentials, employee);
    }

    @Data
    public static class OutputFrame {
        private String data;
        private String state;

        public static OutputFrame of(byte[] bytes) {
            OutputFrame frame = new OutputFrame();
            frame.setData(new String(bytes, StandardCharsets.UTF_8));
            frame.setState("ok");
            return frame;
        }

        public static OutputFrame connectionError() {
            OutputFrame frame = new OutputFrame();
            frame.setData("\nНе удалось подключится к удаленному хосту");
            frame.setState("error");
            return frame;
        }

        public static OutputFrame disconnect() {
            OutputFrame frame = new OutputFrame();
            frame.setData("\nСоединение разорвано");
            frame.setState("disconnect");
            return frame;
        }
    }
}
