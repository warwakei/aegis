package fun.aegis.features.impl.combat;

import fun.aegis.events.player.MotionEvent;
import fun.aegis.events.render.DrawEvent;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.BooleanSetting;
import fun.aegis.features.module.setting.implement.MultiSelectSetting;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.utils.client.Instance;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.features.aura.target.TargetFinder;
import fun.aegis.utils.features.aura.utils.MathAngle;
import fun.aegis.utils.features.aura.warp.Turns;
import fun.aegis.utils.features.aura.warp.TurnsConfig;
import fun.aegis.utils.features.aura.warp.TurnsConnection;
import fun.aegis.utils.math.task.TaskPriority;
import fun.aegis.utils.math.time.StopWatch;
import fun.aegis.utils.display.shape.ShapeProperties;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.util.Comparator;

public class AimBot extends Module {

     public static AimBot getInstance() {
          return Instance.get(AimBot.class);
     }

     SliderSettings range = new SliderSettings("Range from player", "Дистанция захвата")
               .range(1.0f, 10.0f).setValue(4.5f);

     SliderSettings fov = new SliderSettings("FOV", "Угол обзора аимбота")
               .range(1.0f, 360.0f).setValue(90.0f);

     BooleanSetting wallCheck = new BooleanSetting("Wall Check", "Проверка на стены")
               .setValue(true);

     SelectSetting priority = new SelectSetting("Priority", "Приоритет выбора цели")
               .value("Distance", "FOV", "Health").selected("FOV");

     MultiSelectSetting aimPoints = new MultiSelectSetting("Aim Points", "Точки наводки")
               .value("Head", "Chest", "Pelvis", "Feet")
               .selected("Chest");

     SliderSettings smooth = new SliderSettings("Smooth", "Плавность доводки")
               .range(1.0f, 20.0f).setValue(5.0f);

     SliderSettings speedH = new SliderSettings("Speed horizontal", "Скорость горизонтальная")
               .range(1.0f, 180.0f).setValue(45.0f);

     SliderSettings speedV = new SliderSettings("Speed vertical", "Скорость вертикальная")
               .range(1.0f, 180.0f).setValue(45.0f);

     BooleanSetting silent = new BooleanSetting("Silent", "Сайлент аим (невидимая наводка)")
               .setValue(false);

     SliderSettings flickDelay = new SliderSettings("Flick Delay", "Задержка между фликами (мс)")
               .range(0f, 1000f).setValue(0f).visible(silent::isValue);

     BooleanSetting organic = new BooleanSetting("Organic", "Органическая рандомизация")
               .setValue(true);

     BooleanSetting fovCircle = new BooleanSetting("FOV Circle", "Отображать круг FOV")
               .setValue(true);

     MultiSelectSetting targets = new MultiSelectSetting("Targets", "Кого атаковать")
               .value("Players", "Mobs", "Animals", "Armor Stand")
               .selected("Players");

     private final TargetFinder targetSelector = new TargetFinder();
     private LivingEntity target;
     private Turns originalRotation;
     private boolean isTargetLocked = false;
     private final StopWatch flickTimer = new StopWatch();

     public AimBot() {
          super("AimBot", ModuleCategory.COMBAT);
          setup(range, fov, wallCheck, priority, aimPoints, smooth, speedH, speedV, silent, flickDelay, organic,
                    fovCircle, targets);
     }

     private double getTargetValue(LivingEntity entity) {
          return switch (priority.getSelected()) {
               case "Distance" -> entity.distanceTo(mc.player);
               case "Health" -> entity.getHealth();
               case "FOV" -> {
                    Turns angle = MathAngle.calculateAngle(entity.getEyePos());
                    yield Math.abs(MathHelper.wrapDegrees(angle.getYaw() - mc.player.getYaw()));
               }
               default -> 0;
          };
     }

     private Vec3d getAimPos(LivingEntity entity) {
          double heightFactor = 0.75;
          if (!aimPoints.getSelected().isEmpty()) {
               int index = entity.getId() % aimPoints.getSelected().size();
               String point = aimPoints.getSelected().get(index);
               heightFactor = switch (point) {
                    case "Head" -> 0.85;
                    case "Chest" -> 0.70;
                    case "Pelvis" -> 0.45;
                    case "Feet" -> 0.15;
                    default -> 0.75;
               };
          }

          Vec3d basePos = entity.getPos().add(0, entity.getHeight() * heightFactor, 0);

          // Organic Randomization: Золотое Сечение (Aperiodic Motion)
          if (organic.isValue() || silent.isValue()) {
               double phi = 1.618033988749895;
               float time = (float) (System.currentTimeMillis() % 100000L) / 3000f;

               // Используем иррациональные частоты (фи и фи^2), чтобы прицел НИКОГДА не шел по
               // одному кругу
               double offsetX = Math.sin(time * phi) * 0.12;
               double offsetZ = Math.cos(time * (phi * phi)) * 0.12;
               double offsetY = Math.sin(time * (phi / 2.0)) * 0.08;

               return basePos.add(offsetX, offsetY, offsetZ);
          }

          return basePos;
     }

