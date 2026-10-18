package com.fleet.control.fuel.controller;

import com.fleet.control.fuel.commons.constants.ApiPathConstants;
import com.fleet.control.fuel.commons.exception.dto.ErrorResponse;
import com.fleet.control.fuel.models.dto.request.CreateFuelRecordRequest;
import com.fleet.control.fuel.models.dto.response.FuelConsumptionResponse;
import com.fleet.control.fuel.models.dto.response.FuelRecordResponse;
import com.fleet.control.fuel.models.dto.response.FuelStatsResponse;
import com.fleet.control.fuel.models.dto.response.PageResponse;
import com.fleet.control.fuel.models.enums.Granularity;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Refuel registration, history, consumption and statistics endpoints. */
@RequestMapping(ApiPathConstants.FUEL_BASE_PATH)
@SecurityRequirement(name = "BearerAuth")
public interface FuelApi {

  @Tag(name = "Refuels")
  @Operation(
      summary = "Register a refuel",
      description = "Records a refuel with capacity and odometer validation.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Refuel registered successfully",
            content = @Content(schema = @Schema(implementation = FuelRecordResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "No vehicle exists with the provided ID",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "409",
            description = "Odometer lower than the previous refuel",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "503",
            description = "ms-vehicles did not respond",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PostMapping(ApiPathConstants.REFUELS)
  ResponseEntity<FuelRecordResponse> create(@Valid @RequestBody CreateFuelRecordRequest request);

  @Tag(name = "Refuels")
  @Operation(
      summary = "Paginated list of refuels",
      description = "Ordered by refueledAt descending.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Paginated refuels",
            content = @Content(schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid pagination params",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
      })
  @GetMapping(ApiPathConstants.REFUELS)
  ResponseEntity<PageResponse<FuelRecordResponse>> list(
      @Parameter(description = "Filter by vehicle") @RequestParam(required = false) UUID vehicle,
      @Parameter(description = "Range start (YYYY-MM-DD)")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate from,
      @Parameter(description = "Range end (YYYY-MM-DD)")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate to,
      @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size);

  @Tag(name = "Fuel consumption")
  @Operation(
      summary = "Aggregated consumption per vehicle",
      description = "Ordered by cost descending. Consumed by the dashboard and alerts.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Consumption per vehicle",
            content = @Content(schema = @Schema(implementation = FuelConsumptionResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid date range",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "503",
            description = "ms-vehicles did not respond",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
      })
  @GetMapping(ApiPathConstants.CONSUMPTION)
  ResponseEntity<FuelConsumptionResponse> consumption(
      @Parameter(description = "Range start (YYYY-MM-DD)")
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate from,
      @Parameter(description = "Range end (YYYY-MM-DD)")
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate to);

  @Tag(name = "Fuel statistics")
  @Operation(
      summary = "Fuel time series",
      description =
          "Liters, cost and average consumption grouped by period. Consumed by the dashboard.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Fuel stats",
            content = @Content(schema = @Schema(implementation = FuelStatsResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid date range",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
      })
  @GetMapping(ApiPathConstants.STATS)
  ResponseEntity<FuelStatsResponse> stats(
      @Parameter(description = "Range start (YYYY-MM-DD)")
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate from,
      @Parameter(description = "Range end (YYYY-MM-DD)")
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate to,
      @Parameter(description = "Granularity") @RequestParam(defaultValue = "DAY")
          Granularity granularity,
      @Parameter(description = "Filter by vehicle") @RequestParam(required = false) UUID vehicle);
}
