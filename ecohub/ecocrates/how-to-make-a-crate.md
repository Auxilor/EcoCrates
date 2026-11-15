---
title: "How to Make a Crate"
sidebar_position: 3
---

Crates are the core of EcoCrates: each one ties together a **key**, a **preview GUI**, a set of **rewards**, and the **effects** that fire when it opens. This page walks you through making a crate from an empty file to a working, openable crate.

## Quick start

1. Open the `/crates/` folder inside the EcoCrates plugin folder.
2. Copy `_example.yml` and rename it to your crate's ID, e.g. `demo_crate.yml`.
3. Set the `name`, pick a `roll` animation, and point `key` at a key ID from your `/keys/` folder.
4. List the reward IDs you want under `rewards`, and lay them out in the `preview` pages.
5. Run `/ecocrates reload`, then `/ecocrates set <crate>` while looking at a block to place it, or `/ecocrates give <player> <crate>` to hand out a key.
6. Open the crate in game and confirm the roll plays and a reward is given.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real crate. You can also organise crates into subfolders inside `crates/`, and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the crate's ID. This is what you use in commands, key configs, and effects. The first time you reference an item (for a reward display, mask, or arrow) it uses the [Item Lookup System](https://hub.auxilor.io/wiki/eco/the-item-lookup-system-the-item-lookup-system) format.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the crate will not load.
:::

## The structure of a crate

| Part | What it controls |
| --- | --- |
| **Basics** | The display name, roll animation, key, and reroll settings |
| **Preview** | The GUI players see when previewing the crate |
| **Pay to open** | Letting players pay currency instead of using a key |
| **Placed** | Holograms and particles for physically placed crates |
| **Effects** | What runs when the crate opens and finishes rolling |
| **Rewards** | Which rewards are in the pool |
| **Bonus rewards** | Optional. Extra rewards given on top of the first one |

```yaml
# === Basics: identity and behaviour ===
name: "Demo Crate" # The display name of the crate
roll: csgo # The opening animation; see the Animations / Rolls page for options
rerolls: # Reroll settings; see the Animations / Rolls page
  enabled: true
  max-rerolls: 3
  price:
    type: emerald
    value: "%reroll%*2" # %reroll% is the reroll number (1-based); scales the cost per reroll
    display: "&e%value% &7Emeralds"
key: demo_crate # The key ID this crate uses (configure in /keys/demo_crate.yml); crates can share a key

# === Preview: the GUI players browse before opening ===
preview:
  title: "Demo Crate (%page%/%max_page%)" # The title of the preview GUI. Supports %page% and %max_page% placeholders.
  rows: 6 # Number of rows, between 1 and 6
  hide-unobtainable: false # Hides rewards from players who cannot win them. Off by default.
  custom-slots: [] # GUI slots shared across all preview pages
  forwards-arrow: # Shown on every page except the last
    item: arrow name:"&fNext Page"
    row: 6
    column: 6
    locations: [ ] # Optional list of row/column slots; if set, the button shows in each of them instead of row/column
  backwards-arrow: # Shown on every page except the first
    item: arrow name:"&fPrevious Page"
    row: 6
    column: 4
    locations: [ ] # Optional list of row/column slots; if set, the button shows in each of them instead of row/column
  pages:
    - page: 1
      mask: # Decorative filler items
        items:
          - gray_stained_glass_pane
          - black_stained_glass_pane
        pattern:
          - "222222222"
          - "211111112"
          - "211011112"
          - "211110112"
          - "211111112"
          - "222222222"
      custom-slots: [] # GUI slots for this page only
      rewards:
        - id: diamond_sword # The reward ID to display here
          row: 3
          column: 4
        - id: stack_of_emeralds
          row: 4
          column: 6

# === Pay to open: optional currency alternative to keys ===
pay-to-open:
  enabled: false # If players can pay to open instead of using a key
  price: 5000 # The cost to open
  type: coins # The currency to charge
  display: "%value_commas% coins" # Optional. How %price% shows in the confirm GUI

# === Placed: holograms and particles for physically placed crates ===
placed:
  shift-right-click-open-all: true # Shift right-click instantly opens the whole stack of physical keys held, skipping the roll animation
  open-all-effects-per-key: false # If true, open/finish effects fire for every key in the stack. If false (default), they fire once for the whole batch.
  random-reward: # A floating item showing a possible reward
    enabled: true
    height: 1.5 # Height above the crate
    delay: 30 # Ticks between cycling to a new item
    name: "&fYou could win: %reward%" # Text above the item; %reward% is the reward name
  particles: # Particle effects around the crate, add as many as you want
    - particle: flame # A Bukkit particle name
      animation: spiral # spiral, double_spiral, circle, or twirl
  hologram: # Text hologram, requires a hologram plugin installed
    height: 1.5 # Height above the crate
    ticks: 200 # Total ticks to cycle all frames
    frames:
      - tick: 0 # The tick this frame starts showing
        lines:
          - "<g:#56ab2f>&lDEMO CRATE</g:#a8e063>"
          - "&b&lLeft Click to Preview"
          - "&a&lRight click to Open"
      - tick: 100
        lines:
          - "<g:#56ab2f>&lDEMO CRATE</g:#a8e063>"
          - "&a&lLeft Click to Preview"
          - "&b&lRight click to Open"

# === Open conditions: requirements checked before a key or money is taken ===
open-conditions: [ ]

# === Effects: what runs on open and on finish ===
open-effects:
  - id: broadcast
    args:
      message: "%player%&f is opening the %crate%&f!"
finish-effects:
  - id: broadcast
    args:
      message: "%player%&f won %reward%&f from the %crate%&f!"

# === Rewards: the pool of reward IDs ===
rewards:
  - diamond_sword
  - stack_of_emeralds
  - 1000_coins
```

