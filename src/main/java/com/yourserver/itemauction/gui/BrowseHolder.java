package com.yourserver.itemauction.gui;

import com.yourserver.itemauction.Listing;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

/**
 * Holder для окна просмотра лотов. slotToListingId связывает слот инвентаря с ID лота на текущей странице.
 */
public class BrowseHolder implements InventoryHolder {

    public static final int PAGE_SIZE = 45; // слоты 0-44, 45-53 - навигация/служебное
    public static final int PREV_SLOT = 45;
    public static final int INFO_SLOT = 49;
    public static final int NEXT_SLOT = 53;

    private Inventory inventory;
    private final int page;
    private final List<Listing> pageListings; // соответствуют слотам 0..pageListings.size()-1
    private final boolean hasNextPage;

    public BrowseHolder(int page, List<Listing> pageListings, boolean hasNextPage) {
        this.page = page;
        this.pageListings = pageListings;
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

    public Listing getListingAt(int slot) {
        if (slot < 0 || slot >= pageListings.size()) return null;
        return pageListings.get(slot);
    }
}
