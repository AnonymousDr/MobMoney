package com.anderhurtado.spigot.mobmoney.util.function;

import net.objecthunter.exp4j.function.Function;

public class Random extends Function {

    private static final Random INSTANCE = new Random();

    public Random() {
        super("random", 2);
    }

    public static Random getInstance() {
        return INSTANCE;
    }

    @Override
    public double apply(double... doubles) {
        double random = Math.random();
        double min = Math.min(doubles[0], doubles[1]);
        return min + (random * (Math.max(doubles[0], doubles[1]) - min));
    }
}