### Basics

The identity of the crate: its name, the roll animation, the key it consumes, and its reroll settings.

```yaml
name: "Demo Crate" # The display name of the crate
roll: csgo # The opening animation; see the Animations / Rolls page for options
rerolls: # Reroll settings; see the Animations / Rolls page
  enabled: true
  max-rerolls: 3
  price:
    type: emerald
    value: "%reroll%*2" # %reroll% is the reroll number (1-based); scales the cost per reroll
    display: "&e%value% &7Emeralds"
key: demo_crate # The key ID this crate uses (configure in /keys/demo_crate.yml); crates can share a key
```

### Preview

The GUI players see when they preview the crate, laid out as one or more pages with a decorative mask and reward slots.

```yaml
preview:
  title: "Demo Crate (%page%/%max_page%)" # The title of the preview GUI. Supports %page% and %max_page% placeholders.
  rows: 6 # Number of rows, between 1 and 6
  hide-unobtainable: false # Hides rewards from players who cannot win them. Off by default.
  custom-slots: [] # GUI slots shared across all preview pages
  forwards-arrow: # Shown on every page except the last
    item: arrow name:"&fNext Page"
    row: 6
    column: 6
    locations: [ ] # Optional list of row/column slots; if set, the button shows in each of them instead of row/column
  backwards-arrow: # Shown on every page except the first
    item: arrow name:"&fPrevious Page"
    row: 6
    column: 4
    locations: [ ] # Optional list of row/column slots; if set, the button shows in each of them instead of row/column
  pages:
    - page: 1
      mask: # Decorative filler items
        items:
          - gray_stained_glass_pane
          - black_stained_glass_pane
      custom-slots: [] # GUI slots for this page only
      rewards:
        - id: diamond_sword # The reward ID to display here
          row: 3
          column: 4
```

:::info
With `hide-unobtainable: true`, a reward is hidden for each player who cannot win it, for example after reaching its `max-wins`, or when it is not in this `rewards` list. The page mask shows in its place. A very rare reward whose chance only rounds to 0.00% stays visible.
:::

