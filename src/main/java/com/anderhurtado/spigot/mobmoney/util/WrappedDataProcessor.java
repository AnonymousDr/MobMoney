package com.anderhurtado.spigot.mobmoney.util;

import com.comphenix.protocol.wrappers.AbstractWrapper;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;

public class WrappedDataProcessor {

    public static int getIndex(AbstractWrapper aw) {
        if(aw instanceof WrappedWatchableObject) {
            return ((WrappedWatchableObject)aw).getIndex();
        } else if(aw instanceof com.comphenix.protocol.wrappers.WrappedDataValue) {
            return ((com.comphenix.protocol.wrappers.WrappedDataValue)aw).getIndex();
        } else try {
            return (int)aw.getClass().getMethod("getIndex").invoke(aw);
        } catch (Exception Ex) {
            throw new RuntimeException(Ex);
        }
    }

    public static Object getValue(AbstractWrapper aw) {
        if(aw instanceof WrappedWatchableObject) {
            return ((WrappedWatchableObject)aw).getValue();
        } else if(aw instanceof com.comphenix.protocol.wrappers.WrappedDataValue) {
            return ((com.comphenix.protocol.wrappers.WrappedDataValue)aw).getValue();
        } else try {
            return aw.getClass().getMethod("getValue").invoke(aw);
        } catch (Exception Ex) {
            throw new RuntimeException(Ex);
        }
    }

    public static void setValue(AbstractWrapper aw, Object value) {
        if(aw instanceof WrappedWatchableObject) {
            ((WrappedWatchableObject)aw).setValue(value, true);
        } else if(aw instanceof com.comphenix.protocol.wrappers.WrappedDataValue) {
            ((com.comphenix.protocol.wrappers.WrappedDataValue)aw).setValue(value);
        } else try {
            aw.getClass().getMethod("setValue", Object.class).invoke(aw, value);
        } catch (Exception Ex) {
            throw new RuntimeException(Ex);
        }
    }

}
