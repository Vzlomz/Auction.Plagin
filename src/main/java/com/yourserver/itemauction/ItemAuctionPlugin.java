package com.yourserver.itemauction;

import com.yourserver.itemauction.commands.AhCommand;
import com.yourserver.itemauction.listeners.GuiListener;
import org.bukkit.plugin.java.JavaPlugin;

public class ItemAuctionPlugin extends JavaPlugin {

    private ListingManager listingManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getDataFolder().mkdirs();

        this.listingManager = new ListingManager(this);

        GuiListener guiListener = new GuiListener(this);
        getServer().getPluginManager().registerEvents(guiListener, this);

        AhCommand ahCommand = new AhCommand(this, guiListener);
        getCommand("ah").setExecutor(ahCommand);

        getLogger().info("ItemAuction включён. Лотов загружено: " + listingManager.getAllListings().size());
    }

    @Override
    public void onDisable() {
        if (listingManager != null) {
            listingManager.save();
        }
        getLogger().info("ItemAuction выключен, данные сохранены.");
    }

    public ListingManager getListingManager() {
        return listingManager;
    }
}
