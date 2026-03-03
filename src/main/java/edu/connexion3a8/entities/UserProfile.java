package edu.connexion3a8.entities;

import java.sql.Timestamp;

public class UserProfile {
    private String id;
    private String userId;
    private String phone;
    private String location;
    private String website;
    private String linkedinUrl;
    private String twitterUrl;
    private String company;
    private String jobTitle;
    private String skills;
    private String interests;
    private String investmentDomains;
    private String pastProjects;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public UserProfile() {
    }

    public UserProfile(String userId) {
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public void setLinkedinUrl(String linkedinUrl) {
        this.linkedinUrl = linkedinUrl;
    }

    public String getTwitterUrl() {
        return twitterUrl;
    }

    public void setTwitterUrl(String twitterUrl) {
        this.twitterUrl = twitterUrl;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getInterests() {
        return interests;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }

    public String getInvestmentDomains() {
        return investmentDomains;
    }

    public void setInvestmentDomains(String investmentDomains) {
        this.investmentDomains = investmentDomains;
    }

    public String getPastProjects() {
        return pastProjects;
    }

    public void setPastProjects(String pastProjects) {
        this.pastProjects = pastProjects;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "UserProfile{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", phone='" + phone + '\'' +
                ", location='" + location + '\'' +
                ", company='" + company + '\'' +
                ", jobTitle='" + jobTitle + '\'' +
                '}';
    }
}
