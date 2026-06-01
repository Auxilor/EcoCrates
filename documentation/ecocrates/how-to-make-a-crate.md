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

The file name without `.yml` is the crate's ID. This is what you use in commands, key configs, and effects. The first time you reference an item (for a reward display, mask, or arrow) it uses the [Item Lookup System](https://plugins.auxilor.io/the-item-lookup-system) format.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the crate will not load.
:::

## The structure of a crate

| Part | What it controls |
| --- | --- |
| **Basics** | The display name, roll animation, key, and reroll toggle |
| **Preview** | The GUI players see when previewing the crate |
| **Pay to open** | Letting players pay currency instead of using a key |
| **Placed** | Holograms and particles for physically placed crates |
| **Effects** | What runs when the crate opens and finishes rolling |
| **Rewards** | Which rewards are in the pool |

```yaml
# === Basics: identity and behaviour ===
name: "Demo Crate" # The display name of the crate
roll: csgo # The opening animation; see the Animations / Rolls page for options
can-reroll: true # If true, the player can reroll a won reward once
key: demo_crate # The key ID this crate uses (configure in /keys/demo_crate.yml); crates can share a key

# === Preview: the GUI players browse before opening ===
preview:
  title: Demo Crate # The title of the preview GUI
  rows: 6 # Number of rows, between 1 and 6
  custom-slots: [] # GUI slots shared across all preview pages
  forwards-arrow: # Shown on every page except the last
    item: arrow name:"&fNext Page"
    row: 6
    column: 6
  backwards-arrow: # Shown on every page except the first
    item: arrow name:"&fPrevious Page"
    row: 6
    column: 4
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

# === Placed: holograms and particles for physically placed crates ===
placed:
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

The identity of the crate: its name, the roll animation, the key it consumes, and whether rerolls are allowed.

```yaml
name: "Demo Crate" # The display name of the crate
roll: csgo # The opening animation; see the Animations / Rolls page for options
can-reroll: true # If true, the player can reroll a won reward once
key: demo_crate # The key ID this crate uses (configure in /keys/demo_crate.yml); crates can share a key
```

### Preview

The GUI players see when they preview the crate, laid out as one or more pages with a decorative mask and reward slots.

```yaml
preview:
  title: Demo Crate # The title of the preview GUI
  rows: 6 # Number of rows, between 1 and 6
  custom-slots: [] # GUI slots shared across all preview pages
  forwards-arrow: # Shown on every page except the last
    item: arrow name:"&fNext Page"
    row: 6
    column: 6
  backwards-arrow: # Shown on every page except the first
    item: arrow name:"&fPrevious Page"
    row: 6
    column: 4
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

:::tip
Add custom buttons to the preview with custom GUI slots; see [Custom GUI Slots](https://plugins.auxilor.io/all-plugins/custom-gui-slots).
:::

### Pay to open

Lets players spend currency to open the crate instead of consuming a key.

```yaml
pay-to-open:
  enabled: false # If players can pay to open instead of using a key
  price: 5000 # The cost to open
  type: coins # The currency to charge
```

### Placed

Holograms and particles shown on a crate that has been physically placed in the world with `/ecocrates set`.

```yaml
placed:
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

- [Configuring an Effect](https://plugins.auxilor.io/effects/configuring-an-effect)
- [Configuring an Effect Chain](https://plugins.auxilor.io/effects/configuring-a-chain)
:::

### Rewards

The pool of reward IDs this crate can give. This is kept separate from the preview layout, so you can include secret rewards that never show in the GUI.

```yaml
rewards:
  - diamond_sword
  - stack_of_emeralds
  - 1000_coins
```

## Internal placeholders

| Placeholder | Value |
| --- | --- |
| `%player%` | The player's name. |
| `%crate%` | The crate display name. |
| `%reward%` | The reward display name. |

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