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

## Where to go next

- **eco framework:** shared APIs live in the [eco framework](https://github.com/Auxilor/eco).
- **Configure crates:** the config-side workflow is in [How to Make a Crate](how-to-make-a-crate).