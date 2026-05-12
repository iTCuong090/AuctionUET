package com.auctionuet.protocol.contract;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DtoSchema {
    private final List<FieldDef> fields;
    private final boolean allowUnknownFields;

    private DtoSchema(List<FieldDef> fields, boolean allowUnknownFields) {
        this.fields = fields != null ? List.copyOf(fields) : List.of();
        this.allowUnknownFields = allowUnknownFields;
    }

    public static DtoSchema strict(List<FieldDef> fields) {
        return new DtoSchema(fields, false);
    }

    public static DtoSchema open(List<FieldDef> fields) {
        return new DtoSchema(fields, true);
    }

    public static DtoSchema strict(FieldDef... fields) {
        Objects.requireNonNull(fields, "fields");
        List<FieldDef> defs = new ArrayList<>(fields.length);
        for (FieldDef field : fields) {
            defs.add(field);
        }
        return strict(defs);
    }

    public static DtoSchema open(FieldDef... fields) {
        Objects.requireNonNull(fields, "fields");
        List<FieldDef> defs = new ArrayList<>(fields.length);
        for (FieldDef field : fields) {
            defs.add(field);
        }
        return open(defs);
    }

    public List<FieldDef> fields() {
        return fields;
    }

    public boolean allowUnknownFields() {
        return allowUnknownFields;
    }
}
