package com.microel.trackerbackend.services.external.billing;

import com.microel.trackerbackend.misc.ListItem;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public enum BillingPayType {
    BANK(2),
    REFUND(3),
    CREDIT(4),
    SERVICE(11);

    private final Integer value;

    BillingPayType(int id) {
        this.value = id;
    }

    public Integer getValue() {
        return value;
    }

    public String getLabel() {
        return switch (this) {
            case BANK -> "Банк";
            case REFUND -> "Возврат";
            case CREDIT -> "Кредит";
            case SERVICE -> "Служебный";
        };
    }

    public static List<ListItem> getList(){
        return Stream.of(BillingPayType.values()).map(value->new ListItem(value.getLabel(), value.ordinal())).toList();
    }
}
