package com.microel.trackerbackend.storage.entities.templating.model.dto;

import com.microel.trackerbackend.misc.ListItem;
import com.microel.trackerbackend.services.external.billing.BillingPayType;
import com.microel.trackerbackend.storage.entities.templating.WireframeFieldType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Getter
@Setter
public class FieldItem {
    String name;
    WireframeFieldType type;
    String id;
    String variation;
    Integer listViewIndex;
    Integer orderPosition;
    @Nullable
    DisplayType displayType;

    public FilterModelItem toFilterModelItem() {
        return new FilterModelItem(getId(), getType(), getName());
    }

    public enum DisplayType {
        NONE("NONE"),
        LIST_ONLY("LIST_ONLY"),
        TELEGRAM_ONLY("TELEGRAM_ONLY"),
        LIST_AND_TELEGRAM("LIST_AND_TELEGRAM");

        private final String value;

        DisplayType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return switch (this) {
                case NONE -> "В теле задачи";
                case LIST_ONLY -> "В теле и в списке";
                case TELEGRAM_ONLY -> "В теле и в телеграм";
                case LIST_AND_TELEGRAM -> "Везде";
            };
        }

        public static List<Map<String, String>> getList(){
            return Stream.of(DisplayType.values()).map(value->Map.of("label", value.getLabel(), "value", value.getValue())).toList();
        }
    }
}
