package com.yourserver.itemauction;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Хранит состояние процесса выставления лота для одного игрока,
 * пока он переключается между окнами (создание -> каталог -> количество -> создание).
 */
public class CreateSession {

    private ItemStack offerItem;      // предмет на продажу (физически изъят у игрока)
    private Material priceMaterial;   // выбранный предмет-цена
    private int priceAmount = 1;      // количество цены
    private int catalogPage = 0;      // страница каталога, чтобы вернуться туда же

    // Флаг: true, когда мы сами программно переключаем игроку окно -
    // нужен, чтобы InventoryCloseEvent не воспринял это как "закрыл и отменил"
    private boolean switchingScreens = false;

    public ItemStack getOfferItem() {
        return offerItem;
    }

    public void setOfferItem(ItemStack offerItem) {
        this.offerItem = offerItem;
    }

    public Material getPriceMaterial() {
        return priceMaterial;
    }

    public void setPriceMaterial(Material priceMaterial) {
        this.priceMaterial = priceMaterial;
    }

    public int getPriceAmount() {
        return priceAmount;
    }

    public void setPriceAmount(int priceAmount) {
        this.priceAmount = priceAmount;
    }

    public int getCatalogPage() {
        return catalogPage;
    }

    public void setCatalogPage(int catalogPage) {
        this.catalogPage = catalogPage;
    }

    public boolean isSwitchingScreens() {
        return switchingScreens;
    }

    public void setSwitchingScreens(boolean switchingScreens) {
        this.switchingScreens = switchingScreens;
    }

    public boolean hasPrice() {
        return priceMaterial != null;
    }

    public ItemStack buildPriceItem() {
        return priceMaterial == null ? null : new ItemStack(priceMaterial, priceAmount);
    }
}
