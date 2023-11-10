package com.microel.trackerbackend.misc.sorting;

public enum TaskJournalSortingTypes {

    CREATE_DATE_ASC("CREATE_DATE_ASC"),
    CREATE_DATE_DESC("CREATE_DATE_DESC");

    private final String value;

    TaskJournalSortingTypes(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
