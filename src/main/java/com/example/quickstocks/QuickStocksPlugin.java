package com.example.quickstocks;

import org.bukkit.plugin.java.JavaPlugin;

public final class QuickStocksPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("QuickStocks enabled (Paper 1.21.8)");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("QuickStocks disabled");
    }
}

