package com.microel.trackerbackend.misc.autosupport.schema.predicates;

import com.microel.trackerbackend.misc.autosupport.schema.predicates.impl.*;
import com.microel.trackerbackend.services.api.ResponseException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PredicatesMetadataProvider {

    private final Map<PredicateType, IPredicate> predicatesMap = new EnumMap<>(PredicateType.class);

    public PredicatesMetadataProvider() {
        List<IPredicate> predicates = List.of(
                new UserCredentialsPredicate(),
                new AuthUserPredicate(),
                new PositiveBalancePredicate(),
                new DeferredPaymentPredicate(),
                new HasAuthHardwarePredicate(),
                new HasOnlineHardwarePredicate(),
                new IsLargeUptimePredicate()
        );
        registerPredicates(predicates.toArray(IPredicate[]::new));
    }

    private void registerPredicates(IPredicate... predicates) {
        for (IPredicate predicate : predicates) {
            predicatesMap.put(predicate.type(), predicate);
        }
    }

    public Map<PredicateType, List<String>> getAllArguments() {
        Map<PredicateType, List<String>> argumentsMap = new EnumMap<>(PredicateType.class);
        for (PredicateType predicateType : PredicateType.values()) {
            IPredicate predicate = predicatesMap.get(predicateType);
            if (predicate == null)
                throw new ResponseException(predicateType.getValue() + " не зарегистрирован в PredicatesMetadataProvider");
            argumentsMap.put(predicateType, predicate.listOfArguments());
        }
        return argumentsMap;
    }
}
