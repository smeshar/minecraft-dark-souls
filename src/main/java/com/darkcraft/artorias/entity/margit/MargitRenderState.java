package com.darkcraft.artorias.entity.margit;

import net.minecraft.client.render.entity.state.BipedEntityRenderState;

public class MargitRenderState extends BipedEntityRenderState {
    public MargitAttackState attackState = MargitAttackState.IDLE;
    public boolean isStunned = false;
    public boolean isPinned = false;
    public boolean isPhaseTwo = false;
    public int attackTicks = 0;
    public float ageInTicks = 0.0f;
}
