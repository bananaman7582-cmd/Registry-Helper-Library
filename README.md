# Registry Helper Library

Tired of writing the same `Identifier` -> `ResourceKey` -> `Registry.register(...)` boilerplate every time you add an item, block, entity, sound, whatever? This little class collapses it all down to one method call per thing, without taking away any of the vanilla builder options underneath.

Built for Fabric Loader + Fabric API, on Mojang's official mappings (not Yarn).

## Requirements

| Mappings | Mojang official (not Yarn) |
|---|---|
| Mod loader | Fabric Loader + Fabric API |
| Java | 21+ |
| Fabric Loom | whatever's current for your Minecraft version |

Fabric API is only needed for `registerCreativeTab`, `registerParticle`, and the command helpers. Everything else is plain vanilla.

## Installation

Add JitPack as a repository in `build.gradle`:

```groovy
repositories {
    maven { url "https://jitpack.io" }
}
```

Then add the dependency, alongside Fabric Loader and Fabric API on Mojang mappings:

```groovy
dependencies {
    minecraft "com.mojang:minecraft:<version>"
    mappings loom.officialMojangMappings()
    modImplementation "net.fabricmc:fabric-loader:<version>"
    modImplementation "net.fabricmc.fabric-api:fabric-api:<version>"

    modImplementation "com.github.bananaman7582-cmd:Registry-Helper-Library:<Version>"
}
```

Sync Gradle. First build takes a minute while JitPack compiles it, then it's cached.

## How to use it

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

### Effects

```java
public class ModEffects {
    private static final RegistryHelper<MobEffect> EFFECTS = RegistryHelper.mobEffects(MOD_ID);

    public static final MobEffect RUBY_SHIELD = EFFECTS.register("ruby_shield", new RubyShieldEffect(...));
}
```

### Commands

```java
public class ModCommands {
    public static void register() {
        RegistryHelper.registerCommand("heal", context -> {
            context.getSource().sendSuccess(() -> Component.literal("Healed!"), false);
            return 1;
        });
    }
}
```

Need arguments or subcommands? Use the overload that hands you the builder instead:

```java
RegistryHelper.registerCommand("give_ruby", literal -> literal
        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                .executes(context -> {
                    int amount = IntegerArgumentType.getInteger(context, "amount");
                    // ... give the player `amount` rubies ...
                    return 1;
                })));
```

### Creative tab

```java
public class ModItemGroups {
    public static final CreativeModeTab RUBY_TAB =
            ModItems.ITEMS.registerCreativeTab("ruby_tab", () -> new ItemStack(ModItems.RUBY));
}
```

`registerCreativeTab` populates itself from everything `ModItems.ITEMS` has registered (items, block items, armor, all of it) and reads that list lazily at populate time, so it doesn't matter whether you set up the tab before or after the rest of your items.

## Backwards compatibility

Every method that existed on an earlier version of this class keeps its exact name, parameters, and return type. Updating won't break your build.

## Contributing

Issues and PRs welcome: [github.com/bananaman7582-cmd/Registry-Helper-Library](https://github.com/bananaman7582-cmd/Registry-Helper-Library).
