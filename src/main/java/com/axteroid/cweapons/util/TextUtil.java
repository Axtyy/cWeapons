package com.cheetah.customweapons.util;

import org.bukkit.ChatColor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtil {

    private static final Pattern COLORIZE_TAG = Pattern.compile("<colorize:([^>]+)>");
    private static final Pattern HEX_COLOR = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private TextUtil() {}

    public static String applyAmpColors(String input) {
        if (input == null) return null;
        String withHex = translateHexColorCodes(input);
        return ChatColor.translateAlternateColorCodes('&', withHex);
    }

    private static String translateHexColorCodes(String input) {
        Matcher matcher = HEX_COLOR.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    public static String applyPlaceholders(String input, List<String> colors, String boostLine) {
        if (input == null) return null;
        String out = input;
        for (int i = 0; i < colors.size(); i++) {
            String key = "{color_" + (i + 1) + "}";
            out = out.replace(key, applyAmpColors(colors.get(i)));
        }
        if (boostLine != null) {
            out = out.replace("{boost}", boostLine);
            out = out.replace("{boosts}", boostLine);
        }
        return out;
    }

    public static String applyColorizeTag(String input, List<String> colorized) {
        if (input == null) return null;
        Matcher m = COLORIZE_TAG.matcher(input);
        if (!m.find()) return applyAmpColors(input);
        String text = m.group(1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String prefix = i < colorized.size() ? colorized.get(i) : colorized.get(colorized.size() - 1);
            sb.append(applyAmpColors(prefix)).append(text.charAt(i));
        }
        String replaced = m.replaceFirst(sb.toString());
        return applyAmpColors(replaced);
    }
}


