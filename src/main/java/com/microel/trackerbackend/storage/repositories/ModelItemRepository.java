package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.templating.model.CountItemsByTask;
import com.microel.trackerbackend.storage.entities.templating.model.ModelItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ModelItemRepository extends JpaRepository<ModelItem, Long>, JpaSpecificationExecutor<ModelItem> {
    @Query(value = "select task_task_id as taskId, count(model_item_id)\n"
            + "from models_items\n"
            + "where"
            + " (:strDataId is null or id = :strDataId and string_data = :strData)\n"
            + "   or (:adrDataId is null or id = :adrDataId and f_address_id in :adrData)\n"
            + "group by taskId", nativeQuery = true)
    List<CountItemsByTask> countingFilteredItems(@Param("strDataId") String strDataId, @Param("strData") String strData, @Param("adrDataId") String adrDataId, @Param("adrData") List<Long> adrData);

    List<ModelItem> findAllByTask_TaskId(Long id);
}
