package com.darkcraft.artorias.entity.omen;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.util.math.MathHelper;

public class OmenModel extends BipedEntityModel<OmenRenderState> {
    private final ModelPart horns;
    private final ModelPart cleaver;
    private final ModelPart belly;

    public OmenModel(ModelPart root) {
        super(root);
        this.horns = this.head.getChild("horns");
        this.cleaver = this.rightArm.getChild("cleaver");
        this.belly = this.body.getChild("belly");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = BipedEntityModel.getModelData(Dilation.NONE, 0.0f);
        ModelPartData root = modelData.getRoot();

        // 1. Broad monstrous head with menacing curled horns of the Omen
        ModelPartData head = root.getChild(EntityModelPartNames.HEAD);
        head.addChild("horns", ModelPartBuilder.create()
                        // Right curled horn
                        .uv(0, 32).cuboid(-6.0f, -7.0f, -2.0f, 2.0f, 4.0f, 5.0f)
                        .uv(14, 32).cuboid(-7.5f, -10.0f, 1.0f, 2.0f, 5.0f, 2.0f)
                        // Left curled horn
                        .uv(0, 32).cuboid(4.0f, -7.0f, -2.0f, 2.0f, 4.0f, 5.0f)
                        .uv(14, 32).cuboid(5.5f, -10.0f, 1.0f, 2.0f, 5.0f, 2.0f)
                        // Forehead horn stubs
                        .uv(22, 32).cuboid(-1.5f, -9.0f, -4.5f, 3.0f, 3.0f, 2.0f),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        // 2. Bulky Omen Torso with protruding heavy belly
        ModelPartData body = root.getChild(EntityModelPartNames.BODY);
        body.addChild("belly", ModelPartBuilder.create()
                        .uv(32, 32).cuboid(-4.5f, 2.0f, -3.5f, 9.0f, 8.0f, 3.0f),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        // 3. Giant Greatcleaver attached to Right Arm
        ModelPartData rightArm = root.getChild(EntityModelPartNames.RIGHT_ARM);
        rightArm.addChild("cleaver", ModelPartBuilder.create()
                        // Cleaver handle
                        .uv(0, 48).cuboid(-1.0f, 8.0f, -2.0f, 2.0f, 12.0f, 2.0f)
                        // Massive curved rusty blade
                        .uv(8, 44).cuboid(-1.5f, 10.0f, -12.0f, 3.0f, 10.0f, 10.0f),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(OmenRenderState state) {
        super.setAngles(state);

        // Hide cleaver if brawler
        this.cleaver.visible = (state.variant == OmenEntity.Variant.CLEAVER);

        // Heavy brutish hunch
        this.body.pitch += 0.12f;
        this.head.pitch += 0.08f;

        if (state.isStaggered) {
            // Staggered onto one knee
            this.body.pitch = 0.55f;
            this.head.pitch = 0.75f;
            this.rightArm.pitch = 0.6f;
            this.leftArm.pitch = 0.6f;
            this.rightLeg.pitch = -1.2f;
            this.leftLeg.pitch = 0.6f;
        } else if (state.attackState == OmenEntity.STATE_OVERHEAD_SLAM) {
            float progress = MathHelper.clamp(state.attackTicks / 26.0f, 0.0f, 1.0f);
            if (progress > 0.35f) {
                // Raising cleaver with both hands
                this.rightArm.pitch = -2.6f;
                this.leftArm.pitch = -2.4f;
                this.leftArm.yaw = 0.4f;
                this.body.pitch = -0.2f;
                this.head.pitch = -0.3f;
            } else {
                // Smashing down!
                this.rightArm.pitch = 0.8f;
                this.leftArm.pitch = 0.6f;
                this.body.pitch = 0.45f;
                this.head.pitch = 0.35f;
            }
        } else if (state.attackState == OmenEntity.STATE_KICK) {
            this.rightLeg.pitch = -1.5f;
            this.body.pitch = -0.3f;
        } else if (state.attackState == OmenEntity.STATE_STOMP) {
            float stompProg = MathHelper.sin(state.attackTicks * 0.4f);
            this.rightLeg.pitch = -stompProg * 0.8f;
        } else if (state.attackState == OmenEntity.STATE_CURSED_BREATH) {
            this.body.pitch = 0.42f;
            this.head.pitch = -0.1f;
            this.rightArm.pitch = 0.5f;
            this.leftArm.pitch = 0.5f;
        }
    }
}
