---
title: "How to Make a Key"
sidebar_position: 2
---

Keys control access to crates. A key defines the **physical item** players hold, an optional **crafting recipe**, and how it shows up in the **keys GUI**. This page covers making one from scratch.

## Quick start

1. Open the `/keys/` folder inside the EcoCrates plugin folder.
2. Copy `_example.yml` and rename it to your key's ID, e.g. `demo_crate.yml`.
3. Set the `item` and `lore` for the physical key.
4. Point a crate at this key by setting `key: <id>` in that crate's config.
5. Run `/ecocrates reload`, then `/ecocrates give <player> <crate> physical` to hand the key out.
6. Hold the key and right-click your placed crate to confirm it opens.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real key. You can also organise keys into subfolders inside `keys/`, and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the key's ID. This is what crates reference with their `key` option. The `item` and recipe ingredients use the [Item Lookup System](https://plugins.auxilor.io/the-item-lookup-system) format, so you can use vanilla items, custom items, and modifiers.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the key will not load.
:::

## The structure of a key

| Part | What it controls |
| --- | --- |
| **Item** | The physical key item and its lore |
| **Crafting** | An optional recipe to craft the key |
| **Keys GUI** | How the key appears in the `/ecocrates keys` GUI |

```yaml
# === Item: the physical key ===
item: tripwire_hook unbreaking:1 hide_enchants name:"&aDemo Key" # The physical key item
lore: # Lore applied to the physical key
  - "&fUse this key to open"
  - "&fdemonstration crates"
use-custom-item: false # If true, use an existing custom item as the key; lore is NOT applied

# === Crafting: optional recipe ===
craftable: false # If this key can be crafted
recipe-permission: "" # Optional; permission needed to use the recipe
shapeless: false # Optional; if true, slot positions don't matter, defaults to false
recipe: # The 3x3 recipe ingredients, use "" for empty slots
  - "iron_ingot"
  - "iron_ingot"
  - "iron_ingot"
  - "iron_ingot"
  - "tripwire_hook"
  - "iron_ingot"
  - "iron_ingot"
  - "iron_ingot"
  - "iron_ingot"

# === Keys GUI: how it shows in /ecocrates keys ===
keygui:
  enabled: true # If this key appears in the GUI
  crate: demo_crate # The crate to open/preview on click; defaults to the key ID
  item: tripwire_hook unbreaking:1 hide_enchants name:"Demo Key" # The GUI display item
  lore:
    - "<g:#56ab2f>Demo Key</g:#a8e063>"
    - "&fYou have %keys% keys"
    - "&fGet more at &astore.example.net"
  row: 2 # Row in the GUI, 1-indexed
  column: 3 # Column in the GUI, 1-indexed
  page: 1 # Page in the GUI, 1-indexed
  right-click-previews: true # Right-click opens the crate preview
  left-click-opens: true # Left-click opens the crate with a virtual key
  shift-left-click-message: # Message sent on shift-left-click
    - "Buy a key here! &astore.example.net"
  custom-slots: [] # GUI slots for extra buttons
```

### Item

The physical key players carry, with its lore.

```yaml
item: tripwire_hook unbreaking:1 hide_enchants name:"&aDemo Key" # The physical key item
lore: # Lore applied to the physical key
  - "&fUse this key to open"
  - "&fdemonstration crates"
use-custom-item: false # If true, use an existing custom item as the key; lore is NOT applied
```

:::info
Set `use-custom-item: true` to use an item from another eco plugin (e.g. an EcoItems item) as the key. When enabled, the `lore` above is not applied, since the custom item carries its own.
:::

### Crafting

Optionally lets players craft the key with a recipe. Ingredients use the same item format as the rest of the plugin.

```yaml
craftable: false # If this key can be crafted
recipe-permission: "" # Optional; permission needed to use the recipe
shapeless: false # Optional; if true, slot positions don't matter, defaults to false
recipe: # The 3x3 recipe ingredients, use "" for empty slots
  - "iron_ingot"
  - "iron_ingot"
  - "iron_ingot"
  - "iron_ingot"
  - "tripwire_hook"
  - "iron_ingot"
  - "iron_ingot"
  - "iron_ingot"
  - "iron_ingot"
```

:::tip
Recipes support both shaped and shapeless layouts; set `shapeless: true` when slot positions don't matter. See [Recipes](https://plugins.auxilor.io/the-item-lookup-system/recipes).
:::

### Keys GUI

How the key appears in the `/ecocrates keys` GUI, including its slot and click behaviour.

```yaml
keygui:
  enabled: true # If this key appears in the GUI
  crate: demo_crate # The crate to open/preview on click; defaults to the key ID
  row: 2 # Row in the GUI, 1-indexed
  column: 3 # Column in the GUI, 1-indexed
  page: 1 # Page in the GUI, 1-indexed
  right-click-previews: true # Right-click opens the crate preview
  left-click-opens: true # Left-click opens the crate with a virtual key
  custom-slots: [] # GUI slots for extra buttons
```

## Internal placeholders

| Placeholder | Value |
| --- | --- |
| `%keys%` | The player's amount of this key type. |

:::tip Troubleshooting
- **Key not loading?** Check the file name is lowercase letters, numbers, and underscores only, and that it isn't prefixed with `_`.
- **Key won't open the crate?** Confirm the crate's `key` option matches this key's ID.
- **Lore missing on the item?** You have `use-custom-item: true`; lore is taken from the custom item, not this file.
:::

<hr/>

## Where to go next

- **Crates:** wire this key to a crate in [How to Make a Crate](how-to-make-a-crate).
- **Commands:** give and take keys with [Commands and Permissions](commands-and-permissions).
- **Defaults:** browse the shipped example configs [here](https://github.com/Auxilor/EcoCrates/tree/master/eco-core/core-plugin/src/main/resources/keys).