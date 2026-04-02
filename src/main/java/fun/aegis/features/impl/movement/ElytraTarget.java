package fun.aegis.features.impl.movement;

import fun.aegis.display.hud.Notifications;
import fun.aegis.events.keyboard.KeyEvent;
import fun.aegis.events.player.AttackEvent;
import fun.aegis.events.render.WorldRenderEvent;
import fun.aegis.features.impl.combat.Aura;
import fun.aegis.features.impl.render.Hud;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.Setting;
import fun.aegis.features.module.setting.implement.BindSetting;
import fun.aegis.features.module.setting.implement.BooleanSetting;
import fun.aegis.features.module.setting.implement.ColorSetting;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.utils.client.Instance;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.client.sound.SoundManager;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.display.geometry.Render3D;
import java.awt.Color;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class ElytraTarget extends Module {
    public static ElytraTarget getInstance() {
        return (ElytraTarget) Instance.get(ElytraTarget.class);
    }

    public SliderSettings elytraFindRange = (new SliderSettings("ElytraFindRange", "Elytra Find Range"))
            .setValue(32.0F).range(6.0F, 64.0F);

    public SliderSettings elytraForward = (new SliderSettings("ElytraForward", "Elytra Forward"))
            .setValue(3.0F).range(0.0F, 6.0F);

    private final SelectSetting mode = (new SelectSetting("Mode", "ElytraTarget mode"))
            .value(new String[]{"Default", "HitRun"}).selected("Default");

    public final SliderSettings hitRunDistance = (new SliderSettings("HitRunDistance", "Hit&Run"))
            .setValue(6.0F).range(2.0F, 16.0F).visible(() -> this.mode.isSelected("HitRun"));

    final BindSetting forward = new BindSetting("Forward", "Forward key bind for elytra target");

    public static boolean shouldElytraTarget = false;

    private final BooleanSetting elytraPredictEsp = (new BooleanSetting("ElytraPredictEsp", "ESP"))
            .setValue(true);

    private final ColorSetting elytraPredictColor = (new ColorSetting("ElytraPredictColor", "ESP"))
            .value((new Color(255, 0, 0, 180)).getRGB())
            .visible(() -> this.elytraPredictEsp.isValue());

    private Vec3d lastPredictedPos = null;

    public ElytraTarget() {
        super("ElytraTarget", "Elytra Target", ModuleCategory.MOVEMENT);
        setup(new Setting[]{this.elytraFindRange, this.elytraForward, this.mode, this.hitRunDistance, this.forward, this.elytraPredictEsp, this.elytraPredictColor});
    }

    @EventHandler
    private void onEventKey(KeyEvent e) {
        if (e.isKeyDown(this.forward.getKey())) {
            float volume = Hud.getInstance().getModuleVolume();
            shouldElytraTarget = !shouldElytraTarget;
            Notifications.getInstance().addList("Elytra Forward " + (shouldElytraTarget ? "enabled!" : "disabled"), 1500L, null);
            SoundManager.playSound(shouldElytraTarget ? SoundManager.ENABLE_MODULE : SoundManager.DISABLE_MODULE, volume, 1.0F);
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (!shouldElytraTarget || mc == null || mc.world == null || mc.player == null)
            return;
        if (!mc.player.isGliding())
            return;
        if (!mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA))
            return;
        Aura aura = Aura.getInstance();
        if (aura == null || !aura.isState())
            return;
        LivingEntity target = aura.getTarget();
        if (target == null || target.isDead())
            return;
        if (mc.player.distanceTo(target) > this.elytraFindRange.getValue())
            return;
        Vec3d predictedPos = predictPosition(target);
        if (predictedPos == null)
            return;
        if (this.lastPredictedPos == null) {
            this.lastPredictedPos = predictedPos;
        } else {
            double smooth = 0.4D;
            this.lastPredictedPos = this.lastPredictedPos.add(predictedPos.subtract(this.lastPredictedPos).multiply(smooth));
        }
        if (this.elytraPredictEsp.isValue())
            renderPredictedHitbox(target, this.lastPredictedPos);
    }

    private Vec3d predictPosition(Entity target) {
        if (target == null)
            return null;
        Vec3d velocity = target.getVelocity();
        int predictTicks = (int) this.elytraForward.getValue();
        return target.getPos().add(velocity.multiply(predictTicks));
    }

    private void renderPredictedHitbox(LivingEntity target, Vec3d pos) {
        Box realBox = target.getBoundingBox();
        Vec3d delta = pos.subtract(target.getPos());
        Box box = realBox.offset(delta);
        int color = ColorAssist.multAlpha(this.elytraPredictColor.getColor(), 0.5F);
        Render3D.drawBox(box, color, 1.5F);
    }

    @EventHandler
    public void onAttack(AttackEvent e) {
        LivingEntity target;
        if (!this.mode.isSelected("HitRun"))
            return;
        if (!shouldElytraTarget)
            return;
        if (mc == null || mc.world == null || mc.player == null)
            return;
        if (!mc.player.isGliding())
            return;
        if (!mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA))
            return;
        Entity entity = e.getEntity();
        if (entity instanceof LivingEntity) {
            target = (LivingEntity) entity;
        } else {
            return;
        }
        if (mc.player.distanceTo(target) > this.elytraFindRange.getValue())
            return;
        Vec3d playerPos = mc.player.getPos();
        Vec3d targetPos = target.getPos();
        Vec3d dir = playerPos.subtract(targetPos);
        dir = new Vec3d(dir.x, 0.0D, dir.z);
        if (dir.length() < 1.0E-4D)
            return;
        dir = dir.normalize();
        double distance = this.hitRunDistance.getValue();
        Vec3d desiredOffset = dir.multiply(distance);
        Vec3d futurePos = playerPos.add(desiredOffset);
        Box futureBox = mc.player.getBoundingBox().offset(desiredOffset);
        boolean blocked = !mc.world.getBlockCollisions(mc.player, futureBox).iterator().hasNext();
        if (blocked)
            dir = dir.multiply(-1.0D);
        Vec3d impulse = dir.multiply(1.0D);
        mc.player.addVelocity(impulse.x, 0.0D, impulse.z);
    }
}
