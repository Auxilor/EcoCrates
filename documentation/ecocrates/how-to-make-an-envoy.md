---
title: "How to Make an Envoy"
sidebar_position: 5
---

An envoy is a timed event: reward crates spawn around the world and players race to find and open them. Envoy crates have no roll animation, no reroll, and need no key, physical or virtual — right-clicking one gives exactly one reward instantly. Only one envoy can run at a time, across the whole server. This page walks you through making one from an empty file to a running event.

## Quick start

1. Open the `/envoys/` folder inside the EcoCrates plugin folder.
2. Copy `_example.yml` and rename it to your envoy's ID, e.g. `airdrop.yml`.
3. Set the `name`, `duration`, and how it should `start`.
4. Pick a `location-type` and configure `radius` (or set points in game later).
5. Add one or more `rarities`, each with its own `rewards` pool.
6. Run `/ecocrates reload`, then `/ecocrates envoy start <envoy>` to test it.
7. Right-click a spawned crate to confirm it gives a reward instantly.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real envoy. Envoys are a `RegistrableCategory`, the same loading mechanism crates and keys use, so you can also organise envoys into subfolders inside `envoys/` and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the envoy's ID. This is what you use in commands, effects, and placeholders.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the envoy will not load.
:::

## Name and duration

The display name and how long a session lasts, in ticks.

```yaml
name: "Example Envoy" # The display name of the envoy
duration: 6000 # How long a session lasts, in ticks (6000 = 5 minutes).
               # The session also ends early if every crate gets collected.
```

## Starting

Envoys can start automatically on a schedule, or be triggered manually (command, flare, or effect).

```yaml
start:
  # A list of 24h HH:mm times. If this list is non-empty it takes priority
  # and 'interval' below is ignored. Leave empty to use 'interval' instead.
  time:
    - "12:00"
    - "18:30"
  interval: 36000 # Ticks between automatic starts (36000 = 30 minutes).
                  # Only used when 'time' above is empty. Set to 0 to never
                  # auto-start (command / flare only).
```

## Location type

Where crates spawn. Pick exactly one mode.

```yaml
location-type: radius
```

`radius` rolls a random point inside a box:

```yaml
radius:
  center:
    world: world
    x: 0
    y: 70
    z: 0
  x_radius: 200
  y_radius: 20
  z_radius: 200
```

`points` picks from a fixed set of locations you mark in game with `/ecocrates envoy set <envoy> <current|x y z>`. Points are stored in `envoypoints.yml`, not in this file.

:::info
`envoypoints.yml` is internal storage, not something you hand-edit. Because points live outside `<envoy>.yml`, reloading the plugin or updating this file can never wipe the spawn points you've set.
:::

## Spawn counts

```yaml
# The total number of crates in one session, rolled once at the start.
# Which rarity fills each of those slots is decided by the rarity weights below.
min-spawns: 8
max-spawns: 15
```

`min-spawns`/`max-spawns` are for the **whole session**, rolled once when it starts — not per rarity. Once the total is rolled, each individual slot is filled by whichever rarity wins the weighted roll below.

## Rarities

Each envoy needs at least one rarity. Every field below comes straight from `_example.yml`.

```yaml
rarities:
  - id: common # Unique within this envoy. Used by the envoy_type filter.
    display_name: "&aCommon"
    weight: 80 # Relative chance this rarity fills a given spawn slot
    collection_cooldown: 100 # Per-player, in ticks, between collecting crates
                             # of THIS rarity. Not persisted across restarts.
    show_on_compass: true # Whether this rarity shows up on envoy compasses
    compass_color: "#a8e063" # The locator bar dot colour. Colour name or hex.
                             # Remove to use the client's default colour.
    rewards: # Reward IDs from the /rewards folder. One is picked per open,
             # using each reward's own weight - the same as crates.
      - 1000_coins
      - stack_of_emeralds
    block: chest # An eco block lookup string
    hologram:
      message: "&a&lCOMMON ENVOY"
      height: 1.5
    item-display: # A floating item above the crate, cycling this rarity's rewards
      enabled: true
      height: 1.0
      delay: 30 # Ticks between showing a new item
      name: "&fYou could win:"
    fireworks: # Fired when a crate of this rarity spawns
      enabled: true
      amount: 1
      type: ball # ball, large_ball, burst, star, creeper
      colors: # Colour names or hex codes
        - lime
        - "#a8e063"
    spawn-sound: # Played at the crate's location when it spawns. Remove or set enabled to false to disable.
      enabled: true
      sound: entity_experience_orb_pickup
      pitch: 1.0 # Can also be a range, e.g. "0.8..1.2"
      volume: 1.0
    open-effects: # libreforge effects run when a player collects this crate.
      - id: send_message
        args:
          message: "&aYou found a %envoy_rarity% &aenvoy and won %reward%&a!"
```

