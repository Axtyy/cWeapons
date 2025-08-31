# cWeapons (Paper 1.21)

A lightweight, event-driven custom weapons plugin with configurable names, lore, and boosts. Items are unenchanted (no glint), support hex colors (&#RRGGBB), and apply/remove hold effects instantly without lag.

## Features
- Legendary-style items via config: name, lore, colors, and boost lines
- Hex colors with format `&#RRGGBB` and classic `&` codes
- Unenchanted visuals: enchantment glint disabled
- Instant hold effects; instantly removed when unheld (main/off-hand aware)
- Optimized, event-driven effect updates for 50+ players
- Robust reload: cleanly unregisters and re-registers components
- Works even if another plugin creates the item by matching name + lore (PDC fallback)
- King's Bow: arrows that hit players make them glow for 10s

## Commands
- `/cweapons give <player> <weapon_id>`: Give a configured weapon
- `/cweapons list`: List available weapon ids
- `/cweapons reload`: Reload config and reinitialize components

## Quick Start
1) Build
```bash
mvn clean package
```
2) Install
- Copy `target/LegendaryWeapons.jar` into your Paper 1.21 `plugins/` folder
- Start/restart the server (or use `/cweapons reload` after the first start)

## Config Overview (config.yml)
```yaml
weapons:
  samurai_katana:
    weapon:
      name: "Samurai Katana"
      lore:
        - "{boosts}"
      material: NETHERITE_SWORD
    boosts:
      - type: HOLD_SPEED
        value: 3
      - type: REFRESH_SECONDS
        value: 5

  kings_sword:
    weapon:
      name: "King's Sword"
      lore:
        - "{boosts}"
      material: NETHERITE_SWORD
    boosts:
      - type: HOLD_STRENGTH
        value: 1
      - type: HOLD_SPEED
        value: 1

  kings_shield:
    weapon:
      name: "King's Shield"
      lore:
        - "{boosts}"
      material: SHIELD
    boosts:
      - type: HOLD_RESISTANCE
        value: 2
      - type: HOLD_SLOWNESS
        value: 2

  kings_bow:
    weapon:
      name: "King's Bow"
      lore:
        - "{boosts}"
      material: BOW
    boosts:
      - type: BOW_GLOW_ON_HIT
        value: 10
```

Notes:
- Use `{boosts}` in lore; each boost is rendered with `boost-display`
- Hex colors `&#RRGGBB` and `&` color codes are supported

## License
MIT
