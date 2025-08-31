package com.cheetah.customweapons.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeaponConfig {

	public static class BoostDef {
		public final String type;
		public final Double value; // nullable
		public final Double chance; // nullable

		public BoostDef(String type, Double value, Double chance) {
			this.type = type;
			this.value = value;
			this.chance = chance;
		}
	}

    public static class WeaponDefinition {
        public final String id;
        public final String persistentKey;

        // Visuals
        public final String weaponNameTemplate; // may contain <colorize:...>
        public final List<String> weaponLoreTemplates;
        public final List<String> colorized; // for <colorize>
        public final List<String> colors; // {color_1..n}
        public final String boostDisplay; // e.g., "{color_1}▪ &7{boost}"

        // Base item
        public final Material material;
        public final Map<String, Integer> weaponEnchants; // unsafe allowed
        public final boolean vanillaEnchants;

        // Back-compat fields (optional)
        public final String legacyDisplayName;
        public final Map<String, Integer> legacyEnchantments;
        public final Map<String, Integer> legacyHoldEffects;
        public final boolean legacyGiveSpectral;
        public final Map<String, Object> legacyBowModifiers;

        // Boosts
        public final List<BoostDef> boosts;

        public WeaponDefinition(
                String id,
                String persistentKey,
                String weaponNameTemplate,
                List<String> weaponLoreTemplates,
                List<String> colorized,
                List<String> colors,
                String boostDisplay,
                Material material,
                Map<String, Integer> weaponEnchants,
                boolean vanillaEnchants,
                String legacyDisplayName,
                Map<String, Integer> legacyEnchantments,
                Map<String, Integer> legacyHoldEffects,
                boolean legacyGiveSpectral,
                Map<String, Object> legacyBowModifiers,
                List<BoostDef> boosts
        ) {
            this.id = id;
            this.persistentKey = persistentKey;
            this.weaponNameTemplate = weaponNameTemplate;
            this.weaponLoreTemplates = weaponLoreTemplates;
            this.colorized = colorized;
            this.colors = colors;
            this.boostDisplay = boostDisplay;
            this.material = material;
            this.weaponEnchants = weaponEnchants;
            this.vanillaEnchants = vanillaEnchants;
            this.legacyDisplayName = legacyDisplayName;
            this.legacyEnchantments = legacyEnchantments;
            this.legacyHoldEffects = legacyHoldEffects;
            this.legacyGiveSpectral = legacyGiveSpectral;
            this.legacyBowModifiers = legacyBowModifiers;
            this.boosts = boosts;
        }
    }

    private final Map<String, WeaponDefinition> definitions = new HashMap<>();

    public WeaponConfig(FileConfiguration config) {
        ConfigurationSection weapons = config.getConfigurationSection("weapons");
        if (weapons != null) {
            for (String id : weapons.getKeys(false)) {
                ConfigurationSection section = weapons.getConfigurationSection(id);
                if (section == null) continue;

                // Detect new schema by presence of sub-section 'weapon'
                ConfigurationSection weaponSec = section.getConfigurationSection("weapon");
                if (weaponSec != null) {
                    String persistentKey = section.getString("id", id);
                    String weaponNameTemplate = weaponSec.getString("name", id);
                    List<String> lore = weaponSec.getStringList("lore");
                    List<String> colorized = section.getStringList("colorized");
                    List<String> colors = section.getStringList("colors");
                    String boostDisplay = section.getString("boost-display", "{color_1}{boost}");
                    String matName = weaponSec.getString("material", "NETHERITE_SWORD");
                    Material material = Material.matchMaterial(matName);
                    if (material == null) material = Material.NETHERITE_SWORD;
                    boolean vanillaEnchants = section.getBoolean("vanillaEnchants", false);

                    Map<String, Integer> enchants = new HashMap<>();
                    ConfigurationSection enchSec = weaponSec.getConfigurationSection("enchants");
                    if (enchSec != null) {
                        for (String key : enchSec.getKeys(false)) {
                            ConfigurationSection e = enchSec.getConfigurationSection(key);
                            if (e == null) continue;
                            String enchName = e.getString("enchant");
                            int level = e.getInt("level", 1);
                            if (enchName != null) {
                                enchants.put(enchName.toUpperCase(), level);
                            }
                        }
                    }

                    List<BoostDef> boosts = new ArrayList<>();
                    ConfigurationSection boostsSec = section.getConfigurationSection("boosts");
                    if (boostsSec != null) {
                        for (String k : boostsSec.getKeys(false)) {
                            ConfigurationSection b = boostsSec.getConfigurationSection(k);
                            if (b == null) continue;
                            String type = b.getString("type", "").toUpperCase();
                            Double value = b.isSet("value") ? b.getDouble("value") : null;
                            Double chance = b.isSet("chance") ? b.getDouble("chance") : null;
                            boosts.add(new BoostDef(type, value, chance));
                        }
                    }

                    definitions.put(id, new WeaponDefinition(
                            id,
                            persistentKey,
                            weaponNameTemplate,
                            Collections.unmodifiableList(lore),
                            Collections.unmodifiableList(colorized),
                            Collections.unmodifiableList(colors),
                            boostDisplay,
                            material,
                            Collections.unmodifiableMap(enchants),
                            vanillaEnchants,
                            null,
                            Collections.emptyMap(),
                            Collections.emptyMap(),
                            false,
                            Collections.emptyMap(),
                            Collections.unmodifiableList(boosts)
                    ));
                    continue;
                }

                // Fallback legacy schema
                String displayName = section.getString("display_name", id);
                String matName = section.getString("material", "NETHERITE_SWORD");
                Material material = Material.matchMaterial(matName);
                if (material == null) material = Material.NETHERITE_SWORD;

                Map<String, Integer> enchantments = new HashMap<>();
                ConfigurationSection enchSec = section.getConfigurationSection("enchantments");
                if (enchSec != null) {
                    for (String ench : enchSec.getKeys(false)) {
                        enchantments.put(ench.toUpperCase(), enchSec.getInt(ench));
                    }
                }

                Map<String, Integer> holdEffects = new HashMap<>();
                ConfigurationSection effectsSec = section.getConfigurationSection("hold_effects");
                if (effectsSec != null) {
                    for (String eff : effectsSec.getKeys(false)) {
                        holdEffects.put(eff.toUpperCase(), effectsSec.getInt(eff));
                    }
                }

                boolean giveSpectral = section.getBoolean("give_spectral_arrows", false);
                Map<String, Object> bowModifiers = new HashMap<>();
                ConfigurationSection bowSec = section.getConfigurationSection("bow_modifiers");
                if (bowSec != null) {
                    for (String key : bowSec.getKeys(false)) {
                        bowModifiers.put(key, bowSec.get(key));
                    }
                }

                String persistentKey = section.getString("persistent_key", id);

                definitions.put(id, new WeaponDefinition(
                        id,
                        persistentKey,
                        null,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        "",
                        material,
                        Collections.emptyMap(),
                        false,
                        displayName,
                        Collections.unmodifiableMap(enchantments),
                        Collections.unmodifiableMap(holdEffects),
                        giveSpectral,
                        Collections.unmodifiableMap(bowModifiers),
                        Collections.emptyList()
                ));
            }
        }
    }

    public Map<String, WeaponDefinition> getDefinitions() {
        return Collections.unmodifiableMap(definitions);
    }

    public WeaponDefinition get(String id) {
        return definitions.get(id);
    }
}


