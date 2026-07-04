package com.bmht.palcraft.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

import com.bmht.palcraft.base.BaseData;
import com.bmht.palcraft.partner.PalInstance;
import com.bmht.palcraft.partner.PalSkill;
import com.bmht.palcraft.partner.PlayerPalData;

import java.util.EnumSet;
import java.util.UUID;

public class SparkitEntity extends PathAwareEntity {
    private static final String OWNER_UUID_KEY = "PalCraftOwnerUuid";
    private static final String INSTANCE_UUID_KEY = "PalCraftInstanceUuid";
    private static final String BASE_UUID_KEY = "PalCraftBaseUuid";
    private static final String BASE_X_KEY = "PalCraftBaseX";
    private static final String BASE_Y_KEY = "PalCraftBaseY";
    private static final String BASE_Z_KEY = "PalCraftBaseZ";
    private static final double FOLLOW_START_DISTANCE_SQUARED = 25.0D;
    private static final double FOLLOW_STOP_DISTANCE_SQUARED = 9.0D;
    private static final double LOW_HEALTH_RETURN_DISTANCE_SQUARED = 4.0D;
    private static final double TELEPORT_DISTANCE_SQUARED = 576.0D;
    private static final float LOW_HEALTH_RATIO = 0.35F;

    private UUID ownerUuid;
    private UUID instanceUuid;
    private UUID baseUuid;
    private BlockPos baseCorePos;
    private UUID lastAssistTargetUuid;
    private UUID lastProtectTargetUuid;
    private boolean lowHealthWarningSent;
    private int palLevel = 1;
    private float palAttack = 3.0F;
    private float palDefense;
    private EnumSet<PalSkill> skills = EnumSet.noneOf(PalSkill.class);
    private int tackleCooldownTicks;
    private int energyBoltCooldownTicks;
    private int selfRepairCooldownTicks;

