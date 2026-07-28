package com.yourserver.itemauction.gui;

import com.yourserver.itemauction.Listing;
import com.yourserver.itemauction.MaterialCatalog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            if (meta != null) {
                List<Component> loreComponents = new ArrayList<>();
                loreComponents.add(Component.text("Продавец: " + l.getSellerName()).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                loreComponents.add(Component.text("Цена: " + describeItem(l.getPriceItem())).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                loreComponents.add(Component.text("").decoration(TextDecoration.ITALIC, false));
                loreComponents.add(Component.text("Нажми, чтобы купить").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
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

    public static Inventory buildCreateInventory(String title, ItemStack currentOffer, ItemStack currentPrice) {
        CreateHolder holder = new CreateHolder();
        Inventory inv = Bukkit.createInventory(holder, 36, Component.text(title));
        holder.setInventory(inv);

        for (int i = 0; i < 36; i++) {
            inv.setItem(i, filler());
        }
        inv.setItem(CreateHolder.OFFER_SLOT, currentOffer);

        inv.setItem(2, namedItem(Material.PAPER, "Сюда положи предмет, который ПРОДАЁШЬ", List.of("слот слева (11)")));
        inv.setItem(6, namedItem(Material.PAPER, "Цена выбирается из каталога", List.of("нажми на слот справа (15)", "не обязательно иметь этот предмет у себя")));

        renderPriceButton(inv, currentPrice);

        inv.setItem(CreateHolder.CONFIRM_SLOT, namedItem(Material.LIME_STAINED_GLASS_PANE, "✔ Подтвердить и выставить лот", null));
        inv.setItem(CreateHolder.CANCEL_SLOT, namedItem(Material.RED_STAINED_GLASS_PANE, "✘ Отменить (вернуть предметы)", null));

        return inv;
    }

    public static void renderPriceButton(Inventory inv, ItemStack pendingPrice) {
        if (pendingPrice == null) {
            inv.setItem(CreateHolder.PRICE_BUTTON_SLOT,
                    namedItem(Material.NAME_TAG, "Нажми, чтобы указать цену", List.of("Откроется каталог предметов")));
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

    // ---------- Окно выбора категории ----------

    public static Inventory buildCategoryInventory(String title) {
        CategoryHolder holder = new CategoryHolder();
        Inventory inv = Bukkit.createInventory(holder, 45, Component.text(title));
        holder.setInventory(inv);

        for (int i = 0; i < 45; i++) {
            inv.setItem(i, filler());
        }

        MaterialCatalog.Category[] categories = MaterialCatalog.Category.values();
        int[] slots = CategoryHolder.CATEGORY_SLOTS;
        for (int i = 0; i < categories.length && i < slots.length; i++) {
            MaterialCatalog.Category c = categories[i];
            inv.setItem(slots[i], namedItem(c.getIcon(), c.getDisplayName(), List.of("Нажми, чтобы открыть раздел")));
            holder.assign(slots[i], c);
        }

        inv.setItem(CategoryHolder.BACK_SLOT, namedItem(Material.BARRIER, "« Назад (без выбора)", null));

        return inv;
    }

    // ---------- Окно каталога предметов (выбор цены) ----------

    public static Inventory buildMaterialPickerInventory(String title, MaterialCatalog.Category category, int page, List<Material> allMaterials) {
        int from = page * MaterialPickerHolder.PAGE_SIZE;
        int to = Math.min(from + MaterialPickerHolder.PAGE_SIZE, allMaterials.size());
        List<Material> pageMaterials = from < allMaterials.size() ? allMaterials.subList(from, to) : List.of();
        boolean hasNext = to < allMaterials.size();

        MaterialPickerHolder holder = new MaterialPickerHolder(category, page, pageMaterials, hasNext);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(title + "  (стр. " + (page + 1) + ")"));
        holder.setInventory(inv);

        for (int i = MaterialPickerHolder.PAGE_SIZE; i < 54; i++) {
            inv.setItem(i, filler());
        }

        for (int i = 0; i < pageMaterials.size(); i++) {
            Material m = pageMaterials.get(i);
            inv.setItem(i, namedItem(m, prettyName(m), List.of("Нажми, чтобы выбрать")));
        }

        if (page > 0) {
            inv.setItem(MaterialPickerHolder.PREV_SLOT, namedItem(Material.ARROW, "« Предыдущая страница", null));
        }
        inv.setItem(MaterialPickerHolder.BACK_SLOT, namedItem(Material.BARRIER, "« К разделам", null));
        if (hasNext) {
            inv.setItem(MaterialPickerHolder.NEXT_SLOT, namedItem(Material.ARROW, "Следующая страница »", null));
        }

        return inv;
    }

    // ---------- Окно выбора зачарования ----------

    public static Inventory buildEnchantPickerInventory(String title, Material material, int page, List<Enchantment> allEnchants) {
        int from = page * EnchantPickerHolder.PAGE_SIZE;
        int to = Math.min(from + EnchantPickerHolder.PAGE_SIZE, allEnchants.size());
        List<Enchantment> pageEnchants = from < allEnchants.size() ? allEnchants.subList(from, to) : List.of();
        boolean hasNext = to < allEnchants.size();

        EnchantPickerHolder holder = new EnchantPickerHolder(material, page, pageEnchants, hasNext);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(title + "  (стр. " + (page + 1) + ")"));
        holder.setInventory(inv);

        for (int i = EnchantPickerHolder.PAGE_SIZE; i < 54; i++) {
            inv.setItem(i, filler());
        }

        for (int i = 0; i < pageEnchants.size(); i++) {
            Enchantment e = pageEnchants.get(i);
            inv.setItem(i, namedItem(Material.ENCHANTED_BOOK, enchantName(e), List.of("Нажми, чтобы выбрать уровень")));
        }

        if (page > 0) {
            inv.setItem(EnchantPickerHolder.PREV_SLOT, namedItem(Material.ARROW, "« Предыдущая страница", null));
        }
        inv.setItem(EnchantPickerHolder.NO_ENCHANT_SLOT, namedItem(Material.BOOK, "Без чар (обычный предмет)", null));
        inv.setItem(EnchantPickerHolder.BACK_SLOT, namedItem(Material.BARRIER, "« К выбору предмета", null));
        if (hasNext) {
            inv.setItem(EnchantPickerHolder.NEXT_SLOT, namedItem(Material.ARROW, "Следующая страница »", null));
        }

        return inv;
    }

    // ---------- Окно выбора уровня зачарования ----------

    public static Inventory buildEnchantLevelInventory(String title, Material material, Enchantment enchantment, int level) {
        EnchantLevelHolder holder = new EnchantLevelHolder(material, enchantment, level);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text(title));
        holder.setInventory(inv);

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler());
        }

        inv.setItem(EnchantLevelHolder.MINUS_1, namedItem(Material.RED_CONCRETE, "-1 уровень", null));
        inv.setItem(EnchantLevelHolder.PLUS_1, namedItem(Material.GREEN_CONCRETE, "+1 уровень", null));
        renderEnchantLevelDisplay(inv, material, enchantment, level);

        inv.setItem(EnchantLevelHolder.BACK_SLOT, namedItem(Material.ARROW, "« Назад к чарам", null));
        inv.setItem(EnchantLevelHolder.CONFIRM_SLOT, namedItem(Material.LIME_STAINED_GLASS_PANE, "✔ Готово", null));

        return inv;
    }

    public static void renderEnchantLevelDisplay(Inventory inv, Material material, Enchantment enchantment, int level) {
        ItemStack display = new ItemStack(material);
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            meta.addEnchant(enchantment, level, true);
            List<Component> loreComponents = new ArrayList<>();
            loreComponents.add(Component.text(enchantName(enchantment) + " " + level).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            loreComponents.add(Component.text("(мин " + Math.max(1, enchantment.getStartLevel()) + ", макс " + enchantment.getMaxLevel() + ")").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(loreComponents);
            display.setItemMeta(meta);
        }
        inv.setItem(EnchantLevelHolder.DISPLAY_SLOT, display);
    }

    public static String enchantName(Enchantment enchantment) {
        return prettyName(enchantment.getKey().getKey());
    }

    private static String prettyName(String rawName) {
        String[] parts = rawName.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    // ---------- Окно выбора количества ----------

    public static Inventory buildAmountInventory(String title, Material material, int amount) {
        AmountHolder holder = new AmountHolder(material, amount);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text(title));
        holder.setInventory(inv);

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler());
        }

        inv.setItem(AmountHolder.MINUS_64, namedItem(Material.RED_CONCRETE, "-64", null));
        inv.setItem(AmountHolder.MINUS_10, namedItem(Material.ORANGE_CONCRETE, "-10", null));
        inv.setItem(AmountHolder.MINUS_1, namedItem(Material.YELLOW_CONCRETE, "-1", null));
        inv.setItem(AmountHolder.PLUS_1, namedItem(Material.LIME_CONCRETE, "+1", null));
        inv.setItem(AmountHolder.PLUS_10, namedItem(Material.GREEN_CONCRETE, "+10", null));
        inv.setItem(AmountHolder.PLUS_64, namedItem(Material.EMERALD_BLOCK, "+64", null));

        renderAmountDisplay(inv, material, amount);

        inv.setItem(AmountHolder.BACK_SLOT, namedItem(Material.ARROW, "« Назад к каталогу", null));
        inv.setItem(AmountHolder.CONFIRM_SLOT, namedItem(Material.LIME_STAINED_GLASS_PANE, "✔ Готово", null));

        return inv;
    }

    public static void renderAmountDisplay(Inventory inv, Material material, int amount) {
        ItemStack display = new ItemStack(material, Math.max(1, Math.min(amount, material.getMaxStackSize() > 0 ? 64 : 1)));
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(prettyName(material)).decoration(TextDecoration.ITALIC, false));
            List<Component> loreComponents = new ArrayList<>();
            loreComponents.add(Component.text("Количество: " + amount).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            meta.lore(loreComponents);
            display.setItemMeta(meta);
        }
        inv.setItem(AmountHolder.DISPLAY_SLOT, display);
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
        String name = prettyName(item.getType());
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            name = item.getItemMeta().getDisplayName();
        }
        String result = item.getAmount() + "x " + name;
        if (item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Enchantment, Integer> entry : item.getItemMeta().getEnchants().entrySet()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(enchantName(entry.getKey())).append(' ').append(entry.getValue());
            }
            result += " (" + sb + ")";
        }
        return result;
    }

    public static String prettyName(Material material) {
        return material.name().toLowerCase().replace('_', ' ');
    }
}
