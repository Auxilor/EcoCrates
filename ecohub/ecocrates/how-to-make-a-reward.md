---
title: "How to Make a Reward"
sidebar_position: 1
---

Rewards are what players win from crates. A reward defines the **effects** that fire when it's won, its **weight** (how likely it is), and its **display** in previews and roll animations. This page covers making one from scratch.

## Quick start

1. Open the `/rewards/` folder inside the EcoCrates plugin folder.
2. Copy `_example.yml` and rename it to your reward's ID, e.g. `diamond_sword.yml`.
3. Set the `win-effects` for what the player gets, and tune the `weight`.
4. Set the `display` item and lore that show in the preview GUI.
5. Add the reward's ID to a crate's `rewards` list, then run `/ecocrates reload`.
6. Force-open the crate with `/ecocrates forceopen <crate>` a few times to confirm the reward can be won.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real reward. You can also organise rewards into subfolders inside `rewards/`, and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the reward's ID. This is what you list in crate configs and preview layouts. The `display` item and any `give_item` effect use the [Item Lookup System](https://hub.auxilor.io/wiki/eco/the-item-lookup-system-the-item-lookup-system) format.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the reward will not load.
:::

## The structure of a reward

| Part | What it controls |
| --- | --- |
| **Basics** | The reward name and the effects that run on win |
| **Weight** | How likely the reward is and how many times it can be won |
| **Multiple rewards** | Optional. How the reward behaves when a crate gives several rewards per open |
| **Display** | The item shown in the preview GUI and roll animation |

```yaml
# === Basics: name and what the player wins ===
name: "&bDiamond Sword" # The name of the reward
win-effects: # The effects to run when the reward is won
  - id: give_item
    args:
      item: diamond_sword sharpness:5 unbreaking:3
  - id: send_message
    args:
      message: "&fYou won the %reward% &fin %crate%&f!"

# === Weight: chance and win limits ===
weight:
  permission-multipliers: true # If permission multipliers affect this reward's chance
  value: 1 # The chance weight; supports maths and placeholders, e.g. %player_y% * 25
max-wins: -1 # Max times any player can win this reward; -1 for unlimited
max-per-open: -1 # Optional. The most times this reward can be won in one open. -1 or missing means no limit.
bonus-eligible: true # Optional. If false, this reward can only be the first reward of an open, never a bonus reward.
rerollable: true # Optional. If false, winning this reward skips the reroll offer for that pick.

# === Display: how it looks in the preview ===
display:
  name: "&bDiamond Sword" # The item name
  item: diamond_sword sharpness:5 unbreaking:3 # The shown item
  dont-keep-lore: false # Optional; true shows only the custom lore below
  lore: # Supports %chance% and %weight% placeholders
    - "&fChance: &a%chance%%"
    - "&fWeight: &b%weight%"
```

### Basics

The reward's name and the effects that run the moment it is won.

```yaml
name: "&bDiamond Sword" # The name of the reward
win-effects: # The effects to run when the reward is won
  - id: give_item
    args:
      item: diamond_sword sharpness:5 unbreaking:3
  - id: send_message
    args:
      message: "&fYou won the %reward% &fin %crate%&f!"
```

:::danger Effects are their own system
Effects, conditions, and filters are a shared system documented outside this plugin. To configure them:

- [Configuring an Effect](https://hub.auxilor.io/wiki/libreforge/configuring-an-effect)
- [Configuring an Effect Chain](https://hub.auxilor.io/wiki/libreforge/configuring-a-chain)
:::

### Weight

How likely the reward is to be picked, and a cap on how many times it can be won.

```yaml
weight:
  permission-multipliers: true # If permission multipliers affect this reward's chance
  value: 1 # The chance weight; supports maths and placeholders, e.g. %player_y% * 25
max-wins: -1 # Max times any player can win this reward; -1 for unlimited
```

:::info
`value` is a weight relative to the other rewards in a crate, not a percentage. A reward with weight 2 is twice as likely as one with weight 1 in the same crate. The displayed `%chance%` is calculated from all the crate's weights.
:::

### Display

The item shown for this reward in the preview GUI and roll animation.

```yaml
display:
  name: "&bDiamond Sword" # The item name
  item: diamond_sword sharpness:5 unbreaking:3 # The shown item
  dont-keep-lore: false # Optional; true shows only the custom lore below
  lore: # Supports %chance% and %weight% placeholders
    - "&fChance: &a%chance%%"
    - "&fWeight: &b%weight%"
```

### Multiple rewards

These optional keys only matter for crates and pouches that give several rewards per open with `bonus-chances`; see [How to Make a Crate](how-to-make-a-crate#bonus-rewards). Each reward in an open is independent, so these keys apply to every pick on its own.

```yaml
max-per-open: -1 # Optional. The most times this reward can be won in one open. -1 or missing means no limit.
bonus-eligible: true # Optional. If false, this reward can only be the first reward of an open, never a bonus reward.
rerollable: true # Optional. If false, winning this reward skips the reroll offer for that pick.
```

:::info
Rewards are shared between crates, so these keys apply in every crate and pouch that lists the reward. `rerollable` also applies to crates that only give one reward. It has no effect on pouches, which never offer rerolls.
:::

## Internal placeholders

| Placeholder | Value |
| --- | --- |
| `%chance%` | Calculated chance of this reward in the crate. |
| `%weight%` | Raw weight value of this reward. |
| `%reward%` | The reward display name. |
| `%crate%` | The crate display name. |
| `%bonus_chance_<n>%` | The chance of the n-th `bonus-chances` entry of the crate, for the player viewing. Reward lore only. |
| `%max_rewards%` | The most rewards one open of the crate can give. Reward lore only. |

Win effects can also use `%reward_number%`, the 1-based number of the reward within the open, and `%reward_amount%`, how many rewards the open gives.

Reward lore also reads other plugin placeholders, for example `In existence: %ecocrates_<reward>_total%` to show how many times a reward has been won. See [PlaceholderAPI](placeholderapi).

:::tip Troubleshooting
- **Reward not loading?** Check the file name is lowercase letters, numbers, and underscores only, and that it isn't prefixed with `_`.
- **Reward never won?** Confirm its ID is in the crate's `rewards` list and its `weight.value` is above 0.
- **`%chance%` shows wrong?** Chance is relative to the other rewards in that crate, so adding or removing rewards shifts every chance.
:::

<hr/>

## Where to go next

- **Crates:** add this reward to a crate in [How to Make a Crate](how-to-make-a-crate).
- **Permissions:** gate rewards with `ecocrates.rewards.<reward>`, see [Commands and Permissions](commands-and-permissions).
- **Defaults:** browse the shipped example configs [here](https://github.com/Auxilor/EcoCrates/tree/master/eco-core/core-plugin/src/main/resources/rewards).