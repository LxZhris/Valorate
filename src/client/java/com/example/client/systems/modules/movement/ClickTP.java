package com.example.client.systems.modules.movement;

import com.example.client.systems.modules.Category;
import com.example.client.systems.modules.Module;
import com.example.client.systems.settings.BooleanSetting;
import com.example.client.systems.settings.ModeSetting;
import com.example.client.systems.settings.NumberSetting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Blocks;

import java.util.*;

public class ClickTP extends Module {
    // Mode erweitert: "Coords" ermöglicht Teleport zu Slider-Koordinaten
    private final ModeSetting mode = new ModeSetting("Mode", "Packet", "Packet", "SetPos", "Coords");

    private final NumberSetting range = new NumberSetting("Range", 200.0, 10.0, 210.0, 5.0);
    private final NumberSetting overlayOpacity = new NumberSetting("Overlay Opacity", 120.0, 0.0, 255.0, 5.0);
    private final BooleanSetting sneakOnly = new BooleanSetting("Sneak Only", false);
    private final BooleanSetting ignoreInteract = new BooleanSetting("Ignore Interact", true);

    private BlockHitResult currentHit;

    // --- Neue Settings für Coords-Mode ---
    private final NumberSetting targetX = new NumberSetting("Target X", 0.0, -30000000.0, 30000000.0, 1.0);
    private final NumberSetting targetZ = new NumberSetting("Target Z", 0.0, -30000000.0, 30000000.0, 1.0);
    private final BooleanSetting coordsUsePathfinding = new BooleanSetting("Pathfind To Coords", true);
    private final NumberSetting maxPathPackets = new NumberSetting("Max Path Packets", 12, 1, 40, 1);
    // Anzahl an Pfadknoten/Blöcken die pro Ausführung teleportiert werden sollen
    private final NumberSetting stepsPerTeleport = new NumberSetting("Steps Per Teleport", 5, 1, 20, 1);

    // --- Avoidance settings (copied from TargetStrafe) ---
    private final BooleanSetting avoidHazards = new BooleanSetting("Avoid Hazards", true);
    private final BooleanSetting antiVoid = new BooleanSetting("Anti Void", true);
    private final NumberSetting voidThreshold = new NumberSetting("Void Threshold", 0.0, -64.0, 64.0, 1.0);

    public ClickTP() {
        super("ClickTP", Category.MOVEMENT, false, "Teleports you on top of the block you right click.");

        addSetting(mode);
        addSetting(range);
        addSetting(overlayOpacity);
        addSetting(sneakOnly);
        addSetting(ignoreInteract);

        // Coords settings visible always (UI arrangement depends on framework)
        addSetting(targetX);
        addSetting(targetZ);
        addSetting(coordsUsePathfinding);
        addSetting(maxPathPackets);
        addSetting(stepsPerTeleport);
        // register avoidance settings used by the pathfinder
        addSetting(avoidHazards);
        addSetting(antiVoid);
        voidThreshold.visibleWhen(antiVoid::get);
        addSetting(voidThreshold);
    }

    @Override
    public void onTick() {
        if (!isEnabled()) {
            currentHit = null;
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) {
            currentHit = null;
            return;
        }

        currentHit = raycast(mc);

        if (!mc.options.keyUse.isDown()) return;
        if (sneakOnly.get() && !mc.player.isShiftKeyDown()) return;

        if (mc.player.getInventory().getSelectedItem().getUseAnimation() != ItemUseAnimation.NONE) return;

        if (ignoreInteract.get() && mc.hitResult != null) {
            if (mc.hitResult.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHit = (EntityHitResult) mc.hitResult;

                if (mc.player.interactOn(entityHit.getEntity(), InteractionHand.MAIN_HAND, mc.hitResult.getLocation()) != InteractionResult.PASS) {
                    return;
                }
            }

            if (mc.hitResult.getType() == HitResult.Type.BLOCK
                    && mc.player.getMainHandItem().getItem() instanceof BlockItem) {
                return;
            }
        }

        // Wenn Mode == Coords: teleportiere zu Slider-Koordinaten (mit optionaler Pfadfindung)
        if (mode.get().equalsIgnoreCase("Coords")) {
            teleportToCoords(mc);
            return;
        }

        if (currentHit == null || currentHit.getType() != HitResult.Type.BLOCK) return;

        Vec3 newPos = getTeleportPosition(mc, currentHit);
        if (newPos == null) return;

        if (mode.get().equalsIgnoreCase("Packet")) {
            teleportPacket(mc, newPos);
        } else {
            mc.player.setPos(newPos);
        }
    }

