package com.auctionuet.protocol.contract;

import java.util.Objects;
import java.util.function.Function;

public class FieldDef {
    private final String name;
    private final Class<?> type;
    private final boolean required;
    private final Function<Object, String> validator;

    private FieldDef(String name, Class<?> type, boolean required, Function<Object, String> validator) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.required = required;
        this.validator = validator;
    }

    public static FieldDef required(String name, Class<?> type) {
        return new FieldDef(name, type, true, null);
    }

    public static FieldDef required(String name, Class<?> type, Function<Object, String> validator) {
        return new FieldDef(name, type, true, validator);
    }

    public static FieldDef optional(String name, Class<?> type) {
        return new FieldDef(name, type, false, null);
    }

    public static FieldDef optional(String name, Class<?> type, Function<Object, String> validator) {
        return new FieldDef(name, type, false, validator);
    }

    public String getName() { return name; }
    public Class<?> getType() { return type; }
    public boolean isRequired() { return required; }
    public Function<Object, String> getValidator() { return validator; }
}
