---
title: "Animations / Rolls"
sidebar_position: 4
---

A roll is the animation a player watches while a crate decides what they win. Each crate picks one roll by ID, and the rolls themselves are tuned globally. By the end of this page you'll know which rolls exist and how rerolls fit in.

## What are rolls?

A roll is the animation that plays when a crate opens, before the reward is revealed. You choose one per crate with the `roll` option in the crate config, and you tune each roll's behaviour in the `rolls` section of [Plugin Config](plugin-config).

| Roll ID | What it does | Video |
| --- | --- | --- |
| `csgo` | Scrolls a row of items in a GUI, easing to a stop on the winner | [Video](https://youtu.be/IGwYEmMBGk8) |
| `slot_machine` | Spins reels in a GUI, stopping them one at a time | - |
| `elimination` | Knocks candidates out of a GUI one by one until the winner is the last left | - |
| `pick` | Face-down boxes in a GUI; the player clicks one to open it | - |
| `choose` | Every reward the player can win is shown in a GUI; they click the one they want | - |
| `match` | A scratchcard: the player scratches cards until enough of them match | - |
| `encircle` | Rings the player with items that spin, then reveal the winner | [Video](https://youtu.be/EhLiTVnQ6zs) |
| `flash` | Blinds the player while an item flies toward their face | [Video](https://youtu.be/J9S5HKUBFwA) |
| `orbit_collapse` | Orbits items around the player, then collapses them into the winner | - |
| `vortex` | Spirals items inwards and upwards, flinging out a loser on every pass | - |
| `roulette` | A glowing cursor runs a ring of items and slows to a stop on the winner | - |
| `cycle` | Floats an item at the crate, swapping it every few ticks | - |
| `hologram` | No items at all: a hologram cycles reward names and locks onto the winner | - |
| `sky_drop` | Throws items out of the crate and drops the winner from above | - |
| `strike` | The item rises, hangs, and a lightning strike reveals the winner | - |
| `delivery` | An entity carries the reward from the crate to the player | - |
| `quick` | Rises a single item out of the crate | [Video](https://youtu.be/_gaMLZ_QM6E) |
| `instant` | No animation, the reward is given straight away | [Video](https://youtu.be/U3TNbZMrju4) |
| `semi_instant` | Throws the item out of the crate for a moment | [Video](https://youtu.be/ecsIdOLwSnU) |

## Hiding the placed crate

Rolls that play out in the world can sit on top of a placed crate's hologram and preview item. Each of those rolls has a `hide-placed-crate` option in `config.yml`:

```yaml
rolls:
  cycle:
    hide-placed-crate: true
```

When enabled, the hologram and floating preview item are hidden from the player opening the crate for as long as the roll runs, then restored when it finishes. Only that player is affected - everyone else still sees the crate as normal.

| Roll | Default |
| --- | --- |
| `cycle` | `true` |
| `hologram` | `true` |
| `flash` | `false` |
| `encircle` | `false` |
| `vortex` | `false` |
| `roulette` | `false` |
| `sky_drop` | `false` |
| `orbit_collapse` | `false` |
| `strike` | `false` |
| `delivery` | `false` |
| `quick` | `false` |
| `semi_instant` | `false` |

The two that default to `true` are the ones that take over the crate's own space: `cycle` swaps the preview item for a cycling one, and `hologram` replaces the crate's hologram with its own. The rest leave the crate alone unless you turn the option on.

`csgo`, `slot_machine`, `elimination`, `pick`, `choose`, and `instant` have no `hide-placed-crate` option, because they play out in a GUI or don't animate at all - the crate's hologram never gets in their way. Adding the option to their config section does nothing.

The option is also inert when there's no placed crate to hide, such as opening with `/ecocrates open` or a virtual key.

## The scratchcard

`match` deals a card of face-down rewards. The player scratches them off one at a time, and the first reward they turn up `to-match` copies of is what they win.

There's no card size to set. A losing reward is dealt at most `to-match - 1` copies, so it can tease a set the player can't finish but never become a second winner, and the card is then dealt as large as the crate's own rewards can fill, up to 21. With `to-match: 2` every loser is unique, since a repeat would be a winning pair, so the card is as big as the crate has distinct rewards. Raising `to-match` allows duplicates and so deals a bigger card: a crate with six rewards deals seven cards at `to-match: 2`, and thirteen at `to-match: 3`.

The winner is decided before the roll starts, as it is for every roll except `choose`, so the card is dealt around it. What the player finds is genuinely on the card, though: the reveal at the end flips the rest face-up and those near misses are the cards they didn't scratch, not invented afterwards.

`min-scratches` stops an anticlimactic instant win. If the set would complete too early, the winner is quietly moved under a card that hasn't been scratched yet - unscratched cards give nothing away, and past the floor the card behaves exactly as dealt. If the player stops scratching, `auto-scratch` scratches for them so the roll always finishes.

## Choosing your own reward

`choose` is the one roll where the winner isn't decided beforehand. It opens a GUI listing every reward the player is currently eligible to win (the same eligibility every other roll's weighted pick respects - permission-gated rewards that a player can't win never appear), paginated if there are more than fit on one page. Whichever one they click is what they win.

If the player doesn't click anything, `auto-pick` ticks pass and a random eligible reward is chosen for them so the roll can't hang open forever. `reveal-time` is how long the GUI stays open showing the pick before the crate finishes.

## Rerolls

A reroll lets a player swap a won reward for a fresh roll. After winning, the player is offered the choice to accept the reward or reroll. You can watch a video on rerolls [here](https://youtu.be/giDXQMwRsPU).

Rerolls are configured per crate with the `rerolls:` block:

- `enabled` — whether rerolling is offered.
- `max-rerolls` — how many rerolls in a row before the player is forced to accept. The count resets once a reward is accepted (or forced).
- `price` — the cost of each reroll, using the standard price system (currencies or items). The `%reroll%` placeholder (1-based) lets the price scale per reroll, e.g. `value: "%reroll%*2"` charges 2, then 4, then 6. If the player can't afford the next reroll, the reward is given automatically.

```yaml
rerolls:
  enabled: true
  max-rerolls: 3
  price:
    type: emerald
    value: "%reroll%*2"
    display: "&e%value% &7Emeralds"
```

By default every player can reroll where it's enabled. To stop certain groups or players, negate the `ecocrates.reroll.<crate>` permission. The reroll GUI itself is configured in [Plugin Config](plugin-config).

<hr/>

## Where to go next

- **Use a roll:** set the `roll` option in [How to Make a Crate](how-to-make-a-crate).
- **Tune a roll:** adjust each animation in [Plugin Config](plugin-config).
- **Permissions:** control who can reroll in [Commands and Permissions](commands-and-permissions).