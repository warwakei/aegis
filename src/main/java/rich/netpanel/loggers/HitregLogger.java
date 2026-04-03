package rich.netpanel.loggers;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

/**
 * Hitreg logger — logs all attack attempts with detailed info:
 * target, distance, hit result, damage, miss reasons.
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
        String targetName = target != null ? target.getName().getString() : "null";
        String status = hit ? "HIT" : "MISS";
        String msg = String.format("[Aura/%s] [%s] %s | dist=%.2f | %s",
                module, status, targetName, distance, reason);
        BUFFER.add(hit ? "HIT" : "MISS", msg);
    }

    public static void logMaceAttack(String stage, LivingEntity target, double distance, boolean hit, String reason) {
        String targetName = target != null ? target.getName().getString() : "null";
        String status = hit ? "HIT" : "MISS";
        String msg = String.format("[MaceTarget/%s] [%s] %s | dist=%.2f | %s",
                stage, status, targetName, distance, reason);
        BUFFER.add(hit ? "HIT" : "MISS", msg);
    }

    public static void logInfo(String msg) {
        BUFFER.add("INFO", msg);
    }

    public static LogBuffer getBuffer() {
        return BUFFER;
    }
}
