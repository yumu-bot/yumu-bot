package com.now.nowbot.model.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;

public class UserGroup {

    @JsonProperty("colour")
    String color;

    @JsonProperty("has_listing")
    boolean hasListing;

    @JsonProperty("has_playmodes")
    boolean hasPlaymodes;

    Integer id;

    String identifier;

    @JsonProperty("is_probationary")
    boolean isProbationary;

    String name;

    @JsonProperty("short_name")
    String shortName;

    @Nullable
    String[] playmodes;

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isHasListing() {
        return hasListing;
    }

    public void setHasListing(boolean hasListing) {
        this.hasListing = hasListing;
    }

    public boolean isHasPlaymodes() {
        return hasPlaymodes;
    }

    public void setHasPlaymodes(boolean hasPlaymodes) {
        this.hasPlaymodes = hasPlaymodes;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public boolean isProbationary() {
        return isProbationary;
    }

    public void setProbationary(boolean probationary) {
        isProbationary = probationary;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String[] getPlaymodes() {
        return playmodes;
    }

    public void setPlaymodes(@Nullable String[] playmodes) {
        this.playmodes = playmodes;
    }
}
