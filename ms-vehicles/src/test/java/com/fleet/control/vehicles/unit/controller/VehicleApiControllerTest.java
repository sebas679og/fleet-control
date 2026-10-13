package com.fleet.control.vehicles.unit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fleet.control.vehicles.common.constants.ApiPathConstants;
import com.fleet.control.vehicles.common.exceptions.ApiVehicleException;
import com.fleet.control.vehicles.common.exceptions.GlobalExceptionHandler;
import com.fleet.control.vehicles.common.exceptions.enums.ErrorCode;
import com.fleet.control.vehicles.common.models.dtos.request.CreateVehicleRequest;
import com.fleet.control.vehicles.common.models.dtos.request.UpdateVehicleRequest;
import com.fleet.control.vehicles.common.models.dtos.response.PageResponse;
import com.fleet.control.vehicles.common.models.dtos.response.VehicleResponse;
import com.fleet.control.vehicles.common.models.enums.FuelType;
import com.fleet.control.vehicles.common.models.enums.VehicleStatus;
import com.fleet.control.vehicles.common.models.enums.VehicleType;
import com.fleet.control.vehicles.controller.impl.VehicleApiController;
import com.fleet.control.vehicles.service.VehicleService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * HTTP contract tests for the fleet inventory user story only: 201 create / 409 duplicate,
 * paginated filtered list, detail / 404, update, and 400 VALIDATION_ERROR with details for invalid
 * payloads.
 */
@ExtendWith(MockitoExtension.class)
class VehicleApiControllerTest {

  @Mock private VehicleService vehicleService;

  @InjectMocks private VehicleApiController controller;

  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private static final int VALID_YEAR = LocalDate.now().getYear();

  @BeforeEach
  void setUp() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
            .build();
  }

  private CreateVehicleRequest validCreateRequest() {
    return new CreateVehicleRequest(
        "4821-KDF", "Ford", "Transit", VALID_YEAR, VehicleType.VAN, FuelType.DIESEL, 80, 45210);
  }

  private VehicleResponse vehicleResponse(UUID id) {
    return new VehicleResponse(
        id,
        "4821-KDF",
        "Ford",
        "Transit",
        VALID_YEAR,
        VehicleType.VAN,
        FuelType.DIESEL,
        80,
        45210,
        VehicleStatus.AVAILABLE,
        null,
        null);
  }

  @Test
  void postCreateReturns201InAvailable() throws Exception {
    var response = vehicleResponse(UUID.randomUUID());
    given(vehicleService.create(any(CreateVehicleRequest.class))).willReturn(response);

    mockMvc
        .perform(
            post(ApiPathConstants.VEHICLE_BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCreateRequest())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.plate").value("4821-KDF"))
        .andExpect(jsonPath("$.status").value("AVAILABLE"));
  }

  @Test
  void postDuplicatePlateReturns409() throws Exception {
    given(vehicleService.create(any(CreateVehicleRequest.class)))
        .willThrow(new ApiVehicleException.VehicleAlreadyExistsException("4821-KDF"));

    mockMvc
        .perform(
            post(ApiPathConstants.VEHICLE_BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCreateRequest())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value(ErrorCode.VEHICLE_ALREADY_EXISTS.name()));
  }

  @Test
  void getListForwardsFiltersAndReturns200() throws Exception {
    var response = vehicleResponse(UUID.randomUUID());
    given(
            vehicleService.list(
                eq(VehicleStatus.AVAILABLE), eq(VehicleType.VAN), eq("4821"), eq(0), eq(20)))
        .willReturn(PageResponse.of(List.of(response), 0, 20, 1));

    mockMvc
        .perform(
            get(ApiPathConstants.VEHICLE_BASE_PATH)
                .param("status", "AVAILABLE")
                .param("type", "VAN")
                .param("plate", "4821")
                .param("page", "0")
                .param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].plate").value("4821-KDF"))
        .andExpect(jsonPath("$.totalElements").value(1));

    verify(vehicleService).list(VehicleStatus.AVAILABLE, VehicleType.VAN, "4821", 0, 20);
  }

  @Test
  void getByIdReturns200() throws Exception {
    UUID id = UUID.randomUUID();
    given(vehicleService.getById(id)).willReturn(vehicleResponse(id));

    mockMvc
        .perform(get(ApiPathConstants.VEHICLE_BASE_PATH + "/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()));
  }

  @Test
  void getByIdNotFoundReturns404() throws Exception {
    UUID id = UUID.randomUUID();
    given(vehicleService.getById(id))
        .willThrow(new ApiVehicleException.VehicleNotFoundException(id));

    mockMvc
        .perform(get(ApiPathConstants.VEHICLE_BASE_PATH + "/" + id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value(ErrorCode.VEHICLE_NOT_FOUND.name()));
  }

  @Test
  void putUpdateReturns200() throws Exception {
    UUID id = UUID.randomUUID();
    var request =
        new UpdateVehicleRequest(
            "Ford", "Transit Custom", VALID_YEAR, VehicleType.VAN, FuelType.DIESEL, 80);
    given(vehicleService.update(eq(id), any(UpdateVehicleRequest.class)))
        .willReturn(vehicleResponse(id));

    mockMvc
        .perform(
            put(ApiPathConstants.VEHICLE_BASE_PATH + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()));
  }

  @Test
  void postInvalidTankReturns400WithDetails() throws Exception {
    var request =
        new CreateVehicleRequest(
            "4821-KDF", "Ford", "Transit", VALID_YEAR, VehicleType.VAN, FuelType.DIESEL, 0, 0);

    mockMvc
        .perform(
            post(ApiPathConstants.VEHICLE_BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value(ErrorCode.VALIDATION_ERROR.name()))
        .andExpect(jsonPath("$.details[0].field").value("tankCapacityL"));
  }

  @Test
  void postInvalidYearReturns400WithDetails() throws Exception {
    var request =
        new CreateVehicleRequest(
            "4821-KDF", "Ford", "Transit", 1980, VehicleType.VAN, FuelType.DIESEL, 80, 0);

    mockMvc
        .perform(
            post(ApiPathConstants.VEHICLE_BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value(ErrorCode.VALIDATION_ERROR.name()))
        .andExpect(jsonPath("$.details[0].field").value("year"));
  }
}
