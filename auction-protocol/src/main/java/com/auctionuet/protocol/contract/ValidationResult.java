package com.auctionuet.protocol.contract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidationResult {
    private final List<String> errors;

    private ValidationResult(List<String> errors) {
        this.errors = errors == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(errors));
    }

    public static ValidationResult ok() {
        return new ValidationResult(List.of());
    }

    public static ValidationResult of(List<String> errors) {
        return new ValidationResult(errors);
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public List<String> errors() {
        return errors;
    }

    public String firstError() {
        return errors.isEmpty() ? null : errors.getFirst();
    }

    public void requireValid() {
        if (!isValid()) {
            throw new IllegalArgumentException(firstError());
        }
    }
}
