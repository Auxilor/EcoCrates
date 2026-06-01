---
title: "Commands and Permissions"
sidebar_position: 6
---

This page lists every EcoCrates command and permission, so you can hand out keys, place crates, and control who can open what. All commands run under `/ecocrates`, `/crates`, `/crate`, `/key`, or `/keys`.

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/ecocrates` | Base command | `ecocrates.command.ecocrates` |
| `/ecocrates reload` | Reloads the plugin | `ecocrates.command.reload` |
| `/ecocrates set <crate>` | Set the block you're looking at to be a crate | `ecocrates.command.set` |
| `/ecocrates give <player> <crate> [physical/virtual] [amount]` | Gives a player keys (physical item or virtual keys) | `ecocrates.command.give` |
| `/ecocrates giveall <crate> [physical/virtual] [amount]` | Give all online players keys | `ecocrates.command.giveall` |
| `/ecocrates giveoffline <crate> [physical/virtual] [amount]` | Give all online and offline players keys | `ecocrates.command.giveoffline` |
| `/ecocrates take <player> <crate> [physical/virtual] [amount]` | Takes keys from an online player | `ecocrates.command.take` |
| `/ecocrates keys` | View your keys | `ecocrates.command.keys` |
| `/ecocrates preview <crate>` | Open the preview for a crate | `ecocrates.command.preview` |
| `/ecocrates open <crate> [player]` | Opens a crate using virtual keys | `ecocrates.command.open` |
| `/ecocrates open <crate> <player>` | Opens a crate for another player | `ecocrates.command.open.others` |
| `/ecocrates forceopen <crate>` | Force-opens a crate without a key | `ecocrates.command.forceopen` |
| `/ecocrates forceopen <crate> <player>` | Force-opens a crate for another player | `ecocrates.command.forceopen.others` |
| `/ecocrates resetwins <player/all>` | Resets tracked reward wins | `ecocrates.command.resetwins` |
| `/ecocrates convert <converter>` | Converts data from a supported crate plugin | `ecocrates.command.convert` |

## Additional permissions

| Permission | Description |
| --- | --- |
| `ecocrates.open.<crate>` | Permission to open a specific crate |
| `ecocrates.open.*` | Permission to open all crates |
| `ecocrates.reroll.<crate>` | Permission to reroll the crate reward (if enabled). Given by default; negate to prevent rerolls |
| `ecocrates.reroll.*` | Permission to reroll all crates |
| `ecocrates.rewards.<reward>` | Permission to be eligible for a specific reward (use `ecocrates.rewards.*` for all rewards) |

## Chance multiplier permissions

You can create permissions that give players a chance multiplier for rewards, in `config.yml`.

```yaml
permission-multipliers:
  - permission: ecocrates.multiplier.vip # The permission node
    multiplier: 1.5 # The chance multiplier applied to eligible rewards
    priority: 1 # Higher priority wins when a player has several, e.g. 2 beats 1
```

:::info
A player only ever gets one multiplier: the highest-priority node they have permission for. So if a player has both the vip and mvp nodes, only the higher-priority one applies.
:::

<hr/>

## Where to go next

- **Keys:** hand out and configure keys in [How to Make a Key](how-to-make-a-key).
- **Multipliers:** the full multiplier config lives in [Plugin Config](plugin-config).
- **Rerolls:** see how rerolls work on [Animations / Rolls](roll-animations).