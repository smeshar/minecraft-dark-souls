package com.darkcraft.artorias.entity.margit;

import com.darkcraft.artorias.ArtoriasMod;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * GeckoLib Render Layer that activates and renders luminous golden spectral weapons
 * (spectral hammer and sword) during Phase 2 of Margit's boss fight.
 */
public class MargitGoldenWeaponLayer extends GeoRenderLayer<MargitEntity> {
    private static final Identifier WEAPON_TEXTURE = ArtoriasMod.id("textures/entity/margit.png");

    public MargitGoldenWeaponLayer(GeoRenderer<MargitEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(MatrixStack poseStack, MargitEntity animatable, BakedGeoModel bakedModel, RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        boolean isHammerAttack = animatable.getAttackState() == MargitAttackState.HAMMER_SLAM;
        if (!animatable.isPhaseTwo() && !isHammerAttack) return;

        RenderLayer glowLayer = RenderLayer.getEntityTranslucent(WEAPON_TEXTURE);
        VertexConsumer glowConsumer = bufferSource.getBuffer(glowLayer);

        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glowLayer, glowConsumer, partialTick, 0xF000F0, packedOverlay, 0xFFFFFFFF);
    }
}

