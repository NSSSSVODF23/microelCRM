package com.microel.trackerbackend.misc.accounting;

import com.lowagie.text.pdf.BaseFont;
import com.microel.trackerbackend.BackendApplication;
import com.microel.trackerbackend.controllers.telegram.Utils;
import com.microel.trackerbackend.misc.FactorAction;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.storage.entities.address.Address;
import com.microel.trackerbackend.storage.entities.salary.ActionTaken;
import com.microel.trackerbackend.storage.entities.salary.WorkCalculation;
import com.microel.trackerbackend.storage.entities.salary.WorkingDay;
import com.microel.trackerbackend.storage.entities.templating.PassportDetails;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.lang.Nullable;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.Year;
import java.util.*;
import java.util.stream.Stream;

public class TDocumentFactory {

    public static MonthlySalaryReportTable createMonthlySalaryReportTable(Map<Date, List<WorkingDay>> workingDaysByOffsiteEmployees, Date startDate, Date endDate) {

        Workbook workbook = new XSSFWorkbook();

        short percentFormat = workbook.createDataFormat().getFormat("0.00%");
        short factorFormat = workbook.createDataFormat().getFormat("0.00");

        CellStyle percentStyle = workbook.createCellStyle();
        percentStyle.setDataFormat(percentFormat);

        CellStyle factorStyle = workbook.createCellStyle();
        factorStyle.setDataFormat(factorFormat);

        CellStyle headerStyle = workbook.createCellStyle();
        XSSFFont headerFont = ((XSSFWorkbook) workbook).createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());

        CellStyle allSumStyle = workbook.createCellStyle();
        XSSFFont allSumFont = ((XSSFWorkbook) workbook).createFont();
        allSumFont.setBold(true);
        allSumFont.setColor(IndexedColors.WHITE.getIndex());
        allSumStyle.setFont(allSumFont);
        allSumStyle.setAlignment(HorizontalAlignment.RIGHT);
        allSumStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        allSumStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        allSumStyle.setFillForegroundColor(IndexedColors.SEA_GREEN.getIndex());

        Sheet sheet = workbook.createSheet("Persons");

        // Установка заголовка таблицы
        Row header = sheet.createRow(0);
        header.setHeight((short) 500);
        List<String> headers = List.of("Дата", "ФИО", "ID Задачи", "Действие", "Кол-во", "Коэф.", "Цена", "Доля", "Сумма");
        for(int i = 0; i < headers.size(); i++){
            Cell headerCell = header.createCell(i);
            headerCell.setCellValue(headers.get(i));
            headerCell.setCellStyle(headerStyle);
        }

        long daysCount = Duration.between(startDate.toInstant(), endDate.toInstant()).toDays()+1;

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

        GregorianCalendar calendar = new GregorianCalendar();
        sdf.setCalendar(calendar);
        calendar.setTime(startDate);

        int globalRowIndex = 1;

