package com.yourserver.itemauction.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class CreateHolder implements InventoryHolder {

    public static final int OFFER_SLOT = 11;        // сюда игрок кладёт то, что продаёт (физически)
    public static final int PRICE_BUTTON_SLOT = 15; // кнопка "указать цену" (открывает каталог предметов)
    public static final int CONFIRM_SLOT = 22;      // подтвердить
    public static final int CANCEL_SLOT = 31;       // отменить

    private Inventory inventory;

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
