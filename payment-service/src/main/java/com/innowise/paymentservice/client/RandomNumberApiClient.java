package com.innowise.paymentservice.client;

import com.innowise.paymentservice.model.entity.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;

/**
 * Client component for determining payment status using an external random number API.
 */
@Slf4j
@Component
@EnableRetry
@RequiredArgsConstructor
public class RandomNumberApiClient {
    private final RestTemplate restTemplate;
    private static final SecureRandom FALLBACK_RANDOM = new SecureRandom();

    @Value("${external-api.url}")
    private String externalApiUrl;

    /**
     * Determines the payment status based on a random number from external API.
     *
     * @return  {@link PaymentStatus#SUCCESS} if the number is even,
     *          {@link PaymentStatus#FAILED} if the number is odd
     */
    public PaymentStatus determinePaymentStatus() {
        int randomNumber;

        try {
            randomNumber = getRandomNumber();
        } catch (Exception e) {
            log.warn("External API unavailable, using fallback random. Error: {}", e.getMessage());

            randomNumber = FALLBACK_RANDOM.nextInt(100) + 1;
        }

        return randomNumber % 2 == 0 ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 5)
    private int getRandomNumber() {
        int[] apiResponse = restTemplate.getForObject(externalApiUrl, int[].class);

        if (apiResponse.length == 0) {
            throw new IllegalStateException("Empty API response");
        }

        return apiResponse[0];
    }
}