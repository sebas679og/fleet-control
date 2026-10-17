package com.fleet.control.fuel.service;

import com.fleet.control.fuel.models.dto.request.CreateFuelRecordRequest;
import com.fleet.control.fuel.models.dto.response.FuelConsumptionResponse;
import com.fleet.control.fuel.models.dto.response.FuelRecordResponse;
import com.fleet.control.fuel.models.dto.response.FuelStatsResponse;
import com.fleet.control.fuel.models.dto.response.PageResponse;
import com.fleet.control.fuel.models.enums.Granularity;
import java.time.LocalDate;
import java.util.UUID;

/** Refuel registration, history, aggregated consumption and statistics. */
public interface FuelService {

  /** Registers a refuel validating the vehicle, capacity and odometer. */
  FuelRecordResponse create(CreateFuelRecordRequest request);

  /** Lists refuels with optional filters, ordered by refueledAt descending. */
  PageResponse<FuelRecordResponse> list(
      UUID vehicleId, LocalDate from, LocalDate to, int page, int size);

  /** Aggregates consumption per vehicle over a period, ordered by cost descending. */
  FuelConsumptionResponse consumption(LocalDate from, LocalDate to);

  /** Builds the liters, cost and consumption time series for the period. */
  FuelStatsResponse stats(LocalDate from, LocalDate to, Granularity granularity, UUID vehicleId);
}
