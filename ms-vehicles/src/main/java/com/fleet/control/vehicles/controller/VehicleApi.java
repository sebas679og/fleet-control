package com.fleet.control.vehicles.controller;

import com.fleet.control.vehicles.common.constants.ApiPathConstants;
import com.fleet.control.vehicles.common.exceptions.dto.ErrorResponse;
import com.fleet.control.vehicles.models.dtos.request.CreateVehicleRequest;
import com.fleet.control.vehicles.models.dtos.request.UpdateStatusRequest;
import com.fleet.control.vehicles.models.dtos.request.UpdateVehicleRequest;
import com.fleet.control.vehicles.models.dtos.response.PageResponse;
import com.fleet.control.vehicles.models.dtos.response.VehicleResponse;
import com.fleet.control.vehicles.models.enums.VehicleStatus;
import com.fleet.control.vehicles.models.enums.VehicleType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Vehicle inventory endpoints: create, list, detail and update. */
@RequestMapping(ApiPathConstants.VEHICLE_BASE_PATH)
@Tag(name = "Vehicles", description = "Vehicle inventory operations")
@SecurityRequirement(name = "BearerAuth")
public interface VehicleApi {

  @Operation(
      summary = "Register a vehicle",
      description = "Creates a vehicle always in AVAILABLE status.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Vehicle created successfully",
            content = @Content(schema = @Schema(implementation = VehicleResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "409",
            description = "A vehicle with that plate already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PostMapping
  ResponseEntity<VehicleResponse> create(@Valid @RequestBody CreateVehicleRequest request);

  @Operation(
      summary = "Paginated list of vehicles",
      description = "Filterable by status, type or plate (partial match).")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Paginated vehicles",
            content = @Content(schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid pagination params",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
      })
  @GetMapping
  ResponseEntity<PageResponse<VehicleResponse>> list(
      @Parameter(description = "Filter by status") @RequestParam(required = false)
          VehicleStatus status,
      @Parameter(description = "Filter by type") @RequestParam(required = false, name = "type")
          VehicleType vehicleType,
      @Parameter(description = "Partial plate match") @RequestParam(required = false) String plate,
      @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size);

  @Operation(summary = "Vehicle detail", description = "Returns a vehicle by its ID.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Vehicle found",
            content = @Content(schema = @Schema(implementation = VehicleResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "No vehicle exists with the provided ID",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
      })
  @GetMapping(ApiPathConstants.BY_ID)
  ResponseEntity<VehicleResponse> getById(
      @Parameter(description = "Vehicle identifier") @PathVariable UUID vehicleId);

  @Operation(
      summary = "Update vehicle descriptive data",
      description =
          "Updates make, model, year, type, fuelType and tankCapacityL. "
              + "It does not modify the plate, the status or the odometer.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Vehicle updated",
            content = @Content(schema = @Schema(implementation = VehicleResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "No vehicle exists with the provided ID",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PutMapping(ApiPathConstants.BY_ID)
  ResponseEntity<VehicleResponse> update(
      @Parameter(description = "Vehicle identifier") @PathVariable UUID vehicleId,
      @Valid @RequestBody UpdateVehicleRequest request);

  @Operation(
      summary = "Change vehicle status",
      description =
          "Applies an allowed status transition and optionally advances the odometer. "
              + "Only ADMIN can reactivate OUT_OF_SERVICE to AVAILABLE. "
              + "Internal services authenticate with X-Internal-Key as MANAGER.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Status updated",
            content = @Content(schema = @Schema(implementation = VehicleResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Role without permission (reactivation requires ADMIN)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "No vehicle exists with the provided ID",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "409",
            description = "Transition not allowed or odometer lower than current",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PatchMapping(ApiPathConstants.STATUS)
  ResponseEntity<VehicleResponse> updateStatus(
      @Parameter(description = "Vehicle identifier") @PathVariable UUID vehicleId,
      @Valid @RequestBody UpdateStatusRequest request);
}
