package com.microel.trackerbackend.storage;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class OffsetPageable implements Pageable {

    private final Long offset;
    private final Integer limit;
    private Sort sort = Sort.unsorted();

    public OffsetPageable(Long offset, Integer limit) {
        this.offset = offset;
        this.limit = limit;
    }

    public OffsetPageable(Long offset, Integer limit, Sort sort) {
        this.offset = offset;
        this.limit = limit;
        this.sort = sort;
    }

    @Override
    public int getPageNumber() {
        return (int) Math.floorDiv(offset, limit);
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetPageable(offset + limit, limit, sort);
    }

    @Override
    public Pageable previousOrFirst() {
        if (hasPrevious()) {
            return new OffsetPageable(offset - limit, limit, sort);
        } else {
            return first();
        }
    }

    @Override
    public Pageable first() {
        return new OffsetPageable(0L, limit, sort);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetPageable((long) pageNumber * (long) limit, limit, sort);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }

}
