package com.darkcraft.artorias.entity.artorias;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.util.math.MathHelper;

public class ArtoriasModel extends BipedEntityModel<ArtoriasRenderState> {
    private final ModelPart helmetCrest;
    private final ModelPart plumeSocket;
    private final ModelPart plumeUpper;
    private final ModelPart plumeLower;
    private final ModelPart cowl;
    private final ModelPart cape;
    private final ModelPart tabardFront;
    private final ModelPart tabardBack;
    private final ModelPart rightPauldron;
    private final ModelPart rightPauldronRim;
    private final ModelPart leftCorruption;
    private final ModelPart rightKnee;
    private final ModelPart leftKnee;

    public ArtoriasModel(ModelPart root) {
        super(root);
        this.helmetCrest = this.head.getChild("helmet_crest");
        this.plumeSocket = this.head.getChild("plume_socket");
        this.plumeUpper = this.head.getChild("plume_upper");
        this.plumeLower = this.plumeUpper.getChild("plume_lower");
        this.cowl = this.body.getChild("cowl");
        this.cape = this.body.getChild("cape");
        this.tabardFront = this.body.getChild("tabard_front");
        this.tabardBack = this.body.getChild("tabard_back");
        this.rightPauldron = this.rightArm.getChild("pauldron");
        this.rightPauldronRim = this.rightArm.getChild("pauldron_rim");
        this.leftCorruption = this.leftArm.getChild("left_corruption");
        this.rightKnee = this.rightLeg.getChild("knee");
        this.leftKnee = this.leftLeg.getChild("knee");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = BipedEntityModel.getModelData(Dilation.NONE, 0.0f);
        ModelPartData root = modelData.getRoot();

        // 1. Knight of Gwyn / Wolf Knight Helmet & Crest
        ModelPartData head = root.getChild(EntityModelPartNames.HEAD);

        // Tall, elegant curved gothic crest fin along the skull midline (matching reference concept art)
        head.addChild("helmet_crest", ModelPartBuilder.create()
                        .uv(48, 48)
                        .cuboid(-0.5f, -12.5f, -2.5f, 1.0f, 5.0f, 5.0f),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        // Plume mount socket on top-rear of helmet
        head.addChild("plume_socket", ModelPartBuilder.create()
                        .uv(58, 42)
                        .cuboid(-0.5f, -8.5f, 2.0f, 1.0f, 2.0f, 1.0f),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        // Artorias iconic flowing mane / ponytail cascading down the back of the helmet & spine
        ModelPartData plumeUpper = head.addChild("plume_upper", ModelPartBuilder.create()
                        .uv(0, 48)
                        .cuboid(-1.5f, 0.0f, 0.0f, 3.0f, 5.0f, 2.0f),
                ModelTransform.origin(0.0f, -8.0f, 2.5f));

        plumeUpper.addChild("plume_lower", ModelPartBuilder.create()
                        .uv(0, 56)
                        .cuboid(-2.0f, 0.0f, 0.0f, 4.0f, 6.0f, 2.0f),
                ModelTransform.origin(0.0f, 5.0f, 0.0f));

        // 2. Torso: Tattered Royal Blue Cowl, Cape, and Knight Surcoat Tabards
        ModelPartData body = root.getChild(EntityModelPartNames.BODY);

        // Draped royal blue mantle around the neck and across the left shoulder
        body.addChild("cowl", ModelPartBuilder.create()
                        .uv(0, 32)
                        .cuboid(-4.0f, -0.5f, -2.0f, 8.0f, 3.0f, 4.0f, new Dilation(0.25f)),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        // Tattered knight cloak flowing down the spine to knee-level
        body.addChild("cape", ModelPartBuilder.create()
                        .uv(24, 32)
                        .cuboid(-4.0f, 0.0f, 0.0f, 8.0f, 14.0f, 1.0f),
                ModelTransform.origin(0.0f, 1.0f, 2.0f));

        // Front surcoat / tabard with chainmail hanging over thighs
        body.addChild("tabard_front", ModelPartBuilder.create()
                        .uv(0, 40)
                        .cuboid(-2.5f, 0.0f, 0.0f, 5.0f, 6.0f, 1.0f),
                ModelTransform.origin(0.0f, 9.5f, -2.2f));

        // Back surcoat / tabard hanging behind thighs
        body.addChild("tabard_back", ModelPartBuilder.create()
                        .uv(0, 40)
                        .cuboid(-2.5f, 0.0f, 0.0f, 5.0f, 6.0f, 1.0f),
                ModelTransform.origin(0.0f, 9.5f, 1.5f));

        // 3. Right Sword Arm: Heavy Knight Spaulder with Flared Haute-piece (Neck Guard)
        ModelPartData rightArm = root.getChild(EntityModelPartNames.RIGHT_ARM);
        rightArm.addChild("pauldron", ModelPartBuilder.create()
                        .uv(48, 32)
                        .cuboid(-3.5f, -2.5f, -2.0f, 4.0f, 4.0f, 4.0f, new Dilation(0.25f)),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        // Upward flared haute-piece neck guard flange
        rightArm.addChild("pauldron_rim", ModelPartBuilder.create()
                        .uv(48, 41)
                        .cuboid(-4.0f, -4.5f, -2.0f, 1.0f, 3.0f, 4.0f),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        // 4. Left Arm: Shattered, Limp, Corrupted Arm wrapped in Abyss Sludge
        ModelPartData leftArm = root.getChild(EntityModelPartNames.LEFT_ARM);
        leftArm.addChild("left_corruption", ModelPartBuilder.create()
                        .uv(12, 40)
                        .cuboid(-0.5f, -2.5f, -1.5f, 3.0f, 3.0f, 3.0f, new Dilation(0.15f)),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        // 5. Slender Knight Legs: Pointed Gothic Knee Poleyns
        ModelPartData rightLeg = root.getChild(EntityModelPartNames.RIGHT_LEG);
        rightLeg.addChild("knee", ModelPartBuilder.create()
                        .uv(42, 32)
                        .cuboid(-1.0f, 3.5f, -2.5f, 2.0f, 3.0f, 1.0f),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        ModelPartData leftLeg = root.getChild(EntityModelPartNames.LEFT_LEG);
        leftLeg.addChild("knee", ModelPartBuilder.create()
                        .uv(42, 32)
                        .cuboid(-1.0f, 3.5f, -2.5f, 2.0f, 3.0f, 1.0f),
                ModelTransform.origin(0.0f, 0.0f, 0.0f));

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(ArtoriasRenderState state) {
        super.setAngles(state);

        // Menacing predatory combat hunch (classic Dark Souls Artorias stance)
        this.body.pitch += 0.16f;
        this.head.pitch += 0.08f;

        // Idle breathing motion
        float breath = MathHelper.sin(state.ageInTicks * 0.08f) * 0.035f;
        this.body.pitch += breath;
        this.rightArm.pitch += breath * 0.5f;

        // Broken, limp left arm swaying lifelessly
        this.leftArm.pitch = 0.24f + MathHelper.cos(state.ageInTicks * 0.06f) * 0.06f;
        this.leftArm.roll = -0.18f;
        this.leftArm.yaw = 0.12f;

        // Dynamic cloth and plume physics
        float walkSway = MathHelper.abs(this.rightLeg.pitch) * 0.35f;
        float runSpeed = MathHelper.sin(state.ageInTicks * 0.12f) * 0.05f;

        // Cape sways back with movement and breathes
        this.cape.pitch = 0.14f + runSpeed + walkSway;
        this.cape.roll = MathHelper.cos(state.ageInTicks * 0.08f) * 0.03f;

        // Tabards sway with leg movement
        this.tabardFront.pitch = -walkSway * 0.5f;
        this.tabardBack.pitch = walkSway * 0.5f;

        // Double-jointed cascading plume physics down the back!
        float headPitchOffset = this.head.pitch * 0.25f;
        this.plumeUpper.pitch = -0.06f + MathHelper.sin(state.ageInTicks * 0.08f) * 0.04f + walkSway * 0.4f - headPitchOffset;
        this.plumeLower.pitch = 0.08f + MathHelper.sin(state.ageInTicks * 0.08f - 0.5f) * 0.06f + walkSway * 0.6f;
        this.plumeUpper.roll = MathHelper.cos(state.ageInTicks * 0.06f) * 0.03f;
        this.plumeLower.roll = MathHelper.cos(state.ageInTicks * 0.06f - 0.4f) * 0.05f;

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
            this.plumeUpper.pitch = 0.5f;
            this.plumeLower.pitch = 0.4f;
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
                this.plumeUpper.pitch = 1.2f;
                this.plumeLower.pitch = 0.8f;
            } else {
                // Crushing ground slam impact
                this.body.pitch = 0.65f;
                this.rightArm.pitch = 0.95f;
                this.head.pitch = 0.3f;
                this.rightLeg.pitch = -0.8f;
                this.leftLeg.pitch = 0.5f;
                this.cape.pitch = 0.2f;
                this.plumeUpper.pitch = 0.3f;
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
                this.cape.pitch = 1.2f;
                this.plumeUpper.pitch = 0.9f;
                this.plumeLower.pitch = 0.7f;
            }
        } else if (state.attackState == ArtoriasEntity.STATE_SPIN_SLASH) {
            // 360 Spin Whirlwind: Torso twists in a full sweeping arc
            float progress = MathHelper.clamp(state.attackTicks / 10.0f, 0.0f, 1.0f);
            this.body.yaw += progress * (float) Math.PI * 2.0f;
            this.rightArm.pitch = -0.25f;
            this.rightArm.roll = 1.35f;
            this.rightArm.yaw = 0.95f;
            this.cape.roll = 0.65f;
            this.plumeUpper.yaw = -0.6f;
            this.plumeLower.yaw = -0.9f;
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
            this.cape.pitch = 0.55f + MathHelper.sin(state.ageInTicks * 1.8f) * 0.25f;
            this.plumeUpper.pitch = -0.5f + MathHelper.sin(state.ageInTicks * 1.8f) * 0.2f;
            this.plumeLower.pitch = -0.3f + MathHelper.sin(state.ageInTicks * 1.8f) * 0.15f;
        }
    }
}