`collection_cooldown` is per-player, per-rarity, and lives in memory only — it resets on a plugin/server restart, unlike most other cooldowns in the plugin.

### `open-effects` placeholders

Fired when a player collects a crate of this rarity.

| Placeholder | Meaning |
| --- | --- |
| `%envoy_category%` | The envoy's display name |
| `%envoy_rarity%` | The rarity's display name |
| `%reward%` | The won reward's name |
| `%reward_id%` | The won reward's ID |

:::info
These are display names, not IDs. `%envoy_rarity%` also reaches the reward's own `win-effects`, so a reward config can announce which rarity it came from. Both `%envoy_category%` and `%envoy_rarity%` work in `lang.yml` messages that carry the same context, so the vocabulary is consistent everywhere.
:::

## Session-wide effects

`start-effects` and `end-effects` run once for the whole session, with no single player attached — so player-scoped effects like `send_message` can't be used directly. Two ways round it:

- `broadcast`, which needs no player at all.
- `all_players`, which re-dispatches nested effects once per online player, carrying the envoy placeholders through so `%player%` and e.g. `%envoy_top_collector%` both resolve inside it.

```yaml
start-effects: # Runs when a session starts.
  - id: broadcast
    args:
      message: "&6An %envoy_category% &6envoy has landed! &f%envoy_crates% &6crates are up for grabs."

end-effects: # Runs when a session ends, whether by timeout or by the last crate being collected.
  - id: broadcast
    args:
      message: "&6The %envoy_category% &6envoy is over! &f%envoy_top_collector% &6collected the most, with &f%envoy_top_collector_amount%&6."
  # Example of the all_players escape hatch: a personalised message plus a
  # sound, both of which need a player.
  - id: all_players
    args:
      effects:
        - id: send_message
          args:
            message: "&7%player%&7, the envoy is over. &f%envoy_top_collector% &7won it."
        - id: play_sound
          args:
            sound: entity_player_levelup
            pitch: 1.0
            volume: 1.0
```

`start-effects` placeholders:

| Placeholder | Meaning |
| --- | --- |
| `%envoy_category%` | The envoy's display name |
| `%envoy_crates%` | How many crates the session spawned |

`end-effects` placeholders:

| Placeholder | Meaning |
| --- | --- |
| `%envoy_category%` | The envoy's display name |
| `%envoy_top_collector%` | Name of whoever collected the most |
| `%envoy_top_collector_amount%` | How many they collected |
| `%envoy_crates_collected%` | Total collected this session |
| `%envoy_crates_remaining%` | Crates left standing when it ended |

## Start flare

An optional item that lets players start this envoy themselves, without a command.

```yaml
start-flare:
  enabled: true
  item: firework_rocket name:"&6Envoy Flare" # An eco item lookup string
  lore:
    - "&fRight click to call in an envoy!"
  crafting: # Remove this whole block to make the flare uncraftable.
    enabled: true
    permission: ecocrates.craft.flare # Remove to let everyone craft it
    shapeless: false
    recipe:
      - air
      - gunpowder
      - air
      - gunpowder
      - firework_rocket
      - gunpowder
      - air
      - gunpowder
      - air
  cooldown-ticks: 12000 # Per-player cooldown between flare uses (10 minutes)
  reset-schedule: true # If using a flare should reset this envoy's own
                       # scheduled timer, so it doesn't fire again immediately.
  price: # Optional extra cost, on top of consuming the flare item.
         # Remove this block for the item to be the only cost.
    type: coins
    value: 5000
    display: "&e%value% coins"
```

