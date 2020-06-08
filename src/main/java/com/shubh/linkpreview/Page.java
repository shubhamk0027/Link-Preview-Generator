package com.shubh.linkpreview;

public class Page {

    private String originalUrl;
    private String title;
    private String description;
    private String image;
    private String shortenUrl;

    public String getOriginalUrl(){ return originalUrl; }
    public String getTitle(){ return title; }
    public String getDescription(){ return description; }
    public String getImage(){ return image; }
    public String getShortenUrl(){ return shortenUrl; }

    public void setOriginalUrl(String originalUrl) {this.originalUrl = originalUrl;}
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setImage(String image) { this.image = image;  }
    public void setShortenUrl(String shortenUrl){ this.shortenUrl=shortenUrl;}

}
