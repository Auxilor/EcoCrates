---
title: "Commands and Permissions"
sidebar_position: 6
---

## General Information
All commands can be run with `/ecocrates`, `/crates`, `/crate`, `/key`, or `/keys`.

| Command                                                        | Description                                         | Permission                           |
|----------------------------------------------------------------|-----------------------------------------------------|--------------------------------------|
| `/ecocrates`                                                   | Base command                                        | `ecocrates.command.ecocrates`        |
| `/ecocrates reload`                                            | Reloads the plugin                                  | `ecocrates.command.reload`           |
| `/ecocrates set <crate>`                                       | Set the block you're looking at to be a crate       | `ecocrates.command.set`              |
| `/ecocrates give <player> <crate> [physical/virtual] [amount]` | Gives a player keys (physical item or virtual keys) | `ecocrates.command.give`             |
| `/ecocrates giveall <crate> [physical/virtual] [amount]`       | Give all online players keys                        | `ecocrates.command.giveall`          |
| `/ecocrates giveoffline <crate> [physical/virtual] [amount]`   | Give all online and offline players keys            | `ecocrates.command.giveoffline`      |
| `/ecocrates take <player> <crate> [physical/virtual] [amount]` | Takes keys from an online player                    | `ecocrates.command.take`             |
| `/ecocrates keys`                                              | View your keys                                      | `ecocrates.command.keys`             |
| `/ecocrates preview <crate>`                                   | Open the preview for a crate                        | `ecocrates.command.preview`          |
| `/ecocrates open <crate> [player]`                             | Opens a crate using virtual keys                    | `ecocrates.command.open`             |
| `/ecocrates open <crate> <player>`                             | Opens a crate for another player                    | `ecocrates.command.open.others`      |
| `/ecocrates forceopen <crate>`                                 | Force-opens a crate without a key                   | `ecocrates.command.forceopen`        |
| `/ecocrates forceopen <crate> <player>`                        | Force-opens a crate for another player              | `ecocrates.command.forceopen.others` |
| `/ecocrates resetwins <player/all>`                                | Resets tracked reward wins                          | `ecocrates.command.resetwins`        |
| `/ecocrates convert <converter>`                               | Converts data from a supported crate plugin         | `ecocrates.command.convert`          |

### Additional Permissions

| Permission                   | Description                                                                                                  |
|------------------------------|--------------------------------------------------------------------------------------------------------------|
| `ecocrates.open.<crate>`     | Permission to open a specific crate                                                                          |
| `ecocrates.open.*`           | Permission to open all crates                                                                                |
| `ecocrates.reroll.<crate>`   | Permission to re-roll the crate reward (if enabled). Given by default, negate permission to prevent re-rolls |
| `ecocrates.reroll.*`         | Permission to re-roll all crates                                                                             |
| `ecocrates.rewards.<reward>` | Permission to be eligible for a specific reward (Use `ecocrates.rewards.*` for all rewards)                  |

#### Chance multiplier permissions

You can create permissions to give players a chance multiplier for specific rewards in `config.yml`

```yaml
permission-multipliers:
  - permission: ecocrates.multiplier.vip # The permission node
    multiplier: 1.5 # The chance multiplier
    priority: 1 # The priority of the multiplier, higher priority multipliers override lower ones, 2 > 1
```