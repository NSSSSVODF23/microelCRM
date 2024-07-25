package com.microel.trackerbackend.misc.autosupport.schema.predicates.impl;

import com.microel.trackerbackend.misc.autosupport.AutoSupportContext;
import com.microel.trackerbackend.misc.autosupport.schema.predicates.IPredicate;
import com.microel.trackerbackend.misc.autosupport.schema.predicates.PredicateEvaluateException;
import com.microel.trackerbackend.misc.autosupport.schema.predicates.PredicateType;
import com.microel.trackerbackend.misc.network.NetworkState;
import com.microel.trackerbackend.services.external.acp.types.DhcpBinding;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.microel.trackerbackend.misc.autosupport.schema.predicates.PredicateType.HAS_ONLINE_HARDWARE;
import static com.microel.trackerbackend.misc.autosupport.schema.predicates.PredicateType.IS_LARGE_UPTIME;

public class IsLargeUptimePredicate implements IPredicate {
    @Override
    public PredicateType type() {
        return IS_LARGE_UPTIME;
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
            final DhcpBinding onlineBinding = dhcpBindings.stream()
                    .filter(dhcpBinding -> Objects.equals(dhcpBinding.getOnlineStatus(), NetworkState.ONLINE))
                    .findFirst().orElse(null);
            if (onlineBinding == null) return false;
            Instant leaseStart = Instant.ofEpochMilli(onlineBinding.getLeaseStart());
            Instant now = Instant.now();
            return Duration.between(leaseStart, now).toDaysPart() > 30;
        } catch (Exception e) {
            throw new PredicateEvaluateException(e.getMessage());
        }
    }
}
