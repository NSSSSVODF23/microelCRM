package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.storehouse.ItemsGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ItemsGroupRepository extends JpaRepository<ItemsGroup, Long>, JpaSpecificationExecutor<ItemsGroup> {
}
