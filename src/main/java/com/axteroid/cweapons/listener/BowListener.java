package com.axteroid.customweapons.listener;

import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
// removed unused meta imports
import org.bukkit.projectiles.ProjectileSource;

import com.axteroid.customweapons.CustomWeapons;
import com.axteroid.customweapons.config.WeaponConfig;
import com.axteroid.customweapons.item.CustomItemManager;

import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class BowListener implements Listener {
    private final CustomWeapons plugin;
    private final CustomItemManager itemManager;

    public BowListener(CustomWeapons plugin, CustomItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        ProjectileSource source = event.getEntity().getShooter();
        if (!(source instanceof Player)) return;
        Player player = (Player) source;
        ItemStack used = player.getInventory().getItemInMainHand();
        if (used == null || (used.getType() != Material.BOW && used.getType() != Material.CROSSBOW)) {
            // try offhand
            used = player.getInventory().getItemInOffHand();
        }
        String id = itemManager.getItemId(used);
        if (id == null) return;
        WeaponConfig.WeaponDefinition def = plugin.getWeaponConfig().get(id);
        if (def == null) return;

        if (event.getEntity() instanceof Arrow arrow) {
            // Remove arrow glowing - we'll handle player glowing on hit instead

            Object flameLevelObj = def.legacyBowModifiers.get("flame_level");
            int flameLevel = (flameLevelObj instanceof Number) ? ((Number) flameLevelObj).intValue() : 0;
            for (WeaponConfig.BoostDef b : def.boosts) {
                if ("BOW_FLAME_LEVEL".equalsIgnoreCase(b.type) && b.value != null) {
                    flameLevel = Math.max(flameLevel, b.value.intValue());
                }
            }
            if (flameLevel > 0) {
                int ticks = 80 * flameLevel; // scale burn duration
                arrow.setFireTicks(Math.max(arrow.getFireTicks(), ticks));
            }

            // Attach weapon ID to arrow for hit detection
            PersistentDataContainer pdc = arrow.getPersistentDataContainer();
            pdc.set(plugin.key("bow_weapon_id"), PersistentDataType.STRING, id);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player)) return;
        
        String weaponId = arrow.getPersistentDataContainer().get(plugin.key("bow_weapon_id"), PersistentDataType.STRING);
        if (weaponId == null || !"kings_bow".equals(weaponId)) return;
        
        if (event.getHitEntity() instanceof Player hitPlayer) {
            // Make player glow for 10 seconds
            hitPlayer.setGlowing(true);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (hitPlayer.isOnline()) {
                    hitPlayer.setGlowing(false);
                }
            }, 200L); // 10 seconds = 200 ticks
        }
    }
}


