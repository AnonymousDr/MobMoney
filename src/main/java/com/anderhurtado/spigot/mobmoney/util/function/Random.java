package com.anderhurtado.spigot.mobmoney.util.function;

import net.objecthunter.exp4j.function.Function;

public class Random extends Function {

    private static final Random INSTANCE = new Random();

    public Random() {
        super("random");
    }

    public static Random getInstance() {
        return INSTANCE;
    }

    @Override
    public double apply(double... doubles) {
        double random = Math.random();
        switch (doubles.length) {
            case 0:
                return random;
            case 1:
                return random * doubles[0];
            default:
                return Math.min(doubles[0], doubles[1]) + (random * Math.max(doubles[0], doubles[1]));
        }
    }
}
