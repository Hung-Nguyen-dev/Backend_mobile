package com.mobilebackend.ungdunglapkehoachdulich.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "travel.flight.amadeus")
public class FlightProviderProperties {
    private String baseUrl = "https://test.api.amadeus.com";
    private String authUrl = "https://test.api.amadeus.com/v1/security/oauth2/token";
    private String clientId;
    private String clientSecret;
    private Integer timeoutSeconds = 30;
}

