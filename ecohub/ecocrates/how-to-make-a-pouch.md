---
title: "How to Make a Pouch"
sidebar_position: 6
---

Pouches are single-use reward items. Right-clicking one uses it up, plays a GUI roll, and gives exactly one reward. A pouch defines the **item** players hold, an optional **price** and **open conditions**, a **preview GUI**, and the **rewards** it can give. This page walks you through making one from an empty file to a working item.

## Quick start

1. Open the `/pouches/` folder inside the EcoCrates plugin folder.
2. Copy `_example.yml` and rename it to your pouch's ID, e.g. `mining.yml`.
3. Set the `name`, `rarity`, pick a GUI `roll`, and set the `item` players will hold.
4. List the reward IDs you want under `rewards`, and lay them out in the `preview` pages.
5. Run `/ecocrates reload`, then `/ecocrates give <player> pouch mining` to hand one out.
6. Right-click the pouch to confirm the roll plays and a reward is given.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real pouch. You can also organise pouches into subfolders inside `pouches/`, and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the pouch's ID. This is what you use in commands and placeholders. The `item`, recipe ingredients, and preview items use the [Item Lookup System](https://hub.auxilor.io/wiki/eco/the-item-lookup-system-the-item-lookup-system) format.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the pouch will not load.
:::

## The structure of a pouch

| Part | What it controls |
| --- | --- |
| **Basics** | The display name, rarity, roll animation, and shift-click behaviour |
| **Item** | The pouch item players hold, and an optional crafting recipe |
| **Price** | An optional cost on top of using up the pouch |
| **Open conditions** | Requirements checked before anything is taken |
| **Preview** | The GUI players see when previewing the pouch |
| **Effects** | What runs when the pouch opens and finishes rolling |
| **Rewards** | Which rewards are in the pool |

```yaml
# === Basics: identity and behaviour ===
name: "&aMining Pouch" # The display name of the pouch
rarity: "&aCommon" # Display only
roll: csgo # The opening animation; GUI rolls only
preview-on-shift: true # Shift right-click opens the preview instead of the pouch

# === Item: the pouch players hold ===
item:
  item: leather name:"&aMining Pouch" # The pouch item
  lore: # Lore applied to the pouch
    - "&fRight click to open"
    - "&fShift + right click to preview"
  crafting: # Optional; remove this block to make the pouch uncraftable
    enabled: false # If this pouch can be crafted
    permission: ecocrates.craft.pouch.mining # Optional; permission needed to use the recipe
    shapeless: false # Optional; if true, slot positions don't matter, defaults to false
    recipe: # The 3x3 recipe ingredients
      - air
      - leather
      - air
      - leather
      - iron_pickaxe
      - leather
      - air
      - leather
      - air

# === Price: optional extra cost ===
price:
  type: coins # The currency to charge
  value: 500 # The cost to open
  display: "&e%value% coins" # How the price is shown to players

# === Open conditions: requirements checked before the price or pouch is taken ===
open-conditions: [ ]

# === Preview: the GUI players browse before opening ===
preview:
  title: "Mining Pouch" # The title of the preview GUI. Supports %page% and %max_page% placeholders.
  rows: 3 # Number of rows, between 1 and 6
  custom-slots: [] # GUI slots shared across all preview pages
  forwards-arrow: # Shown on every page except the last
    item: arrow name:"&fNext Page"
    row: 3
    column: 6
  backwards-arrow: # Shown on every page except the first
    item: arrow name:"&fPrevious Page"
    row: 3
    column: 4
  pages:
    - page: 1
      mask: # Decorative filler items
        items:
          - gray_stained_glass_pane
        pattern:
          - "111111111"
          - "110101011"
          - "111111111"
      custom-slots: [] # GUI slots for this page only
      rewards:
        - id: diamond_sword # The reward ID to display here
          row: 2
          column: 3
        - id: stack_of_emeralds
          row: 2
          column: 5
        - id: 1000_coins
          row: 2
          column: 7

# === Effects: what runs on open and on finish ===
open-effects:
  - id: play_sound
    args:
      sound: item_bundle_drop_contents
      volume: 1
      pitch: 1
finish-effects:
  - id: send_message
    args:
      message: "You found %reward%&f in your %pouch%&f!"

# === Rewards: the pool of reward IDs ===
rewards:
  - diamond_sword
  - stack_of_emeralds
  - 1000_coins
```

### Basics

The identity of the pouch: its name, rarity, roll animation, and what shift right-click does.

```yaml
name: "&aMining Pouch" # The display name of the pouch
rarity: "&aCommon" # Display only
roll: csgo # The opening animation; GUI rolls only
preview-on-shift: true # Shift right-click opens the preview instead of the pouch
```

:::warning GUI rolls only
Pouches open from the player's hand, not at a place in the world, so only GUI rolls are supported: `csgo`, `slot_machine`, `elimination`, `pick`, `choose`, `match`, and `instant`. Any other roll falls back to `csgo` and logs a warning on reload. See [Animations / Rolls](roll-animations).
:::

