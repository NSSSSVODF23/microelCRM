package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.entities.EmployeeIntervention;
import com.microel.trackerbackend.storage.entities.task.Contract;
import com.microel.trackerbackend.storage.entities.task.TypesOfContracts;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.repositories.ContractRepository;
import com.microel.trackerbackend.storage.repositories.EmployeeRepository;
import com.microel.trackerbackend.storage.repositories.TypesOfContractsRepository;
import com.microel.trackerbackend.storage.repositories.WorkLogRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Transactional(readOnly = true)
public class ContractsDispatcher {
    private final TypesOfContractsRepository typesOfContractsRepository;
    private final ContractRepository contractRepository;
    private final WorkLogRepository workLogRepository;
    private final EmployeeRepository employeeRepository;
    private final StompController stompController;

    public ContractsDispatcher(TypesOfContractsRepository typesOfContractsRepository, ContractRepository contractRepository, WorkLogRepository workLogRepository, EmployeeRepository employeeRepository, StompController stompController) {
        this.typesOfContractsRepository = typesOfContractsRepository;
        this.contractRepository = contractRepository;
        this.workLogRepository = workLogRepository;
        this.employeeRepository = employeeRepository;
        this.stompController = stompController;
    }

    public List<TypesOfContracts> getTypesOfContracts() {
        return typesOfContractsRepository.findAllByIsDeletedIsFalse(Sort.by("name"));
    }

    public List<TypesOfContracts.Suggestion> getTypesOfContractsSuggestions(String stringQuery) {
        return typesOfContractsRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));
            predicates.add(cb.like(cb.lower(root.get("name")), "%" + stringQuery.toLowerCase() + "%"));
            return cb.and(predicates.toArray(Predicate[]::new));
        }, Sort.by("name"))
                .stream()
                .map(TypesOfContracts::toSuggestion)
                .collect(Collectors.toList());
    }

    @Transactional
    public TypesOfContracts createContractType(TypesOfContracts.Form form, Employee createdBy) {
        if(typesOfContractsRepository.existsByName(form.getName()))
            throw new ResponseException("Тип договора с таким именем уже существует");
        List<Employee> receivers = employeeRepository.findAllByLoginIsInAndDeletedIsFalseAndOffsiteIsFalse(new HashSet<>(form.getReceivers()));
        if(receivers.isEmpty())
            throw new ResponseException("Не заданы ответственные за прием договоров");
        List<Employee> archivers = employeeRepository.findAllByLoginIsInAndDeletedIsFalseAndOffsiteIsFalse(new HashSet<>(form.getArchivers()));
        if(archivers.isEmpty())
            throw new ResponseException("Не заданы ответственные за архивацию договоров");
        TypesOfContracts typesOfContracts = new TypesOfContracts();
        typesOfContracts.setName(form.getName());
        typesOfContracts.setDescription(form.getDescription());
        typesOfContracts.setReceivers(receivers);
        typesOfContracts.setArchivers(archivers);
        typesOfContracts.setCreatedBy(EmployeeIntervention.from(createdBy));
        typesOfContracts.setIsDeleted(false);
        typesOfContracts = typesOfContractsRepository.save(typesOfContracts);
        stompController.createTypeOfContract(typesOfContracts);
        return typesOfContracts;
    }

    @Transactional
    public TypesOfContracts updateContractType(Long id, TypesOfContracts.Form form, Employee updatedBy) {
        List<Employee> receivers = employeeRepository.findAllByLoginIsInAndDeletedIsFalseAndOffsiteIsFalse(new HashSet<>(form.getReceivers()));
        if(receivers.isEmpty())
            throw new ResponseException("Не заданы ответственные за прием договоров");
        List<Employee> archivers = employeeRepository.findAllByLoginIsInAndDeletedIsFalseAndOffsiteIsFalse(new HashSet<>(form.getArchivers()));
        if(archivers.isEmpty())
            throw new ResponseException("Не заданы ответственные за архивацию договоров");
        TypesOfContracts typesOfContracts = typesOfContractsRepository.findById(id).orElseThrow(()->new ResponseException("Тип договора не найден"));
        typesOfContracts.setName(form.getName());
        typesOfContracts.setDescription(form.getDescription());
        if(!typesOfContracts.getReceivers().equals(receivers))
            typesOfContracts.setReceivers(receivers);
        if(!typesOfContracts.getArchivers().equals(archivers))
            typesOfContracts.setArchivers(archivers);
        typesOfContracts.getUpdatedByList().add(EmployeeIntervention.from(updatedBy));
        typesOfContracts = typesOfContractsRepository.save(typesOfContracts);
        stompController.updateTypeOfContract(typesOfContracts);
        return typesOfContracts;
    }

    @Transactional
    public void removeContractType(Long id, Employee deletedBy) {
        TypesOfContracts typesOfContracts = typesOfContractsRepository.findById(id).orElseThrow(()->new ResponseException("Тип договора не найден"));
        typesOfContracts.getDeletedByList().add(EmployeeIntervention.from(deletedBy));
        typesOfContracts.setIsDeleted(true);
        stompController.deleteTypeOfContract(typesOfContracts);
    }

    @Transactional
    public void receiveContractCheck(List<Long> contractIds, Employee employee){
        List<Contract> contracts = contractRepository.findAllById(contractIds);
        contracts.forEach(contract -> {
            contract.setReceived(EmployeeIntervention.from(employee));
        });
        contractRepository.saveAll(contracts);
        stompController.updatingMarkedContracts();
    }

    @Transactional
    public void archiveContractCheck(List<Long> contractIds, Employee employee){
        List<Contract> contracts = contractRepository.findAllById(contractIds);
        contracts.forEach(contract -> {
            contract.setArchived(EmployeeIntervention.from(employee));
        });
        contractRepository.saveAll(contracts);
        stompController.updatingMarkedContracts();
    }
}
