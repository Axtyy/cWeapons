package com.cheetah.customweapons;

import com.cheetah.customweapons.command.CustomWeaponsCommand;
import com.cheetah.customweapons.config.WeaponConfig;
import com.cheetah.customweapons.item.CustomItemManager;
import com.cheetah.customweapons.listener.BowListener;
import com.cheetah.customweapons.listener.InventoryListener;
import com.cheetah.customweapons.task.HoldEffectsTask;
import org.bukkit.event.HandlerList;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomWeapons extends JavaPlugin {

    private static CustomWeapons instance;
    private WeaponConfig weaponConfig;
    private CustomItemManager itemManager;
    private HoldEffectsTask holdEffectsTask;

    public static CustomWeapons getInstance() {
        return instance;
    }

    public NamespacedKey key(String key) {
        return new NamespacedKey(this, key);
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadWeaponConfig();

        this.itemManager = new CustomItemManager(this);

        // Disabled polling task - using event-driven detection instead
        // int interval = Math.max(1, getConfig().getInt("tick_interval", 100));
        // this.holdEffectsTask = new HoldEffectsTask(this, itemManager, interval);
        // this.holdEffectsTask.start();

        getServer().getPluginManager().registerEvents(new BowListener(this, itemManager), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this, itemManager), this);

        CustomWeaponsCommand command = new CustomWeaponsCommand(this, itemManager);
        if (getCommand("cweapons") != null) {
            getCommand("cweapons").setExecutor(command);
            getCommand("cweapons").setTabCompleter(command);
        }
    }

    @Override
    public void onDisable() {
        if (holdEffectsTask != null) {
            holdEffectsTask.stop();
        }
    }

    public void reloadWeaponConfig() {
        reloadConfig();
        FileConfiguration config = getConfig();
        this.weaponConfig = new WeaponConfig(config);
    }

    public void reloadAll() {
        // Stop tasks
        if (holdEffectsTask != null) {
            holdEffectsTask.stop();
        }
        // Unregister listeners
        HandlerList.unregisterAll(this);
        // Reload configuration and managers
        reloadWeaponConfig();
        // Recreate item manager to ensure clean state
        this.itemManager = new CustomItemManager(this);
        // Re-register listeners
        getServer().getPluginManager().registerEvents(new BowListener(this, itemManager), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this, itemManager), this);
        // Re-attach command executor/tab completer
        CustomWeaponsCommand command = new CustomWeaponsCommand(this, itemManager);
        if (getCommand("cweapons") != null) {
            getCommand("cweapons").setExecutor(command);
            getCommand("cweapons").setTabCompleter(command);
        }
    }

    public WeaponConfig getWeaponConfig() {
        return weaponConfig;
    }

    public CustomItemManager getItemManager() {
        return itemManager;
    }
}


