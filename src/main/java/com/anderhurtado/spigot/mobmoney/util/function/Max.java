package com.anderhurtado.spigot.mobmoney.util.function;

import net.objecthunter.exp4j.function.Function;

public class Max extends Function {

    private static final Max INSTANCE = new Max();

    public static Max getInstance() {
        return INSTANCE;
    }

    private Max() {
        super("max", 2);
    }

    @Override
    public double apply(double... doubles) {
        return Math.max(doubles[0], doubles[1]);
    }
}
