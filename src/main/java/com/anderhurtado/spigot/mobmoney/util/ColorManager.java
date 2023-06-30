package com.anderhurtado.spigot.mobmoney.util;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorManager {

    private static final Pattern pattern = Pattern.compile("#([0-9A-Fa-f]){6}");
    public static String translateColorCodes(String txt) {
        if(VersionManager.VERSION >= 16) {
            try {
                String match;
                Matcher matcher = pattern.matcher(txt);
                while(matcher.find()) {
                    match = txt.substring(matcher.start(), matcher.end());
                    txt = txt.replace(match, net.md_5.bungee.api.ChatColor.of(match).toString());
                    matcher = pattern.matcher(txt);
                }
            } catch (Throwable ignored) {}
        }
        return ChatColor.translateAlternateColorCodes('&', txt);
    }

}
