package com.fleet.control.fuel.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fleet.control.fuel.commons.client.VehiclesClient;
import com.fleet.control.fuel.commons.client.dto.VehicleRef;
import com.fleet.control.fuel.models.dto.request.CreateFuelRecordRequest;
import com.fleet.control.fuel.models.dto.response.FuelConsumptionResponse;
import com.fleet.control.fuel.models.dto.response.FuelRecordResponse;
import com.fleet.control.fuel.models.dto.response.FuelStatsResponse;
import com.fleet.control.fuel.models.dto.response.PageResponse;
import com.fleet.control.fuel.models.entities.FuelRecord;
import com.fleet.control.fuel.models.enums.FuelType;
import com.fleet.control.fuel.models.enums.Granularity;
import com.fleet.control.fuel.models.mappers.FuelRecordMapper;
import com.fleet.control.fuel.repository.FuelRepository;
import com.fleet.control.fuel.service.impl.FuelReporting;
import com.fleet.control.fuel.service.impl.FuelServiceImpl;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

/** Happy-path unit tests for refuel registration, history, consumption and stats. */
@ExtendWith(MockitoExtension.class)
class FuelServiceImplTest {

  private static final Instant DAY_ONE = Instant.parse("2026-10-01T08:00:00Z");
  private static final Instant DAY_TWO = Instant.parse("2026-10-02T08:00:00Z");
  private static final Instant DAY_THREE = Instant.parse("2026-10-03T08:00:00Z");

  @Mock private FuelRepository fuelRepository;
  @Mock private FuelRecordMapper fuelRecordMapper;
  @Mock private VehiclesClient vehiclesClient;

  private FuelServiceImpl fuelService;

  @BeforeEach
  void setUp() {
    fuelService =
        new FuelServiceImpl(
            fuelRepository, fuelRecordMapper, vehiclesClient, new FuelReporting(vehiclesClient));
  }

  private static VehicleRef dieselVehicle() {
    return new VehicleRef("2137KLM", "CAR", "DIESEL", 80, 45500, "AVAILABLE");
  }

  private static CreateFuelRecordRequest fullTankRequest(UUID vehicleId) {
    return new CreateFuelRecordRequest(
        vehicleId, DAY_TWO, new BigDecimal("38"), new BigDecimal("1.62"), 45500, true, "Repsol");
  }

  private static FuelRecord record(
      UUID vehicleId, Instant refueledAt, String liters, String cost, int odometer) {
    return FuelRecord.builder()
        .vehicleId(vehicleId)
        .refueledAt(refueledAt)
        .fuelType(FuelType.DIESEL)
        .liters(new BigDecimal(liters))
        .pricePerLiter(new BigDecimal("1.62"))
        .totalCost(new BigDecimal(cost))
        .odometerKm(odometer)
        .fullTank(true)
        .build();
  }

  @Test
  void createFullTankWithPreviousComputesConsumption() {
    UUID vehicleId = UUID.randomUUID();
    FuelRecord previous = record(vehicleId, DAY_ONE, "40", "64.80", 45000);
    FuelRecord entity =
        FuelRecord.builder()
            .vehicleId(vehicleId)
            .refueledAt(DAY_TWO)
            .liters(new BigDecimal("38"))
            .pricePerLiter(new BigDecimal("1.62"))
            .odometerKm(45500)
            .fullTank(true)
            .station("Repsol")
            .build();
    given(vehiclesClient.getById(vehicleId)).willReturn(dieselVehicle());
    given(fuelRepository.findTopByVehicleIdOrderByRefueledAtDesc(vehicleId))
        .willReturn(Optional.of(previous));
    given(fuelRecordMapper.toEntity(any())).willReturn(entity);
    given(
            fuelRepository
                .findTopByVehicleIdAndFullTankTrueAndRefueledAtBeforeOrderByRefueledAtDesc(
                    vehicleId, DAY_TWO))
        .willReturn(Optional.of(previous));
    given(fuelRepository.sumLitersBetween(vehicleId, DAY_ONE, DAY_TWO))
        .willReturn(new BigDecimal("20"));
    given(fuelRepository.save(entity)).willReturn(entity);
    given(fuelRecordMapper.toDto(entity))
        .willReturn(
            new FuelRecordResponse(
                UUID.randomUUID(),
                vehicleId,
                DAY_TWO,
                FuelType.DIESEL,
                new BigDecimal("38"),
                new BigDecimal("1.62"),
                new BigDecimal("61.56"),
                45500,
                true,
                "Repsol",
                new BigDecimal("11.60")));

    FuelRecordResponse response = fuelService.create(fullTankRequest(vehicleId));

    assertEquals(new BigDecimal("61.56"), response.totalCost());
    assertEquals(new BigDecimal("11.60"), response.consumptionL100km());
    assertEquals(FuelType.DIESEL, response.fuelType());
    ArgumentCaptor<FuelRecord> saved = ArgumentCaptor.forClass(FuelRecord.class);
    verify(fuelRepository).save(saved.capture());
    assertEquals(FuelType.DIESEL, saved.getValue().getFuelType());
    assertEquals(new BigDecimal("61.56"), saved.getValue().getTotalCost());
    assertEquals(new BigDecimal("11.60"), saved.getValue().getConsumptionL100km());
  }

