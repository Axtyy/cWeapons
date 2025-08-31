package com.axteroid.customweapons.item;

import com.axteroid.customweapons.CustomWeapons;
import com.axteroid.customweapons.config.WeaponConfig;
import com.axteroid.customweapons.util.TextUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
// removed unused import
import java.util.Map;

public class CustomItemManager {

	private static final String TAG_ID = "cweapons_id";
	private final CustomWeapons plugin;

	public CustomItemManager(CustomWeapons plugin) {
		this.plugin = plugin;
	}

	public ItemStack createItem(String id) {
		WeaponConfig.WeaponDefinition def = plugin.getWeaponConfig().get(id);
		if (def == null) return null;
		ItemStack stack = new ItemStack(def.material);
		ItemMeta meta = stack.getItemMeta();
		if (meta != null) {
			// Name
			String displayName;
			if (def.weaponNameTemplate != null) {
				displayName = TextUtil.applyColorizeTag(def.weaponNameTemplate, def.colorized);
				displayName = TextUtil.applyPlaceholders(displayName, def.colors, null);
			} else {
				displayName = def.legacyDisplayName;
			}
			if (displayName != null) meta.setDisplayName(displayName);

			// Lore
			List<String> lore = new ArrayList<>();
			if (def.weaponLoreTemplates != null && !def.weaponLoreTemplates.isEmpty()) {
				// Render boosts lines
				List<String> boostLines = new ArrayList<>();
				for (WeaponConfig.BoostDef b : def.boosts) {
					String bText = b.type;
					if (b.value != null) bText += " x" + b.value;
					if (b.chance != null) bText += " (" + (int) Math.round(b.chance * 100) + "%)";
					String rendered = def.boostDisplay.replace("{boost}", bText);
					rendered = TextUtil.applyPlaceholders(rendered, def.colors, bText);
					boostLines.add(TextUtil.applyAmpColors(rendered));
				}
				String joinedBoosts = String.join("\n", boostLines);
				for (String line : def.weaponLoreTemplates) {
					String withColors = TextUtil.applyPlaceholders(line, def.colors, joinedBoosts);
					withColors = TextUtil.applyAmpColors(withColors);
					lore.add(withColors);
				}
			}
			if (lore.isEmpty() && def.legacyHoldEffects != null && !def.legacyHoldEffects.isEmpty()) {
				lore.add(TextUtil.applyAmpColors("&7Hold Effects:"));
				for (Map.Entry<String, Integer> e : def.legacyHoldEffects.entrySet()) {
					lore.add(TextUtil.applyAmpColors("&3" + e.getKey() + " " + e.getValue()));
				}
			}
			if (!lore.isEmpty()) meta.setLore(lore);

			// Requested: keep items unenchanted (ignore configured enchants)
			// Ensure no enchants and no glint regardless of external changes
			for (Enchantment e : meta.getEnchants().keySet()) {
				meta.removeEnchant(e);
			}
			try {
				meta.setEnchantmentGlintOverride(Boolean.FALSE);
			} catch (Throwable ignored) {
				// Older APIs may not have this; safe to ignore
			}
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

			PersistentDataContainer pdc = meta.getPersistentDataContainer();
			pdc.set(plugin.key(TAG_ID), PersistentDataType.STRING, def.persistentKey);
			stack.setItemMeta(meta);
		}

		return stack;
	}

	public String getItemId(ItemStack stack) {
		if (stack == null) return null;
		ItemMeta meta = stack.getItemMeta();
		if (meta == null) return null;
		String id = meta.getPersistentDataContainer().get(plugin.key(TAG_ID), PersistentDataType.STRING);
		if (id != null) {
			for (WeaponConfig.WeaponDefinition def : plugin.getWeaponConfig().getDefinitions().values()) {
				if (def.persistentKey.equals(id)) return def.id;
			}
		}

		// Fallback: identify by display name + lore when another plugin created the item
		final String actualName = meta.hasDisplayName() ? meta.getDisplayName() : null;
		final List<String> actualLore = meta.hasLore() ? meta.getLore() : null;
		final Material material = stack.getType();

		for (WeaponConfig.WeaponDefinition def : plugin.getWeaponConfig().getDefinitions().values()) {
			if (def.material != null && def.material != material) continue;
			String expectedName = renderDisplayName(def);
			if (!matchesText(actualName, expectedName)) continue;
			List<String> expectedLore = renderLore(def);
			if (!matchesLore(actualLore, expectedLore)) continue;
			return def.id;
		}

		return null;
	}

	private String renderDisplayName(WeaponConfig.WeaponDefinition def) {
		if (def.weaponNameTemplate != null) {
			String displayName = TextUtil.applyColorizeTag(def.weaponNameTemplate, def.colorized);
			return TextUtil.applyPlaceholders(displayName, def.colors, null);
		}
		return def.legacyDisplayName;
	}

	private List<String> renderLore(WeaponConfig.WeaponDefinition def) {
		List<String> lore = new ArrayList<>();
		if (def.weaponLoreTemplates != null && !def.weaponLoreTemplates.isEmpty()) {
			List<String> boostLines = new ArrayList<>();
			for (WeaponConfig.BoostDef b : def.boosts) {
				String bText = b.type;
				if (b.value != null) bText += " x" + b.value;
				if (b.chance != null) bText += " (" + (int) Math.round(b.chance * 100) + "%)";
				String rendered = def.boostDisplay.replace("{boost}", bText);
				rendered = TextUtil.applyPlaceholders(rendered, def.colors, bText);
				boostLines.add(TextUtil.applyAmpColors(rendered));
			}
			String joinedBoosts = String.join("\n", boostLines);
			for (String line : def.weaponLoreTemplates) {
				String withColors = TextUtil.applyPlaceholders(line, def.colors, joinedBoosts);
				withColors = TextUtil.applyAmpColors(withColors);
				lore.add(withColors);
			}
		}
		if (lore.isEmpty() && def.legacyHoldEffects != null && !def.legacyHoldEffects.isEmpty()) {
			lore.add(TextUtil.applyAmpColors("&7Hold Effects:"));
			for (Map.Entry<String, Integer> e : def.legacyHoldEffects.entrySet()) {
				lore.add(TextUtil.applyAmpColors("&3" + e.getKey() + " " + e.getValue()));
			}
		}
		return lore;
	}

	private boolean matchesText(String actual, String expected) {
		if (actual == null || expected == null) return false;
		if (actual.equals(expected)) return true;
		return ChatColor.stripColor(actual).equals(ChatColor.stripColor(expected));
	}

	private boolean matchesLore(List<String> actual, List<String> expected) {
		if (expected == null || expected.isEmpty()) return false;
		if (actual == null || actual.size() != expected.size()) return false;
		for (int i = 0; i < expected.size(); i++) {
			if (!matchesText(actual.get(i), expected.get(i))) return false;
		}
		return true;
	}

	public boolean isCustomWeapon(ItemStack stack) {
		return getItemId(stack) != null;
	}

	public void give(Player player, String id) {
		ItemStack item = createItem(id);
		if (item != null) {
			player.getInventory().addItem(item);
		}
	}
}


