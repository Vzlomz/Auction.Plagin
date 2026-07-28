package com.yourserver.itemauction.gui;

import com.yourserver.itemauction.Listing;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

public class MyListingsHolder implements InventoryHolder {

    private Inventory inventory;
    private final List<Listing> myListings; // слот i -> myListings.get(i)

    public MyListingsHolder(List<Listing> myListings) {
        this.myListings = myListings;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Listing getListingAt(int slot) {
        if (slot < 0 || slot >= myListings.size()) return null;
        return myListings.get(slot);
    }
}
