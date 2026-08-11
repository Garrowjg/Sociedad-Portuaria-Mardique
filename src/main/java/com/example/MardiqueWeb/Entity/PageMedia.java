package com.example.MardiqueWeb.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "page_media")
public class PageMedia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String page;

    @Column(name = "media_key")
    private String mediaKey;

    private String label;

    private Integer orden;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "default_path")
    private String defaultPath;

    @Column(name = "media_type")
    private String mediaType;

    private boolean active = true;

    public PageMedia() {}

    public PageMedia(String page, String mediaKey, String label, Integer orden, String defaultPath, String mediaType) {
        this.page = page;
        this.mediaKey = mediaKey;
        this.label = label;
        this.orden = orden;
        this.defaultPath = defaultPath;
        this.mediaType = mediaType;
    }

    public String getDisplayUrl() {
        return (filePath != null && !filePath.isEmpty()) ? filePath : defaultPath;
    }

    public String getTypeLabel() {
        return "VIDEO".equalsIgnoreCase(mediaType) ? "Video" : "Imagen";
    }

    public boolean isVideo() {
        return "VIDEO".equalsIgnoreCase(mediaType);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPage() { return page; }
    public void setPage(String page) { this.page = page; }
    public String getMediaKey() { return mediaKey; }
    public void setMediaKey(String mediaKey) { this.mediaKey = mediaKey; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getDefaultPath() { return defaultPath; }
    public void setDefaultPath(String defaultPath) { this.defaultPath = defaultPath; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}