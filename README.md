# Registry Helper Library

A tiny Fabric helper class for Minecraft 1.21.11 that encapsulates Minecraft's entire registry system — items, blocks, block entities, entities, sound events, particles, armor sets, and creative tabs — into a few overloaded methods, allowing your `ModItems` / `ModBlocks` / etc. classes to avoid the repetitive `Identifier` / `ResourceKey` registration boilerplate.

Compatible with Fabric Loader + Fabric API, using Mojang's official mappings (not Yarn).

## Why

Modern Minecraft registration requires creating an `Identifier`, wrapping it in a `ResourceKey`, and registering it via `Registry.register(...)` — plus, for blocks, armor, entities, and block entities, repeating this process multiple times per entity. `RegistryHelper` reduces each step down to a single method call, without compromising any vanilla builder options.

## Features

- Items — normal items or any subclass of `Item` with or without custom `Item.Properties`
- Blocks — registers the block and its corresponding `BlockItem` in a single method, plus an option for fire, portals, technical blocks, etc. or for a custom item class
- Block entities — wraps `BlockEntityType.Builder` and relieves you from remembering its `build(null)` datafixer parameter
- Entities — wraps `EntityType.Builder`, solving the "the builder needs its own key before it can build" problem for you
- Armor — single pieces or a whole set of a helmet, chest plate, leggings, and boots
- Sound events — variable-range (vanilla default) or fixed-range, one method call each
- Particles — standard particle types, optionally ignoring the Minimal particles setting
- Creative tabs — builds and registers a `CreativeModeTab` with items that `RegistryHelper` has already registered
- Bookkeeping — all registered `Item`s and `Block`s are kept track of automatically, allowing you to pass them to a creative tab, a loot table, a recipe viewer category, etc. without maintaining another list yourself
- Every signature from the original version of this class has been preserved, this is a drop-in replacement, not a complete rewrite

## Requirements
| Minecraft | 1.21.11 |
| Mappings | Mojang (official) — not Yarn |
| Mod loader | Fabric Loader + Fabric API |
| Java | 21+ |
| Fabric Loom | 1.14+ (per Fabric's own 1.21.11 dev guidance) |

Fabric API is necessary only for `registerCreativeTab` (`FabricItemGroup`) and `registerParticle` (`FabricParticleTypes`). Everything else deals exclusively with vanilla classes.

## Installation
Registry Helper Library comes fully packaged as a dependency through [JitPack](https://jitpack.io), compiled right out of this repo's source and releases. You do not need to create your own Maven publication. To install it:
1. Declare JitPack as a repository in your `build.gradle`:
```groovy
repositories {
maven { url "https://jitpack.io" }
}
```
2. Add the library as a dependency along with Fabric Loader and Fabric API for Minecraft 1.21.11 on Mojang mappings:
```groovy
dependencies {
minecraft "com.mojang:minecraft:1.21.11"
mappings loom.officialMojangMappings()
modImplementation "net.fabricmc:fabric-loader:"
modImplementation "net.fabricmc.fabric-api:fabric-api:"

modImplementation "com.github.bananaman7582-cmd:Registry-Helper-Library:Release"
}
```
3. Sync Gradle. The first time you resolve a given tag from JitPack, it will take a few moments to compile, then it will be cached. After that just import and use `RegistryHelper` like any other mod dependency — there is no need to copy the library's source files or rename its package.
## Usage

### Items

```java
public class ModItems {
public static final RegistryHelper ITEMS = RegistryHelper.items(MOD_ID);

public static final Item RUBY = ITEMS.registerItem("ruby", new Item.Properties());
public static final Item RUBY_SWORD = ITEMS.registerItem("ruby_sword", RubySwordItem::new,
new Item.Properties().durability(750));
}
```

### Blocks (+ block entity)

```java
public class ModBlocks {
public static final RegistryHelper BLOCKS = RegistryHelper.blocks(MOD_ID);

// registers the block AND a matching BlockItem
public static final Block RUBY_BLOCK = BLOCKS.registerBlock("ruby_block", Block::new,
BlockBehaviour.Properties.of());

// no BlockItem at all - for fire, portals, technical blocks, etc.
public static final Block RUBY_PORTAL = BLOCKS.registerBlockWithoutItem("ruby_portal",
RubyPortalBlock::new, BlockBehaviour.Properties.of().noCollission());

public static final BlockEntityType RUBY_FURNACE_BE =
BLOCKS.registerBlockEntityType("ruby_furnace", RubyFurnaceBlockEntity::new, RUBY_BLOCK);
}
```

### Armor

```java
public class ModArmor {
private static final RegistryHelper ITEMS = RegistryHelper.items(MOD_ID);

// registers helmet, chestplate, leggings, and boots in one call
public static final RegistryHelper.ArmorSet RUBY_ARMOR =
ITEMS.registerArmorSet("ruby", ModArmorMaterials.RUBY);
}
```

### Entities

```java
public class ModEntities {
public static final RegistryHelper> ENTITY_TYPES = RegistryHelper.entityTypes(MOD_ID);

public static final EntityType RUBY_GOLEM = ENTITY_TYPES.registerEntityType(
"ruby_golem",
EntityType.Builder.of(RubyGolemEntity::new, MobCategory.CREATURE).sized(1.0f, 2.0f)
);
}
```

### Sound events & particles

```java
public class ModSounds {
private static final RegistryHelper SOUNDS = RegistryHelper.soundEvents(MOD_ID);

public static final SoundEvent RUBY_CHIME = SOUNDS.registerSoundEvent("ruby_chime");
}

public class ModParticles {
private static final RegistryHelper> PARTICLES = RegistryHelper.particleTypes(MOD_ID);

public static final SimpleParticleType RUBY_SPARKLE = PARTICLES.registerParticle("ruby_sparkle");
}
```

### Creative tab

```java
public class ModItemGroups {
public static final CreativeModeTab RUBY_TAB =
ModItems.ITEMS.registerCreativeTab("ruby_tab", () -> new ItemStack(ModItems.RUBY));
}
```

The `registerCreativeTab` method populates the creative tab from all the items registered by `ModItems.ITEMS` — plain items, block items, armor items, etc. — reading lazily at populate time, so it does not matter whether the creative tab is created before or after your items.

## Backwards compatibility

Every method that was present in the original version of this class retains its exact name, parameters, and return type. Code which already uses `RegistryHelper` will keep working without any changes.

## License

There is no LICENSE published for this repo yet. Until one is provided, consider the source copyrighted and get permission from the maintainer before distributing it.

## Contributing

Any issues and pull requests are welcome at [github.com/bananaman7582-cmd/Registry-Helper-Library](https://github.com/bananaman7582-cmd/Registry-Helper-Library).
