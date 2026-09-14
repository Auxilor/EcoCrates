---
title: "How to Make a Pouch"
sidebar_position: 6
---

A pouch is a single-use item. Right-clicking it uses up one pouch, plays a GUI roll, and gives exactly one reward. Pouches need no keys and have no rerolls, which makes them ideal for loot drops, shop items, and quest rewards. This page walks you through making one from an empty file to a working item.

## Quick start

1. Open the `/pouches/` folder inside the EcoCrates plugin folder.
2. Copy `_example.yml` and rename it to your pouch's ID, e.g. `mining.yml`.
3. Set the `name`, `rarity`, `roll`, and the `item` players will hold.
4. List the reward IDs under `rewards`, and lay them out under `preview`.
5. Run `/ecocrates reload`, then `/ecocrates give <player> pouch mining` to test it.
6. Right-click the pouch to open it, or shift + right-click to preview it.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real pouch. You can organise pouches into subfolders inside `pouches/` and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the pouch's ID. This is what you use in commands, effects, filters, and placeholders.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the pouch will not load.
:::

## Basics

```yaml
name: "&aMining Pouch" # The display name, used as %pouch% in effects
rarity: "&aCommon" # Display only, used as %pouch_rarity% and by the pouch_rarity filter
roll: csgo # The GUI roll to play
preview-on-shift: true # Shift + right-click opens the preview instead of the pouch
```

:::warning GUI rolls only
Pouches open from the player's hand, not at a place in the world, so only GUI rolls are supported: `csgo`, `slot_machine`, `elimination`, `pick`, `choose`, `match`, and `instant`. Any other roll falls back to `csgo` and logs a warning on reload. See [Animations / Rolls](roll-animations).
:::

## The item

```yaml
item:
  item: leather name:"&aMining Pouch" # Any item lookup
  lore:
    - "&fRight click to open"
    - "&fShift + right click to preview"
  crafting: # Remove this block to make the pouch uncraftable
    enabled: true
    permission: ecocrates.craft.pouch.mining # Remove to let everyone craft it
    shapeless: false
    recipe:
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

Every pouch is registered as a custom item called `ecocrates:pouch_<id>`, so you can sell it in shops, drop it from mobs, or give it with any plugin that supports item lookups, e.g. `ecocrates:pouch_mining`.

:::info
Pouches can never be placed as blocks, even if the item is a block.
:::

## Price

```yaml
price: # Optional. Remove this block and the pouch item is the only cost.
  type: coins
  value: 500
  display: "&e%value% coins"
```

The price is checked before anything is taken. A player who can't afford it keeps their pouch.

## Open conditions

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

Every condition must be met to open the pouch. They're checked before the price is charged or the item is used up, so a failed condition costs nothing. Use `not-met-effects` to tell the player why.

## Preview

The `preview` block is identical to a crate's preview, with pages, masks, reward slots, custom slots, and page arrows. See the preview section of [How to Make a Crate](how-to-make-a-crate#preview). Players open it with shift + right-click (if `preview-on-shift` is on) or `/ecocrates preview pouch <id>`.

## Effects

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

`open-effects` run once the pouch has been paid for and used up. `finish-effects` run when the roll lands on a reward.

| Placeholder | Value |
| --- | --- |
| `%player%` | The player's name |
| `%pouch%` | The pouch display name |
| `%pouch_id%` | The pouch ID |
| `%pouch_rarity%` | The pouch rarity |
| `%reward%` | The reward display name (finish-effects only) |
| `%reward_id%` | The reward ID (finish-effects only) |

## Rewards

```yaml
rewards:
  - diamond_sword
  - stack_of_emeralds
  - 1000_coins
```

Rewards work exactly like they do for crates: weights, win caps, permission multipliers, and `win-effects` all apply. See [How to Make a Reward](how-to-make-a-reward).

## Triggers, filters, and effects

Use these in any libreforge config, e.g. EcoItems, EcoSkills, or EcoCrates' own effect blocks.

| Type | ID | Description |
| --- | --- | --- |
| Trigger | `pouch_open` | Fires when a player opens a pouch |
| Trigger | `pouch_win` | Fires when a player wins a reward from a pouch. The value is the percentage chance of that reward |
| Filter | `pouch` | Matches a list of pouch IDs |
| Filter | `pouch_rarity` | Matches a list of rarities, with colour codes ignored (e.g. `Common`) |
| Filter | `crate_reward` | Matches a list of reward IDs. Works for crates and pouches |
| Effect | `give_pouch` | Gives pouch items. Args: `id`, `amount` |

```yaml
effects:
  - id: give_pouch
    args:
      id: mining
      amount: 1
    triggers:
      - mine_block
    filters:
      blocks:
        - diamond_ore
```

:::danger Effects are their own system
Effects, conditions, and filters are a shared system documented outside this plugin:

- [Configuring an Effect](https://hub.auxilor.io/wiki/libreforge/configuring-an-effect)
- [Configuring an Effect Chain](https://hub.auxilor.io/wiki/libreforge/configuring-a-chain)
:::

:::tip Troubleshooting
- **Right-clicking does nothing?** Check the player has `ecocrates.pouch.<id>` (given by default) and isn't already opening something.
- **Wrong animation?** Check the console after `/ecocrates reload`. A non-GUI roll logs a warning and falls back to `csgo`.
- **Player left mid-roll?** They get the reward the next time they join.
:::

<hr/>

## Where to go next

- **Rewards:** define what pouches give in [How to Make a Reward](how-to-make-a-reward).
- **Rolls:** see which GUI rolls pouches can use on [Animations / Rolls](roll-animations).
- **Commands:** give, take, and preview pouches with the commands on [Commands and Permissions](commands-and-permissions).
- **Defaults:** browse the shipped example configs [here](https://github.com/Auxilor/EcoCrates/tree/master/eco-core/core-plugin/src/main/resources/pouches).
