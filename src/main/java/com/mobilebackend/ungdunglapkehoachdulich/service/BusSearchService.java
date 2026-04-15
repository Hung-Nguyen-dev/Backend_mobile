package com.mobilebackend.ungdunglapkehoachdulich.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobilebackend.ungdunglapkehoachdulich.config.TravelPlacesProperties;
import com.mobilebackend.ungdunglapkehoachdulich.dto.bus.BusSearchItemRes;
import com.mobilebackend.ungdunglapkehoachdulich.dto.bus.BusSearchRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BusSearchService {
    private final TravelPlacesProperties placesProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BusSearchRes search(String from, String to, String date, Integer limit) throws Exception {
        String f = sanitize(from);
        String t = sanitize(to);
        String d = sanitize(date);
        if (f.isEmpty() || t.isEmpty() || d.isEmpty()) {
            throw new IllegalArgumentException("Thieu diem di, diem den hoac ngay khoi hanh");
        }

        TravelPlacesProperties.SerpApi serp = placesProperties.getSerpApi();
        if (serp == null || serp.getApiKey() == null || serp.getApiKey().isBlank()) {
            throw new IllegalStateException("SerpApi chua duoc cau hinh api-key");
        }

        int max = limit == null || limit <= 0 ? 12 : Math.min(limit, 30);
        String q = "site:vexere.com ve xe tu " + f + " di " + t + " ngay " + d;
        String base = trimSlash(serp.getBaseUrl().isBlank() ? "https://serpapi.com" : serp.getBaseUrl());
        String url = base + "/search.json"
                + "?engine=google"
                + "&google_domain=google.com"
                + "&gl=" + URLEncoder.encode(defaultString(serp.getGl(), "vn"), StandardCharsets.UTF_8)
                + "&hl=" + URLEncoder.encode(defaultString(serp.getHl(), "vi"), StandardCharsets.UTF_8)
                + "&q=" + URLEncoder.encode(q, StandardCharsets.UTF_8)
                + "&api_key=" + URLEncoder.encode(serp.getApiKey(), StandardCharsets.UTF_8)
                + "&num=" + max;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(8, placesProperties.getTimeoutSeconds())))
                .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(Math.max(10, placesProperties.getTimeoutSeconds())))
                .header("Accept", "application/json")
                .header("User-Agent", placesProperties.getUserAgent())
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("SerpApi HTTP " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        if (root.hasNonNull("error")) {
            throw new IllegalStateException(root.path("error").asText("SerpApi loi"));
        }

        List<BusSearchItemRes> items = parseOrganic(root.path("organic_results"), max);
        if (items.isEmpty()) {
            String fallbackQ = "vexere " + f + " " + t + " " + d;
            String fallbackUrl = base + "/search.json"
                    + "?engine=google"
                    + "&google_domain=google.com"
                    + "&gl=" + URLEncoder.encode(defaultString(serp.getGl(), "vn"), StandardCharsets.UTF_8)
                    + "&hl=" + URLEncoder.encode(defaultString(serp.getHl(), "vi"), StandardCharsets.UTF_8)
                    + "&q=" + URLEncoder.encode(fallbackQ, StandardCharsets.UTF_8)
                    + "&api_key=" + URLEncoder.encode(serp.getApiKey(), StandardCharsets.UTF_8)
                    + "&num=" + max;
            HttpRequest fallbackReq = HttpRequest.newBuilder(URI.create(fallbackUrl))
                    .timeout(Duration.ofSeconds(Math.max(10, placesProperties.getTimeoutSeconds())))
                    .header("Accept", "application/json")
                    .header("User-Agent", placesProperties.getUserAgent())
                    .GET()
                    .build();
            HttpResponse<String> fallbackResp = client.send(fallbackReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (fallbackResp.statusCode() / 100 == 2) {
                JsonNode fallbackRoot = objectMapper.readTree(fallbackResp.body());
                items = parseOrganic(fallbackRoot.path("organic_results"), max);
            }
        }

        return BusSearchRes.builder()
                .query(q)
                .items(items)
                .build();
    }

    private List<BusSearchItemRes> parseOrganic(JsonNode organic, int max) {
        List<BusSearchItemRes> items = new ArrayList<>();
        if (!organic.isArray()) {
            return items;
        }
        for (JsonNode item : organic) {
            String title = item.path("title").asText("").trim();
            String snippet = item.path("snippet").asText("").trim();
            String link = item.path("link").asText("").trim();
            if (title.isEmpty() || link.isEmpty()) {
                continue;
            }
            if (!isLikelyBusResult(title, snippet, link)) {
                continue;
            }
            items.add(BusSearchItemRes.builder()
                    .title(title)
                    .snippet(snippet)
                    .link(link)
                    .build());
            if (items.size() >= max) {
                break;
            }
        }
        return items;
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String trimSlash(String url) {
        if (url == null) return "";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static boolean isLikelyBusResult(String title, String snippet, String link) {
        String merged = (title + " " + snippet + " " + link).toLowerCase(Locale.ROOT);
        boolean hasVexere = merged.contains("vexere.com");
        boolean hasBusIntent = merged.contains("xe khach")
                || merged.contains("xe khách")
                || merged.contains("ve xe khach")
                || merged.contains("vé xe khách")
                || merged.contains("limousine")
                || merged.contains("nha xe")
                || merged.contains("nhà xe")
                || link.toLowerCase(Locale.ROOT).contains("ve-xe-khach");
        boolean hasFlightIntent = merged.contains("ve may bay")
                || merged.contains("vé máy bay")
                || link.toLowerCase(Locale.ROOT).contains("ve-may-bay");
        return hasVexere && hasBusIntent && !hasFlightIntent;
    }
}
