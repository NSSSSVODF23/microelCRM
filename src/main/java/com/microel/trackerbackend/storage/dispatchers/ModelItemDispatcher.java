package com.microel.trackerbackend.storage.dispatchers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.storage.entities.templating.WireframeFieldType;
import com.microel.trackerbackend.storage.entities.templating.model.CountItemsByTask;
import com.microel.trackerbackend.storage.entities.templating.model.ModelItem;
import com.microel.trackerbackend.storage.entities.templating.model.dto.FilterModelItem;
import com.microel.trackerbackend.storage.repositories.ModelItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class ModelItemDispatcher {
    private final ModelItemRepository modelItemRepository;
    private final AddressDispatcher addressDispatcher;

    public ModelItemDispatcher(ModelItemRepository modelItemRepository, AddressDispatcher addressDispatcher) {
        this.modelItemRepository = modelItemRepository;
        this.addressDispatcher = addressDispatcher;
    }

    public List<Long> getTaskIdsByFilters(List<FilterModelItem> filters) {

        // Действительное кол-во примененных фильтров
        AtomicLong acceptedFilters = new AtomicLong();

        // Фильтруем таблицу с данными по задачам
        Page<ModelItem> modelItems = modelItemRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Object, Object> phoneTable = root.join("phoneData", JoinType.LEFT);
            for (FilterModelItem filter : filters) {
                switch (filter.getWireframeFieldType()) {
                    case SMALL_TEXT:
                        if (filter.getValue().asText().isBlank()) break;
                        if (filter.getValue().asText().chars().filter(c -> c == ' ').findFirst().isPresent()) {
                            predicates.add(cb.and(cb.equal(root.get("id"), filter.getId()), cb.isTrue(cb.function("fts", Boolean.class, root.get("stringData"), cb.literal(filter.getValue().asText())))));
                        } else {
                            predicates.add(cb.and(cb.equal(root.get("id"), filter.getId()), cb.like(cb.lower(root.get("stringData")), "%" + filter.getValue().asText().toLowerCase() + "%")));
                        }
                        acceptedFilters.getAndIncrement();
                        break;
                    case LARGE_TEXT:
                        if (filter.getValue().asText().isBlank()) break;
                        predicates.add(cb.and(cb.equal(root.get("id"), filter.getId()), cb.isTrue(cb.function("fts", Boolean.class, root.get("stringData"), cb.literal(filter.getValue().asText())))));
                        acceptedFilters.getAndIncrement();
                        break;
                    case ADDRESS:
                        try {
                            List<Long> adrData = addressDispatcher.getAddressIds(filter);
                            CriteriaBuilder.In<Long> inAddressPred = cb.in(root.join("addressData", JoinType.LEFT).get("addressId"));
                            adrData.forEach(inAddressPred::value);
                            predicates.add(cb.and(cb.equal(root.get("id"), filter.getId()), inAddressPred));
                            acceptedFilters.getAndIncrement();
                        } catch (JsonProcessingException e) {
                            throw new ResponseException("Не верный формат фильтрации адреса");
                        }
                        break;
                    case PHONE_ARRAY:
                        String preparedString = "%" + filter.getValue().asText().replaceAll("\\D", "") + "%";
                        predicates.add(
                                cb.and(
                                        cb.equal(root.get("id"), filter.getId()),
                                        cb.like(
                                                cb.function(
                                                        "REGEXP_REPLACE",
                                                        String.class, phoneTable,
                                                        cb.literal("\\D"),
                                                        cb.literal(""),
                                                        cb.literal('g')),
                                                preparedString)
                                )
                        );
                        acceptedFilters.getAndIncrement();
                        break;
                }
            }
            return cb.or(predicates.toArray(Predicate[]::new));
        }, Pageable.unpaged());

        // Map для подсчета кол-ва полей данных прошедших фильтрацию
        Map<Long, CountItemsByTask> countItemsMap = new HashMap<>();
        // Подсчитываем кол-во
        modelItems.forEach(item -> {
            if (item.getTask() == null) return;
            countItemsMap.compute(item.getTask().getTaskId(), (key, value) -> {
                if (value == null) {
                    return CountItemsByTask.create(key, 1L);
                } else {
                    return CountItemsByTask.create(key, value.getCount() + 1L);
                }
            });
        });
        // Берем значения из Map фильтруем по кол-ву установленных фильтров пользователем и преобразуем в коллекцию идентификаторов задач
        return countItemsMap.values().stream()
                .filter(ct -> {
                    return ct.getCount() == acceptedFilters.get();
                })
                .map(CountItemsByTask::getTaskId).collect(Collectors.toList());
    }

    public List<ModelItem> saveAll(List<ModelItem> modelItems) {
        return modelItemRepository.saveAll(modelItems);
    }

    public List<Long> getTaskIdsByGlobalSearch(String globalSearchValue) {

        // Фильтруем таблицу с данными по задачам
        Page<ModelItem> modelItems = modelItemRepository.findAll((root, query, cb) ->
                cb.and(cb.isTrue(
                        cb.function("fts", Boolean.class, root.get("stringData"), cb.literal(globalSearchValue))
                )), Pageable.unpaged());

        return modelItems.stream().map(modelItem -> modelItem.getTask().getTaskId()).collect(Collectors.toList());
    }

    public List<ModelItem> getFieldsTask(Long id) {
        return modelItemRepository.findAllByTask_TaskId(id);
    }

    public List<ModelItem> prepareModelItems(List<ModelItem> fields) {
        return fields.stream()
                .peek((item) -> {
                    if (item.getWireframeFieldType() == WireframeFieldType.ADDRESS) {
                        item.setAddressData(addressDispatcher.findIdentical(item.getAddressData()));
                    }
                }).collect(Collectors.toList());
    }

    public static List<ModelItem> cleanToCreate(List<ModelItem> fields) {
        return fields.stream().peek(ModelItem::cleanToCreate).collect(Collectors.toList());
    }
}