    private BlockHitResult raycast(Minecraft mc) {
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();

        Vec3 direction = Vec3.directionFromRotation(camera.xRot(), camera.yRot()).scale(range.get());
        Vec3 targetPos = cameraPos.add(direction);

        ClipContext context = new ClipContext(
                cameraPos,
                targetPos,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                mc.player
        );

        BlockHitResult hit = mc.level.clip(context);

        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        return hit;
    }

    private Vec3 getTeleportPosition(Minecraft mc, BlockHitResult hit) {
        BlockState state = mc.level.getBlockState(hit.getBlockPos());

        if (ignoreInteract.get()) {
            if (state.useWithoutItem(mc.level, mc.player, hit) != InteractionResult.PASS) {
                return null;
            }
        }

        VoxelShape shape = state.getCollisionShape(mc.level, hit.getBlockPos());

        if (shape.isEmpty()) {
            shape = state.getShape(mc.level, hit.getBlockPos());
        }

        double height = shape.isEmpty() ? 1.0 : shape.max(Direction.Axis.Y);

        return new Vec3(
                hit.getBlockPos().getX() + 0.5,
                hit.getBlockPos().getY() + height,
                hit.getBlockPos().getZ() + 0.5
        );
    }

    // --- Neue Methode: Teleport to configured coordinates (uses optional A* pathfinding) ---
    private void teleportToCoords(Minecraft mc) {
        double tx = targetX.get();
        double tz = targetZ.get();

        // find a reasonable goal block near (tx,tz)
        BlockPos approx = BlockPos.containing(tx, mc.player.getY(), tz);
        BlockPos goal = findClosestSafe(mc, approx, 4);
        if (goal == null) {
            // fallback: try ground at player's Y
            goal = new BlockPos((int)Math.floor(tx), (int)Math.floor(mc.player.getY()), (int)Math.floor(tz));
        }

        // teleport slightly above the block's actual collision top so we land cleanly
        VoxelShape goalShape = mc.level.getBlockState(goal).getCollisionShape(mc.level, goal);
        double goalTop = goalShape.isEmpty() ? 1.0 : goalShape.max(Direction.Axis.Y);
        Vec3 goalVec = new Vec3(goal.getX() + 0.5, goal.getY() + goalTop + 0.1, goal.getZ() + 0.5);

        // allowed steps this execution (respect maxPathPackets as upper bound)
        int allowedSteps = Math.max(1, stepsPerTeleport.get().intValue());
        allowedSteps = Math.min(allowedSteps, Math.max(1, maxPathPackets.get().intValue()));

        // safety: maximum allowed climb per teleport (prevents big upward jumps)
        final double MAX_CLIMB = 1.5;

        if (!coordsUsePathfinding.get()) {
            // No pathfinding: teleport a fixed number of blocks towards the goal
            Vec3 playerPos = mc.player.position();
            double dx = tx - playerPos.x;
            double dz = tz - playerPos.z;
            double len = Math.hypot(dx, dz);
            if (len < 0.001) {
                // already there (or too close) -> teleport to exact goal
                teleportPacket(mc, goalVec);
                return;
            }
            double nx = dx / len;
            double nz = dz / len;
            double stepCount = allowedSteps; // blocks to move this activation
            Vec3 raw = new Vec3(playerPos.x + nx * stepCount, playerPos.y, playerPos.z + nz * stepCount);
            // place on top of the block under the raw target
            BlockPos floor = BlockPos.containing(raw.x, mc.player.getY(), raw.z);
            VoxelShape floorShape = mc.level.getBlockState(floor).getCollisionShape(mc.level, floor);
            double floorTop = floorShape.isEmpty() ? 0.0 : floorShape.max(Direction.Axis.Y);
            Vec3 stepTarget = new Vec3(raw.x, floor.getY() + floorTop + 0.1, raw.z);

            // clamp vertical jump: if target too high, try to find a lower safe spot nearby
            double climb = stepTarget.y - mc.player.getY();
            if (climb > MAX_CLIMB) {
                BlockPos alt = findClosestSafe(mc, floor, 2);
                if (alt != null) {
                    VoxelShape altShape = mc.level.getBlockState(alt).getCollisionShape(mc.level, alt);
                    double altTop = altShape.isEmpty() ? 1.0 : altShape.max(Direction.Axis.Y);
                    stepTarget = new Vec3(alt.getX() + 0.5, alt.getY() + altTop + 0.1, alt.getZ() + 0.5);
                } else {
                    // avoid unsafe huge climb: abort this activation
                    return;
                }
            }

            teleportPacket(mc, stepTarget);
            return;
        }

        // compute path (A*) to goal
        calculatePath(mc, goal);
        if (path.isEmpty()) {
            // if path empty, fallback to stepping straight-line (not full instant)
            Vec3 playerPos = mc.player.position();
            double dx = tx - playerPos.x;
            double dz = tz - playerPos.z;
            double len = Math.hypot(dx, dz);
            if (len < 0.001) {
                teleportPacket(mc, goalVec);
                return;
            }
            double nx = dx / len;
            double nz = dz / len;
            double stepCount = Math.max(1, Math.min(allowedSteps, (int)Math.ceil(len)));
            Vec3 raw = new Vec3(playerPos.x + nx * stepCount, playerPos.y, playerPos.z + nz * stepCount);
            BlockPos floor = BlockPos.containing(raw.x, mc.player.getY(), raw.z);
            VoxelShape floorShape = mc.level.getBlockState(floor).getCollisionShape(mc.level, floor);
            double floorTop = floorShape.isEmpty() ? 0.0 : floorShape.max(Direction.Axis.Y);
            Vec3 stepTarget = new Vec3(raw.x, floor.getY() + floorTop + 0.1, raw.z);
            teleportPacket(mc, stepTarget);
            return;
        }

        // Send only a limited number of path nodes this activation (steps), then remove them from path
        int steps = Math.min(allowedSteps, path.size());
        int consumed = 0;
        for (int i = 0; i < steps; i++) {
            BlockPos p = path.get(i);
            // teleport to top of the path node block so we step up correctly (use the block's collision top)
            VoxelShape nodeShape = mc.level.getBlockState(p).getCollisionShape(mc.level, p);
            double nodeTop = nodeShape.isEmpty() ? 1.0 : nodeShape.max(Direction.Axis.Y);
            Vec3 pos = new Vec3(p.getX() + 0.5, p.getY() + nodeTop + 0.1, p.getZ() + 0.5);

            // fail if this node implies an excessive climb
            double climb = pos.y - mc.player.getY();
            if (climb > MAX_CLIMB) {
                // try to find a lower nearby safe spot for this node
                BlockPos alt = findClosestSafe(mc, p, 2);
                if (alt != null) {
                    VoxelShape altShape = mc.level.getBlockState(alt).getCollisionShape(mc.level, alt);
                    double altTop = altShape.isEmpty() ? 1.0 : altShape.max(Direction.Axis.Y);
                    Vec3 altPos = new Vec3(alt.getX() + 0.5, alt.getY() + altTop + 0.1, alt.getZ() + 0.5);
                    if (altPos.y - mc.player.getY() <= MAX_CLIMB) {
                        teleportPacket(mc, altPos);
                        consumed++;
                        continue;
                    }
                }
                // cannot safely climb this node -> try recalc with larger radius once
                BlockPos recalc = findClosestSafe(mc, lastPathGoal != null ? lastPathGoal : goal, 5);
                if (recalc != null) {
                    calculatePath(mc, recalc);
                    lastPathGoal = recalc;
                    lastPathCalcTime = System.currentTimeMillis();
                    return;
                }
                // else abort sending further nodes this activation
                break;
            }

            // also avoid hazards
            if (isHazard(mc, p)) {
                // abort and recalc
                BlockPos recalc2 = findClosestSafe(mc, lastPathGoal != null ? lastPathGoal : goal, 4);
                if (recalc2 != null) {
                    calculatePath(mc, recalc2);
                    lastPathGoal = recalc2;
                    lastPathCalcTime = System.currentTimeMillis();
                    return;
                }
                break;
            }

            teleportPacket(mc, pos);
            consumed++;
        }
        // remove consumed nodes from path
        if (consumed > 0) {
            path.subList(0, consumed).clear();
        } else {
            // if we couldn't consume any node, attempt a small fallback: straight step 1 block
            Vec3 playerPos = mc.player.position();
            double dx = goal.getX() + 0.5 - playerPos.x;
            double dz = goal.getZ() + 0.5 - playerPos.z;
            double dist = Math.hypot(dx, dz);
            if (dist > 0.5) {
                double nx = dx / dist, nz = dz / dist;
                Vec3 raw = new Vec3(playerPos.x + nx * 1.0, playerPos.y, playerPos.z + nz * 1.0);
                BlockPos floor = BlockPos.containing(raw.x, mc.player.getY(), raw.z);
                VoxelShape floorShape = mc.level.getBlockState(floor).getCollisionShape(mc.level, floor);
                double floorTop = floorShape.isEmpty() ? 0.0 : floorShape.max(Direction.Axis.Y);
                Vec3 stepTarget = new Vec3(raw.x, floor.getY() + floorTop + 0.1, raw.z);
                if (stepTarget.y - mc.player.getY() <= MAX_CLIMB) teleportPacket(mc, stepTarget);
            }
        }
    }

