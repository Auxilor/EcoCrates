---
title: "Animations / Rolls"
sidebar_position: 4
---

A roll is the animation a player watches while a crate decides what they win. Each crate picks one roll by ID, and the rolls themselves are tuned globally. By the end of this page you'll know which rolls exist and how rerolls fit in.

## What are rolls?

A roll is the animation that plays when a crate opens, before the reward is revealed. You choose one per crate with the `roll` option in the crate config, and you tune each roll's behaviour in the `rolls` section of [Plugin Config](plugin-config).

| Roll ID | Video |
| --- | --- |
| `csgo` | [Video](https://youtu.be/IGwYEmMBGk8) |
| `encircle` | [Video](https://youtu.be/EhLiTVnQ6zs) |
| `flash` | [Video](https://youtu.be/J9S5HKUBFwA) |
| `quick` | [Video](https://youtu.be/_gaMLZ_QM6E) |
| `instant` | [Video](https://youtu.be/U3TNbZMrju4) |
| `semi_instant` | [Video](https://youtu.be/ecsIdOLwSnU) |

## Rerolls

A reroll lets a player swap a won reward for a fresh roll, once. When a crate has `can-reroll: true`, the player is offered a reroll after winning, and can either accept the reward or try again. You can watch a video on rerolls [here](https://youtu.be/giDXQMwRsPU).

By default every player can reroll where it's enabled. To stop certain groups or players, negate the `ecocrates.reroll.<crate>` permission. The reroll GUI itself is configured in [Plugin Config](plugin-config).

<hr/>

## Where to go next

- **Use a roll:** set the `roll` option in [How to Make a Crate](how-to-make-a-crate).
- **Tune a roll:** adjust each animation in [Plugin Config](plugin-config).
- **Permissions:** control who can reroll in [Commands and Permissions](commands-and-permissions).