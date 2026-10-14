package com.fleet.control.vehicles.common.models.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

/** Paginated response wrapper. */
@Schema(description = "Paginated response")
public record PageResponse<T>(
    List<T> content, int page, int size, long totalElements, int totalPages) {

  /** Builds a page response from content and pagination metadata. */
  public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
    int pages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    return new PageResponse<>(content, page, size, totalElements, pages);
  }

  /** Builds a page response from a Spring Data page. */
  public static <T> PageResponse<T> of(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
