package fun.aegis.utils.display;

import fun.aegis.features.impl.combat.Aura;
import fun.aegis.features.impl.movement.Speed;
import fun.aegis.features.impl.movement.TargetStrafe;
import fun.aegis.common.animation.Animation;
import fun.aegis.common.animation.Direction;
import fun.aegis.common.animation.implement.Decelerate;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.display.geometry.Render2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;

public class ComboEffect {
     private static final Animation animation = new Decelerate().setMs(200).setValue(1);
     private static boolean wasActive = false;
     private static final MinecraftClient mc = MinecraftClient.getInstance();

     public static void update() {
          if (mc.player == null || mc.world == null)
               return;

          boolean isAura = Aura.getInstance().isState();
          boolean isStrafe = TargetStrafe.getInstance().isState();
          boolean isGrimSpeed = Speed.getInstance().isState() && Speed.getInstance().getMode().isSelected("Grim");

          boolean isActive = isAura && isStrafe && isGrimSpeed;

          if (isActive && !wasActive) {
               animation.setDirection(Direction.FORWARDS);
               animation.reset();
          }

          if (animation.isFinished(Direction.FORWARDS)) {
               animation.setDirection(Direction.BACKWARDS);
          }

          wasActive = isActive;
     }

     public static void draw(DrawContext context) {
          update();
          float alpha = animation.getOutput().floatValue();
          if (alpha > 0.001f) {
               int width = mc.getWindow().getScaledWidth();
               int height = mc.getWindow().getScaledHeight();
               // Золотое свечение (255, 215, 0)
               int color = ColorAssist.getColor(255, 215, 0, (int) (alpha * 60));
               Render2D.drawQuad(0, 0, width, height, color);
          }
     }
}
