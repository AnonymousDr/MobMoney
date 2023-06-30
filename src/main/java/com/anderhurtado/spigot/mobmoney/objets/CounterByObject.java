package com.anderhurtado.spigot.mobmoney.objets;

import java.util.HashMap;

public class CounterByObject {

    HashMap<Object, Counter> counters = new HashMap<>();

    public double getStatus(Object o) {
        if(counters.containsKey(o)) return counters.get(o).getStatus();
        else return 0;
    }

    public double getStatus(Object o, double sum) {
        if(counters.containsKey(o)) return counters.get(o).getStatus(sum);
        else {
            Counter counter = new Counter();
            counters.put(o, counter);
            return counter.getStatus(sum);
        }
    }

}
