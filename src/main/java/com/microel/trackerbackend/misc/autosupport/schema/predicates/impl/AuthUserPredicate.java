package com.microel.trackerbackend.misc.autosupport.schema.predicates.impl;

import com.microel.trackerbackend.controllers.telegram.UserTelegramController;
import com.microel.trackerbackend.misc.autosupport.AutoSupportContext;
import com.microel.trackerbackend.misc.autosupport.schema.predicates.IPredicate;
import com.microel.trackerbackend.misc.autosupport.schema.predicates.PredicateType;

import java.util.List;
import java.util.Map;

import static com.microel.trackerbackend.misc.autosupport.schema.predicates.PredicateType.AUTH_USER;

public class AuthUserPredicate implements IPredicate {
    @Override
    public PredicateType type() {
        return AUTH_USER;
    }

    @Override
    public List<String> listOfArguments() {
        return List.of("username");
    }

    @Override
    public Boolean evaluate(AutoSupportContext context, Map<String, String> params, Long userId) {
        try {
            context.getUserAccountService().doSaveUserAccount(UserTelegramController.UserTelegramCredentials.from(userId, params.get("username")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
