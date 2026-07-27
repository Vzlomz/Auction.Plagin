package com.yourserver.itemauction.gui;

import com.yourserver.itemauction.Listing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GuiFactory {

    private static ItemStack namedItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
            if (lore != null && !lore.isEmpty()) {
                List<Component> loreComponents = new ArrayList<>();
                for (String line : lore) {
                    loreComponents.add(Component.text(line).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                }
                meta.lore(loreComponents);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack filler() {
        return namedItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
    }

    // ---------- Окно просмотра лотов ----------

    public static Inventory buildBrowseInventory(String title, int page, List<Listing> allListings) {
        int from = page * BrowseHolder.PAGE_SIZE;
        int to = Math.min(from + BrowseHolder.PAGE_SIZE, allListings.size());
        List<Listing> pageListings = from < allListings.size() ? allListings.subList(from, to) : List.of();
        boolean hasNext = to < allListings.size();

        BrowseHolder holder = new BrowseHolder(page, pageListings, hasNext);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(title + "  (стр. " + (page + 1) + ")"));
        holder.setInventory(inv);

        for (int i = BrowseHolder.PAGE_SIZE; i < 54; i++) {
            inv.setItem(i, filler());
        }

        for (int i = 0; i < pageListings.size(); i++) {
            Listing l = pageListings.get(i);
            ItemStack display = l.getOfferItem().clone();
            ItemMeta meta = display.getItemMeta();
            List<String> lore = new ArrayList<>();
            lore.add("Продавец: " + l.getSellerName());
            lore.add("Цена: " + describeItem(l.getPriceItem()));
            lore.add("");
            lore.add("Нажми, чтобы купить");
            if (meta != null) {
                List<Component> loreComponents = new ArrayList<>();
                for (String line : lore) {
                    loreComponents.add(Component.text(line).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                }
                meta.lore(loreComponents);
                display.setItemMeta(meta);
            }
            inv.setItem(i, display);
        }

        if (page > 0) {
            inv.setItem(BrowseHolder.PREV_SLOT, namedItem(Material.ARROW, "« Предыдущая страница", null));
        }
        inv.setItem(BrowseHolder.INFO_SLOT, namedItem(Material.BOOK, "/ah create - выставить лот", List.of("/ah my - мои лоты", "/ah mail - забрать оплату")));
        if (hasNext) {
            inv.setItem(BrowseHolder.NEXT_SLOT, namedItem(Material.ARROW, "Следующая страница »", null));
        }

        return inv;
    }

    // ---------- Окно создания лота ----------

    public static Inventory buildCreateInventory(String title) {
        CreateHolder holder = new CreateHolder();
        Inventory inv = Bukkit.createInventory(holder, 36, Component.text(title));
        holder.setInventory(inv);

        for (int i = 0; i < 36; i++) {
            inv.setItem(i, filler());
        }
        inv.setItem(CreateHolder.OFFER_SLOT, null);

        inv.setItem(2, namedItem(Material.PAPER, "Сюда положи предмет, который ПРОДАЁШЬ", List.of("слот слева (11)")));
        inv.setItem(6, namedItem(Material.PAPER, "Цена задаётся через чат", List.of("нажми на слот справа (15)", "не обязательно иметь этот предмет у себя")));

        renderPriceButton(inv, null);

        inv.setItem(CreateHolder.CONFIRM_SLOT, namedItem(Material.LIME_STAINED_GLASS_PANE, "✔ Подтвердить и выставить лот", null));
        inv.setItem(CreateHolder.CANCEL_SLOT, namedItem(Material.RED_STAINED_GLASS_PANE, "✘ Отменить (вернуть предметы)", null));

        return inv;
    }

    /**
     * Обновляет визуальный слот цены в уже открытом окне создания лота.
     * pendingPrice может отсутствовать физически у игрока - это просто описание условия.
     */
    public static void renderPriceButton(Inventory inv, ItemStack pendingPrice) {
        if (pendingPrice == null) {
            inv.setItem(CreateHolder.PRICE_BUTTON_SLOT,
                    namedItem(Material.NAME_TAG, "Нажми, чтобы указать цену", List.of("Например: gold_ingot 3")));
            return;
        }
        ItemStack display = pendingPrice.clone();
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            List<Component> loreComponents = new ArrayList<>();
            loreComponents.add(Component.text("Цена лота (виртуально)").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            loreComponents.add(Component.text("Нажми, чтобы изменить").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(loreComponents);
            display.setItemMeta(meta);
        }
        inv.setItem(CreateHolder.PRICE_BUTTON_SLOT, display);
    }

    // ---------- Окно "мои лоты" ----------

    public static Inventory buildMyListingsInventory(String title, List<Listing> myListings) {
        MyListingsHolder holder = new MyListingsHolder(myListings);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(title));
        holder.setInventory(inv);

        for (int i = 0; i < myListings.size() && i < 54; i++) {
            Listing l = myListings.get(i);
            ItemStack display = l.getOfferItem().clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<Component> loreComponents = new ArrayList<>();
                loreComponents.add(Component.text("Цена: " + describeItem(l.getPriceItem())).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                loreComponents.add(Component.text("Нажми, чтобы снять с продажи").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
                meta.lore(loreComponents);
                display.setItemMeta(meta);
            }
            inv.setItem(i, display);
        }

        return inv;
    }

    public static String describeItem(ItemStack item) {
        String name = item.getType().name().toLowerCase().replace('_', ' ');
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            name = item.getItemMeta().getDisplayName();
        }
        return item.getAmount() + "x " + name;
    }
}
