package com.yourserver.itemauction.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class CreateHolder implements InventoryHolder {

    public static final int OFFER_SLOT = 11;        // сюда игрок кладёт то, что продаёт (физически)
    public static final int PRICE_BUTTON_SLOT = 15; // кнопка "указать цену" (открывает ввод в чат)
    public static final int CONFIRM_SLOT = 22;      // подтвердить
    public static final int CANCEL_SLOT = 31;       // отменить

    private Inventory inventory;
    private boolean confirmed = false;
    private boolean cancelledManually = false;
    // Цена задаётся через чат и НЕ списывается с игрока при создании лота -
    // это просто "виртуальное" описание того, что должен принести покупатель.
    private ItemStack pendingPrice;

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public boolean isCancelledManually() {
        return cancelledManually;
    }

    public void setCancelledManually(boolean cancelledManually) {
        this.cancelledManually = cancelledManually;
    }

    public ItemStack getPendingPrice() {
        return pendingPrice;
    }

    public void setPendingPrice(ItemStack pendingPrice) {
        this.pendingPrice = pendingPrice;
    }
}
