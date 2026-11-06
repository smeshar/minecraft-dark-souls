package com.darkcraft.artorias.entity.margit;

import com.darkcraft.artorias.ArtoriasMod;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * GeckoLib Render Layer for Margit, the Fell Omen.
 * In Phase 2 (HP <= 60%), dynamically renders the glowing spectral golden hammer and dagger
 * with an ethereal, luminous translucent shader over Margit's hands.
 */
public class MargitGoldenWeaponLayer extends GeoRenderLayer<MargitEntity> {
    private static final Identifier SPECTRAL_TEXTURE = ArtoriasMod.id("textures/entity/margit_spectral_weapons.png");

    public MargitGoldenWeaponLayer(GeoRenderer<MargitEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(MatrixStack poseStack, MargitEntity animatable, BakedGeoModel bakedModel,
                       RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay, int color) {
        // Only render golden spectral weapons once Phase 2 has ignited
        if (!animatable.isPhaseTwo()) {
            return;
        }

        RenderLayer glowLayer = getRenderer().getRenderType(animatable, SPECTRAL_TEXTURE, bufferSource, partialTick);
        VertexConsumer glowConsumer = bufferSource.getBuffer(glowLayer);

        // Render spectral overlay with full emissive glow (packedLight maxed to 15728880 / fullbright)
        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glowLayer, glowConsumer,
                partialTick, 15728880, OverlayTexture.DEFAULT_UV, color);
    }
}
