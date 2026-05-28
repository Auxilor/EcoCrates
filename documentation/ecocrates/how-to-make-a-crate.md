---
title: "How to make a Crate"
sidebar_position: 3
---

Crates are the core of EcoCrates. This is where you define how a crate opens, which key it uses, what players see in previews, and which rewards can be won.

## How to add crates

Each crate is its own config file, placed in the `/crates/` folder, and you can add or remove them as you please. There is an example config called `_example.yml` to help you out!

The ID of the crate is the file name. This is what you use in commands, key configs, and effects.\
ID's must be lowercase letters, numbers, and underscores only.

## Example Crate Config

```yaml
name: "Demo Crate"
roll: csgo
can-reroll: true

key: demo_crate

preview:
  title: Demo Crate
  rows: 6
  custom-slots: []
  forwards-arrow:
    item: arrow name:"&fNext Page"
    row: 6
    column: 6
  backwards-arrow:
    item: arrow name:"&fPrevious Page"
    row: 6
    column: 4
  pages:
    - page: 1
      mask:
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
      custom-slots: [ ]
      rewards:
        - id: diamond_sword
          row: 3
          column: 4
        - id: stack_of_emeralds
          row: 4
          column: 6

pay-to-open:
  enabled: false
  price: 5000
  type: coins

placed:
  random-reward:
    enabled: true
    height: 1.5
    delay: 30
    name: "&fYou could win: %reward%"
  particles:
    - particle: flame
      animation: spiral
  hologram:
    height: 1.5
    ticks: 200
    frames:
      - tick: 0
        lines:
          - "<g:#56ab2f>&lDEMO CRATE</g:#a8e063>"
          - "&b&lLeft Click to Preview"
          - '&a&lRight click to Open'
      - tick: 100
        lines:
          - "<g:#56ab2f>&lDEMO CRATE</g:#a8e063>"
          - "&a&lLeft Click to Preview"
          - '&b&lRight click to Open'

open-effects:
  - id: send_message
    args:
      message: "Good luck!"

finish-effects:
  - id: send_message
    args:
      message: "You won %reward%&f!"

rewards:
  - diamond_sword
  - stack_of_emeralds
```

## Understanding the Sections

#### The Crate Basics Section

```yaml
name: "Demo Crate" # The display name of the crate
roll: csgo # The opening style, check here: https://plugins.auxilor.io/ecocrates/animationsandrolls
can-reroll: true # If once you win a reward, you can choose to reroll

key: demo_crate # The ID of the key for this crate (configure in /keys/demo_crate.yml)
                # Multiple crates can share the same key by using the same key ID here.
```

#### The Preview GUI Section

```yaml
preview:
  title: Demo Crate # The title of the preview GUI
  rows: 6 # The amount of rows for the gui, between 1 and 6
  custom-slots: [ ] # Custom GUI slots shared across all preview pages; see here for a how-to: https://plugins.auxilor.io/all-plugins/custom-gui-slots
  forwards-arrow: # The arrow for switching between pages. If on the last page, this will not show up.
    item: arrow name:"&fNext Page"
    row: 6
    column: 6
  backwards-arrow: # The arrow for switching between pages. If on the first page, this will not show up.
    item: arrow name:"&fPrevious Page"
    row: 6
    column: 4
  pages:
    - page: 1
      mask: # Filler items for decoration
        items: # Add as many items as you want
          - gray_stained_glass_pane # Item 1
          - black_stained_glass_pane # Item 2
      custom-slots: [ ] # Custom GUI slots for this page; see here for a how-to: https://plugins.auxilor.io/all-plugins/custom-gui-slots
      rewards:
        - id: diamond_sword # The reward ID
          row: 3 # The row
          column: 4 # The column
```

#### The Pay to Open Section

```yaml
pay-to-open:
  enabled: false # If it should be allowed
  price: 5000 # The price to buy the crate
  type: coins # The type of currency to use
```

#### The Placed Crate Section

```yaml
placed: # Options for physically placed crates
  random-reward: # The random reward hologram, shows an item
    enabled: true # If the random reward should be shown
    height: 1.5 # The height above the crate at which to show the reward
    delay: 30 # The ticks between showing a new item
    name: "&fYou could win: %reward%" # The text above the item - use %reward% for the reward name
  particles: # The particle effects around the crate, add as many as you want
    - particle: flame # https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Particle.html
      animation: spiral # spiral, double_spiral, circle, or twirl
  hologram: # The text hologram, requires a hologram plugin to be installed
    height: 1.5 # The height above the crate
    ticks: 200 # The total ticks to cycle all frames
    frames:
      - tick: 0 # The starting tick to show this frame
        lines:
          - "<g:#56ab2f>&lDEMO CRATE</g:#a8e063>"
          - "&b&lLeft Click to Preview"
          - '&a&lRight click to Open'
      - tick: 100
        lines:
          - "<g:#56ab2f>&lDEMO CRATE</g:#a8e063>"
          - "&a&lLeft Click to Preview"
          - '&b&lRight click to Open'
```

#### The Open and Finish Effects Section
:::danger Effects Section

The effects section is the core functionality of the crate. You can configure effects, conditions, and filters to run when the crate is opened or when it finishes rolling.

Check out [Configuring an Effect](https://plugins.auxilor.io/effects/configuring-an-effect) to understand how to configure this section correctly.

For more advanced users or setups, you can configure chains in this section to string together different effects under one trigger. Check out [Configuring an Effect Chain](https://plugins.auxilor.io/effects/configuring-a-chain) for more info.

:::
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

#### The Rewards List Section

```yaml
# The rewards to give, configure in /rewards/ folder
# This is separate to the reward locations so that you could have secret rewards that don't show up in the preview GUI
rewards:
  - diamond_sword
  - stack_of_emeralds
  - 1000_coins
```

## Internal Placeholders

| Placeholder | Value |
| ----------- | ----- |
| `%player%`  | The player's name. |
| `%crate%`   | The crate display name. |
| `%reward%`  | The reward display name. |

<hr/>

## Default Configs
The default configs can be found [here](https://github.com/Auxilor/EcoCrates/tree/master/eco-core/core-plugin/src/main/resources/crates).


