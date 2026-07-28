package com.yourserver.itemauction.gui;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class EnchantLevelHolder implements InventoryHolder {

    public static final int MINUS_1 = 11;
    public static final int DISPLAY_SLOT = 13;
    public static final int PLUS_1 = 15;
    public static final int BACK_SLOT = 20;
    public static final int CONFIRM_SLOT = 24;

    private Inventory inventory;
    private final Material material;
    private final Enchantment enchantment;
    private int level;

    public EnchantLevelHolder(Material material, Enchantment enchantment, int level) {
        this.material = material;
        this.enchantment = enchantment;
        this.level = level;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Material getMaterial() {
        return material;
    }

    public Enchantment getEnchantment() {
        return enchantment;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        int min = Math.max(1, enchantment.getStartLevel());
        int max = Math.max(min, enchantment.getMaxLevel());
        this.level = Math.max(min, Math.min(max, level));
    }
}
