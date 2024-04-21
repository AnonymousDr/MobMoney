package com.anderhurtado.spigot.mobmoney.objets;

import com.anderhurtado.spigot.mobmoney.MobMoney;
import com.anderhurtado.spigot.mobmoney.util.Cleanable;
import com.anderhurtado.spigot.mobmoney.util.CleanableArrayList;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.UUID;

public class DailyLimit implements Serializable {

    private static final File FILE = new File(MobMoney.cplugin, "dailylimit.dat");

    private static DailyLimit instance;
    public static DailyLimit getInstance() {
        if(instance == null ) {
            if(FILE.exists()) {
                try(ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(FILE.toPath()))) {
                    instance = (DailyLimit) ois.readObject();
                    instance.validate();
                } catch (InvalidClassException ICEx) {
                    instance = new DailyLimit();
                } catch (Exception Ex) {
                    Ex.printStackTrace();
                    instance = new DailyLimit();
                }
            } else instance = new DailyLimit();
        }
        return instance;
    }

    private final HashMap<UUID, CleanableArrayList<Kill>> users = new HashMap<>();


    public double addCount(UUID user, double value) {
        CleanableArrayList<Kill> list;
        if(users.containsKey(user)) {
            list = users.get(user);
            synchronized (list) {
                list.doCleaning();
            }
        } else users.put(user, list = new CleanableArrayList<>());

        synchronized (list) {
            list.add(new Kill(value));
        }
        return sum(list);
    }

    public double getCount(UUID user) {
        CleanableArrayList<Kill> list = users.get(user);
        if(list == null) return 0;
        synchronized (list) {
            list.doCleaning();
            if(list.isEmpty()) {
                users.remove(user);
                return 0;
            }
        }
        return sum(list);
    }

    private double sum(CleanableArrayList<Kill> list) {
        double sum = 0;
        synchronized (list) {
            for(Kill kill : list) sum += kill.value;
        }
        return sum;
    }

    public void validate() {
        users.values().forEach(CleanableArrayList::doCleaning);
        users.entrySet().removeIf(entry->{
            synchronized (entry.getValue()) {
                return entry.getValue().isEmpty();
            }
        });
    }

    public void save() {
        validate();
        try(ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(FILE.toPath()))) {
            oos.writeObject(this);
        } catch (Exception Ex) {
            Ex.printStackTrace();
        }
    }

}

class Kill implements Cleanable, Serializable {

    private static final int DAY_IN_MILIS_LENGTH = 24 * 3600 * 1000;

    private final long timestamp = System.currentTimeMillis();
    final double value;

    Kill(double value) {
        this.value = value;
    }

    @Override
    public boolean isCleanable() {
        long time = System.currentTimeMillis();
        return (timestamp + DAY_IN_MILIS_LENGTH) < time || timestamp > time;

    }

}
