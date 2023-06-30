package com.anderhurtado.spigot.mobmoney.objets;

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
        player.playSound(player.getLocation(), sound, volume, pitch);
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
