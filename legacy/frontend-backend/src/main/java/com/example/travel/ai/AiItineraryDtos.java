package com.example.travel.ai;

import java.util.List;

/** DTOs khớp schema frontend (Java 8 compatible). */
public final class AiItineraryDtos {
  private AiItineraryDtos() {}

  public static class AiItineraryRequest {
    private String destination;
    private int dayCount;
    private List<String> preferences;
    private String budgetTier;
    private String startDate;

    public String getDestination() {
      return destination;
    }

    public void setDestination(String destination) {
      this.destination = destination;
    }

    public int getDayCount() {
      return dayCount;
    }

    public void setDayCount(int dayCount) {
      this.dayCount = dayCount;
    }

    public List<String> getPreferences() {
      return preferences;
    }

    public void setPreferences(List<String> preferences) {
      this.preferences = preferences;
    }

    public String getBudgetTier() {
      return budgetTier;
    }

    public void setBudgetTier(String budgetTier) {
      this.budgetTier = budgetTier;
    }

    public String getStartDate() {
      return startDate;
    }

    public void setStartDate(String startDate) {
      this.startDate = startDate;
    }
  }

  public static class SuggestedRestaurant {
    private String id;
    private String name;
    private String note;

    public SuggestedRestaurant() {}

    public SuggestedRestaurant(String id, String name, String note) {
      this.id = id;
      this.name = name;
      this.note = note;
    }

    public String getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public String getNote() {
      return note;
    }
  }

  public static class SuggestedActivity {
    private String id;
    private String title;
    private String description;
    private String estimatedDuration;
    private String suggestedStart;

    public SuggestedActivity() {}

    public SuggestedActivity(
        String id,
        String title,
        String description,
        String estimatedDuration,
        String suggestedStart) {
      this.id = id;
      this.title = title;
      this.description = description;
      this.estimatedDuration = estimatedDuration;
      this.suggestedStart = suggestedStart;
    }

    public String getId() {
      return id;
    }

    public String getTitle() {
      return title;
    }

    public String getDescription() {
      return description;
    }

    public String getEstimatedDuration() {
      return estimatedDuration;
    }

    public String getSuggestedStart() {
      return suggestedStart;
    }
  }

  public static class SuggestedDay {
    private int dayIndex;
    private String label;
    private List<SuggestedActivity> activities;
    private List<SuggestedRestaurant> restaurants;

    public SuggestedDay() {}

    public SuggestedDay(
        int dayIndex,
        String label,
        List<SuggestedActivity> activities,
        List<SuggestedRestaurant> restaurants) {
      this.dayIndex = dayIndex;
      this.label = label;
      this.activities = activities;
      this.restaurants = restaurants;
    }

    public int getDayIndex() {
      return dayIndex;
    }

    public String getLabel() {
      return label;
    }

    public List<SuggestedActivity> getActivities() {
      return activities;
    }

    public List<SuggestedRestaurant> getRestaurants() {
      return restaurants;
    }
  }

  public static class AiItineraryResponse {
    private String summary;
    private List<SuggestedDay> days;
    private String generatedAt;

    public AiItineraryResponse() {}

    public AiItineraryResponse(String summary, List<SuggestedDay> days, String generatedAt) {
      this.summary = summary;
      this.days = days;
      this.generatedAt = generatedAt;
    }

    public String getSummary() {
      return summary;
    }

    public List<SuggestedDay> getDays() {
      return days;
    }

    public String getGeneratedAt() {
      return generatedAt;
    }
  }
}

