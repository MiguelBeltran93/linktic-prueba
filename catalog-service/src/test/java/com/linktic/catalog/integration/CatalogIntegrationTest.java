package com.linktic.catalog.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linktic.catalog.dto.request.CreateItemRequest;
import com.linktic.catalog.repository.CatalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Catalog Service Integration Tests")
class CatalogIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("linktic_test")
            .withUsername("postgres")
            .withPassword("admin");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CatalogRepository catalogRepository;

    private static final String API_KEY = "secret123";
    private static final String BASE_URL = "/api/v1/catalog/items";

    @BeforeEach
    void setUp() {
        catalogRepository.deleteAll();
    }

    @Test
    @DisplayName("Full flow: register item then retrieve it by ID")
    void registerAndFindById_shouldPersistAndReturnCorrectly() throws Exception {
        CreateItemRequest request = CreateItemRequest.builder()
                .name("Gaming Monitor")
                .price(349.99)
                .description("27 inch 144Hz gaming monitor")
                .build();

        String createBody = mockMvc.perform(post(BASE_URL)
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("catalog-items"))
                .andExpect(jsonPath("$.data.attributes.name").value("Gaming Monitor"))
                .andExpect(jsonPath("$.data.attributes.price").value(349.99))
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(createBody).get("data").get("id").asText();

        mockMvc.perform(get(BASE_URL + "/" + id)
                        .header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.attributes.name").value("Gaming Monitor"))
                .andExpect(jsonPath("$.data.attributes.price").value(349.99));

        assertThat(catalogRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("listAll - after registering multiple items should return all")
    void listAll_afterMultipleRegistrations_shouldReturnAll() throws Exception {
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post(BASE_URL)
                    .header("X-API-KEY", API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                            CreateItemRequest.builder()
                                    .name("Item " + i)
                                    .price(10.0 * i)
                                    .build()
                    )));
        }

        mockMvc.perform(get(BASE_URL).header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    @DisplayName("findById - non-existing id should return 404 with JSON API error format")
    void findById_withNonExistingId_shouldReturn404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/9999")
                        .header("X-API-KEY", API_KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].status").value("404"))
                .andExpect(jsonPath("$.errors[0].title").value("Item Not Found"))
                .andExpect(jsonPath("$.errors[0].detail").value("No catalog item found with id: 9999"));
    }

    @Test
    @DisplayName("register - missing API key should return 401")
    void register_withoutApiKey_shouldReturn401() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                CreateItemRequest.builder().name("Test").price(10.0).build()
                        )))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("register - invalid API key should return 401")
    void register_withInvalidApiKey_shouldReturn401() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .header("X-API-KEY", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                CreateItemRequest.builder().name("Test").price(10.0).build()
                        )))
                .andExpect(status().isUnauthorized());
    }
}
