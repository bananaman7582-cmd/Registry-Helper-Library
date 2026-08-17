package com.bananaman.api;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * A small helper for registering game objects (items, blocks, armor, and anything
 * else) against a vanilla {@link Registry} without repeating the same
 * {@link ResourceLocation} / {@link ResourceKey} boilerplate every time.
 *
 * <p>Every method that existed on the original version of this class keeps the exact
 * same name, parameters and return type, so existing code that uses it keeps
 * compiling unchanged. Everything else here is additive: extra overloads for the
 * common shortcuts (a plain {@link Item}, a block with no {@link BlockItem}, a full
 * armor set, a populated creative tab, block entities, entities, sound events, and
 * particles) so you reach for this class instead of hand-rolling registration again.
 *
 * <p>Targets Fabric for Minecraft 1.21.1 on Mojang mappings (no Yarn).
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
 *     // registers helmet/chestplate/leggings/boots in one call. In 1.21.1 an
 *     // ArmorMaterial is a registry object, so you pass a Holder<ArmorMaterial>
 *     // (e.g. ModArmorMaterials.RUBY, obtained from the ARMOR_MATERIAL registry)
 *     // rather than a bare ArmorMaterial instance.
 *     public static final RegistryHelper.ArmorSet RUBY_ARMOR =
 *             ITEMS.registerArmorSet("ruby", ModArmorMaterials.RUBY);
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
     * {@code PlacedFeature} - those are data-driven since 1.19.3 and belong in a
     * datapack (or a {@code DataGeneratorEntrypoint}), not in code.
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

    /** Original signature - builds the item from {@code factory}, using {@code properties} as the base. */
    public Item registerItem(
            String name,
            Function<Item.Properties, Item> factory,
            Item.Properties properties
    ) {
        ResourceKey<Item> key = itemKey(id(name));

        Item item = factory.apply(properties);
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
     * Original signature - registers the block and a matching {@link BlockItem} built
     * with fresh default {@link Item.Properties}, exactly like before.
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
     * custom item class. {@code itemFactory} receives the already-registered block.
     */
    public Block registerBlock(
            String name,
            Function<BlockBehaviour.Properties, Block> factory,
            BlockBehaviour.Properties blockProperties,
            BiFunction<Block, Item.Properties, Item> itemFactory,
            Item.Properties itemProperties
    ) {
        ResourceLocation resId = id(name);
        ResourceKey<Block> blockKey = blockKey(resId);
        ResourceKey<Item> itemKey = itemKey(resId);

        Block block = factory.apply(blockProperties);
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
        registeredBlocks.add(block);

        Item item = itemFactory.apply(block, itemProperties);
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);
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

        Block block = factory.apply(properties);
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
     * <p>In 1.21.1, vanilla's {@code BlockEntityType.Builder} is still public, so this
     * uses it directly rather than Fabric API's {@code FabricBlockEntityTypeBuilder}
     * (that builder only became necessary from 1.21.2 onward, once the vanilla builder
     * was made private). {@code build(null)} is intentional - the argument is a
     * datafixer {@code Type} that mods have no legitimate use for, and vanilla itself
     * passes {@code null} for every modded-style registration.
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
            BlockEntityType.BlockEntitySupplier<E> factory,
            Block... validBlocks
    ) {
        BlockEntityType<E> type = BlockEntityType.Builder.of(factory, validBlocks).build(null);
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id(name), type);
    }

    // -----------------------------------------------------------------
    // Entities
    // -----------------------------------------------------------------

    /**
     * Builds and registers an {@link EntityType} from {@code builder}.
     *
     * <p>In 1.21.1, {@code EntityType.Builder.build(...)} still takes a plain {@code String}
     * rather than a {@link ResourceKey} - the {@code ResourceKey}-accepting overload (needed
     * for the newer network-tracking system) wasn't added until 1.21.2. This passes {@code
     * name} itself; that string only matters for looking up a datafixer "choice type" for
     * saveable vanilla entities, so for a modded entity type it's fine as-is.
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
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id(name), builder.build(name));
    }

    // -----------------------------------------------------------------
    // Sound events
    // -----------------------------------------------------------------

    /** Registers a normal, distance-attenuated {@link SoundEvent} - what almost every sound in the game uses. */
    public SoundEvent registerSoundEvent(String name) {
        ResourceLocation resId = id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, resId, SoundEvent.createVariableRangeEvent(resId));
    }

    /**
     * Registers a {@link SoundEvent} that doesn't attenuate with distance past {@code range}
     * blocks - useful for UI sounds, ambience, etc. The engine caps {@code range} at 16
     * regardless of what's passed in.
     */
    public SoundEvent registerSoundEvent(String name, float range) {
        ResourceLocation resId = id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, resId, SoundEvent.createFixedRangeEvent(resId, range));
    }

    // -----------------------------------------------------------------
    // Particles
    // -----------------------------------------------------------------

    /** Registers a {@link SimpleParticleType} with no parameters - what most vanilla particles use. */
    public SimpleParticleType registerParticle(String name) {
        return registerParticle(name, false);
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
     * see {@link #registerCommand(String, int, Command)} if you need to restrict it.
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
     * Like {@link #registerCommand(String, Command)}, but only usable by sources whose
     * permission level is at least {@code permissionLevel}.
     *
     * <p>In 1.21.1, permission checks are still done through the plain integer OP-level
     * system via {@link CommandSourceStack#hasPermission(int)} - the {@code Permission}
     * object-based API doesn't exist yet. The vanilla OP tiers are 0 (everyone), 1
     * (bypass spawn protection), 2 (moderator - most commands, cheats), 3 (gamemaster -
     * multiplayer management), and 4 (admin - everything, including {@code /stop}).
     *
     * <pre>{@code
     * RegistryHelper.registerCommand("required_command", 2, context -> {
     *     context.getSource().sendSuccess(() -> Component.literal("Called it!"), false);
     *     return 1;
     * });
     * }</pre>
     */
    public static void registerCommand(String name, int permissionLevel, Command<CommandSourceStack> executor) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal(name)
                        .requires(source -> source.hasPermission(permissionLevel))
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

    /** Original signature - registers a single armor piece with default {@link Item.Properties}. */
    public Item registerArmor(String name, Holder<ArmorMaterial> material, ArmorItem.Type type) {
        return registerArmor(name, material, type, new Item.Properties());
    }

    /**
     * Like above, but lets you start from your own {@link Item.Properties} (durability, rarity, etc.).
     *
     * <p>In 1.21.1, {@code ArmorMaterial} is a registry object rather than a plain data
     * holder, so it's passed as a {@link Holder}. You get one of these back when you
     * register your own material into {@code Registries.ARMOR_MATERIAL} - see the vanilla
     * {@code ArmorMaterials} class for reference values on durability, protection, etc.
     */
    public Item registerArmor(
            String name,
            Holder<ArmorMaterial> material,
            ArmorItem.Type type,
            Item.Properties properties
    ) {
        ResourceKey<Item> key = itemKey(id(name));

        Item item = new ArmorItem(material, type, properties);
        Item registered = Registry.register(BuiltInRegistries.ITEM, key, item);
        registeredItems.add(registered);
        return registered;
    }

    /**
     * Registers a full set of armor - {@code baseName + "_helmet"}, {@code "_chestplate"},
     * {@code "_leggings"} and {@code "_boots"} - in one call.
     */
    public ArmorSet registerArmorSet(String baseName, Holder<ArmorMaterial> material) {
        return registerArmorSet(baseName, material, Item.Properties::new);
    }

    /** Like above, with a supplier for the base {@link Item.Properties} of every piece. */
    public ArmorSet registerArmorSet(
            String baseName,
            Holder<ArmorMaterial> material,
            Supplier<Item.Properties> properties
    ) {
        Item helmet = registerArmor(baseName + "_helmet", material, ArmorItem.Type.HELMET, properties.get());
        Item chestplate = registerArmor(baseName + "_chestplate", material, ArmorItem.Type.CHESTPLATE, properties.get());
        Item leggings = registerArmor(baseName + "_leggings", material, ArmorItem.Type.LEGGINGS, properties.get());
        Item boots = registerArmor(baseName + "_boots", material, ArmorItem.Type.BOOTS, properties.get());
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
     * items registered afterwards still show up.
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

        CreativeModeTab.Builder builder = FabricItemGroup.builder()
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
    // Internals
    // -----------------------------------------------------------------

    private ResourceLocation id(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Registration name cannot be null or blank (mod id: '" + modId + "')"
            );
        }
        return ResourceLocation.fromNamespaceAndPath(modId, name);
    }

    private ResourceKey<Item> itemKey(ResourceLocation id) {
        return ResourceKey.create(Registries.ITEM, id);
    }

    private ResourceKey<Block> blockKey(ResourceLocation id) {
        return ResourceKey.create(Registries.BLOCK, id);
    }

    private ResourceKey<CreativeModeTab> creativeTabKey(ResourceLocation id) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, id);
    }

    private void track(T element) {
        if (element instanceof Item item) {
            registeredItems.add(item);
        } else if (element instanceof Block block) {
            registeredBlocks.add(block);
        }
    }
}