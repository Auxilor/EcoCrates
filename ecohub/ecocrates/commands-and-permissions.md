---
title: "Commands and Permissions"
sidebar_position: 8
---

This page lists every EcoCrates command and permission, so you can hand out keys, place crates, and control who can open what. All commands run under `/ecocrates`, `/crates`, `/crate`, `/key`, or `/keys`.

:::info
Every `/ecocrates envoy <sub>` command below is also available as `/ecoenvoy <sub>`.
:::

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/ecocrates` | Base command | `ecocrates.command.ecocrates` |
| `/ecocrates reload` | Reloads the plugin | `ecocrates.command.reload` |
| `/ecocrates set <crate>` | Set the block you're looking at to be a crate | `ecocrates.command.set` |
| `/ecocrates give <player> <crate> [physical/virtual] [amount]` | Gives a player keys (physical item or virtual keys) | `ecocrates.command.give` |
| `/ecocrates give <player> <envoy> <flare\|compass> [amount]` | Gives a player an envoy's flare or compass | `ecocrates.command.give` |
| `/ecocrates giveall <crate> [physical/virtual] [amount]` | Give all online players keys | `ecocrates.command.giveall` |
| `/ecocrates giveall <envoy> <flare\|compass> [amount]` | Gives all online players an envoy's flare or compass | `ecocrates.command.giveall` |
| `/ecocrates giveoffline <crate> [physical/virtual] [amount]` | Give all online and offline players keys | `ecocrates.command.giveoffline` |
| `/ecocrates giveoffline <envoy> <flare\|compass> [amount]` | Gives all online and offline players an envoy's flare or compass | `ecocrates.command.giveoffline` |
| `/ecocrates take <player> <crate> [physical/virtual] [amount]` | Takes keys from an online player | `ecocrates.command.take` |
| `/ecocrates take <player> <envoy> <flare\|compass> [amount]` | Takes an envoy's flare or compass from an online player | `ecocrates.command.take` |
| `/ecocrates keys` | View your keys | `ecocrates.command.keys` |
| `/ecocrates preview <crate>` | Open the preview for a crate | `ecocrates.command.preview` |
| `/ecocrates open <crate> [player]` | Opens a crate using virtual keys | `ecocrates.command.open` |
| `/ecocrates open <crate> <player>` | Opens a crate for another player | `ecocrates.command.open.others` |
| `/ecocrates forceopen <crate>` | Force-opens a crate without a key | `ecocrates.command.forceopen` |
| `/ecocrates forceopen <crate> <player>` | Force-opens a crate for another player | `ecocrates.command.forceopen.others` |
| `/ecocrates resetwins <player/all>` | Resets tracked reward wins | `ecocrates.command.resetwins` |
| `/ecocrates convert <converter>` | Converts data from a supported crate plugin | `ecocrates.command.convert` |
| `/ecocrates envoy set <envoy> <current\|x> [y] [z]` | Marks a spawn point for a points-mode envoy | `ecocrates.command.envoy.set` |
| `/ecocrates envoy unset <envoy> <point#>` | Removes a spawn point | `ecocrates.command.envoy.set` |
| `/ecocrates envoy locate` | Lists remaining envoy crates, click to teleport | `ecocrates.command.envoy.locate` |
| `/ecocrates envoy start <envoy>` | Manually starts an envoy | `ecocrates.command.envoy.start` |
| `/ecocrates envoy end` | Ends the active envoy | `ecocrates.command.envoy.end` |

For `give`/`giveall`/`giveoffline`/`take`, the type argument decides how the ID before it is read: `flare`/`compass` makes it an envoy ID, `physical`/`virtual`/anything else makes it a crate ID, so a crate and an envoy may safely share an ID. Envoy items are physical-only, so offline players receive them on next join.

## Additional permissions

| Permission | Description |
| --- | --- |
| `ecocrates.open.<crate>` | Permission to open a specific crate |
| `ecocrates.open.*` | Permission to open all crates |
| `ecocrates.reroll.<crate>` | Permission to reroll the crate reward (if enabled). Given by default; negate to prevent rerolls |
| `ecocrates.reroll.*` | Permission to reroll all crates |
| `ecocrates.rewards.<reward>` | Permission to be eligible for a specific reward (use `ecocrates.rewards.*` for all rewards) |
| `ecocrates.command.envoy` | Allows the use of /ecocrates envoy (and /ecoenvoy) |
| `ecocrates.craft.flare` | Permission to craft envoy start flares (if the envoy's flare recipe sets one). Given by default |
| `ecocrates.craft.compass` | Permission to craft envoy compasses (if the envoy's compass recipe sets one). Given by default |

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
- **Envoys:** configure flares, compasses, and rarities in [How to Make an Envoy](how-to-make-an-envoy).