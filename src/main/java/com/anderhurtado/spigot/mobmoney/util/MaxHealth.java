package com.anderhurtado.spigot.mobmoney.util;

import org.bukkit.entity.LivingEntity;

public class MaxHealth {
    private static final Conversor conversor;

    static {
        Conversor convert;
        try {
            Class.forName("org.bukkit.attribute.Attribute");
            convert = e->e.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        } catch (Throwable T) {
            //noinspection deprecation
            convert = LivingEntity::getMaxHealth;
        }
        conversor = convert;
    }

    public static double getMaxHealth(LivingEntity livingEntity) {
        return conversor.getMaxHealth(livingEntity);
    }

}
interface Conversor {
    double getMaxHealth(LivingEntity entity);
}