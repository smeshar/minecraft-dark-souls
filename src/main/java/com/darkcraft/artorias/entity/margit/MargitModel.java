package com.darkcraft.artorias.entity.margit;

import com.darkcraft.artorias.ArtoriasMod;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib GeoModel for Margit, the Fell Omen.
 * Links to geo/entity/margit.geo.json and animations/entity/margit.animation.json.
 */
public class MargitModel extends GeoModel<MargitEntity> {
    private static final Identifier MODEL = ArtoriasMod.id("geo/entity/margit.geo.json");
    private static final Identifier TEXTURE = ArtoriasMod.id("textures/entity/margit.png");
    private static final Identifier ANIMATION = ArtoriasMod.id("animations/entity/margit.animation.json");

    @Override
    public Identifier getModelResource(MargitEntity animatable) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(MargitEntity animatable) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(MargitEntity animatable) {
        return ANIMATION;
    }
}
