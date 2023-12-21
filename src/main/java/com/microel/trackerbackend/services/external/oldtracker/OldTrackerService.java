package com.microel.trackerbackend.services.external.oldtracker;

import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.external.oldtracker.task.TaskClassOT;
import com.microel.trackerbackend.services.external.oldtracker.task.TaskFieldOT;
import com.microel.trackerbackend.services.external.oldtracker.task.TaskStageOT;
import com.microel.trackerbackend.services.external.oldtracker.task.fields.*;
import com.microel.trackerbackend.storage.entities.task.WorkReport;
import com.microel.trackerbackend.storage.entities.team.Employee;
import org.javatuples.Pair;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class OldTrackerService {

    List<TaskClassOT> taskClasses = new ArrayList<>();
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd-MM-yyyy HH-mm");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");


    public OldTrackerService() {
        List<TaskStageOT> accidentTaskStages = new ArrayList<>();
        List<TaskFieldOT> accidentTaskFields = new ArrayList<>();

        accidentTaskStages.add(new TaskStageOT(1, "Открытые", TaskStageOT.Type.ACTIVE));
        accidentTaskStages.add(new TaskStageOT(964, "КТВ / IPTV", TaskStageOT.Type.ACTIVE));
        accidentTaskStages.add(new TaskStageOT(1201, "Романовка (аварии)", TaskStageOT.Type.ACTIVE));
        accidentTaskStages.add(new TaskStageOT(922, "ЧС Волгодонск", TaskStageOT.Type.ACTIVE));
        accidentTaskStages.add(new TaskStageOT(887, "Групповые аварии", TaskStageOT.Type.ACTIVE));
        accidentTaskStages.add(new TaskStageOT(5, "Отдано монтажникам", TaskStageOT.Type.ACTIVE));
        accidentTaskStages.add(new TaskStageOT(442, "Отложено", TaskStageOT.Type.ACTIVE));
        accidentTaskStages.add(new TaskStageOT(924, "Зависло", TaskStageOT.Type.ACTIVE));
        accidentTaskStages.add(new TaskStageOT(799, "Клиент отказался", TaskStageOT.Type.ARCHIVE));
        accidentTaskStages.add(new TaskStageOT(1269, "Закрыто саппортом", TaskStageOT.Type.ARCHIVE));
        accidentTaskStages.add(new TaskStageOT(1224, "Закрыто за день", TaskStageOT.Type.ARCHIVE));
        accidentTaskStages.add(new TaskStageOT(1225, "Закрыто за неделю", TaskStageOT.Type.ARCHIVE));
        accidentTaskStages.add(new TaskStageOT(1226, "Закрыто за месяц", TaskStageOT.Type.ARCHIVE));
        accidentTaskStages.add(new TaskStageOT(800, "Архив", TaskStageOT.Type.ARCHIVE));

        Map<Long, Pair<Integer, String>> accidentStreets = new HashMap<>();
        accidentStreets.put(37L, Pair.with(8,"БВП"));
        accidentStreets.put(42L, Pair.with(1,"Весенняя"));
        accidentStreets.put(53L, Pair.with(52,"Волгодонская"));
        accidentStreets.put(59L, Pair.with(2,"Гагарина"));
        accidentStreets.put(60L, Pair.with(14,"Гаражная"));
        accidentStreets.put(176L, Pair.with(24,"Горького"));
        accidentStreets.put(75L, Pair.with(26,"Дзержинского"));
        accidentStreets.put(85L, Pair.with(12,"Дружбы"));
        accidentStreets.put(99L, Pair.with(22,"Западный"));
        accidentStreets.put(113L, Pair.with(19,"Индустриальная"));
        accidentStreets.put(177L, Pair.with(27,"Козлова"));
        accidentStreets.put(7L, Pair.with(11,"Королева"));
        accidentStreets.put(186L, Pair.with(6,"Кошевого"));
        accidentStreets.put(115L, Pair.with(3,"КМ"));
        accidentStreets.put(154L, Pair.with(7,"Курчатова"));
        accidentStreets.put(156L, Pair.with(17,"Лазоревый"));
        accidentStreets.put(161L, Pair.with(9,"Ленинградская"));
        accidentStreets.put(160L, Pair.with(10,"Ленина"));
        accidentStreets.put(169L, Pair.with(20,"Логовской"));
        accidentStreets.put(4L, Pair.with(15,"Мира"));
        accidentStreets.put(201L, Pair.with(18,"Молодежная"));
        accidentStreets.put(77L, Pair.with(23,"Морская"));
        accidentStreets.put(254L, Pair.with(25,"Пионерская"));
        accidentStreets.put(334L, Pair.with(5,"Строителей"));
        accidentStreets.put(332L, Pair.with(21,"Степная"));
        accidentStreets.put(231L, Pair.with(13,"Окт. Шоссе"));
        accidentStreets.put(370L, Pair.with(16,"Черникова"));
        accidentStreets.put(383L, Pair.with(4,"Энтузиастов"));
        accidentStreets.put(400L, Pair.with(50,"30 Лет победы"));
        accidentStreets.put(402L, Pair.with(51,"50 Лет СССР"));

//        accidentTaskFields.add(new DefaultFieldOT(938, "Тип клиента", "chastn"));
//        accidentTaskFields.add(new CurrentDateTimeFieldOT(4, "Время обращения"));
        accidentTaskFields.add(new TextFieldOT(2, "Логин"));
        accidentTaskFields.add(new StreetFieldOT(1163, "Улица", accidentStreets, 0));
        accidentTaskFields.add(new TextFieldOT(1161, "Дом"));
        accidentTaskFields.add(new TextFieldOT(1162, "Квартира"));
        accidentTaskFields.add(new TextFieldOT(110, "Контактный телефон"));
        accidentTaskFields.add(new TextFieldOT(5, "Информация о неисправностях"));
//        accidentTaskFields.add(new TextFieldOT(1, "Монтажник на линии"));

        TaskClassOT accidentTaskClass = new TaskClassOT(1, "Аварии (Волгодонск)", accidentTaskStages, accidentTaskFields, ()->{
            List<OldTrackerRequestFactory.FieldData> dataList = new ArrayList<>();
            dataList.add(new OldTrackerRequestFactory.FieldData(938, TaskFieldOT.Type.TEXT,"chastn"));
            dataList.add(new OldTrackerRequestFactory.FieldData(4, TaskFieldOT.Type.DATETIME,dateTimeFormat.format(new Date())));
            return dataList;
        }, employees -> {
            List<OldTrackerRequestFactory.FieldData> dataList = new ArrayList<>();
            for(Employee employee : employees){
                if(employee.getOldTrackerCredentials() != null && employee.getOldTrackerCredentials().getInstallerId() != null) {
                    dataList.add(new OldTrackerRequestFactory.FieldData(1, TaskFieldOT.Type.TEXT, employee.getOldTrackerCredentials().getInstallerId()));
                    dataList.add(new OldTrackerRequestFactory.FieldData(9, TaskFieldOT.Type.DATETIME, dateTimeFormat.format(new Date())));
                    break;
                }
            }
            return dataList;
        }, workReports -> {
            List<OldTrackerRequestFactory.FieldData> dataList = new ArrayList<>();
            StringBuilder reportString = new StringBuilder();
            for(WorkReport workReport : workReports){
                reportString.append(workReport.getAuthor().getFullName()).append(": ").append(workReport.getDescription());
            }
            dataList.add(new OldTrackerRequestFactory.FieldData(12, TaskFieldOT.Type.TEXT, reportString.toString()));
            return dataList;
        });

        taskClasses.add(accidentTaskClass);

        List<TaskStageOT> connectionMKDTaskStages = new ArrayList<>();
        List<TaskFieldOT> connectionMKDTaskFields = new ArrayList<>();

        connectionMKDTaskStages.add(new TaskStageOT(778, "Принять заявку (проверить ТВ)", TaskStageOT.Type.ACTIVE));
        connectionMKDTaskStages.add(new TaskStageOT(789, "Документы готовы", TaskStageOT.Type.ACTIVE));
        connectionMKDTaskStages.add(new TaskStageOT(790, "Работы определены", TaskStageOT.Type.ACTIVE));
        connectionMKDTaskStages.add(new TaskStageOT(779, "Запланировано (пауза)", TaskStageOT.Type.ACTIVE));
        connectionMKDTaskStages.add(new TaskStageOT(791, "Отдано в работу", TaskStageOT.Type.ACTIVE));
        connectionMKDTaskStages.add(new TaskStageOT(1270, "Зависло", TaskStageOT.Type.ACTIVE));
        connectionMKDTaskStages.add(new TaskStageOT(780, "Клиент включен", TaskStageOT.Type.ARCHIVE));
        connectionMKDTaskStages.add(new TaskStageOT(1209, "Нет ТВ", TaskStageOT.Type.ARCHIVE));
        connectionMKDTaskStages.add(new TaskStageOT(921, "Архив", TaskStageOT.Type.ARCHIVE));

        Map<Long, Pair<Integer, String>> connectionMKDStreets = new HashMap<>();
        connectionMKDStreets.put(400L, Pair.with(50, "30 Лет победы"));
        connectionMKDStreets.put(402L, Pair.with(51, "50 Лет СССР"));
        connectionMKDStreets.put(37L, Pair.with(1, "Бульвар Великой Победы"));
        connectionMKDStreets.put(42L, Pair.with(2, "Весенняя"));
        connectionMKDStreets.put(53L, Pair.with(52, "Волгодонская"));
        connectionMKDStreets.put(59L, Pair.with(3, "Гагарина"));
        connectionMKDStreets.put(60L, Pair.with(4, "Гаражная"));
        connectionMKDStreets.put(176L, Pair.with(25, "Горького"));
        connectionMKDStreets.put(75L, Pair.with(26, "Дзержинского"));
        connectionMKDStreets.put(85L, Pair.with(5, "Дружбы"));
        connectionMKDStreets.put(99L, Pair.with(6, "Западный"));
        connectionMKDStreets.put(113L, Pair.with(7, "Индустриальная"));
        connectionMKDStreets.put(115L, Pair.with(10, "К. Маркса"));
        connectionMKDStreets.put(177L, Pair.with(27, "Козлова"));
        connectionMKDStreets.put(7L, Pair.with(8, "Королева"));
        connectionMKDStreets.put(186L, Pair.with(9, "Кошевого"));
        connectionMKDStreets.put(154L, Pair.with(11, "Курчатова"));
        connectionMKDStreets.put(156L, Pair.with(13, "Лазоревый"));
        connectionMKDStreets.put(160L, Pair.with(14, "Ленина"));
        connectionMKDStreets.put(161L, Pair.with(12, "Ленинградская"));
        connectionMKDStreets.put(169L, Pair.with(15, "Логовской"));
        connectionMKDStreets.put(4L, Pair.with(16, "Мира"));
        connectionMKDStreets.put(201L, Pair.with(17, "Молодежная"));
        connectionMKDStreets.put(77L, Pair.with(24, "Морская"));
        connectionMKDStreets.put(254L, Pair.with(23, "Пионерская"));
        connectionMKDStreets.put(332L, Pair.with(18, "Степная"));
        connectionMKDStreets.put(334L, Pair.with(19, "Строителей"));
        connectionMKDStreets.put(231L, Pair.with(20, "Октябрьское шоссе"));
        connectionMKDStreets.put(370L, Pair.with(21, "Черникова"));
        connectionMKDStreets.put(383L, Pair.with(22, "Энтузиастов"));


//        connectionMKDTaskFields.add(new DefaultFieldOT(1273, "Принято от", "2"));
        connectionMKDTaskFields.add(new ConnectionTypeFieldOT(1469, "Тип"));
//        connectionMKDTaskFields.add(new CurrentDateFieldOT(1225, "Создано"));
        connectionMKDTaskFields.add(new AdvertisingSourceFieldOT(1818, "Рекламный источник"));
        connectionMKDTaskFields.add(new TextFieldOT(1449, "Фамилия"));
        connectionMKDTaskFields.add(new TextFieldOT(1226, "Имя"));
        connectionMKDTaskFields.add(new TextFieldOT(1451, "Отчество"));
        connectionMKDTaskFields.add(new StreetFieldOT(1233, "Улица", connectionMKDStreets, 0));
        connectionMKDTaskFields.add(new TextFieldOT(1234, "Дом"));
        connectionMKDTaskFields.add(new TextFieldOT(1237, "Квартира"));
        connectionMKDTaskFields.add(new TextFieldOT(1235, "Подъезд"));
        connectionMKDTaskFields.add(new TextFieldOT(1236, "Этаж"));
        connectionMKDTaskFields.add(new TextFieldOT(1239, "Контакт. тел."));
        connectionMKDTaskFields.add(new TextFieldOT(1268, "Определенные тех. работы"));
        connectionMKDTaskFields.add(new TextFieldOT(1224, "Номер договора в базе"));
        connectionMKDTaskFields.add(new TextFieldOT(1240, "Логин"));
        connectionMKDTaskFields.add(new TextFieldOT(1241, "Пароль"));
        connectionMKDTaskFields.add(new TextFieldOT(1452, "Дата рождения"));
        connectionMKDTaskFields.add(new TextFieldOT(1453, "Место рождения (Край/Обл)"));
        connectionMKDTaskFields.add(new TextFieldOT(1454, "Место рождения (город./село)"));
        connectionMKDTaskFields.add(new TextFieldOT(1227, "Паспорт, серия"));
        connectionMKDTaskFields.add(new TextFieldOT(1228, "Паспорт, номер"));
        connectionMKDTaskFields.add(new TextFieldOT(1229, "Кем выдан"));
        connectionMKDTaskFields.add(new TextFieldOT(1230, "Дата выдачи"));
        connectionMKDTaskFields.add(new TextFieldOT(1232, "Адрес регистрации"));
//        connectionMKDTaskFields.add(new TextFieldOT(1269, "Монтажник"));

        TaskClassOT connectionMKDTaskClass = new TaskClassOT(87, "Подключения", connectionMKDTaskStages, connectionMKDTaskFields, ()->{
            List<OldTrackerRequestFactory.FieldData> fieldData = new ArrayList<>();
            fieldData.add(new OldTrackerRequestFactory.FieldData(1273, TaskFieldOT.Type.TEXT,"2"));
            fieldData.add(new OldTrackerRequestFactory.FieldData(1225, TaskFieldOT.Type.DATE, dateFormat.format(new Date())));
            return fieldData;
        }, employees -> {
            List<OldTrackerRequestFactory.FieldData> dataList = new ArrayList<>();
            for(Employee employee : employees){
                if(employee.getOldTrackerCredentials() != null && employee.getOldTrackerCredentials().getInstallerId() != null) {
                    dataList.add(new OldTrackerRequestFactory.FieldData(1269, TaskFieldOT.Type.TEXT, employee.getOldTrackerCredentials().getInstallerId()));
                    dataList.add(new OldTrackerRequestFactory.FieldData(1274, TaskFieldOT.Type.DATETIME, dateTimeFormat.format(new Date())));
                    break;
                }
            }
            return dataList;
        }, workReports->new ArrayList<>());

        taskClasses.add(connectionMKDTaskClass);

        List<TaskStageOT> connectionPSVlgdTaskStages = new ArrayList<>();
        List<TaskFieldOT> connectionPSVlgdTaskFields = new ArrayList<>();

        connectionPSVlgdTaskStages.add(new TaskStageOT(989, "Проверить ТВ", TaskStageOT.Type.ACTIVE));
        connectionPSVlgdTaskStages.add(new TaskStageOT(1249, "Актуальное", TaskStageOT.Type.ACTIVE));
        connectionPSVlgdTaskStages.add(new TaskStageOT(1250, "На паузе", TaskStageOT.Type.ACTIVE));
        connectionPSVlgdTaskStages.add(new TaskStageOT(995, "Отдано в работу", TaskStageOT.Type.ACTIVE));
        connectionPSVlgdTaskStages.add(new TaskStageOT(1252, "Клиент включен (на схему)", TaskStageOT.Type.ACTIVE));
        connectionPSVlgdTaskStages.add(new TaskStageOT(1251, "Архив", TaskStageOT.Type.ARCHIVE));
        connectionPSVlgdTaskStages.add(new TaskStageOT(998, "Не подключен (нет ТВ)", TaskStageOT.Type.ARCHIVE));
        connectionPSVlgdTaskStages.add(new TaskStageOT(999, "Подключен", TaskStageOT.Type.ARCHIVE));

        Map<Long, Pair<Integer, String>> connectionPSVlgdStreets = new HashMap<>();

        connectionPSVlgdStreets.put(5L, new Pair<>(1, "Адмиральский пр-д."));
        connectionPSVlgdStreets.put(7L, new Pair<>(2, "Академика Королёва"));
        connectionPSVlgdStreets.put(8L, new Pair<>(3, "Академический пер."));
        connectionPSVlgdStreets.put(9L, new Pair<>(4, "Аксайский пр-д."));
        connectionPSVlgdStreets.put(10L, new Pair<>(5, "Алексея Улесова"));
        connectionPSVlgdStreets.put(12L, new Pair<>(6, "Алый пер."));
        connectionPSVlgdStreets.put(19L, new Pair<>(7, "Байкальская"));
        connectionPSVlgdStreets.put(20L, new Pair<>(8, "Балтийская"));
        connectionPSVlgdStreets.put(21L, new Pair<>(9, "Батайский пер."));
        connectionPSVlgdStreets.put(25L, new Pair<>(10, "Беркутянская"));
        connectionPSVlgdStreets.put(29L, new Pair<>(11, "Бирюзовый пр-д."));
        connectionPSVlgdStreets.put(31L, new Pair<>(88, "Богатырский пер."));
        connectionPSVlgdStreets.put(32L, new Pair<>(12, "Боковский пер."));
        connectionPSVlgdStreets.put(34L, new Pair<>(13, "Братская"));
        connectionPSVlgdStreets.put(39L, new Pair<>(14, "Верхний пр-д."));
        connectionPSVlgdStreets.put(44L, new Pair<>(15, "Вешенский пер."));
        connectionPSVlgdStreets.put(49L, new Pair<>(89, "Возрождения пер."));
        connectionPSVlgdStreets.put(59L, new Pair<>(16, "Гагарина"));
        connectionPSVlgdStreets.put(60L, new Pair<>(17, "Гаражная"));
        connectionPSVlgdStreets.put(70L, new Pair<>(18, "Гранатовый пер."));
        connectionPSVlgdStreets.put(72L, new Pair<>(19, "Гуковский пер."));
        connectionPSVlgdStreets.put(578L, new Pair<>(20, "Дивноморская"));
        connectionPSVlgdStreets.put(78L, new Pair<>(90, "Дивный пер."));
        connectionPSVlgdStreets.put(80L, new Pair<>(21, "Добрый пер."));
        connectionPSVlgdStreets.put(82L, new Pair<>(22, "Донецкий пер."));
        connectionPSVlgdStreets.put(85L, new Pair<>(23, "Дружбы"));
        connectionPSVlgdStreets.put(90L, new Pair<>(24, "Дунайский пер."));
        connectionPSVlgdStreets.put(102L, new Pair<>(25, "Здоровья пер."));
        connectionPSVlgdStreets.put(105L, new Pair<>(26, "Зерноградская"));
        connectionPSVlgdStreets.put(112L, new Pair<>(27, "Изумрудный пр-д."));
        connectionPSVlgdStreets.put(113L, new Pair<>(28, "Индустриальная"));
        connectionPSVlgdStreets.put(118L, new Pair<>(29, "Каменский пер."));
        connectionPSVlgdStreets.put(122L, new Pair<>(30, "Каспийская"));
        connectionPSVlgdStreets.put(123L, new Pair<>(31, "Каштановый пер."));
        connectionPSVlgdStreets.put(131L, new Pair<>(32, "Кольцевая"));
        connectionPSVlgdStreets.put(132L, new Pair<>(33, "Кольцо Надежды"));
        connectionPSVlgdStreets.put(150L, new Pair<>(34, "Крымская"));
        connectionPSVlgdStreets.put(151L, new Pair<>(35, "Кубанский пер."));
        connectionPSVlgdStreets.put(154L, new Pair<>(36, "Курчатова просп."));
        connectionPSVlgdStreets.put(156L, new Pair<>(37, "Лазоревый просп."));
        connectionPSVlgdStreets.put(161L, new Pair<>(38, "Ленинградская"));
        connectionPSVlgdStreets.put(169L, new Pair<>(39, "Логовская"));
        connectionPSVlgdStreets.put(171L, new Pair<>(40, "Лозновский пер."));
        connectionPSVlgdStreets.put(178L, new Pair<>(91, "Магистральный пер."));
        connectionPSVlgdStreets.put(186L, new Pair<>(85, "Маршала Кошевого"));
        connectionPSVlgdStreets.put(190L, new Pair<>(41, "Мачтовая"));
        connectionPSVlgdStreets.put(192L, new Pair<>(42, "Маячный пер."));
        connectionPSVlgdStreets.put(195L, new Pair<>(43, "Мелиховский пер."));
        connectionPSVlgdStreets.put(196L, new Pair<>(44, "Миллеровская"));
        connectionPSVlgdStreets.put(4L, new Pair<>(45, "Мира"));
        connectionPSVlgdStreets.put(203L, new Pair<>(46, "Мореходная"));
        connectionPSVlgdStreets.put(204L, new Pair<>(47, "Морозовский пер."));
        connectionPSVlgdStreets.put(213L, new Pair<>(48, "Нахичеванский пер."));
        connectionPSVlgdStreets.put(214L, new Pair<>(49, "Невская"));
        connectionPSVlgdStreets.put(217L, new Pair<>(50, "Нижний пр-д."));
        connectionPSVlgdStreets.put(226L, new Pair<>(51, "Овражная"));
        connectionPSVlgdStreets.put(231L, new Pair<>(52, "Октябрьское ш."));
        connectionPSVlgdStreets.put(232L, new Pair<>(53, "Олимпийский пер."));
        connectionPSVlgdStreets.put(241L, new Pair<>(92, "Открытый пр-д."));
        connectionPSVlgdStreets.put(242L, new Pair<>(54, "Отрадный пр-д."));
        connectionPSVlgdStreets.put(243L, new Pair<>(55, "Офицерский пр-д."));
        connectionPSVlgdStreets.put(244L, new Pair<>(56, "Охотский пр-д."));
        connectionPSVlgdStreets.put(247L, new Pair<>(86, "Паромный пер."));
        connectionPSVlgdStreets.put(249L, new Pair<>(57, "Парусная"));
        connectionPSVlgdStreets.put(251L, new Pair<>(58, "Песчанная"));
        connectionPSVlgdStreets.put(263L, new Pair<>(59, "Приветливый пр-д."));
        connectionPSVlgdStreets.put(264L, new Pair<>(60, "Пригородный пер."));
        connectionPSVlgdStreets.put(265L, new Pair<>(61, "Приморский бульв."));
        connectionPSVlgdStreets.put(267L, new Pair<>(62, "Пролетарский пер."));
        connectionPSVlgdStreets.put(270L, new Pair<>(63, "Прохладная"));
        connectionPSVlgdStreets.put(278L, new Pair<>(64, "Раздорский пер."));
        connectionPSVlgdStreets.put(283L, new Pair<>(65, "Роз бульв."));
        connectionPSVlgdStreets.put(289L, new Pair<>(93, "Рыбачий пер."));
        connectionPSVlgdStreets.put(293L, new Pair<>(66, "Сальский пер."));
        connectionPSVlgdStreets.put(305L, new Pair<>(67, "Серебрянный пер."));
        connectionPSVlgdStreets.put(310L, new Pair<>(94, "Славный пер."));
        connectionPSVlgdStreets.put(319L, new Pair<>(68, "Содружества бульв."));
        connectionPSVlgdStreets.put(326L, new Pair<>(95, "Спокойный пр-д."));
        connectionPSVlgdStreets.put(328L, new Pair<>(69, "Средиземная"));
        connectionPSVlgdStreets.put(329L, new Pair<>(70, "Средний пр-д."));
        connectionPSVlgdStreets.put(331L, new Pair<>(71, "Старочеркасский бульв."));
        connectionPSVlgdStreets.put(338L, new Pair<>(72, "Таганрогская"));
        connectionPSVlgdStreets.put(339L, new Pair<>(73, "Таисский пер."));
        connectionPSVlgdStreets.put(340L, new Pair<>(74, "Тараса Ботяновского бульв."));
        connectionPSVlgdStreets.put(341L, new Pair<>(75, "Тацинский бульв."));
        connectionPSVlgdStreets.put(352L, new Pair<>(76, "Удачный пер."));
        connectionPSVlgdStreets.put(355L, new Pair<>(77, "Уютный пер."));
        connectionPSVlgdStreets.put(357L, new Pair<>(78, "Флотская"));
        connectionPSVlgdStreets.put(358L, new Pair<>(79, "Фонтанный пер."));
        connectionPSVlgdStreets.put(360L, new Pair<>(87, "Фрегатная"));
        connectionPSVlgdStreets.put(365L, new Pair<>(80, "Цветочный бульв."));
        connectionPSVlgdStreets.put(376L, new Pair<>(81, "Шахтинский пер."));
        connectionPSVlgdStreets.put(381L, new Pair<>(82, "Штурвальная"));
        connectionPSVlgdStreets.put(382L, new Pair<>(96, "Энергетиков пер."));
        connectionPSVlgdStreets.put(383L, new Pair<>(83, "Энтузиастов"));
        connectionPSVlgdStreets.put(388L, new Pair<>(84, "Якорный пер."));

//        connectionPSVlgdTaskFields.add(new DefaultFieldOT(1871, "Район", "0"));
        connectionPSVlgdTaskFields.add(new StreetFieldOT(1870, "Улица", connectionPSVlgdStreets, 0));
        connectionPSVlgdTaskFields.add(new TextFieldOT(1536, "Дом"));
        connectionPSVlgdTaskFields.add(new TextFieldOT(1537, "Телефон"));
        connectionPSVlgdTaskFields.add(new TextFieldOT(1538, "Контактное лицо"));
        connectionPSVlgdTaskFields.add(new AdvertisingSourceFieldOT(1819, "Рекламный источник"));
//        connectionPSVlgdTaskFields.add(new CurrentDateTimeFieldOT(1546, "Отдано в работу"));
//        connectionPSVlgdTaskFields.add(new TextFieldOT(1547, "Кому отдано"));

        TaskClassOT connectionPSVlgdTaskClass = new TaskClassOT(106, "ЧС Волгодонск", connectionPSVlgdTaskStages, connectionPSVlgdTaskFields, ()->{
            List<OldTrackerRequestFactory.FieldData> fieldData = new ArrayList<>();
            fieldData.add(new OldTrackerRequestFactory.FieldData(1871, TaskFieldOT.Type.TEXT, "0"));
            return fieldData;
        }, employees -> {
            List<OldTrackerRequestFactory.FieldData> dataList = new ArrayList<>();
            String employeesNames = Stream.of(employees).map(Employee::getFullName).collect(Collectors.joining(", "));
            dataList.add(new OldTrackerRequestFactory.FieldData(1547, TaskFieldOT.Type.TEXT, employeesNames));
            dataList.add(new OldTrackerRequestFactory.FieldData(1546, TaskFieldOT.Type.DATETIME, dateTimeFormat.format(new Date())));
            return dataList;
        }, workReports -> new ArrayList<>());

        taskClasses.add(connectionPSVlgdTaskClass);


        List<TaskStageOT> connectionPSRomaTaskStages = new ArrayList<>();
        List<TaskFieldOT> connectionPSRomaTaskFields = new ArrayList<>();

        connectionPSRomaTaskStages.add(new TaskStageOT(1253, "Проверить ТВ", TaskStageOT.Type.ACTIVE));
        connectionPSRomaTaskStages.add(new TaskStageOT(1254, "Актуальное", TaskStageOT.Type.ACTIVE));
        connectionPSRomaTaskStages.add(new TaskStageOT(1255, "На паузе", TaskStageOT.Type.ACTIVE));
        connectionPSRomaTaskStages.add(new TaskStageOT(1256, "Отдано в работу", TaskStageOT.Type.ACTIVE));
        connectionPSRomaTaskStages.add(new TaskStageOT(1257, "Клиент включен (на схему)", TaskStageOT.Type.ACTIVE));
        connectionPSRomaTaskStages.add(new TaskStageOT(1258, "Архив", TaskStageOT.Type.ARCHIVE));
        connectionPSRomaTaskStages.add(new TaskStageOT(1259, "Не подключен (нет ТВ)", TaskStageOT.Type.ARCHIVE));
        connectionPSRomaTaskStages.add(new TaskStageOT(1260, "Подключен", TaskStageOT.Type.ARCHIVE));

        Map<Long, Pair<Integer, String>> connectionPSRomaStreets = new HashMap<>();

        connectionPSRomaStreets.put(460L, new Pair<>(2, "40 лет Победы"));
        connectionPSRomaStreets.put(461L, new Pair<>(3, "50 лет Победы"));
        connectionPSRomaStreets.put(462L, new Pair<>(4, "70 лет Октября"));
        connectionPSRomaStreets.put(463L, new Pair<>(5, "75 лет Победы"));
        connectionPSRomaStreets.put(405L, new Pair<>(6, "Алферовский пер."));
        connectionPSRomaStreets.put(406L, new Pair<>(7, "Базарная"));
        connectionPSRomaStreets.put(407L, new Pair<>(8, "Береговая"));
        connectionPSRomaStreets.put(408L, new Pair<>(9, "Бобровский пер."));
        connectionPSRomaStreets.put(409L, new Pair<>(10, "Братская"));
        connectionPSRomaStreets.put(410L, new Pair<>(11, "Весенняя"));
        connectionPSRomaStreets.put(411L, new Pair<>(12, "Виноградный пер."));
        connectionPSRomaStreets.put(412L, new Pair<>(13, "Вознесенский пер."));
        connectionPSRomaStreets.put(413L, new Pair<>(14, "Гагарина пер."));
        connectionPSRomaStreets.put(414L, new Pair<>(15, "Гладкова пер."));
        connectionPSRomaStreets.put(415L, new Pair<>(16, "Гоголя пер."));
        connectionPSRomaStreets.put(416L, new Pair<>(17, "Депутатская"));
        connectionPSRomaStreets.put(417L, new Pair<>(18, "Донской пер."));
        connectionPSRomaStreets.put(418L, new Pair<>(19, "Жемчужная"));
        connectionPSRomaStreets.put(419L, new Pair<>(20, "Забазновой"));
        connectionPSRomaStreets.put(420L, new Pair<>(21, "Заречная"));
        connectionPSRomaStreets.put(421L, new Pair<>(22, "Изумрудный пер."));
        connectionPSRomaStreets.put(422L, new Pair<>(23, "Казачий пер."));
        connectionPSRomaStreets.put(423L, new Pair<>(24, "Каргальского"));
        connectionPSRomaStreets.put(424L, new Pair<>(25, "Кожанова пер."));
        connectionPSRomaStreets.put(425L, new Pair<>(26, "Колхозный пер."));
        connectionPSRomaStreets.put(426L, new Pair<>(27, "Комсомольский пер."));
        connectionPSRomaStreets.put(427L, new Pair<>(28, "Котова пер."));
        connectionPSRomaStreets.put(428L, new Pair<>(29, "Красноармейская"));
        connectionPSRomaStreets.put(429L, new Pair<>(30, "Ленина"));
        connectionPSRomaStreets.put(430L, new Pair<>(31, "Лесная"));
        connectionPSRomaStreets.put(431L, new Pair<>(32, "Луговая"));
        connectionPSRomaStreets.put(432L, new Pair<>(33, "Мелиоративная"));
        connectionPSRomaStreets.put(433L, new Pair<>(34, "Мелиораторов"));
        connectionPSRomaStreets.put(434L, new Pair<>(35, "Мира"));
        connectionPSRomaStreets.put(435L, new Pair<>(36, "Набережная"));
        connectionPSRomaStreets.put(436L, new Pair<>(37, "Одесская"));
        connectionPSRomaStreets.put(437L, new Pair<>(38, "Октябрьский пер."));
        connectionPSRomaStreets.put(559L, new Pair<>(39, "Отдыха"));
        connectionPSRomaStreets.put(438L, new Pair<>(40, "Пионерский пер."));
        connectionPSRomaStreets.put(439L, new Pair<>(41, "Полевая"));
        connectionPSRomaStreets.put(440L, new Pair<>(42, "Почтовая"));
        connectionPSRomaStreets.put(441L, new Pair<>(43, "Пронина"));
        connectionPSRomaStreets.put(442L, new Pair<>(44, "Рубиновый пер."));
        connectionPSRomaStreets.put(443L, new Pair<>(45, "Садовая"));
        connectionPSRomaStreets.put(444L, new Pair<>(46, "Смолякова"));
        connectionPSRomaStreets.put(445L, new Pair<>(47, "Советский пер."));
        connectionPSRomaStreets.put(446L, new Pair<>(48, "Соловьевых"));
        connectionPSRomaStreets.put(447L, new Pair<>(49, "Союзный пер."));
        connectionPSRomaStreets.put(448L, new Pair<>(50, "Стахановский пер."));
        connectionPSRomaStreets.put(449L, new Pair<>(51, "Степана Разина"));
        connectionPSRomaStreets.put(450L, new Pair<>(52, "Степной пер."));
        connectionPSRomaStreets.put(451L, new Pair<>(53, "Строительная"));
        connectionPSRomaStreets.put(452L, new Pair<>(54, "Тюхова"));
        connectionPSRomaStreets.put(453L, new Pair<>(55, "Чехова пер."));
        connectionPSRomaStreets.put(454L, new Pair<>(56, "Чибисова"));
        connectionPSRomaStreets.put(455L, new Pair<>(57, "Чкаловский пер."));
        connectionPSRomaStreets.put(456L, new Pair<>(58, "Шмутовой пер."));
        connectionPSRomaStreets.put(457L, new Pair<>(59, "Юбилейная"));
        connectionPSRomaStreets.put(458L, new Pair<>(60, "Язева"));
        connectionPSRomaStreets.put(459L, new Pair<>(61, "Ясина пер."));

        connectionPSRomaTaskFields.add(new StreetFieldOT(1877, "Улица", connectionPSRomaStreets, 0));
        connectionPSRomaTaskFields.add(new TextFieldOT(1878, "Дом"));
        connectionPSRomaTaskFields.add(new TextFieldOT(1879, "Телефон"));
        connectionPSRomaTaskFields.add(new TextFieldOT(1880, "Контактное лицо"));
        connectionPSRomaTaskFields.add(new AdvertisingSourceFieldOT(1881, "Рекламный источник"));
//        connectionPSRomaTaskFields.add(new CurrentDateTimeFieldOT(1883, "Отдано в работу"));
//        connectionPSRomaTaskFields.add(new TextFieldOT(1884, "Кому отдано"));

        TaskClassOT connectionPSRomaTaskClass = new TaskClassOT(121, "ЧС Романовская (Микроэл)", connectionPSRomaTaskStages, connectionPSRomaTaskFields, ArrayList::new, employees -> {
            List<OldTrackerRequestFactory.FieldData> dataList = new ArrayList<>();
            String employeesNames = Stream.of(employees).map(Employee::getFullName).collect(Collectors.joining(", "));
            dataList.add(new OldTrackerRequestFactory.FieldData(1884, TaskFieldOT.Type.TEXT, employeesNames));
            dataList.add(new OldTrackerRequestFactory.FieldData(1883, TaskFieldOT.Type.DATETIME, dateTimeFormat.format(new Date())));
            return dataList;
        }, workReports -> new ArrayList<>());

        taskClasses.add(connectionPSRomaTaskClass);
    }

    public List<TaskClassOT> getTaskClasses() {
        return taskClasses;
    }

    public TaskClassOT getTaskClassById(Integer classId) {
        return taskClasses.stream().filter(c -> c.getId().equals(classId)).findFirst().orElseThrow(()->new ResponseException("Не найдена класс задачи с id " + classId));
    }

    public TaskClassOT getTaskClassByName(String className) {
        return taskClasses.stream().filter(c -> c.getName().equals(className)).findFirst().orElseThrow(()->new ResponseException("Не найдена класс задачи с именем " + className));
    }
}
