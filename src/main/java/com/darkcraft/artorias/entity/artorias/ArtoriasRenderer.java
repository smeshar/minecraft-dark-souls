package com.darkcraft.artorias.entity.artorias;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib GeoEntityRenderer for Knight Artorias, the Abysswalker.
 */
public class ArtoriasRenderer extends GeoEntityRenderer<ArtoriasEntity> {
    public ArtoriasRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new ArtoriasModel());
        this.shadowRadius = 0.75f;
    }

    @Override
    public void render(ArtoriasEntity entity, float entityYaw, float partialTick, MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight) {
        poseStack.push();
        poseStack.scale(1.25f, 1.25f, 1.25f);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.pop();
    }
}
