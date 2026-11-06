package com.darkcraft.artorias.entity.margit;

import com.darkcraft.artorias.ArtoriasMod;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public class MargitModel extends GeoModel<MargitEntity> {
    private static final Identifier MODEL_RESOURCE = ArtoriasMod.id("geo/entity/margit.geo.json");
    private static final Identifier TEXTURE_RESOURCE = ArtoriasMod.id("textures/entity/margit.png");
    private static final Identifier ANIMATION_RESOURCE = ArtoriasMod.id("animations/entity/margit.animation.json");

    @Override
    public Identifier getModelResource(MargitEntity animatable, GeoRenderer<MargitEntity> renderer) {
        return MODEL_RESOURCE;
    }

    @Override
    public Identifier getTextureResource(MargitEntity animatable, GeoRenderer<MargitEntity> renderer) {
        return TEXTURE_RESOURCE;
    }

    @Override
    public Identifier getAnimationResource(MargitEntity animatable) {
        return ANIMATION_RESOURCE;
    }
}
