package com.anderhurtado.spigot.mobmoney.util.softdepend;

import com.anderhurtado.spigot.mobmoney.objets.User;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PlaceholderConnector extends PlaceholderExpansion {
    @Override
    public @org.jetbrains.annotations.NotNull String getIdentifier() {
        return "MobMoney";
    }

    @Override
    public @org.jetbrains.annotations.NotNull String getAuthor() {
        return "Anonymous_Dr";
    }

    @Override
    public @org.jetbrains.annotations.NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if(player == null) return null;
        if(params.equalsIgnoreCase("messages_enabled")) {
            User user = User.getUser(player.getUniqueId());
            boolean status = user.getReceiveOnDeath();
            if(!player.isOnline()) user.disconnect();
            return String.valueOf(status);
        }
        return null;
    }
}
