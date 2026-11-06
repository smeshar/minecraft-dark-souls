package com.darkcraft.artorias.entity.margit;

/**
 * State machine representing Margit, the Fell Omen's combat actions,
 * frame-perfect timing windows, and animation phases.
 */
public enum MargitAttackState {
    IDLE(0, 0, 0, 0, 0, 0, "animation.margit.idle"),

    // Fast horizontal sweeps with deliberate wind-up delay (distance < 4 blocks)
    STAFF_SWING_COMBO(1, 24, 11, 12, 15, 9, "animation.margit.staff_swing_combo"),

    // High jumping forward staff leap slam (distance > 12 blocks)
    STAFF_LEAP(2, 36, 16, 16, 24, 12, "animation.margit.staff_leap"),

    // Double spectral golden dagger toss (distance > 12 blocks)
    DAGGER_TOSS(3, 22, 6, 7, 14, 8, "animation.margit.dagger_toss"),

    // Rapid 360-degree tail swipe when player gets behind boss
    TAIL_SWIPE(4, 18, 5, 6, 10, 8, "animation.margit.tail_swipe"),

    // Phase 2 massive jumping golden hammer overhead slam with ground shockwave
    HAMMER_SLAM(5, 42, 21, 22, 28, 14, "animation.margit.hammer_slam"),

    // Phase 2 transition roar with golden aura and invulnerability (HP < 60%)
    PHASE_TRANSITION(6, 80, 0, 0, 0, 80, "animation.margit.phase2_roar"),

    // Stance / poise break state: boss drops to one knee, vulnerable to critical riposte
    STUNNED(7, 90, 0, 0, 0, 90, "animation.margit.stunned"),

    // Pinned to ground by Margit's Shackle for 3 seconds (60 ticks)
    PINNED(8, 60, 0, 0, 0, 60, "animation.margit.pinned");

    private final int id;
    private final int totalDuration;    // Total animation ticks
    private final int windupTicks;      // Ticks before hitbox activates (delay window)
    private final int activeStartTick;  // First tick of damage application
    private final int activeEndTick;    // Last tick of damage application
    private final int recoveryTicks;    // Ticks after hit before returning to neutral
    private final String animationName; // GeckoLib animation name

    MargitAttackState(int id, int totalDuration, int windupTicks, int activeStartTick, int activeEndTick, int recoveryTicks, String animationName) {
        this.id = id;
        this.totalDuration = totalDuration;
        this.windupTicks = windupTicks;
        this.activeStartTick = activeStartTick;
        this.activeEndTick = activeEndTick;
        this.recoveryTicks = recoveryTicks;
        this.animationName = animationName;
    }

    public int getId() {
        return id;
    }

    public int getTotalDuration() {
        return totalDuration;
    }

    public int getWindupTicks() {
        return windupTicks;
    }

    public int getActiveStartTick() {
        return activeStartTick;
    }

    public int getActiveEndTick() {
        return activeEndTick;
    }

    public int getRecoveryTicks() {
        return recoveryTicks;
    }

    public String getAnimationName() {
        return animationName;
    }

    /**
     * Checks if the attack is in its active damage hitbox window.
     */
    public boolean isActiveFrame(int currentTick) {
        return currentTick >= activeStartTick && currentTick <= activeEndTick;
    }

    /**
     * Checks if Margit is in the wind-up delay phase (before the weapon descends).
     */
    public boolean isWindupPhase(int currentTick) {
        return currentTick < activeStartTick;
    }

    /**
     * Checks if Margit is in the recovery/cooldown phase (punish window).
     */
    public boolean isRecoveryPhase(int currentTick) {
        return currentTick > activeEndTick;
    }

    public boolean isStunOrSpecial() {
        return this == STUNNED || this == PINNED || this == PHASE_TRANSITION;
    }

    public static MargitAttackState fromId(int id) {
        for (MargitAttackState state : values()) {
            if (state.id == id) {
                return state;
            }
        }
        return IDLE;
    }
}

