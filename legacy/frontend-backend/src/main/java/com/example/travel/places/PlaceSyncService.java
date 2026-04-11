package com.example.travel.places;

import com.example.travel.osm.OverpassClient;
import com.example.travel.osm.OverpassResponse;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PlaceSyncService {

  private final OverpassClient overpassClient;
  private final PlaceRepository placeRepository;

  public PlaceSyncService(OverpassClient overpassClient, PlaceRepository placeRepository) {
    this.overpassClient = overpassClient;
    this.placeRepository = placeRepository;
  }

  private static final Map<String, String> CITY_TO_OSM_NAME = buildCityMap();

  private static Map<String, String> buildCityMap() {
    Map<String, String> m = new HashMap<>();
    m.put("Đà Nẵng", "Đà Nẵng");
    m.put("Da Nang", "Đà Nẵng");
    m.put("Hà Nội", "Hà Nội");
    m.put("Ha Noi", "Hà Nội");
    m.put("TP.HCM", "Thành phố Hồ Chí Minh");
    m.put("Ho Chi Minh City", "Thành phố Hồ Chí Minh");
    return m;
  }

  public int syncCity(String uiCityName) {
    String osmCity = CITY_TO_OSM_NAME.getOrDefault(uiCityName, uiCityName);
    String q = buildQueryForCity(osmCity);
    OverpassResponse resp = overpassClient.query(q);

    int newCount = 0;
    for (OverpassResponse.Element el : resp.getElements()) {
      Map<String, String> tags = el.getTags();
      if (tags == null) continue;

      PlaceType type = mapType(tags);
      if (type == null) continue;

      String name = tags.get("name");
      if (name == null || name.trim().isEmpty()) continue;

      String sourcePlaceId = el.getType() + "/" + el.getId();
      if (placeRepository.findBySourceAndSourcePlaceId("OSM", sourcePlaceId).isPresent()) {
        continue;
      }

      Place p = new Place();
      p.setName(name);
      p.setType(type);
      p.setLat(el.getLat());
      p.setLng(el.getLon());
      p.setAddress(buildAddress(tags));
      p.setCity(uiCityName);
      p.setSource("OSM");
      p.setSourcePlaceId(sourcePlaceId);

      placeRepository.save(p);
      newCount++;
    }
    return newCount;
  }

  private String buildQueryForCity(String osmCityName) {
    return "[out:json][timeout:25];\n"
        + "area[\"name\"=\"" + osmCityName + "\"][\"boundary\"=\"administrative\"][\"admin_level\"~\"4|6\"]->.a;\n"
        + "(\n"
        + "  node[\"tourism\"=\"attraction\"](area.a);\n"
        + "  node[\"amenity\"=\"restaurant\"](area.a);\n"
        + "  node[\"amenity\"=\"cafe\"](area.a);\n"
        + ");\n"
        + "out center 200;\n";
  }

  private PlaceType mapType(Map<String, String> tags) {
    String tourism = tags.get("tourism");
    String amenity = tags.get("amenity");

    if ("attraction".equalsIgnoreCase(tourism)
        || "viewpoint".equalsIgnoreCase(tourism)
        || "museum".equalsIgnoreCase(tourism)) {
      return PlaceType.ATTRACTION;
    }
    if ("restaurant".equalsIgnoreCase(amenity)) return PlaceType.RESTAURANT;
    if ("cafe".equalsIgnoreCase(amenity)) return PlaceType.CAFE;
    return null;
  }

  private String buildAddress(Map<String, String> tags) {
    StringBuilder sb = new StringBuilder();
    append(sb, tags.get("addr:street"));
    append(sb, tags.get("addr:housenumber"));
    append(sb, tags.get("addr:suburb"));
    append(sb, tags.get("addr:city"));
    return sb.toString().trim();
  }

  private void append(StringBuilder sb, String part) {
    if (part == null || part.trim().isEmpty()) return;
    if (sb.length() > 0) sb.append(", ");
    sb.append(part);
  }
}

