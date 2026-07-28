package com.yourserver.itemauction;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Хранит состояние процесса выставления лота для одного игрока,
 * пока он переключается между окнами (создание -> категория -> каталог -> количество -> создание).
 */
public class CreateSession {

    private ItemStack offerItem;      // предмет на продажу (физически изъят у игрока)
    private Material priceMaterial;   // выбранный предмет-цена
    private int priceAmount = 1;      // количество цены
    private int catalogPage = 0;      // страница каталога, чтобы вернуться туда же
    private com.yourserver.itemauction.MaterialCatalog.Category currentCategory = com.yourserver.itemauction.MaterialCatalog.Category.ALL;
    private org.bukkit.enchantments.Enchantment priceEnchant; // выбранное зачарование для предмета-цены (может быть null)
    private int priceEnchantLevel = 1;
    private int enchantPage = 0; // страница списка чар, чтобы вернуться туда же

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

    public com.yourserver.itemauction.MaterialCatalog.Category getCurrentCategory() {
        return currentCategory;
    }

    public void setCurrentCategory(com.yourserver.itemauction.MaterialCatalog.Category currentCategory) {
        this.currentCategory = currentCategory;
    }

    public org.bukkit.enchantments.Enchantment getPriceEnchant() {
        return priceEnchant;
    }

    public void setPriceEnchant(org.bukkit.enchantments.Enchantment priceEnchant) {
        this.priceEnchant = priceEnchant;
    }

    public int getPriceEnchantLevel() {
        return priceEnchantLevel;
    }

    public void setPriceEnchantLevel(int priceEnchantLevel) {
        this.priceEnchantLevel = priceEnchantLevel;
    }

    public int getEnchantPage() {
        return enchantPage;
    }

    public void setEnchantPage(int enchantPage) {
        this.enchantPage = enchantPage;
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
        if (priceMaterial == null) return null;
        ItemStack item = new ItemStack(priceMaterial, priceAmount);
        if (priceEnchant != null) {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addEnchant(priceEnchant, priceEnchantLevel, true);
                item.setItemMeta(meta);
            }
        }
        return item;
    }
}
