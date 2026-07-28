package com.yourserver.itemauction;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Один лот на аукционе: игрок отдаёт offerItem, взамен просит priceItem (с указанным количеством).
 */
public class Listing {

    private final int id;
    private final UUID sellerUuid;
    private final String sellerName;
    private final ItemStack offerItem; // то, что продаётся
    private final ItemStack priceItem; // то, что нужно отдать взамен (material+amount+meta)
    private final long createdAt;

    public Listing(int id, UUID sellerUuid, String sellerName, ItemStack offerItem, ItemStack priceItem, long createdAt) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.offerItem = offerItem;
        this.priceItem = priceItem;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public UUID getSellerUuid() {
        return sellerUuid;
    }

    public String getSellerName() {
        return sellerName;
    }

    public ItemStack getOfferItem() {
        return offerItem;
    }

    public ItemStack getPriceItem() {
        return priceItem;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
