package com.mealup.texts;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One edited piece of website wording, for example key "home.hero.title". */
@Entity
@Table(name = "site_texts")
public class SiteText {

    @Id
    @Column(name = "text_key", length = 80)
    private String key;

    @Column(name = "text_value", nullable = false, length = 1500)
    private String value;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SiteText() { }

    public SiteText(String key, String value) {
        this.key = key;
        this.value = value;
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public void change(String newValue) {
        this.value = newValue;
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public String getKey() { return key; }
    public String getValue() { return value; }
}
