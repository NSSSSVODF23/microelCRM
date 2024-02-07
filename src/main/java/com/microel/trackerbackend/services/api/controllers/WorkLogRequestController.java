package com.microel.trackerbackend.services.api.controllers;

import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.services.external.billing.ApiBillingController;
import com.microel.trackerbackend.storage.dispatchers.EmployeeDispatcher;
import com.microel.trackerbackend.storage.dispatchers.WorkLogDispatcher;
import com.microel.trackerbackend.storage.entities.comments.Attachment;
import com.microel.trackerbackend.storage.entities.comments.FileType;
import com.microel.trackerbackend.storage.entities.filesys.TFile;
import com.microel.trackerbackend.storage.entities.task.TypesOfContracts;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import com.microel.trackerbackend.storage.entities.task.WorkLogTargetFile;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.repositories.ModelItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Controller
@Slf4j
@RequestMapping("api/private/work-log")
public class WorkLogRequestController {

    private final ApiBillingController apiBillingController;
    private final EmployeeDispatcher employeeDispatcher;
    private final ModelItemRepository modelItemRepository;
    private final StompController stompController;
    private final WorkLogDispatcher workLogDispatcher;

    public WorkLogRequestController(ApiBillingController apiBillingController, EmployeeDispatcher employeeDispatcher,
                                    ModelItemRepository modelItemRepository, StompController stompController, WorkLogDispatcher workLogDispatcher) {
        this.apiBillingController = apiBillingController;
        this.employeeDispatcher = employeeDispatcher;
        this.modelItemRepository = modelItemRepository;
        this.stompController = stompController;
        this.workLogDispatcher = workLogDispatcher;
    }

