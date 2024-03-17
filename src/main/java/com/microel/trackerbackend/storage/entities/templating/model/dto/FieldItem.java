package com.microel.trackerbackend.storage.entities.templating.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.misc.ListItem;
import com.microel.trackerbackend.services.external.billing.BillingPayType;
import com.microel.trackerbackend.storage.entities.templating.WireframeFieldType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    @JsonIgnore
    public FilterModelItem toFilterModelItem() {
        return new FilterModelItem(getId(), getType(), getName());
    }

    @JsonIgnore
    public Boolean isRegistryField(String modelItemId){
        if(!Objects.equals(getId(), modelItemId)) return false;
        if(getDisplayType() == null) return true;
        return false;
//        boolean isDisplayNotNone = !Objects.equals(getDisplayType(), FieldItem.DisplayType.NONE);
//        boolean isDisplayNotExceptTheRegistry = !Objects.equals(getDisplayType(), FieldItem.DisplayType.EXCEPT_THE_REGISTRY);
//        return isDisplayNotNone && isDisplayNotExceptTheRegistry;
    }

    @JsonIgnore
    public Boolean isNotLargeField(){
        return !Objects.equals(getType(), WireframeFieldType.LARGE_TEXT) && !Objects.equals(getType(), WireframeFieldType.COUNTING_LIVES);
    }


    public enum DisplayType {
        NONE("NONE"),
        LIST_ONLY("LIST_ONLY"),
        TELEGRAM_ONLY("TELEGRAM_ONLY"),
        EXCEPT_THE_REGISTRY("EXCEPT_THE_REGISTRY"),
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
                case EXCEPT_THE_REGISTRY -> "Везде кроме реестра";
                case LIST_AND_TELEGRAM -> "Везде";
            };
        }

        public static List<Map<String, String>> getList(){
            return Stream.of(DisplayType.values()).map(value->Map.of("label", value.getLabel(), "value", value.getValue())).toList();
        }
    }
}
