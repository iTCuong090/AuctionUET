package com.auctionuet.server.mapper;

import com.auctionuet.server.domain.model.BidRecord;
import com.auctionuet.server.network.dto.BidDTO;
import com.auctionuet.server.persistence.schema.BidSchema;
import com.auctionuet.server.util.IdGenerator;

import java.time.LocalDateTime;

public class BidMapper {
    public static BidDTO toDTO(BidRecord record, String auctionId) {
        return new BidDTO(auctionId, record.getBidderUsername(),
                record.getAmount(), record.getTimestamp());
    }

    public static BidSchema toSchema(BidRecord record, String auctionId) {
        return new BidSchema(
                IdGenerator.generate(), LocalDateTime.now(), LocalDateTime.now(),
                auctionId, record.getBidderId(), record.getAmount(), record.getTimestamp()
        );
    }

    public static BidDTO schemaToDTO(BidSchema schema) {
        return new BidDTO(schema.getAuctionId(), schema.getBidderId(),
                schema.getAmount(), schema.getTimestamp());
    }
}
