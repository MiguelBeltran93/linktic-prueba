package com.linktic.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linktic.catalog.config.ApiAuthFilter;
import com.linktic.catalog.dto.request.CreateItemRequest;
import com.linktic.catalog.dto.response.ApiData;
import com.linktic.catalog.dto.response.ApiResponse;
import com.linktic.catalog.dto.response.ItemRepresentation;
import com.linktic.catalog.exception.ApiExceptionHandler;
import com.linktic.catalog.exception.ItemNotFoundException;
import com.linktic.catalog.service.CatalogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = CatalogController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApiAuthFilter.class)
)
@Import(ApiExceptionHandler.class)
@DisplayName("CatalogController Unit Tests")
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CatalogService catalogService;

    private static final String BASE_URL = "/api/v1/catalog/items";

    private ApiResponse<ApiData<ItemRepresentation>> buildSingleResponse(Long id, String name, Double price) {
        return ApiResponse.<ApiData<ItemRepresentation>>builder()
                .data(ApiData.<ItemRepresentation>builder()
                        .id(String.valueOf(id))
                        .type("catalog-items")
                        .attributes(ItemRepresentation.builder()
                                .id(id).name(name).price(price).build())
                        .build())
                .build();
    }

    @Test
    @DisplayName("POST /items - valid request should return 201 with JSON API body")
    void register_withValidRequest_shouldReturn201() throws Exception {
        CreateItemRequest request = CreateItemRequest.builder()
                .name("Laptop Pro").price(999.99).description("Test laptop").build();

        when(catalogService.register(any())).thenReturn(buildSingleResponse(1L, "Laptop Pro", 999.99));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("1"))
                .andExpect(jsonPath("$.data.type").value("catalog-items"))
                .andExpect(jsonPath("$.data.attributes.name").value("Laptop Pro"))
                .andExpect(jsonPath("$.data.attributes.price").value(999.99));
    }

    @Test
    @DisplayName("POST /items - missing name should return 422 with validation errors")
    void register_withMissingName_shouldReturn422() throws Exception {
        CreateItemRequest request = CreateItemRequest.builder().price(99.99).build();

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].status").value("422"));
    }

    @Test
    @DisplayName("POST /items - negative price should return 422")
    void register_withNegativePrice_shouldReturn422() throws Exception {
        CreateItemRequest request = CreateItemRequest.builder()
                .name("Item").price(-10.0).build();

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("GET /items/{id} - existing item should return 200")
    void findById_whenItemExists_shouldReturn200() throws Exception {
        when(catalogService.findById(1L)).thenReturn(buildSingleResponse(1L, "Laptop Pro", 999.99));

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("1"))
                .andExpect(jsonPath("$.data.type").value("catalog-items"))
                .andExpect(jsonPath("$.data.attributes.name").value("Laptop Pro"));
    }

    @Test
    @DisplayName("GET /items/{id} - non-existing item should return 404 with JSON API error")
    void findById_whenNotFound_shouldReturn404() throws Exception {
        when(catalogService.findById(99L)).thenThrow(new ItemNotFoundException(99L));

        mockMvc.perform(get(BASE_URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].status").value("404"))
                .andExpect(jsonPath("$.errors[0].title").value("Item Not Found"))
                .andExpect(jsonPath("$.errors[0].detail").value("No catalog item found with id: 99"));
    }

    @Test
    @DisplayName("GET /items - should return list of all items")
    void listAll_shouldReturn200WithList() throws Exception {
        ApiResponse<List<ApiData<ItemRepresentation>>> response =
                ApiResponse.<List<ApiData<ItemRepresentation>>>builder()
                        .data(List.of(
                                ApiData.<ItemRepresentation>builder()
                                        .id("1").type("catalog-items")
                                        .attributes(ItemRepresentation.builder()
                                                .id(1L).name("Laptop Pro").price(999.99).build())
                                        .build()
                        ))
                        .build();

        when(catalogService.listAll()).thenReturn(response);

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].type").value("catalog-items"))
                .andExpect(jsonPath("$.data[0].id").value("1"));
    }

    @Test
    @DisplayName("GET /items - empty catalog should return empty array")
    void listAll_whenEmpty_shouldReturnEmptyArray() throws Exception {
        when(catalogService.listAll()).thenReturn(
                ApiResponse.<List<ApiData<ItemRepresentation>>>builder().data(List.of()).build()
        );

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
