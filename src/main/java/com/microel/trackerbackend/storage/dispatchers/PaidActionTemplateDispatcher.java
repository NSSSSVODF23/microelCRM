package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.storage.entities.salary.PaidAction;
import com.microel.trackerbackend.storage.entities.salary.PaidActionTemplate;
import com.microel.trackerbackend.storage.repositories.PaidActionTemplateRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaidActionTemplateDispatcher {
    private final PaidActionTemplateRepository paidActionTemplateRepository;

    public PaidActionTemplateDispatcher(PaidActionTemplateRepository paidActionTemplateRepository) {
        this.paidActionTemplateRepository = paidActionTemplateRepository;
    }

    public void replaceActualAction(Long oldActionId, PaidAction action){
        List<PaidActionTemplate> templates = paidActionTemplateRepository.findAll(
                (root, query, cb) -> cb.equal(root.join("action").get("paidActionId"), oldActionId)
        );
        templates.forEach(template -> {
            template.setAction(action);
        });
        paidActionTemplateRepository.saveAll(templates);
    }
}
