package com.example.tunipromos.model;

public class Promotion {
    private String promoId;
    private String title;
    private String description;
    private double priceBefore;
    private double priceAfter;
    private String category;
    private String providerId;
    private String startDate;
    private String endDate;
    private String imageUrl;

    public Promotion() {
        // Constructeur vide requis pour Firestore
    }

    public Promotion(String promoId, String title, String description, double priceBefore, double priceAfter, String category, String providerId, String startDate, String endDate, String imageUrl) {
        this.promoId = promoId;
        this.title = title;
        this.description = description;
        this.priceBefore = priceBefore;
        this.priceAfter = priceAfter;
        this.category = category;
        this.providerId = providerId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.imageUrl = imageUrl;
    }

    public String getPromoId() { return promoId; }
    public void setPromoId(String promoId) { this.promoId = promoId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPriceBefore() { return priceBefore; }
    public void setPriceBefore(double priceBefore) { this.priceBefore = priceBefore; }

    public double getPriceAfter() { return priceAfter; }
    public void setPriceAfter(double priceAfter) { this.priceAfter = priceAfter; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
