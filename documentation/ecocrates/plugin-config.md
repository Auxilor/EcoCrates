---
title: "Plugin Config"
sidebar_position: 8
---

This is the server-wide config for EcoCrates, `config.yml`, found at `/plugins/EcoCrates/config.yml`. It holds storage, permission multipliers, the reroll and keys GUIs, and the shared animation and roll settings. Apply changes with `/ecocrates reload`.

:::warning
Changing `use-local-storage` only takes full effect after a server restart, since it switches where player data is read from. A reload alone can leave stale data loaded.
:::

## Default config.yml

```yaml
# Even if eco is set up to use a database, you can
# force EcoCrates to save to local storage to disable
# cross-server sync.
use-local-storage: false

no-key-velocity: 1.5 # The speed at which a player should be launched away from a crate if they try to open it without a key. Set to 0 to disable.
track-player-keys: false # If an NBT tag should be added to keys that links them to a player - will prevent stacking some keys.

# If a reward allows permission multipliers, the weights
# will be multiplied by highest-priority multiplier that a player
# has permission for - i.e. if a player has both vip and mvp permissions,
# than they'll have the mvp one applied as it has a higher priority.
permission-multipliers:
  - permission: ecocrates.mutliplier.vip
    multiplier: 1.5
    priority: 1
  - permission: ecocrates.mutliplier.mvp
    multiplier: 2
    priority: 2

# The reroll GUI, shown after a win when the crate allows rerolling.
reroll:
  rows: 3 # Rows in the reroll GUI
  mask: # Decorative filler items
    items:
      - black_stained_glass_pane
      - green_stained_glass_pane
    pattern:
      - "122211111"
      - "120211011"
      - "122211111"
  title: "Accept your reward?" # The GUI title
  accept: # The button to keep the current reward
    row: 2
    column: 3
  reroll: # The button to reroll for a new reward
    row: 2
    column: 7
    item: orange_stained_glass_pane
    name: "&6Reroll"
    lore:
      - "&fNot happy with your item?"
      - "&fClick to try again for"
      - "&fa chance at something else!"
      - ""
      - "&cYou can only reroll once!"

# The /ecocrates keys GUI that lists a player's keys.
keygui:
  rows: 3 # Rows in the keys GUI
  mask: # Decorative filler items
    items:
      - black_stained_glass_pane
    pattern:
      - "111111111"
      - "110101011"
      - "111111111"
  title: "Your Keys" # The GUI title
  custom-slots: [] # Custom GUI slots; see https://plugins.auxilor.io/all-plugins/custom-gui-slots

# Particle animations used by placed crates; referenced by name in a crate's placed.particles.
animations:
  spiral:
    spirals-per-second: 0.5 # Full spirals completed per second
    rises-per-second: 0.25 # Vertical rises per second
    radius: 1.5 # Spiral radius in blocks
    height: 1 # Total rise height
    count: 1 # Particles spawned per step
  double_spiral:
    spirals-per-second: 0.25
    rises-per-second: 0.125
    radius: 1.5
    height: 1
    count: 1
  circle:
    spirals-per-second: 0.5
    radius: 1.5
    height: 1
    count: 1
  twirl:
    spirals-per-second: 0.5
    ticks: 80 # Total ticks for one twirl cycle
    small-radius: 0.2 # Radius at the tight end
    large-radius: 1.2 # Radius at the wide end
    start-height: 2 # Starting height of the twirl
    end-height: 0.7 # Ending height of the twirl
    count: 1
  tilted_rings:
    spirals-per-second: 0.5
    radius: 1.5
    y-offset: 1 # Vertical offset of the rings
    x-offset: 1 # Horizontal offset of the rings
    count: 1

# Settings for each roll animation; pick one per crate with the crate's roll option.
rolls:
  csgo:
    filler: black_stained_glass_pane name:"" # Item filling non-selected slots
    selector: lime_stained_glass_pane name:"" # Item marking the selected slot
    bias: 0.65 # How strongly the scroll eases toward the winner, 0 to 1
    scrolls: 35 # Number of items scrolled past before stopping
    max-delay: 25 # Max ticks between scroll steps near the end
  flash:
    duration: 80 # Total ticks the flash runs
    wait: 20 # Ticks held on the winner before closing
  encircle:
    spin-time: 100 # Ticks the items spin
    reveal-time: 80 # Ticks the winner is revealed for
    items: 12 # Items in the ring
    radius: 3 # Ring radius in blocks
    height: 1 # Height above the player
    spins-per-second: 0.5 # Ring rotation speed
    rise-velocity: 0.05 # Upward speed of items
    spin-velocity: 0.4 # Per-item spin speed
    reveal-velocity: 0.2 # Speed the winner rises on reveal
  quick:
    height: 1.5 # Height the item rises to
    rise-velocity: 0.05 # Upward speed
    suspend: 10 # Ticks the item is held before finishing
  semi_instant:
    velocity: # Initial throw velocity of the item
      randomness: 0.2 # Random spread added to the velocity
      x: 0
      y: 0.3
      z: 0
    item-lifespan: 30 # Ticks the item exists before finishing
  slot_machine:
    filler: black_stained_glass_pane name:"" # Item filling non-selected slots
    selector: yellow_stained_glass_pane name:"" # Item marking the selected row
    symbols: 18 # Symbols on the reel
    spin-ticks: 36 # Ticks the reel spins
    reel-stop-delay: 8 # Ticks between each reel stopping
    spin-interval: 2 # Ticks between reel steps
    start-pitch: 0.9 # Pitch of the first spin sound
    pitch-step: 0.08 # Pitch increase per spin sound
```

<hr/>

## Where to go next

- **Rolls:** see each animation in action on [Animations / Rolls](roll-animations).
- **Multipliers:** set up rank chance multipliers in [Commands and Permissions](commands-and-permissions).
- **Defaults:** browse the shipped config [here](https://github.com/Auxilor/EcoCrates/blob/master/eco-core/core-plugin/src/main/resources/config.yml).
