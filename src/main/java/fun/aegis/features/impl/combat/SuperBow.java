//package fun.aegis.common.modules.combat;
//
//import fun.aegis.common.managers.api.module.Module;
//import fun.aegis.common.managers.api.module.ModuleCategory;
//import fun.aegis.common.managers.api.module.setting.implement.ValueSetting;
//import fun.aegis.common.managers.api.module.setting.implement.BooleanSetting;
//import fun.aegis.common.managers.event.EventHandler;
//import fun.aegis.utils.events.player.TickEvent;
//import fun.aegis.utils.entity.PlayerInventoryUtil;
//import fun.aegis.util.math.stopwatch.StopWatch;
//import lombok.AccessLevel;
//import lombok.Getter;
//import lombok.experimental.FieldDefaults;
//import net.minecraft.entity.Entity;
//import net.minecraft.entity.LivingEntity;
//import net.minecraft.entity.projectile.PersistentProjectileEntity;
//import net.minecraft.item.BowItem;
//import net.minecraft.item.ItemStack;
//import net.minecraft.item.Items;
//import net.minecraft.util.Hand;
//import net.minecraft.util.math.Box;
//import net.minecraft.util.math.Vec3d;
//import java.util.Comparator;
//
//@Getter
//@FieldDefaults(level = AccessLevel.PRIVATE)
//public class SuperBow extends Module {
//    ValueSetting powerMultiplier = new ValueSetting("Множитель силы", "Увеличивает силу выстрела (velocity multiplier для имитации fall speed)")
//            .setValue(1.05F).range(1.0F, 1.1F);
//    ValueSetting delay = new ValueSetting("Задержка выстрела (тики)", "Задержка между выстрелами для избежания флагов")
//            .setValue(20).range(10, 60);
//    BooleanSetting justShot = new BooleanSetting("JustShot", "Internal flag");
//    StopWatch stopWatch = new StopWatch();
//
//    public SuperBow() {
//        super("SuperBow", "Имитирует critical bow shot с высоты (8+ сердец урона) без флагов Grim", ModuleCategory.COMBAT);
//        setup(powerMultiplier, delay);
//        justShot.setValue(false);
//    }
//
//    @EventHandler
//    public void onTick(TickEvent event) {
//        if (!mc.player.isUsingItem() || mc.player.getActiveItem().getItem() != Items.BOW) {
//            return;
//        }
//
//        ItemStack bow = mc.player.getActiveItem();
//        int useTicks = bow.getMaxUseTime(mc.player) - mc.player.getItemUseTime();
//        if (useTicks >= 20 && stopWatch.every((int) delay.getValue() * 50L)) {
//            if (PlayerInventoryUtil.hasItem(Items.ARROW)) {
//                mc.player.stopUsingItem();
//                justShot.setValue(true);
//                stopWatch.reset();
//            }
//        }
//
//        if (justShot.isValue()) {
//            PersistentProjectileEntity arrow = findLastArrow(mc.player);
//            if (arrow != null) {
//                arrow.setCritical(true);
//                arrow.setDamage(arrow.getDamage() * 1.5);
//                Vec3d vel = arrow.getVelocity();
//                arrow.setVelocity(vel.x * powerMultiplier.getValue(), vel.y * powerMultiplier.getValue() - 0.05, vel.z * powerMultiplier.getValue());
//            }
//            justShot.setValue(false);
//        }
//    }
//
//    private PersistentProjectileEntity findLastArrow(LivingEntity shooter) {
//        return mc.world.getEntitiesByClass(PersistentProjectileEntity.class, shooter.getBoundingBox().expand(10.0), a -> a.getOwner() == shooter)
//                .stream().max(Comparator.comparingInt(Entity::getId))
//                .orElse(null);
//    }
//}
