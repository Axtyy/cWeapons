package com.cheetah.customweapons.task;

import com.cheetah.customweapons.CustomWeapons;
import com.cheetah.customweapons.config.WeaponConfig;
import com.cheetah.customweapons.item.CustomItemManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

public class HoldEffectsTask implements Runnable {
    private final CustomWeapons plugin;
    private final CustomItemManager itemManager;
    private final int intervalTicks;
    private int taskId = -1;
    
    // Track what each player was holding last tick
    private final Map<Player, String> lastMainHand = new HashMap<>();
    private final Map<Player, String> lastOffHand = new HashMap<>();

    public HoldEffectsTask(CustomWeapons plugin, CustomItemManager itemManager, int intervalTicks) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.intervalTicks = intervalTicks;
    }

    public void start() {
        if (taskId != -1) stop();
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, 20L, intervalTicks);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        // Clean up tracking maps
        lastMainHand.clear();
        lastOffHand.clear();
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack main = player.getInventory().getItemInMainHand();
            ItemStack off = player.getInventory().getItemInOffHand();
            
            String currentMainId = itemManager.getItemId(main);
            String currentOffId = itemManager.getItemId(off);
            String lastMainId = lastMainHand.get(player);
            String lastOffId = lastOffHand.get(player);
            
            // Check if main hand changed
            if (!java.util.Objects.equals(currentMainId, lastMainId)) {
                if (lastMainId != null) {
                    // Remove effects from previous item
                    removeEffects(player, lastMainId);
                }
                if (currentMainId != null) {
                    // Apply effects from new item
                    applyEffects(player, currentMainId);
                }
                lastMainHand.put(player, currentMainId);
            }
            
            // Check if off hand changed
            if (!java.util.Objects.equals(currentOffId, lastOffId)) {
                if (lastOffId != null) {
                    // Remove effects from previous item
                    removeEffects(player, lastOffId);
                }
                if (currentOffId != null) {
                    // Apply effects from new item
                    applyEffects(player, currentOffId);
                }
                lastOffHand.put(player, currentOffId);
            }
        }
    }

    private void applyEffects(Player player, String weaponId) {
        WeaponConfig.WeaponDefinition def = plugin.getWeaponConfig().get(weaponId);
        if (def == null) return;
        
        // Legacy hold_effects support
        if (def.legacyHoldEffects != null) {
            for (Map.Entry<String, Integer> e : def.legacyHoldEffects.entrySet()) {
                PotionEffectType type = PotionEffectType.getByName(e.getKey());
                if (type == null) continue;
                int amplifier = Math.max(0, e.getValue() - 1);
                // Apply with long duration (effectively permanent while holding)
                player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, amplifier, true, false, true));
            }
        }
        
        // New schema boost-based hold effects
        if (def.boosts != null) {
            for (WeaponConfig.BoostDef b : def.boosts) {
                PotionEffectType type = null;
                Integer level = null;
                switch (b.type) {
                    case "HOLD_SPEED":
                        type = PotionEffectType.SPEED; level = b.value != null ? b.value.intValue() : 1; break;
                    case "HOLD_STRENGTH":
                        type = PotionEffectType.STRENGTH; level = b.value != null ? b.value.intValue() : 1; break;
                    case "HOLD_RESISTANCE":
                        type = PotionEffectType.RESISTANCE; level = b.value != null ? b.value.intValue() : 1; break;
                    case "HOLD_SLOWNESS":
                        type = PotionEffectType.SLOWNESS; level = b.value != null ? b.value.intValue() : 1; break;
                    default:
                        break;
                }
                if (type != null && level != null) {
                    int amplifier = Math.max(0, level - 1);
                    // Apply with long duration (effectively permanent while holding)
                    player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, amplifier, true, false, true));
                }
            }
        }
    }
    
    private void removeEffects(Player player, String weaponId) {
        WeaponConfig.WeaponDefinition def = plugin.getWeaponConfig().get(weaponId);
        if (def == null) return;
        
        // Remove legacy hold_effects
        if (def.legacyHoldEffects != null) {
            for (String effectName : def.legacyHoldEffects.keySet()) {
                PotionEffectType type = PotionEffectType.getByName(effectName);
                if (type != null) {
                    player.removePotionEffect(type);
                }
            }
        }
        
        // Remove new schema boost-based hold effects
        if (def.boosts != null) {
            for (WeaponConfig.BoostDef b : def.boosts) {
                PotionEffectType type = null;
                switch (b.type) {
                    case "HOLD_SPEED":
                        type = PotionEffectType.SPEED; break;
                    case "HOLD_STRENGTH":
                        type = PotionEffectType.STRENGTH; break;
                    case "HOLD_RESISTANCE":
                        type = PotionEffectType.RESISTANCE; break;
                    case "HOLD_SLOWNESS":
                        type = PotionEffectType.SLOWNESS; break;
                    default:
                        break;
                }
                if (type != null) {
                    player.removePotionEffect(type);
                }
            }
        }
    }
}