:::warning Cumulative cost
If `start-flare` is enabled, the flare item must be held and is **always** consumed on a successful start. If `price` is also configured, it's charged **first**, before the item is consumed — there's no "price-only" flare, the item is always required.
:::

`reset-schedule: false` means using the flare doesn't push back the category's own scheduled timer — its next `time`/`interval` auto-start still happens on the original schedule.

## Compass

An optional consumable item that marks nearby envoy crates on the vanilla locator bar.

:::info Requirements
The compass needs Minecraft **1.21.6+** (the vanilla locator bar) and eco **2026.31+**.
:::

```yaml
compass:
  enabled: true
  item: compass name:"&bEnvoy Compass" # An eco item lookup string
  lore:
    - "&fRight click during an envoy to"
    - "&ftrack the nearest crates."
  crafting: # Remove this whole block to make the compass uncraftable.
    enabled: true
    permission: ecocrates.craft.compass # Remove to let everyone craft it
    shapeless: false
    recipe:
      - air
      - redstone
      - air
      - redstone
      - compass
      - redstone
      - air
      - redstone
      - air
  duration: 1200 # How long one compass lasts once used, in ticks (1 minute)
  cooldown-ticks: 2400 # Per-player cooldown between compass uses
  max-tracked: 3 # How many crates one compass marks at once (nearest first)
  range: 500 # Only mark crates within this many blocks. 0 for unlimited.
  price: # Optional extra cost on top of consuming the compass item.
         # Remove this block for the item to be the only cost.
    type: coins
    value: 1000
    display: "&e%value% coins"
```

A few behaviours worth knowing:

- The compass is consumed at **activation**, not at expiry — relogging loses whatever time was left, and nothing is refunded.
- It's per-category: activating one while a *different* envoy's session is running, or with no session running at all, is refused.
- It marks the nearest `max-tracked` crates within `range` blocks. If nothing is trackable when it's used, the item is **not** consumed and a message explains why.
- `show_on_compass` on a rarity only affects the compass — `/ecocrates envoy locate` still lists every crate, since it's admin tooling.

## Bossbar

Shown to every online player for the duration of the session, and cleared the instant it ends, for any reason.

```yaml
bossbar:
  enabled: true
  name: "&6%envoy_category% &7- &f%envoy_remaining_crates%&7/&f%envoy_total_crates% crates left &7- &f%envoy_remaining_time%"
  # Defaults to the envoy's display name if omitted.
  color: YELLOW # PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE
  style: SOLID # SOLID, SEGMENTED_6, SEGMENTED_10, SEGMENTED_12, SEGMENTED_20
```

`name` supports its own hand-rolled placeholders, not PlaceholderAPI, so it works even without PAPI installed:

| Placeholder | Meaning |
| --- | --- |
| `%envoy_category%` | The envoy's display name |
| `%envoy_remaining_crates%` | Crates still standing |
| `%envoy_total_crates%` | Crates the session spawned |
| `%envoy_remaining_time%` | Time left, as `m:ss` |
| `%envoy_remaining_seconds%` | Time left in seconds, for maths |

If `name` is omitted, the bar just shows the envoy's display name.

## Full example

The complete, shipped `_example.yml`:

