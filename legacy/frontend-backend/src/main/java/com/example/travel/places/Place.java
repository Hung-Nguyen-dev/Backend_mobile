package com.example.travel.places;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.Instant;

@Entity
@Table(
    name = "place",
    indexes = {
      @Index(name = "idx_place_city_type", columnList = "city,type"),
      @Index(name = "idx_place_source_id", columnList = "source,sourcePlaceId", unique = true),
    })
public class Place {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 255)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private PlaceType type;

  private Double lat;
  private Double lng;

  @Column(length = 512)
  private String address;

  @Column(nullable = false, length = 128)
  private String city;

  @Column(nullable = false, length = 32)
  private String source; // "OSM"

  @Column(nullable = false, length = 64)
  private String sourcePlaceId; // "node/123"

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  @PrePersist
  void prePersist() {
    createdAt = Instant.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public PlaceType getType() {
    return type;
  }

  public void setType(PlaceType type) {
    this.type = type;
  }

  public Double getLat() {
    return lat;
  }

  public void setLat(Double lat) {
    this.lat = lat;
  }

  public Double getLng() {
    return lng;
  }

  public void setLng(Double lng) {
    this.lng = lng;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getSourcePlaceId() {
    return sourcePlaceId;
  }

  public void setSourcePlaceId(String sourcePlaceId) {
    this.sourcePlaceId = sourcePlaceId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}

