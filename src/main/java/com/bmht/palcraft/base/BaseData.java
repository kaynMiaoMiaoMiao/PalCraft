package com.bmht.palcraft.base;

import com.bmht.palcraft.PalCraft;
import com.bmht.palcraft.partner.PalInstance;
import com.bmht.palcraft.partner.PlayerPalData;
import com.bmht.palcraft.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import com.bmht.palcraft.entity.SparkitEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class BaseData extends PersistentState {
    private static final String STATE_ID = PalCraft.MOD_ID + "_bases";
    private static final int DEFAULT_RADIUS = 24;
    private static final int MAX_ASSIGN_DISTANCE = 64;
    private static final int TASK_SCAN_INTERVAL_TICKS = 600;
    private static final int MAX_TASK_QUEUE_SIZE = 8;
    private static final int MAX_TASKS_PER_SCAN = 3;
    private static final int SCAN_Y_MIN = -4;
    private static final int SCAN_Y_MAX = 8;
    private static final int SCAN_STEPS_PER_WORK = 768;
    private static final int TASK_WORK_UNIT_MULTIPLIER = 3;
    private static final float STORAGE_HEAL_MIN = 4.0F;
    private static final float STORAGE_HEAL_RATIO = 0.25F;

    private final List<BaseRecord> bases = new ArrayList<>();

    public static BaseData get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(
                BaseData::fromNbt,
                BaseData::new,
                STATE_ID
        );
    }

    public BaseView createBase(ServerPlayerEntity owner, ServerWorld world, BlockPos corePos) {
        Identifier dimensionId = world.getRegistryKey().getValue();
        Optional<BaseRecord> existing = findBaseAt(dimensionId, corePos);
        if (existing.isPresent()) {
            return existing.get().toView();
        }

        BaseRecord record = new BaseRecord(
                UUID.randomUUID(),
                owner.getUuid(),
                dimensionId,
                corePos.toImmutable(),
                DEFAULT_RADIUS
        );
        bases.add(record);
        markDirty();
        return record.toView();
    }

    public boolean removeBaseAt(ServerWorld world, BlockPos corePos, UUID ownerUuid) {
        Identifier dimensionId = world.getRegistryKey().getValue();
        boolean removed = bases.removeIf(base -> base.ownerUuid.equals(ownerUuid)
                && base.dimensionId.equals(dimensionId)
                && base.corePos.equals(corePos));
        if (removed) {
            markDirty();
        }
        return removed;
    }

    public List<BaseView> getBases(UUID ownerUuid) {
        return bases.stream()
                .filter(base -> base.ownerUuid.equals(ownerUuid))
                .sorted(Comparator.comparing(base -> base.corePos.asLong()))
                .map(BaseRecord::toView)
                .toList();
    }

    public Optional<BaseView> getBaseAt(ServerWorld world, BlockPos corePos, UUID ownerUuid) {
        Identifier dimensionId = world.getRegistryKey().getValue();
        return findBaseAt(dimensionId, corePos)
                .filter(base -> base.ownerUuid.equals(ownerUuid))
                .map(BaseRecord::toView);
    }

    public StoreResult storePalInBestBase(ServerPlayerEntity player, PalInstance palInstance) {
        Optional<BaseRecord> base = findBestStorageBase(player);
        if (base.isEmpty()) {
            return StoreResult.NO_BASE;
        }

        base.get().storedPals.add(palInstance);
        markDirty();
        return StoreResult.STORED;
    }

    public boolean storePlayerPal(ServerPlayerEntity player, UUID baseUuid, int playerSlot) {
        Optional<BaseRecord> matchingBase = findOwnedBase(player.getUuid(), baseUuid);
        if (matchingBase.isEmpty()) {
            return false;
        }

        List<PalInstance> carriedPals = PlayerPalData.get(player.getServer()).getStoredPals(player.getUuid());
        if (playerSlot < 0 || playerSlot >= carriedPals.size()) {
            return false;
        }

        BaseRecord base = matchingBase.get();
        PalInstance candidate = carriedPals.get(playerSlot);
        if (base.storedPals.stream().anyMatch(pal -> pal.instanceUuid().equals(candidate.instanceUuid()))) {
            return false;
        }

        Optional<PalInstance> removedPal = PlayerPalData.get(player.getServer()).takeStoredPal(player, playerSlot);
        if (removedPal.isEmpty()) {
            return false;
        }

        base.storedPals.add(removedPal.get());
        markDirty();
        return true;
    }

    public boolean takeStoredPalToPlayer(ServerPlayerEntity player, UUID baseUuid, int storageSlot) {
        Optional<BaseRecord> matchingBase = findOwnedBase(player.getUuid(), baseUuid);
        if (matchingBase.isEmpty()) {
            return false;
        }

        PlayerPalData palData = PlayerPalData.get(player.getServer());
        if (!palData.hasCarrySpace(player.getUuid())) {
            return false;
        }

        BaseRecord base = matchingBase.get();
        if (storageSlot < 0 || storageSlot >= base.storedPals.size()) {
            return false;
        }

        PalInstance pal = base.storedPals.get(storageSlot);
        base.deployments.stream()
                .filter(deployment -> deployment.palUuid.equals(pal.instanceUuid()))
                .findFirst()
                .ifPresent(deployment -> discardWorkerEntity(player.getServer(), base, deployment));
        base.deployments.removeIf(deployment -> deployment.palUuid.equals(pal.instanceUuid()));
        base.assignments.removeIf(assignment -> assignment.palUuid.equals(pal.instanceUuid()));

        PalInstance removedPal = base.storedPals.remove(storageSlot);
        if (!palData.addStoredPal(player.getUuid(), removedPal)) {
            base.storedPals.add(storageSlot, removedPal);
            return false;
        }

        markDirty();
        return true;
    }

    public AssignResult assignStoredPal(UUID ownerUuid, UUID baseUuid, int storageSlot, BaseWorkType requestedWorkType) {
        Optional<BaseRecord> matchingBase = bases.stream()
                .filter(base -> base.ownerUuid.equals(ownerUuid))
                .filter(base -> base.baseUuid.equals(baseUuid))
                .findFirst();
        if (matchingBase.isEmpty()) {
            return AssignResult.noBaseNearby();
        }

        BaseRecord base = matchingBase.get();
        if (storageSlot < 0 || storageSlot >= base.storedPals.size()) {
            return AssignResult.invalidPal();
        }

        PalInstance pal = base.storedPals.get(storageSlot);
        if (!base.hasDeployment(pal.instanceUuid())) {
            return AssignResult.notDeployed(base.toView());
        }
        if (base.hasAssignment(pal.instanceUuid())) {
            return AssignResult.alreadyAssigned(base.toView());
        }

        BaseWorkType workType = requestedWorkType == null || !requestedWorkType.isAssignable()
                ? BaseWorkType.bestFor(pal)
                : requestedWorkType;
        base.assignments.add(new AssignedPal(pal.instanceUuid(), workType));
        markDirty();
        return AssignResult.assigned(base.toView(), workType);
    }

    public boolean unassignStoredPal(MinecraftServer server, UUID ownerUuid, UUID baseUuid, UUID palUuid) {
        Optional<BaseRecord> matchingBase = bases.stream()
                .filter(base -> base.ownerUuid.equals(ownerUuid))
                .filter(base -> base.baseUuid.equals(baseUuid))
                .findFirst();
        if (matchingBase.isEmpty()) {
            return false;
        }

        BaseRecord base = matchingBase.get();
        boolean removed = base.assignments.removeIf(assignment -> assignment.palUuid.equals(palUuid));
        if (removed) {
            markDirty();
        }
        return removed;
    }

    public DeployResult deployStoredPal(MinecraftServer server, UUID ownerUuid, UUID baseUuid, int storageSlot) {
        Optional<BaseRecord> matchingBase = bases.stream()
                .filter(base -> base.ownerUuid.equals(ownerUuid))
                .filter(base -> base.baseUuid.equals(baseUuid))
                .findFirst();
        if (matchingBase.isEmpty()) {
            return DeployResult.NO_BASE;
        }

        BaseRecord base = matchingBase.get();
        if (storageSlot < 0 || storageSlot >= base.storedPals.size()) {
            return DeployResult.INVALID_PAL;
        }

        PalInstance pal = base.storedPals.get(storageSlot);
        if (base.hasDeployment(pal.instanceUuid())) {
            return DeployResult.ALREADY_DEPLOYED;
        }

        ServerWorld world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, base.dimensionId));
        if (world == null || pal.health() <= 0.0F) {
            return DeployResult.INVALID_PAL;
        }

        DeployedPal deployment = new DeployedPal(pal.instanceUuid());
        if (!ensureWorkerEntity(world, base, deployment, pal)) {
            return DeployResult.SPAWN_FAILED;
        }

        base.deployments.add(deployment);
        markDirty();
        return DeployResult.DEPLOYED;
    }

    public boolean recallDeployedPal(MinecraftServer server, UUID ownerUuid, UUID baseUuid, UUID palUuid) {
        Optional<BaseRecord> matchingBase = bases.stream()
                .filter(base -> base.ownerUuid.equals(ownerUuid))
                .filter(base -> base.baseUuid.equals(baseUuid))
                .findFirst();
        if (matchingBase.isEmpty()) {
            return false;
        }

        BaseRecord base = matchingBase.get();
        base.deployments.stream()
                .filter(deployment -> deployment.palUuid.equals(palUuid))
                .findFirst()
                .ifPresent(deployment -> discardWorkerEntity(server, base, deployment));
        boolean removed = base.deployments.removeIf(deployment -> deployment.palUuid.equals(palUuid));
        boolean unassigned = base.assignments.removeIf(assignment -> assignment.palUuid.equals(palUuid));
        if (removed || unassigned) {
            markDirty();
        }
        return removed;
    }

    public AssignResult assignPalToNearestBase(ServerPlayerEntity player, PalInstance palInstance, BaseWorkType requestedWorkType) {
        Optional<BaseRecord> duplicate = bases.stream()
                .filter(base -> base.ownerUuid.equals(player.getUuid()))
                .filter(base -> base.hasAssignment(palInstance.instanceUuid()))
                .findFirst();
        if (duplicate.isPresent()) {
            return AssignResult.alreadyAssigned(duplicate.get().toView());
        }

        Optional<BaseRecord> nearestBase = findNearestBase(player);
        if (nearestBase.isEmpty()) {
            return AssignResult.noBaseNearby();
        }

        BaseRecord base = nearestBase.get();
        BaseWorkType workType = requestedWorkType == null || !requestedWorkType.isAssignable()
                ? BaseWorkType.bestFor(palInstance)
                : requestedWorkType;
        if (base.storedPals.stream().noneMatch(storedPal -> storedPal.instanceUuid().equals(palInstance.instanceUuid()))) {
            base.storedPals.add(palInstance);
        }
        if (!base.hasDeployment(palInstance.instanceUuid())) {
            return AssignResult.notDeployed(base.toView());
        }
        base.assignments.add(new AssignedPal(palInstance.instanceUuid(), workType));
        markDirty();
        return AssignResult.assigned(base.toView(), workType);
    }

    public void tickWork(MinecraftServer server) {
        PlayerPalData palData = PlayerPalData.get(server);
        boolean changed = false;
        for (BaseRecord base : bases) {
            changed |= tickStoredPalRecovery(base);

            if (base.deployments.isEmpty() && base.assignments.isEmpty()) {
                continue;
            }

            ServerWorld world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, base.dimensionId));
            if (world == null) {
                continue;
            }

            List<PalInstance> ownerPals = palData.getStoredPals(base.ownerUuid);
            for (DeployedPal deployment : base.deployments) {
                Optional<PalInstance> pal = findPal(base.storedPals, deployment.palUuid)
                        .or(() -> findPal(ownerPals, deployment.palUuid));
                if (pal.isEmpty()) {
                    continue;
                }
                changed |= ensureWorkerEntity(world, base, deployment, pal.get());
            }

            if (base.assignments.isEmpty()) {
                continue;
            }

            changed |= base.clearStaleTaskWorkers();

            if (server.getTicks() >= base.nextTaskScanTick && base.taskQueue.size() < MAX_TASK_QUEUE_SIZE) {
                changed |= enqueueDiscoveredTasks(base, world, server.getTicks());
            }

            for (AssignedPal assignment : base.assignments) {
                Optional<DeployedPal> deployment = base.findDeployment(assignment.palUuid);
                if (deployment.isEmpty()) {
                    continue;
                }
                Optional<PalInstance> pal = findPal(base.storedPals, assignment.palUuid)
                        .or(() -> findPal(ownerPals, assignment.palUuid));
                if (pal.isEmpty()) {
                    continue;
                }

                Optional<BaseTask> task = base.findTaskFor(assignment.workType, assignment.palUuid);
                if (task.isEmpty()) {
                    clearWorkerTarget(world, deployment.get());
                    continue;
                }

                BaseTask baseTask = task.get();
                baseTask.workerPalUuid = assignment.palUuid;
                Optional<SparkitEntity> workerEntity = findWorkerEntity(world, deployment.get());
                if (workerEntity.isEmpty()) {
                    continue;
                }

                SparkitEntity worker = workerEntity.get();
                if (!isValidWorkTarget(world.getBlockState(baseTask.targetPos), baseTask.workType)) {
                    worker.clearBaseWorkTarget();
                    base.taskQueue.remove(baseTask);
                    changed = true;
                    continue;
                }

                worker.setBaseWorkTarget(baseTask.taskUuid, baseTask.workType, baseTask.targetPos);
                if (!worker.canProgressBaseTask(baseTask.taskUuid, baseTask.targetPos)) {
                    continue;
                }

                baseTask.progress += Math.max(1, assignment.workType.suitability(pal.get()));
                if (baseTask.progress >= baseTask.requiredWork) {
                    if (completeTask(world, baseTask)) {
                        worker.clearBaseWorkTarget();
                        base.taskQueue.remove(baseTask);
                    }
                }
                changed = true;
            }
        }
        if (changed) {
            markDirty();
        }
    }

    private boolean tickStoredPalRecovery(BaseRecord base) {
        boolean changed = false;
        for (int i = 0; i < base.storedPals.size(); i++) {
            PalInstance pal = base.storedPals.get(i);
            if (base.hasDeployment(pal.instanceUuid()) || pal.health() >= pal.maxHealth()) {
                continue;
            }

            float healAmount = Math.max(STORAGE_HEAL_MIN, pal.maxHealth() * STORAGE_HEAL_RATIO);
            float health = Math.min(pal.maxHealth(), Math.max(0.0F, pal.health()) + healAmount);
            base.storedPals.set(i, pal.withHealth(health));
            changed = true;
        }
        return changed;
    }

    private boolean enqueueDiscoveredTasks(BaseRecord base, ServerWorld world, int serverTicks) {
        int queued = 0;
        for (BaseWorkType workType : base.preferredWorkTypes()) {
            if (queued >= MAX_TASKS_PER_SCAN || base.taskQueue.size() >= MAX_TASK_QUEUE_SIZE) {
                break;
            }
            Optional<BlockPos> targetPos = findTargetForWork(base, world, workType);
            if (targetPos.isEmpty()) {
                continue;
            }
            base.taskQueue.add(new BaseTask(UUID.randomUUID(), workType, 0, workType.requiredWork() * TASK_WORK_UNIT_MULTIPLIER, targetPos.get(), null));
            queued++;
        }
        base.nextTaskScanTick = serverTicks + TASK_SCAN_INTERVAL_TICKS;
        return queued > 0;
    }

    private Optional<BlockPos> findTargetForWork(BaseRecord base, ServerWorld world, BaseWorkType workType) {
        if (workType == BaseWorkType.HAULING || workType == BaseWorkType.MANUFACTURING) {
            return Optional.empty();
        }

        int diameter = base.radius * 2 + 1;
        int scanHeight = SCAN_Y_MAX - SCAN_Y_MIN + 1;
        int totalPositions = diameter * diameter * scanHeight;
        int steps = Math.min(SCAN_STEPS_PER_WORK, totalPositions);
        for (int step = 0; step < steps; step++) {
            int index = Math.floorMod(base.scanCursor++, totalPositions);
            int yIndex = index % scanHeight;
            int horizontalIndex = index / scanHeight;
            int dx = horizontalIndex % diameter - base.radius;
            int dz = horizontalIndex / diameter - base.radius;
            BlockPos pos = base.corePos.add(dx, SCAN_Y_MIN + yIndex, dz);
            if (base.hasQueuedTarget(workType, pos)) {
                continue;
            }

            BlockState state = world.getBlockState(pos);
            if (isValidWorkTarget(state, workType)) {
                return Optional.of(pos.toImmutable());
            }
        }
        return Optional.empty();
    }

    private boolean completeTask(ServerWorld world, BaseTask task) {
        if (task.workType == BaseWorkType.HAULING || task.workType == BaseWorkType.MANUFACTURING) {
            return false;
        }

        BlockState state = world.getBlockState(task.targetPos);
        if (!isValidWorkTarget(state, task.workType)) {
            return false;
        }

        ItemStack result = createTaskResult(state, task.workType);
        if (result.isEmpty() || !insertIntoNearbyStorage(world, task.targetPos, result)) {
            return false;
        }

        if (task.workType == BaseWorkType.PLANTING && state.getBlock() instanceof CropBlock cropBlock) {
            world.breakBlock(task.targetPos, false);
            world.setBlockState(task.targetPos, cropBlock.withAge(0), Block.NOTIFY_ALL);
            return true;
        }

        world.breakBlock(task.targetPos, false);
        return true;
    }

    private ItemStack createTaskResult(BlockState state, BaseWorkType workType) {
        if (workType == BaseWorkType.PLANTING) {
            return new ItemStack(Items.WHEAT);
        }
        if (state.getBlock().asItem() == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(state.getBlock().asItem());
    }

    private boolean insertIntoNearbyStorage(ServerWorld world, BlockPos taskPos, ItemStack stack) {
        Optional<BaseRecord> base = bases.stream()
                .filter(record -> record.dimensionId.equals(world.getRegistryKey().getValue()))
                .filter(record -> isWithinBase(record, taskPos))
                .findFirst();
        if (base.isEmpty()) {
            return false;
        }

        for (BlockPos pos : scanBasePositions(base.get())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof Inventory inventory && insertStack(inventory, stack.copy())) {
                return true;
            }
        }
        return false;
    }

    private boolean insertStack(Inventory inventory, ItemStack stack) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack existing = inventory.getStack(slot);
            if (existing.isEmpty()) {
                int amount = Math.min(stack.getCount(), inventory.getMaxCountPerStack());
                ItemStack inserted = stack.copy();
                inserted.setCount(amount);
                inventory.setStack(slot, inserted);
                stack.decrement(amount);
                inventory.markDirty();
            } else if (ItemStack.canCombine(existing, stack) && existing.getCount() < Math.min(existing.getMaxCount(), inventory.getMaxCountPerStack())) {
                int limit = Math.min(existing.getMaxCount(), inventory.getMaxCountPerStack());
                int amount = Math.min(stack.getCount(), limit - existing.getCount());
                existing.increment(amount);
                stack.decrement(amount);
                inventory.markDirty();
            }

            if (stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidWorkTarget(BlockState state, BaseWorkType workType) {
        return switch (workType) {
            case MINING -> isMineableResource(state);
            case LOGGING -> state.isIn(BlockTags.LOGS);
            case PLANTING -> state.getBlock() instanceof CropBlock cropBlock && cropBlock.isMature(state);
            case HAULING, MANUFACTURING -> true;
        };
    }

    private boolean isMineableResource(BlockState state) {
        if (state.isOf(ModBlocks.BASE_CORE) || !state.isIn(BlockTags.PICKAXE_MINEABLE)) {
            return false;
        }

        String blockPath = Registries.BLOCK.getId(state.getBlock()).getPath();
        return blockPath.endsWith("_ore")
                || state.isOf(Blocks.STONE)
                || state.isOf(Blocks.DEEPSLATE)
                || state.isOf(Blocks.COBBLESTONE)
                || state.isOf(Blocks.COBBLED_DEEPSLATE)
                || state.isOf(Blocks.BLACKSTONE)
                || state.isOf(Blocks.SANDSTONE);
    }

    private boolean ensureWorkerEntity(ServerWorld world, BaseRecord base, DeployedPal deployment, PalInstance palInstance) {
        if (deployment.entityUuid != null) {
            Entity existing = world.getEntity(deployment.entityUuid);
            if (existing != null && existing.isAlive()) {
                keepWorkerNearBase(existing, base);
                return false;
            }
            deployment.entityUuid = null;
        }

        if (palInstance.health() <= 0.0F) {
            return false;
        }

        EntityType<?> entityType = Registries.ENTITY_TYPE.get(palInstance.speciesId());
        Entity entity = entityType.create(world);
        if (!(entity instanceof LivingEntity livingEntity)) {
            return false;
        }

        BlockPos spawnPos = findWorkerSpawnPos(world, base);
        livingEntity.refreshPositionAndAngles(
                spawnPos.getX() + 0.5D,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5D,
                world.random.nextFloat() * 360.0F,
                0.0F
        );
        if (!palInstance.customName().isBlank()) {
            livingEntity.setCustomName(Text.literal(palInstance.customName()));
        }
        if (livingEntity instanceof MobEntity mobEntity) {
            mobEntity.setPersistent();
        }
        if (livingEntity instanceof SparkitEntity sparkitEntity) {
            sparkitEntity.setBaseWorkerData(base.ownerUuid, base.baseUuid, base.corePos, palInstance);
        }
        livingEntity.setHealth(Math.max(1.0F, Math.min(palInstance.health(), livingEntity.getMaxHealth())));
        if (!world.spawnEntity(livingEntity)) {
            return false;
        }

        deployment.entityUuid = livingEntity.getUuid();
        return true;
    }

    private void keepWorkerNearBase(Entity entity, BaseRecord base) {
        double maxDistanceSquared = (base.radius + 6.0D) * (base.radius + 6.0D);
        if (entity.getBlockPos().getSquaredDistance(base.corePos) <= maxDistanceSquared) {
            return;
        }
        entity.refreshPositionAndAngles(
                base.corePos.getX() + 0.5D,
                base.corePos.getY() + 1.0D,
                base.corePos.getZ() + 0.5D,
                entity.getYaw(),
                entity.getPitch()
        );
    }

    private void discardWorkerEntity(MinecraftServer server, BaseRecord base, DeployedPal deployment) {
        if (deployment.entityUuid == null) {
            return;
        }
        ServerWorld world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, base.dimensionId));
        if (world == null) {
            return;
        }
        Entity entity = world.getEntity(deployment.entityUuid);
        if (entity != null) {
            if (entity instanceof LivingEntity livingEntity) {
                replacePalHealth(base.storedPals, deployment.palUuid, livingEntity.getHealth());
            }
            entity.discard();
        }
        deployment.entityUuid = null;
    }

    private BlockPos findWorkerSpawnPos(ServerWorld world, BaseRecord base) {
        for (int attempt = 0; attempt < 16; attempt++) {
            int xOffset = world.random.nextBetween(-3, 3);
            int zOffset = world.random.nextBetween(-3, 3);
            BlockPos pos = base.corePos.add(xOffset, 1, zOffset);
            if (world.isSpaceEmpty(null, net.minecraft.util.math.Box.from(net.minecraft.util.math.Vec3d.of(pos)))) {
                return pos;
            }
        }
        return base.corePos.up();
    }

    private Optional<SparkitEntity> findWorkerEntity(ServerWorld world, DeployedPal deployment) {
        if (deployment.entityUuid == null) {
            return Optional.empty();
        }
        Entity entity = world.getEntity(deployment.entityUuid);
        if (entity instanceof SparkitEntity sparkitEntity && sparkitEntity.isAlive()) {
            return Optional.of(sparkitEntity);
        }
        return Optional.empty();
    }

    private void clearWorkerTarget(ServerWorld world, DeployedPal deployment) {
        findWorkerEntity(world, deployment).ifPresent(SparkitEntity::clearBaseWorkTarget);
    }

    public void markBaseWorkerDead(UUID ownerUuid, UUID baseUuid, UUID palUuid, UUID entityUuid, float health) {
        Optional<BaseRecord> matchingBase = bases.stream()
                .filter(base -> base.ownerUuid.equals(ownerUuid))
                .filter(base -> base.baseUuid.equals(baseUuid))
                .findFirst();
        if (matchingBase.isEmpty()) {
            return;
        }

        BaseRecord base = matchingBase.get();
        replacePalHealth(base.storedPals, palUuid, health);
        for (DeployedPal deployment : base.deployments) {
            if (deployment.palUuid.equals(palUuid) && entityUuid.equals(deployment.entityUuid)) {
                deployment.entityUuid = null;
            }
        }
        base.deployments.removeIf(deployment -> deployment.palUuid.equals(palUuid));
        base.assignments.removeIf(assignment -> assignment.palUuid.equals(palUuid));
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList baseList = new NbtList();
        for (BaseRecord base : bases) {
            NbtCompound baseNbt = new NbtCompound();
            baseNbt.putUuid("BaseUuid", base.baseUuid);
            baseNbt.putUuid("OwnerUuid", base.ownerUuid);
            baseNbt.putString("Dimension", base.dimensionId.toString());
            baseNbt.putInt("X", base.corePos.getX());
            baseNbt.putInt("Y", base.corePos.getY());
            baseNbt.putInt("Z", base.corePos.getZ());
            baseNbt.putInt("Radius", base.radius);
            baseNbt.putLong("NextTaskScanTick", base.nextTaskScanTick);
            baseNbt.putInt("ScanCursor", base.scanCursor);

            NbtList assignedPals = new NbtList();
            for (AssignedPal assignment : base.assignments) {
                NbtCompound palNbt = new NbtCompound();
                palNbt.putUuid("PalUuid", assignment.palUuid);
                palNbt.putString("WorkType", assignment.workType.id());
                assignedPals.add(palNbt);
            }
            baseNbt.put("AssignedPals", assignedPals);

            NbtList deployedPals = new NbtList();
            for (DeployedPal deployment : base.deployments) {
                NbtCompound palNbt = new NbtCompound();
                palNbt.putUuid("PalUuid", deployment.palUuid);
                if (deployment.entityUuid != null) {
                    palNbt.putUuid("EntityUuid", deployment.entityUuid);
                }
                deployedPals.add(palNbt);
            }
            baseNbt.put("DeployedPals", deployedPals);

            NbtList storedPals = new NbtList();
            for (PalInstance palInstance : base.storedPals) {
                storedPals.add(palInstance.writeNbt());
            }
            baseNbt.put("StoredPals", storedPals);

            NbtList stockList = new NbtList();
            for (Map.Entry<BaseWorkType, Long> stockEntry : base.stock.entrySet()) {
                NbtCompound stockNbt = new NbtCompound();
                stockNbt.putString("WorkType", stockEntry.getKey().id());
                stockNbt.putLong("Amount", stockEntry.getValue());
                stockList.add(stockNbt);
            }
            baseNbt.put("Stock", stockList);

            NbtList taskList = new NbtList();
            for (BaseTask task : base.taskQueue) {
                NbtCompound taskNbt = new NbtCompound();
                taskNbt.putUuid("TaskUuid", task.taskUuid);
                taskNbt.putString("WorkType", task.workType.id());
                taskNbt.putInt("Progress", task.progress);
                taskNbt.putInt("RequiredWork", task.requiredWork);
                taskNbt.putInt("TargetX", task.targetPos.getX());
                taskNbt.putInt("TargetY", task.targetPos.getY());
                taskNbt.putInt("TargetZ", task.targetPos.getZ());
                if (task.workerPalUuid != null) {
                    taskNbt.putUuid("WorkerPalUuid", task.workerPalUuid);
                }
                taskList.add(taskNbt);
            }
            baseNbt.put("Tasks", taskList);
            baseList.add(baseNbt);
        }
        nbt.put("Bases", baseList);
        return nbt;
    }

    private static BaseData fromNbt(NbtCompound nbt) {
        BaseData data = new BaseData();
        NbtList baseList = nbt.getList("Bases", NbtElement.COMPOUND_TYPE);
        for (NbtElement baseElement : baseList) {
            NbtCompound baseNbt = (NbtCompound) baseElement;
            Identifier dimensionId = Identifier.tryParse(baseNbt.getString("Dimension"));
            if (dimensionId == null) {
                dimensionId = new Identifier("minecraft", "overworld");
            }

            BaseRecord record = new BaseRecord(
                    baseNbt.getUuid("BaseUuid"),
                    baseNbt.getUuid("OwnerUuid"),
                    dimensionId,
                    new BlockPos(baseNbt.getInt("X"), baseNbt.getInt("Y"), baseNbt.getInt("Z")),
                    baseNbt.getInt("Radius")
            );
            record.nextTaskScanTick = baseNbt.getInt("NextTaskScanTick");
            record.scanCursor = baseNbt.getInt("ScanCursor");

            NbtList assignedPals = baseNbt.getList("AssignedPals", NbtElement.COMPOUND_TYPE);
            for (NbtElement palElement : assignedPals) {
                NbtCompound palNbt = (NbtCompound) palElement;
                BaseWorkType workType = palNbt.contains("WorkType")
                        ? BaseWorkType.fromId(palNbt.getString("WorkType"))
                        : BaseWorkType.HAULING;
                if (!workType.isAssignable()) {
                    continue;
                }
                AssignedPal assignment = new AssignedPal(palNbt.getUuid("PalUuid"), workType);
                record.assignments.add(assignment);

                // Migration path for the previous implementation, where assignment implied a spawned worker.
                if (palNbt.containsUuid("EntityUuid")) {
                    DeployedPal deployment = new DeployedPal(palNbt.getUuid("PalUuid"));
                    deployment.entityUuid = palNbt.getUuid("EntityUuid");
                    record.deployments.add(deployment);
                }
            }

            NbtList deployedPals = baseNbt.getList("DeployedPals", NbtElement.COMPOUND_TYPE);
            for (NbtElement palElement : deployedPals) {
                NbtCompound palNbt = (NbtCompound) palElement;
                if (record.hasDeployment(palNbt.getUuid("PalUuid"))) {
                    continue;
                }
                DeployedPal deployment = new DeployedPal(palNbt.getUuid("PalUuid"));
                if (palNbt.containsUuid("EntityUuid")) {
                    deployment.entityUuid = palNbt.getUuid("EntityUuid");
                }
                record.deployments.add(deployment);
            }

            NbtList storedPals = baseNbt.getList("StoredPals", NbtElement.COMPOUND_TYPE);
            for (NbtElement palElement : storedPals) {
                record.storedPals.add(PalInstance.fromNbt((NbtCompound) palElement));
            }

            if (baseNbt.contains("Stock", NbtElement.LIST_TYPE)) {
                NbtList stockList = baseNbt.getList("Stock", NbtElement.COMPOUND_TYPE);
                for (NbtElement stockElement : stockList) {
                    NbtCompound stockNbt = (NbtCompound) stockElement;
                    record.addStock(BaseWorkType.fromId(stockNbt.getString("WorkType")), stockNbt.getLong("Amount"));
                }
            } else if (baseNbt.contains("GatheredMaterials")) {
                record.addStock(BaseWorkType.HAULING, baseNbt.getLong("GatheredMaterials"));
            }

            NbtList taskList = baseNbt.getList("Tasks", NbtElement.COMPOUND_TYPE);
            for (NbtElement taskElement : taskList) {
                NbtCompound taskNbt = (NbtCompound) taskElement;
                BaseWorkType workType = BaseWorkType.fromId(taskNbt.getString("WorkType"));
                if (!workType.isAssignable()) {
                    continue;
                }
                record.taskQueue.add(new BaseTask(
                        taskNbt.getUuid("TaskUuid"),
                        workType,
                        taskNbt.getInt("Progress"),
                        taskNbt.getInt("RequiredWork"),
                        taskNbt.contains("TargetX")
                                ? new BlockPos(taskNbt.getInt("TargetX"), taskNbt.getInt("TargetY"), taskNbt.getInt("TargetZ"))
                                : record.corePos,
                        taskNbt.containsUuid("WorkerPalUuid") ? taskNbt.getUuid("WorkerPalUuid") : null
                ));
            }
            data.bases.add(record);
        }
        return data;
    }

    private Optional<BaseRecord> findNearestBase(ServerPlayerEntity player) {
        Identifier dimensionId = player.getWorld().getRegistryKey().getValue();
        BlockPos playerPos = player.getBlockPos();
        int maxDistanceSquared = MAX_ASSIGN_DISTANCE * MAX_ASSIGN_DISTANCE;
        return bases.stream()
                .filter(base -> base.ownerUuid.equals(player.getUuid()))
                .filter(base -> base.dimensionId.equals(dimensionId))
                .filter(base -> base.corePos.getSquaredDistance(playerPos) <= maxDistanceSquared)
                .min(Comparator.comparingDouble(base -> base.corePos.getSquaredDistance(playerPos)));
    }

    private Optional<BaseRecord> findOwnedBase(UUID ownerUuid, UUID baseUuid) {
        return bases.stream()
                .filter(base -> base.ownerUuid.equals(ownerUuid))
                .filter(base -> base.baseUuid.equals(baseUuid))
                .findFirst();
    }

    private Optional<BaseRecord> findBestStorageBase(ServerPlayerEntity player) {
        Identifier dimensionId = player.getWorld().getRegistryKey().getValue();
        BlockPos playerPos = player.getBlockPos();
        return bases.stream()
                .filter(base -> base.ownerUuid.equals(player.getUuid()))
                .min(Comparator
                        .comparing((BaseRecord base) -> !base.dimensionId.equals(dimensionId))
                        .thenComparingDouble(base -> base.dimensionId.equals(dimensionId)
                                ? base.corePos.getSquaredDistance(playerPos)
                                : Double.MAX_VALUE));
    }

    private Optional<BaseRecord> findBaseAt(Identifier dimensionId, BlockPos corePos) {
        return bases.stream()
                .filter(base -> base.dimensionId.equals(dimensionId) && base.corePos.equals(corePos))
                .findFirst();
    }

    private boolean isWithinBase(BaseRecord base, BlockPos pos) {
        return Math.abs(pos.getX() - base.corePos.getX()) <= base.radius
                && Math.abs(pos.getZ() - base.corePos.getZ()) <= base.radius
                && pos.getY() >= base.corePos.getY() + SCAN_Y_MIN
                && pos.getY() <= base.corePos.getY() + SCAN_Y_MAX;
    }

    private List<BlockPos> scanBasePositions(BaseRecord base) {
        List<BlockPos> positions = new ArrayList<>();
        for (int dx = -base.radius; dx <= base.radius; dx++) {
            for (int dz = -base.radius; dz <= base.radius; dz++) {
                for (int dy = SCAN_Y_MIN; dy <= SCAN_Y_MAX; dy++) {
                    positions.add(base.corePos.add(dx, dy, dz));
                }
            }
        }
        return positions;
    }

    public int countStorageBlocks(MinecraftServer server, UUID baseUuid) {
        Optional<BaseRecord> matchingBase = bases.stream()
                .filter(base -> base.baseUuid.equals(baseUuid))
                .findFirst();
        if (matchingBase.isEmpty()) {
            return 0;
        }

        BaseRecord base = matchingBase.get();
        ServerWorld world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, base.dimensionId));
        if (world == null) {
            return 0;
        }

        int count = 0;
        for (BlockPos pos : scanBasePositions(base)) {
            if (world.getBlockEntity(pos) instanceof Inventory) {
                count++;
            }
        }
        return count;
    }

    public static String formatPos(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    public record BaseView(
            UUID baseUuid,
            Identifier dimensionId,
            BlockPos corePos,
            int radius,
            int assignedCount,
            long gatheredMaterials,
            int queuedTasks,
            String stockSummary,
            String assignmentSummary,
            String taskProgressSummary,
            List<BasePalView> storedPals,
            List<AssignedPalView> assignedPals
    ) {
        public String corePositionText() {
            return formatPos(corePos);
        }
    }

    public record BasePalView(int slot, PalInstance pal, boolean deployed, boolean assigned, BaseWorkType workType, String workProgressSummary) {
    }

    public record AssignedPalView(UUID palUuid, BaseWorkType workType) {
    }

    public record AssignResult(AssignStatus status, BaseView base, BaseWorkType workType) {
        public static AssignResult assigned(BaseView base, BaseWorkType workType) {
            return new AssignResult(AssignStatus.ASSIGNED, base, workType);
        }

        public static AssignResult alreadyAssigned(BaseView base) {
            return new AssignResult(AssignStatus.ALREADY_ASSIGNED, base, null);
        }

        public static AssignResult noBaseNearby() {
            return new AssignResult(AssignStatus.NO_BASE_NEARBY, null, null);
        }

        public static AssignResult invalidPal() {
            return new AssignResult(AssignStatus.INVALID_PAL, null, null);
        }

        public static AssignResult notDeployed(BaseView base) {
            return new AssignResult(AssignStatus.NOT_DEPLOYED, base, null);
        }
    }

    public enum AssignStatus {
        ASSIGNED,
        ALREADY_ASSIGNED,
        NO_BASE_NEARBY,
        INVALID_PAL,
        NOT_DEPLOYED
    }

    public enum StoreResult {
        STORED,
        NO_BASE
    }

    public enum DeployResult {
        DEPLOYED,
        ALREADY_DEPLOYED,
        NO_BASE,
        INVALID_PAL,
        SPAWN_FAILED
    }

    private static final class BaseRecord {
        private final UUID baseUuid;
        private final UUID ownerUuid;
        private final Identifier dimensionId;
        private final BlockPos corePos;
        private final int radius;
        private final List<PalInstance> storedPals = new ArrayList<>();
        private final List<DeployedPal> deployments = new ArrayList<>();
        private final List<AssignedPal> assignments = new ArrayList<>();
        private final List<BaseTask> taskQueue = new ArrayList<>();
        private final EnumMap<BaseWorkType, Long> stock = new EnumMap<>(BaseWorkType.class);
        private int nextTaskScanTick;
        private int scanCursor;

        private BaseRecord(UUID baseUuid, UUID ownerUuid, Identifier dimensionId, BlockPos corePos, int radius) {
            this.baseUuid = baseUuid;
            this.ownerUuid = ownerUuid;
            this.dimensionId = dimensionId;
            this.corePos = corePos;
            this.radius = radius;
        }

        private BaseView toView() {
            return new BaseView(
                    baseUuid,
                    dimensionId,
                    corePos,
                    radius,
                    assignments.size(),
                    totalStock(),
                    taskQueue.size(),
                    stockSummary(),
                    assignmentSummary(),
                    taskProgressSummary(),
                    storedPalViews(),
                    assignedPalViews()
            );
        }

        private boolean hasAssignment(UUID palUuid) {
            return assignments.stream().anyMatch(assignment -> assignment.palUuid.equals(palUuid));
        }

        private boolean hasDeployment(UUID palUuid) {
            return deployments.stream().anyMatch(deployment -> deployment.palUuid.equals(palUuid));
        }

        private Optional<DeployedPal> findDeployment(UUID palUuid) {
            return deployments.stream()
                    .filter(deployment -> deployment.palUuid.equals(palUuid))
                    .findFirst();
        }

        private Optional<BaseTask> findTaskFor(BaseWorkType workType, UUID palUuid) {
            Optional<BaseTask> matchingTask = taskQueue.stream()
                    .filter(task -> task.workType == workType)
                    .filter(task -> task.workerPalUuid == null || task.workerPalUuid.equals(palUuid))
                    .findFirst();
            return matchingTask.or(() -> taskQueue.stream()
                    .filter(task -> task.workerPalUuid == null || task.workerPalUuid.equals(palUuid))
                    .findFirst());
        }

        private boolean hasQueuedTarget(BaseWorkType workType, BlockPos pos) {
            return taskQueue.stream().anyMatch(task -> task.workType == workType && task.targetPos.equals(pos));
        }

        private List<BaseWorkType> preferredWorkTypes() {
            List<BaseWorkType> workTypes = assignments.stream()
                    .map(assignment -> assignment.workType)
                    .filter(BaseWorkType::isAssignable)
                    .distinct()
                    .toList();
            return workTypes;
        }

        private boolean clearStaleTaskWorkers() {
            boolean changed = false;
            for (BaseTask task : taskQueue) {
                if (task.workerPalUuid == null) {
                    continue;
                }
                if (!hasDeployment(task.workerPalUuid) || !hasAssignment(task.workerPalUuid)) {
                    task.workerPalUuid = null;
                    changed = true;
                }
            }
            return changed;
        }

        private void addStock(BaseWorkType workType, long amount) {
            stock.merge(workType, amount, Long::sum);
        }

        private long totalStock() {
            return stock.values().stream().mapToLong(Long::longValue).sum();
        }

        private String stockSummary() {
            if (stock.isEmpty()) {
                return "none";
            }
            List<String> entries = new ArrayList<>();
            for (BaseWorkType workType : BaseWorkType.values()) {
                long amount = stock.getOrDefault(workType, 0L);
                if (amount > 0) {
                    entries.add(workType.id() + ":" + amount);
                }
            }
            return entries.isEmpty() ? "none" : String.join(", ", entries);
        }

        private String assignmentSummary() {
            if (assignments.isEmpty()) {
                return "none";
            }
            EnumMap<BaseWorkType, Integer> counts = new EnumMap<>(BaseWorkType.class);
            for (AssignedPal assignment : assignments) {
                counts.merge(assignment.workType, 1, Integer::sum);
            }
            List<String> entries = new ArrayList<>();
            for (BaseWorkType workType : BaseWorkType.values()) {
                int count = counts.getOrDefault(workType, 0);
                if (count > 0) {
                    entries.add(workType.id() + ":" + count);
                }
            }
            return String.join(", ", entries);
        }

        private String taskProgressSummary() {
            if (taskQueue.isEmpty()) {
                return "none";
            }
            List<String> entries = new ArrayList<>();
            for (BaseTask task : taskQueue) {
                entries.add(task.progressSummary());
                if (entries.size() >= 4) {
                    break;
                }
            }
            return String.join(", ", entries);
        }

        private List<BasePalView> storedPalViews() {
            List<BasePalView> views = new ArrayList<>();
            for (int i = 0; i < storedPals.size(); i++) {
                PalInstance pal = storedPals.get(i);
                Optional<AssignedPal> assignment = assignments.stream()
                        .filter(assignedPal -> assignedPal.palUuid.equals(pal.instanceUuid()))
                        .findFirst();
                views.add(new BasePalView(
                        i,
                        pal,
                        hasDeployment(pal.instanceUuid()),
                        assignment.isPresent(),
                        assignment.map(assignedPal -> assignedPal.workType).orElse(null),
                        workProgressSummary(pal.instanceUuid())
                ));
            }
            return List.copyOf(views);
        }

        private String workProgressSummary(UUID palUuid) {
            return taskQueue.stream()
                    .filter(task -> palUuid.equals(task.workerPalUuid))
                    .findFirst()
                    .map(BaseTask::progressSummary)
                    .orElse("none");
        }

        private List<AssignedPalView> assignedPalViews() {
            return assignments.stream()
                    .map(assignment -> new AssignedPalView(assignment.palUuid, assignment.workType))
                    .toList();
        }
    }

    private static final class AssignedPal {
        private final UUID palUuid;
        private final BaseWorkType workType;

        private AssignedPal(UUID palUuid, BaseWorkType workType) {
            this.palUuid = palUuid;
            this.workType = workType;
        }
    }

    private static final class DeployedPal {
        private final UUID palUuid;
        private UUID entityUuid;

        private DeployedPal(UUID palUuid) {
            this.palUuid = palUuid;
        }
    }

    private static final class BaseTask {
        private final UUID taskUuid;
        private final BaseWorkType workType;
        private int progress;
        private final int requiredWork;
        private final BlockPos targetPos;
        private UUID workerPalUuid;

        private BaseTask(UUID taskUuid, BaseWorkType workType, int progress, int requiredWork, BlockPos targetPos, UUID workerPalUuid) {
            this.taskUuid = taskUuid;
            this.workType = workType;
            this.progress = progress;
            this.requiredWork = requiredWork;
            this.targetPos = targetPos;
            this.workerPalUuid = workerPalUuid;
        }

        private String progressSummary() {
            return workType.id() + ":" + progress + "/" + requiredWork;
        }
    }

    private static Optional<PalInstance> findPal(List<PalInstance> pals, UUID palUuid) {
        return pals.stream()
                .filter(pal -> pal.instanceUuid().equals(palUuid))
                .findFirst();
    }

    private static void replacePalHealth(List<PalInstance> pals, UUID palUuid, float health) {
        for (int i = 0; i < pals.size(); i++) {
            PalInstance pal = pals.get(i);
            if (pal.instanceUuid().equals(palUuid)) {
                pals.set(i, pal.withHealth(health));
                return;
            }
        }
    }
}
