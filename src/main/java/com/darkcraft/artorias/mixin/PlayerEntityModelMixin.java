package com.darkcraft.artorias.mixin;

import com.darkcraft.artorias.client.GroundSlamClient;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityModel.class)
public class PlayerEntityModelMixin {

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void artorias$applySlamAnimations(PlayerEntityRenderState state, CallbackInfo ci) {
        PlayerEntityModel model = (PlayerEntityModel) (Object) this;
        int entityId = state.id;

        if (GroundSlamClient.isSlamming(entityId)) {
            // Dark Souls / Elden Ring Plunge Attack downward dive pose
            // Arms thrust down, holding sword vertically downwards
            model.rightArm.pitch = 3.10f;
            model.rightArm.yaw = -0.15f;
            model.rightArm.roll = -0.10f;

            model.leftArm.pitch = 3.10f;
            model.leftArm.yaw = 0.15f;
            model.leftArm.roll = 0.10f;

            // Torso leaned aggressively forward into the dive
            model.body.pitch = 0.55f;
            model.head.pitch = -0.30f;

            // Legs trailing behind in descent
            model.rightLeg.pitch = 0.40f;
            model.leftLeg.pitch = 0.40f;
        } else if (GroundSlamClient.isRecovering(entityId)) {
            // Impact Recovery: Superhero landing crouch pose
            // Right arm slammed down to the ground
            model.rightArm.pitch = -1.25f;
            model.rightArm.yaw = 0.10f;

            // Left arm back for balance
            model.leftArm.pitch = 0.35f;
            model.leftArm.yaw = -0.30f;

            // Deep impact crouch
            model.body.pitch = 0.65f;
            model.head.pitch = 0.15f;
            model.rightLeg.pitch = -1.10f;
            model.leftLeg.pitch = 0.85f;
        }
    }
}
