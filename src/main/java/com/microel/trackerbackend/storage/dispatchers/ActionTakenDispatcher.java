package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.storage.entities.salary.ActionTaken;
import com.microel.trackerbackend.storage.repositories.ActionTakenRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ActionTakenDispatcher {
    private final ActionTakenRepository actionTakenRepository;

    public ActionTakenDispatcher(ActionTakenRepository actionTakenRepository) {
        this.actionTakenRepository = actionTakenRepository;
    }

    public List<ActionTaken> saveAll(List<ActionTaken> actions) {
        return actionTakenRepository.saveAll(actions);
    }
}