  @Test
  void createFirstRefuelLeavesConsumptionNull() {
    UUID vehicleId = UUID.randomUUID();
    FuelRecord entity =
        FuelRecord.builder()
            .vehicleId(vehicleId)
            .refueledAt(DAY_ONE)
            .liters(new BigDecimal("40"))
            .pricePerLiter(new BigDecimal("1.62"))
            .odometerKm(45000)
            .fullTank(true)
            .build();
    given(vehiclesClient.getById(vehicleId)).willReturn(dieselVehicle());
    given(fuelRepository.findTopByVehicleIdOrderByRefueledAtDesc(vehicleId))
        .willReturn(Optional.empty());
    given(fuelRecordMapper.toEntity(any())).willReturn(entity);
    given(
            fuelRepository
                .findTopByVehicleIdAndFullTankTrueAndRefueledAtBeforeOrderByRefueledAtDesc(
                    vehicleId, DAY_ONE))
        .willReturn(Optional.empty());
    given(fuelRepository.save(entity)).willReturn(entity);
    given(fuelRecordMapper.toDto(entity)).willReturn(responseOf(entity));

    FuelRecordResponse response = fuelService.create(fullTankRequest(vehicleId));

    assertNull(response.consumptionL100km());
  }

  @Test
  void createPartialTankSkipsPreviousFullLookup() {
    UUID vehicleId = UUID.randomUUID();
    FuelRecord last = record(vehicleId, DAY_ONE, "40", "64.80", 45000);
    FuelRecord entity =
        FuelRecord.builder()
            .vehicleId(vehicleId)
            .refueledAt(DAY_TWO)
            .liters(new BigDecimal("20"))
            .pricePerLiter(new BigDecimal("1.62"))
            .odometerKm(45200)
            .fullTank(false)
            .build();
    CreateFuelRecordRequest request =
        new CreateFuelRecordRequest(
            vehicleId, DAY_TWO, new BigDecimal("20"), new BigDecimal("1.62"), 45200, false, null);
    given(vehiclesClient.getById(vehicleId)).willReturn(dieselVehicle());
    given(fuelRepository.findTopByVehicleIdOrderByRefueledAtDesc(vehicleId))
        .willReturn(Optional.of(last));
    given(fuelRecordMapper.toEntity(request)).willReturn(entity);
    given(fuelRepository.save(entity)).willReturn(entity);
    given(fuelRecordMapper.toDto(entity)).willReturn(responseOf(entity));

    FuelRecordResponse response = fuelService.create(request);

    assertNull(response.consumptionL100km());
    verify(fuelRepository, never())
        .findTopByVehicleIdAndFullTankTrueAndRefueledAtBeforeOrderByRefueledAtDesc(any(), any());
  }

