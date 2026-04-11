package com.example.travel.osm;

import java.util.List;
import java.util.Map;

/** Minimal DTO for Overpass JSON response (Java 8 compatible). */
public class OverpassResponse {
  private List<Element> elements;

  public List<Element> getElements() {
    return elements;
  }

  public void setElements(List<Element> elements) {
    this.elements = elements;
  }

  public static class Element {
    private long id;
    private String type;
    private double lat;
    private double lon;
    private Map<String, String> tags;

    public long getId() {
      return id;
    }

    public void setId(long id) {
      this.id = id;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public double getLat() {
      return lat;
    }

    public void setLat(double lat) {
      this.lat = lat;
    }

    public double getLon() {
      return lon;
    }

    public void setLon(double lon) {
      this.lon = lon;
    }

    public Map<String, String> getTags() {
      return tags;
    }

    public void setTags(Map<String, String> tags) {
      this.tags = tags;
    }
  }
}

