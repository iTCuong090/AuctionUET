package com.auctionuet.client.view;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

final class CurrencyFormatter {
    private static final double COMPACT_THRESHOLD = 1_000_000_000.0;
    private static final double SCIENTIFIC_THRESHOLD = 1_000_000_000_000_000_000.0;
    private static final DecimalFormat COMPACT_FORMAT =
            new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat SCIENTIFIC_FORMAT =
            new DecimalFormat("0.##E0", DecimalFormatSymbols.getInstance(Locale.US));

    private CurrencyFormatter() {}

    static String format(double amount) {
        if (!Double.isFinite(amount)) {
            return "-- VND";
        }

        double absAmount = Math.abs(amount);
        if (absAmount < COMPACT_THRESHOLD) {
            return String.format("%,.0f VND", amount);
        }
        if (absAmount >= SCIENTIFIC_THRESHOLD) {
            return SCIENTIFIC_FORMAT.format(amount) + " VND";
        }

        CompactUnit unit = findUnit(absAmount);
        return COMPACT_FORMAT.format(amount / unit.divisor) + unit.suffix + " VND";
    }

    static String formatFull(double amount) {
        if (!Double.isFinite(amount)) {
            return "-- VND";
        }
        if (Math.abs(amount) >= SCIENTIFIC_THRESHOLD) {
            return SCIENTIFIC_FORMAT.format(amount) + " VND";
        }
        return String.format("%,.0f VND", amount);
    }

    static void setMoneyText(Label label, double amount) {
        if (label == null) {
            return;
        }
        label.setText(format(amount));
        label.setTooltip(new Tooltip(formatFull(amount)));
    }

    private static CompactUnit findUnit(double absAmount) {
        if (absAmount >= 1_000_000_000_000_000.0) {
            return new CompactUnit(1_000_000_000_000_000.0, "P");
        }
        if (absAmount >= 1_000_000_000_000.0) {
            return new CompactUnit(1_000_000_000_000.0, "T");
        }
        if (absAmount >= 1_000_000_000.0) {
            return new CompactUnit(1_000_000_000.0, "B");
        }
        if (absAmount >= 1_000_000.0) {
            return new CompactUnit(1_000_000.0, "M");
        }
        return new CompactUnit(1_000.0, "K");
    }

    private record CompactUnit(double divisor, String suffix) {}
}
