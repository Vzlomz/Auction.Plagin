package com.yourserver.itemauction.commands;

import com.yourserver.itemauction.ItemAuctionPlugin;
import com.yourserver.itemauction.Listing;
import com.yourserver.itemauction.ListingManager;
import com.yourserver.itemauction.listeners.GuiListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class AhCommand implements CommandExecutor {

    private final ItemAuctionPlugin plugin;
    private final GuiListener guiListener;
    private final ListingManager manager;

    public AhCommand(ItemAuctionPlugin plugin, GuiListener guiListener) {
        this.plugin = plugin;
        this.guiListener = guiListener;
        this.manager = plugin.getListingManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Эта команда только для игроков.");
            return true;
        }

        if (args.length == 0) {
            guiListener.openBrowse(player, 0);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> guiListener.openCreate(player);
            case "my" -> guiListener.openMyListings(player);
            case "mail" -> handleMail(player);
            case "cancel" -> handleCancel(player, args);
            case "help" -> sendHelp(player);
            default -> {
                player.sendMessage("§c[Аукцион] Неизвестная команда.");
                sendHelp(player);
            }
        }
        return true;
    }

    private void handleMail(Player player) {
        List<ItemStack> items = manager.takeMailbox(player.getUniqueId());
        if (items.isEmpty()) {
            player.sendMessage("§7[Аукцион] У тебя нет оплат для получения.");
            return;
        }
        for (ItemStack item : items) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            for (ItemStack left : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), left);
            }
        }
        player.sendMessage("§a[Аукцион] Получено предметов: " + items.size());
    }

    private void handleCancel(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c[Аукцион] Используй: /ah cancel <id>  (либо открой /ah my)");
            return;
        }
        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§c[Аукцион] ID должен быть числом.");
            return;
        }
        Listing listing = manager.getListing(id);
        if (listing == null) {
            player.sendMessage("§c[Аукцион] Лот #" + id + " не найден.");
            return;
        }
        if (!listing.getSellerUuid().equals(player.getUniqueId()) && !player.hasPermission("itemauction.admin")) {
            player.sendMessage("§c[Аукцион] Это не твой лот.");
            return;
        }
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(listing.getOfferItem().clone());
        for (ItemStack left : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), left);
        }
        manager.removeListing(id);
        player.sendMessage("§a[Аукцион] Лот #" + id + " снят, предмет возвращён.");
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6§l===== Аукцион (предмет за предмет) =====");
        player.sendMessage("");
        player.sendMessage("§e▶ Как купить вещь:");
        player.sendMessage("§7 1. Напиши §f/ah §7- откроется витрина всех лотов");
        player.sendMessage("§7 2. Наведи курсор на предмет - в подсказке видно продавца и цену");
        player.sendMessage("§7 3. Кликни по предмету - если у тебя хватает нужных вещей в инвентаре,");
        player.sendMessage("§7    покупка пройдёт автоматически, и предмет окажется у тебя");
        player.sendMessage("§7 4. Стрелки внизу окна - переключение страниц");
        player.sendMessage("");
        player.sendMessage("§e▶ Как продать (выставить) вещь:");
        player.sendMessage("§7 1. Напиши §f/ah create");
        player.sendMessage("§7 2. Положи в левый слот предмет, который хочешь продать");
        player.sendMessage("§7 3. Нажми на слот §f\"Указать цену\"§7 (справа)");
        player.sendMessage("§7 4. Напиши в чат название предмета и количество, например: §fgold_ingot 3");
        player.sendMessage("§7    (этот предмет у тебя быть не обязан - это просто условие для покупателя)");
        player.sendMessage("§7 5. Нажми на зелёное стекло §f\"Подтвердить\"§7 - лот опубликован");
        player.sendMessage("§7 Красное стекло §f\"Отменить\"§7 в любой момент вернёт твой предмет обратно");
        player.sendMessage("");
        player.sendMessage("§e▶ Управление своими лотами:");
        player.sendMessage("§7 §f/ah my §7- посмотреть свои лоты, клик по предмету снимает его с продажи");
        player.sendMessage("§7 §f/ah cancel <id> §7- снять лот по номеру, не открывая окно");
        player.sendMessage("§7 §f/ah mail §7- забрать оплату, если лот купили, пока ты был оффлайн");
        player.sendMessage("");
        player.sendMessage("§6§l========================================");
    }
}