    // Получить WorkLog по его идентификатору
    @GetMapping("{workLogId}")
    public ResponseEntity<WorkLog> getWorkLog(@PathVariable Long workLogId) {
        try {
            return ResponseEntity.ok(workLogDispatcher.get(workLogId));
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Получить журналы работ определенной задачи
    @GetMapping("task/{taskId}/list")
    public ResponseEntity<List<WorkLog>> getWorkLogs(@PathVariable Long taskId) {
        return ResponseEntity.ok(workLogDispatcher.getAllByTaskId(taskId));
    }

    /**
     *Получить список активных журналов работ
     */
    @GetMapping("active/list")
    public ResponseEntity<List<WorkLog>> getActiveWorkLogs() {
        return ResponseEntity.ok(workLogDispatcher.getActive());
    }

    // Получить количество активных журналов работ
    @GetMapping("active/count")
    public ResponseEntity<Long> getActiveWorkLogsCount() {
        return ResponseEntity.ok(workLogDispatcher.getActiveCount());
    }

    @GetMapping("task/{taskId}/active")
    public ResponseEntity<WorkLog> getActiveWorkLogsByTask(@PathVariable Long taskId) {
        try {
            return ResponseEntity.ok(workLogDispatcher.getActiveByTaskId(taskId));
        } catch (EntryNotFound e) {
            return ResponseEntity.ok(null);
        }
    }

    @GetMapping("uncalculated/list")
    public ResponseEntity<List<WorkLog>> getUncalculatedWorkLogs() {
        return ResponseEntity.ok(workLogDispatcher.getUncalculated());
    }

    @GetMapping("after-work/list")
    public ResponseEntity<List<WorkLog>> getAfterWorkList(HttpServletRequest request) {
        return ResponseEntity.ok(workLogDispatcher.getAfterWork(employeeDispatcher.getEmployeeFromRequest(request)));
    }

    @PatchMapping("{id}/mark-as-completed")
    public ResponseEntity<WorkLog> markWorkLogAsCompleted(@PathVariable Long id, @RequestBody @Nullable List<TypesOfContracts.Suggestion> contracts) {
        return ResponseEntity.ok(workLogDispatcher.markAsCompleted(id, contracts));
    }

    @PatchMapping("{id}/mark-as-uncompleted")
    public ResponseEntity<WorkLog> markWorkLogAsUncompleted(@PathVariable Long id) {
        return ResponseEntity.ok(workLogDispatcher.markAsUncompleted(id));
    }

    @PatchMapping("{id}/mark-as-uncompleted-and-close")
    public ResponseEntity<WorkLog> markWorkLogAsUncompletedAndClose(@PathVariable Long id) {
        return ResponseEntity.ok(workLogDispatcher.markAsUncompletedAndClose(id));
    }

    @PostMapping("unconfirmed-contracts/{page}")
    public ResponseEntity<Page<WorkLog>> getPageOfConfirmationOfContracts(
            @PathVariable Integer page,
            @RequestBody WorkLogDispatcher.ContractConfirmationFilters filters,
            HttpServletRequest request
    ){
        return ResponseEntity.ok(workLogDispatcher.getPageOfConfirmationOfContracts(page, filters, employeeDispatcher.getEmployeeFromRequest(request)));
    }

//    @GetMapping("unconfirmed-contracts/test-notification")
//    public ResponseEntity<Void> testNotification() {
//        workLogDispatcher.notificationOfUnrecievedContracts();
//        return ResponseEntity.ok().build();
//    }

    @GetMapping("employee-work-log/list")
    public ResponseEntity<List<WorkLogDispatcher.EmployeeWorkLogs>> getEmployeeWorkLogs(HttpServletRequest request) {
        return ResponseEntity.ok(workLogDispatcher.getEmployeeWorkLogList(employeeDispatcher.getEmployeeFromRequest(request)));
    }

    @GetMapping("target-file/{id}")
    public void getTargetFile(@PathVariable Long id,
                         @RequestHeader(value = "Range", required = false) String rangeHeader,
                         HttpServletResponse response) {
        WorkLogTargetFile targetFile = workLogDispatcher.getTargetFileById(id);

        try {
            OutputStream os = response.getOutputStream();

            // Получаем размер фала
            long fileSize = Files.size(Path.of(targetFile.getPath()));

            byte[] buffer = new byte[1024];

            try (RandomAccessFile file = new RandomAccessFile(targetFile.getPath(), "r")) {
                if (rangeHeader == null) {
                    response.setHeader("Content-Type", targetFile.getMimeType());
                    response.setHeader("Content-Length", String.valueOf(fileSize));
                    response.setHeader("Content-Disposition", "inline;filename="+targetFile.getName());

                    response.setStatus(HttpStatus.OK.value());
                    long pos = 0;
                    file.seek(pos);
                    while (pos < fileSize - 1) {
                        file.read(buffer);
                        os.write(buffer);
                        pos += buffer.length;
                    }
                    os.flush();
                    return;
                }

                String[] ranges = rangeHeader.split("-");
                long rangeStart = Long.parseLong(ranges[0].substring(6));
                long rangeEnd;
                if (ranges.length > 1) {
                    rangeEnd = Long.parseLong(ranges[1]);
                } else {
                    rangeEnd = fileSize - 1;
                }
                if (fileSize < rangeEnd) {
                    rangeEnd = fileSize - 1;
                }

                String contentLength = String.valueOf((rangeEnd - rangeStart) + 1);
                response.setHeader("Content-Type", targetFile.getMimeType());
                response.setHeader("Content-Length", contentLength);
                response.setHeader("Accept-Ranges", "bytes");
                response.setHeader("Content-Range", "bytes" + " " + rangeStart + "-" + rangeEnd + "/" + fileSize);
                response.setHeader("Content-Disposition", "inline;filename="+targetFile.getName());
                response.setStatus(HttpStatus.PARTIAL_CONTENT.value());
                long pos = rangeStart;
                file.seek(pos);
                while (pos < rangeEnd) {
                    file.read(buffer);
                    os.write(buffer);
                    pos += buffer.length;
                }
                os.flush();


            } catch (FileNotFoundException e) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
            }

        } catch (IOException e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @GetMapping("thumbnail/{id}")
    public void getTargetFileThumbnail(@PathVariable Long id, HttpServletResponse response) {
        WorkLogTargetFile targetFile = workLogDispatcher.getTargetFileById(id);

        if (targetFile.getType() == FileType.PHOTO || targetFile.getType() == FileType.VIDEO) {
            if (targetFile.getThumbnail() == null) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
                return;
            }
            Path filePath = Path.of(targetFile.getThumbnail());
            try (InputStream inputStream = Files.newInputStream(filePath)) {
                long size = Files.size(filePath);
                response.setHeader("Content-Type", "image/jpeg");
                response.setHeader("Content-Length", String.valueOf(size));
                response.setStatus(HttpStatus.OK.value());
                inputStream.transferTo(response.getOutputStream());
            } catch (IOException e) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
            }
        }else{
            response.setStatus(HttpStatus.NOT_FOUND.value());
        }
    }

}