    public SparkitEntity(EntityType<? extends SparkitEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createSparkitAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 16.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 18.0D);
    }

    public static boolean canSpawn(EntityType<SparkitEntity> type, ServerWorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random) {
        BlockState groundState = world.getBlockState(pos.down());
        return groundState.isIn(BlockTags.ANIMALS_SPAWNABLE_ON) && world.getLightLevel(pos) > 8;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.add(2, new FollowSummonedOwnerGoal(this, 1.2D));
        this.goalSelector.add(4, new BaseWorkerWanderGoal(this, 0.85D));
        this.goalSelector.add(5, new WildWanderGoal(this, 0.9D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient && this.isSummonedPal()) {
            tickSkillCooldowns();
            if (this.age % 10 == 0) {
                updateSummonedCombatTarget();
            }
            tickSummonedSkills();
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        float adjustedAmount = amount;
        if (isSummonedPal() && palDefense > 0.0F && amount > 1.0F) {
            adjustedAmount = Math.max(1.0F, amount - palDefense * 0.35F);
        }
        return super.damage(source, adjustedAmount);
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean attacked = super.tryAttack(target);
        if (attacked && target instanceof LivingEntity livingTarget && hasSkill(PalSkill.TACKLE) && tackleCooldownTicks <= 0) {
            float damage = 1.5F + palLevel * 0.35F;
            livingTarget.damage(this.getDamageSources().mobAttack(this), damage);
            tackleCooldownTicks = PalSkill.TACKLE.cooldownTicks();
            spawnBurstParticles(ParticleTypes.CRIT, livingTarget, 8);
            sendSkillMessage("message.palcraft.skill.tackle", livingTarget);
        }
        return attacked;
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        return super.canTarget(target) && !isOwner(target) && (!isSummonedPal() || !isLowHealth());
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (ownerUuid != null) {
            nbt.putUuid(OWNER_UUID_KEY, ownerUuid);
        }
        if (instanceUuid != null) {
            nbt.putUuid(INSTANCE_UUID_KEY, instanceUuid);
        }
        if (baseUuid != null) {
            nbt.putUuid(BASE_UUID_KEY, baseUuid);
        }
        if (baseCorePos != null) {
            nbt.putInt(BASE_X_KEY, baseCorePos.getX());
            nbt.putInt(BASE_Y_KEY, baseCorePos.getY());
            nbt.putInt(BASE_Z_KEY, baseCorePos.getZ());
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        ownerUuid = nbt.containsUuid(OWNER_UUID_KEY) ? nbt.getUuid(OWNER_UUID_KEY) : null;
        instanceUuid = nbt.containsUuid(INSTANCE_UUID_KEY) ? nbt.getUuid(INSTANCE_UUID_KEY) : null;
        baseUuid = nbt.containsUuid(BASE_UUID_KEY) ? nbt.getUuid(BASE_UUID_KEY) : null;
        baseCorePos = nbt.contains(BASE_X_KEY)
                ? new BlockPos(nbt.getInt(BASE_X_KEY), nbt.getInt(BASE_Y_KEY), nbt.getInt(BASE_Z_KEY))
                : null;
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        sendDeathMessage();
        super.onDeath(damageSource);
        if (this.getWorld() instanceof ServerWorld serverWorld && ownerUuid != null && baseUuid != null && instanceUuid != null) {
            BaseData.get(serverWorld.getServer()).markBaseWorkerDead(ownerUuid, baseUuid, instanceUuid, this.getUuid(), 0.0F);
        } else if (this.getWorld() instanceof ServerWorld serverWorld && ownerUuid != null) {
            PlayerPalData.get(serverWorld.getServer()).markActivePalDead(ownerUuid, this.getUuid());
        }
    }

    public void setSummonedPalData(UUID ownerUuid, PalInstance palInstance) {
        this.ownerUuid = ownerUuid;
        this.instanceUuid = palInstance.instanceUuid();
        this.baseUuid = null;
        this.baseCorePos = null;
        applyPalInstanceStats(palInstance, false);
        this.setPersistent();
    }

    public boolean isSummonedPal() {
        return ownerUuid != null && baseUuid == null;
    }

    public void setBaseWorkerData(UUID ownerUuid, UUID baseUuid, BlockPos baseCorePos, PalInstance palInstance) {
        this.ownerUuid = ownerUuid;
        this.instanceUuid = palInstance.instanceUuid();
        this.baseUuid = baseUuid;
        this.baseCorePos = baseCorePos.toImmutable();
        applyPalInstanceStats(palInstance, false);
        this.setPersistent();
    }

    public boolean isBaseWorker() {
        return ownerUuid != null && baseUuid != null;
    }

    public boolean isCapturedPal() {
        return ownerUuid != null && instanceUuid != null;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public UUID getInstanceUuid() {
        return instanceUuid;
    }

    public void applyPalInstanceStats(PalInstance palInstance, boolean restoreLevelUpHealth) {
        palLevel = palInstance.level();
        palAttack = palInstance.attack();
        palDefense = palInstance.defense();
        skills = palInstance.skills().isEmpty()
                ? EnumSet.noneOf(PalSkill.class)
                : EnumSet.copyOf(palInstance.skills());

        EntityAttributeInstance maxHealthAttribute = getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (maxHealthAttribute != null) {
            maxHealthAttribute.setBaseValue(palInstance.maxHealth());
        }
        EntityAttributeInstance attackAttribute = getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attackAttribute != null) {
            attackAttribute.setBaseValue(palAttack);
        }

        if (restoreLevelUpHealth) {
            setHealth(Math.min(getMaxHealth(), getHealth() + 4.0F));
        } else {
            setHealth(Math.min(getHealth(), getMaxHealth()));
        }
    }

    private ServerPlayerEntity getOwner() {
        if (ownerUuid == null || !(this.getWorld() instanceof ServerWorld serverWorld)) {
            return null;
        }
        return serverWorld.getServer().getPlayerManager().getPlayer(ownerUuid);
    }

    private void updateSummonedCombatTarget() {
        ServerPlayerEntity owner = getOwner();
        if (owner == null || !owner.isAlive() || owner.getWorld() != this.getWorld()) {
            this.setTarget(null);
            return;
        }

        LivingEntity currentTarget = this.getTarget();
        if (currentTarget != null && !isValidCombatTarget(currentTarget, owner)) {
            this.setTarget(null);
            currentTarget = null;
        }

        if (isLowHealth()) {
            if (currentTarget != null) {
                this.setTarget(null);
            }
            sendLowHealthWarning(owner);
            return;
        }

        LivingEntity ownerTarget = owner.getAttacking();
        if (isValidCombatTarget(ownerTarget, owner)) {
            this.setTarget(ownerTarget);
            sendAssistAttackMessage(owner, ownerTarget);
            return;
        }

        LivingEntity attacker = owner.getAttacker();
        if (isValidCombatTarget(attacker, owner)) {
            this.setTarget(attacker);
            sendProtectOwnerMessage(owner, attacker);
        }
    }

    private boolean isValidCombatTarget(LivingEntity target, ServerPlayerEntity owner) {
        return target != null
                && target != this
                && target != owner
                && target.isAlive()
                && target.getWorld() == this.getWorld()
                && this.canTarget(target);
    }

    private boolean isOwner(LivingEntity entity) {
        return ownerUuid != null && entity != null && ownerUuid.equals(entity.getUuid());
    }

    private boolean isLowHealth() {
        return this.getHealth() <= this.getMaxHealth() * LOW_HEALTH_RATIO;
    }

    private void tickSkillCooldowns() {
        if (tackleCooldownTicks > 0) {
            tackleCooldownTicks--;
        }
        if (energyBoltCooldownTicks > 0) {
            energyBoltCooldownTicks--;
        }
        if (selfRepairCooldownTicks > 0) {
            selfRepairCooldownTicks--;
        }
    }

    private void tickSummonedSkills() {
        tryUseSelfRepair();
        tryUseEnergyBolt();
    }

    private boolean hasSkill(PalSkill skill) {
        return skills.contains(skill);
    }

    private void tryUseEnergyBolt() {
        LivingEntity target = getTarget();
        if (!hasSkill(PalSkill.ENERGY_BOLT)
                || energyBoltCooldownTicks > 0
                || target == null
                || !target.isAlive()
                || target.getWorld() != this.getWorld()) {
            return;
        }

        double distanceSquared = squaredDistanceTo(target);
        if (distanceSquared < 9.0D || distanceSquared > 196.0D) {
            return;
        }

        float damage = 2.5F + palLevel * 0.45F + palAttack * 0.25F;
        target.damage(this.getDamageSources().mobAttack(this), damage);
        energyBoltCooldownTicks = PalSkill.ENERGY_BOLT.cooldownTicks();
        spawnLineParticles(target);
        sendSkillMessage("message.palcraft.skill.energy_bolt", target);
    }

    private void tryUseSelfRepair() {
        if (!hasSkill(PalSkill.SELF_REPAIR)
                || selfRepairCooldownTicks > 0
                || this.getHealth() > this.getMaxHealth() * 0.5F) {
            return;
        }

        float healAmount = 3.0F + palLevel * 0.6F;
        heal(healAmount);
        addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 0, false, true, true));
        selfRepairCooldownTicks = PalSkill.SELF_REPAIR.cooldownTicks();
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.HEART, getX(), getBodyY(0.75D), getZ(), 5, 0.35D, 0.35D, 0.35D, 0.02D);
        }
        sendSkillMessage("message.palcraft.skill.self_repair", null);
    }

    private void spawnLineParticles(LivingEntity target) {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        double startX = getX();
        double startY = getBodyY(0.65D);
        double startZ = getZ();
        double endX = target.getX();
        double endY = target.getBodyY(0.65D);
        double endZ = target.getZ();
        for (int i = 1; i <= 8; i++) {
            double progress = i / 8.0D;
            serverWorld.spawnParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    startX + (endX - startX) * progress,
                    startY + (endY - startY) * progress,
                    startZ + (endZ - startZ) * progress,
                    2,
                    0.05D,
                    0.05D,
                    0.05D,
                    0.01D
            );
        }
    }

    private void spawnBurstParticles(net.minecraft.particle.ParticleEffect particleEffect, LivingEntity target, int count) {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(particleEffect, target.getX(), target.getBodyY(0.5D), target.getZ(), count, 0.25D, 0.25D, 0.25D, 0.05D);
        }
    }

    private void sendSkillMessage(String translationKey, LivingEntity target) {
        ServerPlayerEntity owner = getOwner();
        if (owner == null) {
            return;
        }
        if (target == null) {
            owner.sendMessage(Text.translatable(translationKey, this.getDisplayName()), false);
        } else {
            owner.sendMessage(Text.translatable(translationKey, this.getDisplayName(), target.getDisplayName()), false);
        }
    }

    private void sendAssistAttackMessage(ServerPlayerEntity owner, LivingEntity target) {
        UUID targetUuid = target.getUuid();
        if (!targetUuid.equals(lastAssistTargetUuid)) {
            owner.sendMessage(Text.translatable("message.palcraft.combat.assist_attack", this.getDisplayName(), target.getDisplayName()), false);
            lastAssistTargetUuid = targetUuid;
        }
    }

    private void sendProtectOwnerMessage(ServerPlayerEntity owner, LivingEntity target) {
        UUID targetUuid = target.getUuid();
        if (!targetUuid.equals(lastProtectTargetUuid)) {
            owner.sendMessage(Text.translatable("message.palcraft.combat.protect_owner", this.getDisplayName(), target.getDisplayName()), false);
            lastProtectTargetUuid = targetUuid;
        }
    }

    private void sendLowHealthWarning(ServerPlayerEntity owner) {
        if (!lowHealthWarningSent) {
            owner.sendMessage(Text.translatable("message.palcraft.combat.low_health_retreat", this.getDisplayName()), false);
            lowHealthWarningSent = true;
        }
    }

    private void sendDeathMessage() {
        ServerPlayerEntity owner = getOwner();
        if (owner != null) {
            owner.sendMessage(Text.translatable("message.palcraft.combat.pal_death", this.getDisplayName()), false);
        }
    }

    private boolean tryTeleportNearOwner(ServerPlayerEntity owner) {
        BlockPos ownerPos = owner.getBlockPos();
        for (int attempt = 0; attempt < 16; attempt++) {
            int xOffset = this.random.nextBetween(-3, 3);
            int zOffset = this.random.nextBetween(-3, 3);
            int yOffset = this.random.nextBetween(-1, 1);
            BlockPos targetPos = ownerPos.add(xOffset, yOffset, zOffset);
            if (canTeleportTo(targetPos)) {
                this.refreshPositionAndAngles(
                        targetPos.getX() + 0.5D,
                        targetPos.getY(),
                        targetPos.getZ() + 0.5D,
                        this.getYaw(),
                        this.getPitch()
                );
                this.getNavigation().stop();
                return true;
            }
        }
        return false;
    }

    private boolean canTeleportTo(BlockPos pos) {
        World world = this.getWorld();
        BlockPos groundPos = pos.down();
        if (!world.getWorldBorder().contains(pos) || !world.getBlockState(groundPos).isSolidBlock(world, groundPos)) {
            return false;
        }

        double x = pos.getX() + 0.5D;
        double y = pos.getY();
        double z = pos.getZ() + 0.5D;
        Box targetBox = this.getBoundingBox().offset(x - this.getX(), y - this.getY(), z - this.getZ());
        return world.isSpaceEmpty(this, targetBox);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource source) {
        return SoundEvents.ENTITY_FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_FOX_DEATH;
    }

    private static final class FollowSummonedOwnerGoal extends Goal {
        private final SparkitEntity sparkit;
        private final double speed;
        private int updateCountdown;

        private FollowSummonedOwnerGoal(SparkitEntity sparkit, double speed) {
            this.sparkit = sparkit;
            this.speed = speed;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (!sparkit.isSummonedPal()) {
                return false;
            }
            ServerPlayerEntity owner = sparkit.getOwner();
            if (owner == null || !owner.isAlive() || owner.getWorld() != sparkit.getWorld()) {
                return false;
            }

            double distanceSquared = sparkit.squaredDistanceTo(owner);
            return sparkit.isLowHealth()
                    ? distanceSquared > LOW_HEALTH_RETURN_DISTANCE_SQUARED
                    : distanceSquared > FOLLOW_START_DISTANCE_SQUARED;
        }

        @Override
        public boolean shouldContinue() {
            if (!sparkit.isSummonedPal()) {
                return false;
            }
            ServerPlayerEntity owner = sparkit.getOwner();
            if (owner == null || !owner.isAlive() || owner.getWorld() != sparkit.getWorld()) {
                return false;
            }

            double distanceSquared = sparkit.squaredDistanceTo(owner);
            return sparkit.isLowHealth()
                    ? distanceSquared > LOW_HEALTH_RETURN_DISTANCE_SQUARED
                    : distanceSquared > FOLLOW_STOP_DISTANCE_SQUARED;
        }

        @Override
        public void start() {
            updateCountdown = 0;
        }

        @Override
        public void stop() {
            sparkit.getNavigation().stop();
        }

        @Override
        public void tick() {
            ServerPlayerEntity owner = sparkit.getOwner();
            if (owner == null) {
                return;
            }

            sparkit.getLookControl().lookAt(owner, 10.0F, sparkit.getMaxLookPitchChange());
            double distanceSquared = sparkit.squaredDistanceTo(owner);
            if (distanceSquared > TELEPORT_DISTANCE_SQUARED && sparkit.tryTeleportNearOwner(owner)) {
                return;
            }

            if (--updateCountdown <= 0) {
                updateCountdown = 10;
                sparkit.getNavigation().startMovingTo(owner, speed);
            }
        }
    }

    private static final class BaseWorkerWanderGoal extends Goal {
        private final SparkitEntity sparkit;
        private final double speed;
        private int cooldown;

        private BaseWorkerWanderGoal(SparkitEntity sparkit, double speed) {
            this.sparkit = sparkit;
            this.speed = speed;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            return sparkit.isBaseWorker() && sparkit.baseCorePos != null && cooldown-- <= 0;
        }

        @Override
        public void start() {
            cooldown = 60 + sparkit.random.nextInt(80);
            BlockPos target = sparkit.baseCorePos.add(
                    sparkit.random.nextBetween(-5, 5),
                    0,
                    sparkit.random.nextBetween(-5, 5)
            );
            sparkit.getNavigation().startMovingTo(target.getX() + 0.5D, target.getY() + 1.0D, target.getZ() + 0.5D, speed);
        }

        @Override
        public boolean shouldContinue() {
            return sparkit.isBaseWorker() && !sparkit.getNavigation().isIdle();
        }
    }

    private static final class WildWanderGoal extends WanderAroundFarGoal {
        private final SparkitEntity sparkit;

        private WildWanderGoal(SparkitEntity sparkit, double speed) {
            super(sparkit, speed);
            this.sparkit = sparkit;
        }

        @Override
        public boolean canStart() {
            return !sparkit.isSummonedPal() && !sparkit.isBaseWorker() && super.canStart();
        }
    }
}
