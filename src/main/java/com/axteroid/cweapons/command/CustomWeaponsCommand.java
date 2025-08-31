package com.cheetah.customweapons.command;

import com.cheetah.customweapons.CustomWeapons;
import com.cheetah.customweapons.config.WeaponConfig;
import com.cheetah.customweapons.item.CustomItemManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CustomWeaponsCommand implements CommandExecutor, TabCompleter {

    private final CustomWeapons plugin;
    private final CustomItemManager itemManager;

    public CustomWeaponsCommand(CustomWeapons plugin, CustomItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " <give|list|reload> ...");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "list":
                Map<String, WeaponConfig.WeaponDefinition> defs = plugin.getWeaponConfig().getDefinitions();
                sender.sendMessage(ChatColor.AQUA + "Custom Weapons (" + defs.size() + "):");
                for (WeaponConfig.WeaponDefinition d : defs.values()) {
                    String shownName = d.legacyDisplayName != null ? d.legacyDisplayName : d.weaponNameTemplate;
                    sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.GOLD + d.id + ChatColor.GRAY + " (" + (shownName != null ? shownName : d.id) + ")");
                }
                return true;
            case "reload":
                if (!sender.hasPermission("customweapons.use")) {
                    sender.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }
                plugin.reloadAll();
                sender.sendMessage(ChatColor.GREEN + "CustomWeapons fully reloaded.");
                return true;
            case "give":
                if (!sender.hasPermission("customweapons.use")) {
                    sender.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " give <weaponId> [player]");
                    return true;
                }
                String id = args[1];
                Player target;
                if (args.length >= 3) {
                    target = Bukkit.getPlayer(args[2]);
                } else if (sender instanceof Player) {
                    target = (Player) sender;
                } else {
                    sender.sendMessage(ChatColor.RED + "Specify a player.");
                    return true;
                }
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                if (plugin.getWeaponConfig().get(id) == null) {
                    sender.sendMessage(ChatColor.RED + "Unknown weapon id: " + id);
                    return true;
                }
                itemManager.give(target, id);
                // If bow with spectral behavior, optionally give one spectral arrow for convenience
                WeaponConfig.WeaponDefinition def = plugin.getWeaponConfig().get(id);
                boolean spectralLegacy = def.legacyBowModifiers != null && Boolean.TRUE.equals(def.legacyBowModifiers.get("spectral"));
                boolean spectralBoost = def.boosts.stream().anyMatch(b -> "BOW_SPECTRAL".equalsIgnoreCase(b.type));
                if (spectralLegacy || spectralBoost) {
                    target.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.SPECTRAL_ARROW, 1));
                }
                sender.sendMessage(ChatColor.GREEN + "Gave " + id + " to " + target.getName());
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand.");
                return true;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> base = new ArrayList<>();
            base.add("give");
            base.add("list");
            base.add("reload");
            return base.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return plugin.getWeaponConfig().getDefinitions().keySet().stream()
                    .filter(k -> k.startsWith(args[1].toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}


