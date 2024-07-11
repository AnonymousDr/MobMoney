package com.anderhurtado.spigot.mobmoney.util;

import com.anderhurtado.spigot.mobmoney.MobMoney;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.logging.Level;

public class ItemStackUtils {

    private static final Class<?> NMS_ITEMSTACK;
    private static final Method CONVERT_NMS_ITEMSTACK_TO_BUKKIT, CONVERT_BUKKIT_ITEMSTACK_TO_NMS;

    static {
        try{
            Class<?> craftItemStack;
            try {
                craftItemStack = Class.forName("org.bukkit.craftbukkit."+VersionManager.NMS_VERSION+".inventory.CraftItemStack");
            } catch (ClassNotFoundException CNFEx) {
                craftItemStack = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
            }
            Method convertor = null;
            for(Method m : craftItemStack.getMethods()) {
                if(m.getName().equals("asBukkitCopy")) {
                    if(m.getParameterCount() == 1) {
                        convertor = m;
                        break;
                    }
                }
            }
            if(convertor == null) throw new RuntimeException("This version is not compatible with MobMoney!");
            else {
                CONVERT_NMS_ITEMSTACK_TO_BUKKIT = convertor;
                NMS_ITEMSTACK = convertor.getParameters()[0].getType();
            }
            CONVERT_BUKKIT_ITEMSTACK_TO_NMS = craftItemStack.getMethod("asNMSCopy", ItemStack.class);
        } catch (Throwable T) {
            throw new RuntimeException(T);
        }
    }

    /**
     * Format:
     * [Amount] <Material[:data]> [NBT]
     * Amount is optional with a default value of 1 as long as NBT is not defined. Samples:
     * stone
     * 3 glass
     * wool:2
     * 1 DIAMOND_SWORD {Enchantments:[{id:unbreaking,lvl:1}]}
     * @param definiton ItemStack definition following format
     * @return ItemStack converted
     */
    public static ItemStack convert(String definiton) {
        String[] datas = definiton.split(" ", 3);
        int amount;
        String materialName;
        if(datas.length == 1) {
            amount = 1;
            materialName = datas[0].toUpperCase();
        } else {
            amount = Integer.parseInt(datas[0]);
            materialName = datas[1].toUpperCase();
        }
        if(materialName.startsWith("MINECRAFT:")) materialName = materialName.substring(10);
        ItemStack is;
        if(materialName.matches("^.+:\\d+$")) {
            int index = materialName.lastIndexOf(":");
            short data = Short.parseShort(materialName.substring(index+1));
            materialName = materialName.substring(0, index);
            is = new ItemStack(Material.getMaterial(materialName), amount, data);
        } else is = new ItemStack(Material.getMaterial(materialName), amount);
        if(datas.length == 3) {
            try {
                is = Bukkit.getUnsafe().modifyItemStack(is, datas[2]);
            } catch (Throwable T) {
                MobMoney.instance.getLogger().log(Level.CONFIG,"Invalid NBT tag in "+definiton);
                throw T;
            }
        }
        return is;
    }

    public static ItemStack[] convert(String... definitions) {
        ItemStack[] items = new ItemStack[definitions.length];
        for(int i=0; i<items.length; i++) items[i] = convert(definitions[i]);
        return items;
    }

    public static boolean isNMSItemStack(Object o) {
        return NMS_ITEMSTACK.isInstance(o);
    }

    public static ItemStack convertToBukkitItemStack(Object o) {
        try {
            return (ItemStack) CONVERT_NMS_ITEMSTACK_TO_BUKKIT.invoke(null, o);
        } catch (Exception Ex) {
            Ex.printStackTrace();
            return null;
        }
    }

    public static Object convertToNMSItemStack(ItemStack is) {
        try {
            return CONVERT_BUKKIT_ITEMSTACK_TO_NMS.invoke(null, is);
        } catch (Exception Ex) {
            Ex.printStackTrace();
            return null;
        }
    }

}
