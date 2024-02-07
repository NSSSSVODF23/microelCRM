package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.task.TypesOfContracts;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TypesOfContractsRepository extends JpaRepository<TypesOfContracts, Long>, JpaSpecificationExecutor<TypesOfContracts> {
    boolean existsByName(String name);

    List<TypesOfContracts> findAllByIsDeletedIsFalse(Sort sort);

    List<TypesOfContracts> findAllByIsDeletedIsFalseAndNameContains(String stringQuery, Sort sort);
}
