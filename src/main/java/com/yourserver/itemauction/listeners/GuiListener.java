package com.yourserver.itemauction.listeners;

import com.yourserver.itemauction.CreateSession;
import com.yourserver.itemauction.ItemAuctionPlugin;
import com.yourserver.itemauction.Listing;
import com.yourserver.itemauction.ListingManager;
import com.yourserver.itemauction.MaterialCatalog;
import com.yourserver.itemauction.gui.AmountHolder;
import com.yourserver.itemauction.gui.BrowseHolder;
import com.yourserver.itemauction.gui.CreateHolder;
import com.yourserver.itemauction.gui.GuiFactory;
import com.yourserver.itemauction.gui.MaterialPickerHolder;
import com.yourserver.itemauction.gui.MyListingsHolder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GuiListener implements Listener {

    private final ItemAuctionPlugin plugin;
    private final ListingManager manager;
    private final Map<UUID, CreateSession> sessions = new HashMap<>();

    public GuiListener(ItemAuctionPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getListingManager();
    }

    private CreateSession sessionOf(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), k -> new CreateSession());
    }

    // ================= КЛИКИ =================

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        Player player = (Player) event.getWhoClicked();

        if (holder instanceof BrowseHolder browseHolder) {
            event.setCancelled(true);
            handleBrowseClick(event, browseHolder, player);
        } else if (holder instanceof CreateHolder) {
            handleCreateClick(event, player);
        } else if (holder instanceof MaterialPickerHolder pickerHolder) {
            event.setCancelled(true);
            handlePickerClick(event, pickerHolder, player);
        } else if (holder instanceof AmountHolder amountHolder) {
            event.setCancelled(true);
            handleAmountClick(event, amountHolder, player);
        } else if (holder instanceof MyListingsHolder myHolder) {
            event.setCancelled(true);
            handleMyListingsClick(event, myHolder, player);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof BrowseHolder || holder instanceof MyListingsHolder
                || holder instanceof MaterialPickerHolder || holder instanceof AmountHolder) {
            event.setCancelled(true);
            return;
        }
        if (holder instanceof CreateHolder) {
            for (int slot : event.getRawSlots()) {
                if (slot != CreateHolder.OFFER_SLOT) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    // ================= BROWSE (покупка) =================

    private void handleBrowseClick(InventoryClickEvent event, BrowseHolder holder, Player player) {
        int slot = event.getRawSlot();

        if (slot == BrowseHolder.PREV_SLOT) {
            if (holder.getPage() > 0) openBrowse(player, holder.getPage() - 1);
            return;
        }
        if (slot == BrowseHolder.NEXT_SLOT) {
            if (holder.hasNextPage()) openBrowse(player, holder.getPage() + 1);
            return;
        }

        Listing listing = holder.getListingAt(slot);
        if (listing == null) return;

        attemptPurchase(player, listing, holder.getPage());
    }

    private void attemptPurchase(Player buyer, Listing listing, int currentPage) {
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
                player.getInventory().setItem(i, content.getAmount() <= 0 ? null : content);
            }
        }
    }

    // ================= CREATE (выставление лота) =================

    private void handleCreateClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();

        if (slot >= event.getInventory().getSize()) return; // клики по своему инвентарю - свободно
        if (slot == CreateHolder.OFFER_SLOT) return; // разрешаем класть/забирать предмет

        event.setCancelled(true);
        CreateSession session = sessionOf(player);

        if (slot == CreateHolder.PRICE_BUTTON_SLOT) {
            session.setOfferItem(event.getInventory().getItem(CreateHolder.OFFER_SLOT));
            event.getInventory().setItem(CreateHolder.OFFER_SLOT, null);
            session.setSwitchingScreens(true);
            openMaterialPicker(player, 0);
            return;
        }

        if (slot == CreateHolder.CONFIRM_SLOT) {
            ItemStack offer = event.getInventory().getItem(CreateHolder.OFFER_SLOT);
            if (offer == null || offer.getType().isAir()) {
                player.sendMessage("§c[Аукцион] Положи предмет, который хочешь продать.");
                return;
            }
            if (!session.hasPrice()) {
                player.sendMessage("§c[Аукцион] Сначала укажи цену (нажми на слот цены).");
                return;
            }

            int maxListings = plugin.getConfig().getInt("max-listings-per-player", 10);
            if (manager.countListingsOf(player.getUniqueId()) >= maxListings) {
                player.sendMessage("§c[Аукцион] У тебя уже максимум активных лотов (" + maxListings + "). Сними один через /ah my");
                return;
            }

            ItemStack price = session.buildPriceItem();
            Listing listing = manager.createListing(player, offer, price);
            event.getInventory().setItem(CreateHolder.OFFER_SLOT, null);
            sessions.remove(player.getUniqueId());

            player.sendMessage("§a[Аукцион] Лот #" + listing.getId() + " выставлен: §f"
                    + GuiFactory.describeItem(offer) + " §7за§f " + GuiFactory.describeItem(price));
            player.closeInventory();
        } else if (slot == CreateHolder.CANCEL_SLOT) {
            returnItemIfPresent(player, event.getInventory(), CreateHolder.OFFER_SLOT);
            sessions.remove(player.getUniqueId());
            player.closeInventory();
        }
    }

    // ================= КАТАЛОГ ПРЕДМЕТОВ (выбор цены) =================

    private void handlePickerClick(InventoryClickEvent event, MaterialPickerHolder holder, Player player) {
        int slot = event.getRawSlot();
        CreateSession session = sessionOf(player);

        if (slot == MaterialPickerHolder.PREV_SLOT) {
            if (holder.getPage() > 0) {
                session.setSwitchingScreens(true);
                openMaterialPicker(player, holder.getPage() - 1);
            }
            return;
        }
        if (slot == MaterialPickerHolder.NEXT_SLOT) {
            if (holder.hasNextPage()) {
                session.setSwitchingScreens(true);
                openMaterialPicker(player, holder.getPage() + 1);
            }
            return;
        }
        if (slot == MaterialPickerHolder.BACK_SLOT) {
            session.setSwitchingScreens(true);
            openCreate(player);
            return;
        }

        Material material = holder.getMaterialAt(slot);
        if (material == null) return;

        session.setPriceMaterial(material);
        session.setPriceAmount(1);
        session.setCatalogPage(holder.getPage());
        session.setSwitchingScreens(true);
        openAmount(player, material, 1);
    }

    // ================= ВЫБОР КОЛИЧЕСТВА =================

    private void handleAmountClick(InventoryClickEvent event, AmountHolder holder, Player player) {
        int slot = event.getRawSlot();
        CreateSession session = sessionOf(player);

        int delta = switch (slot) {
            case AmountHolder.MINUS_64 -> -64;
            case AmountHolder.MINUS_10 -> -10;
            case AmountHolder.MINUS_1 -> -1;
            case AmountHolder.PLUS_1 -> 1;
            case AmountHolder.PLUS_10 -> 10;
            case AmountHolder.PLUS_64 -> 64;
            default -> 0;
        };

        if (delta != 0) {
            holder.setAmount(holder.getAmount() + delta);
            GuiFactory.renderAmountDisplay(event.getInventory(), holder.getMaterial(), holder.getAmount());
            return;
        }

        if (slot == AmountHolder.BACK_SLOT) {
            session.setSwitchingScreens(true);
            openMaterialPicker(player, session.getCatalogPage());
            return;
        }

        if (slot == AmountHolder.CONFIRM_SLOT) {
            session.setPriceMaterial(holder.getMaterial());
            session.setPriceAmount(holder.getAmount());
            session.setSwitchingScreens(true);
            openCreate(player);
        }
    }

    // ================= MY LISTINGS (снятие лота) =================

    private void handleMyListingsClick(InventoryClickEvent event, MyListingsHolder holder, Player player) {
        int slot = event.getRawSlot();
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

    // ================= CLOSE =================

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        boolean isCreationScreen = holder instanceof CreateHolder
                || holder instanceof MaterialPickerHolder
                || holder instanceof AmountHolder;
        if (!isCreationScreen) return;

        HumanEntity human = event.getPlayer();
        if (!(human instanceof Player player)) return;

        CreateSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        if (session.isSwitchingScreens()) {
            session.setSwitchingScreens(false);
            return;
        }

        // Настоящее закрытие (не переключение между окнами процесса) - отменяем и возвращаем предмет
        if (holder instanceof CreateHolder) {
            returnItemIfPresent(player, event.getInventory(), CreateHolder.OFFER_SLOT);
        }
        ItemStack heldOffer = session.getOfferItem();
        if (heldOffer != null && !heldOffer.getType().isAir()) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(heldOffer);
            for (ItemStack left : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), left);
            }
        }
        sessions.remove(player.getUniqueId());
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
        player.openInventory(GuiFactory.buildBrowseInventory(title, page, all));
    }

    public void openCreate(Player player) {
        CreateSession session = sessionOf(player);
        String title = plugin.getConfig().getString("gui-titles.create", "Выставить лот");
        Inventory inv = GuiFactory.buildCreateInventory(title, session.getOfferItem(), session.buildPriceItem());
        session.setOfferItem(null); // предмет теперь снова физически лежит в открытом окне
        player.openInventory(inv);
    }

    private void openMaterialPicker(Player player, int page) {
        String title = "Выбери предмет-цену";
        player.openInventory(GuiFactory.buildMaterialPickerInventory(title, page, MaterialCatalog.all()));
    }

    private void openAmount(Player player, Material material, int amount) {
        String title = "Количество: " + GuiFactory.prettyName(material);
        player.openInventory(GuiFactory.buildAmountInventory(title, material, amount));
    }

    public void openMyListings(Player player) {
        String title = plugin.getConfig().getString("gui-titles.my-listings", "Мои лоты");
        player.openInventory(GuiFactory.buildMyListingsInventory(title, manager.getListingsOf(player.getUniqueId())));
    }
}
