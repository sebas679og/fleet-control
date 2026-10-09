package com.fleet.control.vehicles.common.models.dtos.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse <T>(

        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages

) {
    public static <T> PageResponse<T> of(List<T> content, int page,
                                         int size, long totalElements)
    {
        int pages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, pages);
    }

    public static <T> PageResponse<T> of(Page<T> page)
    {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
