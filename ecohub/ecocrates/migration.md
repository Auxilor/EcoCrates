---
title: "Migrating / Converting from Other Crate Plugins"
sidebar_position: 7
---

Switching to EcoCrates doesn't mean rebuilding every crate by hand. EcoCrates can convert your existing setup from several other crate plugins with a single command. This page lists the supported plugins and how to run a conversion.

## Supported plugins

Run the matching command while both EcoCrates and the source plugin are enabled. The source plugin must stay installed and enabled during the conversion so EcoCrates can read its data.

- **CrateReloaded:** `/ecocrates convert CrateReloaded`
- **CrazyCrates:** `/ecocrates convert CrazyCrates`
- **ExcellentCrates** (formerly GoldenCrates): `/ecocrates convert ExcellentCrates`
- **SpecializedCrates:** `/ecocrates convert SpecializedCrates`

## Requesting another converter

If your current crate plugin isn't listed, you can request a converter for it in our [Discord Server](https://hub.auxilor.io/discord).

<hr/>

## Where to go next

- **Review the result:** check your converted crates against [How to Make a Crate](how-to-make-a-crate).
- **Hand out keys:** give keys for your crates via [Commands and Permissions](commands-and-permissions).