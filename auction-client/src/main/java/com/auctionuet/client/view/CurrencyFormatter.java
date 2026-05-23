package com.auctionuet.client.view;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

final class CurrencyFormatter {
    private static final double COMPACT_THRESHOLD = 100_000_000_000.0;
    private static final DecimalFormat COMPACT_FORMAT =
            new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat FULL_FORMAT =
            new DecimalFormat("#,##0.##########", DecimalFormatSymbols.getInstance(Locale.US));

    private CurrencyFormatter() {}

    static String format(double amount) {
        if (!Double.isFinite(amount)) {
            return "-- VND";
        }

        double absAmount = Math.abs(amount);
        if (absAmount <= COMPACT_THRESHOLD) {
            return formatFull(amount);
        }
        CompactUnit unit = findUnit(absAmount);
        return COMPACT_FORMAT.format(amount / unit.divisor) + " " + unit.suffix + " VND";
    }

    static String formatFull(double amount) {
        if (!Double.isFinite(amount)) {
            return "-- VND";
        }
        return FULL_FORMAT.format(amount) + " VND";
    }

    static void setMoneyText(Label label, double amount) {
        if (label == null) {
            return;
        }
        label.setText(format(amount));
        installTooltip(label, formatFull(amount));
    }

    static void setMoneyText(Label label, String prefix, double amount) {
        setMoneyText(label, prefix, amount, "");
    }

    static void setMoneyText(Label label, String prefix, double amount, String suffix) {
        if (label == null) {
            return;
        }
        String safePrefix = prefix != null ? prefix : "";
        String safeSuffix = suffix != null ? suffix : "";
        label.setText(safePrefix + format(amount) + safeSuffix);
        installTooltip(label, safePrefix + formatFull(amount) + safeSuffix);
    }

    static void installTooltip(Label label, double amount) {
        installTooltip(label, formatFull(amount));
    }

    static void installTooltip(Label label, String text) {
        if (label == null) {
            return;
        }
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(420);
        label.setTooltip(tooltip);
    }

    private static CompactUnit findUnit(double absAmount) {
        if (absAmount >= 1_000_000_000_000_000.0) {
            return new CompactUnit(1_000_000_000_000_000.0, "triệu tỷ");
        }
        if (absAmount >= 1_000_000_000_000.0) {
            return new CompactUnit(1_000_000_000_000.0, "nghìn tỷ");
        }
        if (absAmount >= 1_000_000_000.0) {
            return new CompactUnit(1_000_000_000.0, "tỷ");
        }
        return new CompactUnit(1.0, "");
    }

    private record CompactUnit(double divisor, String suffix) {}
}
