package com.hugo.inventory.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class ProductClient {

    private final RestTemplate restTemplate;

    @Value("${products.service.base-url}")
    private String productsServiceBaseUrl;

    @Value("${services.api-key}")
    private String apiKey;

    @Retry(name = "productsService", fallbackMethod = "validateProductExistsFallback")
    @CircuitBreaker(name = "productsService", fallbackMethod = "validateProductExistsFallback")
    @TimeLimiter(name = "productsService")
    public CompletableFuture<Void> validateProductExists(UUID productId) {
        return CompletableFuture.runAsync(() -> {
            String url = productsServiceBaseUrl + "/products/" + productId + "/exists";

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", apiKey);

            String correlationId = MDC.get("correlationId");
            if (correlationId != null && !correlationId.isBlank()) {
                headers.set("X-Correlation-Id", correlationId);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            try {
                restTemplate.exchange(url, HttpMethod.GET, entity, Void.class);
            } catch (HttpClientErrorException.NotFound ex) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
            } catch (HttpClientErrorException.Unauthorized ex) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API Key");
            } catch (Exception ex) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Products service unavailable"
                );
            }
        });
    }

    public CompletableFuture<Void> validateProductExistsFallback(UUID productId, Throwable throwable) {
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Products service temporarily unavailable"
        );
    }
}