package com.darkcraft.artorias.entity.artorias;

import com.darkcraft.artorias.ArtoriasMod;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/**
 * GeckoLib GeoModel for Knight Artorias, the Abysswalker.
 * Links to geo/entity/artorias.geo.json and animations/entity/artorias.animation.json.
 */
public class ArtoriasModel extends GeoModel<ArtoriasEntity> {
    private static final Identifier MODEL = ArtoriasMod.id("geo/entity/artorias.geo.json");
    private static final Identifier TEXTURE = ArtoriasMod.id("textures/entity/artorias.png");
    private static final Identifier ANIMATION = ArtoriasMod.id("animations/entity/artorias.animation.json");

    @Override
    public Identifier getModelResource(ArtoriasEntity animatable) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(ArtoriasEntity animatable) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(ArtoriasEntity animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(ArtoriasEntity animatable, long instanceId, AnimationState<ArtoriasEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        // Dynamic head tracking towards the target during idle and walking
        if (animatable.getAttackState() == ArtoriasEntity.STATE_IDLE) {
            getBone("head").ifPresent(head -> {
                EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
                if (entityData != null) {
                    head.setRotX(head.getRotX() + entityData.headPitch() * MathHelper.RADIANS_PER_DEGREE);
                    head.setRotY(head.getRotY() + entityData.netHeadYaw() * MathHelper.RADIANS_PER_DEGREE);
                }
            });
        }
    }
}
