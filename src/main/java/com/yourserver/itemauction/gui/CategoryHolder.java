package com.yourserver.itemauction.gui;

import com.yourserver.itemauction.MaterialCatalog;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class CategoryHolder implements InventoryHolder {

    // Раскладка вкладок по слотам 3-го ряда (индексы 0-8 центральной строки, слоты 20-26 и т.д.)
    public static final int[] CATEGORY_SLOTS = {19, 20, 21, 22, 23, 24, 25};
    public static final int BACK_SLOT = 31;

    private Inventory inventory;
    private final MaterialCatalog.Category[] slotCategories = new MaterialCatalog.Category[54];

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void assign(int slot, MaterialCatalog.Category category) {
        slotCategories[slot] = category;
    }

    public MaterialCatalog.Category getCategoryAt(int slot) {
        if (slot < 0 || slot >= slotCategories.length) return null;
        return slotCategories[slot];
    }
}
