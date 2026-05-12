package com.auctionuet.protocol.enums;

import com.auctionuet.protocol.contract.Dto;
import com.auctionuet.protocol.contract.DtoSchema;
import com.auctionuet.protocol.contract.FieldDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public enum ItemType {
    ELECTRONICS(List.of(
            FieldDef.optional("brand", String.class),
            FieldDef.optional("model", String.class),
            FieldDef.optional("warrantyMonths", Integer.class, v ->
                    (Integer) v < 0 ? "Bảo hành không được âm" : null)
    )), // Đồ điện tử
    ART(List.of(
            FieldDef.optional("artist", String.class),
            FieldDef.optional("year", Integer.class),
            FieldDef.optional("medium", String.class)
    )),         // Đồ nghệ thuật
    VEHICLE(List.of(
            FieldDef.optional("make", String.class),
            FieldDef.optional("modelYear", Integer.class),
            FieldDef.optional("mileage", Integer.class, v ->
                    (Integer) v < 0 ? "Số km không được âm" : null)
    ));      // Phương tiện

    private final List<FieldDef> schema;

    ItemType(List<FieldDef> extraFields) {
        List<FieldDef> fullSchema = new ArrayList<>(List.of(
                FieldDef.optional("id", String.class),
                FieldDef.required("name", String.class),
                FieldDef.required("description", String.class),
                FieldDef.required("startingPrice", Double.class, v ->
                        (Double) v <= 0 ? "Giá khởi điểm phải lớn hơn 0" : null),
                FieldDef.required("type", String.class),
                FieldDef.optional("sellerUsername", String.class),
                FieldDef.optional("imageUrl", String.class),
                FieldDef.optional("condition", String.class)
        ));
        fullSchema.addAll(extraFields);
        this.schema = fullSchema;
    }

    public List<FieldDef> getSchema() {
        return schema;
    }

    public Dto<?> createData() {
        return Dto.ofSchema(DtoSchema.open(schema));
    }

    public Dto<?> parseData(Map<String, Object> raw) {
        return Dto.ofSchema(DtoSchema.open(schema), raw);
    }
    
    public Dto<?> parseData(Dto<?> dto) {
        return Dto.ofSchema(DtoSchema.open(schema), dto != null ? dto.toMap() : null);
    }
}
