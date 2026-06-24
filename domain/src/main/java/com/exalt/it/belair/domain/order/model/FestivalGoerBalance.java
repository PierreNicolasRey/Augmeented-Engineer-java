package com.exalt.it.belair.domain.order.model;

import com.exalt.it.belair.domain.order.exceptions.InsufficientTokensException;

public class FestivalGoerBalance {
    private int totalDrinkTokens;
    private int totalSnackTokens;
    private int reservedDrinkTokens = 0;
    private int reservedSnackTokens = 0;

    public FestivalGoerBalance(int drinkTokens, int snackTokens) {
        this.totalDrinkTokens = drinkTokens;
        this.totalSnackTokens = snackTokens;
    }

    public int getDrinkTokens() {
        return totalDrinkTokens;
    }

    public int getSnackTokens() {
        return totalSnackTokens;
    }

    public int getReservedDrinkTokens() {
        return reservedDrinkTokens;
    }

    public int getAvailableDrinkTokens() {
        return totalDrinkTokens - reservedDrinkTokens;
    }

    public int getReservedSnackTokens() {
        return reservedSnackTokens;
    }

    public int getAvailableSnackTokens() {
        return totalSnackTokens - reservedSnackTokens;
    }

    public void reserveDrinkTokens(int amount) {
        if (amount > getAvailableDrinkTokens()) {
            throw new InsufficientTokensException("Insufficient drink tokens");
        }
        reservedDrinkTokens += amount;
    }

    public void reserveSnackTokens(int amount) {
        if (amount > getAvailableSnackTokens()) {
            throw new InsufficientTokensException("Insufficient snack tokens");
        }
        reservedSnackTokens += amount;
    }
}
