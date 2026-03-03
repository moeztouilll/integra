package edu.connexion3a8.services.gamification;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

/**
 * Service class for interacting with YouTube Data API v3
 * Fetches video metadata like title, description, thumbnail, duration, etc.
 */
public class YouTubeApiService {
    
    // TODO: Replace with your actual API key from Google Cloud Console
    // Get your API key: https://console.cloud.google.com/apis/credentials
    private static final String API_KEY = "YOUR_API_KEY_HERE";
    
    private static final String APPLICATION_NAME = "Connections3a8-LMS";
    
    private YouTube youtube;
    
    public YouTubeApiService() {
        try {
            youtube = new YouTube.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                null
            )
            .setApplicationName(APPLICATION_NAME)
            .build();
        } catch (GeneralSecurityException | IOException e) {
            System.err.println("Error initializing YouTube API: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Fetches video information from YouTube API
     * @param videoId YouTube video ID
     * @return YouTubeVideoInfo object with video details
     */
    public YouTubeVideoInfo getVideoInfo(String videoId) {
        if (API_KEY.equals("YOUR_API_KEY_HERE")) {
            System.err.println("YouTube API key not configured. Please set your API key in YouTubeApiService.java");
            return null;
        }
        
        try {
            YouTube.Videos.List request = youtube.videos()
                .list(Arrays.asList("snippet", "contentDetails", "statistics", "status"));
            
            VideoListResponse response = request
                .setKey(API_KEY)
                .setId(Arrays.asList(videoId))
                .execute();
            
            List<Video> videos = response.getItems();
            
            if (videos.isEmpty()) {
                System.err.println("No video found with ID: " + videoId);
                return null;
            }
            
            Video video = videos.get(0);
            
            // Extract video information
            YouTubeVideoInfo info = new YouTubeVideoInfo();
            info.setVideoId(videoId);
            info.setTitle(video.getSnippet().getTitle());
            info.setDescription(video.getSnippet().getDescription());
            info.setChannelTitle(video.getSnippet().getChannelTitle());
            info.setPublishedAt(video.getSnippet().getPublishedAt().toString());
            
            // Thumbnails (get highest quality available)
            if (video.getSnippet().getThumbnails().getMaxres() != null) {
                info.setThumbnailUrl(video.getSnippet().getThumbnails().getMaxres().getUrl());
            } else if (video.getSnippet().getThumbnails().getHigh() != null) {
                info.setThumbnailUrl(video.getSnippet().getThumbnails().getHigh().getUrl());
            } else if (video.getSnippet().getThumbnails().getMedium() != null) {
                info.setThumbnailUrl(video.getSnippet().getThumbnails().getMedium().getUrl());
            }
            
            // Duration (ISO 8601 format like PT15M33S)
            info.setDuration(video.getContentDetails().getDuration());
            
            // Statistics
            if (video.getStatistics() != null) {
                info.setViewCount(video.getStatistics().getViewCount() != null ? 
                    video.getStatistics().getViewCount().longValue() : 0);
                info.setLikeCount(video.getStatistics().getLikeCount() != null ? 
                    video.getStatistics().getLikeCount().longValue() : 0);
            }
            
            // Check if embeddable
            info.setEmbeddable(video.getStatus().getEmbeddable());
            
            return info;
            
        } catch (IOException e) {
            System.err.println("Error fetching video info: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Converts ISO 8601 duration to minutes
     * Example: PT15M33S -> 15 minutes
     */
    public static int parseDurationToMinutes(String isoDuration) {
        if (isoDuration == null || isoDuration.isEmpty()) {
            return 0;
        }
        
        try {
            // Remove PT prefix
            String duration = isoDuration.replace("PT", "");
            
            int hours = 0;
            int minutes = 0;
            int seconds = 0;
            
            // Parse hours
            if (duration.contains("H")) {
                String[] parts = duration.split("H");
                hours = Integer.parseInt(parts[0]);
                duration = parts.length > 1 ? parts[1] : "";
            }
            
            // Parse minutes
            if (duration.contains("M")) {
                String[] parts = duration.split("M");
                minutes = Integer.parseInt(parts[0]);
                duration = parts.length > 1 ? parts[1] : "";
            }
            
            // Parse seconds
            if (duration.contains("S")) {
                String[] parts = duration.split("S");
                seconds = Integer.parseInt(parts[0]);
            }
            
            // Convert to total minutes (round up if seconds > 30)
            int totalMinutes = hours * 60 + minutes;
            if (seconds > 30) {
                totalMinutes++;
            }
            
            return totalMinutes;
            
        } catch (Exception e) {
            System.err.println("Error parsing duration: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Inner class to hold YouTube video information
     */
    public static class YouTubeVideoInfo {
        private String videoId;
        private String title;
        private String description;
        private String channelTitle;
        private String thumbnailUrl;
        private String duration;
        private String publishedAt;
        private long viewCount;
        private long likeCount;
        private boolean embeddable;
        
        // Getters and setters
        public String getVideoId() { return videoId; }
        public void setVideoId(String videoId) { this.videoId = videoId; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getChannelTitle() { return channelTitle; }
        public void setChannelTitle(String channelTitle) { this.channelTitle = channelTitle; }
        
        public String getThumbnailUrl() { return thumbnailUrl; }
        public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
        
        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }
        
        public String getPublishedAt() { return publishedAt; }
        public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }
        
        public long getViewCount() { return viewCount; }
        public void setViewCount(long viewCount) { this.viewCount = viewCount; }
        
        public long getLikeCount() { return likeCount; }
        public void setLikeCount(long likeCount) { this.likeCount = likeCount; }
        
        public boolean isEmbeddable() { return embeddable; }
        public void setEmbeddable(boolean embeddable) { this.embeddable = embeddable; }
        
        public int getDurationInMinutes() {
            return parseDurationToMinutes(duration);
        }
        
        @Override
        public String toString() {
            return "YouTubeVideoInfo{" +
                    "videoId='" + videoId + '\'' +
                    ", title='" + title + '\'' +
                    ", channelTitle='" + channelTitle + '\'' +
                    ", duration='" + duration + '\'' +
                    ", viewCount=" + viewCount +
                    ", embeddable=" + embeddable +
                    '}';
        }
    }
}