:::tip
Add custom buttons to the preview with custom GUI slots; see [Custom GUI Slots](https://hub.auxilor.io/wiki/eco/pages).
:::

### Pay to open

Lets players spend currency to open a placed crate instead of consuming a key. Players confirm the payment in a GUI first; the GUI is set up, or turned off, under `pay-to-open-confirm` in [`config.yml`](plugin-config).

```yaml
pay-to-open:
  enabled: false # If players can pay to open instead of using a key
  price: 5000 # The cost to open
  type: coins # The currency to charge
  display: "%value_commas% coins" # Optional. How %price% shows in the confirm GUI
```

### Placed

Holograms and particles shown on a crate that has been physically placed in the world with `/ecocrates set`.

:::info Removing a placed crate
A placed crate is protected: normal breaking, explosions, pistons and fire leave it in place. To remove it, sneak and break it with the `ecocrates.break` permission. The block stays and stops being a crate.
:::

```yaml
placed:
  shift-right-click-open-all: true # Shift right-click instantly opens the whole stack of physical keys held, skipping the roll animation
  open-all-effects-per-key: false # If true, open/finish effects fire for every key in the stack. If false (default), they fire once for the whole batch.
  random-reward: # A floating item showing a possible reward
    enabled: true
    height: 1.5 # Height above the crate
    delay: 30 # Ticks between cycling to a new item
    name: "&fYou could win: %reward%" # Text above the item; %reward% is the reward name
  particles: # Particle effects around the crate, add as many as you want
    - particle: flame # A Bukkit particle name
      animation: spiral # spiral, double_spiral, circle, or twirl
  hologram: # Text hologram, requires a hologram plugin installed
    height: 1.5 # Height above the crate
    ticks: 200 # Total ticks to cycle all frames
    frames:
      - tick: 0 # The tick this frame starts showing
        lines:
          - "<g:#56ab2f>&lDEMO CRATE</g:#a8e063>"
          - "&b&lLeft Click to Preview"
          - "&a&lRight click to Open"
```

:::info
The text hologram only appears if a supported hologram plugin is installed. Particles and the random-reward item work without one.
:::

### Open conditions

Conditions that must all be met before the crate opens. They're checked before any key or money is taken. On a placed crate, a failed condition pushes the player back the same way a missing key does. `/ecocrates forceopen` and rerolls skip them.

```yaml
open-conditions:
  - id: has_permission
    args:
      permission: ecocrates.example.vip
    not-met-effects:
      - id: send_message
        args:
          message: "&cOnly VIPs can open this crate!"
```

### Effects

`open-effects` run the moment the crate is opened; `finish-effects` run when the roll lands on a reward.

```yaml
open-effects:
  - id: broadcast
    args:
      message: "%player%&f is opening the %crate%&f!"
finish-effects:
  - id: broadcast
    args:
      message: "%player%&f won %reward%&f from the %crate%&f!"
```

:::danger Effects are their own system
Effects, conditions, and filters are a shared system documented outside this plugin. To configure them:

- [Configuring an Effect](https://hub.auxilor.io/wiki/libreforge/configuring-an-effect)
- [Configuring an Effect Chain](https://hub.auxilor.io/wiki/libreforge/configuring-a-chain)
:::

### Rewards

The pool of reward IDs this crate can give. This is kept separate from the preview layout, so you can include secret rewards that never show in the GUI.

```yaml
rewards:
  - diamond_sword
  - stack_of_emeralds
  - 1000_coins
```

### Bonus rewards

By default a crate gives one reward per open. Add `bonus-chances` to also give a chance of extra bonus rewards. One reward is always given, and each entry in the list is the chance of one more.

```yaml
bonus-chances: # Optional. Each entry is the chance, as a percentage, of one more bonus reward.
  - 50
  - 20
  - 5
multi-reward: # Optional. Overrides the multi-reward section in config.yml for this crate only.
  fallback: sequential
  bonus-rerollable: true
  bonus-chance-mode: overall
all-finish-effects: [] # Optional. Runs once after every reward from the open is given.
```

A crate with no `bonus-chances` behaves exactly as it always has. The example above has three entries, so an open gives between 1 and 4 rewards. Entries are percentages from 0 to 100, are worked out for the player opening the crate, and accept maths and placeholders like a reward's `weight`, e.g. `"%vip_bonus% * 5"`. `100` always hits and `0` never does, so `[100, 100]` gives exactly 3 rewards.

:::info Every reward is independent
Each reward in an open is its own reward. It is picked on its own, using the normal reward weights. It can be rerolled on its own, counts as its own win, and runs its own `finish-effects` and the reward's own `win-effects`. A multi-reward open is the same as several ordinary opens that share one key (or payment) and one set of `open-effects`.
:::

#### Bonus chance modes

`bonus-chance-mode` decides how the entries combine into a number of bonus rewards. It defaults to `overall` and can be set for every crate in [Plugin Config](plugin-config), or for one crate in its `multi-reward` section. The table shows the chance of each bonus count for `[50, 20, 5]`.

| Mode | How it works | 0 bonus | 1 bonus | 2 bonus | 3 bonus |
| --- | --- | --- | --- | --- | --- |
| `overall` | A single roll is made. Entry `k` is the chance of getting **at least** `k` bonus rewards. | 50% | 30% | 15% | 5% |
| `chained` | Entries are rolled in order and rolling stops at the first miss. The bonus count is the number of hits before it. | 50% | 40% | 9.5% | 0.5% |
| `independent` | Every entry is rolled on its own. The bonus count is the number of hits. | 38% | 49.5% | 12% | 0.5% |

In `overall` mode the entries are sorted from highest to lowest, and a warning is logged on load if your plain-number entries were not already in that order. The other two modes use the order you wrote.

#### How rewards are picked

The first reward is picked as normal. Each bonus reward is then picked the same way, from the same weights, so `max-wins`, permission multipliers and weight effects all apply. Two reward keys, set in [How to Make a Reward](how-to-make-a-reward), tune this:

- `max-per-open` caps how many times one reward can be won in a single open.
- `bonus-eligible: false` keeps a reward out of bonus picks, so it can only ever be the first reward.

A reward already picked earlier in the same open counts towards its `max-wins`, so a reward with `max-wins: 1` can't be won twice in one open. If no reward is left to pick for a bonus, that bonus is skipped. Every pick is decided when the crate opens, before any animation plays.

#### Animations

Every built-in roll can show several rewards at once, up to a limit that depends on the roll. If an open has more rewards than the roll can show, they play in batches. The limits are listed on [Animations / Rolls](roll-animations). Rolls added by other plugins that don't support several rewards use `multi-reward.fallback`:

- `sequential`: the roll plays once per reward, one after another. Each reward gets its own reroll offer before the next starts.
- `primary`: the roll plays for the first reward. The bonus rewards are given straight after, with no animation and no reroll offer.

#### Rerolls

Each reward is rerolled on its own, and the `max-rerolls` limit and the `%reroll%` price count per reward. A reward can be rerolled if the crate's `rerolls` allow it, the reward isn't set to `rerollable: false`, and, for a bonus reward, `bonus-rerollable` is `true`. A bonus reward can't be rerolled into a reward that is `bonus-eligible: false`. A reroll keeps `max-per-open` across the open's other rewards: if nothing is left that the reward can be rerolled into, the reroll doesn't happen, the reward is kept and nothing is charged. If no reward from the open can be rerolled, no reroll menu opens.

When an animation shows several rewards at once, a reroll menu opens afterwards listing every reward from the open. Clicking a reward rerolls only that one, then the menu opens again with the new reward in its place. Accepting, or closing the menu, gives every reward. The menu is set up under `reroll.multi` in [Plugin Config](plugin-config).

#### Choosing your own rewards

With the `choose` roll the player picks every reward themselves, so weights don't apply. The player makes one choice for the first reward and one for each bonus reward.

- Bonus choices hide rewards that are `bonus-eligible: false`, and rewards already chosen `max-per-open` times in the open.
- If a bonus choice has nothing left to offer, it is dropped. It isn't given and isn't counted in `%reward_amount%`.
- The number of choices comes from `bonus-chances`, so any entry under `100` makes the number of choices random. A crate that must be fully non-random, with the same number of choices every time, uses only `100` entries.
- Rerolling a choice means choosing that reward again.

#### Effects

- `open-effects` run once per open, not again for bonus rewards or rerolls.
- `finish-effects` run once per reward, when that reward is given. They can use `%reward_number%` and `%reward_amount%`.
- `all-finish-effects` run once after the last reward of the open is given. They can use `%rewards%` and `%reward_amount%`.
- The reward's own `win-effects` run once per reward too, with the same two extra placeholders.

```yaml
all-finish-effects:
  - id: broadcast
    args:
      message: "%player%&f won %reward_amount% rewards from the %crate%&f: %rewards%"
```

The `opens` counter goes up once per open and once per reroll, as before, while wins count once per reward given.

#### Open paths

Every way of opening gives the extra rewards: placed keys, virtual keys, paying to open, and `/ecocrates forceopen`. One key or one payment is taken per open.

Shift right-click open-all gives every key its bonus rewards too, with no animation and no rerolls. With `open-all-effects-per-key: true`, `open-effects` run once per key, `finish-effects` once per reward, and `all-finish-effects` once per key. With it `false`, `open-effects` run once, `finish-effects` run once for the last reward, and `all-finish-effects` run once for the whole batch.

A player can't start a new open while another is in progress, including while the reroll menu is open.

If a player logs out in the middle of an open, or the server reloads or shuts down, every reward not yet given is given or queued for when they next log in. `all-finish-effects` run at that moment if the player is online. Rewards queued for later run their `finish-effects` as single rewards, so `%reward_number%` and `%reward_amount%` are `1`.

#### Previewing bonus chances

Show players what to expect in the preview title, custom slot lore and reward lore.

| Placeholder | Value |
| --- | --- |
| `%max_rewards%` | The most rewards one open can give, which is 1 plus the number of `bonus-chances` entries. Works in the preview title too. |
| `%bonus_chance_<n>%` | The chance of the n-th `bonus-chances` entry for the player viewing, formatted like `%chance%`. Works in custom slot lore and reward lore, but not the preview title, which has no viewer. |

```yaml
preview:
  title: "Demo Crate (up to %max_rewards% rewards)" # %bonus_chance_<n>% would not work here.
```

In a reward's `display.lore`, a line such as `"&fChance of a bonus: &a%bonus_chance_1%%"` shows the first chance to the player previewing.

## Internal placeholders

| Placeholder | Value |
| --- | --- |
| `%player%` | The player's name. |
| `%crate%` | The crate display name. |
| `%crate_id%` | The crate ID. |
| `%reward%` | The reward display name. Finish effects and reward win effects. |
| `%reward_id%` | The reward ID. Finish effects and reward win effects. |
| `%reward_number%` | The 1-based number of the reward within the open. Finish effects and reward win effects. |
| `%reward_amount%` | How many rewards the open gives, after any bonus that dropped out. Finish effects, reward win effects and all-finish effects. |
| `%rewards%` | The reward names, joined with commas. All-finish effects only. |

:::tip Troubleshooting
- **Crate not loading?** Check the file name is lowercase letters, numbers, and underscores only, and that it isn't prefixed with `_`.
- **Opening launches the player away?** They don't have a key; give one with `/ecocrates give`, or set `no-key-velocity` to 0 in `config.yml`.
- **Reward never appears?** Confirm its ID is listed under `rewards` and that the reward file exists in `/rewards/`.
:::

<hr/>

## Where to go next

- **Keys:** every crate needs one, see [How to Make a Key](how-to-make-a-key).
- **Rewards:** define what players win in [How to Make a Reward](how-to-make-a-reward).
- **Rolls:** pick the right opening animation on [Animations / Rolls](roll-animations).
- **Defaults:** browse the shipped example configs [here](https://github.com/Auxilor/EcoCrates/tree/master/eco-core/core-plugin/src/main/resources/crates).