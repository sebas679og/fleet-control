package com.fleet.control.drivers.models.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Paginated result wrapper")
public record PageResponse<T>(
    @Schema(description = "Page content") List<T> content,
    @Schema(description = "Current page number (zero-based)", example = "0") int page,
    @Schema(description = "Requested page size", example = "20") int size,
    @Schema(description = "Total number of elements", example = "42") long totalElements,
    @Schema(description = "Total number of pages", example = "3") int totalPages) {
  public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
    int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    return new PageResponse<>(content, page, size, totalElements, totalPages);
  }
}
