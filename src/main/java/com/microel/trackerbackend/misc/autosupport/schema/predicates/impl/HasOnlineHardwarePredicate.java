package com.microel.trackerbackend.misc.autosupport.schema.predicates.impl;

import com.microel.trackerbackend.misc.autosupport.AutoSupportContext;
import com.microel.trackerbackend.misc.autosupport.schema.predicates.IPredicate;
import com.microel.trackerbackend.misc.autosupport.schema.predicates.PredicateEvaluateException;
import com.microel.trackerbackend.misc.autosupport.schema.predicates.PredicateType;
import com.microel.trackerbackend.misc.network.NetworkState;
import com.microel.trackerbackend.services.external.acp.types.DhcpBinding;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.microel.trackerbackend.misc.autosupport.schema.predicates.PredicateType.HAS_ONLINE_HARDWARE;

public class HasOnlineHardwarePredicate implements IPredicate {
    @Override
    public PredicateType type() {
        return HAS_ONLINE_HARDWARE;
    }

    @Override
    public List<String> listOfArguments() {
        return List.of("username");
    }

    @Override
    public Boolean evaluate(AutoSupportContext context, Map<String, String> params, Long userId) {
        try {
            final List<DhcpBinding> dhcpBindings = context.getAcpClient().getBindingsByLogin(params.get("username"));
            if (dhcpBindings.isEmpty()) return false;
            return dhcpBindings.stream().anyMatch(dhcpBinding -> Objects.equals(dhcpBinding.getOnlineStatus(), NetworkState.ONLINE) && dhcpBinding.getIsAuth());
        } catch (Exception e) {
            throw new PredicateEvaluateException(e.getMessage());
        }
    }
}