```yaml
# The ID of the envoy is the name of the .yml file,
# for example airdrop.yml has the ID of airdrop.
# You can place envoys anywhere in this folder, including in subfolders.
# _example.yml is not loaded.
#
# An envoy is an event where crates spawn around the world and players race
# to find and open them. Envoy crates have no roll animation, no rerolls, and
# need no keys - right-clicking one instantly gives a reward.
# Only one envoy session can run at a time across the whole server.

name: "Example Envoy" # The display name of the envoy
duration: 6000 # How long a session lasts, in ticks (6000 = 5 minutes).
               # The session also ends early if every crate gets collected.

start: # When this envoy automatically starts.
  # A list of 24h HH:mm times. If this list is non-empty it takes priority
  # and 'interval' below is ignored. Leave empty to use 'interval' instead.
  time:
    - "12:00"
    - "18:30"
  interval: 36000 # Ticks between automatic starts (36000 = 30 minutes).
                  # Only used when 'time' above is empty. Set to 0 to never
                  # auto-start (command / flare only).

# 'radius' rolls a random point inside the box below, 'points' picks from the
# fixed points you set with /ecocrates envoy set. Pick exactly one.
location-type: radius

radius:
  # Only used when location-type is 'radius'.
  # Points-mode spawn points are NOT configured here - set them in game with
  # /ecocrates envoy set, and they're stored in envoypoints.yml so that
  # reloading or updating this file can never wipe them.
  center:
    world: world
    x: 0
    y: 70
    z: 0
  x_radius: 200
  y_radius: 20
  z_radius: 200

# The total number of crates in one session, rolled once at the start.
# Which rarity fills each of those slots is decided by the rarity weights below.
min-spawns: 8
max-spawns: 15

start-flare: # An item players can use to start this envoy themselves.
  enabled: true
  item: firework_rocket name:"&6Envoy Flare" # An eco item lookup string
  lore:
    - "&fRight click to call in an envoy!"
  crafting: # Remove this whole block to make the flare uncraftable.
    enabled: true
    permission: ecocrates.craft.flare # Remove to let everyone craft it
    shapeless: false
    recipe:
      - air
      - gunpowder
      - air
      - gunpowder
      - firework_rocket
      - gunpowder
      - air
      - gunpowder
      - air
  cooldown-ticks: 12000 # Per-player cooldown between flare uses (10 minutes)
  reset-schedule: true # If using a flare should reset this envoy's own
                       # scheduled timer, so it doesn't fire again immediately.
  price: # Optional extra cost, on top of consuming the flare item.
         # Remove this block for the item to be the only cost.
    type: coins
    value: 5000
    display: "&e%value% coins"

compass: # A consumable item that marks nearby envoy crates on the locator bar.
  enabled: true
  item: compass name:"&bEnvoy Compass" # An eco item lookup string
  lore:
    - "&fRight click during an envoy to"
    - "&ftrack the nearest crates."
  crafting: # Remove this whole block to make the compass uncraftable.
    enabled: true
    permission: ecocrates.craft.compass # Remove to let everyone craft it
    shapeless: false
    recipe:
      - air
      - redstone
      - air
      - redstone
      - compass
      - redstone
      - air
      - redstone
      - air
  duration: 1200 # How long one compass lasts once used, in ticks (1 minute)
  cooldown-ticks: 2400 # Per-player cooldown between compass uses
  max-tracked: 3 # How many crates one compass marks at once (nearest first)
  range: 500 # Only mark crates within this many blocks. 0 for unlimited.
  price: # Optional extra cost on top of consuming the compass item.
         # Remove this block for the item to be the only cost.
    type: coins
    value: 1000
    display: "&e%value% coins"

bossbar: # Shown to every online player for the duration of this envoy's session.
  enabled: true
  name: "&6%envoy_category% &7- &f%envoy_remaining_crates%&7/&f%envoy_total_crates% crates left &7- &f%envoy_remaining_time%"
  # Defaults to the envoy's display name if omitted.
  color: YELLOW # PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE
  style: SOLID # SOLID, SEGMENTED_6, SEGMENTED_10, SEGMENTED_12, SEGMENTED_20

# Effects for the session as a whole. These run globally, with no single
# player attached, so player-scoped effects like send_message can't be used
# directly here. Two ways round it:
#   - `broadcast`, which needs no player at all
#   - `all_players`, which runs nested effects once per online player. The
#     envoy placeholders carry through, and %player% resolves per recipient.
start-effects: # Runs when a session starts.
               # Placeholders: %envoy_category%, %envoy_crates%
  - id: broadcast
    args:
      message: "&6An %envoy_category% &6envoy has landed! &f%envoy_crates% &6crates are up for grabs."

end-effects: # Runs when a session ends, whether by timeout or by the last crate
             # being collected.
             # Placeholders: %envoy_category%, %envoy_top_collector%,
             # %envoy_top_collector_amount%, %envoy_crates_collected%,
             # %envoy_crates_remaining%
  - id: broadcast
    args:
      message: "&6The %envoy_category% &6envoy is over! &f%envoy_top_collector% &6collected the most, with &f%envoy_top_collector_amount%&6."
  # Example of the all_players escape hatch: a personalised message plus a
  # sound, both of which need a player.
  - id: all_players
    args:
      effects:
        - id: send_message
          args:
            message: "&7%player%&7, the envoy is over. &f%envoy_top_collector% &7won it."
        - id: play_sound
          args:
            sound: entity_player_levelup
            pitch: 1.0
            volume: 1.0

rarities: # Add as many as you want.
  - id: common # Unique within this envoy. Used by the envoy_type filter.
    display_name: "&aCommon"
    weight: 80 # Relative chance this rarity fills a given spawn slot
    # Per-player cooldown, in ticks, between collecting crates of THIS rarity.
    # Not persisted across restarts.
    collection_cooldown: 100
    show_on_compass: true # Whether this rarity shows up on envoy compasses
    compass_color: "#a8e063" # The locator bar dot colour. Colour name or hex.
                             # Remove to use the client's default colour.
    rewards: # Reward IDs from the /rewards folder. One is picked per open,
             # using each reward's own weight - the same as crates.
      - 1000_coins
      - stack_of_emeralds
    block: chest # An eco block lookup string
    hologram:
      message: "&a&lCOMMON ENVOY"
      height: 1.5
    item-display: # A floating item above the crate, cycling this rarity's rewards
      enabled: true
      height: 1.0
      delay: 30 # Ticks between showing a new item
      name: "&fYou could win:"
    fireworks: # Fired when a crate of this rarity spawns
      enabled: true
      amount: 1
      type: ball # ball, large_ball, burst, star, creeper
      colors: # Colour names or hex codes
        - lime
        - "#a8e063"
    spawn-sound: # Played at the crate's location when it spawns. Remove or set enabled to false to disable.
      enabled: true
      sound: entity_experience_orb_pickup
      pitch: 1.0 # Can also be a range, e.g. "0.8..1.2"
      volume: 1.0
    open-effects: # libreforge effects run when a player collects this crate.
                  # Placeholders: %envoy_category%, %envoy_rarity%,
                  # %reward%, %reward_id%
      - id: send_message
        args:
          message: "&aYou found a %envoy_rarity% &aenvoy and won %reward%&a!"

  - id: legendary
    display_name: "&6&lLegendary"
    weight: 20
    collection_cooldown: 600
    show_on_compass: false # Legendary crates are hidden from envoy compasses,
                           # so finding one is down to luck, not tracking.
    rewards:
      - diamond_sword
    block: ender_chest
    hologram:
      message: "&6&lLEGENDARY ENVOY"
      height: 1.5
    item-display:
      enabled: true
      height: 1.0
      delay: 20
      name: "&6You could win:"
    fireworks:
      enabled: true
      amount: 1
      type: burst
      colors:
        - orange
        - yellow
    spawn-sound:
      enabled: true
      sound: entity_ender_dragon_growl
      pitch: 1.0
      volume: 1.0
    open-effects:
      - id: send_message
        args:
          message: "&6You found a LEGENDARY envoy and won %reward%&6!"
```

<hr/>

## Where to go next

- **Rewards:** envoy rarities pick from the same reward pool as crates, see [How to Make a Reward](how-to-make-a-reward).
- **Commands:** start, end, and locate envoys with [Commands and Permissions](commands-and-permissions).
- **Placeholders:** show live envoy status anywhere with [PlaceholderAPI](placeholderapi).
- **Defaults:** browse the shipped example configs [here](https://github.com/Auxilor/EcoCrates/tree/master/eco-core/core-plugin/src/main/resources/envoys).
