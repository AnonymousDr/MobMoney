package com.anderhurtado.spigot.mobmoney.util;

import java.util.ArrayList;

public class CleanableArrayList<E extends Cleanable> extends ArrayList<E> {

    public void doCleaning() {
        removeIf(E::isCleanable);
    }

}
