package com.microel.trackerbackend.misc;

import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class CustomContentPage <T> implements Page<T> {

    List<T> newContent;
    Page<?> page;
    int newSize;

    public CustomContentPage(List<T> newContent, Page<?> previousPage, int newSize) {
        this.newContent = newContent;
        this.page = previousPage;
        this.newSize = newSize;
    }


    @Override
    public Iterator<T> iterator() {
        return newContent.iterator();
    }

    @Override
    public int getTotalPages() {
        return page.getTotalPages();
    }

    @Override
    public long getTotalElements() {
        return page.getTotalElements();
    }

    @Override
    public int getNumber() {
        return page.getNumber();
    }

    @Override
    public int getSize() {
        return newSize;
    }

    @Override
    public int getNumberOfElements() {
        return newSize;
    }

    @Override
    public @NonNull List<T> getContent() {
        return newContent;
    }

    @Override
    public boolean hasContent() {
        return true;
    }

    @Override
    public @NonNull Sort getSort() {
        return page.getSort();
    }

    @Override
    public boolean isFirst() {
        return page.isFirst();
    }

    @Override
    public boolean isLast() {
        return page.isLast();
    }

    @Override
    public boolean hasNext() {
        return page.hasNext();
    }

    @Override
    public boolean hasPrevious() {
        return page.hasPrevious();
    }

    @Override
    public @NonNull Pageable nextPageable() {
        return page.nextPageable();
    }

    @Override
    public @NonNull Pageable previousPageable() {
        return page.previousPageable();
    }

    @Override
    public <U> @NonNull Page<U> map(@NonNull Function<? super T, ? extends U> converter) {
        return this.map(converter);
    }
}
