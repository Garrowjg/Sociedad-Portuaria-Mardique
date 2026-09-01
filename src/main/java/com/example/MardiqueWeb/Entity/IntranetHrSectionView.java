package com.example.MardiqueWeb.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "intranet_hr_section_views", indexes = {
        @Index(name = "idx_hr_section_view_section", columnList = "section_id"),
        @Index(name = "idx_hr_section_view_email", columnList = "viewer_email")
})
public class IntranetHrSectionView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "section_id", nullable = false, length = 60)
    private String sectionId;

    @Column(name = "viewer_email", nullable = false, length = 180)
    private String viewerEmail;

    @Column(name = "viewer_name", length = 120)
    private String viewerName;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt = LocalDateTime.now();

    public IntranetHrSectionView() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSectionId() { return sectionId; }
    public void setSectionId(String sectionId) { this.sectionId = sectionId; }

    public String getViewerEmail() { return viewerEmail; }
    public void setViewerEmail(String viewerEmail) { this.viewerEmail = viewerEmail; }

    public String getViewerName() { return viewerName; }
    public void setViewerName(String viewerName) { this.viewerName = viewerName; }

    public LocalDateTime getViewedAt() { return viewedAt; }
    public void setViewedAt(LocalDateTime viewedAt) { this.viewedAt = viewedAt; }
}
