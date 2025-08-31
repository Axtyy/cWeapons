package com.cheetah.customweapons.listener;

import com.cheetah.customweapons.CustomWeapons;
import com.cheetah.customweapons.config.WeaponConfig;
import com.cheetah.customweapons.item.CustomItemManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class InventoryListener implements Listener {
	private final CustomWeapons plugin;
	private final CustomItemManager itemManager;
	
	// Cache for effect types to avoid repeated lookups
	private static final Map<String, PotionEffectType> EFFECT_CACHE = new ConcurrentHashMap<>();
	static {
		EFFECT_CACHE.put("HOLD_SPEED", PotionEffectType.SPEED);
		EFFECT_CACHE.put("HOLD_STRENGTH", PotionEffectType.STRENGTH);
		EFFECT_CACHE.put("HOLD_RESISTANCE", PotionEffectType.RESISTANCE);
		EFFECT_CACHE.put("HOLD_SLOWNESS", PotionEffectType.SLOWNESS);
	}
	
	// Track what each player is currently holding to avoid redundant processing
	// Using synchronized maps instead of ConcurrentHashMap to avoid corruption
	private final Map<Player, String> currentMainHand = new java.util.HashMap<>();
	private final Map<Player, String> currentOffHand = new java.util.HashMap<>();
	
	// Track which effects are currently active for each player
	private final Map<Player, Set<String>> activeEffects = new java.util.HashMap<>();
	
	// Synchronization lock for all map operations
	private final Object mapLock = new Object();

	public InventoryListener(CustomWeapons plugin, CustomItemManager itemManager) {
		this.plugin = plugin;
		this.itemManager = itemManager;
	}
	
	// Safe synchronized map operations
	private void putMainHand(Player player, String weaponId) {
		synchronized (mapLock) {
			try {
				currentMainHand.put(player, weaponId);
			} catch (Exception e) {
				if (plugin != null) {
					plugin.getLogger().warning("Failed to update main hand for " + player.getName() + ": " + e.getClass().getSimpleName());
				}
			}
		}
	}
	
	private void putOffHand(Player player, String weaponId) {
		synchronized (mapLock) {
			try {
				currentOffHand.put(player, weaponId);
			} catch (Exception e) {
				if (plugin != null) {
					plugin.getLogger().warning("Failed to update off hand for " + player.getName() + ": " + e.getClass().getSimpleName());
				}
			}
		}
	}
	
	private void removePlayer(Player player) {
		synchronized (mapLock) {
			try {
				currentMainHand.remove(player);
				currentOffHand.remove(player);
				activeEffects.remove(player);
			} catch (Exception e) {
				if (plugin != null) {
					plugin.getLogger().warning("Failed to remove player " + player.getName() + ": " + e.getClass().getSimpleName());
				}
			}
		}
	}
	
	private Set<String> getOrCreateEffects(Player player) {
		synchronized (mapLock) {
			try {
				return activeEffects.computeIfAbsent(player, k -> new HashSet<>());
			} catch (Exception e) {
				if (plugin != null) {
					plugin.getLogger().warning("Failed to get effects for " + player.getName() + ": " + e.getClass().getSimpleName());
				}
				return new HashSet<>();
			}
		}
	}

	// Main method to update all effects for a player
	private void updatePlayerEffects(Player player) {
		try {
			if (itemManager == null) {
				return;
			}
			
			// Additional safety checks
			if (player == null || !player.isOnline() || player.getInventory() == null) {
				return;
			}
			
			// Get current items with additional null checks
			String mainId = null;
			String offId = null;
			
			try {
				mainId = itemManager.getItemId(player.getInventory().getItemInMainHand());
			} catch (Exception e) {
				if (plugin != null) {
					plugin.getLogger().warning("Error getting main hand item for " + player.getName() + ": " + e.getClass().getSimpleName());
				}
			}
			
			try {
				offId = itemManager.getItemId(player.getInventory().getItemInOffHand());
			} catch (Exception e) {
				if (plugin != null) {
					plugin.getLogger().warning("Error getting off hand item for " + player.getName() + ": " + e.getClass().getSimpleName());
				}
			}
			
			// Remove ALL current effects first
			clearAllEffects(player);
			
			// Apply new effects
			if (mainId != null) applyEffects(player, mainId);
			if (offId != null) applyEffects(player, offId);
			
			// Update tracking using synchronized operations
			putMainHand(player, mainId);
			putOffHand(player, offId);
			
		} catch (Exception e) {
			if (plugin != null) {
				plugin.getLogger().warning("Error updating player effects for " + player.getName() + ": " + 
					(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
				plugin.getLogger().warning("Stack trace: " + e.getClass().getSimpleName() + " at " + 
					(e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "unknown location"));
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onItemHeld(PlayerItemHeldEvent event) {
		if (plugin != null && event.getPlayer() != null && event.getPlayer().isOnline()) {
			// Delay to ensure the inventory is updated with the new item
			plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
				updatePlayerEffects(event.getPlayer());
			}, 1L);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onSwapHands(PlayerSwapHandItemsEvent event) {
		if (plugin != null && event.getPlayer() != null && event.getPlayer().isOnline()) {
			updatePlayerEffects(event.getPlayer());
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent event) {
		if (plugin == null) return;
		if (!(event.getWhoClicked() instanceof Player player)) return;
		// Any inventory click (number-keys, shift-click, swap-offhand) can affect held/offhand
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> updatePlayerEffects(player), 1L);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onInventoryDrag(InventoryDragEvent event) {
		if (plugin == null) return;
		if (!(event.getWhoClicked() instanceof Player player)) return;
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> updatePlayerEffects(player), 1L);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onItemDrop(PlayerDropItemEvent event) {
		if (plugin == null) return;
		// Update effects after a short delay to ensure item is dropped
		plugin.getServer().getScheduler().runTask(plugin, () -> updatePlayerEffects(event.getPlayer()));
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onItemPickup(EntityPickupItemEvent event) {
		if (plugin == null) return;
		if (!(event.getEntity() instanceof Player player)) return;
		// Delay to allow hotbar auto-insert or equip to settle
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> updatePlayerEffects(player), 1L);
	}

	// Initialize player state when they join
	@EventHandler
	public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
		// Delay initialization to ensure everything is properly loaded
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
			try {
				Player player = event.getPlayer();
				
				// Safety check for null plugin
				if (plugin == null) {
					System.err.println("Plugin is null during player join for " + player.getName());
					return;
				}
				
				// Safety check for null itemManager
				if (itemManager == null) {
					plugin.getLogger().warning("ItemManager is null during player join for " + player.getName());
					return;
				}
				
							// Initialize with current items and apply effects
			String mainId = itemManager.getItemId(player.getInventory().getItemInMainHand());
			String offId = itemManager.getItemId(player.getInventory().getItemInOffHand());
			
			// Use synchronized operations
			putMainHand(player, mainId);
			putOffHand(player, offId);
				
				// Apply effects from both hands
				if (mainId != null) applyEffects(player, mainId);
				if (offId != null) applyEffects(player, offId);
				
			} catch (Exception e) {
				if (plugin != null) {
					plugin.getLogger().severe("Error during player join: " + e.getMessage());
					e.printStackTrace();
				} else {
					System.err.println("Error during player join: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}, 1L); // 1 tick delay
	}
	
	// Clean up player data when they disconnect
	@EventHandler
	public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
		try {
			Player player = event.getPlayer();
			removePlayer(player);
		} catch (Exception e) {
			if (plugin != null) {
				plugin.getLogger().warning("Error during player quit cleanup: " + e.getMessage());
			}
		}
	}
	
	// Clear all custom weapon effects from a player
	private void clearAllEffects(Player player) {
		try {
			Set<String> effects;
			synchronized (mapLock) {
				effects = activeEffects.get(player);
			}
			
			if (effects != null) {
				for (String effectType : effects) {
					PotionEffectType type = EFFECT_CACHE.get(effectType);
					if (type != null) {
						player.removePotionEffect(type);
					}
				}
				effects.clear();
			}
		} catch (Exception e) {
			if (plugin != null) {
				plugin.getLogger().warning("Error clearing effects for " + player.getName() + ": " + e.getMessage());
			}
		}
	}
	
	private void applyEffects(Player player, String weaponId) {
		try {
			if (plugin == null) return;
			
			WeaponConfig.WeaponDefinition def = plugin.getWeaponConfig().get(weaponId);
			if (def == null || def.boosts == null) return;
			
			// Get or create active effects set for this player
			Set<String> effects = getOrCreateEffects(player);
			
			// Batch effect application for better performance
			Set<PotionEffect> effectsToApply = new HashSet<>();
			
			for (WeaponConfig.BoostDef b : def.boosts) {
				PotionEffectType type = EFFECT_CACHE.get(b.type);
				if (type != null && b.value != null) {
					int amplifier = Math.max(0, b.value.intValue() - 1);
					effectsToApply.add(new PotionEffect(type, Integer.MAX_VALUE, amplifier, true, false, true));
					try {
						effects.add(b.type); // Track this effect as active
					} catch (Exception e) {
						if (plugin != null) {
							plugin.getLogger().warning("Failed to track effect " + b.type + " for " + player.getName() + ": " + e.getClass().getSimpleName());
						}
					}
				}
			}
			
			// Apply all effects at once
			if (!effectsToApply.isEmpty()) {
				player.addPotionEffects(effectsToApply);
			}
		} catch (Exception e) {
			if (plugin != null) {
				plugin.getLogger().warning("Error applying effects for " + player.getName() + ": " + e.getMessage());
			}
		}
	}
}