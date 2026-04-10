package rich.netpanel.loggers;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Hitreg logger — logs all attack attempts with detailed info:
 * target, distance, hit result, damage, miss reasons.
 * Supports multiline detailed entries (\n rendered by NetPanel frontend).
 */
public class HitregLogger {

    private static final LogBuffer BUFFER = new LogBuffer(500);

    public static void logAttackAttempt(LivingEntity target, double distance, boolean hit, String reason, double damage) {
        String targetName = target != null ? target.getName().getString() : "null";
        String status = hit ? "HIT" : "MISS";
        String msg = String.format("[%s] target=%s dist=%.2f dmg=%.1f reason=%s",
                status, targetName, distance, damage, reason);
        BUFFER.add(hit ? "HIT" : "MISS", msg);
    }

    public static void logAttackAttempt(LivingEntity target, double distance, boolean hit, String reason) {
        logAttackAttempt(target, distance, hit, reason, 0.0);
    }

    public static void logAuraAttack(String module, LivingEntity target, double distance, boolean hit, String reason) {
        logDetailedAttack(module, target, distance, hit, reason, 0.0, null, null);
    }

    public static void logAuraAttack(String module, LivingEntity target, double distance, boolean hit, String reason, String whileCondition) {
        logDetailedAttack(module, target, distance, hit, reason, 0.0, null, whileCondition);
    }

    public static void logMaceAttack(String stage, LivingEntity target, double distance, boolean hit, String reason) {
        logDetailedAttack("MaceTarget/" + stage, target, distance, hit, reason, 0.0, null, null);
    }

    public static void logMaceAttack(String stage, LivingEntity target, double distance, boolean hit, String reason, String whileCondition) {
        logDetailedAttack("MaceTarget/" + stage, target, distance, hit, reason, 0.0, null, whileCondition);
    }

    /**
     * Logs a detailed multiline attack entry for the Hitreg panel.
     * Each line is rendered separately thanks to \n support.
     */
    public static void logDetailedAttack(String source, LivingEntity target, double distance,
                                         boolean hit, String reason, double damage, String extra) {
        logDetailedAttack(source, target, distance, hit, reason, damage, extra, null);
    }

    /**
     * Full version with "While" condition display (player state at attack moment).
     */
    public static void logDetailedAttack(String source, LivingEntity target, double distance,
                                         boolean hit, String reason, double damage, String extra, String whileCondition) {
        String targetName = target != null ? target.getName().getString() : "null";
        String status = hit ? "HIT" : "MISS";
        String statusIcon = hit ? "✔" : "✘";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s [%s/%s] %s", statusIcon, source, status, targetName));
        sb.append(String.format("\n  Dist: %.2f | Dmg: %.1f", distance, damage));
        if (reason != null && !reason.isEmpty()) {
            sb.append(String.format("\n  Reason: %s", reason));
        }
        if (whileCondition != null && !whileCondition.isEmpty()) {
            sb.append(String.format("\n  While: %s", whileCondition));
        }
        if (extra != null && !extra.isEmpty()) {
            sb.append(String.format("\n  %s", extra));
        }

        BUFFER.add(hit ? "HIT" : "MISS", sb.toString());
    }

    /**
     * Builds a "While" condition string from the player's current state.
     * Example: "Sprint, Fall (d=2.3)" or "Jump, ElytraFlying"
     */
    public static String buildWhileCondition(net.minecraft.entity.player.PlayerEntity player) {
        if (player == null) return "null";

        List<String> conditions = new ArrayList<>();

        if (player.isSprinting()) conditions.add("Sprint");
        if (player.isJumping()) conditions.add("Jump");
        if (player.isGliding()) conditions.add("ElytraFlying");

        double velocityY = player.getVelocity().y;
        if (player.isOnGround()) {
            conditions.add("OnGround");
        } else if (velocityY < -0.1) {
            conditions.add(String.format("Fall (d=%.1f)", player.fallDistance));
        } else if (velocityY > 0.1) {
            conditions.add("Rising");
        } else {
            conditions.add("InAir");
        }

        if (player.isSwimming()) conditions.add("Swimming");
        if (player.isSneaking()) conditions.add("Sneak");
        if (player.isUsingItem()) conditions.add("UsingItem");
        if (player.isTouchingWater()) conditions.add("InWater");

        return conditions.isEmpty() ? "Idle" : String.join(", ", conditions);
    }

    /**
     * Returns the last N entries formatted for clipboard copy.
     * Each entry is separated by a blank line.
     */
    public static String getFormattedEntries(int count) {
        List<LogBuffer.LogEntry> entries = BUFFER.getLatest(count);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) sb.append("\n\n");
            sb.append(entries.get(i).message());
        }
        return sb.toString();
    }

    public static void logInfo(String msg) {
        BUFFER.add("INFO", msg);
    }

    public static LogBuffer getBuffer() {
        return BUFFER;
    }
}
