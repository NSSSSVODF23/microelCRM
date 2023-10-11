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

//    private Observable<String> getInputBufferStream() {
//        return outputStream.buffer(outputStream.debounce(500L, TimeUnit.MILLISECONDS))
//                .map(arr -> arr.stream().map(String::valueOf).collect(Collectors.joining())).take(1);
//    }
//
//    private Observable<String> getInputBufferStream(String until) {
//        Observable<String> observable = outputStream.buffer(outputStream.debounce(500L, TimeUnit.MILLISECONDS))
//                .map(arr -> arr.stream().map(String::valueOf).collect(Collectors.joining())).map(s->s.substring(s.indexOf('\n')+1));
//        Observable<String> filter = observable.filter(s -> {
//                    Pattern pattern = Pattern.compile(until);
//                    return pattern.matcher(s).find();
//                }).take(1);
//        return observable.buffer(filter).map(arr -> arr.stream().map(String::valueOf).collect(Collectors.joining())).doOnNext(s -> System.out.print("\""+s+"\"")).take(1);
//    }

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

//    private void writeCommand(String command) {
//        try {
//            telnetClient.getOutputStream().write(command.getBytes());
//            telnetClient.getOutputStream().flush();
//        } catch (IOException e) {
//            close();
//            throw new TelnetConnectionException("Не удалось подключиться к " + ip);
//        }
//    }
//
//    public String sendCommand(String command) {
//        return send(command + "\r\n");
//    }
//
//    public String sendCommand(String command, String wait) {
//        Future<String> commandResult = getInputBufferStream(wait).toFuture();
//        writeCommand(command + "\r\n");
//        try {
//            return commandResult.get(30, TimeUnit.SECONDS);
//        } catch (InterruptedException e) {
//            close();
//            throw new TelnetConnectionException("Не удалось считать ответ на команду " + command + " поток прерван.");
//        } catch (ExecutionException e) {
//            close();
//            throw new TelnetConnectionException("Не удалось считать ответ на команду " + command + " ошибка в потоке: " + e.getMessage());
//        } catch (TimeoutException e) {
//            close();
//            throw new TelnetConnectionException("Не удалось считать ответ на команду " + command + " таймаут ответа.");
//        }
//    }
//
//    public String send(String command) {
//        Future<String> commandResult = getInputBufferStream().toFuture();
//        writeCommand(command);
//        try {
//            return commandResult.get(30, TimeUnit.SECONDS);
//        } catch (InterruptedException e) {
//            close();
//            throw new TelnetConnectionException("Не удалось считать ответ на команду " + command + " поток прерван.");
//        } catch (ExecutionException e) {
//            close();
//            throw new TelnetConnectionException("Не удалось считать ответ на команду " + command + " ошибка в потоке.");
//        } catch (TimeoutException e) {
//            throw new TelnetConnectionException("Не удалось считать ответ на команду " + command + " таймаут ответа.");
//        }
//    }

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
//
//
//        Thread listenThread = new Thread(()->{
//            try (Reader reader = new InputStreamReader(telnetClient.getInputStream())) {
//                int r;
//                while ((r = reader.read()) != -1) {
//                    char ch = (char) r;
////                    System.out.print(ch);
//                    outputStream.onNext(ch);
//                }
//            } catch (IOException e) {
//                close();
//                throw new TelnetConnectionException("Не удалось подключиться к " + ip);
//            }
//        });
//        listenThread.start();
//        try {
//            String data = getInputBufferStream().toFuture().get(30,  TimeUnit.SECONDS);
//            for (String initRegExp : checkInitialData) {
//                Pattern dataPattern = Pattern.compile(initRegExp);
//                if (dataPattern.matcher(data).find()) {
//                    hardwareVariant = initRegExp;
//                    return;
//                }
//            }
//            close();
//            throw new TelnetConnectionException("Не верный тип оборудования " + ip);
//        } catch (ExecutionException e) {
//            close();
//            throw new TelnetConnectionException("Не удалось запустить поток считывания " + ip);
//        } catch (InterruptedException e) {
//            close();
//            throw new TelnetConnectionException("Поток считывания прерван " + ip);
//        } catch (TimeoutException e) {
//            close();
//            throw new TelnetConnectionException("Таймаут ответа telnet " + ip);
//        }
//    }
}
