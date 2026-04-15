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
    /**
     * Nominatim (OSMF) khuyến nghị dạng: App/ver (+https://trang-gioi-thieu-ung-dung)
     */
    private String userAgent = "UngDungLapKeHoachDuLich/1.0 (+https://github.com/mobilebackend)";
    private String nominatimBaseUrl = "https://nominatim.openstreetmap.org";
    /**
     * Header From (email) khi gọi Nominatim — giảm nguy cơ 403 nếu gọi thường xuyên.
     */
    private String nominatimFromEmail = "";
    /**
     * Geocode dự phòng (Komoot Photon — cùng dữ liệu OSM, không cần key).
     */
    private String photonBaseUrl = "https://photon.komoot.io";
    private String overpassInterpreterUrl = "https://overpass-api.de/api/interpreter";
    /**
     * Khi overpass-api.de quá tải / timeout, thử lần lượt các mirror.
     */
    private List<String> overpassFallbackUrls = new ArrayList<>();
    private Integer timeoutSeconds = 25;
    private Integer defaultRadiusMeters = 4000;

    /**
     * SerpAPI (engine=google_maps) — uu tien khi {@link SerpApi#enabled} va co api-key.
     */
    @NestedConfigurationProperty
    private SerpApi serpApi = new SerpApi();

    @Data
    public static class SerpApi {
        private boolean enabled = false;
        private String apiKey = "";
        private String baseUrl = "https://serpapi.com";
        /** Ngon ngu ket qua Maps */
        private String hl = "vi";
        private String gl = "vn";
        /** Zoom trong tham so ll=@lat,lon,{zoom}z */
        private int mapZoom = 14;
        /** Khi SerpAPI loi / tra rong: quay lai Overpass OSM */
        private boolean fallbackToOsm = true;
    }
}
