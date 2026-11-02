package com.darkcraft.artorias.entity;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;

public class ArtoriasModel extends BipedEntityModel<ArtoriasRenderState> {
    public ArtoriasModel(ModelPart root) {
        super(root);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = BipedEntityModel.getModelData(Dilation.NONE, 0.0f);
        ModelPartData root = modelData.getRoot();

        // Custom plume / crest on helmet
        ModelPartData head = root.getChild(EntityModelPartNames.HEAD);
        head.addChild("plume", ModelPartBuilder.create()
                        .uv(24, 0)
                        .cuboid(-1.0f, -12.0f, -2.0f, 2.0f, 5.0f, 6.0f),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(ArtoriasRenderState state) {
        super.setAngles(state);

        // Artorias broken left arm hangs limp (iconic Souls detail)
        this.leftArm.pitch = 0.15f;
        this.leftArm.roll = -0.08f;
        this.leftArm.yaw = 0.05f;

        if (state.isStaggered) {
            // Stance Broken / Staggered: On one knee, vulnerable for riposte
            this.body.pitch = 0.35f;
            this.head.pitch = 0.5f;

            this.rightArm.pitch = 0.8f;
            this.rightArm.yaw = -0.3f;
            this.leftArm.pitch = 0.4f;

            this.rightLeg.pitch = -0.9f;
            this.leftLeg.pitch = 0.8f;
        } else if (state.attackState == ArtoriasEntity.STATE_SOMERSAULT_FLIP) {
            // Flip pose: curling in mid-air with sword held high overhead
            this.body.pitch = -0.6f;
            this.rightArm.pitch = -2.6f;
            this.head.pitch = -0.4f;
        } else if (state.attackState == ArtoriasEntity.STATE_GLIDE_LUNGE) {
            // Glide lunge pose: leaning low forward, sword thrusting
            this.body.pitch = 0.5f;
            this.rightArm.pitch = -1.4f;
            this.rightArm.yaw = -0.2f;
            this.head.pitch = -0.3f;
        } else if (state.attackState == ArtoriasEntity.STATE_SPIN_SLASH) {
            // 360 Spin pose: sword held out wide
            this.rightArm.pitch = -0.3f;
            this.rightArm.roll = 1.2f;
            this.rightArm.yaw = 1.0f;
        } else if (state.attackState == ArtoriasEntity.STATE_PHASE_TWO_ROAR) {
            // Roar pose: head thrown back to sky, right arm spread wide
            this.head.pitch = -0.8f;
            this.body.pitch = -0.2f;
            this.rightArm.pitch = -0.6f;
            this.rightArm.roll = 0.9f;
            this.leftArm.roll = -0.9f;
        }
    }
}