        for (int i = 0; i < daysCount; i++) {

            List<WorkingDay> workingDays = workingDaysByOffsiteEmployees.get(calendar.getTime());
            Row contentRow = sheet.getRow(globalRowIndex);
            if(contentRow == null) contentRow = sheet.createRow(globalRowIndex);

            Cell dateCell = contentRow.createCell(0);
            dateCell.setCellValue(sdf.format(calendar.getTime()));

            if(workingDays != null) {
                for (WorkingDay workingDay : workingDays) {

                    contentRow = sheet.getRow(globalRowIndex);
                    if (contentRow == null) contentRow = sheet.createRow(globalRowIndex);

                    Cell fioCell = contentRow.createCell(1);
                    fioCell.setCellValue(workingDay.getEmployee().getFullName());
                    List<WorkCalculation> calculations = workingDay.getCalculations().stream().filter(WorkCalculation::isNotEmpty).filter(WorkCalculation::isNotZero).toList();

                    for (WorkCalculation calculation : calculations) {

                        contentRow = sheet.getRow(globalRowIndex);
                        if (contentRow == null) contentRow = sheet.createRow(globalRowIndex);

                        Long taskId = calculation.getWorkLog().getTask().getTaskId();
                        Cell taskIdCell = contentRow.createCell(2);
                        taskIdCell.setCellValue(taskId);

                        List<ActionTaken> actions = calculation.getActions();
                        for (int j = 0; j < actions.size(); j++) {
                            contentRow = sheet.getRow(globalRowIndex + j);
                            if (contentRow == null) contentRow = sheet.createRow(globalRowIndex + j);

                            ActionTaken actionTaken = actions.get(j);

                            Cell actionCell = contentRow.createCell(3);
                            Cell countCell = contentRow.createCell(4);
                            Cell factorCell = contentRow.createCell(5);
                            Cell priceCell = contentRow.createCell(6);

                            actionCell.setCellValue(actionTaken.getPaidAction().getName());
                            float factor = 1.0f;
                            if(calculation.getFactorsActions() != null){
                                factor = calculation.getFactorsActions().stream().filter(fa -> fa.getActionUuids().contains(actionTaken.getUuid())).map(FactorAction::getFactor).findFirst().orElse(1.0f);
                            }
                            factorCell.setCellValue(factor);
                            factorCell.setCellStyle(factorStyle);
                            countCell.setCellValue(actionTaken.getCount());
                            priceCell.setCellValue(actionTaken.getPaidAction().getCost());
                        }

                        globalRowIndex += actions.size();

                        Cell shareCell = contentRow.createCell(7);
                        shareCell.setCellValue(calculation.getRatio());
                        shareCell.setCellStyle(percentStyle);
                        Cell sumCell = contentRow.createCell(8);
                        sumCell.setCellValue(calculation.getSum());
                    }
                    Float allSum = workingDay.getCalculations().stream().map(WorkCalculation::getSum).reduce(Float::sum).orElse(null);
                    if (allSum != null && allSum > 0) {
                        CellRangeAddress region = new CellRangeAddress(globalRowIndex, globalRowIndex, 1, 8);
                        sheet.addMergedRegion(region);
                        contentRow = sheet.getRow(globalRowIndex);
                        if (contentRow == null) contentRow = sheet.createRow(globalRowIndex);
                        Cell allSumCell = contentRow.getCell(1);
                        if(allSumCell == null) allSumCell = contentRow.createCell(1);
                        allSumCell.setCellValue("Итог за день: "+String.format("%.2f", allSum));
                        allSumCell.setCellStyle(allSumStyle);
                        globalRowIndex++;
                    }
                }
            } else {
                globalRowIndex++;
            }

            calendar.add(GregorianCalendar.DATE, 1);
        }

        for (int i = 0; i < headers.size() - 1; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream ms = new ByteArrayOutputStream();
        try {
            workbook.write(ms);
        } catch (Exception e) {
            throw new ResponseException("Не удалось преобразовать тело документа");
        }
        byte b[] = ms.toByteArray();

        SimpleDateFormat fileNameFormatter = new SimpleDateFormat("dd MM yyyy");

        return new MonthlySalaryReportTable("Otchet po montajnikam "+fileNameFormatter.format(startDate)+" - "+fileNameFormatter.format(endDate)+".xlsx", DocumentMimeType.XLSX.value, b);
    }

