package com.yourserver.itemauction;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class ListingManager {

    private final ItemAuctionPlugin plugin;
    private final File listingsFile;
    private final File mailboxFile;

    private final Map<Integer, Listing> listings = new LinkedHashMap<>();
    private final Map<UUID, List<ItemStack>> mailbox = new HashMap<>();
    private int nextId = 1;

    public ListingManager(ItemAuctionPlugin plugin) {
        this.plugin = plugin;
        this.listingsFile = new File(plugin.getDataFolder(), "listings.yml");
        this.mailboxFile = new File(plugin.getDataFolder(), "mailbox.yml");
        load();
    }

    // ---------- Загрузка / сохранение ----------

    public void load() {
        listings.clear();
        mailbox.clear();

        if (listingsFile.exists()) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(listingsFile);
            nextId = cfg.getInt("next-id", 1);
            if (cfg.isConfigurationSection("listings")) {
                for (String key : cfg.getConfigurationSection("listings").getKeys(false)) {
                    try {
                        String path = "listings." + key;
                        int id = Integer.parseInt(key);
                        UUID sellerUuid = UUID.fromString(cfg.getString(path + ".seller-uuid"));
                        String sellerName = cfg.getString(path + ".seller-name", "Unknown");
                        ItemStack offer = cfg.getItemStack(path + ".offer-item");
                        ItemStack price = cfg.getItemStack(path + ".price-item");
                        long created = cfg.getLong(path + ".created", System.currentTimeMillis());
                        if (offer != null && price != null) {
                            listings.put(id, new Listing(id, sellerUuid, sellerName, offer, price, created));
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Не удалось загрузить лот " + key, e);
                    }
                }
            }
        }

        if (mailboxFile.exists()) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(mailboxFile);
            if (cfg.isConfigurationSection("mailbox")) {
                for (String key : cfg.getConfigurationSection("mailbox").getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        List<?> raw = cfg.getList("mailbox." + key + ".items");
                        List<ItemStack> items = new ArrayList<>();
                        if (raw != null) {
                            for (Object o : raw) {
                                if (o instanceof ItemStack) items.add((ItemStack) o);
                            }
                        }
                        mailbox.put(uuid, items);
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Не удалось загрузить почту " + key, e);
                    }
                }
            }
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("next-id", nextId);
        for (Listing l : listings.values()) {
            String path = "listings." + l.getId();
            cfg.set(path + ".seller-uuid", l.getSellerUuid().toString());
            cfg.set(path + ".seller-name", l.getSellerName());
            cfg.set(path + ".offer-item", l.getOfferItem());
            cfg.set(path + ".price-item", l.getPriceItem());
            cfg.set(path + ".created", l.getCreatedAt());
        }
        try {
            cfg.save(listingsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить listings.yml", e);
        }

        YamlConfiguration mcfg = new YamlConfiguration();
        for (Map.Entry<UUID, List<ItemStack>> entry : mailbox.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            mcfg.set("mailbox." + entry.getKey() + ".items", entry.getValue());
        }
        try {
            mcfg.save(mailboxFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить mailbox.yml", e);
        }
    }

    // ---------- Работа с лотами ----------

    public Collection<Listing> getAllListings() {
        return listings.values();
    }

    public List<Listing> getListingsOf(UUID uuid) {
        List<Listing> result = new ArrayList<>();
        for (Listing l : listings.values()) {
            if (l.getSellerUuid().equals(uuid)) result.add(l);
        }
        return result;
    }

    public int countListingsOf(UUID uuid) {
        return getListingsOf(uuid).size();
    }

    public Listing getListing(int id) {
        return listings.get(id);
    }

    /**
     * Создаёт новый лот. Предметы уже должны быть изъяты у игрока до вызова.
     */
    public Listing createListing(Player seller, ItemStack offer, ItemStack price) {
        int id = nextId++;
        Listing listing = new Listing(id, seller.getUniqueId(), seller.getName(), offer.clone(), price.clone(), System.currentTimeMillis());
        listings.put(id, listing);
        save();
        return listing;
    }

    /**
     * Удаляет лот (например, при отмене или после покупки).
     */
    public void removeListing(int id) {
        listings.remove(id);
        save();
    }

    // ---------- Почта (для оффлайн игроков) ----------

    public void addToMailbox(UUID uuid, ItemStack item) {
        mailbox.computeIfAbsent(uuid, k -> new ArrayList<>()).add(item.clone());
        save();
    }

    public List<ItemStack> takeMailbox(UUID uuid) {
        List<ItemStack> items = mailbox.remove(uuid);
        save();
        return items == null ? Collections.emptyList() : items;
    }

    public int countMailbox(UUID uuid) {
        List<ItemStack> items = mailbox.get(uuid);
        return items == null ? 0 : items.size();
    }

    /**
     * Отдаёт предмет продавцу: если онлайн - в инвентарь (или на пол при переполнении), если оффлайн - в почту.
     */
    public void payoutToSeller(UUID sellerUuid, ItemStack paymentItem) {
        Player online = Bukkit.getPlayer(sellerUuid);
        if (online != null && online.isOnline()) {
            Map<Integer, ItemStack> leftover = online.getInventory().addItem(paymentItem.clone());
            for (ItemStack left : leftover.values()) {
                online.getWorld().dropItemNaturally(online.getLocation(), left);
            }
            online.sendMessage("§a[Аукцион] §fТвой лот куплен, оплата зачислена в инвентарь.");
        } else {
            addToMailbox(sellerUuid, paymentItem);
        }
    }
}
