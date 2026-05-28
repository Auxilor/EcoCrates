---
title: "How to make a Reward"
sidebar_position: 1
---

Rewards are what players can win from crates. This is where you define what happens when the reward is won, how likely it is to be picked, and how it is displayed in the preview GUI and roll animations.

## How to add rewards

Each reward is its own config file, placed in the `/rewards/` folder, and you can add or remove them as you please. There is an example config called `_example.yml` to help you out!

The ID of the reward is the file name. This is what you use in your crate configs and preview layouts.\
ID's must be lowercase letters, numbers, and underscores only.

## Example Reward Config

```yaml
name: "&bDiamond Sword"

win-effects:
  - id: give_item
    args:
      item: diamond_sword sharpness:5 unbreaking:3
  - id: send_message
    args:
      message: "&fYou won the %reward% &fin %crate%&f!"

weight:
  permission-multipliers: true
  value: 1

max-wins: -1

display:
  name: "&bDiamond Sword"
  item: diamond_sword sharpness:5 unbreaking:3
  dont-keep-lore: false
  lore:
    - "&fChance: &a%chance%%"
```

## Understanding the Sections

#### The Reward Basics Section
:::danger Effects Section

The effects section is the core functionality of the crate reward. You can configure effects, conditions, and filters to run when the crate reward is won.

Check out [Configuring an Effect](https://plugins.auxilor.io/effects/configuring-an-effect) to understand how to configure this section correctly.

For more advanced users or setups, you can configure chains in this section to string together different effects under one trigger. Check out [Configuring an Effect Chain](https://plugins.auxilor.io/effects/configuring-a-chain) for more info.

:::
```yaml
name: "&bDiamond Sword" # The name of the reward

win-effects: # The effects to run when winning the reward
  - id: give_item
    args:
      item: diamond_sword sharpness:5 unbreaking:3
```

#### The Weight and Win Limits Section

```yaml
weight:
  permission-multipliers: true # If permission multipliers should affect the reward chance
  value: 1 # The chance of winning the crate, can use maths and placeholders (eg %player_y% * 25) for reactive chance

max-wins: -1 # The max amount of times any player can win the reward, useful for giving permissions / perks
```

#### The Display Item Section

```yaml
display:
  name: "&bDiamond Sword" # The item name
  item: diamond_sword sharpness:5 unbreaking:3 # The shown item
  dont-keep-lore: false # Optional config, set to true to only show the custom lore
  lore: # Can use %chance% and %weight% as placeholders
    - "&fChance: &a%chance%%"
    - "&fWeight: &b%weight%"
```

## Internal Placeholders

| Placeholder | Value |
| ----------- | ----- |
| `%chance%`  | Calculated chance of this reward in the crate. |
| `%weight%`  | Raw weight value of this reward. |
| `%reward%`  | The reward display name. |
| `%crate%`   | The crate display name. |

<hr/>

## Default Configs
The default configs can be found [here](https://github.com/Auxilor/EcoCrates/tree/master/eco-core/core-plugin/src/main/resources/rewards).
