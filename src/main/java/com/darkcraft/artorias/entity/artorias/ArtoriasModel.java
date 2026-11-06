package com.darkcraft.artorias.entity.artorias;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.util.math.MathHelper;

public class ArtoriasModel extends BipedEntityModel<ArtoriasRenderState> {
    private final ModelPart plume;
    private final ModelPart pauldron;
    private final ModelPart cape;
    private final ModelPart skirt;

    public ArtoriasModel(ModelPart root) {
        super(root);
        this.plume = this.head.getChild("plume");
        this.pauldron = this.rightArm.getChild("pauldron");
        this.cape = this.body.getChild("cape");
        this.skirt = this.body.getChild("skirt");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = BipedEntityModel.getModelData(Dilation.NONE, 0.0f);
        ModelPartData root = modelData.getRoot();

        // 1. Long flowing knightly Plume on the helmet
        ModelPartData head = root.getChild(EntityModelPartNames.HEAD);
        head.addChild("plume", ModelPartBuilder.create()
                        .uv(0, 48)
                        .cuboid(-1.0f, -14.0f, -1.0f, 2.0f, 6.0f, 6.0f),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        // 2. Heavy layered Pauldron on the Right Sword Arm
        ModelPartData rightArm = root.getChild(EntityModelPartNames.RIGHT_ARM);
        rightArm.addChild("pauldron", ModelPartBuilder.create()
                        .uv(48, 32)
                        .cuboid(-3.5f, -2.5f, -2.0f, 4.0f, 4.0f, 4.0f, new Dilation(0.4f)),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        // 3. Tattered Cape and Battle Surcoat Skirt on Torso
        ModelPartData body = root.getChild(EntityModelPartNames.BODY);
        body.addChild("cape", ModelPartBuilder.create()
                        .uv(24, 32)
                        .cuboid(-4.0f, 0.5f, 2.0f, 8.0f, 14.0f, 1.0f),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        body.addChild("skirt", ModelPartBuilder.create()
                        .uv(0, 32)
                        .cuboid(-4.0f, 9.5f, -2.0f, 8.0f, 5.0f, 4.0f, new Dilation(0.2f)),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(ArtoriasRenderState state) {
        super.setAngles(state);

        // Menacing predatory combat hunch (classic Dark Souls Artorias stance)
        this.body.pitch += 0.14f;
        this.head.pitch += 0.06f;

        // Idle breathing motion
        float breath = MathHelper.sin(state.ageInTicks * 0.08f) * 0.035f;
        this.body.pitch += breath;
        this.rightArm.pitch += breath * 0.5f;

        // Broken, limp left arm swaying lifelessly
        this.leftArm.pitch = 0.22f + MathHelper.cos(state.ageInTicks * 0.06f) * 0.06f;
        this.leftArm.roll = -0.16f;
        this.leftArm.yaw = 0.12f;

        // Cape and plume natural swaying with movement
        float walkSway = MathHelper.abs(this.rightLeg.pitch) * 0.35f;
        this.cape.pitch = 0.12f + MathHelper.sin(state.ageInTicks * 0.08f) * 0.05f + walkSway;
        this.plume.pitch = -0.05f + MathHelper.sin(state.ageInTicks * 0.08f) * 0.04f + walkSway * 0.6f;

        if (state.isStaggered) {
            // Stance Broken / Staggered: Collapses onto one knee, sword resting, head hanging low
            this.body.pitch = 0.48f;
            this.head.pitch = 0.70f;

            this.rightArm.pitch = 0.95f;
            this.rightArm.yaw = -0.25f;
            this.leftArm.pitch = 0.55f;

            this.rightLeg.pitch = -1.25f;
            this.leftLeg.pitch = 0.75f;
            this.cape.pitch = 0.6f;
        } else if (state.attackState == ArtoriasEntity.STATE_SOMERSAULT_FLIP) {
            // Iconic Somersault Flip: Full 360-degree forward rotation mid-air into ground slam
            float progress = MathHelper.clamp(state.attackTicks / 20.0f, 0.0f, 1.0f);
            if (progress < 0.8f) {
                // Mid-air flip curling
                float flip = progress / 0.8f;
                this.body.pitch = -0.2f + flip * (float) Math.PI * 2.0f;
                this.rightArm.pitch = -2.8f;
                this.head.pitch = 0.2f;
                this.rightLeg.pitch = -0.6f;
                this.leftLeg.pitch = -0.6f;
                this.cape.pitch = 1.4f;
            } else {
                // Crushing ground slam impact
                this.body.pitch = 0.65f;
                this.rightArm.pitch = 0.95f;
                this.head.pitch = 0.3f;
                this.rightLeg.pitch = -0.8f;
                this.leftLeg.pitch = 0.5f;
                this.cape.pitch = 0.2f;
            }
        } else if (state.attackState == ArtoriasEntity.STATE_GLIDE_LUNGE) {
            // Glide lunge pose: low aerodynamic sprint with sword thrusting forward
            float progress = MathHelper.clamp(state.attackTicks / 20.0f, 0.0f, 1.0f);
            if (progress < 0.35f) {
                // Coiling back
                this.body.pitch = 0.35f;
                this.rightArm.pitch = 0.5f;
                this.head.pitch = -0.2f;
            } else {
                // Full forward pierce
                this.body.pitch = 0.75f;
                this.head.pitch = -0.45f;
                this.rightArm.pitch = -1.65f;
                this.rightArm.yaw = -0.15f;
                this.leftArm.pitch = 0.65f;
                this.leftArm.roll = -0.45f;
                this.cape.pitch = 1.1f;
            }
        } else if (state.attackState == ArtoriasEntity.STATE_SPIN_SLASH) {
            // 360 Spin Whirlwind: Torso twists in a full sweeping arc
            float progress = MathHelper.clamp(state.attackTicks / 10.0f, 0.0f, 1.0f);
            this.body.yaw += progress * (float) Math.PI * 2.0f;
            this.rightArm.pitch = -0.25f;
            this.rightArm.roll = 1.35f;
            this.rightArm.yaw = 0.95f;
            this.cape.roll = 0.45f;
        } else if (state.attackState == ArtoriasEntity.STATE_ABYSS_SLUDGE) {
            // Abyss Sludge: Whipping withered left arm forward violently
            this.body.pitch = -0.15f;
            this.head.pitch = -0.25f;
            this.leftArm.pitch = -1.30f;
            this.leftArm.yaw = -0.45f;
            this.rightArm.pitch = 0.4f;
        } else if (state.attackState == ArtoriasEntity.STATE_PHASE_TWO_ROAR) {
            // Phase Two Roar: Head thrown back, chest arched, arms spread wide to the heavens
            this.head.pitch = -1.05f;
            this.body.pitch = -0.35f;
            this.rightArm.pitch = -0.85f;
            this.rightArm.roll = 1.15f;
            this.leftArm.pitch = -0.85f;
            this.leftArm.roll = -1.15f;
            this.cape.pitch = 0.45f + MathHelper.sin(state.ageInTicks * 1.8f) * 0.2f;
            this.plume.pitch = -0.4f + MathHelper.sin(state.ageInTicks * 1.8f) * 0.15f;
        }
    }
}
