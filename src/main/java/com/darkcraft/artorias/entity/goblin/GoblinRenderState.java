package com.darkcraft.artorias.entity.goblin;

import net.minecraft.client.render.entity.state.BipedEntityRenderState;

public class GoblinRenderState extends BipedEntityRenderState {
    public int attackState = 0;
    public int attackTicks = 0;
    public boolean isKnight = false;
    public float ageInTicks = 0.0f;
}
