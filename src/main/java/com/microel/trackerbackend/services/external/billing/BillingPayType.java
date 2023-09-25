package com.microel.trackerbackend.services.external.billing;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public enum BillingPayType {
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
            case REFUND -> "Возврат";
            case CREDIT -> "Кредит";
            case SERVICE -> "Сервисный";
        };
    }

    public static List<ListItem> getList(){
        return Stream.of(BillingPayType.values()).map(value->new ListItem(value.getLabel(), value.ordinal())).toList();
    }

    public static class ListItem{
        public String label;
        public Integer value;

        public ListItem(String label, Integer value){
            this.label = label;
            this.value = value;
        }
    }
}
