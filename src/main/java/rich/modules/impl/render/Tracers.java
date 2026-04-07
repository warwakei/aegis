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

    public Tracers() {
        super("Tracers", "Линии к игрокам от прицела", ModuleCategory.RENDER);
        settings(colorMode, staticColor, gradientStart, gradientEnd, animationMode, animationSpeed, lineWidth, lineAlpha, showFriends, showInvisible, targetMode);
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

            Vec3d start = new Vec3d(0, 0, 0);
            Vec3d end = new Vec3d(interpX - cameraPos.x, interpY - cameraPos.y, interpZ - cameraPos.z);

            int lineColor = getLineColor(currentTime, player);

            Render3D.drawLine(start, end, lineColor, lineWidth.getValue(), false);
        }
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

    private int getLineColor(long time, PlayerEntity player) {
        boolean isFriend = FriendUtils.isFriend(player);
        int baseColor = isFriend ? 0xFF55FF55 : staticColor.getColor();
        
        String mode = colorMode.getSelected();
        int color;

        if (mode.equals("Один цвет")) {
            if (!animationMode.getSelected().equals("Нет")) {
                float speed = animationSpeed.getValue();
                String animMode = animationMode.getSelected();

                if (animMode.equals("Радуга")) {
                    color = ColorUtil.rainbow((int) (speed * 200), 0, 0.8f, 0.9f, lineAlpha.getValue());
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
                color = ColorUtil.rainbow((int) (speed * 200), 0, 0.8f, 0.9f, lineAlpha.getValue());
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

        int alpha = (int) (lineAlpha.getValue() * 255);
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
