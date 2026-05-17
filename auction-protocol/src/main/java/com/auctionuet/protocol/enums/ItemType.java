package com.auctionuet.protocol.enums;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public enum ItemType {
    ELECTRONICS(Set.of("brand", "warrantyMonths")) {
        @Override
        protected Map<String, Object> normalizeRequired(Map<String, Object> extraFields) {
            String brand = requireNonBlankString(extraFields, "brand");
            int warrantyMonths = requireInt(extraFields, "warrantyMonths", 0, 120);

            Map<String, Object> normalized = new LinkedHashMap<>();
            normalized.put("brand", brand);
            normalized.put("warrantyMonths", warrantyMonths);
            return normalized;
        }
    },
    ART(Set.of("artist", "year", "medium")) {
        @Override
        protected Map<String, Object> normalizeRequired(Map<String, Object> extraFields) {
            String artist = requireNonBlankString(extraFields, "artist");
            int year = requireInt(extraFields, "year", 1, 9999);
            String medium = requireNonBlankString(extraFields, "medium");

            Map<String, Object> normalized = new LinkedHashMap<>();
            normalized.put("artist", artist);
            normalized.put("year", year);
            normalized.put("medium", medium);
            return normalized;
        }
    },
    VEHICLE(Set.of("make", "model", "mileage", "vehicleYear")) {
        @Override
        protected Map<String, Object> normalizeRequired(Map<String, Object> extraFields) {
            String make = requireNonBlankString(extraFields, "make");
            String model = requireNonBlankString(extraFields, "model");
            int mileage = requireInt(extraFields, "mileage", 0, Integer.MAX_VALUE);
            int vehicleYear = requireInt(extraFields, "vehicleYear", 1886, 9999);

            Map<String, Object> normalized = new LinkedHashMap<>();
            normalized.put("make", make);
            normalized.put("model", model);
            normalized.put("mileage", mileage);
            normalized.put("vehicleYear", vehicleYear);
            return normalized;
        }
    };

    private final Set<String> requiredKeys;

    ItemType(Set<String> requiredKeys) {
        this.requiredKeys = requiredKeys;
    }

    public final Map<String, Object> normalizeAndValidateExtraFields(Map<String, Object> extraFields) {
        if (extraFields == null) {
            throw new IllegalArgumentException("extraFields must not be null");
        }

        if (extraFields.size() != requiredKeys.size()) {
            throw new IllegalArgumentException("extraFields size is invalid for type " + name());
        }

        for (String key : extraFields.keySet()) {
            if (!requiredKeys.contains(key)) {
                throw new IllegalArgumentException("Unexpected extra field: " + key + " for type " + name());
            }
        }

        return normalizeRequired(extraFields);
    }

    protected abstract Map<String, Object> normalizeRequired(Map<String, Object> extraFields);

    protected static String requireNonBlankString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (!(value instanceof String str) || str.isBlank()) {
            throw new IllegalArgumentException("extraFields." + key + " must be a non-blank string");
        }
        return str;
    }

    protected static int requireInt(Map<String, Object> data, String key, int min, int max) {
        Object value = data.get(key);
        int parsed;
        if (value instanceof Number n) {
            parsed = n.intValue();
        } else if (value instanceof String s) {
            try {
                parsed = Integer.parseInt(s.trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("extraFields." + key + " must be an integer");
            }
        } else {
            throw new IllegalArgumentException("extraFields." + key + " must be an integer");
        }

        if (parsed < min || parsed > max) {
            throw new IllegalArgumentException("extraFields." + key + " must be in range [" + min + ", " + max + "]");
        }
        return parsed;
    }
}
