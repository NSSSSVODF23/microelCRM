package com.microel.trackerbackend.services;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.misc.CircularQueue;
import com.microel.trackerbackend.misc.charttype.ChartData;
import com.microel.trackerbackend.misc.charttype.ChartDataSet;
import com.microel.trackerbackend.services.api.StompController;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MonitoringService {

    private final Map<InetAddress, PingMonitoringHolder> pingMonitoringMap = new ConcurrentHashMap<>();
    private final Map<InetAddress, Set<UUID>> pingSessionMap = new ConcurrentHashMap<>();
    private final StompController stompController;

    public MonitoringService(@Lazy StompController stompController) {
        this.stompController = stompController;
    }

    public Flux<PingMonitoring> getFlux(InetAddress ip) {
        return Flux
                .interval(Duration.ofSeconds(1))
                .publishOn(Schedulers.boundedElastic())
                .takeWhile((val) -> pingMonitoringMap.containsKey(ip))
                .map(i -> {
                    PingMonitoringHolder pingMonitoring = pingMonitoringMap.get(ip);
                    try {
                        Instant start = Instant.now();
                        boolean reachable = ip.isReachable(900);
                        pingMonitoring.addResponse(reachable, start, i);
                        return pingMonitoring.getPingMonitoring();
                    } catch (IOException e) {
                        pingMonitoring.addResponse(i);
                        return pingMonitoring.getPingMonitoring();
                    }
                });
    }

    public void appendPingMonitoring(String ip, UUID sessionId) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            if(!pingMonitoringMap.containsKey(address)){
                PingMonitoringHolder monitoringHolder = new PingMonitoringHolder(address, getFlux(address));
                pingMonitoringMap.put(address, monitoringHolder);
                monitoringHolder.run(stompController);
            }
            pingSessionMap.computeIfPresent(address, (k, v) -> {
                v.add(sessionId);
                return v;
            });
            pingSessionMap.putIfAbsent(address, Stream.of(sessionId).collect(Collectors.toSet()));
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public void releasePingMonitoring(UUID sessionId) {
        // find address by session id from pingSessionMap
        pingSessionMap.forEach((k, v) -> {
            if(v.contains(sessionId)){
                v.remove(sessionId);
                if(v.isEmpty()){
                    PingMonitoringHolder remove = pingMonitoringMap.remove(k);
                    pingSessionMap.remove(k);
                    if(remove != null) remove.dispose();
                }
            }
        });
    }

    @Getter
    @Setter
    @ToString
    public static class PingMonitoringHolder {
        private PingMonitoring pingMonitoring;
        private Flux<PingMonitoring> flux;
        private Disposable subscription;

        public PingMonitoringHolder(InetAddress ip, Flux<PingMonitoring> flux) {
            {
                pingMonitoring = new PingMonitoring(ip);
                this.flux = flux;
            }
        }

        public void addResponse(boolean reachable, Instant start, Long i) {
            pingMonitoring.pingResponses.add(new PingResponse(reachable, start, i));
        }

        public void addResponse(Long i) {
            pingMonitoring.pingResponses.add(new PingResponse(i));
        }

        public void run(StompController stompController) {
            subscription = flux.subscribe(stompController::pingMonitoring);
        }

        public void dispose() {
            subscription.dispose();
        }
    }

    @Getter
    @Setter
    @ToString
    public static class PingMonitoring {
        @JsonIgnore
        private InetAddress address;
        @JsonIgnore
        private CircularQueue<PingResponse> pingResponses = new CircularQueue<>(500);

        public PingMonitoring(InetAddress ip) {
            this.address = ip;
        }

        public String getIp() {
            return address.getHostAddress();
        }

        public void addResponse(PingResponse pingResponse) {
            pingResponses.add(pingResponse);
        }

        public Double getReachablePercentage() {
            return (pingResponses.stream().map(pingResponse -> pingResponse.reachable ? 1 : 0).reduce(0, Integer::sum) / Integer.valueOf(pingResponses.size()).doubleValue()) * 100;
        }

        public Double getDelayAvg() {
            return pingResponses.stream().filter(pingResponse -> pingResponse.delay != null).mapToDouble(pingResponse -> pingResponse.delay.toNanos()).average().orElse(999000000d) / 1000000d;
        }

        public Boolean getIsReachable() {
            return pingResponses.getLast().isReachable();
        }

        public ChartData getChartData(){
            List<ChartDataSet.ChartPoint> data = pingResponses.stream()
                    .skip(Math.max(0, pingResponses.size() - 50))
                    .map(pingResponse ->
                            new ChartDataSet.ChartPoint(Instant.ofEpochMilli(pingResponse.iteration),pingResponse.delay != null ? pingResponse.delay.toNanos() / 1000000d : null) )
                    .toList();
            ChartDataSet dataSet = new ChartDataSet();
            dataSet.setLabel("Ping");
            dataSet.setDataOfPoints(data);
            return new ChartData(null,List.of(dataSet));
        }
    }

    @Getter
    @Setter
    @ToString
    public static class PingResponse {
        private boolean reachable = false;
        @Nullable
        private Duration delay;
        private Long iteration;

        public PingResponse(Boolean reachable, Instant start, Long iteration) {
            this.reachable = reachable;
            this.delay = reachable  ? Duration.between(start, Instant.now()) : null;
            this.iteration = iteration;
        }

        public PingResponse(Long iteration) {
            this.reachable = false;
            this.delay = null;
            this.iteration = iteration;
        }
    }
}
