package com.microel.trackerbackend.services.api.controllers;

import com.microel.trackerbackend.misc.ListItem;
import com.microel.trackerbackend.services.FilesWatchService;
import com.microel.trackerbackend.services.external.billing.BillingPayType;
import com.microel.trackerbackend.storage.entities.team.notification.NotificationType;
import com.microel.trackerbackend.storage.entities.team.util.PhyPhoneInfo;
import com.microel.trackerbackend.storage.entities.templating.AdvertisingSource;
import com.microel.trackerbackend.storage.entities.templating.ConnectionService;
import com.microel.trackerbackend.storage.entities.templating.ConnectionType;
import com.microel.trackerbackend.storage.entities.templating.WireframeFieldType;
import com.microel.trackerbackend.storage.entities.templating.documents.DocumentTemplate;
import com.microel.trackerbackend.storage.entities.templating.model.dto.FieldItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@Slf4j
@RequestMapping("api/private/types")
public class TypesRequestController {
    @GetMapping("document-template")
    public ResponseEntity<List<Map<String, String>>> getDocumentTemplateTypes() {
        return ResponseEntity.ok(DocumentTemplate.getDocumentTypes());
    }

    @GetMapping("field-display")
    public ResponseEntity<List<Map<String, String>>> getFieldDisplayTypes() {
        return ResponseEntity.ok(FieldItem.DisplayType.getList());
    }

    @GetMapping("wireframe-field")
    public ResponseEntity<List<Map<String, String>>> getWireframeFieldTypes() {
        return ResponseEntity.ok(WireframeFieldType.getList());
    }

    @GetMapping("connection-service")
    public ResponseEntity<List<Map<String, String>>> getConnectionServiceTypes() {
        return ResponseEntity.ok(ConnectionService.getList());
    }

    @GetMapping("connection-type")
    public ResponseEntity<List<Map<String, String>>> getConnectionTypeTypes() {
        return ResponseEntity.ok(ConnectionType.getList());
    }

    @GetMapping("advertising-source")
    public ResponseEntity<List<Map<String, String>>> getAdvertisingSourceTypes() {
        return ResponseEntity.ok(AdvertisingSource.getList());
    }

    @GetMapping("billing-payment-type")
    public ResponseEntity<List<ListItem>> getPaymentTypeTypes() {
        return ResponseEntity.ok(BillingPayType.getList());
    }

    @GetMapping("files-sorting")
    public ResponseEntity<List<Map<String, String>>> getFilesSortingTypes() {
        return ResponseEntity.ok(FilesWatchService.FileSortingTypes.getList());
    }

    @GetMapping("phy-phone-models")
    public ResponseEntity<List<Map<String, String>>> getPhyPhoneModelsTypes() {
        return ResponseEntity.ok(PhyPhoneInfo.PhyPhoneModel.getList());
    }

    @GetMapping("notification")
    public ResponseEntity<List<Map<String, String>>> getNotificationTypes() {
        return ResponseEntity.ok(NotificationType.getList());
    }

    @GetMapping("connection-service/suggestions")
    public ResponseEntity<List<Map<String, String>>> getSuggestionForConnectionService(@Nullable @RequestParam String query) {
        return ResponseEntity.ok(
                ConnectionService.getList()
                        .stream()
                        .filter(service -> service.get("label").toLowerCase().contains(query != null ? query.toLowerCase() : ""))
                        .collect(Collectors.toList())
        );
    }
}
