package com.example.MardiqueWeb.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "page_contents")
public class PageContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String page;

    @Column(name = "section_key", nullable = false)
    private String sectionKey;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String label;

    public PageContent() {}

    public PageContent(String page, String sectionKey, String content, String label) {
        this.page = page;
        this.sectionKey = sectionKey;
        this.content = content;
        this.label = label;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPage() { return page; }
    public void setPage(String page) { this.page = page; }
    public String getSectionKey() { return sectionKey; }
    public void setSectionKey(String sectionKey) { this.sectionKey = sectionKey; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
