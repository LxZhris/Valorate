package com.example.client.systems.modules.movement;

import com.example.client.systems.modules.Category;
import com.example.client.systems.modules.Module;
import com.example.client.systems.settings.BooleanSetting;
import com.example.client.systems.settings.ModeSetting;
import com.example.client.systems.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/*
  Ziel: Übernehme die Logik deines Beispiels (TargetStrafe),
  aber ohne RayTrace (verwende sampling-basiertes LOS/Path-Check).
  Struktur bleibt im Paket/Dateipfad erhalten.
*/

public class TargetStrafe extends Module {
    public enum MoveMode { Basic, Scroll, Behind, Random, Avoid }
    public enum ExecuteMode { Tick, Move }
    public enum SortMode { Closest, Furthest, Crosshair }

    // --- Settings (einfach gehalten, passe Namen/APIs falls nötig) ---
    private final NumberSetting speed = new NumberSetting("Speed", 0.24, 0.1, 1.0, 0.01);
    private final BooleanSetting autoJump = new BooleanSetting("Auto Jump", true);
    private final BooleanSetting damageBoost = new BooleanSetting("Damage Boost", false);
    private final NumberSetting boost = new NumberSetting("Boost", 0.09, 0.01, 0.5, 0.01);

    private final ModeSetting mode = new ModeSetting("Move Mode", "Basic", "Basic", "Scroll", "Behind", "Random", "Avoid");
    private final ModeSetting executeMode = new ModeSetting("Execute Mode", "Tick", "Tick", "Move");
    private final ModeSetting sortMode = new ModeSetting("Sort Mode", "Closest", "Closest", "Furthest", "Crosshair");

    private final NumberSetting targetRange = new NumberSetting("Target Range", 7.0, 1.0, 30.0, 0.5);
    private final NumberSetting radius = new NumberSetting("Radius", 1.9, 0.1, 30.0, 0.1);
    private final NumberSetting avoidRadius = new NumberSetting("Avoid Radius", 3.8, 1.0, 20.0, 0.1);

    // Pathfinding / avoidance settings
    private final BooleanSetting usePathfinding = new BooleanSetting("Use Pathfinding", true);
    private final BooleanSetting followThroughWalls = new BooleanSetting("Follow Through Walls", true);
    private final BooleanSetting pathAutoJump = new BooleanSetting("Path Auto Jump", true);
    private final BooleanSetting smartDirect = new BooleanSetting("Smart Direct", true);
    private final BooleanSetting renderPath = new BooleanSetting("Render Path", true);
    private final BooleanSetting avoidHazards = new BooleanSetting("Avoid Hazards", true);
    private final BooleanSetting antiVoid = new BooleanSetting("Anti Void", true);
    private final NumberSetting voidThreshold = new NumberSetting("Void Threshold", 0.0, -64.0, 64.0, 1.0);
    private final NumberSetting renderSpacing = new NumberSetting("Render Spacing", 0.5, 0.1, 2.0, 0.1);

    // --- State ---
    private Entity cachedTarget = null;
    private long targetExpireAt = 0L;
    private final Random random = new Random();
    private long randomUntil = 0L;
    private Vec3 randomPosCache = null;

    private final List<BlockPos> path = new ArrayList<>();
    private int pathIndex = 0;

    // render / follow exact points
    private final List<Vec3> renderPoints = new ArrayList<>();
    private int renderIndex = 0;

    private int direction = 1;
    private int pathStuckTicks = 0;
    private double lastPathTargetDist = Double.MAX_VALUE;
    private BlockPos lastPathGoal = null;
    private long lastPathCalcTime = 0L;

    // Module active flag managed locally to avoid running while off
    private volatile boolean moduleActive = false;

