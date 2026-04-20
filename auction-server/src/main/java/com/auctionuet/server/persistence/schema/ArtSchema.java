package com.auctionuet.server.persistence.schema;

import com.auctionuet.server.domain.enums.ItemType;
import java.time.LocalDateTime;

public class ArtSchema extends ItemSchema {
    private String artist;
    private int year;
    private String medium;

    protected ArtSchema() {
    }

    public ArtSchema(String id, LocalDateTime createdAt, LocalDateTime updatedAt,
                     String name, String description, double startingPrice,
                     ItemType type, String sellerId, String imageUrl,
                     String condition, int auctionCount,
                     String artist, int year, String medium) {
        super(id, createdAt, updatedAt, name, description, startingPrice, type, sellerId, imageUrl, condition, auctionCount);
        this.artist = artist;
        this.year = year;
        this.medium = medium;
    }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    
    public String getMedium() { return medium; }
    public void setMedium(String medium) { this.medium = medium; }
}
