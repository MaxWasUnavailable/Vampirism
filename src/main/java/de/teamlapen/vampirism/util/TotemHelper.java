package de.teamlapen.vampirism.util;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import de.teamlapen.lib.lib.util.UtilLib;
import de.teamlapen.vampirism.api.entity.factions.IFaction;
import de.teamlapen.vampirism.blockentity.TotemBlockEntity;
import de.teamlapen.vampirism.config.VampirismConfig;
import de.teamlapen.vampirism.core.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TotemHelper {
    public static final int MIN_HOMES = 4;
    public static final int MIN_WORKSTATIONS = 2;
    public static final int MIN_VILLAGER = 4;

    private static final Logger LOGGER = LogManager.getLogger();


    /**
     * saves the position of a {@link PoiRecord} to the related village totem position
     */
    private static final Map<ResourceKey<Level>, Map<BlockPos, BlockPos>> totemPositions = Maps.newHashMap();

    /**
     * saves the {@link PoiRecord}s for every village totem
     */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Set<PoiRecord>>> poiSets = Maps.newHashMap();


    /**
     * add a totem
     *
     * @param world    world of the totem
     * @param pois     points that may belong to the totem
     * @param totemPos position of the totem
     * @return false if no {@link PoiRecord} belongs to the totem
     */
    public static boolean addTotem(@NotNull ServerLevel world, @NotNull Set<PoiRecord> pois, @NotNull BlockPos totemPos) {
        BlockPos conflict = null;
        Map<BlockPos, BlockPos> totemPositions = TotemHelper.totemPositions.computeIfAbsent(world.dimension(), key -> new HashMap<>());
        for (PoiRecord poi : pois) {
            if (totemPositions.containsKey(poi.getPos()) && !totemPositions.get(poi.getPos()).equals(totemPos)) {
                conflict = totemPositions.get(poi.getPos());
                break;
            }
        }
        if (conflict != null) {
            handleTotemConflict(pois, world, totemPos, conflict);
        }
        if (pois.isEmpty()) {
            return false;
        }
        for (PoiRecord pointOfInterest : pois) {
            totemPositions.put(pointOfInterest.getPos(), totemPos);
        }
        totemPositions.put(totemPos, totemPos);
        Map<BlockPos, Set<PoiRecord>> poiSets = TotemHelper.poiSets.computeIfAbsent(world.dimension(), key -> new HashMap<>());
        if (poiSets.containsKey(totemPos)) {
            poiSets.get(totemPos).forEach(poi -> {
                if (!pois.contains(poi)) {
                    totemPositions.remove(poi.getPos());
                }
            });
        }
        poiSets.put(totemPos, pois);
        return !pois.isEmpty();
    }

    /**
     * removes {@link PoiRecord} from the given set if another totem has more right to control them
     *
     * @param pois        {@link PoiRecord} collection which is disputed
     * @param world       world of the totem
     * @param totem       position of the totem
     * @param conflicting position of the conflicting totem
     */
    private static void handleTotemConflict(@NotNull Set<PoiRecord> pois, @NotNull ServerLevel world, @NotNull BlockPos totem, @NotNull BlockPos conflicting) {

        TotemBlockEntity totem1 = ((TotemBlockEntity) world.getBlockEntity(totem));
        TotemBlockEntity totem2 = ((TotemBlockEntity) world.getBlockEntity(conflicting));

        if (totem2 == null) {
            return;
        }

        boolean ignoreOtherTotem = totem1.getControllingFaction() == totem2.getControllingFaction();

        //both keep their pois

        if (totem1.getCapturingFaction() != null || totem2.getCapturingFaction() != null) { //both keep their pois
            ignoreOtherTotem = false;
        }

        Optional<StructureStart> structure1 = UtilLib.getStructureStartAt(world, totem, StructureTags.VILLAGE);
        Optional<StructureStart> structure2 = UtilLib.getStructureStartAt(world, conflicting, StructureTags.VILLAGE);

        if ((structure1.isPresent()) && (structure2.isPresent())) { //the first totem wins the POIs if located in natural village, other looses then
            ignoreOtherTotem = false;
        }

        if (totem2.getSize() >= totem1.getSize()) { //bigger village gets the pois, other looses them
            ignoreOtherTotem = false;
        }

        if (!ignoreOtherTotem) {
            pois.removeIf(poi -> !totem.equals(totemPositions.get(world.dimension()).get(poi.getPos())));
        }
    }

    /**
     * removes the poi references to the totem
     *
     * @param pois        the related {@link PoiRecord}s
     * @param pos         the position of the totem
     * @param removeTotem if the totem poi should be removed too
     */
    public static void removeTotem(ResourceKey<Level> dimension, @NotNull Collection<PoiRecord> pois, BlockPos pos, boolean removeTotem) {
        Map<BlockPos, BlockPos> totemPositions = TotemHelper.totemPositions.computeIfAbsent(dimension, key -> new HashMap<>());
        pois.forEach(pointOfInterest -> totemPositions.remove(pointOfInterest.getPos(), pos));
        if (removeTotem) {
            totemPositions.remove(pos);
        }
    }


    /**
     * gets a totem position of a {@link PoiRecord} if it exists
     *
     * @param pois collection of {@link PoiRecord} to search for a totem position
     * @return the registered totem position or {@code null} if no totem exists
     */
    @NotNull
    public static Optional<BlockPos> getTotemPosition(ResourceKey<Level> dimension, @NotNull Collection<PoiRecord> pois) {
        Map<BlockPos, BlockPos> totemPositions = TotemHelper.totemPositions.computeIfAbsent(dimension, key -> new HashMap<>());
        for (PoiRecord pointOfInterest : pois) {
            if (totemPositions.containsKey(pointOfInterest.getPos())) {
                return Optional.of(totemPositions.get(pointOfInterest.getPos()));
            }
        }
        return Optional.empty();
    }

    /**
     * gets the saved totem position for a related {@link PoiRecord}
     *
     * @param pos position of the {@link PoiRecord}
     * @return the blockpos of the totem or {@code null} if there is no registered totem position for the {@link PoiRecord}
     */
    @Nullable
    public static BlockPos getTotemPosition(ResourceKey<Level> world, BlockPos pos) {
        if (totemPositions.containsKey(world)) {
            return totemPositions.get(world).get(pos);
        }
        return null;
    }

    @NotNull
    public static Optional<BlockPos> getTotemPosNearPos(@NotNull ServerLevel world, @NotNull BlockPos pos) {
        Collection<PoiRecord> points = world.getPoiManager().getInRange(p -> true, pos, 25, PoiManager.Occupancy.ANY).collect(Collectors.toList());
        if (!points.isEmpty()) {
            return getTotemPosition(world.dimension(), points);
        }
        return Optional.empty();
    }

    @NotNull
    public static Optional<TotemBlockEntity> getTotemNearPos(@NotNull ServerLevel world, @NotNull BlockPos posSource, boolean mustBeLoaded) {
        Optional<BlockPos> posOpt = getTotemPosNearPos(world, posSource);
        if (mustBeLoaded) {
            posOpt = posOpt.filter(world::isPositionEntityTicking);
        }
        return posOpt.map(pos -> {
            BlockEntity tile = world.getBlockEntity(pos);
            if (tile instanceof TotemBlockEntity totem) {
                return (totem);
            } else {
                return null;
            }
        });
    }

    /**
     * forces a village totem to a specific faction
     *
     * @param faction the forced faction
     * @param player  the player that requests the faction
     * @return the feedback for the player
     */
    public static @NotNull Component forceFactionCommand(@Nullable IFaction<?> faction, @NotNull ServerPlayer player) {
        Map<BlockPos, BlockPos> totemPositions = TotemHelper.totemPositions.computeIfAbsent(player.getCommandSenderWorld().dimension(), key -> new HashMap<>());
        List<PoiRecord> pointOfInterests = ((ServerLevel) player.getCommandSenderWorld()).getPoiManager().getInRange(point -> true, player.blockPosition(), 25, PoiManager.Occupancy.ANY).sorted(Comparator.comparingInt(point -> (int) (point.getPos()).distSqr(player.blockPosition()))).toList();
        if (pointOfInterests.stream().noneMatch(point -> totemPositions.containsKey(point.getPos()))) {
            return Component.translatable("command.vampirism.test.village.no_village");
        }
        BlockEntity te = player.getCommandSenderWorld().getBlockEntity(totemPositions.get(pointOfInterests.get(0).getPos()));
        if (!(te instanceof TotemBlockEntity tile)) {
            LOGGER.warn("TileEntity at {} is no TotemTileEntity", totemPositions.get(pointOfInterests.get(0).getPos()));
            return Component.literal("");
        }
        tile.setForcedFaction(faction);
        return Component.translatable("command.vampirism.test.village.success", faction == null ? "none" : faction.getName());
    }

    /**
     * gets all {@link PoiRecord} points for a village totem to consider them as part of the village
     *
     * @param world world in which to search
     * @param pos   position of the village totem to start searching
     * @return a set of all related {@link PoiRecord} points
     */
    public static @NotNull Set<PoiRecord> getVillagePointsOfInterest(@NotNull ServerLevel world, @NotNull BlockPos pos) {
        PoiManager manager = world.getPoiManager();
        Set<PoiRecord> finished = Sets.newHashSet();
        Set<PoiRecord> points = manager.getInRange(type -> !type.is(ModTags.PoiTypes.HAS_FACTION), pos, 50, PoiManager.Occupancy.ANY).collect(Collectors.toSet());
        while (!points.isEmpty()) {
            List<Stream<PoiRecord>> list = points.stream().map(pointOfInterest -> manager.getInRange(type -> !type.is(ModTags.PoiTypes.HAS_FACTION), pointOfInterest.getPos(), 40, PoiManager.Occupancy.ANY)).toList();
            points.clear();
            list.forEach(stream -> stream.forEach(point -> {
                if (!finished.contains(point)) {
                    if (point.getPos().closerThan(pos, VampirismConfig.BALANCE.viMaxTotemRadius.get())) {
                        points.add(point);
                    }
                }
                finished.add(point);
            }));
        }
        return finished;
    }

    /**
     * use {@link #isVillage(Set, ServerLevel, BlockPos, boolean)}
     * <p>
     * <p>
     * {@code flag & 1 != 0} :
     * <p>
     * - enough homes
     * <p>
     * {@code flag & 2 != 0} :
     * <p>
     * - enough work stations
     * <p>
     * {@code flag & 4 != 0} :
     * <p>
     * - enough villager
     * <p>
     *
     * @param stats          the output of {@link #getVillageStats(Set, Level)}
     * @param hasInteraction if the village is influenced by a faction
     * @return flag which requirements are met
     */
    public static int isVillage(@NotNull Map<Integer, Integer> stats, boolean hasInteraction) {
        int status = 0;
        if (stats.get(1) >= MIN_HOMES) {
            status += 1;
        }
        if (stats.get(2) >= MIN_WORKSTATIONS) {
            status += 2;
        }
        if (hasInteraction || stats.get(4) >= MIN_VILLAGER) {
            status += 4;
        }
        return status;
    }

    /**
     * checks if the given  {@link PoiRecord} Set can be interpreted as village
     * <p>
     * <p>
     * {@code flag & 1 != 0} :
     * <p>
     * - enough homes
     * <p>
     * {@code flag & 2 != 0} :
     * <p>
     * - enough work stations
     * <p>
     * {@code flag & 4 != 0} :
     * <p>
     * - enough villager
     * <p>
     *
     * @param pointOfInterests the output of {@link #getVillageStats(Set, Level)}
     * @param world            the world of the point of interests
     * @param hasInteraction   if the village is influenced by a faction
     * @return flag which requirements are met
     */
    public static int isVillage(@NotNull Set<PoiRecord> pointOfInterests, @NotNull ServerLevel world, @NotNull BlockPos totemPos, boolean hasInteraction) {
        if (UtilLib.getStructureStartAt(world, totemPos, StructureTags.VILLAGE).isPresent()) {
            return 7;
        }
        return isVillage(getVillageStats(pointOfInterests, world), hasInteraction);
    }

    /**
     * searches the given {@link PoiRecord} set for village qualifying data
     *
     * @param pointOfInterests a {@link PoiRecord} set to check for a village
     * @param world            world of the point of interests
     * @return map containing village related data
     */
    public static @NotNull Map<Integer, Integer> getVillageStats(@NotNull Set<PoiRecord> pointOfInterests, @NotNull Level world) {
        Map<ResourceKey<PoiType>, Long> poiTCounts = pointOfInterests.stream().map(PoiRecord::getPoiType).flatMap(a -> BuiltInRegistries.POINT_OF_INTEREST_TYPE.getResourceKey(a.value()).stream()).collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        AABB area = getAABBAroundPOIs(pointOfInterests);
        return new HashMap<>() {{
            put(1, poiTCounts.getOrDefault(net.minecraft.world.entity.ai.village.poi.PoiTypes.HOME, 0L).intValue());
            put(2, ((int) poiTCounts.entrySet().stream().filter(entry -> entry.getKey() != net.minecraft.world.entity.ai.village.poi.PoiTypes.HOME).mapToLong(Entry::getValue).sum()));
            put(4, area == null ? 0 : world.getEntitiesOfClass(Villager.class, area).size());
        }};
    }


    /**
     * creates a bounding box for the given {@link PoiRecord}s
     *
     * @throws NoSuchElementException if poi is empty
     */
    @Nullable
    public static AABB getAABBAroundPOIs(@NotNull Set<PoiRecord> pois) {
        return pois.stream().map(poi -> new AABB(poi.getPos()).inflate(25)).reduce(AABB::minmax).orElse(null);
    }

    public static void ringBell(@NotNull Level world, @NotNull Player player) {
        if (!world.isClientSide) {
            Optional<TotemBlockEntity> tile = getTotemNearPos(((ServerLevel) world), player.blockPosition(), false);
            tile.ifPresent(s -> s.ringBell(player));
        }
    }
}
