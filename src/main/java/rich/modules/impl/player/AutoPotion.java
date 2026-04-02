package rich.modules.impl.player;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import rich.events.api.EventHandler;
import rich.events.impl.RotationUpdateEvent;
import rich.events.impl.TickEvent;
import rich.events.api.types.EventType;
import rich.mixin.ClientWorldAccessor;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.modules.impl.combat.aura.AngleConfig;
import rich.modules.impl.combat.aura.impl.LinearConstructor;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.MultiSelectSetting;
import rich.util.math.TaskPriority;
import rich.util.timer.StopWatch;

@SuppressWarnings("all")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoPotion extends ModuleStructure {

    final BooleanSetting autoOff = new BooleanSetting("Авто отключение", "Автоматически выключать модуль после использования")
            .setValue(false);

    final MultiSelectSetting potions = new MultiSelectSetting("Бросать", "Выберите зелья для автоброса")
            .value("Силу", "Скорость", "Огнестойкость")
            .selected("Силу", "Скорость");

    final StopWatch timer = new StopWatch();

    boolean spoofed = false;
    boolean isActivePotion = false;
    int rotationTicks = 0;
    int selectedSlot = -1;
    final float THROW_PITCH = 90f;
    final int ROTATION_WAIT_TICKS = 2;

    public AutoPotion() {
        super("AutoPotion", ModuleCategory.PLAYER);
        settings(potions, autoOff);
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        isActivePotion = false;
        spoofed = false;
        rotationTicks = 0;
        selectedSlot = -1;
        AngleConnection.INSTANCE.startReturning();
    }

    private enum PotionType {
        STRENGTH(StatusEffects.STRENGTH, "Силу"),
        SPEED(StatusEffects.SPEED, "Скорость"),
        FIRE_RESISTANCE(StatusEffects.FIRE_RESISTANCE, "Огнестойкость");

        final RegistryEntry<StatusEffect> effect;
        final String settingName;

        PotionType(RegistryEntry<StatusEffect> effect, String settingName) {
            this.effect = effect;
            this.settingName = settingName;
        }

        public boolean isEnabled(AutoPotion module) {
            return module.potions.isSelected(this.settingName);
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private int findPotionSlot(PotionType type) {
        if (mc.player == null) return -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);

            if (stack.isOf(Items.SPLASH_POTION)) {
                PotionContentsComponent potionComponent = stack.get(DataComponentTypes.POTION_CONTENTS);
                if (potionComponent != null) {
                    for (StatusEffectInstance effect : potionComponent.getEffects()) {
                        if (effect.getEffectType() == type.effect) {
                            return i;
                        }
                    }
                }
            }
        }
        return -1;
    }

    private boolean hasEffect(RegistryEntry<StatusEffect> effect) {
        return mc.player != null && mc.player.hasStatusEffect(effect);
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private boolean canBuff(PotionType type) {
        if (hasEffect(type.effect)) return false;
        return type.isEnabled(this) && findPotionSlot(type) != -1;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private boolean canBuff() {
        if (mc.player == null || mc.world == null) return false;

        return (canBuff(PotionType.STRENGTH) || canBuff(PotionType.SPEED) || canBuff(PotionType.FIRE_RESISTANCE))
                && mc.player.isOnGround()
                && timer.finished(500);
    }

    private boolean isActive() {
        return isActivePotion || canBuff(PotionType.STRENGTH) || canBuff(PotionType.SPEED) || canBuff(PotionType.FIRE_RESISTANCE);
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private boolean shouldThrow() {
        if (mc.player == null || mc.world == null) return false;

        return isActive()
                && canBuff()
                && mc.world.getBlockState(mc.player.getBlockPos().down()).getBlock() != Blocks.AIR;
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onRotationUpdate(RotationUpdateEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (event.getType() == EventType.PRE) {
            if (shouldThrow() || spoofed) {
                performRotation();
            }
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void performRotation() {
        Angle throwAngle = new Angle(mc.player.getYaw(), THROW_PITCH);
        AngleConfig config = new AngleConfig(new LinearConstructor(), true, true);

        AngleConnection.INSTANCE.rotateTo(
                throwAngle,
                3,
                config,
                TaskPriority.HIGH_IMPORTANCE_1,
                this
        );

        if (!spoofed) {
            spoofed = true;
            isActivePotion = true;
            rotationTicks = 0;
            selectedSlot = mc.player.getInventory().getSelectedSlot();
        }
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (isActivePotion && !shouldThrow() && !spoofed) {
            isActivePotion = false;
            if (autoOff.isValue()) {
                setState(false);
            }
        }

        if (spoofed) {
            processThrow();
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void processThrow() {
        rotationTicks++;

        Angle currentRotation = AngleConnection.INSTANCE.getRotation();
        boolean rotationReady = currentRotation != null && currentRotation.getPitch() >= 80f;
        boolean waitedEnough = rotationTicks >= ROTATION_WAIT_TICKS;

        if (rotationReady && waitedEnough) {
            boolean threwAny = false;

            if (canBuff(PotionType.STRENGTH)) {
                throwPotion(PotionType.STRENGTH);
                threwAny = true;
            }
            if (canBuff(PotionType.SPEED)) {
                throwPotion(PotionType.SPEED);
                threwAny = true;
            }
            if (canBuff(PotionType.FIRE_RESISTANCE)) {
                throwPotion(PotionType.FIRE_RESISTANCE);
                threwAny = true;
            }

            if (selectedSlot != -1) {
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(selectedSlot));
            }

            timer.reset();
            spoofed = false;
            rotationTicks = 0;
            isActivePotion = false;

            if (autoOff.isValue() || !threwAny) {
                setState(false);
            }
        }

        if (rotationTicks > 10) {
            resetThrowState();
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void resetThrowState() {
        spoofed = false;
        rotationTicks = 0;
        isActivePotion = false;
        if (selectedSlot != -1) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(selectedSlot));
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void throwPotion(PotionType type) {
        if (!type.isEnabled(this) || hasEffect(type.effect)) return;
        if (mc.player == null || mc.player.networkHandler == null) return;

        int slot = findPotionSlot(type);
        if (slot == -1) return;

        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));

        sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), THROW_PITCH));
    }

    private void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        if (mc.player == null || mc.player.networkHandler == null || mc.world == null) return;

        try {
            ClientWorldAccessor worldAccessor = (ClientWorldAccessor) mc.world;
            PendingUpdateManager pendingUpdateManager = worldAccessor.getPendingUpdateManager().incrementSequence();

            int sequence = pendingUpdateManager.getSequence();
            mc.player.networkHandler.sendPacket(packetCreator.predict(sequence));

            pendingUpdateManager.close();
        } catch (Exception e) {
            mc.player.networkHandler.sendPacket(packetCreator.predict(0));
        }
    }
}