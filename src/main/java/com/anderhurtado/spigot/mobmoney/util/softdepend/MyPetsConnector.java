package com.anderhurtado.spigot.mobmoney.util.softdepend;

import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class MyPetsConnector {

    public Player getPetOwner(LivingEntity entity) {
        if(isPet(entity)) return ((MyPetBukkitEntity)entity).getOwner().getPlayer();
        return null;
    }

    public boolean isPet(LivingEntity entity) {
        return entity instanceof MyPetBukkitEntity;
    }

}
