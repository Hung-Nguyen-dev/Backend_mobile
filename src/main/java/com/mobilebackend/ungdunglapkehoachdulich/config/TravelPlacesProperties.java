package com.mobilebackend.ungdunglapkehoachdulich.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "travel.places")
public class TravelPlacesProperties {
    private String userAgent = "UngDungLapKeHoachDuLich/1.0 (+https://github.com/mobilebackend)";
    private String nominatimBaseUrl = "https://nominatim.openstreetmap.org";
    private String nominatimFromEmail = "";
    private String photonBaseUrl = "https://photon.komoot.io";
    private String overpassInterpreterUrl = "https://overpass-api.de/api/interpreter";
    private List<String> overpassFallbackUrls = new ArrayList<>();
    private Integer timeoutSeconds = 25;
    private Integer defaultRadiusMeters = 4000;

    @NestedConfigurationProperty
    private SerpApi serpApi = new SerpApi();

    @Data
    public static class SerpApi {
        private boolean enabled = false;
        private String apiKey = "";
        private String baseUrl = "https://serpapi.com";
        private String hl = "vi";
        private String gl = "vn";
        private int mapZoom = 14;
        private boolean fallbackToOsm = true;
    }
}
