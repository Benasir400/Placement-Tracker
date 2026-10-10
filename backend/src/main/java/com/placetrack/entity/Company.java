package com.placetrack.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "companies")
public class Company extends BaseEntity {
    @Column(nullable = false, unique = true, length = 160)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(length = 500)
    private String website;

    @Column(length = 500)
    private String logoUrl;

    @Column(length = 120)
    private String industry;

    @Column(nullable = false)
    private boolean archived = false;

    protected Company() {
    }

    public Company(String name, String description, String website, String logoUrl, String industry) {
        this.name = name;
        this.description = description;
        this.website = website;
        this.logoUrl = logoUrl;
        this.industry = industry;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getWebsite() {
        return website;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getIndustry() {
        return industry;
    }

    public boolean isArchived() {
        return archived;
    }

    public void update(String name, String description, String website, String logoUrl, String industry) {
        this.name = name;
        this.description = description;
        this.website = website;
        this.logoUrl = logoUrl;
        this.industry = industry;
    }
}
