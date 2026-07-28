package com.yourserver.itemauction.gui;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class AmountHolder implements InventoryHolder {

    public static final int MINUS_64 = 10;
    public static final int MINUS_10 = 11;
    public static final int MINUS_1 = 12;
    public static final int DISPLAY_SLOT = 13;
    public static final int PLUS_1 = 14;
    public static final int PLUS_10 = 15;
    public static final int PLUS_64 = 16;
    public static final int BACK_SLOT = 18;
    public static final int CONFIRM_SLOT = 22;

    private Inventory inventory;
    private final Material material;
    private int amount;

    public AmountHolder(Material material, int amount) {
        this.material = material;
        this.amount = amount;
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

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = Math.max(1, Math.min(6400, amount));
    }
}