  @Test
  void listReturnsMappedPage() {
    UUID vehicleId = UUID.randomUUID();
    FuelRecord stored = record(vehicleId, DAY_ONE, "40", "64.80", 45000);
    FuelRecordResponse dto = responseOf(stored);
    given(fuelRepository.search(any(), any(), any(), any()))
        .willReturn(new PageImpl<>(List.of(stored)));
    given(fuelRecordMapper.toDto(stored)).willReturn(dto);

    PageResponse<FuelRecordResponse> page = fuelService.list(vehicleId, null, null, 0, 20);

    assertEquals(1, page.totalElements());
    assertEquals(1, page.totalPages());
    assertEquals(dto, page.content().get(0));
    verify(fuelRecordMapper).toDto(stored);
  }

  @Test
  void consumptionAggregatesAndOrdersByCostDescending() {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    given(vehiclesClient.getById(first))
        .willReturn(new VehicleRef("2137KLM", "CAR", "DIESEL", 80, 1500, "AVAILABLE"));
    given(vehiclesClient.getById(second))
        .willReturn(new VehicleRef("2174PRT", "CAR", "GASOLINE", 52, 5200, "AVAILABLE"));
    given(fuelRepository.findInPeriod(any(), any(), any()))
        .willReturn(
            List.of(
                record(first, DAY_ONE, "30", "48.60", 1000),
                record(first, DAY_TWO, "20", "33.00", 1500),
                record(second, DAY_ONE, "10", "16.20", 5000),
                record(second, DAY_TWO, "10", "16.50", 5200)));

    FuelConsumptionResponse response =
        fuelService.consumption(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2));

    assertEquals(2, response.vehicles().size());
    assertEquals(first, response.vehicles().get(0).vehicleId());
    assertEquals("2137KLM", response.vehicles().get(0).plate());
    assertEquals(new BigDecimal("50"), response.vehicles().get(0).liters());
    assertEquals(new BigDecimal("81.60"), response.vehicles().get(0).cost());
    assertEquals(500, response.vehicles().get(0).distanceKm());
    assertEquals(new BigDecimal("10.00"), response.vehicles().get(0).avgL100km());
    assertEquals(second, response.vehicles().get(1).vehicleId());
  }

  @Test
  void statsBuildsDailySeriesAndTotals() {
    UUID vehicleId = UUID.randomUUID();
    given(fuelRepository.findInPeriod(any(), any(), any()))
        .willReturn(
            List.of(
                record(vehicleId, DAY_ONE, "25", "40.50", 1000),
                record(vehicleId, DAY_ONE, "15", "24.30", 1400),
                record(vehicleId, DAY_THREE, "15", "24.30", 1400),
                record(vehicleId, DAY_THREE, "10", "16.20", 1800)));

    FuelStatsResponse response =
        fuelService.stats(
            LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), Granularity.DAY, vehicleId);

    assertEquals(Granularity.DAY, response.granularity());
    assertEquals(3, response.series().size());
    assertEquals(new BigDecimal("40"), response.series().get(0).liters());
    assertEquals(new BigDecimal("64.80"), response.series().get(0).cost());
    assertEquals(new BigDecimal("10.00"), response.series().get(0).avgL100km());
    assertEquals(new BigDecimal("0"), response.series().get(1).liters());
    assertEquals(new BigDecimal("25"), response.series().get(2).liters());
    assertEquals(new BigDecimal("6.25"), response.series().get(2).avgL100km());
    assertEquals(new BigDecimal("65"), response.totals().liters());
    assertEquals(new BigDecimal("105.30"), response.totals().cost());
    assertEquals(new BigDecimal("1.62"), response.totals().avgPricePerLiter());
    assertEquals(new BigDecimal("8.13"), response.totals().avgL100km());
  }

  private static FuelRecordResponse responseOf(FuelRecord entity) {
    return new FuelRecordResponse(
        UUID.randomUUID(),
        entity.getVehicleId(),
        entity.getRefueledAt(),
        entity.getFuelType(),
        entity.getLiters(),
        entity.getPricePerLiter(),
        entity.getTotalCost(),
        entity.getOdometerKm(),
        entity.isFullTank(),
        entity.getStation(),
        entity.getConsumptionL100km());
  }
}
