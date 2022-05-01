package com.anderhurtado.spigot.mobmoney.util;

import com.anderhurtado.spigot.mobmoney.MobMoney;
import com.anderhurtado.spigot.mobmoney.objetos.User;

import java.io.*;

import java.util.HashMap;
import java.util.UUID;

public class UserCache implements Serializable {

    private static UserCache USER_CACHE;

    private static final File file = new File(MobMoney.cplugin, "usercache.dat");

    public static UserCache getInstance() {
        if(USER_CACHE != null) return USER_CACHE;
        if(file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                return USER_CACHE = (UserCache) ois.readObject();
            } catch (Exception Ex) {
                file.delete();
            }
        }
        return USER_CACHE = new UserCache();
    }

    private final HashMap<UUID, Long> notReceivingMessagesOnKill;

    private UserCache() {
        notReceivingMessagesOnKill = new HashMap<>();
    }

    public boolean receivesMessagesOnKill(User user) {
        if(notReceivingMessagesOnKill.containsKey(user.uuid)) {
            notReceivingMessagesOnKill.replace(user.uuid, System.currentTimeMillis());
            return false;
        } else return true;
    }

    public void setReceivingMessagesOnKill(User user, boolean value) {
        if(value) notReceivingMessagesOnKill.remove(user.uuid);
        else notReceivingMessagesOnKill.put(user.uuid, System.currentTimeMillis());
    }

    public void save() {
        flush();
        synchronized (file) {
            try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                oos.writeObject(this);
            } catch (Exception Ex) {
                Ex.printStackTrace();
            }
        }
    }

    private void flush() {
        long limit = System.currentTimeMillis() - 604800000; // 1 week
        notReceivingMessagesOnKill.entrySet().removeIf(entry -> entry.getValue() < limit);
    }

}
