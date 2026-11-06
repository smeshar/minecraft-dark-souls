package com.darkcraft.artorias;

import com.darkcraft.artorias.combat.CombatEventListener;
import com.darkcraft.artorias.entity.artorias.ArtoriasEntity;
import com.darkcraft.artorias.entity.goblin.GoblinEntity;
import com.darkcraft.artorias.entity.goblin.GoblinKnightEntity;
import com.darkcraft.artorias.entity.margit.MargitDaggerEntity;
import com.darkcraft.artorias.entity.margit.MargitEntity;
import com.darkcraft.artorias.entity.omen.OmenEntity;
import com.darkcraft.artorias.entity.warg.WargEntity;
import com.darkcraft.artorias.item.AbyssalPendantItem;
import com.darkcraft.artorias.item.ArtoriasGreatswordItem;
import com.darkcraft.artorias.item.ArtoriasHelmItem;
import com.darkcraft.artorias.item.MargitShackleItem;
import com.darkcraft.artorias.item.SoulRationItem;
import com.darkcraft.artorias.network.ModMessages;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.world.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArtoriasMod implements ModInitializer {
    public static final String MOD_ID = "artorias";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Entity Registry Keys
    public static final RegistryKey<EntityType<?>> ARTORIAS_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, id("knight_artorias"));
    public static final RegistryKey<EntityType<?>> GIANT_WARG_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, id("giant_warg"));
    public static final RegistryKey<EntityType<?>> LESSER_WARG_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, id("lesser_warg"));
    public static final RegistryKey<EntityType<?>> OMEN_CLEAVER_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, id("omen_cleaver"));
    public static final RegistryKey<EntityType<?>> OMEN_BRAWLER_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, id("omen_brawler"));
    public static final RegistryKey<EntityType<?>> GOBLIN_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, id("goblin"));
    public static final RegistryKey<EntityType<?>> GOBLIN_KNIGHT_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, id("goblin_knight"));
    public static final RegistryKey<EntityType<?>> MARGIT_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, id("margit_the_fell_omen"));
    public static final RegistryKey<EntityType<?>> MARGIT_DAGGER_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, id("margit_spectral_dagger"));

    // Entities
    public static final EntityType<ArtoriasEntity> ARTORIAS_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            id("knight_artorias"),
            EntityType.Builder.create(ArtoriasEntity::new, SpawnGroup.MONSTER)
                    .dimensions(1.1f, 2.8f)
                    .maxTrackingRange(16)
                    .build("knight_artorias")
    );

    public static final EntityType<WargEntity> GIANT_WARG_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            id("giant_warg"),
            EntityType.Builder.<WargEntity>create((type, world) -> new WargEntity(type, world, true), SpawnGroup.MONSTER)
                    .dimensions(1.4f, 1.4f)
                    .maxTrackingRange(16)
                    .build("giant_warg")
    );

    public static final EntityType<WargEntity> LESSER_WARG_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            id("lesser_warg"),
            EntityType.Builder.<WargEntity>create((type, world) -> new WargEntity(type, world, false), SpawnGroup.MONSTER)
                    .dimensions(0.9f, 0.9f)
                    .maxTrackingRange(16)
                    .build("lesser_warg")
    );

    public static final EntityType<OmenEntity> OMEN_CLEAVER_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            id("omen_cleaver"),
            EntityType.Builder.<OmenEntity>create((type, world) -> new OmenEntity(type, world, OmenEntity.Variant.CLEAVER), SpawnGroup.MONSTER)
                    .dimensions(1.4f, 2.7f)
                    .maxTrackingRange(16)
                    .build("omen_cleaver")
    );

    public static final EntityType<OmenEntity> OMEN_BRAWLER_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            id("omen_brawler"),
            EntityType.Builder.<OmenEntity>create((type, world) -> new OmenEntity(type, world, OmenEntity.Variant.BRAWLER), SpawnGroup.MONSTER)
                    .dimensions(1.3f, 2.5f)
                    .maxTrackingRange(16)
                    .build("omen_brawler")
    );

    public static final EntityType<GoblinEntity> GOBLIN_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            id("goblin"),
            EntityType.Builder.create(GoblinEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 1.3f)
                    .maxTrackingRange(16)
                    .build("goblin")
    );

    public static final EntityType<GoblinKnightEntity> GOBLIN_KNIGHT_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            id("goblin_knight"),
            EntityType.Builder.create(GoblinKnightEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.7f, 1.4f)
                    .maxTrackingRange(16)
                    .build("goblin_knight")
    );

    public static final EntityType<MargitEntity> MARGIT_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            id("margit_the_fell_omen"),
            EntityType.Builder.create(MargitEntity::new, SpawnGroup.MONSTER)
                    .dimensions(1.4f, 3.2f)
                    .maxTrackingRange(24)
                    .build("margit_the_fell_omen")
    );

    public static final EntityType<MargitDaggerEntity> MARGIT_DAGGER_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            id("margit_spectral_dagger"),
            EntityType.Builder.<MargitDaggerEntity>create(MargitDaggerEntity::new, SpawnGroup.MISC)
                    .dimensions(0.4f, 0.4f)
                    .maxTrackingRange(16)
                    .trackingTickInterval(1)
                    .build("margit_spectral_dagger")
    );

    // Items
    public static final Item ARTORIAS_GREATSWORD = Registry.register(
            Registries.ITEM,
            id("artorias_greatsword"),
            new ArtoriasGreatswordItem(new Item.Settings().rarity(Rarity.EPIC).maxDamage(2500))
    );

    public static final Item ABYSSAL_PENDANT = Registry.register(
            Registries.ITEM,
            id("abyssal_pendant"),
            new AbyssalPendantItem(new Item.Settings().rarity(Rarity.RARE).maxCount(1))
    );

    public static final Item SOUL_OF_ARTORIAS = Registry.register(
            Registries.ITEM,
            id("soul_of_artorias"),
            new Item(new Item.Settings().rarity(Rarity.EPIC).maxCount(16))
    );

    public static final Item ARTORIAS_HELM = Registry.register(
            Registries.ITEM,
            id("artorias_helm"),
            new ArtoriasHelmItem(new Item.Settings().rarity(Rarity.EPIC).maxDamage(363))
    );

    public static final Item SOUL_RATION = Registry.register(
            Registries.ITEM,
            id("soul_ration"),
            new SoulRationItem(new Item.Settings()
                    .food(new FoodComponent.Builder().nutrition(6).saturationModifier(1.2f).alwaysEdible().build())
                    .rarity(Rarity.UNCOMMON)
                    .maxCount(64))
    );

    public static final Item MARGIT_SHACKLE = Registry.register(
            Registries.ITEM,
            id("margits_shackle"),
            new MargitShackleItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC))
    );

    // Spawn Eggs
    public static final Item GIANT_WARG_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            id("giant_warg_spawn_egg"),
            new SpawnEggItem(GIANT_WARG_ENTITY, 0x2b2b2b, 0x8b0000, new Item.Settings())
    );

    public static final Item LESSER_WARG_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            id("lesser_warg_spawn_egg"),
            new SpawnEggItem(LESSER_WARG_ENTITY, 0x4a4a4a, 0x6a0000, new Item.Settings())
    );

    public static final Item OMEN_CLEAVER_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            id("omen_cleaver_spawn_egg"),
            new SpawnEggItem(OMEN_CLEAVER_ENTITY, 0x5a4a3a, 0xd4af37, new Item.Settings())
    );

    public static final Item OMEN_BRAWLER_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            id("omen_brawler_spawn_egg"),
            new SpawnEggItem(OMEN_BRAWLER_ENTITY, 0x3d332a, 0xb8860b, new Item.Settings())
    );

    public static final Item GOBLIN_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            id("goblin_spawn_egg"),
            new SpawnEggItem(GOBLIN_ENTITY, 0x2e472e, 0x8b7355, new Item.Settings())
    );

    public static final Item GOBLIN_KNIGHT_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            id("goblin_knight_spawn_egg"),
            new SpawnEggItem(GOBLIN_KNIGHT_ENTITY, 0x1f301f, 0xc0c0c0, new Item.Settings())
    );

    public static final Item MARGIT_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            id("margit_spawn_egg"),
            new SpawnEggItem(MARGIT_ENTITY, 0x4a3b2c, 0xffd700, new Item.Settings())
    );

    @Override
    public void onInitialize() {
        LOGGER.info("Knight Artorias Mod initialized! Prepare for Dark Souls combat!");

        // Register Entity Attributes
        FabricDefaultAttributeRegistry.register(ARTORIAS_ENTITY, ArtoriasEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(GIANT_WARG_ENTITY, WargEntity.createGiantAttributes());
        FabricDefaultAttributeRegistry.register(LESSER_WARG_ENTITY, WargEntity.createLesserAttributes());
        FabricDefaultAttributeRegistry.register(OMEN_CLEAVER_ENTITY, OmenEntity.createCleaverAttributes());
        FabricDefaultAttributeRegistry.register(OMEN_BRAWLER_ENTITY, OmenEntity.createBrawlerAttributes());
        FabricDefaultAttributeRegistry.register(GOBLIN_ENTITY, GoblinEntity.createGoblinAttributes());
        FabricDefaultAttributeRegistry.register(GOBLIN_KNIGHT_ENTITY, GoblinKnightEntity.createGoblinKnightAttributes());
        FabricDefaultAttributeRegistry.register(MARGIT_ENTITY, MargitEntity.createAttributes());

        // Register Natural World Spawning
        registerSpawnRestrictions();
        registerBiomeSpawns();

        ModMessages.registerC2SPackets();
        CombatEventListener.register();

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(ARTORIAS_GREATSWORD);
            entries.add(ARTORIAS_HELM);
            entries.add(ABYSSAL_PENDANT);
            entries.add(SOUL_OF_ARTORIAS);
            entries.add(SOUL_RATION);
            entries.add(MARGIT_SHACKLE);
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(entries -> {
            entries.add(SOUL_RATION);
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(entries -> {
            entries.add(GIANT_WARG_SPAWN_EGG);
            entries.add(LESSER_WARG_SPAWN_EGG);
            entries.add(OMEN_CLEAVER_SPAWN_EGG);
            entries.add(OMEN_BRAWLER_SPAWN_EGG);
            entries.add(GOBLIN_SPAWN_EGG);
            entries.add(GOBLIN_KNIGHT_SPAWN_EGG);
            entries.add(MARGIT_SPAWN_EGG);
        });
    }

    private static void registerSpawnRestrictions() {
        SpawnRestriction.register(GIANT_WARG_ENTITY, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(LESSER_WARG_ENTITY, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(OMEN_CLEAVER_ENTITY, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(OMEN_BRAWLER_ENTITY, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(GOBLIN_ENTITY, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        SpawnRestriction.register(GOBLIN_KNIGHT_ENTITY, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
    }

    private static void registerBiomeSpawns() {
        // Wargs spawn in Taigas, Mountains, and Forests
        var wargBiomes = BiomeSelectors.tag(BiomeTags.IS_TAIGA)
                .or(BiomeSelectors.tag(BiomeTags.IS_MOUNTAIN))
                .or(BiomeSelectors.tag(BiomeTags.IS_FOREST));
        BiomeModifications.addSpawn(wargBiomes, SpawnGroup.MONSTER, LESSER_WARG_ENTITY, 35, 2, 4);
        BiomeModifications.addSpawn(wargBiomes, SpawnGroup.MONSTER, GIANT_WARG_ENTITY, 12, 1, 2);

        // Goblins spawn across all dark Overworld areas (forests, plains, caves)
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_OVERWORLD), SpawnGroup.MONSTER, GOBLIN_ENTITY, 40, 2, 4);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_OVERWORLD), SpawnGroup.MONSTER, GOBLIN_KNIGHT_ENTITY, 15, 1, 2);

        // Sewer Omens spawn in Overworld dark areas (caves, deep underground, swamps)
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_OVERWORLD), SpawnGroup.MONSTER, OMEN_BRAWLER_ENTITY, 12, 1, 2);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_OVERWORLD), SpawnGroup.MONSTER, OMEN_CLEAVER_ENTITY, 8, 1, 1);
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
