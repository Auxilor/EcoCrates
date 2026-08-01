---
title: "API"
sidebar_position: 9
---

This page is for developers who want to hook into EcoCrates from their own plugin, for example to read a player's keys or react to crate opens. EcoCrates is open-source, so you can also read the implementation directly.

## Source code

The source code is on GitHub [here](https://github.com/Auxilor/EcoCrates).

## Adding the dependency

1. Add the Auxilor repository to your `build.gradle.kts`:
2. Add EcoCrates as a `compileOnly` dependency, replacing `<version>` with the version you want.

```kotlin
repositories {
    maven("https://repo.auxilor.io/repository/maven-public/")
}

dependencies {
    compileOnly("com.willfp:EcoCrates:<version>")
}
```

The latest version available on the repo can be found [here](https://github.com/Auxilor/EcoCrates/tags).

<hr/>

## Envoys

Envoys add a `start_envoy` effect (arg: `category`) and an `end_envoy` effect (no args), an
`envoy_started` condition (optional arg: `category`), an `open_envoy` trigger (parameters:
player, event, location, text = the won reward's ID), an `envoy_type` filter (a list of
rarity IDs), and an `envoy_reward` filter (a list of reward IDs) to libreforge. Collecting an
envoy crate fires `EnvoyOpenEvent`, whose `reward` field is mutable so listeners can swap it
before it's given.

<hr/>

## Where to go next

- **eco framework:** shared APIs live in the [eco framework](https://github.com/Auxilor/eco).
- **Configure crates:** the config-side workflow is in [How to Make a Crate](how-to-make-a-crate).
- **Configure envoys:** the config-side workflow is in [How to Make an Envoy](how-to-make-an-envoy).