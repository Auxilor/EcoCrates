---
title: "Plugin Config"
sidebar_position: 9
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

chance-decimal-places: 2 # How many decimal places to show in the %chance% placeholder. Increase this to distinguish very rare rewards from each other.

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
# Use %price% in the reroll button's name/lore to show the cost of the next reroll (empty for free rerolls).
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
      - "&fCost: &e%price%" # Cost of the next reroll

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
  title: "Your Keys (%page%/%max_page%)" # Supports %page% and %max_page% placeholders.

  page-change-sound: # The sound played when changing pages. Remove or set enabled to false to disable.
    enabled: true
    sound: ui.button.click
    pitch: 1.0
    volume: 1.0

  forwards-arrow: # The arrow for switching to the next page.
    item: arrow name:"&fNext Page" # Shown when there is a next page
    item-inactive: gray_dye name:"&7Next Page" # Shown on the last page. Remove to hide the button instead.
    row: 2
    column: 9

  backwards-arrow: # The arrow for switching to the previous page.
    item: arrow name:"&fPrevious Page" # Shown when there is a previous page
    item-inactive: gray_dye name:"&7Previous Page" # Shown on the first page. Remove to hide the button instead.
    row: 2
    column: 1

  custom-slots: [] # Custom GUI slots; see https://hub.auxilor.io/wiki/eco/pages

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
  # hide-placed-crate hides the placed crate's hologram and preview item from the
  # player who is rolling, for the duration of the roll. Only applies when opening
  # a physically placed crate, and only to the player opening it.
  csgo:
    filler: black_stained_glass_pane name:"" # Item filling non-selected slots
    selector: lime_stained_glass_pane name:"" # Item marking the selected slot
    bias: 0.65 # How strongly the scroll eases toward the winner, 0 to 1
    scrolls: 35 # Number of items scrolled past before stopping
    max-delay: 25 # Max ticks between scroll steps near the end
  flash:
    duration: 80 # Total ticks the flash runs
    wait: 20 # Ticks held on the winner before closing
    hide-placed-crate: false
  cycle:
    duration: 80 # Ticks spent cycling through display rewards
    wait: 20 # Ticks held on the winner before finishing
    interval: 5 # Ticks between each item swap
    height-offset: -1.0 # Offset from the crate's random-reward height the item floats at
    hide-placed-crate: true
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
    hide-placed-crate: false
  elimination:
    candidates: 7 # Rewards on show, including the winner; capped at 7
    interval: 12 # Ticks between each elimination
    reveal-time: 30 # Ticks the winner is left on screen
    filler: black_stained_glass_pane name:"" # Decorative filler items
    eliminated: red_stained_glass_pane name:"&cEliminated!" # Shown in place of a knocked out reward
  pick:
    boxes: 3 # Number of boxes to choose from; capped at 7
    solo-reveal-time: 25 # Ticks only the picked box is open for, before the rest open
    reveal-time: 40 # Ticks the opened boxes are left on screen after the rest open
    auto-pick: 200 # Ticks before a box is picked for the player
    title: "Pick a box!" # The GUI title; supports %crate%
    box: chest name:"&e&lClick to pick!" # The face-down box item
    filler: black_stained_glass_pane name:"" # Decorative filler items
  match:
    to-match: 2 # Copies of the same reward needed to win; losers get at most one fewer
    min-scratches: 3 # The winning set can never complete before this many scratches
    reveal-time: 40 # Ticks the finished card is left on screen
    auto-scratch: 100 # Ticks of no clicking before a card is scratched for the player
    title: "Find a pair!" # The GUI title; supports %crate%
    card: gray_stained_glass_pane name:"&e&lScratch!" # The unscratched card item
    filler: black_stained_glass_pane name:"" # Decorative filler items
  hologram:
    duration: 80 # Ticks spent cycling through rewards
    wait: 20 # Ticks the winner is held before finishing
    interval: 5 # Ticks between each name swap
    height: 1.5 # Height above the crate the hologram sits at
    rolling: # Lines shown while rolling; supports %reward%
      - "&8>> &f%reward% &8<<"
    winner: # Lines shown once the winner is picked
      - "&a&lYou won!"
      - "&f%reward%"
    hide-placed-crate: true
  vortex:
    items: 8 # Items in the funnel, including the winner
    start-radius: 3.0 # Radius the funnel starts at
    end-radius: 0.4 # Radius the funnel closes to
    start-height: 0.5 # Height the funnel starts at
    end-height: 2.5 # Height the funnel finishes at
    spins-per-second: 1.0 # Funnel rotation speed
    duration: 120 # Ticks the funnel takes to close
    eject-velocity: 0.6 # Speed losers are thrown out at
    reward-hold-time: 25 # Ticks the winner is held at the top
    timeout: 400 # Safety cap in ticks; ends the roll if it never resolves
    hide-placed-crate: false
  roulette:
    items: 8 # Items in the ring, including the winner
    radius: 2.5 # Ring radius in blocks
    height: 1.0 # Height the ring sits at
    steps: 32 # Positions the cursor moves through before stopping
    bias: 0.65 # How strongly the cursor eases to a stop, 0 to 1
    max-delay: 12 # Max ticks between cursor steps near the end
    reveal-time: 30 # Ticks the winner is left glowing
    timeout: 400 # Safety cap in ticks; ends the roll if it never resolves
    hide-placed-crate: false
  strike:
    height: 2.0 # Height the item rises to
    rise-time: 30 # Ticks spent rising
    hang-time: 15 # Ticks held at the top before the strike
    reveal-time: 25 # Ticks the winner is left after the strike
    interval: 5 # Ticks between each item swap on the way up
    hide-placed-crate: false
  delivery:
    entity: allay # The courier; any eco entity lookup, e.g. allay, vex, bee
    spawn-distance: 5 # Blocks behind the crate the courier appears
    spawn-height: 1.5 # Height above the crate it appears at
    speed: 0.25 # Blocks travelled per tick
    hover-height: 1.4 # Height above the player it delivers at
    hand-over-time: 25 # Ticks it hovers before handing the reward over
    timeout: 400 # Safety cap in ticks; ends the roll if it never arrives
    hide-placed-crate: false
  orbit_collapse:
    items: 8 # Items in the orbit, including the winner
    radius: 2.5 # Orbit radius in blocks
    height: 1.5 # Height the orbit sits at
    rise-velocity: 0.4 # Speed items move out to the orbit
    orbit-velocity: 0.4 # Speed items chase their orbit position
    collapse-velocity: 0.25 # Speed items fall inwards at the end
    spins-per-second: 0.75 # Orbit rotation speed
    orbit-time: 100 # Ticks the items orbit for
    reward-hold-time: 30 # Ticks the winner is held at the centre
    timeout: 400 # Safety cap in ticks; ends the roll if the items never settle
    hide-placed-crate: false
  sky_drop:
    launch-height: 6 # Height the winner rises to
    rise-velocity: 0.6 # Speed on the way up
    drop-velocity: 0.5 # Speed on the way down
    pause: 20 # Ticks held at the top before dropping
    settle: 20 # Ticks held on the ground before finishing
    decoy-count: 8 # Extra items thrown out of the crate
    decoy-spread: 0.25 # Horizontal spread of the thrown items
    timeout: 400 # Safety cap in ticks; ends the roll if the item never settles
    hide-placed-crate: false
  quick:
    height: 1.5 # Height the item rises to
    rise-velocity: 0.05 # Upward speed
    suspend: 10 # Ticks the item is held before finishing
    hide-placed-crate: false
  semi_instant:
    velocity: # Initial throw velocity of the item
      randomness: 0.2 # Random spread added to the velocity
      x: 0
      y: 0.3
      z: 0
    item-lifespan: 30 # Ticks the item exists before finishing
    hide-placed-crate: false
  slot_machine:
    filler: black_stained_glass_pane name:"" # Item filling non-selected slots
    selector: yellow_stained_glass_pane name:"" # Item marking the selected row
    symbols: 18 # Symbols on the reel
    spin-ticks: 36 # Ticks the reel spins
    reel-stop-delay: 8 # Ticks between each reel stopping
    spin-interval: 2 # Ticks between reel steps
    start-pitch: 0.9 # Pitch of the first spin sound
    pitch-step: 0.08 # Pitch increase per spin sound
    hold-ticks: 30 # Ticks the won reward is held on screen after the last reel stops
```

<hr/>

## Where to go next

- **Rolls:** see each animation in action on [Animations / Rolls](roll-animations).
- **Multipliers:** set up rank chance multipliers in [Commands and Permissions](commands-and-permissions).
- **Defaults:** browse the shipped config [here](https://github.com/Auxilor/EcoCrates/blob/master/eco-core/core-plugin/src/main/resources/config.yml).
