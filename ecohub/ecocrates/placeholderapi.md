---
title: "PlaceholderAPI"
sidebar_position: 7
---

EcoCrates exposes placeholders you can use anywhere PlaceholderAPI is read, e.g. scoreboards, holograms, and menus. In every placeholder below, `<crate>` is a crate ID (its file name) and `<reward>` is a reward ID.

## Placeholders

| Placeholder | Description |
| --- | --- |
| `%ecocrates_<crate>_keys%` | The amount of virtual keys a player has for a given crate |
| `%ecocrates_<crate>_opens%` | The amount of times a player has opened a crate |
| `%ecocrates_<reward>_wins%` | The amount of times a player has won a given reward |
| `%ecocrates_envoy_active%` | `true` while an envoy is running, otherwise `false` |
| `%ecocrates_envoy_active_name%` | Display name of the running envoy |
| `%ecocrates_envoy_active_id%` | ID of the running envoy |
| `%ecocrates_envoy_active_remaining_crates%` | Crates still standing |
| `%ecocrates_envoy_active_total_crates%` | Crates the session spawned |
| `%ecocrates_envoy_active_collected_crates%` | Crates collected so far |
| `%ecocrates_envoy_active_remaining_time%` | Time left, as `m:ss` |
| `%ecocrates_envoy_active_remaining_seconds%` | Time left in seconds |
| `%ecocrates_envoy_active_top_collector%` | Whoever has collected the most this session |
| `%ecocrates_envoy_active_top_collector_amount%` | How many they've collected |
| `%ecocrates_envoy_active_collected%` | How many the viewing player has collected |
| `%ecocrates_envoy_<envoy>_time_to_start%` | Time until that envoy next auto-starts, as `m:ss` |
| `%ecocrates_envoy_<envoy>_seconds_to_start%` | The same, in seconds |
| `%ecocrates_envoy_<envoy>_name%` | That envoy's display name |

Anything with no running envoy falls back to `None`/`0`/`false` as appropriate.

:::info
The bossbar's own placeholders (`%envoy_remaining_crates%` etc, inside `bossbar.name`) are a separate, non-PlaceholderAPI mechanism that works even without PAPI installed. See [How to Make an Envoy](how-to-make-an-envoy) for those.
:::

<hr/>

## Where to go next

- **Crate IDs:** crate IDs are file names, see [How to Make a Crate](how-to-make-a-crate).
- **Reward IDs:** reward IDs are file names, see [How to Make a Reward](how-to-make-a-reward).
- **Envoy IDs:** envoy IDs are file names, see [How to Make an Envoy](how-to-make-an-envoy).