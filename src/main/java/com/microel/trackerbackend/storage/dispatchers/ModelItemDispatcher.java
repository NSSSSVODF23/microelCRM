package com.microel.trackerbackend.storage.dispatchers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.microel.trackerbackend.misc.task.filtering.fields.types.TaskFieldFilter;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.storage.entities.address.Address;
import com.microel.trackerbackend.storage.entities.storehouse.EquipmentRealization;
import com.microel.trackerbackend.storage.entities.templating.ConnectionService;
import com.microel.trackerbackend.storage.entities.templating.PassportDetails;
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
import java.util.*;
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

    public List<Long> getTaskIdsByTaskFilters(List<TaskFieldFilter> filters){
        List<ModelItem> modelItems = modelItemRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = filters.stream().map(filter -> filter.toPredicate(root, cb)).filter(Objects::nonNull).toList();
            query.distinct(true);
            return cb.or(predicates.toArray(Predicate[]::new));
        });
        Map<Long, Long> groupedByTask = modelItems.stream().collect(Collectors.groupingBy(mi -> mi.getTask().getTaskId(), Collectors.counting()));
        return groupedByTask.entrySet().stream().filter(e -> e.getValue() == filters.stream().filter(TaskFieldFilter::nonEmpty).count()).map(Map.Entry::getKey).toList();
    }

    public List<Long> getTaskIdsByFilters(List<FilterModelItem> filters) {

        // Действительное кол-во примененных фильтров
        AtomicLong acceptedFilters = new AtomicLong();

    // todo Для добавления типа поля, нужно добавить сюда3
        // Фильтруем таблицу с данными по задачам
        Page<ModelItem> modelItems = modelItemRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Object, Object> phoneTable = root.join("phoneData", JoinType.LEFT);
            for (FilterModelItem filter : filters) {
                if(filter.getValue() == null) continue;
                switch (filter.getWireframeFieldType()) {
                    case BOOLEAN:
                        predicates.add(cb.and(cb.equal(root.get("id"), filter.getId()), cb.equal(root.get("booleanData"), (Boolean) filter.getValue())));
                        acceptedFilters.getAndIncrement();
                        break;
                    case INTEGER:
                        predicates.add(cb.and(cb.equal(root.get("id"), filter.getId()),cb.equal(root.get("integerData"), (Integer) filter.getValue())));
                        acceptedFilters.getAndIncrement();
                        break;
                    case FLOAT:
                        predicates.add(cb.and(cb.equal(root.get("id"), filter.getId()),cb.equal(root.get("floatData"), (Double) filter.getValue())));
                        acceptedFilters.getAndIncrement();
                        break;
                    case AD_SOURCE:
                    case CONNECTION_TYPE:
                        predicates.add(cb.and(cb.equal(root.get("id"), filter.getId()),cb.equal(root.get("stringData"), (String) filter.getValue())));
                        acceptedFilters.getAndIncrement();
                        break;
                    case LOGIN:
                    case IP:
                    case REQUEST_INITIATOR:
                    case SMALL_TEXT:
                        if (((String) filter.getValue()).isBlank()) break;
                        if (((String) filter.getValue()).chars().filter(c -> c == ' ').findFirst().isPresent()) {
                            predicates.add(cb.and(cb.equal(root.get("id"), filter.getId()), cb.isTrue(cb.function("fts", Boolean.class, root.get("stringData"), cb.literal((String) filter.getValue())))));
                        } else {
                            predicates.add(cb.and(cb.equal(root.get("id"), filter.getId()), cb.like(cb.lower(root.get("stringData")), "%" + ((String) filter.getValue()).toLowerCase() + "%")));
                        }
                        acceptedFilters.getAndIncrement();
                        break;
                    case COUNTING_LIVES:
                    case LARGE_TEXT:
                        if (((String) filter.getValue()).isBlank()) break;
                        predicates.add(cb.and(cb.equal(root.get("id"), filter.getId()), cb.isTrue(cb.function("fts", Boolean.class, root.get("stringData"), cb.literal((String) filter.getValue())))));
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
                        String preparedString = "%" + ((String) filter.getValue()).replaceAll("\\D", "") + "%";
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
                    case CONNECTION_SERVICES:
                        try {
                            List<LinkedHashMap> servicesArray = (List<LinkedHashMap>) filter.getValue();
                            if(servicesArray.isEmpty()) break;
                            List<ConnectionService> serviceNamesList = servicesArray.stream().map(serv -> (String) serv.get("connectionService")).filter(Objects::nonNull).map(ConnectionService::valueOf).toList();
                            predicates.add(cb.and(cb.equal(root.get("id"), filter.getId()),root.join("connectionServicesData", JoinType.LEFT).get("connectionService").in(serviceNamesList)));
                            acceptedFilters.getAndIncrement();
                        }catch (Throwable e){
                            break;
                        }
                        break;
                    case EQUIPMENTS:
                        try {
                            List<LinkedHashMap> equipmentsArray = (List<LinkedHashMap>) filter.getValue();
                            if(equipmentsArray.isEmpty()) break;
                            List<Long> equipmentsIdsList = equipmentsArray.stream().map(serv -> (Integer) serv.get("equipmentRealization")).filter(Objects::nonNull).map(Integer::longValue).toList();
                            predicates.add(cb.and(cb.equal(root.get("id"), filter.getId()),root.join("equipmentRealizationsData", JoinType.LEFT).join("equipment", JoinType.LEFT).get("clientEquipmentId").in(equipmentsIdsList)));
                            acceptedFilters.getAndIncrement();
                        }catch (Throwable e){
                            break;
                        }
                        break;
                    case PASSPORT_DETAILS:
                        if (filter.getValue() == null || ((String) filter.getValue()).isBlank()) break;
                        String[] splitQuery = ((String) filter.getValue()).split("-", 2);
                        List<String> queries = Arrays.stream(splitQuery).map(q -> "%" + q.toLowerCase() + "%").toList();
                        Join<ModelItem, PassportDetails> passportDetailsJoin = root.join("passportDetailsData", JoinType.LEFT);
                        List<Predicate> localPredicates = new ArrayList<>();
                        if(queries.isEmpty()) break;
                        try{
                            String pSer = queries.get(0);
                            localPredicates.add(cb.like(cb.lower(passportDetailsJoin.get("passportSeries")), pSer));
                        }catch (Throwable ignore){}
                        try{
                            String pNum = queries.get(1);
                            localPredicates.add(cb.like(cb.lower(passportDetailsJoin.get("passportNumber")), pNum));
                        }catch (Throwable ignore){}
                        predicates.add(cb.and(localPredicates.toArray(Predicate[]::new)));
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
        List<ModelItem> modelItems = modelItemRepository.findAll((root, query, cb) ->
                cb.and(cb.isTrue(
                        cb.function("fts", Boolean.class, root.get("stringData"), cb.literal(globalSearchValue))
                )));

        return modelItems.stream().map(modelItem -> modelItem.getTask().getTaskId()).collect(Collectors.toList());
    }

    public List<Long> getTaskIdsByAddresses(List<Address> addresses) {

        List<ModelItem> modelItems = modelItemRepository.findAll((root, query, cb) -> {
            Join<ModelItem, Address> addressJoin = root.join("addressData", JoinType.LEFT);
            return cb.and(addressJoin.in(addresses));
        });

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
                    if(item.getWireframeFieldType() == WireframeFieldType.CONNECTION_SERVICES){
                        item.setConnectionServicesData(item.getConnectionServicesData().stream()
                                .peek(cs-> cs.setDataConnectionServiceId(null)).collect(Collectors.toList()));
                    }
                }).collect(Collectors.toList());
    }

    public static List<ModelItem> cleanToCreate(List<ModelItem> fields) {
        return fields.stream().peek(ModelItem::cleanToCreate).collect(Collectors.toList());
    }


}
