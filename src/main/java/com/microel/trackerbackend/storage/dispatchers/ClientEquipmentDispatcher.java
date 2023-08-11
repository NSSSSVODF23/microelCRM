package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.entities.EmployeeIntervention;
import com.microel.trackerbackend.storage.entities.equipment.ClientEquipment;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import com.microel.trackerbackend.storage.repositories.ClientEquipmentRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.Predicate;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class ClientEquipmentDispatcher {
    private final ClientEquipmentRepository clientEquipmentRepository;
    private final StompController stompController;

    public ClientEquipmentDispatcher(ClientEquipmentRepository clientEquipmentRepository, StompController stompController) {
        this.clientEquipmentRepository = clientEquipmentRepository;
        this.stompController = stompController;
    }

    public List<ClientEquipment> get(@Nullable String stringQuery, @Nullable Boolean isDeleted) {
        return clientEquipmentRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if(stringQuery != null && !stringQuery.isBlank()) predicates.add(cb.like(cb.lower(root.get("name")), "%" + stringQuery.toLowerCase() + "%"));
            if(isDeleted != null) predicates.add(cb.equal(root.get("deleted"), isDeleted));
            query.orderBy(cb.desc(root.get("created")));
            return cb.and(predicates.toArray(Predicate[]::new));
        });
    }

    public void create(ClientEquipment.Form form, Employee creator) {
        if(!form.isValid()) throw new IllegalFields("Получены не верные данные для создания клиентского оборудования");
        ClientEquipment clientEquipment = ClientEquipment.builder()
                .name(form.getName())
                .deleted(false)
                .description(form.getDescription())
                .price(form.getPrice())
                .creator(creator)
                .created(Timestamp.from(Instant.now()))
                .editedBy(new ArrayList<>())
                .build();
        stompController.createClientEquipment(clientEquipmentRepository.save(clientEquipment));
    }

    public void edit(Long id, ClientEquipment.Form form, Employee editor) {
        if(!form.isValid()) throw new IllegalFields("Получены не верные данные для редактирования клиентского оборудования");
        ClientEquipment clientEquipment = clientEquipmentRepository.findById(id).orElseThrow(()-> new EntryNotFound("Клиентское оборудование не найденно"));
        if(clientEquipment.getDeleted()) throw new EntryNotFound("Клиентское оборудование удалено");
        clientEquipment.setName(form.getName());
        clientEquipment.setDescription(form.getDescription());
        clientEquipment.setPrice(form.getPrice());
        clientEquipment.addEditedBy(editor, "");
        stompController.updateClientEquipment(clientEquipmentRepository.save(clientEquipment));
    }

    public void delete(Long id, Employee editor) {
        ClientEquipment clientEquipment = clientEquipmentRepository.findById(id).orElseThrow(()-> new EntryNotFound("Клиентское оборудование не найденно"));
        if(clientEquipment.getDeleted()) throw new EntryNotFound("Клиентское оборудование уже удалено");
        clientEquipment.setDeleted(true);
        clientEquipment.addEditedBy(editor, "");
        stompController.deleteClientEquipment(clientEquipmentRepository.save(clientEquipment));
    }
}
