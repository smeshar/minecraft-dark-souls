package com.darkcraft.artorias.entity.warg;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class WargModel extends SinglePartEntityModel<WargEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart lowerJaw;
    private final ModelPart body;
    private final ModelPart mane;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart tail;

    public WargModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.lowerJaw = this.head.getChild("lower_jaw");
        this.body = root.getChild("body");
        this.mane = root.getChild("mane");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.tail = root.getChild("tail");
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        // Head and Snout
        ModelPartData head = root.addChild("head", ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-3.0f, -3.0f, -3.0f, 6.0f, 6.0f, 5.0f)
                        .uv(0, 11).cuboid("snout", -2.0f, -1.0f, -7.0f, 4.0f, 3.0f, 5.0f)
                        .uv(16, 14).cuboid("right_ear", -3.0f, -5.5f, -1.0f, 2.0f, 3.0f, 1.0f)
                        .uv(16, 14).cuboid("left_ear", 1.0f, -5.5f, -1.0f, 2.0f, 3.0f, 1.0f),
                ModelTransform.pivot(-1.0f, 13.5f, -7.0f));

        head.addChild("lower_jaw", ModelPartBuilder.create()
                        .uv(0, 19).cuboid(-1.5f, 0.0f, -4.5f, 3.0f, 1.5f, 4.5f),
                ModelTransform.pivot(0.0f, 1.5f, -2.5f));

        // Muscular Mane and Torso
        root.addChild("mane", ModelPartBuilder.create()
                        .uv(22, 0).cuboid(-4.0f, -3.5f, -3.0f, 8.0f, 8.0f, 7.0f, new Dilation(0.2f)),
                ModelTransform.pivot(-1.0f, 14.0f, -3.0f));

        root.addChild("body", ModelPartBuilder.create()
                        .uv(18, 15).cuboid(-3.0f, -2.5f, -4.0f, 6.0f, 6.0f, 9.0f),
                ModelTransform.of(-1.0f, 14.5f, 2.0f, 0.0f, 0.0f, 0.0f));

        // Muscular Legs
        root.addChild("right_front_leg", ModelPartBuilder.create()
                        .uv(0, 25).cuboid(-1.0f, 0.0f, -1.0f, 2.5f, 8.5f, 2.5f),
                ModelTransform.pivot(-3.0f, 15.5f, -4.0f));

        root.addChild("left_front_leg", ModelPartBuilder.create()
                        .uv(0, 25).cuboid(-1.5f, 0.0f, -1.0f, 2.5f, 8.5f, 2.5f),
                ModelTransform.pivot(1.0f, 15.5f, -4.0f));

        root.addChild("right_hind_leg", ModelPartBuilder.create()
                        .uv(10, 25).cuboid(-1.0f, 0.0f, -1.0f, 2.5f, 8.5f, 2.5f),
                ModelTransform.pivot(-3.0f, 15.5f, 6.0f));

        root.addChild("left_hind_leg", ModelPartBuilder.create()
                        .uv(10, 25).cuboid(-1.5f, 0.0f, -1.0f, 2.5f, 8.5f, 2.5f),
                ModelTransform.pivot(1.0f, 15.5f, 6.0f));

        // Tapered Direwolf Tail
        root.addChild("tail", ModelPartBuilder.create()
                        .uv(20, 25).cuboid(-1.0f, 0.0f, -1.0f, 2.0f, 9.0f, 2.0f),
                ModelTransform.of(-1.0f, 12.5f, 7.0f, 0.85f, 0.0f, 0.0f));

        return TexturedModelData.of(modelData, 64, 48);
    }

    @Override
    public void setAngles(WargEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        // Natural walking stride
        this.rightFrontLeg.pitch = MathHelper.cos(limbAngle * 0.6662f) * 1.4f * limbDistance;
        this.leftFrontLeg.pitch = MathHelper.cos(limbAngle * 0.6662f + (float) Math.PI) * 1.4f * limbDistance;
        this.rightHindLeg.pitch = MathHelper.cos(limbAngle * 0.6662f + (float) Math.PI) * 1.4f * limbDistance;
        this.leftHindLeg.pitch = MathHelper.cos(limbAngle * 0.6662f) * 1.4f * limbDistance;

        this.head.yaw = headYaw * ((float) Math.PI / 180.0f);
        this.head.pitch = headPitch * ((float) Math.PI / 180.0f);
        this.tail.yaw = MathHelper.cos(limbAngle * 0.5f) * 0.4f * limbDistance;

        int attackState = entity.getAttackState();
        int attackTicks = entity.getAttackTicks();

        // Custom animation states
        if (attackState == WargEntity.STATE_HOWL) {
            // Howl: Head pitches straight up, jaw drops open
            this.head.pitch = -0.95f;
            this.lowerJaw.pitch = 0.55f;
            this.mane.pitch = -0.15f;
            this.body.pitch = 0.15f;
        } else if (attackState == WargEntity.STATE_POUNCE) {
            // Pounce: coiled body and paws stretched forward
            this.body.pitch = -0.35f;
            this.head.pitch = 0.25f;
            this.rightFrontLeg.pitch = -0.85f;
            this.leftFrontLeg.pitch = -0.85f;
            this.rightHindLeg.pitch = 0.85f;
            this.leftHindLeg.pitch = 0.85f;
            this.lowerJaw.pitch = 0.4f;
        } else if (attackState == WargEntity.STATE_BITE) {
            // Snapping bite
            float snap = MathHelper.sin(attackTicks * 0.8f);
            this.head.pitch += snap * 0.35f;
            this.lowerJaw.pitch = MathHelper.clamp(snap * 0.5f, 0.0f, 0.5f);
        } else {
            this.lowerJaw.pitch = 0.0f;
            this.body.pitch = 0.0f;
            this.mane.pitch = 0.0f;
        }
    }
}
