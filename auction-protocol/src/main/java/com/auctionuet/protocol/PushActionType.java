package com.auctionuet.protocol;

import com.auctionuet.protocol.contract.Dto;
import com.auctionuet.protocol.contract.DtoSchema;
import com.auctionuet.protocol.contract.FieldDef;

import java.util.List;
import java.util.Map;

public enum PushActionType {
    BID_UPDATE(List.of(
            FieldDef.required("auctionId", String.class),
            FieldDef.required("bidAmount", Double.class),
            FieldDef.required("bidder", String.class),
            FieldDef.required("time", String.class)
    )),
    AUCTION_ENDED(List.of(
            FieldDef.required("auctionId", String.class),
            FieldDef.optional("winner", String.class),
            FieldDef.optional("winningBid", Double.class)
    )),
    AUCTION_EXTENDED(List.of(
            FieldDef.required("auctionId", String.class),
            FieldDef.required("newEndTime", String.class)
    ));

    private final List<FieldDef> schema;

    PushActionType(List<FieldDef> schema) {
        this.schema = schema;
    }

    public List<FieldDef> getSchema() {
        return schema;
    }

    public Dto<?> createData() {
        return Dto.ofSchema(DtoSchema.strict(schema));
    }

    public Dto<?> parseData(Map<String, Object> raw) {
        return Dto.ofSchema(DtoSchema.strict(schema), raw);
    }
}
