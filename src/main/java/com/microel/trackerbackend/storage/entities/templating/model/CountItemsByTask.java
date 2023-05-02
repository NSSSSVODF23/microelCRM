package com.microel.trackerbackend.storage.entities.templating.model;

public interface CountItemsByTask {
    static CountItemsByTask create(Long taskId, Long count) {
        return new CountItemsByTask() {

            @Override
            public Long getCount() {
                return count;
            }

            @Override
            public Long getTaskId() {
                return taskId;
            }
        };
    }

    Long getCount();

    Long getTaskId();

}
