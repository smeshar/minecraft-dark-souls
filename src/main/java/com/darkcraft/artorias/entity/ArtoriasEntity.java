package com.darkcraft.artorias.entity;

import com.darkcraft.artorias.ArtoriasMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;

public class ArtoriasEntity extends HostileEntity {
    public static final int STATE_IDLE = 0;
    public static final int STATE_SOMERSAULT_FLIP = 1;
    public static final int STATE_GLIDE_LUNGE = 2;
    public static final int STATE_SPIN_SLASH = 3;
    public static final int STATE_ABYSS_SLUDGE = 4;
    public static final int STATE_STAGGERED = 5;
    public static final int STATE_PHASE_TWO_ROAR = 6;

    private static final TrackedData<Integer> ATTACK_STATE = DataTracker.registerData(ArtoriasEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> POISE = DataTracker.registerData(ArtoriasEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> PHASE_TWO = DataTracker.registerData(ArtoriasEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    public static final float MAX_POISE = 150.0f;
    private int attackTicks = 0;
    private int staggerTicks = 0;
    private int consecutiveFlips = 0;
    private int timeSinceLastHit = 0;

    private final ServerBossBar bossBar = new ServerBossBar(
            Text.translatable("entity.artorias.knight_artorias"),
            BossBar.Color.BLUE,
            BossBar.Style.NOTCHED_10
    );

    public ArtoriasEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 5000;
        this.setStackInHand(Hand.MAIN_HAND, new ItemStack(ArtoriasMod.ARTORIAS_GREATSWORD));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 800.0) // 400 Hearts boss HP
                .add(EntityAttributes.MOVEMENT_SPEED, 0.35)
                .add(EntityAttributes.ATTACK_DAMAGE, 18.0)
                .add(EntityAttributes.ARMOR, 16.0)
                .add(EntityAttributes.ARMOR_TOUGHNESS, 8.0)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.95)
                .add(EntityAttributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(ATTACK_STATE, STATE_IDLE);
        builder.add(POISE, MAX_POISE);
        builder.add(PHASE_TWO, false);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new ArtoriasCombatGoal(this));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 0.85));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 24.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
    }

    public int getAttackState() {
        return this.dataTracker.get(ATTACK_STATE);
    }

    public void setAttackState(int state) {
        this.dataTracker.set(ATTACK_STATE, state);
        this.attackTicks = 0;
        if (state == STATE_STAGGERED) {
            this.staggerTicks = 120;
        }
    }

    public float getPoise() {
        return this.dataTracker.get(POISE);
    }

    public void setPoise(float poise) {
        this.dataTracker.set(POISE, Math.max(0.0f, Math.min(MAX_POISE, poise)));
    }

    public boolean isPhaseTwo() {
        return this.dataTracker.get(PHASE_TWO);
    }

    public void setPhaseTwo(boolean phaseTwo) {
        this.dataTracker.set(PHASE_TWO, phaseTwo);
        if (phaseTwo) {
            this.bossBar.setColor(BossBar.Color.PURPLE);
            this.bossBar.setName(Text.literal("§5Knight Artorias, the Corrupted Abysswalker"));
        }
    }

    public boolean isStaggered() {
        return getAttackState() == STATE_STAGGERED;
    }

    @Override
    public void tick() {
        super.tick();

        // Update Boss Bar
        if (!this.getEntityWorld().isClient()) {
            this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
        }

        // Ambient dark abyss particles
        if (this.getEntityWorld().isClient()) {
            double px = this.getX() + (this.random.nextDouble() - 0.5) * 1.2;
            double py = this.getY() + this.random.nextDouble() * 2.2;
            double pz = this.getZ() + (this.random.nextDouble() - 0.5) * 1.2;
            this.getEntityWorld().addParticleClient(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 0.0, 0.02, 0.0);
            if (isPhaseTwo()) {
                this.getEntityWorld().addParticleClient(ParticleTypes.WITCH, px, py, pz, 0.0, 0.01, 0.0);
            }
        } else {
            handleServerTick();
        }
    }

    private void handleServerTick() {
        timeSinceLastHit++;

        // Stagger handling
        if (isStaggered()) {
            staggerTicks--;
            this.setVelocity(Vec3d.ZERO);
            this.velocityDirty = true;

            // Swirl dizzy particles above head
            if (this.getEntityWorld() instanceof ServerWorld sw && this.age % 4 == 0) {
                sw.spawnParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 2.8, this.getZ(), 4, 0.2, 0.1, 0.2, 0.02);
            }

            if (staggerTicks <= 0) {
                setAttackState(STATE_IDLE);
                setPoise(MAX_POISE);
            }
            return;
        }

        // Poise recovery over time (if unhit for 7 seconds)
        if (timeSinceLastHit > 140 && getPoise() < MAX_POISE) {
            setPoise(getPoise() + 1.2f);
        }

        // Phase 2 check (50% HP threshold)
        if (!isPhaseTwo() && this.getHealth() <= this.getMaxHealth() * 0.5f && getAttackState() != STATE_PHASE_TWO_ROAR) {
            triggerPhaseTwoTransition();
            return;
        }

        // Attack state ticks
        if (getAttackState() != STATE_IDLE) {
            attackTicks++;
            executeAttackTick();
        }
    }

    private void triggerPhaseTwoTransition() {
        setAttackState(STATE_PHASE_TWO_ROAR);
        this.attackTicks = 0;

        Vec3d back = this.getRotationVec(1.0f).multiply(-0.8).add(0, 0.4, 0);
        this.setVelocity(back);
        this.velocityDirty = true;

        if (this.getEntityWorld() instanceof ServerWorld sw) {
            sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_WARDEN_ROAR, SoundCategory.HOSTILE, 2.0f, 0.7f);
            sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 1.8f, 0.6f);

            sw.getServer().getPlayerManager().broadcast(
                    Text.translatable("message.artorias.phase_two"),
                    false
            );
        }
    }

    private void executeAttackTick() {
        if (!(this.getEntityWorld() instanceof ServerWorld sw)) return;

        // Safety watchdog: an attack cannot get stuck for more than 50 ticks
        if (attackTicks > 50 && getAttackState() != STATE_PHASE_TWO_ROAR) {
            setAttackState(STATE_IDLE);
            return;
        }

        switch (getAttackState()) {
            case STATE_PHASE_TWO_ROAR:
                sw.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 1.2, this.getZ(), 15, 1.5, 1.0, 1.5, 0.05);
                sw.spawnParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 1.0, this.getZ(), 10, 1.2, 0.8, 1.2, 0.04);

                if (attackTicks >= 50) {
                    sw.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.0, this.getZ(), 3, 0.5, 0.5, 0.5, 0.1);
                    sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2.0f, 0.8f);

                    Box blast = new Box(this.getX() - 8, this.getY() - 2, this.getZ() - 8, this.getX() + 8, this.getY() + 4, this.getZ() + 8);
                    for (Entity e : sw.getOtherEntities(this, blast)) {
                        if (e instanceof PlayerEntity p) {
                            p.damage(sw, sw.getDamageSources().mobAttack(this), 16.0f);
                            Vec3d push = p.getEntityPos().subtract(this.getEntityPos()).normalize().multiply(1.5).add(0, 0.35, 0);
                            p.setVelocity(push);
                            p.velocityDirty = true;
                        }
                    }

                    setPhaseTwo(true);
                    setAttackState(STATE_IDLE);
                }
                break;

            case STATE_SOMERSAULT_FLIP:
                if (attackTicks >= 20) {
                    sw.spawnParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(), 2, 0.3, 0.1, 0.3, 0.05);
                    sw.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 0.2, this.getZ(), 35, 2.2, 0.2, 2.2, 0.06);

                    sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.3f, 1.1f);
                    sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_HEAVY_CORE_STEP, SoundCategory.HOSTILE, 1.5f, 0.5f);

                    Box slamBox = new Box(this.getX() - 5, this.getY() - 1, this.getZ() - 5, this.getX() + 5, this.getY() + 3, this.getZ() + 5);
                    for (Entity e : sw.getOtherEntities(this, slamBox)) {
                        if (e instanceof PlayerEntity p) {
                            p.damage(sw, sw.getDamageSources().mobAttack(this), isPhaseTwo() ? 24.0f : 18.0f);
                            p.takeKnockback(1.1, this.getX() - p.getX(), this.getZ() - p.getZ());
                        }
                    }

                    if (isPhaseTwo() && consecutiveFlips < 2 && this.getTarget() != null) {
                        consecutiveFlips++;
                        attackTicks = 0;
                        LivingEntity target = this.getTarget();
                        Vec3d leap = target.getEntityPos().subtract(this.getEntityPos()).normalize().multiply(1.1).add(0, 0.7, 0);
                        this.setVelocity(leap);
                        this.velocityDirty = true;
                        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1.4f, 0.6f);
                        return;
                    }

                    consecutiveFlips = 0;
                    setAttackState(STATE_IDLE);
                }
                break;

            case STATE_GLIDE_LUNGE:
                if (attackTicks >= 8 && attackTicks <= 18) {
                    sw.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 0.5, this.getZ(), 6, 0.4, 0.4, 0.4, 0.02);

                    Box hitBox = this.getBoundingBox().expand(0.8);
                    for (Entity e : sw.getOtherEntities(this, hitBox)) {
                        if (e instanceof PlayerEntity p) {
                            p.damage(sw, sw.getDamageSources().mobAttack(this), 20.0f);
                            p.takeKnockback(0.9, this.getX() - p.getX(), this.getZ() - p.getZ());
                        }
                    }
                }
                if (attackTicks >= 20) {
                    setAttackState(STATE_IDLE);
                }
                break;

            case STATE_SPIN_SLASH:
                if (attackTicks >= 10) {
                    sw.spawnParticles(ParticleTypes.SWEEP_ATTACK, this.getX(), this.getY() + 1.2, this.getZ(), 4, 1.2, 0.2, 1.2, 0.0);
                    sw.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 1.2, this.getZ(), 20, 2.5, 0.2, 2.5, 0.08);

                    sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1.5f, 0.8f);

                    Box spinBox = new Box(this.getX() - 4.5, this.getY() - 1, this.getZ() - 4.5, this.getX() + 4.5, this.getY() + 3, this.getZ() + 4.5);
                    for (Entity e : sw.getOtherEntities(this, spinBox)) {
                        if (e instanceof PlayerEntity p) {
                            p.damage(sw, sw.getDamageSources().mobAttack(this), 19.0f);
                            p.takeKnockback(1.4, this.getX() - p.getX(), this.getZ() - p.getZ());
                        }
                    }
                    setAttackState(STATE_IDLE);
                }
                break;

            case STATE_ABYSS_SLUDGE:
                if (attackTicks >= 14) {
                    sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_WITCH_THROW, SoundCategory.HOSTILE, 1.2f, 0.6f);

                    Vec3d dir = this.getRotationVec(1.0f);
                    for (int i = -2; i <= 2; i++) {
                        double angle = Math.toRadians(i * 18);
                        double vx = dir.x * Math.cos(angle) - dir.z * Math.sin(angle);
                        double vz = dir.x * Math.sin(angle) + dir.z * Math.cos(angle);

                        Vec3d splashPos = this.getEntityPos().add(vx * 5.0, 0.5, vz * 5.0);
                        sw.spawnParticles(ParticleTypes.WITCH, splashPos.x, splashPos.y, splashPos.z, 20, 0.8, 0.3, 0.8, 0.04);
                        sw.spawnParticles(ParticleTypes.SQUID_INK, splashPos.x, splashPos.y, splashPos.z, 10, 0.5, 0.2, 0.5, 0.02);

                        Box sludgeBox = new Box(splashPos.x - 2, splashPos.y - 1, splashPos.z - 2, splashPos.x + 2, splashPos.y + 2, splashPos.z + 2);
                        for (Entity e : sw.getOtherEntities(this, sludgeBox)) {
                            if (e instanceof PlayerEntity p) {
                                p.damage(sw, sw.getDamageSources().mobAttack(this), 14.0f);
                            }
                        }
                    }
                    setAttackState(STATE_IDLE);
                }
                break;

            default:
                setAttackState(STATE_IDLE);
                break;
        }
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if (getAttackState() == STATE_PHASE_TWO_ROAR) {
            return false;
        }

        // CRITICAL RIPOSTE: Player attacks while Artorias is Staggered!
        if (isStaggered() && source.getAttacker() instanceof PlayerEntity player) {
            float riposteDamage = 130.0f;
            super.damage(world, source, riposteDamage);

            setAttackState(STATE_IDLE);
            setPoise(MAX_POISE);

            world.spawnParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 1.5, this.getZ(), 45, 0.8, 0.8, 0.8, 0.2);
            world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, this.getX(), this.getY() + 1.5, this.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
            world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.8f, 0.6f);
            world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ITEM_TRIDENT_THUNDER.value(), SoundCategory.PLAYERS, 1.6f, 1.4f);
            return true;
        }

        boolean damaged = super.damage(world, source, amount);
        if (damaged) {
            this.timeSinceLastHit = 0;

            float poiseDmg = 15.0f;
            if (source.getAttacker() instanceof PlayerEntity player && player.fallDistance > 0.0f) {
                poiseDmg = 38.0f;
            }
            if (source.isOf(net.minecraft.entity.damage.DamageTypes.EXPLOSION) || source.isOf(net.minecraft.entity.damage.DamageTypes.PLAYER_EXPLOSION)) {
                poiseDmg = 50.0f;
            }

            applyPoiseDamage(poiseDmg, source.getAttacker());
        }

        return damaged;
    }

    public void applyPoiseDamage(float amount, Entity attacker) {
        if (isStaggered()) return;

        float currentPoise = getPoise() - amount;
        setPoise(currentPoise);

        if (currentPoise <= 0.0f) {
            setAttackState(STATE_STAGGERED);
            this.staggerTicks = 120; // 6 seconds staggered

            if (this.getEntityWorld() instanceof ServerWorld sw) {
                sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.HOSTILE, 1.8f, 0.7f);
                sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 1.5f, 1.2f);
                sw.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.5, this.getZ(), 2, 0.2, 0.2, 0.2, 0.05);

                sw.getServer().getPlayerManager().broadcast(
                        Text.translatable("message.artorias.stance_break"),
                        false
                );
            }
        }
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
        super.dropEquipment(world, source, causedByPlayer);
        this.dropItem(world, ArtoriasMod.ARTORIAS_GREATSWORD);
        this.dropItem(world, ArtoriasMod.SOUL_OF_ARTORIAS);
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        if (this.getEntityWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.5, this.getZ(), 4, 1.0, 1.0, 1.0, 0.1);
            sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.HOSTILE, 2.0f, 1.0f);

            sw.getServer().getPlayerManager().broadcast(
                    Text.translatable("message.artorias.boss_slain"),
                    false
            );
        }
    }

    static class ArtoriasCombatGoal extends Goal {
        private final ArtoriasEntity boss;
        private int cooldown = 20;
        private int pathUpdateDelay = 0;

        public ArtoriasCombatGoal(ArtoriasEntity boss) {
            this.boss = boss;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = boss.getTarget();
            return target != null && target.isAlive() && !boss.isStaggered();
        }

        @Override
        public boolean shouldContinue() {
            LivingEntity target = boss.getTarget();
            return target != null && target.isAlive() && !boss.isStaggered();
        }

        @Override
        public void start() {
            this.cooldown = 15;
            this.pathUpdateDelay = 0;
        }

        @Override
        public void stop() {
            boss.getNavigation().stop();
            if (boss.getAttackState() != STATE_STAGGERED && boss.getAttackState() != STATE_PHASE_TWO_ROAR) {
                boss.setAttackState(STATE_IDLE);
            }
        }

        @Override
        public void tick() {
            LivingEntity target = boss.getTarget();
            if (target == null) return;

            boss.getLookControl().lookAt(target, 45.0f, 45.0f);
            double distSq = boss.squaredDistanceTo(target);

            cooldown--;
            pathUpdateDelay--;

            // Move towards player when idle
            if (boss.getAttackState() == STATE_IDLE) {
                if (pathUpdateDelay <= 0) {
                    pathUpdateDelay = 8;
                    boss.getNavigation().startMovingTo(target, 1.25);
                }
            } else {
                boss.getNavigation().stop();
            }

            // Attack selector
            if (cooldown <= 0 && boss.getAttackState() == STATE_IDLE) {
                cooldown = boss.isPhaseTwo() ? 18 : 28;

                if (distSq > 100.0) {
                    // Far away: Glide Lunge thrust across arena
                    boss.setAttackState(STATE_GLIDE_LUNGE);
                    Vec3d dash = target.getEntityPos().subtract(boss.getEntityPos()).normalize().multiply(1.35).add(0, 0.1, 0);
                    boss.setVelocity(dash);
                    boss.velocityDirty = true;
                    boss.getEntityWorld().playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                            SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1.4f, 0.6f);
                } else if (distSq > 32.0) {
                    // Mid range: Somersault Flip or Abyss Sludge
                    if (boss.random.nextBoolean()) {
                        boss.setAttackState(STATE_SOMERSAULT_FLIP);
                        Vec3d leap = target.getEntityPos().subtract(boss.getEntityPos()).normalize().multiply(1.15).add(0, 0.75, 0);
                        boss.setVelocity(leap);
                        boss.velocityDirty = true;
                        boss.getEntityWorld().playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1.5f, 0.5f);
                    } else {
                        boss.setAttackState(STATE_ABYSS_SLUDGE);
                    }
                } else {
                    // Close quarters: 360 Spin Slash
                    boss.setAttackState(STATE_SPIN_SLASH);
                }
            }
        }
    }
}
