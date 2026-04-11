package com.mobilebackend.ungdunglapkehoachdulich.dto.ai;

import java.util.List;

public final class AiItineraryDtos {
    private AiItineraryDtos() {
    }

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
        private String description;
        private String address;
        private String type;
        private String hours;
        private String priceRange;
        private Double rating;
        private Integer reviews;
        private String thumbnail;
        private String mapLink;

        public SuggestedRestaurant() {
        }

        public SuggestedRestaurant(
                String id,
                String name,
                String note,
                String description,
                String address,
                String type,
                String hours,
                String priceRange,
                Double rating,
                Integer reviews,
                String thumbnail,
                String mapLink
        ) {
            this.id = id;
            this.name = name;
            this.note = note;
            this.description = description;
            this.address = address;
            this.type = type;
            this.hours = hours;
            this.priceRange = priceRange;
            this.rating = rating;
            this.reviews = reviews;
            this.thumbnail = thumbnail;
            this.mapLink = mapLink;
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

        public String getDescription() {
            return description;
        }

        public String getAddress() {
            return address;
        }

        public String getType() {
            return type;
        }

        public String getHours() {
            return hours;
        }

        public String getPriceRange() {
            return priceRange;
        }

        public Double getRating() {
            return rating;
        }

        public Integer getReviews() {
            return reviews;
        }

        public String getThumbnail() {
            return thumbnail;
        }

        public String getMapLink() {
            return mapLink;
        }
    }

    public static class SuggestedActivity {
        private String id;
        private String title;
        private String description;
        private String estimatedDuration;
        private String suggestedStart;
        private String address;
        private String type;
        private String hours;
        private String priceRange;
        private Double rating;
        private Integer reviews;
        private String thumbnail;
        private String mapLink;

        public SuggestedActivity() {
        }

        public SuggestedActivity(
                String id,
                String title,
                String description,
                String estimatedDuration,
                String suggestedStart,
                String address,
                String type,
                String hours,
                String priceRange,
                Double rating,
                Integer reviews,
                String thumbnail,
                String mapLink
        ) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.estimatedDuration = estimatedDuration;
            this.suggestedStart = suggestedStart;
            this.address = address;
            this.type = type;
            this.hours = hours;
            this.priceRange = priceRange;
            this.rating = rating;
            this.reviews = reviews;
            this.thumbnail = thumbnail;
            this.mapLink = mapLink;
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

        public String getAddress() {
            return address;
        }

        public String getType() {
            return type;
        }

        public String getHours() {
            return hours;
        }

        public String getPriceRange() {
            return priceRange;
        }

        public Double getRating() {
            return rating;
        }

        public Integer getReviews() {
            return reviews;
        }

        public String getThumbnail() {
            return thumbnail;
        }

        public String getMapLink() {
            return mapLink;
        }
    }

    public static class SuggestedDay {
        private int dayIndex;
        private String label;
        private List<SuggestedActivity> activities;
        private List<SuggestedRestaurant> restaurants;

        public SuggestedDay() {
        }

        public SuggestedDay(
                int dayIndex,
                String label,
                List<SuggestedActivity> activities,
                List<SuggestedRestaurant> restaurants
        ) {
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

        public AiItineraryResponse() {
        }

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