    public TargetStrafe() {
        super("target-strafe", Category.MOVEMENT, false, "Strafes around a target entity with optimized pathfinding.");
        // register settings (API-dependent; adapt if your framework differs)
        addSetting(speed);
        addSetting(autoJump);
        addSetting(damageBoost);
        addSetting(boost);
        addSetting(mode);
        addSetting(executeMode);
        addSetting(sortMode);
        addSetting(targetRange);
        addSetting(radius);
        addSetting(avoidRadius);
        addSetting(usePathfinding);
        addSetting(followThroughWalls);
        addSetting(pathAutoJump);
        addSetting(smartDirect);
        addSetting(renderPath);
        addSetting(renderSpacing);
        addSetting(avoidHazards);
        addSetting(antiVoid);
        addSetting(voidThreshold);
    }

    @Override
    public void onEnable() {
        cachedTarget = null;
        targetExpireAt = 0;
        randomPosCache = null;
        randomUntil = 0;
        path.clear();
        pathIndex = 0;
        renderPoints.clear();
        renderIndex = 0;
        moduleActive = true;
    }

    @Override
    public void onDisable() {
        moduleActive = false;
        stopMovement();
    }

    @Override
    public void onTick() {
        if (executeMode.get().equals("Tick")) run();
    }

    // option to call from move event externally if needed
    public void onMove() {
        if (executeMode.get().equals("Move")) run();
    }

    // --- Core ---
    private void run() {
        // ensure we don't run logic while the module is disabled
        if (!moduleActive) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Entity target = pickTarget();
        if (target == null) {
            stopMovement();
            path.clear();
            renderPoints.clear();
            return;
        }

        // auto-jump logic (uses alternative jump via setDeltaMovement)
        if ((autoJump.get() || (mode.get().equals("Avoid") && pathAutoJump.get())) && mc.player.onGround()) {
            BlockPos above = mc.player.blockPosition().above(2);
            if (isAir(above)) mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.42, mc.player.getDeltaMovement().z);
        }

        // decide next action: direct move or pathfinding
        Vec3 playerPos = mc.player.position();
        Vec3 targetPosVec = target.position();

        if (usePathfinding.get()) {
            // smart direct to entity if clear
            if (smartDirect.get() && isPathClearAlt(playerPos, targetPosVec)) {
                path.clear();
                renderPoints.clear();
                moveTo(targetPosVec);
                return;
            }

            // compute/validate goal block
            BlockPos goal = target.blockPosition();
            if (!isSafePath(goal)) {
                BlockPos alt = findClosestSafe(goal, 3);
                if (alt != null) goal = alt;
            }

            long now = System.currentTimeMillis();
            if (path.isEmpty() || pathIndex >= path.size() || now - lastPathCalcTime > 1000L || mc.player.tickCount % 10 == 0) {
                calculatePath(goal);
                lastPathGoal = goal;
                lastPathCalcTime = now;
                pathStuckTicks = 0;
                lastPathTargetDist = Double.MAX_VALUE;
            }

            if (mc.player.horizontalCollision) {
                direction = -direction;
                calculatePath(goal);
                lastPathCalcTime = now;
            }

            followPath();
        } else {
            // legacy orbital strafing around target
            Vec3 next = getOrbitPoint(target, 0);
            if (next != null) moveTo(next);
        }

