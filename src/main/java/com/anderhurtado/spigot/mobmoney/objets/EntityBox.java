package com.anderhurtado.spigot.mobmoney.objets;

import com.anderhurtado.spigot.mobmoney.util.VersionManager;
import org.bukkit.entity.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public abstract class EntityBox {

    private static Conversor conversor;

    static {
        try {
            Class.forName("org.bukkit.util.BoundingBox");
            conversor = FromBukkitBoundingBox::new;
        } catch (Throwable ignored) {
            conversor = LegacyBoundingBox::new;
        }
    }

    public static EntityBox getFromEntity(Entity e) {
        return conversor.getEntityBox(e);
    }

    public abstract EntityBox grow(double x, double y, double z);

    public abstract EntityBox grow(EntityBox other);

    public abstract boolean contains(EntityBox other);

}
interface Conversor {
    EntityBox getEntityBox(Entity e);
}
class FromBukkitBoundingBox extends EntityBox {

    private org.bukkit.util.BoundingBox box;

    public FromBukkitBoundingBox(Entity e) {
        box = e.getBoundingBox();
    }

    @Override
    public EntityBox grow(double x, double y, double z) {
        box = box.expand(x, y, z);
        return this;
    }

    @Override
    public EntityBox grow(EntityBox other) {
        if(other instanceof FromBukkitBoundingBox) {
            box = box.union(((FromBukkitBoundingBox)other).box);
            return this;
        } else throw new IllegalArgumentException("EntityBox have to be created in the same way!");
    }

    @Override
    public boolean contains(EntityBox other) {
        if(other instanceof FromBukkitBoundingBox) {
            return box.contains(((FromBukkitBoundingBox)other).box);
        } else throw new IllegalArgumentException("EntityBox have to be created in the same way!");
    }
}

class LegacyBoundingBox extends EntityBox {

    static Method ENTITY_GET_HANDLE, GET_BOUNDINGBOX;
    final static Field[] FIELDS = new Field[6];

    static {
        try {
            String nmsVersion = VersionManager.NMS_VERSION;
            Class<?> craftEntity = Class.forName("org.bukkit.craftbukkit."+nmsVersion+".entity.CraftEntity");
            ENTITY_GET_HANDLE = craftEntity.getMethod("getHandle");
            Class<?> entity = Class.forName("net.minecraft.server."+nmsVersion+".Entity");
            GET_BOUNDINGBOX = entity.getMethod("getBoundingBox");
            Class<?> legacyBoundingBox = Class.forName("net.minecraft.server."+nmsVersion+".AxisAlignedBB");
            FIELDS[0] = legacyBoundingBox.getField("a");
            FIELDS[1] = legacyBoundingBox.getField("b");
            FIELDS[2] = legacyBoundingBox.getField("c");
            FIELDS[3] = legacyBoundingBox.getField("d");
            FIELDS[4] = legacyBoundingBox.getField("e");
            FIELDS[5] = legacyBoundingBox.getField("f");
        } catch (Throwable T) {
            throw new RuntimeException(T);
        }
    }

    final double[] data = new double[FIELDS.length];

    LegacyBoundingBox(Entity e) {
        try {
            Object boundingBox = GET_BOUNDINGBOX.invoke(ENTITY_GET_HANDLE.invoke(e));
            for(int i=0; i<FIELDS.length; i++) data[i] = FIELDS[i].getDouble(boundingBox);
        } catch (Throwable T) {
            throw new RuntimeException(T);
        }
    }

    public LegacyBoundingBox(double var1, double var3, double var5, double var7, double var9, double var11) {
        data[0] = Math.min(var1, var7);
        data[1] = Math.min(var3, var9);
        data[2] = Math.min(var5, var11);
        data[3] = Math.max(var1, var7);
        data[4] = Math.max(var3, var9);
        data[5] = Math.max(var5, var11);
    }

    public LegacyBoundingBox grow(double var1, double var3, double var5) {
        double newMinX = this.data[0] - var1;
        double newMinY = this.data[1] - var3;
        double newMinZ = this.data[2] - var5;
        double newMaxX = this.data[3] + var1;
        double newMaxY = this.data[4] + var3;
        double newMaxZ = this.data[5] + var5;
        return new LegacyBoundingBox(newMinX, newMinY, newMinZ, newMaxX, newMaxY, newMaxZ);
    }

    @Override
    public LegacyBoundingBox grow(EntityBox entityBox) {
        if(entityBox instanceof LegacyBoundingBox) {
            LegacyBoundingBox var1 = (LegacyBoundingBox)entityBox;
            double var2 = Math.min(this.data[0], var1.data[0]);
            double var4 = Math.min(this.data[1], var1.data[1]);
            double var6 = Math.min(this.data[2], var1.data[2]);
            double var8 = Math.max(this.data[3], var1.data[3]);
            double var10 = Math.max(this.data[4], var1.data[4]);
            double var12 = Math.max(this.data[5], var1.data[5]);
            return new LegacyBoundingBox(var2, var4, var6, var8, var10, var12);
        } else throw new IllegalArgumentException("EntityBox have to be created in the same way!");
    }

    @Override
    public boolean contains(EntityBox entityBox) {
        if(entityBox instanceof LegacyBoundingBox) {
            LegacyBoundingBox var1 = (LegacyBoundingBox)entityBox;
            if (!(var1.data[3] <= this.data[0]) && !(var1.data[0] >= this.data[3])) {
                if (!(var1.data[4] <= this.data[1]) && !(var1.data[1] >= this.data[4])) {
                    return !(var1.data[5] <= this.data[2]) && !(var1.data[2] >= this.data[5]);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
         else throw new IllegalArgumentException("EntityBox have to be created in the same way!");
    }

}