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
tick_interval: 100

weapons:
  samurai_katana:
    name: samurai_katana
    id: samurai_katana
    weapon:
      name: '&#9E83F1Samurai Katana'
      lore:
      - '&#AEAEAEA legendary weapon of the royal armory.'
      - ''
      - '&fPASSIVE: &#AEAEAEGain permanent &#27FFEESpeed III&#AEAEAE'
      - '&#AEAEAEwhile holding. &#AEAEAE(Refreshes every 5 seconds)'
      material: NETHERITE_SWORD
      enchants: {}
    boosts:
      '1':
        type: HOLD_SPEED
        value: 3

  kings_sword:
    name: kings_sword
    id: kings_sword
    weapon:
      name: '&#F16164King''s Sword'
      lore:
      - '&#AEAEAEA legendary weapon of the royal armory.'
      - ''
      - '&fPASSIVE: &#AEAEAEGain permanent &#F13B3BStrength I'
      - '&#AEAEAEand &#27FFEESpeed I&#AEAEAE while holding.'
      material: NETHERITE_SWORD
      enchants: {}
    boosts:
      '1':
        type: HOLD_STRENGTH
        value: 1
      '2':
        type: HOLD_SPEED
        value: 1

  kings_shield:
    name: kings_shield
    id: kings_shield
    weapon:
      name: '&#DBCC00King''s Shield'
      lore:
      - '&#AEAEAEA legendary artifact of the royal armory.'
      - ''
      - '&fPASSIVE: &#AEAEAEGain permanent &#ACF192Resistance II&#AEAEAE while holding.'
      - '&fPASSIVE: &#AEAEAEGain permanent &#3089EASlowness II&#AEAEAE while holding.'
      - ''
      material: SHIELD
      enchants: {}
    boosts:
      '1':
        type: HOLD_RESISTANCE
        value: 2
      '2':
        type: HOLD_SLOWNESS
        value: 2

  kings_bow:
    name: kings_bow
    id: kings_bow
    weapon:
      name: '&#F18A55King''s Bow'
      lore:
      - '&#AEAEAEA legendary weapon of the royal armory.'
      - ''
      - '&fPASSIVE: &#AEAEAEShoots Spectral Arrows.'
      - ''
      material: BOW
      enchants: {}
    boosts:
      '2':
        type: BOW_SPECTRAL
```

Notes:
- Use `{boosts}` in lore; each boost is rendered with `boost-display`
- Hex colors `&#RRGGBB` and `&` color codes are supported

## License
MIT