        // optional render of planned path (particles)
        if (renderPath.get()) {
            renderPathParticles(mc);
        }
    }

    // --- Movement helpers ---
    private double calculateSpeed() {
        Minecraft mc = Minecraft.getInstance();
        double base = speed.get();
        if (mc.player != null && damageBoost.get() && mc.player.hurtTime > 0) base += boost.get();
        return base;
    }

    private Vec3 getOrbitPoint(Entity target, double angleOffset) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || target == null) return null;

        double dist = mc.player.distanceTo(target);
        double r = mode.get().equals("Avoid") ? avoidRadius.get() : radius.get();
        double dx = mc.player.getX() - target.getX();
        double dz = mc.player.getZ() - target.getZ();
        if (dx == 0 && dz == 0) dx = 0.01;
        double currentYaw = Math.atan2(dz, dx);

        if (mc.player.horizontalCollision) direction = -direction;

        if (angleOffset > 0) {
            double targetYaw = currentYaw + (direction * angleOffset);
            double x = target.getX() + Math.cos(targetYaw) * r;
            double z = target.getZ() + Math.sin(targetYaw) * r;
            return new Vec3(x, target.getY(), z);
        }

        double radialStep = r - dist;
        double speedValue = calculateSpeed();
        double cappedRadial = Math.max(-speedValue, Math.min(speedValue, radialStep));
        double tangential = Math.sqrt(Math.max(0, speedValue * speedValue - cappedRadial * cappedRadial));

        if (mode.get().equals("Avoid") && dist < r) {
            cappedRadial = speedValue;
            tangential = 0;
        }

        double moveX = Math.cos(currentYaw) * cappedRadial - Math.sin(currentYaw) * tangential * direction;
        double moveZ = Math.sin(currentYaw) * cappedRadial + Math.cos(currentYaw) * tangential * direction;

        return new Vec3(mc.player.getX() + moveX, mc.player.getY(), mc.player.getZ() + moveZ);
    }

    private void moveTo(Vec3 targetVec) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        double dx = targetVec.x - mc.player.getX();
        double dz = targetVec.z - mc.player.getZ();
        double ang = Math.atan2(dz, dx);
        double s = calculateSpeed();
        double vx = Math.cos(ang) * s;
        double vz = Math.sin(ang) * s;
        mc.player.setDeltaMovement(vx, mc.player.getDeltaMovement().y, vz);
    }

    // --- Pathfinding (A*) ---
    private void calculatePath(BlockPos goal) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        BlockPos start = mc.player.blockPosition();
        if (start.equals(goal)) {
            path.clear();
            renderPoints.clear();
            return;
        }

        if (!isSafePath(goal)) {
            BlockPos alt = findClosestSafe(goal, 3);
            if (alt != null) goal = alt;
        }

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
        Map<BlockPos, Double> g = new HashMap<>();

        open.add(new Node(start, 0, heuristic(start, goal)));
        g.put(start, 0.0);

        int iterations = 0;
        BlockPos best = start;
        double bestDist = start.distSqr(goal);

        while (!open.isEmpty() && iterations < 1000) {
            Node cur = open.poll();
            iterations++;
            if (cur.pos.equals(goal)) {
                reconstructPath(cameFrom, goal);
                return;
            }
            for (BlockPos npos : getPathNeighbors(cur.pos)) {
                double tentative = g.getOrDefault(cur.pos, Double.MAX_VALUE) + 1.0;
                if (tentative < g.getOrDefault(npos, Double.MAX_VALUE)) {
                    cameFrom.put(npos, cur.pos);
                    g.put(npos, tentative);
                    double f = tentative + Math.sqrt(npos.distSqr(goal));
                    open.add(new Node(npos, tentative, f));
                    double d = npos.distSqr(goal);
                    if (d < bestDist) { bestDist = d; best = npos; }
                }
            }
        }
        reconstructPath(cameFrom, best);
    }

    private double heuristic(BlockPos a, BlockPos b) {
        return Math.sqrt(a.distSqr(b));
    }

    private static class Node {
        BlockPos pos;
        double g, f;
        Node(BlockPos pos, double g, double f) { this.pos = pos; this.g = g; this.f = f; }
    }

    private List<BlockPos> getPathNeighbors(BlockPos pos) {
        List<BlockPos> out = new ArrayList<>();
        BlockPos[] dirs = new BlockPos[]{ pos.north(), pos.south(), pos.east(), pos.west() };
        for (BlockPos p : dirs) {
            if (isSafePath(p)) out.add(p);
            else if (isSafePath(p.above()) && isSolidBlock(p)) {
                if (isAir(pos.above(2))) out.add(p.above());
            } else if (isSafePath(p.below()) && isAir(p)) out.add(p.below());
        }
        return out;
    }

    private boolean isSafePath(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (!isAir(pos) || !isAir(pos.above())) return false;
        BlockPos below = pos.below();
        if (avoidHazards.get() && (isHazard(below) || isHazard(pos))) return false;
        if (isAir(below)) {
            if (antiVoid.get() && isOverVoidAlt(new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) return false;
            return isSolidBlock(below.below()) || isSolidBlock(below.below(2)) || isSolidBlock(below.below(3));
        }
        return isSolidBlock(below);
    }

    private void reconstructPath(Map<BlockPos, BlockPos> cameFrom, BlockPos goal) {
        path.clear();
        BlockPos cur = goal;
        while (cur != null) {
            path.add(0, cur);
            cur = cameFrom.get(cur);
        }
        pathIndex = 0;
        generateRenderedPathPoints();
        renderIndex = 0;
    }

    private BlockPos findClosestSafe(BlockPos center, int radius) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    if (isSafePath(p)) {
                        double ddx = p.getX() - center.getX();
                        double ddy = p.getY() - center.getY();
                        double ddz = p.getZ() - center.getZ();
                        double d2 = ddx*ddx + ddy*ddy + ddz*ddz;
                        if (d2 < bestDist) { bestDist = d2; best = p; }
                    }
                }
            }
        }
        return best;
    }

    // follow exact renderPoints if available
    private void followPath() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (path.isEmpty() || pathIndex >= path.size()) { stopMovement(); return; }

        Vec3 playerPos = mc.player.position();

        // try jump-ahead to reachable later node
        for (int i = path.size() -1; i >= pathIndex; i--) {
            BlockPos bp = path.get(i);
            Vec3 cand = new Vec3(bp.getX()+0.5, bp.getY(), bp.getZ()+0.5);
            if (isPathClearAlt(playerPos, cand)) {
                pathIndex = i;
                generateRenderedPathPoints();
                break;
            }
        }

        BlockPos node = path.get(pathIndex);
        Vec3 targetVec;
        if (renderPath.get() && !renderPoints.isEmpty()) {
            if (renderIndex >= renderPoints.size()) renderIndex = renderPoints.size()-1;
            targetVec = renderPoints.get(renderIndex);
        } else {
            targetVec = new Vec3(node.getX()+0.5, node.getY(), node.getZ()+0.5);
        }

        double distSq = mc.player.distanceToSqr(targetVec.x, targetVec.y, targetVec.z);
        if (distSq >= lastPathTargetDist - 1e-6) pathStuckTicks++; else { pathStuckTicks = 0; lastPathTargetDist = distSq; }

        if (distSq < 0.16) {
            if (renderPath.get() && !renderPoints.isEmpty()) {
                renderIndex++;
                if (renderIndex >= renderPoints.size()) {
                    pathIndex++;
                    renderIndex = 0;
                    if (pathIndex >= path.size()) { stopMovement(); return; }
                    generateRenderedPathPoints();
                }
            } else {
                pathIndex++;
                if (pathIndex >= path.size()) { stopMovement(); return; }
            }
        }

        if (pathStuckTicks > 12) {
            if (lastPathGoal != null) {
                BlockPos alt = findClosestSafe(lastPathGoal, 5);
                if (alt != null && !alt.equals(lastPathGoal)) {
                    calculatePath(alt);
                    lastPathGoal = alt;
                    lastPathCalcTime = System.currentTimeMillis();
                    pathStuckTicks = 0;
                    lastPathTargetDist = Double.MAX_VALUE;
                    return;
                }
            }
            direction = -direction;
            if (lastPathGoal != null) { calculatePath(lastPathGoal); lastPathCalcTime = System.currentTimeMillis(); }
            else path.clear();
            pathStuckTicks = 0; lastPathTargetDist = Double.MAX_VALUE;
            return;
        }

        // handle small obstacle: try auto-jump/step-up
        if (!isPathClearAlt(playerPos, targetVec)) {
            boolean handled = tryHandleObstacle(mc, playerPos, node, targetVec);
            if (!handled) {
                for (int i = pathIndex+1; i < Math.min(path.size(), pathIndex+5); i++) {
                    Vec3 cand = new Vec3(path.get(i).getX()+0.5, path.get(i).getY(), path.get(i).getZ()+0.5);
                    if (isPathClearAlt(playerPos, cand)) {
                        pathIndex = i;
                        generateRenderedPathPoints();
                        renderIndex = 0;
                        break;
                    }
                }
            }
        }

        if (pathAutoJump.get() && node.getY() > mc.player.getY() + 0.5 && mc.player.onGround()) {
            if (isAir(mc.player.blockPosition().above(2))) mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.42, mc.player.getDeltaMovement().z);
        }

        moveTo(targetVec);
    }

    private boolean tryHandleObstacle(Minecraft mc, Vec3 playerPos, BlockPos targetPos, Vec3 targetVec) {
        if (mc.player == null) return false;
        double dy = targetVec.y - mc.player.getY();
        if (dy > 0.5 && mc.player.onGround() && pathAutoJump.get()) {
            if (isAir(mc.player.blockPosition().above(2))) {
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.42, mc.player.getDeltaMovement().z);
                return true;
            }
        }
        Vec3 look = new Vec3(targetVec.x - playerPos.x, 0, targetVec.z - playerPos.z);
        double len = Math.sqrt(look.x*look.x + look.z*look.z);
        if (len > 1e-6) {
            double nx = look.x/len, nz = look.z/len;
            double probeX = mc.player.getX() + nx*0.6, probeZ = mc.player.getZ() + nz*0.6;
            BlockPos probeHead = BlockPos.containing(probeX, mc.player.getY()+1.5, probeZ);
            if (!isAir(probeHead) && mc.player.onGround() && pathAutoJump.get()) {
                if (isAir(mc.player.blockPosition().above(2))) {
                    mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.42, mc.player.getDeltaMovement().z);
                    return true;
                }
            }
        }
        BlockPos up = targetPos.above();
        if (isSafePath(up)) {
            path.set(pathIndex, up);
            generateRenderedPathPoints();
            return true;
        }
        return false;
    }

    // --- Utilities (non-raytrace implementations) ---
    private boolean hasLineOfSightAlt(Entity e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || e == null) return false;
        Vec3 start = mc.player.getEyePosition(1.0F);
        Vec3 end = e.getBoundingBox().getCenter();
        Vec3 diff = end.subtract(start);
        double len = diff.length();
        if (len <= 0) return true;
        int steps = Math.max(4, (int)Math.ceil(len*2.0));
        for (int i=0;i<=steps;i++) {
            double t = (double)i/steps;
            Vec3 p = new Vec3(start.x + diff.x*t, start.y + diff.y*t, start.z + diff.z*t);
            BlockPos bp = BlockPos.containing(p.x, p.y, p.z);
            if (isSolidBlock(bp)) return false;
        }
        return true;
    }

    private boolean isPathClearAlt(Vec3 start, Vec3 end) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 diff = end.subtract(start);
        double len = diff.length();
        if (len <= 0) return true;
        int steps = Math.max(4, (int)Math.ceil(len*2.0));
        for (int i=0;i<=steps;i++) {
            double t = (double)i/steps;
            Vec3 p = new Vec3(start.x + diff.x*t, start.y + diff.y*t, start.z + diff.z*t);
            BlockPos floor = BlockPos.containing(p.x, Math.floor(p.y)-1, p.z);
            BlockPos head = BlockPos.containing(p.x, Math.floor(p.y)+1, p.z);
            if (!isAir(head)) return false;
            if (Minecraft.getInstance().level.getBlockState(floor).isAir()) {
                if (antiVoid.get() && isOverVoidAlt(p)) return false;
                if (isHazard(floor)) return false;
            } else if (isHazard(floor)) return false;
        }
        return true;
    }

    private boolean isOverVoidAlt(Vec3 pos) {
        Minecraft mc = Minecraft.getInstance();
        int startY = (int)Math.floor(pos.y);
        int thresholdY = (int)Math.floor(voidThreshold.get());
        int minY = Math.max(thresholdY - 5, mc.level.getMinY());
        for (int y = startY; y >= minY; y--) {
            BlockPos bp = new BlockPos((int)Math.floor(pos.x), y, (int)Math.floor(pos.z));
            if (!mc.level.getBlockState(bp).isAir()) return false;
        }
        return true;
    }

    private boolean isSolidBlock(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        return !mc.level.getBlockState(pos).getCollisionShape(mc.level, pos).isEmpty();
    }

    private boolean isAir(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        return mc.level.getBlockState(pos).isAir();
    }

    private boolean isHazard(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        var s = mc.level.getBlockState(pos);
        return s.is(Blocks.LAVA) || s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE) || s.is(Blocks.CAMPFIRE) || s.is(Blocks.SOUL_CAMPFIRE) || s.is(Blocks.MAGMA_BLOCK);
    }

    // --- Render path particles (use same points the player follows) ---
    private void renderPathParticles(Minecraft mc) {
        if (mc.level == null || renderPoints.isEmpty()) return;
        for (Vec3 p : renderPoints) {
            mc.level.addParticle(ParticleTypes.END_ROD, p.x, p.y, p.z, 0.0, 0.0, 0.0);
        }
    }

    private void generateRenderedPathPoints() {
        renderPoints.clear();
        if (path.isEmpty()) return;
        double spacing = Math.max(0.1, renderSpacing.get());
        for (int i = Math.max(0, pathIndex); i < path.size() - 1; i++) {
            Vec3 a = new Vec3(path.get(i).getX()+0.5, path.get(i).getY()+0.1, path.get(i).getZ()+0.5);
            Vec3 b = new Vec3(path.get(i+1).getX()+0.5, path.get(i+1).getY()+0.1, path.get(i+1).getZ()+0.5);
            Vec3 diff = b.subtract(a);
            double len = diff.length();
            int steps = Math.max(1, (int)Math.ceil(len/spacing));
            for (int s=0;s<=steps;s++) {
                double t = (double)s/steps;
                Vec3 p = new Vec3(a.x + diff.x*t, a.y + diff.y*t, a.z + diff.z*t);
                if (renderPoints.isEmpty() || renderPoints.get(renderPoints.size()-1).distanceTo(p) > 1e-6) renderPoints.add(p);
            }
        }
        BlockPos last = path.get(path.size()-1);
        Vec3 end = new Vec3(last.getX()+0.5, last.getY()+0.1, last.getZ()+0.5);
        if (renderPoints.isEmpty() || renderPoints.get(renderPoints.size()-1).distanceTo(end) > 1e-6) renderPoints.add(end);
        if (renderIndex >= renderPoints.size()) renderIndex = 0;
    }

    // --- Target selection (simple) ---
    private Entity pickTarget() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return null;

        long now = System.currentTimeMillis();
        if (cachedTarget != null && now < targetExpireAt && cachedTarget.isAlive() && mc.player.distanceTo(cachedTarget) <= targetRange.get()) {
            if (followThroughWalls.get() || hasLineOfSightAlt(cachedTarget)) return cachedTarget;
        }

        List<Entity> targets = new ArrayList<>();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity) || e == mc.player) continue;
            double d = mc.player.distanceTo(e);
            if (d > targetRange.get()) continue;
            if (!followThroughWalls.get() && !hasLineOfSightAlt(e)) continue;
            targets.add(e);
        }
        if (targets.isEmpty()) return null;

        Entity best = targets.stream().min(Comparator.comparingDouble(e -> mc.player.distanceTo(e))).orElse(null);
        cachedTarget = best;
        targetExpireAt = now + 250;
        return best;
    }

    private void stopMovement() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.setDeltaMovement(0, mc.player.getDeltaMovement().y, 0);
    }
}
