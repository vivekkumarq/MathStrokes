package com.mathstrokes.common.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** Transport-friendly page wrapper so we never serialise Spring's Page implementation directly. */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return from(page, Function.identity());
    }
}
