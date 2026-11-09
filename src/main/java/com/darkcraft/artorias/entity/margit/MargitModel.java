package com.darkcraft.artorias.entity.margit;

import com.darkcraft.artorias.ArtoriasMod;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

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

    @Override
    public void setCustomAnimations(MargitEntity animatable, long instanceId, AnimationState<MargitEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        // In Elden Ring, Margit summons his luminous golden hammer ONLY for the Hammer Slam attack.
        // It dissolves back into spectral light during idle, walking, and staff/dagger attacks.
        getBone("golden_hammer").ifPresent(hammer -> {
            boolean showHammer = animatable.getAttackState() == MargitAttackState.HAMMER_SLAM;
            hammer.setHidden(!showHammer);
        });

        // Dynamic head tracking towards the player during idle / walking
        if (animatable.getAttackState() == MargitAttackState.IDLE) {
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
