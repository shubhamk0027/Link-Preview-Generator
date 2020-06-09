package com.shubh.linkpreview;

public class Meta {

    private String originalUrl;
    private String title;
    private String description;
    private String image;
    private String shortenUrl;
    private String domainName;

    public String getOriginalUrl(){ return originalUrl; }
    public String getTitle(){ return title; }
    public String getDescription(){ return description; }
    public String getImage(){ return image; }
    public String getShortenUrl(){ return shortenUrl; }
    public String getDomainName(){ return domainName; }

    public void setOriginalUrl(String originalUrl) {this.originalUrl = originalUrl;}
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setImage(String image) { this.image = image;  }
    public void setShortenUrl(String shortenUrl){ this.shortenUrl=shortenUrl;}
    public void setDomainName(String domainName){ this.domainName=domainName; }
}
