---
title: "Animations / Rolls"
sidebar_position: 4
---

A roll is the animation a player watches while a crate decides what they win. Each crate picks one roll by ID, and the rolls themselves are tuned globally. By the end of this page you'll know which rolls exist and how rerolls fit in.

## What are rolls?

A roll is the animation that plays when a crate opens, before the reward is revealed. You choose one per crate with the `roll` option in the crate config, and you tune each roll's behaviour in the `rolls` section of [Plugin Config](plugin-config).

| Roll ID          | What it does                                                                    | Video                                 |
|------------------|---------------------------------------------------------------------------------|---------------------------------------|
| `csgo`           | Scrolls a row of items in a GUI, easing to a stop on the winner                 | [Video](https://youtu.be/n3QVL3NMbqg) |
| `slot_machine`   | Spins reels in a GUI, stopping them one at a time                               | [Video](https://youtu.be/kxz0QvmRGyU) |
| `elimination`    | Knocks candidates out of a GUI one by one until the winner is the last left     | [Video](https://youtu.be/bL4Bcaul_6E) |
| `pick`           | Face-down boxes in a GUI; the player clicks one to open it                      | [Video](https://youtu.be/WBqONQGTglE) |
| `choose`         | Every reward the player can win is shown in a GUI; they click the one they want | [Video](https://youtu.be/Ca8OWNfUPLw) |
| `match`          | A scratchcard: the player scratches cards until enough of them match            | [Video](https://youtu.be/Vr2qADzqeXw) |
| `encircle`       | Rings the player with items that spin, then reveal the winner                   | [Video](https://youtu.be/6n2EMUX3wHE) |
| `flash`          | Blinds the player while an item flies toward their face                         | [Video](https://youtu.be/WYMDjaAHO5s) |
| `orbit_collapse` | Orbits items around the player, then collapses them into the winner             | [Video](https://youtu.be/uJMeESoMiDs) |
| `vortex`         | Spirals items inwards and upwards, flinging out a loser on every pass           | [Video](https://youtu.be/WLcddKR-IS4) |
| `roulette`       | A glowing cursor runs a ring of items and slows to a stop on the winner         | [Video](https://youtu.be/4ncbKA7ouBw) |
| `cycle`          | Floats an item at the crate, swapping it every few ticks                        | [Video](https://youtu.be/p8Ggn_xFeVE) |
| `hologram`       | No items at all: a hologram cycles reward names and locks onto the winner       | [Video](https://youtu.be/kf7vls6UDzQ) |
| `sky_drop`       | Throws items out of the crate and drops the winner from above                   | [Video](https://youtu.be/VkAla0JRpL8) |
| `strike`         | The item rises, hangs, and a lightning strike reveals the winner                | [Video](https://youtu.be/T9VqkAwvdzg) |
| `delivery`       | An entity carries the reward from the crate to the player                       | [Video](https://youtu.be/fujbMo2m3DY) |
| `quick`          | Rises a single item out of the crate                                            | [Video](https://youtu.be/_nS1xGlkIak) |
| `instant`        | No animation, the reward is given straight away                                 | [Video](https://youtu.be/uncKTvuhGcQ) |
| `semi_instant`   | Throws the item out of the crate for a moment                                   | [Video](https://youtu.be/Eeo3srv38SM) |

:::info GUI rolls
`csgo`, `slot_machine`, `elimination`, `pick`, `choose`, `match` and `instant` play without needing a place in the world. They're the only rolls [pouches](how-to-make-a-pouch) can use.
:::

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
| `delivery` | `false` |
| `encircle` | `false` |
| `flash` | `false` |
| `hologram` | `true` |
| `orbit_collapse` | `false` |
| `quick` | `false` |
| `roulette` | `false` |
| `semi_instant` | `false` |
| `sky_drop` | `false` |
| `strike` | `false` |
| `vortex` | `false` |

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

With several rewards the player makes one choice per reward, the first reward plus each bonus reward. Bonus choices hide rewards that are `bonus-eligible: false`, and rewards already chosen `max-per-open` times. A bonus choice with nothing left to offer is dropped: it isn't given and isn't counted in `%reward_amount%`. The number of choices comes from `bonus-chances`, and any entry under `100` makes the number of choices random, so a crate that must be fully non-random uses only `100` entries.

If the player doesn't click anything, `auto-pick` ticks pass and a random eligible reward is chosen for them so the roll can't hang open forever. `reveal-time` is how long the GUI stays open showing the pick before the crate finishes.

## Multiple rewards

A crate or pouch with `bonus-chances` can give several rewards per open; see [How to Make a Crate](how-to-make-a-crate#bonus-rewards). Every reward in an open is independent: it is picked, rerolled, counted and runs its effects on its own. Every built-in roll shows several rewards natively, up to its **capacity**. Every pick is decided when the open starts, so the number of rewards never changes during the animation. The one exception is `choose`: a bonus choice with nothing left to offer is dropped, so that open gives one reward fewer.

| Roll | With several rewards | Capacity |
| --- | --- | --- |
| `instant` | Every reward is given at once. | Unlimited |
| `semi_instant` | One item is thrown out per reward. | Unlimited |
| `delivery` | One courier carries every item and hands them over one after another. | Unlimited |
| `quick` | The items rise side by side around the crate. | 5 |
| `flash`, `cycle`, `strike` | One item per reward side by side, each cycling. They lock on in turn, left to right. | 5 |
| `sky_drop` | One launch per reward, landing in turn. | 5 |
| `hologram` | One block of lines per reward, locking in turn. | 5 |
| `encircle`, `orbit_collapse`, `vortex` | The roll ends with the last few items left, and each one is a winner. | `items` - 1 |
| `roulette` | The cursor stops once per reward, and each stop stays lit. | `items` - 1 |
| `csgo` | One scrolling lane per reward, stacked. The lanes stop in turn. | 4 |
| `slot_machine` | One payline per reward. | 3 |
| `elimination` | Candidates are knocked out until as many are left as there are rewards. | `candidates` - 1 |
| `pick` | The player opens one box per reward. The number of boxes is raised to at least one more than the rewards, up to 7. | 6 |
| `match` | The card holds one set per reward and grows to fit. The player scratches until every set is found. | 28 divided by `to-match` |
| `choose` | The player makes one choice per reward. See [Choosing your own reward](#choosing-your-own-reward). | Unlimited |

`items`, `candidates` and `to-match` are the settings of that roll in [Plugin Config](plugin-config), so changing them changes the capacity. A roll that needs room for the winners, such as `encircle` or `pick`, raises its item or box count automatically when there are more rewards.

If an open has more rewards than the roll's capacity, they play in **batches**: the roll plays once for the first batch of up to its capacity, then again for the next, in order, until every reward has been shown.

`pick` and `match` give the rewards the crate decided in advance, the same as with one reward. The player's clicks only decide where each reward is revealed.

:::tip Hologram roll
The `rolling` and `winner` lines of the `hologram` roll repeat once for every reward, each block locking in turn. In `winner` lines, `%rewards%` lists the names of every reward, so you can show the whole haul on one line. Because the whole block repeats, keep the lines short when a crate gives several rewards.

```yaml
winner:
  - "&a&lYou won!"
  - "&f%rewards%"
```
:::

### Rolls from other plugins

A roll registered by another plugin that doesn't support several rewards uses `multi-reward.fallback`, set in [Plugin Config](plugin-config) or per crate. `sequential` plays the roll once per reward, one after another. `primary` plays it for the first reward only and gives the bonus rewards straight after, with no animation.

## Rerolls

A reroll lets a player swap a won reward for a fresh roll. After winning, the player is offered the choice to accept the reward or reroll. You can watch a video on rerolls [here](https://youtu.be/giDXQMwRsPU).

Rerolls are configured per crate with the `rerolls:` block:

- `enabled`: whether rerolling is offered.
- `max-rerolls`: how many rerolls in a row before the player is forced to accept. The count resets once a reward is accepted (or forced).
- `price`: the cost of each reroll, using the standard price system (currencies or items). The `%reroll%` placeholder (1-based) lets the price scale per reroll, e.g. `value: "%reroll%*2"` charges 2, then 4, then 6. If the player can't afford the next reroll, the reward is given automatically.

```yaml
rerolls:
  enabled: true
  max-rerolls: 3
  price:
    type: emerald
    value: "%reroll%*2"
    display: "&e%value% &7Emeralds"
```

Each reward in an open is rerolled on its own. `max-rerolls` and the `%reroll%` price count per reward. A reward can be rerolled only if the crate's `rerolls` allow it and the reward isn't set to `rerollable: false`. Bonus rewards can also be switched off from rerolling with `bonus-rerollable`. Pouches never offer rerolls.

When a roll shows several rewards at once, a menu lists every reward after the animation. Click a reward to reroll only that one, and the menu reopens with its replacement. Accepting, or closing the menu, gives every reward. Rolls using the `sequential` fallback offer the normal reroll GUI after each reward instead, and the `primary` fallback only offers a reroll for the first reward. The menu is configured under `reroll.multi` in [Plugin Config](plugin-config).

A reroll replaces its reward in place, so it can't change how many rewards the open gives. Rerolling a `choose` reward means choosing it again.

By default every player can reroll where it's enabled. To stop certain groups or players, negate the `ecocrates.reroll.<crate>` permission. The reroll GUI itself is configured in [Plugin Config](plugin-config).

<hr/>

## Where to go next

- **Use a roll:** set the `roll` option in [How to Make a Crate](how-to-make-a-crate).
- **Tune a roll:** adjust each animation in [Plugin Config](plugin-config).
- **Permissions:** control who can reroll in [Commands and Permissions](commands-and-permissions).