package com.anderhurtado.spigot.mobmoney.util.function;

import net.objecthunter.exp4j.function.Function;

public class Min extends Function {

    private static final Min INSTANCE = new Min();

    public static Min getInstance() {
        return INSTANCE;
    }

    private Min() {
        super("min", 2);
    }

    @Override
    public double apply(double... doubles) {
        return Math.min(doubles[0], doubles[1]);
    }
}
