package rich.modules.impl.misc;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import org.lwjgl.glfw.GLFW;
import rich.events.api.EventHandler;
import rich.events.impl.RotationUpdateEvent;
import rich.events.impl.WorldRenderEvent;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.impl.combat.aura.MathAngle;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BindSetting;
import rich.modules.module.setting.implement.ColorSetting;
import rich.modules.module.setting.implement.SelectSetting;
import rich.util.ColorUtil;
import rich.util.inventory.*;
import rich.util.math.MathUtils;
import rich.util.render.Render3D;
import rich.util.repository.friend.FriendUtils;
import rich.util.string.PlayerInteractionHelper;
import rich.util.timer.StopWatch;

import java.util.*;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServerHelper extends ModuleStructure {

    SelectSetting mode = new SelectSetting("Тип сервера", "Позволяет выбрать тип сервера")
            .value("ReallyWorld", "HolyWorld", "FunTime")
            .selected("FunTime");

    SelectSetting swapMode = new SelectSetting("Режим свапа", "Способ свапа предметов")
            .value("Instant", "Legit")
            .selected("Legit");

    ColorSetting boxFillColor = new ColorSetting("Цвет заливки", "Цвет заливки бокса")
            .value(ColorUtil.getColor(130, 32, 16, 40))
            .visible(() -> mode.isSelected("FunTime"));

    ColorSetting boxLineColor = new ColorSetting("Цвет линий", "Цвет линий бокса")
            .value(ColorUtil.getColor(130, 32, 16, 255))
            .visible(() -> mode.isSelected("FunTime"));

    List<KeyBind> keyBindings = new ArrayList<>();

    List<String> potionQueue = new ArrayList<>();
    StopWatch potionTimer = new StopWatch();
    Map<String, ItemInfo> itemConfig = new HashMap<>();
    Map<String, Boolean> lastKeyStates = new HashMap<>();
    Map<String, Boolean> keyPressedThisTick = new HashMap<>();

    @NonFinal int originalSlot = -1;
    @NonFinal int targetSlot = -1;
    @NonFinal ActionState actionState = ActionState.IDLE;
    @NonFinal long actionTimer = 0;
    @NonFinal String pendingItemKey = null;
    @NonFinal long stopMovementUntil = 0;
    @NonFinal boolean keysOverridden = false;
    @NonFinal boolean wasForwardPressed, wasBackPressed, wasLeftPressed, wasRightPressed;
    @NonFinal int originalSourceSlot = -1;

    MovementController movement = new MovementController();

    private enum ActionState {
        IDLE, SLOWING_DOWN, WAITING_STOP, SWAP_TO_ITEM, USE_ITEM, SWAP_BACK, SPEEDING_UP
    }

    private static class EffectRequirement {
        RegistryEntry<StatusEffect> effect;
        int minAmplifier;

        EffectRequirement(RegistryEntry<StatusEffect> effect, int minAmplifier) {
            this.effect = effect;
            this.minAmplifier = minAmplifier;
        }
    }

    private static class ItemInfo {
        List<String> loreKeywords;
        String nameFallback;
        Item item;
        String displayName;
        boolean funTimeOnly;
        List<EffectRequirement> effectRequirements;

        ItemInfo(List<String> loreKeywords, String nameFallback, Item item, String displayName, boolean funTimeOnly) {
            this.loreKeywords = loreKeywords;
            this.nameFallback = nameFallback;
            this.item = item;
            this.displayName = displayName;
            this.funTimeOnly = funTimeOnly;
            this.effectRequirements = null;
        }

        ItemInfo(List<String> loreKeywords, String nameFallback, Item item, String displayName, boolean funTimeOnly, List<EffectRequirement> effectRequirements) {
            this.loreKeywords = loreKeywords;
            this.nameFallback = nameFallback;
            this.item = item;
            this.displayName = displayName;
            this.funTimeOnly = funTimeOnly;
            this.effectRequirements = effectRequirements;
        }

        ItemInfo(String nameFallback, Item item, String displayName) {
            this.loreKeywords = null;
            this.nameFallback = nameFallback;
            this.item = item;
            this.displayName = displayName;
            this.funTimeOnly = false;
            this.effectRequirements = null;
        }
    }

    public ServerHelper() {
        super("Server Assist", "Помощник для серверов", ModuleCategory.MISC);
        initialize();
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    public void initialize() {
        settings(mode, swapMode, boxFillColor, boxLineColor);

        keyBindings.add(new KeyBind(Items.FIREWORK_STAR, new BindSetting("Анти полет", "Клавиша анти полета")
                .visible(() -> mode.isSelected("ReallyWorld")), 0));
        keyBindings.add(new KeyBind(Items.FLOWER_BANNER_PATTERN, new BindSetting("Свиток опыта", "Клавиша свитка опыта")
                .visible(() -> mode.isSelected("ReallyWorld")), 0));
        keyBindings.add(new KeyBind(Items.PRISMARINE_SHARD, new BindSetting("Взрывная трапка", "Клавиша взрывной трапки")
                .visible(() -> mode.isSelected("HolyWorld")), 5));
        keyBindings.add(new KeyBind(Items.POPPED_CHORUS_FRUIT, new BindSetting("Обычная трапка", "Клавиша обычной трапки")
                .visible(() -> mode.isSelected("HolyWorld")), 0));
        keyBindings.add(new KeyBind(Items.NETHER_STAR, new BindSetting("Стан", "Клавиша стана")
                .visible(() -> mode.isSelected("HolyWorld")), 30));
        keyBindings.add(new KeyBind(Items.FIRE_CHARGE, new BindSetting("Взрывная штучка", "Клавиша взрывной штучки")
                .visible(() -> mode.isSelected("HolyWorld")), 5));
        keyBindings.add(new KeyBind(Items.SNOWBALL, new BindSetting("Снежок заморозка", "Клавиша снежка")
                .visible(() -> mode.isSelected("HolyWorld") || mode.isSelected("FunTime")), 7));
        keyBindings.add(new KeyBind(Items.PHANTOM_MEMBRANE, new BindSetting("Божья аура", "Клавиша божьей ауры")
                .visible(() -> mode.isSelected("FunTime")), 2));
        keyBindings.add(new KeyBind(Items.NETHERITE_SCRAP, new BindSetting("Трапка", "Клавиша трапки")
                .visible(() -> mode.isSelected("FunTime")), 0));
        keyBindings.add(new KeyBind(Items.DRIED_KELP, new BindSetting("Пласт", "Клавиша пласта")
                .visible(() -> mode.isSelected("FunTime")), 0));
        keyBindings.add(new KeyBind(Items.SUGAR, new BindSetting("Явная пыль", "Клавиша явной пыли")
                .visible(() -> mode.isSelected("FunTime")), 10));
        keyBindings.add(new KeyBind(Items.FIRE_CHARGE, new BindSetting("Огненный смерч", "Клавиша огненного смерча")
                .visible(() -> mode.isSelected("FunTime")), 10));
        keyBindings.add(new KeyBind(Items.ENDER_EYE, new BindSetting("Дезориентация", "Клавиша дезориентации")
                .visible(() -> mode.isSelected("FunTime")), 10));
        keyBindings.add(new KeyBind(Items.JACK_O_LANTERN, new BindSetting("Светильник Джека", "Клавиша светильника Джека")
                .visible(() -> mode.isSelected("HolyWorld")), 0));
        keyBindings.add(new KeyBind(Items.EXPERIENCE_BOTTLE, new BindSetting("Пузырь опыта", "Клавиша пузыря опыта")
                .visible(() -> mode.isSelected("HolyWorld")), 0));
        keyBindings.add(new KeyBind(Items.PINK_SHULKER_BOX, new BindSetting("Рюкзак 1 уровня", "Клавиша рюкзака 1 уровня")
                .visible(() -> mode.isSelected("HolyWorld")), 0));
        keyBindings.add(new KeyBind(Items.BLUE_SHULKER_BOX, new BindSetting("Рюкзак 2 уровня", "Клавиша рюкзака 2 уровня")
                .visible(() -> mode.isSelected("HolyWorld")), 0));
        keyBindings.add(new KeyBind(Items.RED_SHULKER_BOX, new BindSetting("Рюкзак 3 уровня", "Клавиша рюкзака 3 уровня")
                .visible(() -> mode.isSelected("HolyWorld")), 0));
        keyBindings.add(new KeyBind(Items.PINK_SHULKER_BOX, new BindSetting("Рюкзак 4 уровня", "Клавиша рюкзака 4 уровня")
                .visible(() -> mode.isSelected("HolyWorld")), 0));

        keyBindings.add(new KeyBind(Items.SPLASH_POTION, new BindSetting("Хлопушка", "Клавиша хлопушки")
                .visible(() -> mode.isSelected("FunTime")), 0));
        keyBindings.add(new KeyBind(Items.SPLASH_POTION, new BindSetting("Святая вода", "Клавиша святой воды")
                .visible(() -> mode.isSelected("FunTime")), 0));
        keyBindings.add(new KeyBind(Items.SPLASH_POTION, new BindSetting("Зелье Гнева", "Клавиша зелья гнева")
                .visible(() -> mode.isSelected("FunTime")), 0));
        keyBindings.add(new KeyBind(Items.SPLASH_POTION, new BindSetting("Зелье Палладина", "Клавиша зелья палладина")
                .visible(() -> mode.isSelected("FunTime")), 0));
        keyBindings.add(new KeyBind(Items.SPLASH_POTION, new BindSetting("Зелье Ассасина", "Клавиша зелья ассасина")
                .visible(() -> mode.isSelected("FunTime")), 0));
        keyBindings.add(new KeyBind(Items.SPLASH_POTION, new BindSetting("Зелье Радиации", "Клавиша зелья радиации")
                .visible(() -> mode.isSelected("FunTime")), 0));
        keyBindings.add(new KeyBind(Items.SPLASH_POTION, new BindSetting("Снотворное", "Клавиша снотворного")
                .visible(() -> mode.isSelected("FunTime")), 0));

        keyBindings.forEach(bind -> settings(bind.setting));

        itemConfig.put("sugar", new ItemInfo(
                List.of("световая вспышка", "радиус: 10 блоков", "свечение", "слепота"),
                "явная пыль", Items.SUGAR, "Явная пыль", true));

        itemConfig.put("disorientation", new ItemInfo(
                List.of("чем ближе цель, тем дольше длительность эффектов"),
                "дезориентация", Items.ENDER_EYE, "Дезориентация", true));

        itemConfig.put("trap", new ItemInfo(
                List.of("нерушимая клетка", "длительность: 15 секунд"),
                "трапка", Items.NETHERITE_SCRAP, "Трапка", true));

        itemConfig.put("plast", new ItemInfo(
                List.of("нерушимая стена", "вертикальный:", "горизонтальный:"),
                "пласт", Items.DRIED_KELP, "Пласт", true));

        itemConfig.put("fireSwirl", new ItemInfo(
                List.of("огненная волна", "радиус: 10 блоков", "поджог"),
                "огненный смерч", Items.FIRE_CHARGE, "Огненный смерч", true));

        itemConfig.put("snow", new ItemInfo(
                List.of("ледяная сфера", "радиус: 7 блоков", "заморозка", "слабость"),
                "снежок заморозка", Items.SNOWBALL, "Снежок заморозка", true));

        itemConfig.put("bojaura", new ItemInfo(
                List.of("божественная аура", "радиус: 2 блока", "снятие всех эффектов", "невидимость"),
                "божья аура", Items.PHANTOM_MEMBRANE, "Божья аура", true));

        itemConfig.put("hlopushka", new ItemInfo(
                null, "хлопушка", Items.SPLASH_POTION, "Хлопушка", true,
                List.of(
                        new EffectRequirement(StatusEffects.SLOWNESS, 9),
                        new EffectRequirement(StatusEffects.SPEED, 4),
                        new EffectRequirement(StatusEffects.BLINDNESS, 9),
                        new EffectRequirement(StatusEffects.GLOWING, 0)
                )));

        itemConfig.put("holywater", new ItemInfo(
                null, "святая вода", Items.SPLASH_POTION, "Святая вода", true,
                List.of(
                        new EffectRequirement(StatusEffects.REGENERATION, 2),
                        new EffectRequirement(StatusEffects.INVISIBILITY, 1),
                        new EffectRequirement(StatusEffects.INSTANT_HEALTH, 1)
                )));

        itemConfig.put("gnev", new ItemInfo(
                null, "зелье гнева", Items.SPLASH_POTION, "Зелье Гнева", true,
                List.of(
                        new EffectRequirement(StatusEffects.STRENGTH, 4),
                        new EffectRequirement(StatusEffects.SLOWNESS, 3)
                )));

        itemConfig.put("paladin", new ItemInfo(
                null, "зелье палладина", Items.SPLASH_POTION, "Зелье Палладина", true,
                List.of(
                        new EffectRequirement(StatusEffects.RESISTANCE, 0),
                        new EffectRequirement(StatusEffects.FIRE_RESISTANCE, 0),
                        new EffectRequirement(StatusEffects.INVISIBILITY, 0),
                        new EffectRequirement(StatusEffects.HEALTH_BOOST, 2)
                )));

        itemConfig.put("assassin", new ItemInfo(
                null, "зелье ассасина", Items.SPLASH_POTION, "Зелье Ассасина", true,
                List.of(
                        new EffectRequirement(StatusEffects.STRENGTH, 3),
                        new EffectRequirement(StatusEffects.SPEED, 2),
                        new EffectRequirement(StatusEffects.HASTE, 0),
                        new EffectRequirement(StatusEffects.INSTANT_DAMAGE, 1)
                )));

        itemConfig.put("radiation", new ItemInfo(
                null, "зелье радиации", Items.SPLASH_POTION, "Зелье Радиации", true,
                List.of(
                        new EffectRequirement(StatusEffects.POISON, 1),
                        new EffectRequirement(StatusEffects.WITHER, 1),
                        new EffectRequirement(StatusEffects.SLOWNESS, 2),
                        new EffectRequirement(StatusEffects.HUNGER, 4),
                        new EffectRequirement(StatusEffects.GLOWING, 0)
                )));

        itemConfig.put("snotvornoe", new ItemInfo(
                null, "снотворное", Items.SPLASH_POTION, "Снотворное", true,
                List.of(
                        new EffectRequirement(StatusEffects.WEAKNESS, 1),
                        new EffectRequirement(StatusEffects.MINING_FATIGUE, 1),
                        new EffectRequirement(StatusEffects.WITHER, 2),
                        new EffectRequirement(StatusEffects.BLINDNESS, 0)
                )));

        itemConfig.put("antiflight", new ItemInfo(
                "анти полет", Items.FIREWORK_STAR, "Анти полет"));

        itemConfig.put("expscroll", new ItemInfo(
                "свиток опыта", Items.FLOWER_BANNER_PATTERN, "Свиток опыта"));

        itemConfig.put("dtrap", new ItemInfo(
                "взрывная трапка", Items.PRISMARINE_SHARD, "Взрывная трапка"));

        itemConfig.put("trap_holy", new ItemInfo(
                "трапка", Items.POPPED_CHORUS_FRUIT, "Обычная трапка"));

        itemConfig.put("stan", new ItemInfo(
                "стан", Items.NETHER_STAR, "Стан"));

        itemConfig.put("ditem", new ItemInfo(
                "взрывная", Items.FIRE_CHARGE, "Взрывная штучка"));

        itemConfig.put("tikva", new ItemInfo(
                "светильник джека", Items.JACK_O_LANTERN, "Светильник Джека"));

        itemConfig.put("exp", new ItemInfo(
                "пузырь опыта", Items.EXPERIENCE_BOTTLE, "Пузырь опыта"));

        itemConfig.put("shulker1", new ItemInfo(
                "рюкзак (i уровень)", Items.PINK_SHULKER_BOX, "Рюкзак 1 уровня"));

        itemConfig.put("shulker2", new ItemInfo(
                "рюкзак (ii уровень)", Items.BLUE_SHULKER_BOX, "Рюкзак 2 уровня"));

        itemConfig.put("shulker3", new ItemInfo(
                "рюкзак (iii уровень)", Items.RED_SHULKER_BOX, "Рюкзак 3 уровня"));

        itemConfig.put("shulker4", new ItemInfo(
                "рюкзак (iv уровень)", Items.PINK_SHULKER_BOX, "Рюкзак 4 уровня"));

        itemConfig.keySet().forEach(key -> {
            lastKeyStates.put(key, false);
            keyPressedThisTick.put(key, false);
        });
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void activate() {
        potionQueue.clear();
        potionTimer.reset();
        lastKeyStates.replaceAll((k, v) -> false);
        keyPressedThisTick.replaceAll((k, v) -> false);
        actionState = ActionState.IDLE;
        originalSlot = -1;
        targetSlot = -1;
        originalSourceSlot = -1;
        pendingItemKey = null;
        stopMovementUntil = 0;
        keysOverridden = false;
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        lastKeyStates.replaceAll((k, v) -> false);
        keyPressedThisTick.replaceAll((k, v) -> false);
        potionQueue.clear();
        potionTimer.reset();
        actionState = ActionState.IDLE;
        originalSlot = -1;
        targetSlot = -1;
        originalSourceSlot = -1;
        pendingItemKey = null;
        stopMovementUntil = 0;
        if (keysOverridden) {
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
        }
        keysOverridden = false;
        movement.reset();
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private SwapSettings buildSettings() {
        return switch (swapMode.getSelected()) {
            case "Instant" -> SwapSettings.instant();
            default -> SwapSettings.legit();
        };
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private boolean isKeyPressed(int keyCode) {
        if (keyCode == -1) return false;
        long handle = mc.getWindow().getHandle();
        if (keyCode >= GLFW.GLFW_MOUSE_BUTTON_1 && keyCode <= GLFW.GLFW_MOUSE_BUTTON_8) {
            return GLFW.glfwGetMouseButton(handle, keyCode) == GLFW.GLFW_PRESS;
        }
        return GLFW.glfwGetKey(handle, keyCode) == GLFW.GLFW_PRESS;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void blockMovement() {
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        if (mc.player != null && mc.player.isSprinting()) {
            mc.player.setSprinting(false);
        }
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onRotationUpdate(RotationUpdateEvent e) {
        if (mc.currentScreen != null) return;

        boolean noMoveOrAction = System.currentTimeMillis() < stopMovementUntil || (actionState != ActionState.IDLE && actionState != ActionState.SPEEDING_UP);
        if (noMoveOrAction) {
            blockMovement();
        }

        processKeyBindings();

        if (actionState != ActionState.IDLE) {
            processItemAction();
        }

        processItemQueue();
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void processKeyBindings() {
        for (KeyBind bind : keyBindings) {
            String key = getKeyFromBinding(bind.setting.getName());
            if (key != null && bind.setting.getVisible().get()) {
                boolean currentKey = isKeyPressed(bind.setting.getKey());
                boolean wasPressedLastTick = lastKeyStates.getOrDefault(key, false);

                if (!currentKey && wasPressedLastTick) {
                    ItemInfo info = itemConfig.get(key);
                    if (info != null) {
                        Slot slot = findSlotByItem(info);
                        if (slot != null) {
                            ItemStack stack = slot.getStack();
                            if (!mc.player.getItemCooldownManager().isCoolingDown(stack)) {
                                if (!potionQueue.contains(key)) {
                                    potionQueue.add(key);
                                }
                            }
                        }
                    }
                }
                lastKeyStates.put(key, currentKey);
                keyPressedThisTick.put(key, currentKey);
            }
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void processItemQueue() {
        if (actionState == ActionState.IDLE && !potionQueue.isEmpty() && potionTimer.finished(150)) {
            String potionKey = potionQueue.remove(0);
            ItemInfo info = itemConfig.get(potionKey);
            if (info != null) {
                Slot slot = findSlotByItem(info);
                if (slot != null) {
                    ItemStack stack = slot.getStack();
                    if (!mc.player.getItemCooldownManager().isCoolingDown(stack)) {
                        startItemUse(slot, info);
                    }
                }
                potionTimer.reset();
            }
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void startItemUse(Slot slot, ItemInfo info) {
        originalSlot = mc.player.getInventory().getSelectedSlot();
        originalSourceSlot = slot.id;
        targetSlot = slot.id;
        pendingItemKey = info.displayName;

        boolean needsSwap = !(slot.id >= 0 && slot.id < 9) && !(slot.id >= 36 && slot.id < 45);

        long handle = mc.getWindow().getHandle();
        wasForwardPressed = GLFW.glfwGetKey(handle, mc.options.forwardKey.getDefaultKey().getCode()) == GLFW.GLFW_PRESS;
        wasBackPressed = GLFW.glfwGetKey(handle, mc.options.backKey.getDefaultKey().getCode()) == GLFW.GLFW_PRESS;
        wasLeftPressed = GLFW.glfwGetKey(handle, mc.options.leftKey.getDefaultKey().getCode()) == GLFW.GLFW_PRESS;
        wasRightPressed = GLFW.glfwGetKey(handle, mc.options.rightKey.getDefaultKey().getCode()) == GLFW.GLFW_PRESS;

        SwapSettings settings = buildSettings();

        if (needsSwap && settings.shouldStopMovement()) {
            actionState = ActionState.SLOWING_DOWN;
            actionTimer = System.currentTimeMillis();
            stopMovementUntil = System.currentTimeMillis() + settings.randomWaitStopDelay();
            keysOverridden = true;
            movement.saveState();
            movement.block();
        } else {
            actionState = ActionState.SWAP_TO_ITEM;
            actionTimer = System.currentTimeMillis();
            stopMovementUntil = System.currentTimeMillis() + 95;
            keysOverridden = true;
            blockMovement();
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void processItemAction() {
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - actionTimer;
        SwapSettings settings = buildSettings();

        switch (actionState) {
            case SLOWING_DOWN -> {
                blockMovement();
                if (elapsed > settings.randomPreStopDelay()) {
                    actionState = ActionState.WAITING_STOP;
                }
            }
            case WAITING_STOP -> {
                blockMovement();
                double velocityX = Math.abs(mc.player.getVelocity().x);
                double velocityZ = Math.abs(mc.player.getVelocity().z);
                if ((velocityX < settings.getVelocityThreshold() && velocityZ < settings.getVelocityThreshold()) || elapsed > settings.randomWaitStopDelay()) {
                    actionState = ActionState.SWAP_TO_ITEM;
                    actionTimer = currentTime;
                }
            }
            case SWAP_TO_ITEM -> {
                if (elapsed > settings.randomPreSwapDelay()) {
                    performSwapToItem();
                    actionState = ActionState.USE_ITEM;
                    actionTimer = currentTime;
                }
            }
            case USE_ITEM -> {
                if (elapsed > 40) {
                    performUseItem();
                    actionState = ActionState.SWAP_BACK;
                    actionTimer = currentTime;
                }
            }
            case SWAP_BACK -> {
                if (elapsed > settings.randomPostSwapDelay()) {
                    performSwapBack();
                    restoreKeyStates();
                    actionState = ActionState.SPEEDING_UP;
                    actionTimer = currentTime;
                }
            }
            case SPEEDING_UP -> {
                if (elapsed > settings.randomResumeDelay()) {
                    actionState = ActionState.IDLE;
                    originalSlot = -1;
                    targetSlot = -1;
                    originalSourceSlot = -1;
                    pendingItemKey = null;
                }
            }
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void performSwapToItem() {
        if (targetSlot >= 0 && targetSlot < 9) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(targetSlot));
            mc.player.getInventory().setSelectedSlot(targetSlot);
        } else if (targetSlot >= 36 && targetSlot < 45) {
            int hotbarSlot = targetSlot - 36;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
            mc.player.getInventory().setSelectedSlot(hotbarSlot);
            targetSlot = hotbarSlot;
        } else {
            int swapSlot = 8;
            InventoryUtils.click(targetSlot, swapSlot, SlotActionType.SWAP);
            targetSlot = swapSlot;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(swapSlot));
            mc.player.getInventory().setSelectedSlot(swapSlot);
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void performUseItem() {
        Angle angle = MathAngle.cameraAngle();
        mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, angle.getYaw(), angle.getPitch()));
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void performSwapBack() {
        boolean wasFromInventory = !(originalSourceSlot >= 0 && originalSourceSlot < 9) && !(originalSourceSlot >= 36 && originalSourceSlot < 45);

        if (wasFromInventory) {
            if (targetSlot >= 0 && targetSlot < 9) {
                InventoryUtils.click(originalSourceSlot, targetSlot, SlotActionType.SWAP);
            }
        } else {
            if (originalSourceSlot >= 36 && originalSourceSlot < 45) {
                int hotbarSlot = originalSourceSlot - 36;
                if (targetSlot != hotbarSlot) {
                    mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
                    mc.player.getInventory().setSelectedSlot(hotbarSlot);
                }
            } else if (originalSourceSlot >= 0 && originalSourceSlot < 9) {
                if (targetSlot != originalSourceSlot) {
                    mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(originalSourceSlot));
                    mc.player.getInventory().setSelectedSlot(originalSourceSlot);
                }
            }
        }

        if (mc.player.getInventory().getSelectedSlot() != originalSlot) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
            mc.player.getInventory().setSelectedSlot(originalSlot);
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void restoreKeyStates() {
        if (!keysOverridden) return;

        mc.options.forwardKey.setPressed(wasForwardPressed);
        mc.options.backKey.setPressed(wasBackPressed);
        mc.options.leftKey.setPressed(wasLeftPressed);
        mc.options.rightKey.setPressed(wasRightPressed);

        keysOverridden = false;
        movement.reset();
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        keyBindings.stream()
                .filter(bind -> PlayerInteractionHelper.isKey(bind.setting) && findSlotByBinding(bind) != null)
                .forEach(bind -> {
                    BlockPos playerPos = mc.player.getBlockPos();
                    Vec3d smooth = MathUtils.interpolate(Vec3d.of(BlockPos.ofFloored(mc.player.lastX, mc.player.lastY, mc.player.lastZ)), Vec3d.of(playerPos))
                            .subtract(Vec3d.of(playerPos));

                    int lineColor = mode.isSelected("FunTime") ? boxLineColor.getColor() : getDefaultLineColor();
                    int fillColor = mode.isSelected("FunTime") ? boxFillColor.getColor() : getDefaultFillColor();

                    switch (bind.setting.getName()) {
                        case "Трапка", "Обычная трапка" -> drawItemCube(playerPos, smooth, 1.99F, lineColor, fillColor);
                        case "Дезориентация", "Огненный смерч", "Явная пыль" -> Render3D.drawRadiusCircle(MathUtils.interpolate(mc.player), bind.distance, validDistance(bind.distance) ? ColorUtil.getFriendColor() : lineColor);
                        case "Взрывная штучка" -> Render3D.drawRadiusCircle(MathUtils.interpolate(mc.player), 5, validDistance(5) ? ColorUtil.getFriendColor() : lineColor);
                        case "Пласт" -> Render3D.drawPlastShape(playerPos, smooth, lineColor, fillColor);
                        case "Взрывная трапка" -> drawItemCube(playerPos, smooth, 3.99F, lineColor, fillColor);
                        case "Стан" -> drawItemCube(playerPos, smooth, 15.01F, lineColor, fillColor);
                        case "Снежок заморозка" -> Render3D.drawRadiusCircle(MathUtils.interpolate(mc.player), 7, validDistance(7) ? ColorUtil.getFriendColor() : lineColor);
                        case "Божья аура" -> Render3D.drawRadiusCircle(MathUtils.interpolate(mc.player), 2, validDistance(2) ? ColorUtil.getFriendColor() : lineColor);
                    }
                });
    }

    private int getDefaultLineColor() {
        return 0xFF822010;
    }

    private int getDefaultFillColor() {
        return 0x28822010;
    }

    private void drawItemCube(BlockPos playerPos, Vec3d smooth, float size, int lineColor, int fillColor) {
        Box box = new Box(playerPos.up()).offset(smooth).expand(size);
        boolean inBox = mc.world.getPlayers().stream()
                .anyMatch(player -> player != mc.player && box.intersects(player.getBoundingBox()) && !FriendUtils.isFriend(player));
        if (inBox) {
            Render3D.drawBoxWithCrossFull(box, ColorUtil.getFriendColor(), ColorUtil.multAlpha(ColorUtil.getFriendColor(), 0.15f), 2);
        } else {
            Render3D.drawBoxWithCrossFull(box, lineColor, fillColor, 2);
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private List<String> getLore(ItemStack stack) {
        List<String> lore = new ArrayList<>();
        if (stack == null || stack.isEmpty()) return lore;

        try {
            LoreComponent loreComponent = stack.get(DataComponentTypes.LORE);
            if (loreComponent != null) {
                for (Text text : loreComponent.lines()) {
                    String line = getCleanName(text);
                    if (!line.isEmpty()) {
                        lore.add(line);
                    }
                }
            }
        } catch (Exception ignored) {}

        return lore;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private boolean matchesLore(ItemStack stack, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return false;

        List<String> lore = getLore(stack);
        if (lore.isEmpty()) return false;

        String fullLore = String.join(" ", lore).toLowerCase();

        int matchCount = 0;
        for (String keyword : keywords) {
            if (fullLore.contains(keyword.toLowerCase())) {
                matchCount++;
            }
        }
        return matchCount >= Math.min(2, keywords.size());
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private Map<RegistryEntry<StatusEffect>, Integer> getPotionEffects(ItemStack stack) {
        Map<RegistryEntry<StatusEffect>, Integer> effects = new HashMap<>();
        if (stack == null || stack.isEmpty()) return effects;

        PotionContentsComponent potionContents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (potionContents == null) return effects;

        for (StatusEffectInstance effect : potionContents.customEffects()) {
            effects.put(effect.getEffectType(), effect.getAmplifier());
        }

        return effects;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private boolean matchesPotionEffects(ItemStack stack, List<EffectRequirement> requirements) {
        if (requirements == null || requirements.isEmpty()) return false;
        if (stack.getItem() != Items.SPLASH_POTION && stack.getItem() != Items.LINGERING_POTION) return false;

        Map<RegistryEntry<StatusEffect>, Integer> effects = getPotionEffects(stack);
        if (effects.isEmpty()) return false;

        int matchCount = 0;
        for (EffectRequirement req : requirements) {
            Integer amplifier = effects.get(req.effect);
            if (amplifier != null && amplifier >= req.minAmplifier) {
                matchCount++;
            }
        }

        return matchCount >= Math.min(2, requirements.size());
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private Slot findSlotByItem(ItemInfo info) {
        if (mode.isSelected("FunTime") && info.funTimeOnly) {
            if (info.effectRequirements != null && !info.effectRequirements.isEmpty()) {
                Slot effectMatch = InventoryUtils.findSlot(s -> {
                    ItemStack stack = s.getStack();
                    if (stack.isEmpty() || !stack.getItem().equals(info.item)) return false;
                    return matchesPotionEffects(stack, info.effectRequirements);
                });

                if (effectMatch != null) {
                    return effectMatch;
                }
            }

            if (info.loreKeywords != null && !info.loreKeywords.isEmpty()) {
                Slot loreMatch = InventoryUtils.findSlot(s -> {
                    ItemStack stack = s.getStack();
                    if (stack.isEmpty() || !stack.getItem().equals(info.item)) return false;
                    return matchesLore(stack, info.loreKeywords);
                });

                if (loreMatch != null) {
                    return loreMatch;
                }
            }

            if (info.nameFallback != null && !info.nameFallback.isEmpty()) {
                return InventoryUtils.findSlot(s -> {
                    ItemStack stack = s.getStack();
                    if (stack.isEmpty() || !stack.getItem().equals(info.item)) return false;
                    List<String> lore = getLore(stack);
                    if (!lore.isEmpty()) {
                        String fullLore = String.join(" ", lore).toLowerCase();
                        return fullLore.contains(info.nameFallback.toLowerCase());
                    }
                    return getCleanName(stack.getName()).contains(info.nameFallback.toLowerCase());
                });
            }

            return null;
        }

        if (info.nameFallback != null && !info.nameFallback.isEmpty()) {
            return InventoryUtils.findSlot(s -> {
                ItemStack stack = s.getStack();
                if (stack.isEmpty() || !stack.getItem().equals(info.item)) return false;
                return getCleanName(stack.getName()).contains(info.nameFallback.toLowerCase());
            });
        }

        return null;
    }

    private Slot findSlotByBinding(KeyBind bind) {
        String key = getKeyFromBinding(bind.setting.getName());
        if (key != null) {
            ItemInfo info = itemConfig.get(key);
            if (info != null) {
                return findSlotByItem(info);
            }
        }
        return InventoryUtils.findSlot(s -> s.getStack().getItem().equals(bind.item));
    }

    private String getCleanName(Text name) {
        return name.getString().toLowerCase().replaceAll("§[0-9a-fk-or]", "");
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private String getKeyFromBinding(String bindingName) {
        return switch (bindingName) {
            case "Анти полет" -> "antiflight";
            case "Свиток опыта" -> "expscroll";
            case "Взрывная трапка" -> "dtrap";
            case "Обычная трапка" -> "trap_holy";
            case "Стан" -> "stan";
            case "Взрывная штучка" -> "ditem";
            case "Снежок заморозка" -> "snow";
            case "Божья аура" -> "bojaura";
            case "Трапка" -> "trap";
            case "Пласт" -> "plast";
            case "Явная пыль" -> "sugar";
            case "Огненный смерч" -> "fireSwirl";
            case "Дезориентация" -> "disorientation";
            case "Светильник Джека" -> "tikva";
            case "Пузырь опыта" -> "exp";
            case "Рюкзак 1 уровня" -> "shulker1";
            case "Рюкзак 2 уровня" -> "shulker2";
            case "Рюкзак 3 уровня" -> "shulker3";
            case "Рюкзак 4 уровня" -> "shulker4";
            case "Хлопушка" -> "hlopushka";
            case "Святая вода" -> "holywater";
            case "Зелье Гнева" -> "gnev";
            case "Зелье Палладина" -> "paladin";
            case "Зелье Ассасина" -> "assassin";
            case "Зелье Радиации" -> "radiation";
            case "Снотворное" -> "snotvornoe";
            default -> null;
        };
    }

    private boolean validDistance(float dist) {
        return dist == 0 || mc.world.getPlayers().stream()
                .anyMatch(p -> p != mc.player && !FriendUtils.isFriend(p) && mc.player.distanceTo(p) <= dist);
    }

    public BindSetting getSetting(String name) {
        return keyBindings.stream()
                .filter(bind -> bind.setting().getName().equals(name))
                .map(ServerHelper.KeyBind::setting)
                .findFirst()
                .orElse(null);
    }

    public record KeyBind(Item item, BindSetting setting, float distance) {}
}