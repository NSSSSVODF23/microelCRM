package com.microel.trackerbackend.misc.autosupport.schema.predicates.impl;

import com.microel.trackerbackend.misc.autosupport.AutoSupportContext;
import com.microel.trackerbackend.misc.autosupport.schema.predicates.IPredicate;
import com.microel.trackerbackend.misc.autosupport.schema.predicates.PredicateType;

import java.util.List;
import java.util.Map;

import static com.microel.trackerbackend.misc.autosupport.schema.predicates.PredicateType.DEFERRED_PAYMENT;

public class DeferredPaymentPredicate implements IPredicate {
    @Override
    public PredicateType type() {
        return DEFERRED_PAYMENT;
    }

    @Override
    public List<String> listOfArguments() {
        return List.of("username");
    }

    @Override
    public Boolean evaluate(AutoSupportContext context, Map<String, String> params, Long userId) {
        try {
            context.getBillingController().deferredPayment(params.get("username"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
