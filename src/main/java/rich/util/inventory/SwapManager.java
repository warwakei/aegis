package rich.util.inventory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.util.Hand;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SwapManager {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Map<String, SwapSequence> sequences = new ConcurrentHashMap<>();
    private static SwapSequence activeSequence;

    private SwapManager() {}

    public static void tick() {
        if (activeSequence != null) {
            activeSequence.tick();
            if (activeSequence.isFinished()) {
                activeSequence = null;
            }
        }
        sequences.values().removeIf(SwapSequence::isFinished);
        sequences.values().forEach(SwapSequence::tick);
    }

    public static void execute(SwapSequence sequence) {
        if (activeSequence != null) {
            activeSequence.cancel();
        }
        activeSequence = sequence.start();
    }

    public static void execute(String name, SwapSequence sequence) {
        SwapSequence existing = sequences.get(name);
        if (existing != null) {
            existing.cancel();
        }
        sequences.put(name, sequence.start());
    }

    public static void cancel() {
        if (activeSequence != null) {
            activeSequence.cancel();
            activeSequence = null;
        }
    }

    public static void cancel(String name) {
        SwapSequence sequence = sequences.remove(name);
        if (sequence != null) {
            sequence.cancel();
        }
    }

    public static void cancelAll() {
        cancel();
        sequences.values().forEach(SwapSequence::cancel);
        sequences.clear();
    }

    public static boolean isRunning() {
        return activeSequence != null && !activeSequence.isFinished();
    }

    public static boolean isRunning(String name) {
        SwapSequence seq = sequences.get(name);
        return seq != null && !seq.isFinished();
    }

    public static void swapAndUse(Item item) {
        InventoryResult result = InventoryUtils.find(item);
        if (!result.found()) return;

        if (result.isHotbar()) {
            execute(new SwapSequence()
                    .step(0, InventoryUtils::saveSlot)
                    .step(0, () -> InventoryUtils.selectSlot(result.slot()))
                    .step(1, () -> InventoryUtils.use(Hand.MAIN_HAND))
                    .step(1, InventoryUtils::restoreSlot));
        } else {
            int hotbar = InventoryUtils.currentSlot();
            execute(new SwapSequence()
                    .step(0, () -> InventoryUtils.swapHotbar(result.slot(), hotbar))
                    .step(1, () -> InventoryUtils.use(Hand.MAIN_HAND))
                    .step(1, () -> InventoryUtils.swapHotbar(result.slot(), hotbar))
                    .step(0, InventoryUtils::closeScreen));
        }
    }

    public static void swapAndUseSilent(Item item) {
        InventoryResult result = InventoryUtils.find(item);
        if (!result.found()) return;

        if (result.isHotbar()) {
            execute(new SwapSequence()
                    .step(0, InventoryUtils::saveSlot)
                    .step(0, () -> InventoryUtils.selectSlotSilent(result.slot()))
                    .step(0, () -> InventoryUtils.use(Hand.MAIN_HAND))
                    .step(0, InventoryUtils::restoreSlotSilent));
        } else {
            int hotbar = InventoryUtils.currentSlot();
            execute(new SwapSequence()
                    .step(0, () -> InventoryUtils.swapHotbar(result.slot(), hotbar))
                    .step(0, () -> InventoryUtils.use(Hand.MAIN_HAND))
                    .step(0, () -> InventoryUtils.swapHotbar(result.slot(), hotbar))
                    .step(0, InventoryUtils::closeScreen));
        }
    }

    public static void moveToHotbar(Item item, int hotbarSlot) {
        InventoryResult result = InventoryUtils.find(item);
        if (!result.found() || result.isHotbar()) return;

        execute(new SwapSequence()
                .step(0, () -> InventoryUtils.swapHotbar(result.slot(), hotbarSlot)));
    }

    public static void swapSlots(int from, int to) {
        execute(new SwapSequence()
                .step(0, () -> InventoryUtils.swap(from, to)));
    }
}