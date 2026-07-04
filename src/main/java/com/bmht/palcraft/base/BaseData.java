package com.bmht.palcraft.base;

import com.bmht.palcraft.PalCraft;
import com.bmht.palcraft.partner.PalInstance;
import com.bmht.palcraft.partner.PlayerPalData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

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
        BaseWorkType workType = requestedWorkType == null ? BaseWorkType.bestFor(palInstance) : requestedWorkType;
        base.assignments.add(new AssignedPal(palInstance.instanceUuid(), workType));
        markDirty();
        return AssignResult.assigned(base.toView(), workType);
    }

    public void tickWork(MinecraftServer server) {
        PlayerPalData palData = PlayerPalData.get(server);
        boolean changed = false;
        for (BaseRecord base : bases) {
            if (base.assignments.isEmpty()) {
                continue;
            }

            if (server.getTicks() >= base.nextTaskScanTick && base.taskQueue.size() < MAX_TASK_QUEUE_SIZE) {
                changed |= enqueueDiscoveredTasks(base, server.getTicks());
            }

            List<PalInstance> ownerPals = palData.getStoredPals(base.ownerUuid);
            for (AssignedPal assignment : base.assignments) {
                Optional<PalInstance> pal = findPal(ownerPals, assignment.palUuid);
                if (pal.isEmpty()) {
                    continue;
                }
                Optional<BaseTask> task = base.findTaskFor(assignment.workType);
                if (task.isEmpty()) {
                    continue;
                }

                BaseTask baseTask = task.get();
                baseTask.progress += Math.max(1, assignment.workType.suitability(pal.get()));
                if (baseTask.progress >= baseTask.requiredWork) {
                    base.taskQueue.remove(baseTask);
                    base.addStock(baseTask.workType, 1L);
                }
                changed = true;
            }
        }
        if (changed) {
            markDirty();
        }
    }

    private boolean enqueueDiscoveredTasks(BaseRecord base, int serverTicks) {
        int queued = 0;
        for (BaseWorkType workType : base.preferredWorkTypes()) {
            if (queued >= MAX_TASKS_PER_SCAN || base.taskQueue.size() >= MAX_TASK_QUEUE_SIZE) {
                break;
            }
            base.taskQueue.add(new BaseTask(UUID.randomUUID(), workType, 0, workType.requiredWork()));
            queued++;
        }
        base.nextTaskScanTick = serverTicks + TASK_SCAN_INTERVAL_TICKS;
        return queued > 0;
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

            NbtList assignedPals = new NbtList();
            for (AssignedPal assignment : base.assignments) {
                NbtCompound palNbt = new NbtCompound();
                palNbt.putUuid("PalUuid", assignment.palUuid);
                palNbt.putString("WorkType", assignment.workType.id());
                assignedPals.add(palNbt);
            }
            baseNbt.put("AssignedPals", assignedPals);

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

            NbtList assignedPals = baseNbt.getList("AssignedPals", NbtElement.COMPOUND_TYPE);
            for (NbtElement palElement : assignedPals) {
                NbtCompound palNbt = (NbtCompound) palElement;
                BaseWorkType workType = palNbt.contains("WorkType")
                        ? BaseWorkType.fromId(palNbt.getString("WorkType"))
                        : BaseWorkType.HAULING;
                record.assignments.add(new AssignedPal(palNbt.getUuid("PalUuid"), workType));
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
                record.taskQueue.add(new BaseTask(
                        taskNbt.getUuid("TaskUuid"),
                        BaseWorkType.fromId(taskNbt.getString("WorkType")),
                        taskNbt.getInt("Progress"),
                        taskNbt.getInt("RequiredWork")
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

    private Optional<BaseRecord> findBaseAt(Identifier dimensionId, BlockPos corePos) {
        return bases.stream()
                .filter(base -> base.dimensionId.equals(dimensionId) && base.corePos.equals(corePos))
                .findFirst();
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
            String assignmentSummary
    ) {
        public String corePositionText() {
            return formatPos(corePos);
        }
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
    }

    public enum AssignStatus {
        ASSIGNED,
        ALREADY_ASSIGNED,
        NO_BASE_NEARBY
    }

    private static final class BaseRecord {
        private final UUID baseUuid;
        private final UUID ownerUuid;
        private final Identifier dimensionId;
        private final BlockPos corePos;
        private final int radius;
        private final List<AssignedPal> assignments = new ArrayList<>();
        private final List<BaseTask> taskQueue = new ArrayList<>();
        private final EnumMap<BaseWorkType, Long> stock = new EnumMap<>(BaseWorkType.class);
        private int nextTaskScanTick;

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
                    assignmentSummary()
            );
        }

        private boolean hasAssignment(UUID palUuid) {
            return assignments.stream().anyMatch(assignment -> assignment.palUuid.equals(palUuid));
        }

        private Optional<BaseTask> findTaskFor(BaseWorkType workType) {
            Optional<BaseTask> matchingTask = taskQueue.stream()
                    .filter(task -> task.workType == workType)
                    .findFirst();
            return matchingTask.or(() -> taskQueue.stream().findFirst());
        }

        private List<BaseWorkType> preferredWorkTypes() {
            List<BaseWorkType> workTypes = assignments.stream()
                    .map(assignment -> assignment.workType)
                    .distinct()
                    .toList();
            return workTypes.isEmpty() ? List.of(BaseWorkType.HAULING) : workTypes;
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
    }

    private record AssignedPal(UUID palUuid, BaseWorkType workType) {
    }

    private static final class BaseTask {
        private final UUID taskUuid;
        private final BaseWorkType workType;
        private int progress;
        private final int requiredWork;

        private BaseTask(UUID taskUuid, BaseWorkType workType, int progress, int requiredWork) {
            this.taskUuid = taskUuid;
            this.workType = workType;
            this.progress = progress;
            this.requiredWork = requiredWork;
        }
    }

    private static Optional<PalInstance> findPal(List<PalInstance> pals, UUID palUuid) {
        return pals.stream()
                .filter(pal -> pal.instanceUuid().equals(palUuid))
                .findFirst();
    }
}
