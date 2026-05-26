package com.auctionuet.protocol.dto.response.admin;

import com.auctionuet.protocol.dto.ValidatableDTO;

public class FinancialSummaryDTO implements ValidatableDTO {
    private final double availableBalanceTotal;
    private final double frozenBalanceTotal;
    private final double penaltyRevenueTotal;

    public FinancialSummaryDTO(
            double availableBalanceTotal,
            double frozenBalanceTotal,
            double penaltyRevenueTotal) {
        this.availableBalanceTotal = availableBalanceTotal;
        this.frozenBalanceTotal = frozenBalanceTotal;
        this.penaltyRevenueTotal = penaltyRevenueTotal;
        validate();
    }

    @Override
    public void validate() {
        if (availableBalanceTotal < 0) {
            throw new IllegalArgumentException("availableBalanceTotal must be greater than or equal to 0");
        }
        if (frozenBalanceTotal < 0) {
            throw new IllegalArgumentException("frozenBalanceTotal must be greater than or equal to 0");
        }
        if (penaltyRevenueTotal < 0) {
            throw new IllegalArgumentException("penaltyRevenueTotal must be greater than or equal to 0");
        }
    }

    public double getAvailableBalanceTotal() {
        return availableBalanceTotal;
    }

    public double getFrozenBalanceTotal() {
        return frozenBalanceTotal;
    }

    public double getPenaltyRevenueTotal() {
        return penaltyRevenueTotal;
    }
}