    // --- A* Pathfinding & helpers (localized copy of TargetStrafe logic) ---
    private final List<BlockPos> path = new ArrayList<>();

    // last path goal and timestamp for recovery/recalc logic
    private BlockPos lastPathGoal = null;
    private long lastPathCalcTime = 0L;

    private void calculatePath(Minecraft mc, BlockPos goal) {
        path.clear();
        if (mc.player == null || mc.level == null) return;
        BlockPos start = mc.player.blockPosition();
        if (start.equals(goal)) return;

        if (!isSafePath(mc, goal)) {
            BlockPos alt = findClosestSafe(mc, goal, 3);
            if (alt != null) goal = alt;
        }

        PriorityQueue<CalcNode> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
        Map<BlockPos, Double> g = new HashMap<>();

        open.add(new CalcNode(start, 0, heuristic(start, goal)));
        g.put(start, 0.0);

        int iterations = 0;
        BlockPos best = start;
        double bestDist = start.distSqr(goal);

        while (!open.isEmpty() && iterations < 1000) {
            CalcNode cur = open.poll();
            iterations++;
            if (cur.pos.equals(goal)) {
                reconstructPath(cameFrom, goal);
                return;
            }
            for (BlockPos npos : getPathNeighbors(mc, cur.pos)) {
                double tentative = g.getOrDefault(cur.pos, Double.MAX_VALUE) + 1.0;
                if (tentative < g.getOrDefault(npos, Double.MAX_VALUE)) {
                    cameFrom.put(npos, cur.pos);
                    g.put(npos, tentative);
                    double f = tentative + Math.sqrt(npos.distSqr(goal));
                    open.add(new CalcNode(npos, tentative, f));
                    double d = npos.distSqr(goal);
                    if (d < bestDist) { bestDist = d; best = npos; }
                }
            }
        }
        reconstructPath(cameFrom, best);
    }

