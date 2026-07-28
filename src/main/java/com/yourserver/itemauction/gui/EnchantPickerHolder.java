package com.yourserver.itemauction.gui;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

public class EnchantPickerHolder implements InventoryHolder {

    public static final int PAGE_SIZE = 45;
    public static final int PREV_SLOT = 45;
    public static final int NO_ENCHANT_SLOT = 48;
    public static final int BACK_SLOT = 49;
    public static final int NEXT_SLOT = 53;

    private Inventory inventory;
    private final Material material;
    private final int page;
    private final List<Enchantment> pageEnchants;
    private final boolean hasNextPage;

    public EnchantPickerHolder(Material material, int page, List<Enchantment> pageEnchants, boolean hasNextPage) {
        this.material = material;
        this.page = page;
        this.pageEnchants = pageEnchants;
        this.hasNextPage = hasNextPage;
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

    public int getPage() {
        return page;
    }

    public boolean hasNextPage() {
        return hasNextPage;
    }

    public Enchantment getEnchantAt(int slot) {
        if (slot < 0 || slot >= pageEnchants.size()) return null;
        return pageEnchants.get(slot);
    }
}
