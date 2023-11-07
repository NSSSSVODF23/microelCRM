package com.microel.trackerbackend.parsers.commutator;

import com.microel.trackerbackend.parsers.commutator.exceptions.TelnetConnectionException;
import io.reactivex.rxjava3.subjects.PublishSubject;
import net.sf.expectit.Expect;
import net.sf.expectit.ExpectBuilder;
import net.sf.expectit.matcher.Matcher;
import net.sf.expectit.matcher.Matchers;
import org.apache.commons.net.telnet.TelnetClient;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static net.sf.expectit.matcher.Matchers.anyOf;

public class TelnetParser {
    private TelnetClient telnetClient = new TelnetClient();
    private String ip;
    private PublishSubject<Character> outputStream = PublishSubject.create();
    @Nullable
    private Expect expect;

    private String hardwareVariant = null;

    public TelnetParser() {
//        Instant start = Instant.now();
//        outputStream.subscribe(data -> {
//            System.out.println("Time: " + Duration.between(start, Instant.now()).toMillis() + "Received: " + data);
//        });
    }

    public String getHardwareVariant() {
        return hardwareVariant;
    }

    public Boolean isHardwareVariant(String hardwareVariant) {
        return Objects.equals(this.hardwareVariant, hardwareVariant);
    }

    public void connect(String ip) {
        try {
            telnetClient.connect(ip);
            this.ip = ip;
        } catch (IOException e) {
            throw new TelnetConnectionException("Не удалось подключиться к " + ip);
        }
    }

    public void close() {
        try {
            telnetClient.disconnect();
        } catch (IOException e) {
            throw new TelnetConnectionException("Соединение уже сброшено " + ip);
        }
        try {
            expect.close();
        } catch (IOException | NullPointerException e) {
            throw new TelnetConnectionException("Соединение уже сброшено " + ip);
        }
    }

    public Expect send(String s) {
        if (expect == null) throw new TelnetConnectionException("Не установлен слушатель telnet " + ip);
        try {
            return expect.send(s);
        } catch (IOException e) {
            close();
            throw new TelnetConnectionException("Не удалось отправить текст " + s + " на коммутатор " + ip);
        }
    }

    public Expect sendCommand(String command) {
        if (expect == null) throw new TelnetConnectionException("Не установлен слушатель telnet " + ip);
        try {
            return expect.sendLine(command);
        } catch (IOException e) {
            close();
            throw new TelnetConnectionException("Не удалось отправить комманду " + command + " на коммутатор " + ip);
        }
    }

    public void listen(String... checkInitialData) {
        try {
            expect = new ExpectBuilder()
                    .withInputs(telnetClient.getInputStream())
                    .withOutput(telnetClient.getOutputStream())
                    .withTimeout(30, TimeUnit.SECONDS)
                    .withExceptionOnFailure()
                    .build();
        } catch (IOException e) {
            close();
            throw new TelnetConnectionException("Ошибка чтения потока ввода/вывода" + ip);
        }
        try {
            expect.expect(anyOf(Arrays.stream(checkInitialData).map(Matchers::regexp).toArray(Matcher[]::new)));
        } catch (IOException e) {
            close();
            throw new TelnetConnectionException("Не верный тип оборудования " + ip);
        }
    }
}
