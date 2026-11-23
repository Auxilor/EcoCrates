---
title: "PlaceholderAPI"
sidebar_position: 8
---

EcoCrates exposes placeholders you can use anywhere PlaceholderAPI is read, e.g. scoreboards, holograms, and menus. In every placeholder below, `<crate>` is a crate ID (its file name), `<pouch>` is a pouch ID, and `<reward>` is a reward ID.

## Placeholders

| Placeholder | Description |
| --- | --- |
| `%ecocrates_<crate>_keys%` | The amount of virtual keys a player has for a given crate |
| `%ecocrates_<crate>_opens%` | The amount of times a player has opened a crate |
| `%ecocrates_<crate>_last_winner%` | The last player to win a reward from a crate |
| `%ecocrates_<crate>_last_reward%` | The reward the last winner won from a crate |
| `%ecocrates_<crate>_leaderboard_rank%` | A player's position on a crate's opens leaderboard |
| `%ecocrates_<crate>_leaderboard_rank_display%` | A player's standing on a crate's opens leaderboard, as `#3` or `Top 12.5%` |
| `%ecocrates_<crate>_leaderboard_tracked%` | The amount of players ranked on a crate's opens leaderboard |
| `%ecocrates_<crate>_leaderboard_top_<N>_name%` | The player at position N on a crate's opens leaderboard |
| `%ecocrates_<crate>_leaderboard_top_<N>_value%` | The opens of the player at position N on a crate's opens leaderboard |
| `%ecocrates_top_<crate>_<N>_name%` | The same as `%ecocrates_<crate>_leaderboard_top_<N>_name%` |
| `%ecocrates_top_<crate>_<N>_opens%` | The same as `%ecocrates_<crate>_leaderboard_top_<N>_value%` |
| `%ecocrates_pouch_<pouch>_opens%` | The amount of times a player has opened a pouch |
| `%ecocrates_<reward>_wins%` | The amount of times a player has won a given reward |
| `%ecocrates_<reward>_total%` | The amount of times a reward has been won by all players, counted from when this placeholder was added |
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

The last winner placeholders show `last-winner-none` from `lang.yml` until someone wins from that crate. They are shared by every server on the same database.

:::info Leaderboards
The crate leaderboards rank players by how many times they have opened each crate. Turn them off with `leaderboard.enabled: false` in `config.yml`, which also removes their placeholders and `/ecocrates top`. Refresh timing, entry caps and rank display are set in the `leaderboards:` section of eco's own `config.yml`. Positions with no player show `top.empty-position` from `lang.yml`.
:::

:::info
Servers that share a database also share `%ecocrates_<reward>_total%`. Without Redis, each server can overwrite the others' wins, so the total can undercount. With Redis, the total can only miss wins that happen at the same moment.
:::

:::info
The bossbar's own placeholders (`%envoy_remaining_crates%` etc, inside `bossbar.name`) are a separate, non-PlaceholderAPI mechanism that works even without PAPI installed. See [How to Make an Envoy](how-to-make-an-envoy) for those.
:::

<hr/>

## Where to go next

- **Crate IDs:** crate IDs are file names, see [How to Make a Crate](how-to-make-a-crate).
- **Reward IDs:** reward IDs are file names, see [How to Make a Reward](how-to-make-a-reward).
- **Envoy IDs:** envoy IDs are file names, see [How to Make an Envoy](how-to-make-an-envoy).
- **Pouch IDs:** pouch IDs are file names, see [How to Make a Pouch](how-to-make-a-pouch).