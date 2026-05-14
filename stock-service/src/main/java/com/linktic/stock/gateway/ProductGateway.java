package com.linktic.stock.gateway;

import com.linktic.stock.exception.ProductNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductGateway {

    private final RestTemplate restTemplate;

    @Value("${services.catalog.url}")
    private String catalogUrl;

    @Value("${services.catalog.api-key}")
    private String catalogApiKey;

    @CircuitBreaker(name = "catalogGateway", fallbackMethod = "fallbackValidate")
    @Retry(name = "catalogGateway")
    public void validateProduct(Long productId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", catalogApiKey);
        try {
            restTemplate.exchange(
                    catalogUrl + "/api/v1/catalog/items/" + productId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Object.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ProductNotFoundException(productId);
        }
    }

    public void fallbackValidate(Long productId, Exception ex) {
        if (ex instanceof ProductNotFoundException pnfe) {
            throw pnfe;
        }
        log.warn("Circuit breaker open for productId={}: {}", productId, ex.getMessage());
    }
}