### Item

The pouch item players hold, with its lore and an optional crafting recipe.

```yaml
item:
  item: leather name:"&aMining Pouch" # The pouch item
  lore: # Lore applied to the pouch
    - "&fRight click to open"
    - "&fShift + right click to preview"
  crafting: # Optional; remove this block to make the pouch uncraftable
    enabled: false # If this pouch can be crafted
    permission: ecocrates.craft.pouch.mining # Optional; permission needed to use the recipe
    shapeless: false # Optional; if true, slot positions don't matter, defaults to false
    recipe: # The 3x3 recipe ingredients
      - air
      - leather
      - air
      - leather
      - iron_pickaxe
      - leather
      - air
      - leather
      - air
```

:::info
Every pouch is registered as a custom item, `ecocrates:pouch_<id>`, so you can sell it in shops or give it with any plugin that supports item lookups. Pouches can never be placed, even if the item is a block.
:::

### Price

An optional cost charged on top of using up the pouch. Remove the block and the pouch item is the only cost.

```yaml
price:
  type: coins # The currency to charge
  value: 500 # The cost to open
  display: "&e%value% coins" # How the price is shown to players
```

### Open conditions

Conditions that must all be met before the pouch opens. They're checked before the price is charged or the pouch is used up, so a failed condition costs nothing.

```yaml
open-conditions:
  - id: in_world
    args:
      world: world
    not-met-effects:
      - id: send_message
        args:
          message: "&cYou can only open this pouch in the overworld!"
```

### Preview

The GUI players see when they preview the pouch, opened with shift right-click or `/ecocrates preview pouch <id>`. It works exactly like a crate preview.

```yaml
preview:
  title: "Mining Pouch" # The title of the preview GUI. Supports %page% and %max_page% placeholders.
  rows: 3 # Number of rows, between 1 and 6
  custom-slots: [] # GUI slots shared across all preview pages
  forwards-arrow: # Shown on every page except the last
    item: arrow name:"&fNext Page"
    row: 3
    column: 6
  backwards-arrow: # Shown on every page except the first
    item: arrow name:"&fPrevious Page"
    row: 3
    column: 4
  pages:
    - page: 1
      mask: # Decorative filler items
        items:
          - gray_stained_glass_pane
      custom-slots: [] # GUI slots for this page only
      rewards:
        - id: diamond_sword # The reward ID to display here
          row: 2
          column: 3
```

:::tip
Add custom buttons to the preview with custom GUI slots; see [Custom GUI Slots](https://hub.auxilor.io/wiki/eco/pages).
:::

### Effects

`open-effects` run once the pouch has been paid for and used up; `finish-effects` run when the roll lands on a reward.

```yaml
open-effects:
  - id: play_sound
    args:
      sound: item_bundle_drop_contents
      volume: 1
      pitch: 1
finish-effects:
  - id: send_message
    args:
      message: "You found %reward%&f in your %pouch%&f!"
```

:::danger Effects are their own system
Effects, conditions, and filters are a shared system documented outside this plugin. To configure them:

- [Configuring an Effect](https://hub.auxilor.io/wiki/libreforge/configuring-an-effect)
- [Configuring an Effect Chain](https://hub.auxilor.io/wiki/libreforge/configuring-a-chain)
:::

### Rewards

The pool of reward IDs this pouch can give. This is kept separate from the preview layout, so you can include secret rewards that never show in the GUI.

```yaml
rewards:
  - diamond_sword
  - stack_of_emeralds
  - 1000_coins
```

## Internal placeholders

| Placeholder | Value |
| --- | --- |
| `%player%` | The player's name. |
| `%pouch%` | The pouch display name. |
| `%pouch_id%` | The pouch ID. |
| `%pouch_rarity%` | The pouch rarity. |
| `%reward%` | The reward display name. Finish effects only. |
| `%reward_id%` | The reward ID. Finish effects only. |

:::tip Troubleshooting
- **Pouch not loading?** Check the file name is lowercase letters, numbers, and underscores only, and that it isn't prefixed with `_`.
- **Right-clicking does nothing?** Check the player has `ecocrates.pouch.<id>` (given by default) and isn't already opening something.
- **Wrong animation?** Check the console after `/ecocrates reload`; a non-GUI roll logs a warning and falls back to `csgo`.
:::

<hr/>

## Where to go next

- **Rewards:** define what pouches give in [How to Make a Reward](how-to-make-a-reward).
- **Rolls:** see which GUI rolls pouches can use on [Animations / Rolls](roll-animations).
- **Commands:** give, take, and preview pouches with [Commands and Permissions](commands-and-permissions).
- **Defaults:** browse the shipped example configs [here](https://github.com/Auxilor/EcoCrates/tree/master/eco-core/core-plugin/src/main/resources/pouches).
