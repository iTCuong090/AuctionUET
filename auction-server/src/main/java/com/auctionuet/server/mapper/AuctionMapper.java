package com.auctionuet.server.mapper;

import com.auctionuet.server.domain.enums.AuctionStatus;
import com.auctionuet.server.domain.model.LiveAuction;
import com.auctionuet.server.network.dto.AuctionDTO;
import com.auctionuet.server.network.dto.ItemDTO;
import com.auctionuet.server.persistence.schema.AuctionSchema;
import com.auctionuet.server.util.IdGenerator;

import java.time.LocalDateTime;

public class AuctionMapper {

    public static LiveAuction toDomain(AuctionSchema schema) {
        return new LiveAuction(
                schema.getId(), schema.getItemId(), schema.getSellerId(),
                schema.getEndTime(), schema.getStatus(),
                schema.getHighestBid(), schema.getWinnerId(),
                schema.getAntiSnipingWindowSeconds(),
                schema.getAntiSnipingExtensionSeconds()
        );
    }

    public static AuctionDTO toDTO(AuctionSchema schema, ItemDTO itemDTO,
                                   String sellerUsername, String winnerUsername ) {
        return new AuctionDTO (
                schema.getId(),itemDTO,sellerUsername, // SellerUsername phải được tìm = ID khi gọi toDTO
                schema.getTitle(), schema.getDescription(),
                schema.getStartTime(), schema.getEndTime(),
                schema.getStatus(), schema.getHighestBid(),
                winnerUsername // SellerUsername phải được tìm = ID khi gọi toDTO
        );
    }

    public static AuctionSchema toNewSchema(String itemId, String sellerId,
                                            LocalDateTime startTime, LocalDateTime endTime,
                                            String title, String description) {
        return new AuctionSchema(
                IdGenerator.generate(), LocalDateTime.now(), LocalDateTime.now(),
                itemId, sellerId, title, description,
                startTime, endTime, AuctionStatus.OPEN, 0.0, null,
                60, 120
        );
    }

    public static AuctionSchema toSchema(LiveAuction auction) {
        // Chuyển từ RAM state về Schema để ghi file
        // Dùng khi startAuction hoặc endAuction
        AuctionSchema schema = new AuctionSchema();

        schema.setId(auction.getId());
        schema.setStatus(auction.getStatus());
        schema.setHighestBid(auction.getCurrentHighestBid());
        schema.setWinnerId(auction.getCurrentWinnerId());
        schema.setUpdatedAt(LocalDateTime.now());
        return schema;
    }
}
