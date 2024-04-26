package com.microel.trackerbackend.storage.dispatchers;

import com.microel.tdo.EventType;
import com.microel.tdo.chart.TimeDataset;
import com.microel.tdo.chart.TimePoint;
import com.microel.trackerbackend.controllers.telegram.TelegramController;
import com.microel.trackerbackend.modules.transport.DateRange;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.entities.sensors.temperature.TemperatureRange;
import com.microel.trackerbackend.storage.entities.sensors.temperature.TemperatureSensor;
import com.microel.trackerbackend.storage.entities.sensors.temperature.TemperatureSnapshot;
import com.microel.trackerbackend.storage.repositories.TemperatureRangeRepository;
import com.microel.trackerbackend.storage.repositories.TemperatureSensorRepository;
import com.microel.trackerbackend.storage.repositories.TemperatureSnapshotRepository;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
@Transactional(readOnly = true)
public class TemperatureSensorsDispatcher {
    private final TemperatureSensorRepository temperatureSensorRepository;
    private final TemperatureRangeRepository temperatureRangeRepository;
    private final TemperatureSnapshotRepository temperatureSnapshotRepository;
    private final StompController stompController;
    private final TelegramController telegramController;

    public TemperatureSensorsDispatcher(TemperatureSensorRepository temperatureSensorRepository,
                                        TemperatureRangeRepository temperatureRangeRepository,
                                        TemperatureSnapshotRepository temperatureSnapshotRepository, StompController stompController, TelegramController telegramController) {
        this.temperatureSensorRepository = temperatureSensorRepository;
        this.temperatureRangeRepository = temperatureRangeRepository;
        this.temperatureSnapshotRepository = temperatureSnapshotRepository;
        this.stompController = stompController;
        this.telegramController = telegramController;
    }

    @Scheduled(cron = "0 0 3 1 * *")
    @Async
    @Transactional
    public void clearOldSnapshots() {
        Timestamp interval = Timestamp.from(Instant.now().minus(30, ChronoUnit.DAYS));
        List<TemperatureSnapshot> snapshots = temperatureSnapshotRepository.findAll((root, query, cb) -> cb.and(
                cb.lessThan(root.get("timestamp"), interval)
        ));
        temperatureSnapshotRepository.deleteAll(snapshots);
    }

    @Scheduled(fixedDelay = 10L, timeUnit = TimeUnit.MINUTES)
    @Async
    @Transactional
    public void checkingSensorAvailability() {
        Timestamp interval = Timestamp.from(Instant.now().minusSeconds(600));
        List<TemperatureSensor> sensors = temperatureSensorRepository.findAll((root, query, cb) -> cb.and(
                cb.isTrue(root.get("active")),
                cb.lessThan(root.get("updated"), interval)
        ), Sort.by(Sort.Direction.ASC, "temperatureSensorId"));
        for (TemperatureSensor sensor : sensors) {
            sensor.setActive(false);
            sensor = temperatureSensorRepository.save(sensor);
            stompController.updateSensor(SensorUpdateEvent.of(EventType.UPDATE, sensor));
            try {
                telegramController.sendSensorAlert(
                        SensorAlertEvent.of(
                                SensorAlertEvent.EventType.DOWN,
                                sensor.getName(),
                                SensorAlertEvent.SensorType.TEMPERATURE
                        )
                );
            } catch (Exception e) {
                System.out.println("Не удалось отправить оповещение в телеграм");
            }
        }
    }

