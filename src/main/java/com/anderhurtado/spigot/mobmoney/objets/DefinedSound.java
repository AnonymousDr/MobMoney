package com.anderhurtado.spigot.mobmoney.objets;

import com.anderhurtado.spigot.mobmoney.MobMoney;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class DefinedSound {

    private final Sound sound;
    private final float volume;
    private final float pitch;

    public DefinedSound(Sound sound) {
        this(sound, 1, 1);
    }

    public DefinedSound(Sound sound, float volume, float pitch) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    public void play(Player player) {
        if(Bukkit.isPrimaryThread()) player.playSound(player.getLocation(), sound, volume, pitch);
        else Bukkit.getScheduler().runTask(MobMoney.instance, ()->play(player));
    }

    public Sound getSound() {
        return sound;
    }

    public float getVolume() {
        return volume;
    }

    public float getPitch() {
        return pitch;
    }

}
