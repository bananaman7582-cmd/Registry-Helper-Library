package com.bananaman.api;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.references.BlockItemId;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * A small helper for registering game objects (items, blocks, armor, and anything
 * else) against a vanilla {@link Registry} without repeating the same
 * {@link Identifier} / {@link ResourceKey} boilerplate every time.
 *
 * <p>Targets Fabric for Minecraft 26.2 on Mojang's official (unobfuscated) names.
 * Since 26.1 the game ships unobfuscated and Yarn is gone, so {@code ResourceLocation}
 * is now {@link Identifier} and Fabric API's own classes were renamed to match
 * (for example {@code FabricItemGroup} is now {@link FabricCreativeModeTab}).
 *
 * <h2>What changed since the 1.21.1 version of this class</h2>
 * <ul>
 *   <li>{@code ResourceLocation} → {@link Identifier} ({@code net.minecraft.resources}).</li>
 *   <li>Ids are now baked into the settings object: every item must be built from
 *       {@code Item.Properties#setId(ResourceKey)} and every block from
 *       {@code BlockBehaviour.Properties#setId(ResourceKey)}. This helper does that
 *       for you, so a caller still just passes a plain {@code new Item.Properties()}.</li>
 *   <li>{@code ArmorItem} is gone. Armor is now an ordinary {@link Item} carrying the
 *       {@code humanoidArmor} property, {@link ArmorMaterial} is a plain record (no
 *       {@code Holder}), and {@code ArmorItem.Type} is now {@link ArmorType}.</li>
 *   <li>{@code EntityType.Builder#build} takes the {@link ResourceKey}, not a {@code String}.</li>
 *   <li>Vanilla's {@code BlockEntityType.Builder} is no longer usable by mods, so block
 *       entities go through {@link FabricBlockEntityTypeBuilder}.</li>
 *   <li>Command permissions moved off raw OP integers, so the permission overload now
 *       takes a {@link Predicate} of the source instead of an {@code int}.</li>
 * </ul>
 *
 * <h2>Quick start</h2>
 * <pre>{@code
 * public class ModItems {
 *     public static final RegistryHelper<Item> ITEMS = RegistryHelper.items(MOD_ID);
 *
 *     public static final Item RUBY = ITEMS.registerItem("ruby", new Item.Properties());
 *
 *     // registers a tab that auto-populates with everything ITEMS has registered
 *     public static final CreativeModeTab RUBY_TAB =
 *             ITEMS.registerCreativeTab("ruby_tab", () -> new ItemStack(RUBY));
 * }
 *
 * public class ModBlocks {
 *     public static final RegistryHelper<Block> BLOCKS = RegistryHelper.blocks(MOD_ID);
 *
 *     // registers the block AND a matching BlockItem for it
 *     public static final Block RUBY_BLOCK =
 *             BLOCKS.registerBlock("ruby_block", Block::new, BlockBehaviour.Properties.of());
 *
 *     public static final BlockEntityType<RubyBlockEntity> RUBY_BLOCK_ENTITY =
 *             BLOCKS.registerBlockEntityType("ruby_block", RubyBlockEntity::new, RUBY_BLOCK);
 * }
 *
 * public class ModArmor {
 *     private static final RegistryHelper<Item> ITEMS = RegistryHelper.items(MOD_ID);
 *
 *     // registers helmet/chestplate/leggings/boots in one call. ArmorMaterial is a
 *     // plain record now, so you pass the instance directly, plus the base durability
 *     // the material was built with (ArmorType scales it per slot for you).
 *     public static final RegistryHelper.ArmorSet RUBY_ARMOR =
 *             ITEMS.registerArmorSet("ruby", ModArmorMaterials.INSTANCE, ModArmorMaterials.BASE_DURABILITY);
 * }
 *
 * public class ModEntities {
 *     public static final RegistryHelper<EntityType<?>> ENTITY_TYPES = RegistryHelper.entityTypes(MOD_ID);
 *
 *     public static final EntityType<RubyGolemEntity> RUBY_GOLEM = ENTITY_TYPES.registerEntityType(
 *             "ruby_golem",
 *             EntityType.Builder.of(RubyGolemEntity::new, MobCategory.CREATURE).sized(1.0f, 2.0f)
 *     );
 * }
 *
 * public class ModSounds {
 *     private static final RegistryHelper<SoundEvent> SOUNDS = RegistryHelper.soundEvents(MOD_ID);
 *
 *     public static final SoundEvent RUBY_CHIME = SOUNDS.registerSoundEvent("ruby_chime");
 * }
 *
 * public class ModParticles {
 *     private static final RegistryHelper<ParticleType<?>> PARTICLES = RegistryHelper.particleTypes(MOD_ID);
 *
 *     public static final SimpleParticleType RUBY_SPARKLE = PARTICLES.registerParticle("ruby_sparkle");
 * }
 *
 * public class ModEffects {
 *     private static final RegistryHelper<MobEffect> EFFECTS = RegistryHelper.mobEffects(MOD_ID);
 *
 *     public static final MobEffect RUBY_SHIELD = EFFECTS.register("ruby_shield", new RubyShieldEffect(...));
 * }
 *
 * public class ModFeatures {
 *     private static final RegistryHelper<Feature<?>> FEATURES = RegistryHelper.features(MOD_ID);
 *
 *     // the ConfiguredFeature/PlacedFeature that uses this still belongs in a datapack
 *     public static final RubyOreFeature RUBY_ORE = FEATURES.register("ruby_ore", new RubyOreFeature(...));
 * }
 *
 * public class ModCommands {
 *     public static void register() {
 *         RegistryHelper.registerCommand("heal", context -> {
 *             context.getSource().sendSuccess(() -> Component.literal("Healed!"), false);
 *             return 1;
 *         });
 *     }
 * }
 * }</pre>
 *
 * @param <T> the type of the registry this instance wraps (e.g. {@code Item}, {@code Block})
 */
public class RegistryHelper<T> {

    private final Registry<T> registry;
    private final String modId;

    private final List<Item> registeredItems = new ArrayList<>();
    private final List<Block> registeredBlocks = new ArrayList<>();

    public RegistryHelper(
            @NotNull Registry<T> registry,
            @NotNull String modId
    ) {
        this.registry = Objects.requireNonNull(registry, "Registry cannot be null");
        this.modId = Objects.requireNonNull(modId, "Mod ID cannot be null");

        if (this.modId.isBlank()) {
            throw new IllegalArgumentException("Mod ID cannot be blank");
        }
    }

    // -----------------------------------------------------------------
    // Static factories - the two registries almost every mod needs.
    // For anything else, just use the constructor directly, e.g.
    // new RegistryHelper<>(BuiltInRegistries.SOUND_EVENT, MOD_ID)
    // -----------------------------------------------------------------

    /** A {@code RegistryHelper} bound to the item registry for {@code modId}. */
    public static RegistryHelper<Item> items(@NotNull String modId) {
        return new RegistryHelper<>(BuiltInRegistries.ITEM, modId);
    }

    /** A {@code RegistryHelper} bound to the block registry for {@code modId}. */
    public static RegistryHelper<Block> blocks(@NotNull String modId) {
        return new RegistryHelper<>(BuiltInRegistries.BLOCK, modId);
    }

    /** A {@code RegistryHelper} bound to the block entity type registry for {@code modId}. */
    public static RegistryHelper<BlockEntityType<?>> blockEntityTypes(@NotNull String modId) {
        return new RegistryHelper<>(BuiltInRegistries.BLOCK_ENTITY_TYPE, modId);
    }

    /** A {@code RegistryHelper} bound to the entity type registry for {@code modId}. */
    public static RegistryHelper<EntityType<?>> entityTypes(@NotNull String modId) {
        return new RegistryHelper<>(BuiltInRegistries.ENTITY_TYPE, modId);
    }

    /** A {@code RegistryHelper} bound to the sound event registry for {@code modId}. */
    public static RegistryHelper<SoundEvent> soundEvents(@NotNull String modId) {
        return new RegistryHelper<>(BuiltInRegistries.SOUND_EVENT, modId);
    }

    /** A {@code RegistryHelper} bound to the particle type registry for {@code modId}. */
    public static RegistryHelper<ParticleType<?>> particleTypes(@NotNull String modId) {
        return new RegistryHelper<>(BuiltInRegistries.PARTICLE_TYPE, modId);
    }

    /** A {@code RegistryHelper} bound to the mob effect (status effect / potion effect) registry for {@code modId}. */
    public static RegistryHelper<MobEffect> mobEffects(@NotNull String modId) {
        return new RegistryHelper<>(BuiltInRegistries.MOB_EFFECT, modId);
    }

    /**
     * A {@code RegistryHelper} bound to the feature registry for {@code modId}.
     *
     * <p>This registers the code-level {@link Feature} type itself (the class that
     * implements placement logic), the same way you'd register a custom {@link Block}
     * or {@link Item} class. It does <b>not</b> register a {@code ConfiguredFeature} or
     * {@code PlacedFeature} - those are data-driven and belong in a datapack (or a
     * {@code DataGeneratorEntrypoint}), not in code.
     */
    public static RegistryHelper<Feature<?>> features(@NotNull String modId) {
        return new RegistryHelper<>(BuiltInRegistries.FEATURE, modId);
    }

    /**
     * A {@code RegistryHelper} bound to the structure type registry for {@code modId}.
     *
     * <p>Same idea as {@link #features}: this registers the code-level {@link StructureType},
     * not an actual {@code Structure} instance (village, mineshaft, your own custom
     * structure config, etc.) - those are also data-driven and belong in a datapack.
     */
    public static RegistryHelper<StructureType<?>> structureTypes(@NotNull String modId) {
        return new RegistryHelper<>(BuiltInRegistries.STRUCTURE_TYPE, modId);
    }

    // -----------------------------------------------------------------
    // Generic registration - works for any registry this instance wraps
    // -----------------------------------------------------------------

    /** Registers {@code element} under {@code name} in this helper's registry. */
    public T register(String name, T element) {
        Objects.requireNonNull(element, "Element to register cannot be null");

        T registered = Registry.register(registry, id(name), element);
        track(registered);
        return registered;
    }

    // -----------------------------------------------------------------
    // Items
    // -----------------------------------------------------------------

    /**
     * Builds the item from {@code factory}, using {@code properties} as the base.
     *
     * <p>The registry id is applied to {@code properties} via {@code setId} before the
     * factory runs, which the game requires as of 26.x. Note that {@code Item.Properties}
     * is mutable and now carries an id, so don't reuse one instance across two items -
     * hand each registration its own {@code new Item.Properties()}.
     */
    public Item registerItem(
            String name,
            Function<Item.Properties, Item> factory,
            Item.Properties properties
    ) {
        ResourceKey<Item> key = itemKey(id(name));

        Item item = factory.apply(properties.setId(key));
        Item registered = Registry.register(BuiltInRegistries.ITEM, key, item);
        registeredItems.add(registered);
        return registered;
    }

    /** Like {@link #registerItem(String, Function, Item.Properties)}, with fresh default properties. */
    public Item registerItem(String name, Function<Item.Properties, Item> factory) {
        return registerItem(name, factory, new Item.Properties());
    }

    /** Registers a plain {@link Item} - the common case for crafting materials, etc. */
    public Item registerItem(String name, Item.Properties properties) {
        return registerItem(name, Item::new, properties);
    }

    /** Registers a plain {@link Item} with default properties. */
    public Item registerItem(String name) {
        return registerItem(name, Item::new, new Item.Properties());
    }

    // -----------------------------------------------------------------
    // Blocks
    // -----------------------------------------------------------------

    /**
     * Registers the block and a matching {@link BlockItem} built with fresh default
     * {@link Item.Properties}.
     */
    public Block registerBlock(
            String name,
            Function<BlockBehaviour.Properties, Block> factory,
            BlockBehaviour.Properties properties
    ) {
        return registerBlock(name, factory, properties, new Item.Properties());
    }

    /** Like above, but lets you customize the properties of the generated {@link BlockItem}. */
    public Block registerBlock(
            String name,
            Function<BlockBehaviour.Properties, Block> factory,
            BlockBehaviour.Properties blockProperties,
            Item.Properties itemProperties
    ) {
        return registerBlock(name, factory, blockProperties, BlockItem::new, itemProperties);
    }

    /**
     * Like above, but lets you supply the {@link Item} yourself instead of a plain
     * {@link BlockItem} - useful for signs, tall blocks, or anything that needs a
     * custom item class. {@code itemFactory} receives the already-registered block and
     * an {@code Item.Properties} that already has its id and block-description prefix set.
     *
     * <p>Vanilla now keeps block ids and block-item ids in separate lookup classes
     * ({@code BlockIds} / {@code BlockItemIds}); {@link #blockItemId(String)} gives you the
     * paired {@link BlockItemId} if you want to hold onto it for data generation.
     */
    public Block registerBlock(
            String name,
            Function<BlockBehaviour.Properties, Block> factory,
            BlockBehaviour.Properties blockProperties,
            BiFunction<Block, Item.Properties, Item> itemFactory,
            Item.Properties itemProperties
    ) {
        BlockItemId ids = blockItemId(name);

        Block block = factory.apply(blockProperties.setId(ids.block()));
        Registry.register(BuiltInRegistries.BLOCK, ids.block(), block);
        registeredBlocks.add(block);

        Item item = itemFactory.apply(block, itemProperties.useBlockDescriptionPrefix().setId(ids.item()));
        Registry.register(BuiltInRegistries.ITEM, ids.item(), item);
        registeredItems.add(item);

        return block;
    }

    /** Registers a block using default {@link BlockBehaviour.Properties#of()}. */
    public Block registerBlock(String name, Function<BlockBehaviour.Properties, Block> factory) {
        return registerBlock(name, factory, BlockBehaviour.Properties.of());
    }

    /** Registers a block with no {@link BlockItem} at all - e.g. fire, portals, technical blocks. */
    public Block registerBlockWithoutItem(
            String name,
            Function<BlockBehaviour.Properties, Block> factory,
            BlockBehaviour.Properties properties
    ) {
        ResourceKey<Block> blockKey = blockKey(id(name));

        Block block = factory.apply(properties.setId(blockKey));
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
        registeredBlocks.add(block);
        return block;
    }

    // -----------------------------------------------------------------
    // Block entities
    // -----------------------------------------------------------------

    /**
     * Registers a {@link BlockEntityType} for {@code factory}, valid on {@code validBlocks}.
     *
     * <p>Vanilla's {@code BlockEntityType.Builder} is no longer available to mods, so this
     * goes through Fabric API's {@link FabricBlockEntityTypeBuilder}, which also drops the
     * old {@code build(null)} datafixer argument entirely.
     *
     * <pre>{@code
     * public static final RegistryHelper<Block> BLOCKS = RegistryHelper.blocks(MOD_ID);
     *
     * public static final Block RUBY_FURNACE = BLOCKS.registerBlock("ruby_furnace", RubyFurnaceBlock::new);
     * public static final BlockEntityType<RubyFurnaceBlockEntity> RUBY_FURNACE_BE =
     *         BLOCKS.registerBlockEntityType("ruby_furnace", RubyFurnaceBlockEntity::new, RUBY_FURNACE);
     * }</pre>
     *
     * @param <E> the {@link BlockEntity} subtype being registered
     */
    public <E extends BlockEntity> BlockEntityType<E> registerBlockEntityType(
            String name,
            FabricBlockEntityTypeBuilder.Factory<? extends E> factory,
            Block... validBlocks
    ) {
        BlockEntityType<E> type = FabricBlockEntityTypeBuilder.<E>create(factory, validBlocks).build();
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id(name), type);
    }

    // -----------------------------------------------------------------
    // Entities
    // -----------------------------------------------------------------

    /**
     * Builds and registers an {@link EntityType} from {@code builder}.
     *
     * <p>{@code EntityType.Builder#build} now takes the entity type's
     * {@link ResourceKey} rather than a plain {@code String}, so this creates the key
     * once and uses it for both the build and the registration.
     *
     * <pre>{@code
     * public static final RegistryHelper<EntityType<?>> ENTITY_TYPES = RegistryHelper.entityTypes(MOD_ID);
     *
     * public static final EntityType<RubyGolemEntity> RUBY_GOLEM = ENTITY_TYPES.registerEntityType(
     *         "ruby_golem",
     *         EntityType.Builder.of(RubyGolemEntity::new, MobCategory.CREATURE).sized(1.0f, 2.0f)
     * );
     * }</pre>
     *
     * @param <E> the {@link Entity} subtype being registered
     */
    public <E extends Entity> EntityType<E> registerEntityType(String name, EntityType.Builder<E> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id(name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    // -----------------------------------------------------------------
    // Sound events
    // -----------------------------------------------------------------

    /** Registers a normal, distance-attenuated {@link SoundEvent} - what almost every sound in the game uses. */
    public SoundEvent registerSoundEvent(String name) {
        Identifier soundId = id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, soundId, SoundEvent.createVariableRangeEvent(soundId));
    }

    /**
     * Registers a {@link SoundEvent} that doesn't attenuate with distance past {@code range}
     * blocks - useful for UI sounds, ambience, etc. The engine caps {@code range} at 16
     * regardless of what's passed in.
     */
    public SoundEvent registerSoundEvent(String name, float range) {
        Identifier soundId = id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, soundId, SoundEvent.createFixedRangeEvent(soundId, range));
    }

    // -----------------------------------------------------------------
    // Particles
    // -----------------------------------------------------------------

    /** Registers a {@link SimpleParticleType} with no parameters - what most vanilla particles use. */
    public SimpleParticleType registerParticle(String name) {
        SimpleParticleType particle = FabricParticleTypes.simple();
        return Registry.register(BuiltInRegistries.PARTICLE_TYPE, id(name), particle);
    }

    /**
     * Like {@link #registerParticle(String)}, but lets you set {@code alwaysShow} - whether
     * this particle still spawns when the Particles video setting is set to Minimal. Vanilla
     * sets this {@code true} for things like explosions, campfire smoke, and squid ink.
     */
    public SimpleParticleType registerParticle(String name, boolean alwaysShow) {
        SimpleParticleType particle = FabricParticleTypes.simple(alwaysShow);
        return Registry.register(BuiltInRegistries.PARTICLE_TYPE, id(name), particle);
    }

    // -----------------------------------------------------------------
    // Commands
    // -----------------------------------------------------------------
    //
    // Unlike everything above, a command isn't registered into a vanilla Registry<T> -
    // it's built against a CommandDispatcher inside Fabric API's CommandRegistrationCallback,
    // which fires whenever the server (re)builds its command tree. That makes these static
    // and mod-id-free; call them directly, e.g. RegistryHelper.registerCommand(...), rather
    // than through an instance.

    /**
     * Registers a single, argument-less command - {@code /name} - available to every
     * player, that runs {@code executor} when called. No permission check at all -
     * see {@link #registerCommand(String, Predicate, Command)} if you need to restrict it.
     *
     * <pre>{@code
     * RegistryHelper.registerCommand("heal", context -> {
     *     context.getSource().sendSuccess(() -> Component.literal("Healed!"), false);
     *     return 1;
     * });
     * }</pre>
     */
    public static void registerCommand(String name, Command<CommandSourceStack> executor) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal(name).executes(executor))
        );
    }

    /**
     * Like {@link #registerCommand(String, Command)}, but only usable by sources that
     * satisfy {@code requirement}.
     *
     * <p>Permissions are no longer raw OP integers: {@code CommandSourceStack} exposes a
     * {@code permissions()} view that is queried with the constants on
     * {@code net.minecraft.server.permissions.Permissions}, so the check is passed in as a
     * predicate rather than an {@code int} level. Sources that fail the check don't see the
     * command in tab completion at all.
     *
     * <pre>{@code
     * RegistryHelper.registerCommand(
     *         "required_command",
     *         source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR),
     *         context -> {
     *             context.getSource().sendSuccess(() -> Component.literal("Called it!"), false);
     *             return 1;
     *         });
     * }</pre>
     */
    public static void registerCommand(
            String name,
            Predicate<CommandSourceStack> requirement,
            Command<CommandSourceStack> executor
    ) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal(name)
                        .requires(requirement)
                        .executes(executor))
        );
    }

    /**
     * Escape hatch for anything beyond a flat command - arguments, subcommands, redirects,
     * or a custom {@code requires(...)} check - by handing you the freshly created
     * {@link LiteralArgumentBuilder} to configure and return yourself, instead of building
     * it for you.
     *
     * <pre>{@code
     * RegistryHelper.registerCommand("give_ruby", literal -> literal
     *         .then(Commands.argument("amount", IntegerArgumentType.integer(1))
     *                 .executes(context -> {
     *                     int amount = IntegerArgumentType.getInteger(context, "amount");
     *                     // ... give the player `amount` rubies ...
     *                     return 1;
     *                 })));
     * }</pre>
     */
    public static void registerCommand(
            String name,
            UnaryOperator<LiteralArgumentBuilder<CommandSourceStack>> builder
    ) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(builder.apply(Commands.literal(name)))
        );
    }

    // -----------------------------------------------------------------
    // Armor
    // -----------------------------------------------------------------

    /**
     * Registers a single armor piece.
     *
     * <p>There is no {@code ArmorItem} class any more - an armor piece is a plain
     * {@link Item} whose properties carry {@code humanoidArmor(material, type)}, and
     * {@link ArmorMaterial} is a plain record rather than a registry object, so you pass
     * the instance itself instead of a {@code Holder}.
     *
     * <p>{@code ArmorMaterial} carries no durability, so pass the same
     * {@code baseDurability} you built the material with; {@link ArmorType#getDurability(int)}
     * scales it per slot (boots and helmets get less than a chestplate, as in vanilla).
     */
    public Item registerArmor(String name, ArmorMaterial material, ArmorType type, int baseDurability) {
        return registerItem(
                name,
                Item::new,
                new Item.Properties()
                        .humanoidArmor(material, type)
                        .durability(type.getDurability(baseDurability))
        );
    }

    /**
     * Like above, but lets you start from your own {@link Item.Properties} (rarity, extra
     * components, a hand-picked durability, etc.). The armor property is applied on top of
     * whatever you pass in, so anything you set yourself is preserved.
     */
    public Item registerArmor(
            String name,
            ArmorMaterial material,
            ArmorType type,
            Item.Properties properties
    ) {
        return registerItem(name, Item::new, properties.humanoidArmor(material, type));
    }

    /**
     * Registers a full set of armor - {@code baseName + "_helmet"}, {@code "_chestplate"},
     * {@code "_leggings"} and {@code "_boots"} - in one call.
     */
    public ArmorSet registerArmorSet(String baseName, ArmorMaterial material, int baseDurability) {
        Item helmet = registerArmor(baseName + "_helmet", material, ArmorType.HELMET, baseDurability);
        Item chestplate = registerArmor(baseName + "_chestplate", material, ArmorType.CHESTPLATE, baseDurability);
        Item leggings = registerArmor(baseName + "_leggings", material, ArmorType.LEGGINGS, baseDurability);
        Item boots = registerArmor(baseName + "_boots", material, ArmorType.BOOTS, baseDurability);
        return new ArmorSet(helmet, chestplate, leggings, boots);
    }

    /**
     * Like above, with a supplier for the base {@link Item.Properties} of every piece.
     * The supplier must hand back a <em>fresh</em> instance each call, since properties are
     * mutable and each piece stamps its own id onto them.
     */
    public ArmorSet registerArmorSet(
            String baseName,
            ArmorMaterial material,
            Supplier<Item.Properties> properties
    ) {
        Item helmet = registerArmor(baseName + "_helmet", material, ArmorType.HELMET, properties.get());
        Item chestplate = registerArmor(baseName + "_chestplate", material, ArmorType.CHESTPLATE, properties.get());
        Item leggings = registerArmor(baseName + "_leggings", material, ArmorType.LEGGINGS, properties.get());
        Item boots = registerArmor(baseName + "_boots", material, ArmorType.BOOTS, properties.get());
        return new ArmorSet(helmet, chestplate, leggings, boots);
    }

    /** The four pieces produced by {@link #registerArmorSet}. */
    public record ArmorSet(Item helmet, Item chestplate, Item leggings, Item boots) {
        public List<Item> asList() {
            return List.of(helmet, chestplate, leggings, boots);
        }
    }

    // -----------------------------------------------------------------
    // Creative tabs
    // -----------------------------------------------------------------

    /**
     * Builds and registers a {@link CreativeModeTab} that displays every {@link Item} this
     * helper has registered - plain items, block items, and armor pieces alike - by reading
     * {@link #getRegisteredItems()}.
     *
     * <p>The display callback reads that list lazily, at populate-time rather than
     * registration-time, so it's fine to call this before you've registered everything else -
     * items registered afterwards still show up. The icon is a {@link Supplier} for the same
     * reason: an {@link ItemStack} can't be constructed before a world is loaded.
     *
     * <pre>{@code
     * public static final RegistryHelper<Item> ITEMS = RegistryHelper.items(MOD_ID);
     * // ... register items, blocks, armor via ITEMS/BLOCKS as usual ...
     *
     * public static final CreativeModeTab RUBY_TAB =
     *         ITEMS.registerCreativeTab("ruby_tab", () -> new ItemStack(RUBY));
     * }</pre>
     */
    public CreativeModeTab registerCreativeTab(String name, Supplier<ItemStack> icon) {
        return registerCreativeTab(name, icon, builder -> {});
    }

    /**
     * Like {@link #registerCreativeTab(String, Supplier)}, but hands you the underlying
     * {@link CreativeModeTab.Builder} so you can customize it further - reorder entries with
     * your own {@code displayItems}, enable a search bar, etc. - before this helper's "show
     * everything registered" default is applied. {@code customizer} runs last, so it can
     * override any of the defaults set here.
     */
    public CreativeModeTab registerCreativeTab(
            String name,
            Supplier<ItemStack> icon,
            Consumer<CreativeModeTab.Builder> customizer
    ) {
        ResourceKey<CreativeModeTab> key = creativeTabKey(id(name));

        CreativeModeTab.Builder builder = FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + modId + "." + name))
                .icon(icon)
                .displayItems((parameters, output) -> registeredItems.forEach(output::accept));

        customizer.accept(builder);

        return Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, key, builder.build());
    }

    // -----------------------------------------------------------------
    // Bookkeeping - handy for populating a creative tab, etc.
    // -----------------------------------------------------------------

    /** Every {@link Item} registered through this instance (plain items, block items, armor). */
    public List<Item> getRegisteredItems() {
        return List.copyOf(registeredItems);
    }

    /** Every {@link Block} registered through this instance. */
    public List<Block> getRegisteredBlocks() {
        return List.copyOf(registeredBlocks);
    }

    public String getModId() {
        return modId;
    }

    public Registry<T> getRegistry() {
        return registry;
    }

    // -----------------------------------------------------------------
    // Ids
    // -----------------------------------------------------------------

    /** The namespaced {@link Identifier} this helper would use for {@code name}. */
    public Identifier id(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Registration name cannot be null or blank (mod id: '" + modId + "')"
            );
        }
        return Identifier.fromNamespaceAndPath(modId, name);
    }

    /**
     * The paired block/block-item ids for {@code name}, matching how vanilla now keeps
     * them apart in {@code BlockIds} and {@code BlockItemIds}. Useful to hold onto for tag
     * and model data generation, which wants the ids rather than the {@code Block} instance.
     */
    public BlockItemId blockItemId(String name) {
        Identifier resId = id(name);
        return BlockItemId.create(resId, resId);
    }

    private ResourceKey<Item> itemKey(Identifier id) {
        return ResourceKey.create(Registries.ITEM, id);
    }

    private ResourceKey<Block> blockKey(Identifier id) {
        return ResourceKey.create(Registries.BLOCK, id);
    }

    private ResourceKey<CreativeModeTab> creativeTabKey(Identifier id) {
        return ResourceKey.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), id);
    }

    private void track(T element) {
        if (element instanceof Item item) {
            registeredItems.add(item);
        } else if (element instanceof Block block) {
            registeredBlocks.add(block);
        }
    }
}