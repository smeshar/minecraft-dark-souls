package com.darkcraft.artorias;

import com.darkcraft.artorias.combat.CombatEventListener;
import com.darkcraft.artorias.entity.ArtoriasEntity;
import com.darkcraft.artorias.entity.goblin.GoblinEntity;
import com.darkcraft.artorias.entity.goblin.GoblinKnightEntity;
import com.darkcraft.artorias.entity.omen.OmenEntity;
import com.darkcraft.artorias.entity.warg.WargEntity;
import com.darkcraft.artorias.item.AbyssalPendantItem;
import com.darkcraft.artorias.item.ArtoriasGreatswordItem;
import com.darkcraft.artorias.item.ArtoriasHelmItem;
import com.darkcraft.artorias.item.SoulRationItem;
import com.darkcraft.artorias.network.ModMessages;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.world.Heightmap;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.item.equipment.ArmorMaterials;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
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

    // Entities
    public static final EntityType<ArtoriasEntity> ARTORIAS_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            ARTORIAS_KEY,
            EntityType.Builder.create(ArtoriasEntity::new, SpawnGroup.MONSTER)
                    .dimensions(1.1f, 2.8f)
                    .maxTrackingRange(16)
                    .build(ARTORIAS_KEY)
    );

    public static final EntityType<WargEntity> GIANT_WARG_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            GIANT_WARG_KEY,
            EntityType.Builder.<WargEntity>create((type, world) -> new WargEntity(type, world, true), SpawnGroup.MONSTER)
                    .dimensions(1.4f, 1.4f)
                    .maxTrackingRange(16)
                    .build(GIANT_WARG_KEY)
    );

    public static final EntityType<WargEntity> LESSER_WARG_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            LESSER_WARG_KEY,
            EntityType.Builder.<WargEntity>create((type, world) -> new WargEntity(type, world, false), SpawnGroup.MONSTER)
                    .dimensions(0.9f, 0.9f)
                    .maxTrackingRange(16)
                    .build(LESSER_WARG_KEY)
    );

    public static final EntityType<OmenEntity> OMEN_CLEAVER_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            OMEN_CLEAVER_KEY,
            EntityType.Builder.<OmenEntity>create((type, world) -> new OmenEntity(type, world, OmenEntity.Variant.CLEAVER), SpawnGroup.MONSTER)
                    .dimensions(1.4f, 2.7f)
                    .maxTrackingRange(16)
                    .build(OMEN_CLEAVER_KEY)
    );

    public static final EntityType<OmenEntity> OMEN_BRAWLER_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            OMEN_BRAWLER_KEY,
            EntityType.Builder.<OmenEntity>create((type, world) -> new OmenEntity(type, world, OmenEntity.Variant.BRAWLER), SpawnGroup.MONSTER)
                    .dimensions(1.3f, 2.5f)
                    .maxTrackingRange(16)
                    .build(OMEN_BRAWLER_KEY)
    );

    public static final EntityType<GoblinEntity> GOBLIN_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            GOBLIN_KEY,
            EntityType.Builder.create(GoblinEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 1.3f)
                    .maxTrackingRange(16)
                    .build(GOBLIN_KEY)
    );

    public static final EntityType<GoblinKnightEntity> GOBLIN_KNIGHT_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            GOBLIN_KNIGHT_KEY,
            EntityType.Builder.create(GoblinKnightEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.7f, 1.4f)
                    .maxTrackingRange(16)
                    .build(GOBLIN_KNIGHT_KEY)
    );

    // Item Keys
    public static final RegistryKey<Item> ARTORIAS_GREATSWORD_KEY = RegistryKey.of(RegistryKeys.ITEM, id("artorias_greatsword"));
    public static final RegistryKey<Item> ABYSSAL_PENDANT_KEY = RegistryKey.of(RegistryKeys.ITEM, id("abyssal_pendant"));
    public static final RegistryKey<Item> SOUL_OF_ARTORIAS_KEY = RegistryKey.of(RegistryKeys.ITEM, id("soul_of_artorias"));
    public static final RegistryKey<Item> ARTORIAS_HELM_KEY = RegistryKey.of(RegistryKeys.ITEM, id("artorias_helm"));
    public static final RegistryKey<EquipmentAsset> ARTORIAS_HELM_ASSET_KEY = RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, id("artorias_helm"));
    public static final RegistryKey<Item> SOUL_RATION_KEY = RegistryKey.of(RegistryKeys.ITEM, id("soul_ration"));

    // Spawn Egg Keys
    public static final RegistryKey<Item> GIANT_WARG_SPAWN_EGG_KEY = RegistryKey.of(RegistryKeys.ITEM, id("giant_warg_spawn_egg"));
    public static final RegistryKey<Item> LESSER_WARG_SPAWN_EGG_KEY = RegistryKey.of(RegistryKeys.ITEM, id("lesser_warg_spawn_egg"));
    public static final RegistryKey<Item> OMEN_CLEAVER_SPAWN_EGG_KEY = RegistryKey.of(RegistryKeys.ITEM, id("omen_cleaver_spawn_egg"));
    public static final RegistryKey<Item> OMEN_BRAWLER_SPAWN_EGG_KEY = RegistryKey.of(RegistryKeys.ITEM, id("omen_brawler_spawn_egg"));
    public static final RegistryKey<Item> GOBLIN_SPAWN_EGG_KEY = RegistryKey.of(RegistryKeys.ITEM, id("goblin_spawn_egg"));
    public static final RegistryKey<Item> GOBLIN_KNIGHT_SPAWN_EGG_KEY = RegistryKey.of(RegistryKeys.ITEM, id("goblin_knight_spawn_egg"));

    // Items
    public static final Item ARTORIAS_GREATSWORD = Registry.register(
            Registries.ITEM,
            ARTORIAS_GREATSWORD_KEY,
            new ArtoriasGreatswordItem(new Item.Settings().registryKey(ARTORIAS_GREATSWORD_KEY).rarity(Rarity.EPIC).maxDamage(2500))
    );

    public static final Item ABYSSAL_PENDANT = Registry.register(
            Registries.ITEM,
            ABYSSAL_PENDANT_KEY,
            new AbyssalPendantItem(new Item.Settings().registryKey(ABYSSAL_PENDANT_KEY).rarity(Rarity.RARE).maxCount(1))
    );

    public static final Item SOUL_OF_ARTORIAS = Registry.register(
            Registries.ITEM,
            SOUL_OF_ARTORIAS_KEY,
            new Item(new Item.Settings().registryKey(SOUL_OF_ARTORIAS_KEY).rarity(Rarity.EPIC).maxCount(16))
    );

    public static final Item ARTORIAS_HELM = Registry.register(
            Registries.ITEM,
            ARTORIAS_HELM_KEY,
            new ArtoriasHelmItem(new Item.Settings()
                    .registryKey(ARTORIAS_HELM_KEY)
                    .armor(ArmorMaterials.DIAMOND, EquipmentType.HELMET)
                    .attributeModifiers(ArmorMaterials.DIAMOND.createAttributeModifiers(EquipmentType.HELMET)
                            .with(
                                    EntityAttributes.ATTACK_DAMAGE,
                                    new EntityAttributeModifier(
                                            id("artorias_helm_attack_damage"),
                                            5.0,
                                            EntityAttributeModifier.Operation.ADD_VALUE
                                    ),
                                    AttributeModifierSlot.HEAD
                            ))
                    .component(DataComponentTypes.EQUIPPABLE, EquippableComponent.builder(EquipmentSlot.HEAD)
                            .equipSound(ArmorMaterials.DIAMOND.equipSound())
                            .model(ARTORIAS_HELM_ASSET_KEY)
                            .build())
                    .rarity(Rarity.EPIC))
    );

    public static final Item SOUL_RATION = Registry.register(
            Registries.ITEM,
            SOUL_RATION_KEY,
            new SoulRationItem(new Item.Settings()
                    .registryKey(SOUL_RATION_KEY)
                    .food(new FoodComponent.Builder().nutrition(6).saturationModifier(1.2f).alwaysEdible().build())
                    .rarity(Rarity.UNCOMMON)
                    .maxCount(64))
    );

    // Spawn Eggs
    public static final Item GIANT_WARG_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            GIANT_WARG_SPAWN_EGG_KEY,
            new SpawnEggItem(new Item.Settings().spawnEgg(GIANT_WARG_ENTITY).registryKey(GIANT_WARG_SPAWN_EGG_KEY))
    );

    public static final Item LESSER_WARG_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            LESSER_WARG_SPAWN_EGG_KEY,
            new SpawnEggItem(new Item.Settings().spawnEgg(LESSER_WARG_ENTITY).registryKey(LESSER_WARG_SPAWN_EGG_KEY))
    );

    public static final Item OMEN_CLEAVER_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            OMEN_CLEAVER_SPAWN_EGG_KEY,
            new SpawnEggItem(new Item.Settings().spawnEgg(OMEN_CLEAVER_ENTITY).registryKey(OMEN_CLEAVER_SPAWN_EGG_KEY))
    );

    public static final Item OMEN_BRAWLER_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            OMEN_BRAWLER_SPAWN_EGG_KEY,
            new SpawnEggItem(new Item.Settings().spawnEgg(OMEN_BRAWLER_ENTITY).registryKey(OMEN_BRAWLER_SPAWN_EGG_KEY))
    );

    public static final Item GOBLIN_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            GOBLIN_SPAWN_EGG_KEY,
            new SpawnEggItem(new Item.Settings().spawnEgg(GOBLIN_ENTITY).registryKey(GOBLIN_SPAWN_EGG_KEY))
    );

    public static final Item GOBLIN_KNIGHT_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            GOBLIN_KNIGHT_SPAWN_EGG_KEY,
            new SpawnEggItem(new Item.Settings().spawnEgg(GOBLIN_KNIGHT_ENTITY).registryKey(GOBLIN_KNIGHT_SPAWN_EGG_KEY))
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
