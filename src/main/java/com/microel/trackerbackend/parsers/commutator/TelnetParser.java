package com.microel.trackerbackend.parsers.commutator;

import com.microel.trackerbackend.parsers.commutator.exceptions.TelnetConnectionException;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.apache.commons.net.telnet.TelnetClient;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TelnetParser {
    private TelnetClient telnetClient = new TelnetClient();
    private String ip;
    private PublishSubject<Character> outputStream = PublishSubject.create();
    private String hardwareVariant = null;

    public String getHardwareVariant() {
        return hardwareVariant;
    }

    public Boolean isHardwareVariant(String hardwareVariant) {
        return Objects.equals(this.hardwareVariant, hardwareVariant);
    }

    public TelnetParser() {
//        Instant start = Instant.now();
//        outputStream.subscribe(data -> {
//            System.out.println("Time: " + Duration.between(start, Instant.now()).toMillis() + "Received: " + data);
//        });
    }

    private Observable<String> getInputBufferStream() {
        return outputStream.buffer(outputStream.debounce(500L, TimeUnit.MILLISECONDS))
                .map(arr -> arr.stream().map(String::valueOf).collect(Collectors.joining())).take(1);
    }

    private Observable<String> getInputBufferStream(String until) {
        Observable<String> observable = outputStream.buffer(outputStream.debounce(500L, TimeUnit.MILLISECONDS))
                .map(arr -> arr.stream().map(String::valueOf).collect(Collectors.joining()));
        Observable<String> filter = observable.filter(s -> {
                    Pattern pattern = Pattern.compile(until);
                    return pattern.matcher(s).find();
                });
        return observable.buffer(filter).map(arr -> arr.stream().map(String::valueOf).collect(Collectors.joining())).take(1);
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
    }

    private void writeCommand(String command) {
        try {
            telnetClient.getOutputStream().write(command.getBytes());
            telnetClient.getOutputStream().flush();
        } catch (IOException e) {
            close();
            throw new TelnetConnectionException("Не удалось подключиться к " + ip);
        }
    }

    public String sendCommand(String command) {
        return send(command + "\r\n");
    }

    public String sendCommand(String command, String wait) {
        Future<String> commandResult = getInputBufferStream(wait).toFuture();
        writeCommand(command + "\r\n");
        try {
            return commandResult.get();
        } catch (InterruptedException e) {
            close();
            throw new TelnetConnectionException("Не удалось считать ответ на команду " + command + " поток прерван.");
        } catch (ExecutionException e) {
            close();
            throw new TelnetConnectionException("Не удалось считать ответ на команду " + command + " ошибка в потоке: " + e.getMessage());
        }
    }

    public String send(String command) {
        Future<String> commandResult = getInputBufferStream().toFuture();
        writeCommand(command);
        try {
            return commandResult.get();
        } catch (InterruptedException e) {
            close();
            throw new TelnetConnectionException("Не удалось считать ответ на команду " + command + " поток прерван.");
        } catch (ExecutionException e) {
            close();
            throw new TelnetConnectionException("Не удалось считать ответ на команду " + command + " ошибка в потоке.");
        }
    }

    public void listen(String... checkInitialData) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try (Reader reader = new InputStreamReader(telnetClient.getInputStream())) {
                int r;
                while ((r = reader.read()) != -1) {
                    char ch = (char) r;
                    outputStream.onNext(ch);
                }
            } catch (IOException e) {
                close();
                throw new TelnetConnectionException("Не удалось подключиться к " + ip);
            }
        });
        try {
            String data = getInputBufferStream().toFuture().get();
            for (String initRegExp : checkInitialData) {
                Pattern dataPattern = Pattern.compile(initRegExp);
                if (dataPattern.matcher(data).find()) {
                    hardwareVariant = initRegExp;
                    return;
                }
            }
            close();
            throw new TelnetConnectionException("Не верный тип оборудования " + ip);
        } catch (ExecutionException e) {
            close();
            throw new TelnetConnectionException("Не удалось запустить поток считывания " + ip);
        } catch (InterruptedException e) {
            close();
            throw new TelnetConnectionException("Поток считывания прерван " + ip);
        }
    }
}