    private double heuristic(BlockPos a, BlockPos b) { return Math.sqrt(a.distSqr(b)); }
    private static class CalcNode { BlockPos pos; double g,f; CalcNode(BlockPos p,double g,double f){this.pos=p;this.g=g;this.f=f;} }

    private List<BlockPos> getPathNeighbors(Minecraft mc, BlockPos pos) {
        List<BlockPos> out = new ArrayList<>();
        BlockPos[] dirs = new BlockPos[]{ pos.north(), pos.south(), pos.east(), pos.west() };
        for (BlockPos p : dirs) {
            if (isSafePath(mc, p)) out.add(p);
            else if (isSafePath(mc, p.above()) && isSolidBlock(mc, p)) {
                if (isBodySpaceFree(mc, pos.above(2))) out.add(p.above());
            } else if (isSafePath(mc, p.below()) && isBodySpaceFree(mc, p)) out.add(p.below());
        }
        return out;
    }

    private boolean isSafePath(Minecraft mc, BlockPos pos) {
        // require the player's body/head space to be passable at pos and pos.above()
        if (!isBodySpaceFree(mc, pos) || !isHeadSpaceFree(mc, pos.above())) return false;
        BlockPos below = pos.below();
        if (avoidHazards.get() && (isHazard(mc, below) || isHazard(mc, pos))) return false;
        if (isAir(mc, below)) {
            if (antiVoid.get() && isOverVoidAlt(mc, new Vec3(pos.getX()+0.5, pos.getY(), pos.getZ()+0.5))) return false;
            return isSolidBlock(mc, below.below()) || isSolidBlock(mc, below.below(2)) || isSolidBlock(mc, below.below(3));
        }
        return isSolidBlock(mc, below);
    }

    private void reconstructPath(Map<BlockPos, BlockPos> cameFrom, BlockPos goal) {
        path.clear();
        BlockPos cur = goal;
        while (cur != null) {
            path.add(0, cur);
            cur = cameFrom.get(cur);
        }
    }

