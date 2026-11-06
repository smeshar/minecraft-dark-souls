package com.darkcraft.artorias.entity.margit;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.util.math.MathHelper;

/**
 * Custom 3D Model for Margit, the Fell Omen.
 * Features iconic twisted Omen horns, tattered robes, whipping tail,
 * wooden staff with concealed blade, and Phase 2 spectral golden hammer and dagger.
 */
public class MargitModel extends BipedEntityModel<MargitRenderState> {
    private final ModelPart horns;
    private final ModelPart faceHorns;
    private final ModelPart cloak;
    private final ModelPart tail;
    private final ModelPart staff;
    private final ModelPart goldenHammer;
    private final ModelPart goldenDagger;

    public MargitModel(ModelPart root) {
        super(root);
        this.horns = this.head.getChild("horns");
        this.faceHorns = this.head.getChild("face_horns");
        this.cloak = this.body.getChild("cloak");
        this.tail = this.body.getChild("tail");
        this.staff = this.rightArm.getChild("staff");
        this.goldenHammer = this.leftArm.getChild("golden_hammer");
        this.goldenDagger = this.leftArm.getChild("golden_dagger");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = BipedEntityModel.getModelData(Dilation.NONE, 0.0f);
        ModelPartData root = modelData.getRoot();

        // 1. Head with tangled Cursed Omen Horns
        ModelPartData head = root.getChild(EntityModelPartNames.HEAD);
        head.addChild("horns", ModelPartBuilder.create()
                        // Right large curling horn
                        .uv(0, 32).cuboid(-6.5f, -8.0f, -2.5f, 2.0f, 4.0f, 5.0f)
                        .uv(14, 32).cuboid(-8.0f, -12.0f, 1.0f, 2.0f, 6.0f, 2.0f)
                        .uv(22, 32).cuboid(-6.5f, -14.0f, -2.0f, 2.0f, 3.0f, 4.0f)
                        // Left curling horn
                        .uv(0, 32).cuboid(4.5f, -8.0f, -2.5f, 2.0f, 4.0f, 5.0f)
                        .uv(14, 32).cuboid(6.0f, -12.0f, 1.0f, 2.0f, 6.0f, 2.0f)
                        .uv(22, 32).cuboid(4.5f, -14.0f, -2.0f, 2.0f, 3.0f, 4.0f),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        head.addChild("face_horns", ModelPartBuilder.create()
                        // Asymmetrical horn wrapping across brow and cheek
                        .uv(0, 42).cuboid(-2.0f, -10.0f, -5.0f, 4.0f, 2.0f, 2.0f)
                        .uv(12, 42).cuboid(1.5f, -7.0f, -5.5f, 2.0f, 4.0f, 2.0f),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        // 2. Torso with Tattered Omen Cloak & Muscular Tail
        ModelPartData body = root.getChild(EntityModelPartNames.BODY);
        body.addChild("cloak", ModelPartBuilder.create()
                        // Ragged cowl and cowl collar around shoulders
                        .uv(16, 16).cuboid(-5.5f, -0.5f, -3.5f, 11.0f, 6.0f, 7.0f)
                        // Lower cloak skirt draping down
                        .uv(16, 26).cuboid(-5.0f, 5.5f, -3.0f, 10.0f, 9.0f, 6.0f),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        body.addChild("tail", ModelPartBuilder.create()
                        // Base of thick scaled tail
                        .uv(16, 32).cuboid(-2.0f, 0.0f, 0.0f, 4.0f, 4.0f, 6.0f)
                        // Middle tail segment
                        .uv(20, 34).cuboid(-1.5f, 1.0f, 5.5f, 3.0f, 3.0f, 7.0f)
                        // Tip segment
                        .uv(24, 36).cuboid(-1.0f, 2.0f, 12.0f, 2.0f, 2.0f, 5.0f),
                ModelTransform.origin(0.0f, 10.0f, 2.0f));

        // 3. Right Arm: Margit's Gnarled Staff
        ModelPartData rightArm = root.getChild(EntityModelPartNames.RIGHT_ARM);
        rightArm.addChild("staff", ModelPartBuilder.create()
                        // Gnarled wooden walking cane / staff
                        .uv(32, 32).cuboid(-1.0f, -12.0f, -2.0f, 2.0f, 32.0f, 2.0f)
                        // Top curved handle
                        .uv(40, 32).cuboid(-1.0f, -14.0f, -5.0f, 2.0f, 3.0f, 4.0f)
                        // Concealed blade tip protruding from bottom
                        .uv(36, 44).cuboid(-0.5f, 19.5f, -1.5f, 1.0f, 6.0f, 1.0f),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        // 4. Left Arm: Phase 2 Golden Warhammer & Golden Dagger
        ModelPartData leftArm = root.getChild(EntityModelPartNames.LEFT_ARM);
        leftArm.addChild("golden_hammer", ModelPartBuilder.create()
                        // Long golden shaft
                        .uv(48, 32).cuboid(-1.0f, -10.0f, -1.5f, 2.0f, 26.0f, 2.0f)
                        // Massive double-headed golden hammer head
                        .uv(50, 36).cuboid(-4.0f, -18.0f, -5.0f, 8.0f, 9.0f, 9.0f),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        leftArm.addChild("golden_dagger", ModelPartBuilder.create()
                        // Spectral dagger blade
                        .uv(48, 44).cuboid(-0.5f, 8.0f, -7.0f, 1.0f, 2.0f, 8.0f)
                        // Dagger crossguard
                        .uv(52, 44).cuboid(-1.5f, 7.5f, -1.0f, 3.0f, 3.0f, 1.0f),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(MargitRenderState state) {
        super.setAngles(state);

        // Visibility of spectral weapons
        this.goldenHammer.visible = state.isPhaseTwo && (state.attackState == MargitAttackState.HAMMER_SLAM || state.attackState == MargitAttackState.IDLE);
        this.goldenDagger.visible = (state.attackState == MargitAttackState.DAGGER_TOSS) || (state.isPhaseTwo && state.attackState == MargitAttackState.STAFF_SWING_COMBO);

        // Menacing hunched posture of the Fell Omen
        this.body.pitch += 0.16f;
        this.head.pitch += 0.08f;

        // Tail natural idle swaying
        this.tail.pitch = 0.25f;
        this.tail.yaw = MathHelper.sin(state.ageInTicks * 0.08f) * 0.18f;

        // ==========================================
        // Attack & Special State Keyframe Animations
        // ==========================================
        if (state.isPinned) {
            // Pinned to ground by Margit's Shackle
            this.body.pitch = 0.85f;
            this.head.pitch = 0.6f;
            this.rightArm.pitch = 0.8f;
            this.leftArm.pitch = 0.8f;
            this.rightLeg.pitch = -1.2f;
            this.leftLeg.pitch = -1.2f;
        } else if (state.isStunned) {
            // Poise broken: collapsed on one knee, vulnerable to critical riposte
            this.body.pitch = 0.55f;
            this.head.pitch = 0.85f;
            this.rightLeg.pitch = -1.35f;
            this.leftLeg.pitch = 0.55f;
            this.rightArm.pitch = 0.65f;
            this.leftArm.pitch = 0.65f;
        } else if (state.attackState == MargitAttackState.PHASE_TRANSITION) {
            // Agonizing roar: chest arched back, head looking straight up
            this.body.pitch = -0.35f;
            this.head.pitch = -0.75f;
            this.rightArm.pitch = -1.4f;
            this.rightArm.yaw = -0.4f;
            this.leftArm.pitch = -1.4f;
            this.leftArm.yaw = 0.4f;
        } else if (state.attackState == MargitAttackState.STAFF_SWING_COMBO) {
            float progress = (float) state.attackTicks / MargitAttackState.STAFF_SWING_COMBO.getTotalDuration();
            if (progress < 0.42f) {
                // Wind-up: raising staff high to bait rolls
                this.rightArm.pitch = -2.5f;
                this.rightArm.yaw = 0.3f;
                this.body.yaw = -0.35f;
            } else {
                // Sweeping horizontal slash!
                float swing = MathHelper.clamp((progress - 0.42f) * 2.5f, 0.0f, 1.0f);
                this.rightArm.pitch = -0.2f + swing * 0.5f;
                this.rightArm.yaw = -1.5f * swing;
                this.body.yaw = 0.4f * swing;
            }
        } else if (state.attackState == MargitAttackState.STAFF_LEAP) {
            float progress = (float) state.attackTicks / MargitAttackState.STAFF_LEAP.getTotalDuration();
            if (progress < 0.55f) {
                // In air: legs tucked, staff held high overhead
                this.rightLeg.pitch = -0.7f;
                this.leftLeg.pitch = -0.7f;
                this.rightArm.pitch = -2.8f;
                this.leftArm.pitch = -2.4f;
            } else {
                // Impact ground slam
                this.body.pitch = 0.55f;
                this.rightArm.pitch = 0.85f;
                this.leftArm.pitch = 0.6f;
            }
        } else if (state.attackState == MargitAttackState.DAGGER_TOSS) {
            // Flicking left arm forward throwing spectral dagger
            float flick = MathHelper.sin(state.attackTicks * 0.4f);
            this.leftArm.pitch = -1.6f + flick * 0.4f;
            this.leftArm.yaw = 0.35f;
        } else if (state.attackState == MargitAttackState.TAIL_SWIPE) {
            // Rapid 360 spin with whipping tail
            float spin = (float) state.attackTicks * 0.4f;
            this.body.yaw += spin;
            this.tail.yaw = MathHelper.sin((float) state.attackTicks * 0.8f) * 1.6f;
            this.tail.pitch = -0.1f;
        } else if (state.attackState == MargitAttackState.HAMMER_SLAM) {
            float progress = (float) state.attackTicks / MargitAttackState.HAMMER_SLAM.getTotalDuration();
            if (progress < 0.52f) {
                // Raising colossal golden hammer high overhead
                this.leftArm.pitch = -2.8f;
                this.leftArm.yaw = -0.2f;
                this.rightArm.pitch = -2.4f;
                this.body.pitch = -0.25f;
            } else {
                // Crashing down into ground
                this.body.pitch = 0.6f;
                this.head.pitch = 0.4f;
                this.leftArm.pitch = 0.75f;
                this.rightArm.pitch = 0.65f;
            }
        }
    }
}
