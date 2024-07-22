package com.microel.trackerbackend.misc.autosupport.schema.predicates;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PredicatesMetadataProvider {

    private final Map<PredicateType, IPredicate> predicatesMap = new HashMap<>();

    public PredicatesMetadataProvider() {
        List<IPredicate> predicates = List.of(
                new UserCredentialsPredicate(),
                new AuthUserPredicate()
        );
        registerPredicates(predicates.toArray(IPredicate[]::new));
    }

    private void registerPredicates(IPredicate... predicates) {
        for (IPredicate predicate : predicates) {
            predicatesMap.put(predicate.type(), predicate);
        }
    }

    public Map<PredicateType, List<String>> getAllArguments() {
        Map<PredicateType, List<String>> argumentsMap = new HashMap<>();
        for (PredicateType predicateType : PredicateType.values()) {
            IPredicate predicate = predicatesMap.get(predicateType);
            if (predicate == null)
                throw new RuntimeException(predicateType.getValue() + " не зарегистрирован в PredicatesMetadataProvider");
            argumentsMap.put(predicateType, predicate.listOfArguments());
        }
        return argumentsMap;
    }
}