     @EventHandler
     public void onMotion(MotionEvent e) {
          if (mc.player == null || mc.world == null)
               return;

          targetSelector.searchTargets(mc.world.getEntities(), range.getValue(), fov.getValue(), !wallCheck.isValue());
          TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(targets.getSelected());

          target = targetSelector.getPotentialTargets()
                    .filter(filter::isValid)
                    .filter(entity -> !wallCheck.isValue() || isVisible(entity))
                    .min(Comparator.comparingDouble(this::getTargetValue))
                    .orElse(null);

          if (target != null) {
               Vec3d aimPos = getAimPos(target);
               Turns targetAngle = MathAngle.calculateAngle(aimPos);

               // Легитный джиттер
               float time = (float) (System.currentTimeMillis() % 1000000L);
               float randX = (float) (Math.sin(time / 80D) * 1.2) + (float) (Math.cos(time / 50D) * 1.5);
               float randY = (float) (Math.sin(time / 120D) * 1.5) + (float) (Math.cos(time / 60D) * 1.2);
               targetAngle.setYaw(targetAngle.getYaw() + randX);
               targetAngle.setPitch(targetAngle.getPitch() + randY);

               if (silent.isValue()) {
                    if (flickTimer.finished((long) flickDelay.getValue())) {
                         originalRotation = new Turns(mc.player.getYaw(), mc.player.getPitch());
                         Turns result = targetAngle.adjustSensitivity();
                         TurnsConnection.INSTANCE.rotateTo(result, 1, TurnsConfig.DEFAULT,
                                   TaskPriority.HIGH_IMPORTANCE_1, this);
                         mc.player.setYaw(result.getYaw());
                         mc.player.setPitch(result.getPitch());
                         flickTimer.reset();
                    }
               } else {
                    handleRotation(targetAngle, smooth.getValue(), speedH.getValue(), speedV.getValue());
               }
               isTargetLocked = true;
          } else {
               if (isTargetLocked && silent.isValue() && originalRotation != null) {
                    isTargetLocked = false;
               } else {
                    isTargetLocked = false;
                    originalRotation = new Turns(mc.player.getYaw(), mc.player.getPitch());
               }
          }
     }

     @EventHandler
     public void onPostMotion(fun.aegis.events.player.PostMotionEvent e) {
          if (mc.player == null)
               return;
          if (target != null && silent.isValue() && originalRotation != null) {
               mc.player.setYaw(originalRotation.getYaw());
               mc.player.setPitch(originalRotation.getPitch());
          }
     }

     @EventHandler
     public void onDraw(DrawEvent e) {
          if (!fovCircle.isValue() || mc.player == null)
               return;

          float radius = (float) (fov.getValue()
                    * (window.getScaledWidth() / mc.options.getFov().getValue().doubleValue()) / 2.0);
          float centerX = window.getScaledWidth() / 2f;
          float centerY = window.getScaledHeight() / 2f;

          arc.render(ShapeProperties
                    .create(e.getDrawContext().getMatrices(), centerX - radius, centerY - radius, radius * 2,
                              radius * 2)
                    .thickness(1.5f).start(0).end(360).color(ColorAssist.getClientColor()).build());
     }

     private boolean isVisible(LivingEntity entity) {
          Vec3d start = mc.player.getEyePos();
          Vec3d end = getAimPos(entity);
          BlockHitResult result = mc.world.raycast(new net.minecraft.world.RaycastContext(start, end,
                    net.minecraft.world.RaycastContext.ShapeType.OUTLINE,
                    net.minecraft.world.RaycastContext.FluidHandling.NONE, mc.player));
          return result.getType() == HitResult.Type.MISS;
     }

     private void handleRotation(Turns targetAngle, float smoothVal, float hSpeed, float vSpeed) {
          Turns current = new Turns(mc.player.getYaw(), mc.player.getPitch());
          Turns delta = MathAngle.calculateDelta(current, targetAngle);

          if (Math.abs(delta.getYaw()) < 0.05f && Math.abs(delta.getPitch()) < 0.05f)
               return;

          float strength = 1.0f / Math.max(1.0f, smoothVal);

          float yawChange = delta.getYaw() * strength;
          float pitchChange = delta.getPitch() * strength;

          yawChange = MathHelper.clamp(yawChange, -hSpeed, hSpeed);
          pitchChange = MathHelper.clamp(pitchChange, -vSpeed, vSpeed);

          Turns result = new Turns(mc.player.getYaw() + yawChange,
                    MathHelper.clamp(mc.player.getPitch() + pitchChange, -90f, 90f)).adjustSensitivity();

          TurnsConnection.INSTANCE.rotateTo(result, 1, TurnsConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_1, this);
          mc.player.setYaw(result.getYaw());
          mc.player.setPitch(result.getPitch());
     }
}
