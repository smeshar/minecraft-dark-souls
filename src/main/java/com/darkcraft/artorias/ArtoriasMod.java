package com.darkcraft.artorias;

import com.darkcraft.artorias.combat.CombatEventListener;
import com.darkcraft.artorias.entity.ArtoriasEntity;
import com.darkcraft.artorias.item.AbyssalPendantItem;
import com.darkcraft.artorias.item.ArtoriasGreatswordItem;
import com.darkcraft.artorias.network.ModMessages;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
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

    public static final RegistryKey<EntityType<?>> ARTORIAS_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            id("knight_artorias")
    );

    public static final EntityType<ArtoriasEntity> ARTORIAS_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            ARTORIAS_KEY,
            EntityType.Builder.create(ArtoriasEntity::new, SpawnGroup.MONSTER)
                    .dimensions(1.1f, 2.8f)
                    .maxTrackingRange(16)
                    .build(ARTORIAS_KEY)
    );

    public static final RegistryKey<Item> ARTORIAS_GREATSWORD_KEY = RegistryKey.of(RegistryKeys.ITEM, id("artorias_greatsword"));
    public static final RegistryKey<Item> ABYSSAL_PENDANT_KEY = RegistryKey.of(RegistryKeys.ITEM, id("abyssal_pendant"));
    public static final RegistryKey<Item> SOUL_OF_ARTORIAS_KEY = RegistryKey.of(RegistryKeys.ITEM, id("soul_of_artorias"));

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

    @Override
    public void onInitialize() {
        LOGGER.info("Knight Artorias Mod initialized! Prepare for Dark Souls combat!");

        FabricDefaultAttributeRegistry.register(ARTORIAS_ENTITY, ArtoriasEntity.createAttributes());

        ModMessages.registerC2SPackets();
        CombatEventListener.register();

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(ARTORIAS_GREATSWORD);
            entries.add(ABYSSAL_PENDANT);
            entries.add(SOUL_OF_ARTORIAS);
        });
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
