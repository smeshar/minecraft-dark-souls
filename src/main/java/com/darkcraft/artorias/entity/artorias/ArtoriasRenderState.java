package com.darkcraft.artorias.entity.artorias;

import net.minecraft.client.render.entity.state.BipedEntityRenderState;

public class ArtoriasRenderState extends BipedEntityRenderState {
    public int attackState = 0;
    public boolean isStaggered = false;
    public boolean isPhaseTwo = false;
    public int attackTicks = 0;
    public float ageInTicks = 0.0f;
}
