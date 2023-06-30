package com.anderhurtado.spigot.mobmoney.util;

import org.bukkit.Bukkit;

public class VersionManager {

    static {
        String nmsVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        NMS_VERSION = nmsVersion;
        VERSION = Integer.parseInt(nmsVersion.split("_")[1]);
    }
    public static final String NMS_VERSION;
    public static final int VERSION;

}
