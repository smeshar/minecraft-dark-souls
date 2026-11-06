package com.darkcraft.artorias.entity.goblin;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.util.math.MathHelper;

public class GoblinModel extends BipedEntityModel<GoblinRenderState> {
    private final ModelPart buckler;
    private final ModelPart dagger;
    private final ModelPart helm;

    public GoblinModel(ModelPart root) {
        super(root);
        this.buckler = this.leftArm.getChild("buckler");
        this.dagger = this.rightArm.getChild("dagger");
        this.helm = this.head.getChild("helm");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = BipedEntityModel.getModelData(Dilation.NONE, 0.0f);
        ModelPartData root = modelData.getRoot();

        // 1. Pointed goblin ears, crooked nose, and knight helm
        ModelPartData head = root.getChild(EntityModelPartNames.HEAD);
        head.addChild("right_ear", ModelPartBuilder.create()
                        .uv(24, 0).cuboid(-6.5f, -3.0f, 0.0f, 3.0f, 2.0f, 1.0f),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        head.addChild("left_ear", ModelPartBuilder.create()
                        .uv(24, 0).cuboid(3.5f, -3.0f, 0.0f, 3.0f, 2.0f, 1.0f),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        head.addChild("nose", ModelPartBuilder.create()
                        .uv(24, 4).cuboid(-1.0f, -1.0f, -5.5f, 2.0f, 2.0f, 2.0f),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        head.addChild("helm", ModelPartBuilder.create()
                        .uv(32, 0).cuboid(-4.5f, -8.5f, -4.5f, 9.0f, 9.0f, 9.0f, new Dilation(0.2f)),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        // 2. Weapons: Dagger on right arm, Spiked Buckler on left arm
        ModelPartData rightArm = root.getChild(EntityModelPartNames.RIGHT_ARM);
        rightArm.addChild("dagger", ModelPartBuilder.create()
                        .uv(0, 32).cuboid(-0.5f, 8.0f, -4.0f, 1.0f, 2.0f, 6.0f),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        ModelPartData leftArm = root.getChild(EntityModelPartNames.LEFT_ARM);
        leftArm.addChild("buckler", ModelPartBuilder.create()
                        .uv(16, 32).cuboid(1.5f, 4.0f, -3.0f, 1.0f, 6.0f, 6.0f),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        return TexturedModelData.of(modelData, 64, 48);
    }

    @Override
    public void setAngles(GoblinRenderState state) {
        super.setAngles(state);

        // Visibility based on knight variant
        this.buckler.visible = state.isKnight;
        this.helm.visible = state.isKnight;

        // Sneaky crouched posture
        this.body.pitch += 0.22f;
        this.head.pitch += 0.12f;

        if (state.attackState == GoblinKnightEntity.STATE_GUARDING) {
            // Guard: Left arm raised across chest with shield forward
            this.leftArm.pitch = -1.25f;
            this.leftArm.yaw = 0.85f;
            this.rightArm.pitch = 0.2f;
            this.body.pitch = 0.15f;
        } else if (state.attackState == GoblinKnightEntity.STATE_COUNTER) {
            // Riposte thrust
            this.rightArm.pitch = -1.45f;
            this.leftArm.pitch = -0.4f;
        } else if (state.attackState == GoblinEntity.STATE_FLURRY) {
            // Fast stabbing motion
            float stab = MathHelper.sin(state.attackTicks * 1.2f) * 0.8f;
            this.rightArm.pitch = -1.2f + stab;
        } else if (state.attackState == GoblinEntity.STATE_DODGE) {
            // Backstep tuck
            this.body.pitch = 0.5f;
            this.rightArm.pitch = 0.5f;
            this.leftArm.pitch = 0.5f;
        }
    }
}
