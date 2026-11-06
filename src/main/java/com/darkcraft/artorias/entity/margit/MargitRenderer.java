package com.darkcraft.artorias.entity.margit;

import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Client-side GeckoLib renderer for Margit, the Fell Omen.
 * Configures boss scale, custom render layers (spectral weapons), and shadow radius.
 */
public class MargitRenderer extends GeoEntityRenderer<MargitEntity> {

    public MargitRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new MargitModel());
        this.shadowRadius = 0.85f;
        this.withScale(1.3f);
        this.addRenderLayer(new MargitGoldenWeaponLayer(this));
    }
}
