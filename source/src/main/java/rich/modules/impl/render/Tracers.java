package rich.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import rich.events.api.EventHandler;
import rich.events.impl.WorldRenderEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.ColorSetting;
import rich.modules.module.setting.implement.SelectSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.ColorUtil;
import rich.util.render.AnimatedColorHelper;
import rich.util.render.Render3D;
import rich.util.repository.friend.FriendUtils;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Tracers extends ModuleStructure {

    final List<PlayerEntity> players = new ArrayList<>();

    public SelectSetting colorMode = new SelectSetting("Режим цвета", "Как отображать линии")
            .value("Один цвет", "Градиент")
            .selected("Один цвет");

    public ColorSetting staticColor = new ColorSetting("Цвет", "Цвет линии")
            .value(0xFFFFFFFF)
            .visible(() -> colorMode.getSelected().equals("Один цвет"));

    public ColorSetting gradientStart = new ColorSetting("Градиент - Начало", "Начальный цвет градиента")
            .value(0xFF4488FF)
            .visible(() -> colorMode.getSelected().equals("Градиент"));

    public ColorSetting gradientEnd = new ColorSetting("Градиент - Конец", "Конечный цвет градиента")
            .value(0xFF8844FF)
            .visible(() -> colorMode.getSelected().equals("Градиент"));

    public SelectSetting animationMode = new SelectSetting("Анимация", "Режим анимации цвета")
            .value("Нет", "Ping-Pong", "Круговая", "Пульсация", "Радуга")
            .selected("Нет")
            .visible(() -> colorMode.getSelected().equals("Градиент"));

    public SliderSettings animationSpeed = new SliderSettings("Скорость анимации", "Скорость анимации цвета")
            .range(0.5f, 5.0f).setValue(2.0f)
            .visible(() -> !animationMode.getSelected().equals("Нет"));

    public SliderSettings lineWidth = new SliderSettings("Толщина линии", "Толщина линии Tracers")
            .range(0.5f, 3.0f).setValue(1.5f);

    public SliderSettings lineAlpha = new SliderSettings("Прозрачность", "Прозрачность линии")
            .range(0.1f, 1.0f).setValue(0.8f);

    public BooleanSetting showFriends = new BooleanSetting("Друзья", "Показывать линии к друзьям")
            .setValue(true);

    public BooleanSetting showInvisible = new BooleanSetting("Невидимые", "Показывать невидимых игроков")
            .setValue(false);

    public SelectSetting targetMode = new SelectSetting("Режим цели", "К кому рисовать линии")
            .value("Все игроки", "Только враги", "Только друзья")
            .selected("Все игроки");

    public BooleanSetting glowEffect = new BooleanSetting("Glow эффект", "Добавить свечение к линиям")
            .setValue(true);

    public SliderSettings glowSize = new SliderSettings("Размер свечения", "Интенсивность glow эффекта")
            .range(1.0f, 10.0f).setValue(4.0f)
            .visible(() -> glowEffect.isValue());

    public SliderSettings fadeDistance = new SliderSettings("Fade дистанция", "Начинать fade на этой дистанции")
            .range(50.0f, 200.0f).setValue(150.0f);

    public BooleanSetting drawFromCrosshair = new BooleanSetting("От прицела", "Рисовать линии от перекрестия")
            .setValue(false);

    public Tracers() {
        super("Tracers", "Линии к игрокам от прицела", ModuleCategory.RENDER);
        settings(colorMode, staticColor, gradientStart, gradientEnd, animationMode, animationSpeed, lineWidth, lineAlpha, showFriends, showInvisible, targetMode, glowEffect, glowSize, fadeDistance, drawFromCrosshair);
    }

    @Override
    public void deactivate() {
        players.clear();
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.player == null || mc.world == null) return;

        players.clear();
        mc.world.getPlayers().stream()
                .filter(player -> player != mc.player)
                .filter(player -> showInvisible.isValue() || !player.isInvisible())
                .filter(this::shouldTracePlayer)
                .forEach(players::add);

        if (players.isEmpty()) return;

        float tickDelta = e.getPartialTicks();
        long currentTime = System.currentTimeMillis();

        Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();

        for (PlayerEntity player : players) {
            if (player == null || player.isRemoved()) continue;

            double interpX = MathHelper.lerp(tickDelta, player.lastX, player.getX());
            double interpY = MathHelper.lerp(tickDelta, player.lastY, player.getY()) + player.getHeight() / 2.0;
            double interpZ = MathHelper.lerp(tickDelta, player.lastZ, player.getZ());

            Vec3d target = new Vec3d(interpX - cameraPos.x, interpY - cameraPos.y, interpZ - cameraPos.z);
            Vec3d start = drawFromCrosshair.isValue() ? getCrosshairPosition() : new Vec3d(0, 0, 0);

            float distance = (float) target.length();
            float fadeAlpha = calculateFadeAlpha(distance);

            if (fadeAlpha <= 0.01f) continue;

            int lineColor = getLineColor(currentTime, player, fadeAlpha);

            Render3D.drawLine(start, target, lineColor, lineWidth.getValue(), false);

            if (glowEffect.isValue()) {
                drawGlowLine(start, target, lineColor, glowSize.getValue(), fadeAlpha);
            }
        }
    }

    private Vec3d getCrosshairPosition() {
        if (mc.crosshairTarget == null) return new Vec3d(0, 0, 0);
        return new Vec3d(
                mc.crosshairTarget.getPos().x - mc.gameRenderer.getCamera().getCameraPos().x,
                mc.crosshairTarget.getPos().y - mc.gameRenderer.getCamera().getCameraPos().y,
                mc.crosshairTarget.getPos().z - mc.gameRenderer.getCamera().getCameraPos().z
        ).normalize().multiply(0.5);
    }

    private float calculateFadeAlpha(float distance) {
        float fadeStart = fadeDistance.getValue();
        if (distance <= fadeStart) return 1.0f;
        float fadeRange = 125.0f;
        float fade = Math.min((distance - fadeStart) / fadeRange, 1.0f);
        return 1.0f - fade;
    }

    private void drawGlowLine(Vec3d start, Vec3d end, int color, float glowWidth, float alpha) {
        float glowAlpha = alpha * 0.75f;
        int glowColor = (color & 0x00FFFFFF) | ((int) (glowAlpha * 255) << 24);
        Render3D.drawLine(start, end, glowColor, lineWidth.getValue() + glowWidth, false);
    }

    private boolean shouldTracePlayer(PlayerEntity player) {
        boolean isFriend = FriendUtils.isFriend(player);

        return switch (targetMode.getSelected()) {
            case "Все игроки" -> true;
            case "Только враги" -> !isFriend;
            case "Только друзья" -> isFriend && showFriends.isValue();
            default -> true;
        };
    }

    private int getLineColor(long time, PlayerEntity player, float fadeAlpha) {
        boolean isFriend = FriendUtils.isFriend(player);
        int baseColor = isFriend ? 0xFF55FF55 : staticColor.getColor();

        String mode = colorMode.getSelected();
        int color;

        if (mode.equals("Один цвет")) {
            if (!animationMode.getSelected().equals("Нет")) {
                float speed = animationSpeed.getValue();
                String animMode = animationMode.getSelected();

                if (animMode.equals("Радуга")) {
                    color = ColorUtil.rainbow((int) (speed * 200), 0, 0.8f, 0.9f, fadeAlpha);
                } else if (animMode.equals("Пульсация")) {
                    color = AnimatedColorHelper.getPulsingColor(baseColor, speed, time);
                } else {
                    AnimatedColorHelper.AnimationMode anim = animMode.equals("Ping-Pong") ?
                            AnimatedColorHelper.AnimationMode.PING_PONG :
                            AnimatedColorHelper.AnimationMode.CIRCULAR;
                    color = AnimatedColorHelper.getAnimatedOutlineColor(baseColor, speed, time, anim);
                }
            } else {
                color = baseColor;
            }
        } else {
            float speed = animationSpeed.getValue();
            String animMode = animationMode.getSelected();

            if (animMode.equals("Радуга")) {
                color = ColorUtil.rainbow((int) (speed * 200), 0, 0.8f, 0.9f, fadeAlpha);
            } else if (animMode.equals("Пульсация")) {
                int start = gradientStart.getColor();
                color = AnimatedColorHelper.getPulsingColor(start, speed, time);
            } else if (animMode.equals("Ping-Pong") || animMode.equals("Круговая")) {
                AnimatedColorHelper.AnimationMode anim = animMode.equals("Ping-Pong") ?
                        AnimatedColorHelper.AnimationMode.PING_PONG :
                        AnimatedColorHelper.AnimationMode.CIRCULAR;
                int[] gradient = AnimatedColorHelper.getAnimatedOutlineGradient(
                        gradientStart.getColor(), gradientEnd.getColor(), speed, time, 1.0f, anim);
                color = gradient[0];
            } else {
                color = gradientStart.getColor();
            }
        }

        int alpha = (int) (lineAlpha.getValue() * fadeAlpha * 255);
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
