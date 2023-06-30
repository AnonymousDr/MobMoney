package com.anderhurtado.spigot.mobmoney.objets;

public class Counter {
    private double status = 0;

    public double getStatus() {
        return status;
    }

    public double getStatus(double sum) {
        status += sum;
        return getStatus();
    }
}
