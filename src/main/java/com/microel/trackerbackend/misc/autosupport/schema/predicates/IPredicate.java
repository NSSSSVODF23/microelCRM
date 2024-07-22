package com.microel.trackerbackend.misc.autosupport.schema.predicates;

import com.microel.trackerbackend.misc.autosupport.AutoSupportContext;

import java.util.List;
import java.util.Map;

public interface IPredicate {
    PredicateType type();
    List<String> listOfArguments();
    Boolean evaluate(AutoSupportContext context, Map<String, String> params, Long userId);
}
