package com.microel.trackerbackend.services.api.controllers;

import com.microel.tdo.chart.TimeDataset;
import com.microel.tdo.dynamictable.TablePaging;
import com.microel.tdo.pon.OpticalLineTerminal;
import com.microel.tdo.pon.Worker;
import com.microel.tdo.pon.events.OntStatusChangeEvent;
import com.microel.tdo.pon.terminal.OpticalNetworkTerminal;
import com.microel.trackerbackend.modules.transport.DateRange;
import com.microel.trackerbackend.parsers.commutator.pon.OltCollectorService;
import com.microel.trackerbackend.services.external.pon.PonextenderClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Queue;
import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("api/private/pon")
public class PonRequestController {
    private final PonextenderClient ponClient;
    private final OltCollectorService oltCollectorService;

    public PonRequestController(PonextenderClient ponClient, OltCollectorService oltCollectorService) {
        this.ponClient = ponClient;
        this.oltCollectorService = oltCollectorService;
    }

    @GetMapping("olt/list")
    public ResponseEntity<List<OpticalLineTerminal>> getOltList() {
        return ResponseEntity.ok(ponClient.getOltList());
    }

    @PostMapping("ont/table")
    public ResponseEntity<Page<OpticalNetworkTerminal>> getOntTable(@RequestBody TablePaging paging) {
        return ResponseEntity.ok(ponClient.getOntTable(paging));
    }

    @GetMapping("ont/{id}")
    public ResponseEntity<OpticalNetworkTerminal> getOnt(@PathVariable Long id) {
        return ResponseEntity.ok(ponClient.getOnt(id));
    }

    @PatchMapping("ont/{id}/rename")
    public ResponseEntity<Void> renameOnt(@PathVariable Long id, @RequestParam String name) {
        ponClient.renameOnt(id, name);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("ont/{id}/assign-login")
    public ResponseEntity<Void> assignLoginToOnt(@PathVariable Long id, @RequestParam String login) {
        ponClient.assignLoginToOnt(id, login);
        return ResponseEntity.ok().build();
    }

    @PostMapping("ont/{id}/reboot")
    public ResponseEntity<Void> rebootOnt(@PathVariable Long id) {
        ponClient.rebootOnt(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("ont/{id}/update")
    public ResponseEntity<UUID> updateOnt(@PathVariable Long id) {
        return ResponseEntity.ok(ponClient.updateOnt(id));
    }

    @PostMapping("ont/{id}/signal-chart")
    public ResponseEntity<List<TimeDataset>> getOntSignalChart(@PathVariable Long id, @RequestBody DateRange timeRange) {
        return ResponseEntity.ok(ponClient.getOntSignalChart(id, timeRange));
    }

    @GetMapping("event/ont/status-change/{offset}")
    public ResponseEntity<Page<OntStatusChangeEvent>> getOntStatusChangeEvents(@PathVariable Integer offset,
                                                                               @RequestParam @Nullable Long oltId,
                                                                               @RequestParam @Nullable Integer port) {
        return ResponseEntity.ok(ponClient.getOntStatusChangeEvents(offset, oltId, port));
    }

    @GetMapping("worker-queue")
    public ResponseEntity<Queue<Worker>> getWorkerQueue() {
        return ResponseEntity.ok(ponClient.getWorkerQueue());
    }

    @GetMapping("suggestions/ont")
    public ResponseEntity<List<OpticalNetworkTerminal>> getOntSuggestions(@RequestParam String query) {
        return ResponseEntity.ok(ponClient.getOntSuggestions(query));
    }

    @GetMapping("ont/login/{login}")
    public ResponseEntity<OpticalNetworkTerminal> getOntByLogin(@PathVariable String login) {
        return ResponseEntity.ok(ponClient.getOntByLogin(login));
    }

    @GetMapping("olt/collect-mac-table")
    public ResponseEntity<Void> collectMacTable() {
        oltCollectorService.collectMacTable();
        return ResponseEntity.ok().build();
    }
}
