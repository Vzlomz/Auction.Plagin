package com.yourserver.itemauction.listeners;

import com.yourserver.itemauction.ItemAuctionPlugin;
import com.yourserver.itemauction.Listing;
import com.yourserver.itemauction.ListingManager;
import com.yourserver.itemauction.gui.BrowseHolder;
import com.yourserver.itemauction.gui.CreateHolder;
import com.yourserver.itemauction.gui.GuiFactory;
import com.yourserver.itemauction.gui.MyListingsHolder;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GuiListener implements Listener {

    private final ItemAuctionPlugin plugin;
    private final ListingManager manager;
    private final Set<UUID> awaitingPriceInput = new HashSet<>();

    public GuiListener(ItemAuctionPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getListingManager();
    }

    // ================= КЛИКИ =================

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof BrowseHolder browseHolder) {
            event.setCancelled(true);
            handleBrowseClick(event, browseHolder);
        } else if (holder instanceof CreateHolder createHolder) {
            handleCreateClick(event, createHolder);
        } else if (holder instanceof MyListingsHolder myHolder) {
            event.setCancelled(true);
            handleMyListingsClick(event, myHolder);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof BrowseHolder || holder instanceof MyListingsHolder) {
            event.setCancelled(true);
            return;
        }
        if (holder instanceof CreateHolder) {
            // Разрешаем перетаскивание только в слот с продаваемым предметом
            for (int slot : event.getRawSlots()) {
                if (slot != CreateHolder.OFFER_SLOT) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    // ================= BROWSE (покупка) =================

    private void handleBrowseClick(InventoryClickEvent event, BrowseHolder holder) {
        int slot = event.getRawSlot();
        Player player = (Player) event.getWhoClicked();

        if (slot == BrowseHolder.PREV_SLOT) {
            if (holder.getPage() > 0) {
                openBrowse(player, holder.getPage() - 1);
            }
            return;
        }
        if (slot == BrowseHolder.NEXT_SLOT) {
            if (holder.hasNextPage()) {
                openBrowse(player, holder.getPage() + 1);
            }
            return;
        }

        Listing listing = holder.getListingAt(slot);
        if (listing == null) return;

        attemptPurchase(player, listing, holder.getPage());
    }

    private void attemptPurchase(Player buyer, Listing listing, int currentPage) {
        // Лот мог быть уже куплен/снят другим игроком - перепроверяем
        Listing fresh = manager.getListing(listing.getId());
        if (fresh == null) {
            buyer.sendMessage("§c[Аукцион] Этот лот уже недоступен.");
            openBrowse(buyer, currentPage);
            return;
        }

        if (fresh.getSellerUuid().equals(buyer.getUniqueId())) {
            buyer.sendMessage("§c[Аукцион] Нельзя купить собственный лот.");
            return;
        }

        ItemStack price = fresh.getPriceItem();
        if (!hasEnough(buyer, price)) {
            buyer.sendMessage("§c[Аукцион] У тебя недостаточно предметов для оплаты: §f" + GuiFactory.describeItem(price));
            return;
        }

        // Проверяем, что у покупателя есть место под покупаемый предмет (хотя бы частично - остаток уронится)
        removeItems(buyer, price);
        Map<Integer, ItemStack> leftover = buyer.getInventory().addItem(fresh.getOfferItem().clone());
        for (ItemStack left : leftover.values()) {
            buyer.getWorld().dropItemNaturally(buyer.getLocation(), left);
        }

        manager.payoutToSeller(fresh.getSellerUuid(), price);
        manager.removeListing(fresh.getId());

        buyer.sendMessage("§a[Аукцион] Покупка успешна: §f" + GuiFactory.describeItem(fresh.getOfferItem()));
        if (plugin.getConfig().getBoolean("sounds-enabled", true)) {
            buyer.playSound(buyer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        }

        openBrowse(buyer, currentPage);
    }

    private boolean hasEnough(Player player, ItemStack required) {
        int need = required.getAmount();
        int have = 0;
        for (ItemStack content : player.getInventory().getContents()) {
            if (content != null && content.isSimilar(required)) {
                have += content.getAmount();
                if (have >= need) return true;
            }
        }
        return have >= need;
    }

    private void removeItems(Player player, ItemStack required) {
        int remaining = required.getAmount();
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack content = contents[i];
            if (content != null && content.isSimilar(required)) {
                int take = Math.min(remaining, content.getAmount());
                content.setAmount(content.getAmount() - take);
                remaining -= take;
                if (content.getAmount() <= 0) {
                    player.getInventory().setItem(i, null);
                } else {
                    player.getInventory().setItem(i, content);
                }
            }
        }
    }

    // ================= CREATE (выставление лота) =================

    private void handleCreateClick(InventoryClickEvent event, CreateHolder holder) {
        int slot = event.getRawSlot();
        Player player = (Player) event.getWhoClicked();

        // Клик внутри инвентаря игрока (не GUI) - разрешаем свободно
        if (slot >= event.getInventory().getSize()) {
            return;
        }

        if (slot == CreateHolder.OFFER_SLOT) {
            return; // разрешаем класть/забирать предмет на продажу
        }

        event.setCancelled(true);

        if (slot == CreateHolder.PRICE_BUTTON_SLOT) {
            awaitingPriceInput.add(player.getUniqueId());
            player.sendMessage("§e[Аукцион] Напиши в чат предмет и количество, например: §fgold_ingot 3");
            player.sendMessage("§7(английское имя предмета; напиши §fcancel§7, чтобы отменить ввод)");
            return;
        }

        if (slot == CreateHolder.CONFIRM_SLOT) {
            ItemStack offer = event.getInventory().getItem(CreateHolder.OFFER_SLOT);
            ItemStack price = holder.getPendingPrice();

            if (offer == null || offer.getType().isAir()) {
                player.sendMessage("§c[Аукцион] Положи предмет, который хочешь продать.");
                return;
            }
            if (price == null) {
                player.sendMessage("§c[Аукцион] Сначала укажи цену (нажми на слот цены).");
                return;
            }

            int maxListings = plugin.getConfig().getInt("max-listings-per-player", 10);
            if (manager.countListingsOf(player.getUniqueId()) >= maxListings) {
                player.sendMessage("§c[Аукцион] У тебя уже максимум активных лотов (" + maxListings + "). Сними один через /ah my");
                return;
            }

            Listing listing = manager.createListing(player, offer, price);
            event.getInventory().setItem(CreateHolder.OFFER_SLOT, null);
            holder.setConfirmed(true);

            player.sendMessage("§a[Аукцион] Лот #" + listing.getId() + " выставлен: §f"
                    + GuiFactory.describeItem(offer) + " §7за§f " + GuiFactory.describeItem(price));
            player.closeInventory();
        } else if (slot == CreateHolder.CANCEL_SLOT) {
            holder.setCancelledManually(true);
            player.closeInventory();
        }
    }

    // ================= ВВОД ЦЕНЫ ЧЕРЕЗ ЧАТ =================

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!awaitingPriceInput.contains(player.getUniqueId())) return;

        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        Bukkit.getScheduler().runTask(plugin, () -> handlePriceChatInput(player, message));
    }

    private void handlePriceChatInput(Player player, String message) {
        if (!awaitingPriceInput.contains(player.getUniqueId())) return;

        if (message.equalsIgnoreCase("cancel")) {
            awaitingPriceInput.remove(player.getUniqueId());
            player.sendMessage("§7[Аукцион] Ввод цены отменён.");
            return;
        }

        String[] parts = message.trim().split("\\s+");
        if (parts.length != 2) {
            player.sendMessage("§c[Аукцион] Неверный формат. Пример: §fgold_ingot 3§c (напиши ещё раз или §fcancel§c)");
            return;
        }

        Material material = Material.matchMaterial(parts[0]);
        if (material == null || material.isAir() || !material.isItem()) {
            player.sendMessage("§c[Аукцион] Не найден предмет \"" + parts[0] + "\". Используй английское имя (например gold_ingot). Попробуй ещё раз или §fcancel");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§c[Аукцион] Количество должно быть числом. Попробуй ещё раз или §fcancel");
            return;
        }

        if (amount < 1 || amount > 6400) {
            player.sendMessage("§c[Аукцион] Количество должно быть от 1 до 6400. Попробуй ещё раз или §fcancel");
            return;
        }

        awaitingPriceInput.remove(player.getUniqueId());

        InventoryHolder currentHolder = player.getOpenInventory().getTopInventory().getHolder();
        if (!(currentHolder instanceof CreateHolder createHolder)) {
            player.sendMessage("§c[Аукцион] Окно создания лота уже закрыто, открой /ah create заново.");
            return;
        }

        ItemStack price = new ItemStack(material, amount);
        createHolder.setPendingPrice(price);
        GuiFactory.renderPriceButton(player.getOpenInventory().getTopInventory(), price);

        player.sendMessage("§a[Аукцион] Цена установлена: §f" + GuiFactory.describeItem(price));
    }

    // ================= MY LISTINGS (снятие лота) =================

    private void handleMyListingsClick(InventoryClickEvent event, MyListingsHolder holder) {
        int slot = event.getRawSlot();
        Player player = (Player) event.getWhoClicked();

        Listing listing = holder.getListingAt(slot);
        if (listing == null) return;

        if (!listing.getSellerUuid().equals(player.getUniqueId()) && !player.hasPermission("itemauction.admin")) {
            player.sendMessage("§c[Аукцион] Это не твой лот.");
            return;
        }

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(listing.getOfferItem().clone());
        for (ItemStack left : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), left);
        }
        manager.removeListing(listing.getId());
        player.sendMessage("§a[Аукцион] Лот #" + listing.getId() + " снят, предмет возвращён.");

        openMyListings(player);
    }

    // ================= CLOSE (возврат предметов при отмене создания) =================

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof CreateHolder createHolder)) return;
        if (createHolder.isConfirmed()) return; // уже обработано

        HumanEntity human = event.getPlayer();
        if (!(human instanceof Player player)) return;

        // Возвращаем предмет из слота продажи, если лот не был подтверждён
        returnItemIfPresent(player, event.getInventory(), CreateHolder.OFFER_SLOT);
        awaitingPriceInput.remove(player.getUniqueId());
    }

    private void returnItemIfPresent(Player player, Inventory inv, int slot) {
        ItemStack item = inv.getItem(slot);
        if (item != null && !item.getType().isAir()) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            for (ItemStack left : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), left);
            }
            inv.setItem(slot, null);
        }
    }

    // ================= Вспомогательные методы открытия окон =================

    public void openBrowse(Player player, int page) {
        String title = plugin.getConfig().getString("gui-titles.browse", "Аукцион");
        List<Listing> all = new ArrayList<>(manager.getAllListings());
        Inventory inv = GuiFactory.buildBrowseInventory(title, page, all);
        player.openInventory(inv);
    }

    public void openCreate(Player player) {
        String title = plugin.getConfig().getString("gui-titles.create", "Выставить лот");
        Inventory inv = GuiFactory.buildCreateInventory(title);
        player.openInventory(inv);
    }

    public void openMyListings(Player player) {
        String title = plugin.getConfig().getString("gui-titles.my-listings", "Мои лоты");
        Inventory inv = GuiFactory.buildMyListingsInventory(title, manager.getListingsOf(player.getUniqueId()));
        player.openInventory(inv);
    }
}
