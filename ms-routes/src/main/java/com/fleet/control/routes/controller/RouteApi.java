package com.fleet.control.routes.controller;

import com.fleet.control.routes.commons.constants.ApiPathConstants;
import com.fleet.control.routes.commons.exception.dto.ErrorResponse;
import com.fleet.control.routes.models.dtos.request.CompleteRouteRequest;
import com.fleet.control.routes.models.dtos.request.CreateRouteRequest;
import com.fleet.control.routes.models.dtos.response.PageResponse;
import com.fleet.control.routes.models.dtos.response.RouteResponse;
import com.fleet.control.routes.models.dtos.response.RouteStatsResponse;
import com.fleet.control.routes.models.enums.Granularity;
import com.fleet.control.routes.models.enums.RouteStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Route planning, execution and history endpoints. */
@RequestMapping(ApiPathConstants.ROUTE_BASE_PATH)
@Tag(name = "Routes", description = "Route planning, execution and history operations")
@SecurityRequirement(name = "BearerAuth")
public interface RouteApi {

  @Operation(
      summary = "Plan a route",
      description = "Assigns a vehicle and a driver after business-rule validation.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Route planned successfully",
            content = @Content(schema = @Schema(implementation = RouteResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Vehicle or driver not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "409",
            description = "Vehicle not available, driver not eligible or overlap",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "503",
            description = "Downstream service unavailable",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PostMapping
  ResponseEntity<RouteResponse> create(@Valid @RequestBody CreateRouteRequest request);

  @Operation(
      summary = "Paginated list of routes",
      description = "Ordered by plannedStart descending.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Paginated routes",
            content = @Content(schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid pagination params",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
      })
  @GetMapping
  ResponseEntity<PageResponse<RouteResponse>> list(
      @Parameter(description = "Filter by vehicle") @RequestParam(required = false) UUID vehicle,
      @Parameter(description = "Filter by driver") @RequestParam(required = false) UUID driver,
      @Parameter(description = "Filter by status") @RequestParam(required = false)
          RouteStatus status,
      @Parameter(description = "Filter by plannedStart from (YYYY-MM-DD)")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate from,
      @Parameter(description = "Filter by plannedStart to (YYYY-MM-DD)")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate to,
      @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size);

  @Operation(summary = "Route detail", description = "Returns a route by its ID.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Route found",
            content = @Content(schema = @Schema(implementation = RouteResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "No route exists with the provided ID",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
      })
  @GetMapping(ApiPathConstants.BY_ID)
  ResponseEntity<RouteResponse> getById(
      @Parameter(description = "Route identifier") @PathVariable UUID routeId);

  @Operation(
      summary = "Start a planned route",
      description = "Moves the route to IN_PROGRESS and the vehicle to IN_USE.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Route started",
            content = @Content(schema = @Schema(implementation = RouteResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "No route exists with the provided ID",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "409",
            description = "Route not planned or vehicle not available",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "503",
            description = "Downstream service unavailable",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PostMapping(ApiPathConstants.START)
  ResponseEntity<RouteResponse> start(
      @Parameter(description = "Route identifier") @PathVariable UUID routeId);

  @Operation(
      summary = "Complete a route in progress",
      description = "Moves the route to COMPLETED and releases the vehicle as AVAILABLE.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Route completed",
            content = @Content(schema = @Schema(implementation = RouteResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "No route exists with the provided ID",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "409",
            description = "Route not in progress or odometer conflict",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PostMapping(ApiPathConstants.COMPLETE)
  ResponseEntity<RouteResponse> complete(
      @Parameter(description = "Route identifier") @PathVariable UUID routeId,
      @Valid @RequestBody CompleteRouteRequest request);

  @Operation(
      summary = "Route time series",
      description = "Completed routes grouped by end date. Consumed by the dashboard.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Route stats",
            content = @Content(schema = @Schema(implementation = RouteStatsResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid date range",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
      })
  @GetMapping(ApiPathConstants.STATS)
  ResponseEntity<RouteStatsResponse> stats(
      @Parameter(description = "Start date (YYYY-MM-DD)")
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate from,
      @Parameter(description = "End date (YYYY-MM-DD)")
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate to,
      @Parameter(description = "Granularity") @RequestParam(defaultValue = "DAY")
          Granularity granularity,
      @Parameter(description = "Filter by vehicle") @RequestParam(required = false) UUID vehicle);
}