    private BlockPos findClosestSafe(Minecraft mc, BlockPos center, int radius) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) for (int dy = -1; dy <= 2; dy++) {
            BlockPos p = center.offset(dx, dy, dz);
            if (isSafePath(mc, p)) {
                double ddx = p.getX() - center.getX();
                double ddy = p.getY() - center.getY();
                double ddz = p.getZ() - center.getZ();
                double d2 = ddx*ddx + ddy*ddy + ddz*ddz;
                if (d2 < bestDist) { bestDist = d2; best = p; }
            }
        }
        return best;
    }

    private boolean isPathClearAlt(Vec3 start, Vec3 end) {
        // reuse simple sampling from TargetStrafe for LOS
        Vec3 diff = end.subtract(start);
        double len = diff.length();
        if (len <= 0) return true;
        int steps = Math.max(4, (int)Math.ceil(len*2.0));
        Minecraft mc = Minecraft.getInstance();
        for (int i=0;i<=steps;i++){
            double t = (double)i/steps;
            Vec3 p = new Vec3(start.x + diff.x*t, start.y + diff.y*t, start.z + diff.z*t);
            BlockPos floor = BlockPos.containing(p.x, Math.floor(p.y)-1, p.z);
            BlockPos head = BlockPos.containing(p.x, Math.floor(p.y)+1, p.z);
            if (!isHeadSpaceFree(mc, head)) return false;
            if (mc.level.getBlockState(floor).isAir()) {
                if (antiVoid.get() && isOverVoidAlt(mc,p)) return false;
                if (isHazard(mc,floor)) return false;
            } else if (isHazard(mc,floor)) return false;
        }
        return true;
    }

    private boolean isOverVoidAlt(Minecraft mc, Vec3 pos) {
        int startY = (int)Math.floor(pos.y);
        int thresholdY = (int)Math.floor(voidThreshold.get());
        int minY = Math.max(thresholdY - 5, mc.level.getMinY());
        for (int y = startY; y >= minY; y--) {
            BlockPos bp = new BlockPos((int)Math.floor(pos.x), y, (int)Math.floor(pos.z));
            if (!mc.level.getBlockState(bp).isAir()) return false;
        }
        return true;
    }

    // Helpers: treat thin collision shapes (snow layers, slabs, tall grass etc.) as passable for body/head checks
    private boolean isBodySpaceFree(Minecraft mc, BlockPos pos) {
        if (mc == null || mc.level == null) return false;
        VoxelShape s = mc.level.getBlockState(pos).getCollisionShape(mc.level, pos);
        if (s.isEmpty()) return true;
        // allow shallow shapes (snow layer, non-solid decorations) at foot position
        return s.max(Direction.Axis.Y) < 0.9;
    }

    private boolean isHeadSpaceFree(Minecraft mc, BlockPos pos) {
        if (mc == null || mc.level == null) return false;
        VoxelShape s = mc.level.getBlockState(pos).getCollisionShape(mc.level, pos);
        if (s.isEmpty()) return true;
        // stricter for headspace so we don't stand inside tall blocks
        return s.max(Direction.Axis.Y) < 0.6;
    }

    private boolean isSolidBlock(Minecraft mc, BlockPos pos) {
        return !mc.level.getBlockState(pos).getCollisionShape(mc.level, pos).isEmpty();
    }
    private boolean isAir(Minecraft mc, BlockPos pos) { return mc.level.getBlockState(pos).isAir(); }
    private boolean isHazard(Minecraft mc, BlockPos pos) {
        var s = mc.level.getBlockState(pos);
        return s.is(Blocks.LAVA) || s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE) || s.is(Blocks.CAMPFIRE) || s.is(Blocks.SOUL_CAMPFIRE) || s.is(Blocks.MAGMA_BLOCK);
    }

    // small wrapper to match existing teleportPacket usage
    private void teleportPacket(Minecraft mc, Vec3 newPos) {
        int packetsRequired = (int) Math.ceil(mc.player.position().distanceTo(newPos) / 10.0) - 1;
        if (packetsRequired > 19) packetsRequired = 0;
        for (int i = 0; i < packetsRequired; i++) mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, true));
        mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(newPos.x, newPos.y, newPos.z, true, true));
        mc.player.setPos(newPos);
    }

    public boolean hasTarget() {
        return isEnabled() && currentHit != null;
    }

    public BlockHitResult getCurrentHit() {
        return currentHit;
    }

    public int getOverlayOpacity() {
        return overlayOpacity.get().intValue();
    }
}