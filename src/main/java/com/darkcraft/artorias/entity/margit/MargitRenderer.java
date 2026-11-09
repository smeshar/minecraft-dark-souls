package com.darkcraft.artorias.entity.margit;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib GeoEntityRenderer for Margit, the Fell Omen.
 * Scales the boss silhouette (1.35x) and attaches the Phase 2 golden weapon render layer.
 */
public class MargitRenderer extends GeoEntityRenderer<MargitEntity> {
    public MargitRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new MargitModel());
        this.shadowRadius = 0.85f;
        this.addRenderLayer(new MargitGoldenWeaponLayer(this));
    }

    @Override
    public void render(MargitEntity entity, float entityYaw, float partialTick, MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight) {
        poseStack.push();
        poseStack.scale(1.35f, 1.35f, 1.35f);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.pop();
    }
}
