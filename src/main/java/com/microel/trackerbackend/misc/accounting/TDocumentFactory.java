package com.microel.trackerbackend.misc.accounting;

import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.storage.entities.salary.ActionTaken;
import com.microel.trackerbackend.storage.entities.salary.WorkCalculation;
import com.microel.trackerbackend.storage.entities.salary.WorkingDay;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class TDocumentFactory {

    public static MonthlySalaryReportTable createMonthlySalaryReportTable(Map<Date, List<WorkingDay>> workingDaysByOffsiteEmployees, Date startDate, Date endDate) {

        Workbook workbook = new XSSFWorkbook();

        short percentFormat = workbook.createDataFormat().getFormat("0.00%");

        CellStyle percentStyle = workbook.createCellStyle();
        percentStyle.setDataFormat(percentFormat);

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
        List<String> headers = List.of("Дата", "ФИО", "ID Задачи", "Действие", "Кол-во", "Цена", "Доля", "Сумма");
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
                            Cell priceCell = contentRow.createCell(5);

                            actionCell.setCellValue(actionTaken.getPaidAction().getName());
                            countCell.setCellValue(actionTaken.getCount());
                            priceCell.setCellValue(actionTaken.getPaidAction().getCost());
                        }

                        globalRowIndex += actions.size();

                        Cell shareCell = contentRow.createCell(6);
                        shareCell.setCellValue(calculation.getRatio());
                        shareCell.setCellStyle(percentStyle);
                        Cell sumCell = contentRow.createCell(7);
                        sumCell.setCellValue(calculation.getSum());
                    }
                    Float allSum = workingDay.getCalculations().stream().map(WorkCalculation::getSum).reduce(Float::sum).orElse(null);
                    if (allSum != null) {
                        CellRangeAddress region = new CellRangeAddress(globalRowIndex, globalRowIndex, 1, 7);
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

    public enum DocumentMimeType{
        XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        private String value;

        DocumentMimeType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
