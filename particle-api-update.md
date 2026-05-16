# Particle API Migration Guide: `master` → `feat/particle_overhaul`

---

## 1. `SpawnableParticle.spawn()` — signature completely changed

**Old:**
```java
void spawn(@NotNull Location location, int amount);
void spawn(@NotNull Location location); // default — calls spawn(location, 1)
```

**New:**
```java
// Primary method (must implement if you have a custom SpawnableParticle)
@NotNull Cancellable spawn(@NotNull Location location,
                           @NotNull PlaceholderContext context,
                           @NotNull ParticleAudience audience);

// Convenience overloads (all provided as defaults — no changes needed if just calling)
@NotNull Cancellable spawn(@NotNull Location location);
@NotNull Cancellable spawn(@NotNull Location location, @NotNull PlaceholderContext context);
@NotNull Cancellable spawn(@NotNull Location location, @NotNull ParticleAudience audience);
```

Migration rules:
- The `amount` parameter is **gone entirely**. Remove it from all call sites.
- Return type is now `@NotNull Cancellable` instead of `void`. Callers that previously ignored the return can still ignore it. Callers that need to stop an animation should hold the handle and call `setCancelled(true)`.
- `spawn(location)` still compiles and works — just remove the `amount` arg from 2-arg calls: `spawn(location, 5)` → `spawn(location)`.
- If you **implement** `SpawnableParticle` directly, update the method signature. The old 2-arg method no longer exists on the interface; it will become dead code unless you remove it.

---

## 2. `Particles` registry — complete rewrite

**Old:**
```java
// Registering custom particle types via a factory
Particles.registerParticleFactory(factory); // factory implements ParticleFactory

// Lookup from a string like "FLAME" or "rgb:ff0000"
SpawnableParticle p = Particles.lookup("FLAME");
SpawnableParticle p = Particles.lookup("rgb:ff0000");
```

**New:**
```java
// Register a pre-built SpawnableParticle instance directly
Particles.register(new NamespacedKey(plugin, "my_particle"), myParticle);
Particles.register(new NamespacedKey(plugin, "my_particle"), myParticle, ownerPlugin); // with owner for diagnostics

// Lookup by NamespacedKey (returns EmptyParticle.INSTANCE on miss, never null)
SpawnableParticle p = Particles.lookup(new NamespacedKey(plugin, "my_particle"));

// Lookup by String still exists — delegates to a backend string resolver installed by core
// This is for YAML/config-style strings, not for plugin-registered particles
SpawnableParticle p = Particles.lookup("myplugin:my_particle");

// Nullable lookup
@Nullable SpawnableParticle p = Particles.find(new NamespacedKey(plugin, "my_particle"));

// Unregister
Particles.unregister(new NamespacedKey(plugin, "my_particle"));

// Config-based loading (for plugins that define particles in YAML)
Particles.loadFromConfig(ecoPlugin, "particles");  // loads from plugin's config section
Particles.reloadConfigs();                         // clears CONFIG-origin entries and reloads all

// Diagnostics
List<RegisteredParticle> all = Particles.dump();
Map<NamespacedKey, RegisteredParticle> view = Particles.registryView();
```

Migration rules:
- Replace `registerParticleFactory(factory)` → pre-build particle instances and call `register(key, particle)` for each.
- Replace `lookup(String)` with `lookup(NamespacedKey)` wherever you own the key. String lookup still works for config-driven strings but now goes through the backend resolver, not `ParticleFactory`.
- Namespace all keys: `new NamespacedKey(yourPlugin, "particle_name")`.

---

## 3. `ParticleFactory` — deleted

The `com.willfp.eco.core.particle.ParticleFactory` interface is removed entirely.

**Before:**
```java
public class MyParticleFactory implements ParticleFactory {
    @Override public List<String> getNames() { return List.of("mytype"); }
    @Override public @Nullable SpawnableParticle create(@NotNull String key) { ... }
}
Particles.registerParticleFactory(new MyParticleFactory());
// Used via: Particles.lookup("mytype:somekey")
```

**After:**
Build each `SpawnableParticle` eagerly and register it with a `NamespacedKey` directly. There is no lazy factory mechanism anymore.

```java
SpawnableParticle p = new MySpawnableParticle(...);
Particles.register(new NamespacedKey(plugin, "mytype_somekey"), p);
```

---

## 4. `SimpleParticle` — deleted

`com.willfp.eco.core.particle.impl.SimpleParticle` is removed. It was the built-in wrapper around a `Bukkit Particle` enum value. The backend now handles all primitive particle type rendering internally; you no longer construct `SimpleParticle` instances.

If your code created `SimpleParticle` directly, replace it with a `Particles.lookup()` call for the relevant particle name, or define a custom `SpawnableParticle` implementation.

---

## 5. `EmptyParticle` — minor (binary compatible)

The singleton is now available as `EmptyParticle.INSTANCE`. The no-arg constructor is `@Deprecated` but still present for binary compatibility — existing `new EmptyParticle()` calls still compile. The `spawn()` signature is updated to the new interface.

---

## 6. New types introduced

| Type | Purpose |
|---|---|
| `ParticleAudience` | Functional interface — resolves which `Player`s see a spawn. Constants: `DEFAULT`, `WORLD`, `CONTEXT_PLAYER`. Factory methods: `player(p)`, `players(col)`, `except(p)`, `within(radius)`. Pass `ParticleAudience.DEFAULT` to use the runtime-configured audience. |
| `ParticleOrigin` | Enum: `PLUGIN` (programmatic) or `CONFIG` (YAML). Controls reload semantics — `CONFIG` entries are cleared on `reloadConfigs()`. |
| `RegisteredParticle` | Record: `(NamespacedKey key, SpawnableParticle particle, ParticleOrigin origin, @Nullable Plugin owner)`. Returned from `dump()` / `registryView()`. |

---

## Quick reference: call-site cheat sheet

| Old call | New equivalent |
|---|---|
| `particle.spawn(loc, 1)` | `particle.spawn(loc)` |
| `particle.spawn(loc, n)` | `particle.spawn(loc)` (amount removed) |
| `particle.spawn(loc)` | `particle.spawn(loc)` (unchanged, now returns `Cancellable`) |
| `Particles.lookup("FLAME")` | `Particles.lookup(new NamespacedKey(plugin, "flame"))` or `Particles.lookup("myplugin:flame")` via string resolver |
| `Particles.registerParticleFactory(f)` | Build instances; call `Particles.register(key, particle)` per entry |
| `new SimpleParticle(Particle.FLAME)` | Use config-driven lookup or implement `SpawnableParticle` |
| `new EmptyParticle()` | `EmptyParticle.INSTANCE` (constructor still works) |
