package com.auctionuet.protocol.contract;

import com.auctionuet.protocol.util.NetworkGson;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Dto<T extends DtoContract> {
    private final transient Class<T> contractType;
    private final transient DtoSchema schema;
    private final Map<String, Object> data;
    private transient ValidationResult lastValidation;

    private static final Gson gson = NetworkGson.create();
    private static final Type MAP_STRING_OBJECT = new TypeToken<Map<String, Object>>() {}.getType();

    private Dto(Class<T> contractType, DtoSchema schema, Object rawData) {
        this.contractType = contractType;
        this.schema = schema != null ? schema : DtoSchema.open(List.of());
        this.data = normalizeRawData(rawData);
        this.lastValidation = null;
    }

    public static <T extends DtoContract> Dto<T> of(Class<T> contractType) {
        return new Dto<>(contractType, resolveSchema(contractType), null);
    }

    public static <T extends DtoContract> Dto<T> of(Class<T> contractType, Map<String, Object> rawData) {
        return new Dto<>(contractType, resolveSchema(contractType), rawData);
    }

    public static <T extends DtoContract> Dto<T> of(Class<T> contractType, Object rawData) {
        return new Dto<>(contractType, resolveSchema(contractType), rawData);
    }

    public static Dto<?> ofSchema(DtoSchema schema) {
        return new Dto<>(null, schema, null);
    }

    public static Dto<?> ofSchema(DtoSchema schema, Map<String, Object> rawData) {
        return new Dto<>(null, schema, rawData);
    }

    public static Dto<?> ofSchema(DtoSchema schema, Object rawData) {
        return new Dto<>(null, schema, rawData);
    }

    public static Dto<?> untyped() {
        return ofSchema(DtoSchema.open(List.of()));
    }

    public static Dto<?> untyped(Map<String, Object> rawData) {
        return ofSchema(DtoSchema.open(List.of()), rawData);
    }

    public static Dto<?> untyped(Object rawData) {
        return ofSchema(DtoSchema.open(List.of()), rawData);
    }

    @SuppressWarnings("unchecked")
    private static <T extends DtoContract> DtoSchema resolveSchema(Class<T> contractType) {
        if (contractType == null) {
            throw new IllegalArgumentException("contractType cannot be null");
        }
        try {
            Field schemaField = contractType.getField("SCHEMA");
            Object schemaValue = schemaField.get(null);
            if (!(schemaValue instanceof DtoSchema)) {
                throw new IllegalArgumentException("SCHEMA of " + contractType.getName() + " must be DtoSchema");
            }
            return (DtoSchema) schemaValue;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException("Contract " + contractType.getName() + " must have public static DtoSchema SCHEMA", e);
        }
    }

    private static Map<String, Object> normalizeRawData(Object rawData) {
        if (rawData == null) {
            return new HashMap<>();
        }
        if (rawData instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    normalized.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            return normalized;
        }
        String json = gson.toJson(rawData);
        Map<String, Object> converted = gson.fromJson(json, MAP_STRING_OBJECT);
        return converted != null ? new HashMap<>(converted) : new HashMap<>();
    }

    public Dto<T> set(String key, Object value) {
        data.put(key, value);
        lastValidation = null;
        return this;
    }

    public Object get(String key) {
        return data.get(key);
    }

    public boolean containsKey(String key) {
        return data.containsKey(key);
    }

    public String getString(String key) {
        Object val = data.get(key);
        return val != null ? String.valueOf(val) : null;
    }

    public Integer getInt(String key) {
        Object val = data.get(key);
        if (val == null) return null;
        if (val instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Double getDouble(String key) {
        Object val = data.get(key);
        if (val == null) return null;
        if (val instanceof Number number) return number.doubleValue();
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Long getLong(String key) {
        Object val = data.get(key);
        if (val == null) return null;
        if (val instanceof Number number) return number.longValue();
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Boolean getBoolean(String key) {
        Object val = data.get(key);
        if (val == null) return null;
        if (val instanceof Boolean bool) return bool;
        String normalized = val.toString().trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized)) return true;
        if ("false".equals(normalized)) return false;
        return null;
    }

    public <E> List<E> getAsList(String key, Class<E> clazz) {
        Object val = data.get(key);
        if (!(val instanceof List<?>)) {
            return null;
        }
        Type listType = TypeToken.getParameterized(List.class, clazz).getType();
        return gson.fromJson(gson.toJson(val), listType);
    }

    public <C extends DtoContract> Dto<C> getAsDto(String key, Class<C> childContract) {
        Object val = data.get(key);
        if (!(val instanceof Map<?, ?>)) {
            return null;
        }
        return Dto.of(childContract, val);
    }

    public Map<String, Object> toMap() {
        return new HashMap<>(data);
    }

    public <E> E toObject(Class<E> clazz) {
        return gson.fromJson(gson.toJson(data), clazz);
    }

    public Class<T> contractType() {
        return contractType;
    }

    public DtoSchema schema() {
        return schema;
    }

    public boolean isValid() {
        return validateResult().isValid();
    }

    public ValidationResult validateResult() {
        if (lastValidation != null) {
            return lastValidation;
        }
        List<String> errors = new ArrayList<>();
        List<FieldDef> defs = schema.fields();
        Set<String> known = new HashSet<>();
        for (FieldDef def : defs) {
            known.add(def.getName());
        }

        if (!schema.allowUnknownFields()) {
            for (String key : data.keySet()) {
                if (!known.contains(key)) {
                    errors.add("Unknown field: " + key);
                }
            }
        }

        for (FieldDef field : defs) {
            String name = field.getName();
            Object value = data.get(name);
            if (field.isRequired() && isBlank(value)) {
                errors.add("Missing required field: " + name);
                continue;
            }
            if (isBlank(value)) {
                continue;
            }

            Object converted;
            try {
                converted = coerce(value, field.getType());
            } catch (IllegalArgumentException e) {
                errors.add("Field '" + name + "' " + e.getMessage());
                continue;
            }

            data.put(name, converted);
            if (field.getValidator() != null) {
                try {
                    String error = field.getValidator().apply(converted);
                    if (error != null && !error.isBlank()) {
                        errors.add(error);
                    }
                } catch (Exception e) {
                    errors.add("Field '" + name + "' failed custom validation");
                }
            }
        }

        lastValidation = ValidationResult.of(errors);
        return lastValidation;
    }

    public void validate() {
        validateResult().requireValid();
    }

    public void requireValid() {
        validate();
    }

    public List<String> validationErrors() {
        return validateResult().errors();
    }

    private static boolean isBlank(Object value) {
        return value == null || (value instanceof String str && str.trim().isEmpty());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object coerce(Object value, Class<?> expectedType) {
        if (expectedType == Object.class || value == null) {
            return value;
        }
        if (expectedType.isInstance(value)) {
            return value;
        }
        if (expectedType == String.class) {
            return String.valueOf(value);
        }
        if (expectedType == Integer.class) {
            if (value instanceof Number number) return number.intValue();
            return Integer.parseInt(value.toString());
        }
        if (expectedType == Long.class) {
            if (value instanceof Number number) return number.longValue();
            return Long.parseLong(value.toString());
        }
        if (expectedType == Double.class) {
            if (value instanceof Number number) return number.doubleValue();
            return Double.parseDouble(value.toString());
        }
        if (expectedType == Boolean.class) {
            if (value instanceof Boolean bool) return bool;
            String normalized = value.toString().trim().toLowerCase(Locale.ROOT);
            if ("true".equals(normalized)) return true;
            if ("false".equals(normalized)) return false;
            throw new IllegalArgumentException("must be boolean");
        }
        if (expectedType.isEnum()) {
            return Enum.valueOf((Class<? extends Enum>) expectedType, String.valueOf(value));
        }
        if ((expectedType == List.class && value instanceof List<?>)
                || (expectedType == Map.class && value instanceof Map<?, ?>)) {
            return value;
        }
        try {
            return gson.fromJson(gson.toJson(value), expectedType);
        } catch (Exception e) {
            throw new IllegalArgumentException("has invalid type");
        }
    }
}
