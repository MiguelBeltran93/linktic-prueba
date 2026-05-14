package com.linktic.stock.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.linktic.stock.dto.request.PurchaseRequest;
import com.linktic.stock.repository.PurchaseRepository;
import com.linktic.stock.repository.StockRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
@DisplayName("Stock Service Integration Tests")
class StockIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("linktic_test")
            .withUsername("postgres")
            .withPassword("admin");

    static WireMockServer wireMock;

    @MockBean
    RabbitTemplate rabbitTemplate;

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("services.catalog.url", () -> "http://localhost:" + wireMock.port());
        registry.add("spring.rabbitmq.host", () -> "localhost");
        registry.add("spring.rabbitmq.port", () -> "5672");
    }

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private StockRepository stockRepository;
    @Autowired private PurchaseRepository purchaseRepository;

    private static final String API_KEY  = "secret123";
    private static final String BASE_URL = "/api/v1/stock";

    @BeforeEach
    void setUp() {
        purchaseRepository.deleteAll();
        stockRepository.deleteAll();
        wireMock.resetAll();
    }

    private void stubCatalogProductFound(Long productId) {
        wireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/api/v1/catalog/items/" + productId))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"id\":\"" + productId + "\",\"type\":\"catalog-items\"," +
                                "\"attributes\":{\"id\":" + productId + ",\"name\":\"Product\",\"price\":99.99}}}")));
    }

    private void stubCatalogProductNotFound(Long productId) {
        wireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/api/v1/catalog/items/" + productId))
                .willReturn(WireMock.aResponse().withStatus(404)));
    }

    @Test
    @DisplayName("getStock - valid product should return stock with qty 0 when no entry exists")
    void getStock_newProduct_shouldReturnZeroStock() throws Exception {
        stubCatalogProductFound(1L);

        mockMvc.perform(get(BASE_URL + "/1").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("stock-entries"))
                .andExpect(jsonPath("$.data.attributes.quantity").value(0));

        assertThat(stockRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("getStock - product not found in catalog should return 404")
    void getStock_productNotInCatalog_shouldReturn404() throws Exception {
        stubCatalogProductNotFound(99L);

        mockMvc.perform(get(BASE_URL + "/99").header("X-API-KEY", API_KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].status").value("404"));
    }

    @Test
    @DisplayName("adjustStock then purchase - full flow should work correctly")
    void adjustThenPurchase_shouldDeductCorrectly() throws Exception {
        mockMvc.perform(put(BASE_URL + "/1/adjust")
                        .header("X-API-KEY", API_KEY)
                        .param("quantity", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attributes.quantity").value(50));

        PurchaseRequest purchaseRequest = PurchaseRequest.builder().productId(1L).units(20).build();
        mockMvc.perform(post(BASE_URL + "/purchase")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(purchaseRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.attributes.units").value(20))
                .andExpect(jsonPath("$.data.attributes.remainingStock").value(30));

        assertThat(purchaseRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("purchase - insufficient stock should return 422")
    void purchase_insufficientStock_shouldReturn422() throws Exception {
        PurchaseRequest request = PurchaseRequest.builder().productId(1L).units(100).build();

        mockMvc.perform(post(BASE_URL + "/purchase")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].status").value("422"))
                .andExpect(jsonPath("$.errors[0].title").value("Insufficient Stock"));
    }

    @Test
    @DisplayName("purchase - missing API key should return 401")
    void purchase_withoutApiKey_shouldReturn401() throws Exception {
        PurchaseRequest request = PurchaseRequest.builder().productId(1L).units(1).build();

        mockMvc.perform(post(BASE_URL + "/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("purchase - invalid API key should return 401")
    void purchase_withInvalidApiKey_shouldReturn401() throws Exception {
        PurchaseRequest request = PurchaseRequest.builder().productId(1L).units(1).build();

        mockMvc.perform(post(BASE_URL + "/purchase")
                        .header("X-API-KEY", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("purchase - invalid body should return 422 with validation errors")
    void purchase_withMissingFields_shouldReturn422() throws Exception {
        mockMvc.perform(post(BASE_URL + "/purchase")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].status").value("422"));
    }
}
