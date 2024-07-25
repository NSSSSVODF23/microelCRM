package com.microel.trackerbackend.misc.autosupport.schema.predicates.impl;

import com.microel.trackerbackend.misc.autosupport.AutoSupportContext;
import com.microel.trackerbackend.misc.autosupport.schema.predicates.IPredicate;
import com.microel.trackerbackend.misc.autosupport.schema.predicates.PredicateType;

import java.util.List;
import java.util.Map;

import static com.microel.trackerbackend.misc.autosupport.schema.predicates.PredicateType.USER_CREDENTIALS;

public class UserCredentialsPredicate implements IPredicate {
    @Override
    public PredicateType type() {
        return USER_CREDENTIALS;
    }

    @Override
    public List<String> listOfArguments() {
        return List.of("username", "password");
    }

    @Override
    public Boolean evaluate(AutoSupportContext context, Map<String, String> params, Long userId) {
        return context.getUserAccountService().isCredentialsValid(params.get("username"), params.get("password"));
    }
}
