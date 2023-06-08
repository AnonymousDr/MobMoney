package com.anderhurtado.spigot.mobmoney.util.function;

import net.objecthunter.exp4j.function.Function;

public class Decode extends Function {

    private static final Decode INSTANCE = new Decode();

    public static Decode getInstance() {
        return INSTANCE;
    }

    private Decode() {
        super("decode");
    }

    @Override
    public double apply(double... doubles) throws ArrayIndexOutOfBoundsException {
        double objective = doubles[0];
        for(int i=1; i<doubles.length; i+=2) {
            if(doubles[i] == objective) return doubles[i];
        }
        return doubles[doubles.length-1];
    }
}