    @Transactional
    public void updateSensor(SensorEvent event) {
        EventType eventType = null;
        TemperatureSensor sensor = temperatureSensorRepository.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("name"), event.getName())
        ), Sort.by(Sort.Direction.ASC, "temperatureSensorId")).stream().findFirst().orElse(null);

        if (sensor == null) {
            sensor = TemperatureSensor.create(event);
            eventType = EventType.CREATE;
        } else {
            if(!sensor.getActive()){
                try {
                    telegramController.sendSensorAlert(
                            SensorAlertEvent.of(
                                    SensorAlertEvent.EventType.UP,
                                    sensor.getName(),
                                    SensorAlertEvent.SensorType.TEMPERATURE
                            )
                    );
                } catch (Exception e) {
                    System.out.println("Не удалось отправить оповещение в телеграм");
                }
            }
            sensor.update(event);
            eventType = EventType.UPDATE;
        }

        TemperatureRange temperatureRange = sensor.getRanges().stream().filter(range -> {
            Float minTemp = range.getMinTemp();
            Float maxTemp = range.getMaxTemp();
            return event.getValue() >= minTemp && event.getValue() < maxTemp;
        }).findFirst().orElse(null);

        if (sensor.getCurrentRange() == null || !Objects.equals(sensor.getCurrentRange(), temperatureRange)) {
            sensor.setCurrentRange(temperatureRange);
            try {
                telegramController.sendTempSensorRange(sensor);
            } catch (Exception e) {
                System.out.println("Не удалось отправить оповещение в телеграм");
            }
        }

        sensor = temperatureSensorRepository.save(sensor);

        temperatureSnapshotRepository.save(TemperatureSnapshot.of(sensor));

        stompController.updateSensor(SensorUpdateEvent.of(eventType, sensor));
    }

    @Transactional
    public void deleteSensor(Long id) {
        TemperatureSensor sensor = temperatureSensorRepository.findById(id).orElse(null);
        if (sensor != null) {
            temperatureSensorRepository.delete(sensor);
            stompController.updateSensor(SensorUpdateEvent.of(EventType.DELETE, sensor));
        }
    }

    @Transactional
    public void saveTemperatureRanges(List<TemperatureRange> ranges, Long sensorId) {
        TemperatureSensor sensor = temperatureSensorRepository.findById(sensorId)
                .orElseThrow(() -> new ResponseException("Сенсор не найден"));
        for (TemperatureRange range : ranges) {
            if (range.getTemperatureRangeId() != null) {
                TemperatureRange temperatureRange = temperatureRangeRepository.findById(range.getTemperatureRangeId())
                        .orElseThrow(() -> new ResponseException("Ошибка обновления данных сенсора"));
                temperatureRange.update(range);
                temperatureRangeRepository.save(range);
            } else {
                range.setSensor(sensor);
                temperatureRangeRepository.save(range);
            }
        }
        sensor = temperatureSensorRepository.findById(sensorId)
                .orElseThrow(() -> new ResponseException("Сенсор не найден"));
        stompController.updateSensor(SensorUpdateEvent.of(EventType.UPDATE, sensor));
    }

    public List<TemperatureSensor> getAllSensors() {
        return temperatureSensorRepository.findAll(Sort.by(Sort.Direction.ASC, "temperatureSensorId"));
    }

    public TimeDataset getChart(Long id, DateRange timeRange) {
        if (!timeRange.validate()) throw new ResponseException("Некорректный период данных");
        TemperatureSensor sensor = temperatureSensorRepository.findById(id).orElseThrow(() -> new ResponseException("Датчик не найден"));
        List<TemperatureSnapshot> snapshots = temperatureSnapshotRepository.findAll((root, query, cb) -> cb.and(
                cb.equal(root.join("sensor"), sensor),
                cb.between(root.get("timestamp"), timeRange.start(), timeRange.end())
        ), Sort.by(Sort.Direction.ASC, "timestamp"));
        return TimeDataset.of(sensor.getName(), snapshots.stream().map(snapshot -> TimePoint.of(snapshot.getTimestamp(), snapshot.getValue())).toList());
    }

    @Transactional
    public void appendRange(Long id, TemperatureRange range) {
        if (range.isNotValid()) throw new ResponseException("Не верно указаны параметры диапазона температур");
        TemperatureSensor sensor = temperatureSensorRepository.findById(id).orElseThrow(() -> new ResponseException("Датчик не найден"));
        boolean isNameExist = sensor.getRanges().stream().anyMatch((rng) -> range.getName().equals(rng.getName()));
        boolean isOverlap = sensor.getRanges().stream().anyMatch((rng) ->
            (range.getMaxTemp() > rng.getMinTemp() || range.getMaxTemp() > rng.getMaxTemp())
                    && (range.getMinTemp() < rng.getMinTemp() || range.getMinTemp() < rng.getMaxTemp())
        );
        if (isNameExist) throw new ResponseException("Диапазон с таким именем уже существует");
        if (isOverlap) throw new ResponseException("Диапазон перекрывает существующие");
        range.setSensor(sensor);
        temperatureRangeRepository.save(range);
        sensor = temperatureSensorRepository.findById(id).orElseThrow(() -> new ResponseException("Датчик не найден"));
        stompController.updateSensor(SensorUpdateEvent.of(EventType.UPDATE, sensor));
    }

    @Transactional
    public void editRange(Long id, TemperatureRange range) {
        if (range.isNotValid()) throw new ResponseException("Не верно указаны параметры диапазона температур");
        TemperatureRange temperatureRange = temperatureRangeRepository.findById(id).orElseThrow(() -> new ResponseException("Диапазон не найден"));
        Long temperatureSensorId = temperatureRange.getSensor().getTemperatureSensorId();
        TemperatureSensor sensor = temperatureSensorRepository.findById(temperatureSensorId).orElseThrow(() -> new ResponseException("Датчик не найден"));
        boolean isNameExist = sensor.getRanges().stream()
                .filter((rng) -> !Objects.equals(rng.getTemperatureRangeId(), id))
                .anyMatch((rng) -> range.getName().equals(rng.getName()));
        boolean isOverlap = sensor.getRanges().stream()
                .filter((rng) -> !Objects.equals(rng.getTemperatureRangeId(), id))
                .anyMatch((rng) -> (range.getMaxTemp() > rng.getMinTemp() || range.getMaxTemp() > rng.getMaxTemp())
                        && (range.getMinTemp() < rng.getMinTemp() || range.getMinTemp() < rng.getMaxTemp())
        );
        if (isNameExist) throw new ResponseException("Диапазон с таким именем уже существует");
        if (isOverlap) throw new ResponseException("Диапазон перекрывает существующие");
        temperatureRange.update(range);
        temperatureRangeRepository.save(temperatureRange);
        sensor = temperatureSensorRepository.findById(temperatureSensorId).orElseThrow(() -> new ResponseException("Датчик не найден"));
        stompController.updateSensor(SensorUpdateEvent.of(EventType.UPDATE, sensor));
    }

    @Transactional
    public void deleteRange(Long id) {
        TemperatureRange temperatureRange = temperatureRangeRepository.findById(id).orElseThrow(() -> new ResponseException("Диапазон не найден"));
        Long temperatureSensorId = temperatureRange.getSensor().getTemperatureSensorId();
        temperatureRangeRepository.delete(temperatureRange);
        TemperatureSensor sensor = temperatureSensorRepository.findById(temperatureSensorId).orElseThrow(() -> new ResponseException("Датчик не найден"));
        stompController.updateSensor(SensorUpdateEvent.of(EventType.UPDATE, sensor));
    }

    @Data
    @ToString
    public static class SensorEvent {
        private String name;
        private Float value;
    }

    @Data
    @ToString
    public static class SensorUpdateEvent {
        private EventType eventType;
        private TemperatureSensor data;

        public static SensorUpdateEvent of(EventType eventType, TemperatureSensor sensor) {
            SensorUpdateEvent event = new SensorUpdateEvent();
            event.setEventType(eventType);
            event.setData(sensor);
            return event;
        }
    }

    @Data
    @ToString
    public static class SensorAlertEvent {
        private EventType type;
        private String sensorName;
        private SensorType sensorType;

        public static SensorAlertEvent of(EventType type, String sensorName, SensorType sensorType) {
            SensorAlertEvent event = new SensorAlertEvent();
            event.setType(type);
            event.setSensorName(sensorName);
            event.setSensorType(sensorType);
            return event;
        }

        public enum EventType {
            UP,DOWN;
        }

        public enum SensorType {
            TEMPERATURE;
        }
    }

}
