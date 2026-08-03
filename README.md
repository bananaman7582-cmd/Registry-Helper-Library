# Registry Helper Library

A small Fabric utility class for Minecraft **1.21.11** that wraps Minecraft's registry system — items, blocks, block entities, entities, sound events, particles, armor sets, and creative tabs — into a handful of clean, overloaded methods, so your `ModItems` / `ModBlocks` / etc. classes stop repeating the same `Identifier` / `ResourceKey` boilerplate over and over.

Built for **Fabric Loader + Fabric API**, on **Mojang's official mappings** (no Yarn).

## Why

Registering things in modern Minecraft means building an `Identifier`, wrapping it in a `ResourceKey`, and calling `Registry.register(...)` — and for blocks, armor, entities, and block entities, doing several of those steps more than once per object. `RegistryHelper` collapses each of those into a single call, while still leaving every vanilla builder option available when you need it.

## Features

- **Items** — plain items, or any custom `Item` subclass, with or without custom `Item.Properties`
- **Blocks** — registers the block *and* a matching `BlockItem` in one call, with an option to skip the item entirely (fire, portals, technical blocks) or supply a custom item class
- **Block entities** — wraps `BlockEntityType.Builder` so you don't have to remember its `build(null)` datafixer argument
- **Entities** — wraps `EntityType.Builder`, handling the "the builder needs its own key before it can build" ordering problem for you
- **Armor** — single pieces, or a full helmet/chestplate/leggings/boots set in one call
- **Sound events** — variable-range (the vanilla default) or fixed-range, one line each
- **Particles** — simple particle types, with or without the "ignore the Minimal particles setting" flag
- **Creative tabs** — builds and registers a `CreativeModeTab` that auto-populates from everything the helper has already registered
- **Bookkeeping** — every registered `Item` and `Block` is tracked automatically, so you can hand them to a creative tab, a loot table, a recipe viewer category, etc. without keeping a second list yourself
- Every method signature from the original version of this class is preserved — this is a drop-in upgrade, not a rewrite

## Requirements

| | |
|---|---|
| Minecraft | 1.21.11 |
| Mappings | Mojang (official) — **not** Yarn |
| Mod loader | Fabric Loader + Fabric API |
| Java | 21+ |
| Fabric Loom | 1.14+ (per Fabric's own 1.21.11 dev guidance) |

Fabric API is required specifically for `registerCreativeTab` (`FabricItemGroup`) and `registerParticle` (`FabricParticleTypes`). Everything else only touches vanilla classes.

## Installation

This is currently distributed as a single source file rather than a packaged dependency. To use it:

1. Download [`RegistryHelper.java`](RegistryHelper.java) from this repo.
2. Drop it into your mod's source tree, and update the `package` declaration at the top to match your own mod's package.
3. Make sure your `build.gradle` is already set up for Fabric Loader, Fabric API, and Minecraft 1.21.11 on Mojang mappings, e.g.:

    ```groovy
    dependencies {
        minecraft "com.mojang:minecraft:1.21.11"
        mappings loom.officialMojangMappings()
        modImplementation "net.fabricmc:fabric-loader:<loader_version>"
        modImplementation "net.fabricmc.fabric-api:fabric-api:<fabric_api_version>"
    }
    ```

No extra Maven repositories or coordinates needed — it's just a `.java` file.

## Usage

### Items

```java
public class ModItems {
    public static final RegistryHelper<Item> ITEMS = RegistryHelper.items(MOD_ID);

    public static final Item RUBY = ITEMS.registerItem("ruby", new Item.Properties());
    public static final Item RUBY_SWORD = ITEMS.registerItem("ruby_sword", RubySwordItem::new,
            new Item.Properties().durability(750));
}
```

### Blocks (+ block entity)

```java
public class ModBlocks {
    public static final RegistryHelper<Block> BLOCKS = RegistryHelper.blocks(MOD_ID);

    // registers the block AND a matching BlockItem
    public static final Block RUBY_BLOCK = BLOCKS.registerBlock("ruby_block", Block::new,
            BlockBehaviour.Properties.of());

    // no BlockItem at all - for fire, portals, technical blocks, etc.
    public static final Block RUBY_PORTAL = BLOCKS.registerBlockWithoutItem("ruby_portal",
            RubyPortalBlock::new, BlockBehaviour.Properties.of().noCollission());

    public static final BlockEntityType<RubyFurnaceBlockEntity> RUBY_FURNACE_BE =
            BLOCKS.registerBlockEntityType("ruby_furnace", RubyFurnaceBlockEntity::new, RUBY_BLOCK);
}
```

### Armor

```java
public class ModArmor {
    private static final RegistryHelper<Item> ITEMS = RegistryHelper.items(MOD_ID);

    // registers helmet, chestplate, leggings, and boots in one call
    public static final RegistryHelper.ArmorSet RUBY_ARMOR =
            ITEMS.registerArmorSet("ruby", ModArmorMaterials.RUBY);
}
```

### Entities

```java
public class ModEntities {
    public static final RegistryHelper<EntityType<?>> ENTITY_TYPES = RegistryHelper.entityTypes(MOD_ID);

    public static final EntityType<RubyGolemEntity> RUBY_GOLEM = ENTITY_TYPES.registerEntityType(
            "ruby_golem",
            EntityType.Builder.of(RubyGolemEntity::new, MobCategory.CREATURE).sized(1.0f, 2.0f)
    );
}
```

### Sound events & particles

```java
public class ModSounds {
    private static final RegistryHelper<SoundEvent> SOUNDS = RegistryHelper.soundEvents(MOD_ID);

    public static final SoundEvent RUBY_CHIME = SOUNDS.registerSoundEvent("ruby_chime");
}

public class ModParticles {
    private static final RegistryHelper<ParticleType<?>> PARTICLES = RegistryHelper.particleTypes(MOD_ID);

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

`registerCreativeTab` populates the tab from every item `ModItems.ITEMS` has registered so far — plain items, block items, and armor alike — read lazily at populate-time, so it doesn't matter whether the tab is built before or after the rest of your items.

## Backwards compatibility

Every method that shipped in the original version of this class keeps its exact name, parameters, and return type. Existing code that already uses `RegistryHelper` will keep compiling unchanged.

## License

No license has been published for this repository yet. Until one is added, treat the source as all-rights-reserved and check with the maintainer before redistributing it.

## Contributing

Issues and pull requests are welcome at [github.com/bananaman7582-cmd/Registry-Helper-Library](https://github.com/bananaman7582-cmd/Registry-Helper-Library).
