package com.mobilebackend.ungdunglapkehoachdulich.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "travel.flight")
public class FlightRoutingProperties {
    private String provider = "duffel";

    private Duffel duffel = new Duffel();

    @Data
    public static class Duffel {
        private String baseUrl = "https://api.duffel.com";
        private String apiKey;
        private Integer timeoutSeconds = 30;
    }
}