    public static ConnectionAgreement createConnectionAgreement(@Nullable String login, @Nullable String fullName, @Nullable String dateOfBirth,
                                                                @Nullable String regionOfBirth, @Nullable String cityOfBirth,
                                                                @Nullable PassportDetails passportDetails, @Nullable Address address,
                                                                @Nullable String phone, @Nullable String password, @Nullable String tariff) {
        try {
            InputStream streamTemplate = TDocumentFactory.class.getResourceAsStream("/ConnectionAgreementTemplate.html");
            if(streamTemplate == null) throw new ResponseException("Ресурс шаблона документа не найден");
            Path font = Path.of("./Arial.ttf");
            if(!font.toFile().exists()) throw new ResponseException("Ресурс шрифта документа не найден");
            Document document = Jsoup.parse(streamTemplate, "UTF-8", "");
            document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

            String lastName = "";
            String firstName = "";
            String patronymic = "";
            if(fullName != null) {
                List<String> names = Stream.of(fullName.split(" ")).toList();
                if(names.size() > 0) lastName = names.get(0);
                if(names.size() > 1) firstName = names.get(1);
                if(names.size() > 2) patronymic = names.get(2);
            }

            String passportSeriesStr = "";
            String passportNumberStr = "";
            String passportIssuedByStr = "";
            String passportIssuedDateStr = "";
            String departmentCodeStr = "";
            String registrationAddressStr = "";
            if(passportDetails != null){
                passportSeriesStr = Utils.stringConvertor(passportDetails.getPassportSeries()).orElse("");
                passportNumberStr = Utils.stringConvertor(passportDetails.getPassportNumber()).orElse("");
                passportIssuedByStr = Utils.stringConvertor(passportDetails.getPassportIssuedBy()).orElse("");
                passportIssuedDateStr = Utils.stringConvertor(passportDetails.getPassportIssuedDate()).orElse("");
                departmentCodeStr = Utils.stringConvertor(passportDetails.getDepartmentCode()).orElse("");
                registrationAddressStr = Utils.stringConvertor(passportDetails.getRegistrationAddress()).orElse("");
            }


            String streetStr = "";
            String houseStr = "";
            String apartmentStr = "";
            String entranceStr = "";
            String floorStr = "";

            if(address != null){
                if(address.getStreet() != null) streetStr = Utils.stringConvertor(address.getStreet().getName()).orElse("");
                houseStr = Utils.stringConvertor(address.getHouseNamePart()).orElse("");
                apartmentStr = Utils.stringConvertor(address.getApartmentNum()).orElse("");
                entranceStr = Utils.stringConvertor(address.getEntrance()).orElse("");
                floorStr = Utils.stringConvertor(address.getFloor()).orElse("");
            }

            document.getElementById("current_date").text(String.valueOf(Year.now().getValue()));
            document.getElementById("login_title").text(Utils.stringConvertor(login).orElse("_____________"));
            document.getElementById("lastname").text(lastName);
            document.getElementById("firstname").text(firstName);
            document.getElementById("patronymic").text(patronymic);
            document.getElementById("date_of_birth").text(Utils.stringConvertor(dateOfBirth).orElse(""));
            document.getElementById("region_of_birth").text(Utils.stringConvertor(regionOfBirth).orElse(""));
            document.getElementById("city_of_birth").text(Utils.stringConvertor(cityOfBirth).orElse(""));
            document.getElementById("passport_series").text(passportSeriesStr);
            document.getElementById("passport_number").text(passportNumberStr);
            document.getElementById("passport_issued_by").text(passportIssuedByStr);
            document.getElementById("department_code").text(departmentCodeStr);
            document.getElementById("passport_issued_date").text(passportIssuedDateStr);
            document.getElementById("registration_address").text(registrationAddressStr);
            document.getElementById("street").text(streetStr);
            document.getElementById("house").text(houseStr);
            document.getElementById("apartment").text(apartmentStr);
            document.getElementById("entrance").text(entranceStr);
            document.getElementById("floor").text(floorStr);
            document.getElementById("phone").text(Utils.stringConvertor(phone).orElse(""));
            document.getElementById("login").text(Utils.stringConvertor(login).orElse(""));
            document.getElementById("password").text(Utils.stringConvertor(password).orElse(""));
            document.getElementById("tariff").text(Utils.stringConvertor(tariff).orElse(""));

            ByteArrayOutputStream ms = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer(5.05f, 3);
            SharedContext sharedContext = renderer.getSharedContext();
            sharedContext.setPrint(true);
            sharedContext.setInteractive(false);

            renderer.getFontResolver().addFont(Path.of("./Arial.ttf").toString(), BaseFont.IDENTITY_H, true);
            renderer.setDocumentFromString(document.html());

            renderer.layout();
            renderer.createPDF(ms);
            byte b[] = ms.toByteArray();
            return new ConnectionAgreement("Dogovor na podkluchenie.pdf", DocumentMimeType.PDF.value, b);
        } catch (IOException e) {
            throw new ResponseException("Не удалось прочитать шаблон документа");
        }
    }

    public enum DocumentMimeType{
        XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        PDF("application/pdf");

        private String value;

        DocumentMimeType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
