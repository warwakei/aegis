package fun.aegis.utils.features.aura.utils;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.ItemStack;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.utils.client.packet.network.Network;
import fun.aegis.Aegis;
import fun.aegis.features.impl.combat.Aura;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Pressing implements QuickImports {
    private final int[] funTimeTicks = new int[]{10, 11, 10, 13}, spookyTicks = new int[]{11, 10, 13, 10, 12, 11, 12}, defaultTicks = new int[]{10, 11};
    long lastClickTime = System.currentTimeMillis();
    private static final long MINIMUM_COOLDOWN_MS = 500;
    private int dragCounter = 0;
    private int butterflyCounter = 0;
    private int jitterCounter = 0;


    public boolean isCooldownComplete(boolean dynamicCooldown, int ticks) {
        boolean isMace = isHoldingMace();
        
        long requiredDelay = 500;
        boolean is18Mode = false;
        String clickType = "Normal";
        
        try {
            Aura aura = Aura.getInstance();
            
            if (aura != null && aura.isState()) {
                if (aura.getMode18().isValue()) {
                    is18Mode = true;
                    Float cpsValue = aura.getCps().getValue();
                    if (cpsValue > 0) {
                        requiredDelay = (long) (1000.0f / cpsValue);
                    }
                    clickType = aura.getClickType().getSelected();
                }
            }
        } catch (Exception e) {
            requiredDelay = 500;
        }
        
        if (is18Mode) {
            long timePassed = lastClickPassed();
            
            switch (clickType) {
                case "Drag":
                    return handleDragClick(timePassed, requiredDelay);
                case "Butterfly":
                    return handleButterflyClick(timePassed, requiredDelay);
                case "Jitter-click":
                    return handleJitterClick(timePassed, requiredDelay);
                default: // Normal
                    return timePassed >= requiredDelay;
            }
        }
        
        boolean cooldownReady = isMace || mc.player.getAttackCooldownProgress(ticks) > 0.9F;
        boolean minimumDelayPassed = lastClickPassed() >= requiredDelay;

        return cooldownReady && minimumDelayPassed;
    }

    private boolean handleDragClick(long timePassed, long requiredDelay) {
        // Drag: более плавные клики с небольшой задержкой между ними
        if (timePassed >= requiredDelay) {
            dragCounter++;
            return dragCounter % 2 == 0; // Каждый второй клик проходит
        }
        return false;
    }

    private boolean handleButterflyClick(long timePassed, long requiredDelay) {
        // Butterfly: быстрые двойные клики с чередованием
        long halfDelay = requiredDelay / 2;
        if (timePassed >= halfDelay) {
            butterflyCounter++;
            return true; // Оба клика проходят
        }
        return false;
    }

    private boolean handleJitterClick(long timePassed, long requiredDelay) {
        // Jitter-click: клики с небольшой случайной задержкой
        long jitterDelay = requiredDelay + (System.nanoTime() % 50); // Добавляем случайность
        if (timePassed >= jitterDelay) {
            jitterCounter++;
            return true;
        }
        return false;
    }

    public boolean hasTicksElapsedSinceLastClick(int ticks) {
        return lastClickPassed() >= (ticks * 50L * (20F / Network.TPS));
    }

    public long lastClickPassed() {
        return System.currentTimeMillis() - lastClickTime;
    }

    public void recalculate() {
        lastClickTime = System.currentTimeMillis();
    }

    int tickCount() {
        int count = Aegis.getInstance().getAttackPerpetrator().getAttackHandler().getCount();
        return switch (Network.server) {

            default -> defaultTicks[count % defaultTicks.length];
        };
    }

    private boolean isHoldingMace() {
        ItemStack mainHand = mc.player.getMainHandStack();

        return mainHand.getItem().getTranslationKey().toLowerCase().contains("mace");
    }
}
