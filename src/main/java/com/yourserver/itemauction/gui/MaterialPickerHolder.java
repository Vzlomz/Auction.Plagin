package com.yourserver.itemauction.gui;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

public class MaterialPickerHolder implements InventoryHolder {

    public static final int PAGE_SIZE = 45; // слоты 0-44
    public static final int PREV_SLOT = 45;
    public static final int BACK_SLOT = 49;
    public static final int NEXT_SLOT = 53;

    private Inventory inventory;
    private final int page;
    private final List<Material> pageMaterials;
    private final boolean hasNextPage;

    public MaterialPickerHolder(int page, List<Material> pageMaterials, boolean hasNextPage) {
        this.page = page;
        this.pageMaterials = pageMaterials;
        this.hasNextPage = hasNextPage;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public int getPage() {
        return page;
    }

    public boolean hasNextPage() {
        return hasNextPage;
    }

    public Material getMaterialAt(int slot) {
        if (slot < 0 || slot >= pageMaterials.size()) return null;
        return pageMaterials.get(slot);
    }
}
