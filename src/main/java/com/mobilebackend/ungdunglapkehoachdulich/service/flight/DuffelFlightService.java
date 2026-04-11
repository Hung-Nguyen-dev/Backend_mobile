package com.mobilebackend.ungdunglapkehoachdulich.service.flight;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mobilebackend.ungdunglapkehoachdulich.config.FlightRoutingProperties;
import com.mobilebackend.ungdunglapkehoachdulich.dto.flight.FlightSearchReq;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DuffelFlightService implements FlightService {
    private static final DateTimeFormatter DUFFEL_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final FlightRoutingProperties routingProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Override
    public Object searchFlights(FlightSearchReq req) {
        validateSearchRequest(req);

        String body = buildSearchRequestJson(req);
        HttpRequest request = duffelRequest(buildUri("/air/offer_requests?return_offers=true"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return toJsonObject(parseJson(executeText(request)));
    }

    @Override
    public Object priceFlightOffer(String flightOfferPayload) {
        if (isBlank(flightOfferPayload)) {
            throw new IllegalArgumentException("flightOfferPayload khong duoc rong");
        }
        JsonNode payload = parseJson(flightOfferPayload);
        String offerId = textValue(payload, "id");
        if (isBlank(offerId) && payload.has("data")) {
            JsonNode data = payload.get("data");
            if (data.isArray() && !data.isEmpty()) {
                offerId = textValue(data.get(0), "id");
            } else {
                offerId = textValue(data, "id");
            }
        }
        if (isBlank(offerId)) {
            throw new IllegalArgumentException("Offer phai co id field");
        }

        HttpRequest request = duffelRequest(buildUri("/air/offers/" + offerId))
                .GET()
                .build();

        return toJsonObject(parseJson(executeText(request)));
    }

    @Override
    public Object bookFlight(String bookingRequest) {
        if (isBlank(bookingRequest)) {
            throw new IllegalArgumentException("bookingRequest khong duoc rong");
        }
        JsonNode payload = parseJson(bookingRequest);
        String orderId = textValue(payload, "id");
        if (isBlank(orderId) && payload.has("data")) {
            JsonNode data = payload.get("data");
            if (data.isArray() && !data.isEmpty()) {
                orderId = textValue(data.get(0), "id");
            } else {
                orderId = textValue(data, "id");
            }
        }
        if (isBlank(orderId)) {
            throw new IllegalArgumentException("Order phai co id field");
        }

        HttpRequest request = duffelRequest(buildUri("/air/offers/" + orderId))
                .GET()
                .build();

        JsonNode orderDetail = parseJson(executeText(request));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", "duffel");
        result.put("orderId", orderId);
        result.put("status", textValue(orderDetail, "status"));
        result.put("bookingMode", "ORDER_CREATED");
        result.put("raw", toJsonObject(orderDetail));

        return result;
    }

    private Map<String, Object> buildSearchPayload(FlightSearchReq req) {
        Map<String, Object> payload = new LinkedHashMap<>();

        Map<String, Object> cabin = new LinkedHashMap<>();
        cabin.put("cabin_class", isBlank(req.getTravelClass()) ? "economy" : req.getTravelClass().toLowerCase());

        List<Map<String, Object>> passengers = new ArrayList<>();
        for (int i = 0; i < req.getAdults(); i++) {
            Map<String, Object> pax = new LinkedHashMap<>();
            pax.put("type", "adult");
            passengers.add(pax);
        }
        if (req.getChildren() != null && req.getChildren() > 0) {
            for (int i = 0; i < req.getChildren(); i++) {
                Map<String, Object> pax = new LinkedHashMap<>();
                pax.put("type", "child");
                passengers.add(pax);
            }
        }
        if (req.getInfants() != null && req.getInfants() > 0) {
            for (int i = 0; i < req.getInfants(); i++) {
                Map<String, Object> pax = new LinkedHashMap<>();
                pax.put("type", "infant");
                passengers.add(pax);
            }
        }

        List<Map<String, Object>> slices = new ArrayList<>();
        Map<String, Object> slice = new LinkedHashMap<>();
        slice.put("origin", req.getOriginLocationCode());
        slice.put("destination", req.getDestinationLocationCode());
        slice.put("departure_date", DUFFEL_DATE_FORMAT.format(req.getDepartureDate()));
        slices.add(slice);

        if (req.getReturnDate() != null) {
            Map<String, Object> returnSlice = new LinkedHashMap<>();
            returnSlice.put("origin", req.getDestinationLocationCode());
            returnSlice.put("destination", req.getOriginLocationCode());
            returnSlice.put("departure_date", DUFFEL_DATE_FORMAT.format(req.getReturnDate()));
            slices.add(returnSlice);
        }

        payload.put("slices", slices);
        payload.put("passengers", passengers);
        payload.put("cabin_class", cabin.get("cabin_class"));
        if (!isBlank(req.getCurrencyCode())) {
            payload.put("currency_code", req.getCurrencyCode());
        }
        if (req.getMax() != null && req.getMax() > 0) {
            payload.put("limit", req.getMax());
        }

        return payload;
    }

    private String buildSearchRequestJson(FlightSearchReq req) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.set("data", objectMapper.valueToTree(buildSearchPayload(req)));
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Khong the tao JSON body cho Duffel search", e);
        }
    }

    private HttpRequest.Builder duffelRequest(URI uri) {
        ensureConfigured();
        return HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(timeoutSeconds()))
                .header("Accept", "application/json")
                .header("Duffel-Version", "v2")
                .header("Authorization", "Bearer " + routingProperties.getDuffel().getApiKey());
    }

    private URI buildUri(String path) {
        String baseUrl = routingProperties.getDuffel().getBaseUrl();
        if (isBlank(baseUrl)) {
            baseUrl = "https://api.duffel.com";
        }
        baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return URI.create(baseUrl + path);
    }

    private int timeoutSeconds() {
        Integer configured = routingProperties.getDuffel().getTimeoutSeconds();
        return configured == null || configured <= 0 ? 30 : configured;
    }

    private String executeText(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Duffel API error " + response.statusCode() + ": " + response.body());
            }
            return response.body();
        } catch (IOException e) {
            throw new IllegalStateException("Khong the goi Duffel API", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Duffel API bi ngat", e);
        }
    }

    private JsonNode parseJson(String body) {
        try {
            return objectMapper.readTree(body == null ? "{}" : body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Response Duffel khong phai JSON hop le", e);
        }
    }

    private String serializeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Khong the serialize object thanh JSON", e);
        }
    }

    private Object toJsonObject(JsonNode node) {
        return objectMapper.convertValue(node, Object.class);
    }

    private void validateSearchRequest(FlightSearchReq req) {
        if (req == null) {
            throw new IllegalArgumentException("flight search request khong duoc rong");
        }
        if (isBlank(req.getOriginLocationCode()) || isBlank(req.getDestinationLocationCode())) {
            throw new IllegalArgumentException("originLocationCode va destinationLocationCode la bat buoc");
        }
        if (req.getOriginLocationCode().trim().equalsIgnoreCase(req.getDestinationLocationCode().trim())) {
            throw new IllegalArgumentException("Diem di va diem den khong duoc trung nhau");
        }
        if (req.getDepartureDate() == null) {
            throw new IllegalArgumentException("departureDate la bat buoc");
        }
        if (req.getAdults() == null || req.getAdults() <= 0) {
            throw new IllegalArgumentException("adults phai lon hon 0");
        }
    }

    private void ensureConfigured() {
        if (isBlank(routingProperties.getDuffel().getApiKey())) {
            throw new IllegalStateException("Chua cau hinh travel.flight.duffel.api-key");
        }
    }

    private String textValue(JsonNode node, String field) {
        if (node == null || field == null || field.isBlank()) {
            return null;
        }
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}







