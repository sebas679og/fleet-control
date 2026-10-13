package com.fleet.control.drivers.controller;

import com.fleet.control.drivers.commons.constants.ApiPathConstants;
import com.fleet.control.drivers.commons.exception.dto.ErrorResponse;
import com.fleet.control.drivers.commons.models.dtos.request.CreateDriverRequest;
import com.fleet.control.drivers.commons.models.dtos.request.UpdateDriverRequest;
import com.fleet.control.drivers.commons.models.dtos.response.DriverResponse;
import com.fleet.control.drivers.commons.models.dtos.response.PageResponse;
import com.fleet.control.drivers.commons.models.enums.DriverStatus;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Driver management endpoints: create, list, detail and update. */
@RequestMapping(ApiPathConstants.DRIVER_BASE_PATH)
@Tag(name = "Drivers", description = "Driver and license management operations")
@SecurityRequirement(name = "BearerAuth")
public interface DriverApi {

    @Operation(
            summary = "Register a driver",
            description = "Creates a driver always in ACTIVE status.")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Driver created successfully",
                            content = @Content(schema = @Schema(implementation = DriverResponse.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input, including a past license expiry date",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(
                            responseCode = "409",
                            description = "A driver with that license number already exists",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    @PostMapping
    ResponseEntity<DriverResponse> create(@Valid @RequestBody CreateDriverRequest request);

    @Operation(
            summary = "Paginated list of drivers",
            description =
                    "Filterable by status and by license expiry. "
                            + "licenseExpiringInDays returns drivers whose license expires in that many days "
                            + "or fewer, including already expired ones.")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Paginated drivers",
                            content = @Content(schema = @Schema(implementation = PageResponse.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid pagination params",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    @GetMapping
    ResponseEntity<PageResponse<DriverResponse>> list(
            @Parameter(description = "Filter by status") @RequestParam(required = false)
            DriverStatus status,
            @Parameter(
                    description =
                            "Drivers whose license expires in N days or fewer, including expired ones")
            @RequestParam(required = false)
            Integer licenseExpiringInDays,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size);

    @Operation(summary = "Driver detail", description = "Returns a driver by its ID.")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Driver found",
                            content = @Content(schema = @Schema(implementation = DriverResponse.class))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "No driver exists with the provided ID",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    @GetMapping(ApiPathConstants.BY_ID)
    ResponseEntity<DriverResponse> getById(
            @Parameter(description = "Driver identifier") @PathVariable UUID driverId);

    @Operation(
            summary = "Update driver",
            description =
                    "Updates all driver data, including status (ACTIVE, ON_LEAVE, SUSPENDED) "
                            + "and the license expiry date.")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Driver updated",
                            content = @Content(schema = @Schema(implementation = DriverResponse.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "No driver exists with the provided ID",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(
                            responseCode = "409",
                            description = "A driver with that license number already exists",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    @PutMapping(ApiPathConstants.BY_ID)
    ResponseEntity<DriverResponse> update(
            @Parameter(description = "Driver identifier") @PathVariable UUID driverId,
            @Valid @RequestBody UpdateDriverRequest request);
}