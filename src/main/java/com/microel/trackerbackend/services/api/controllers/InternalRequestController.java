package com.microel.trackerbackend.services.api.controllers;

import com.microel.tdo.pon.Worker;
import com.microel.tdo.pon.events.OntStatusChangeEvent;
import com.microel.tdo.pon.terminal.OpticalNetworkTerminal;
import com.microel.trackerbackend.controllers.telegram.TelegramController;
import com.microel.trackerbackend.services.api.StompController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@Slf4j
@RequestMapping("api/internal")
public class InternalRequestController {
    private final StompController stompController;
    private final TelegramController telegramController;

    public InternalRequestController(StompController stompController, TelegramController telegramController) {
        this.stompController = stompController;
        this.telegramController = telegramController;
    }


    @PostMapping("pon/event/ont/status/change")
    public ResponseEntity<Void> receiveOntStatusChangeEvent(@RequestBody List<OntStatusChangeEvent> events) {
        stompController.sendNewOntStatusChangeEvents(events);
        telegramController.sendOntEvents(events);
        return ResponseEntity.ok().build();
    }

    @PostMapping("pon/worker/new")
    public ResponseEntity<Void> receiveNewWorker(@RequestBody Worker worker) {
        stompController.sendNewWorkerInQueue(worker);
        return ResponseEntity.ok().build();
    }

    @PostMapping("pon/worker/spent")
    public ResponseEntity<Void> receiveSpentWorker(@RequestBody Worker worker) {
        stompController.sendSpentWorkerInQueue(worker);
        return ResponseEntity.ok().build();
    }

    @PostMapping("pon/ont/update")
    public ResponseEntity<Void> receiveSpentWorker(@RequestBody OpticalNetworkTerminal ont) {
        stompController.sendUpdatedOnt(ont);
        return ResponseEntity.ok().build();
    }
}
