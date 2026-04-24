package rich.modules.impl.combat;

import rich.events.api.EventHandler;
import rich.events.impl.AttackEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.SelectSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.util.Instance;
import rich.util.timer.StopWatch;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

public class Criticals extends ModuleStructure {
    
    public static Criticals getInstance() {
        return Instance.get(Criticals.class);
    }
    
    private final SelectSetting mode = new SelectSetting("Режим", "Тип критических ударов")
            .value("Packet", "Jump", "MiniJump", "NoGround", "Vulcan", "Matrix", "Fly")
            .selected("Matrix");
    
    private final SliderSettings delay = new SliderSettings("Задержка", "Задержка между критами в мс")
            .range(0, 500)
            .setValue(50);
    
    private final BooleanSetting onlyGround = new BooleanSetting("Только на земле", "Работать только когда игрок на земле")
            .setValue(false);
    
    private final BooleanSetting smartMatrix = new BooleanSetting("Умный Matrix", "Адаптивный алгоритм под Matrix")
            .setValue(true)
            .visible(() -> mode.isSelected("Matrix"));
    
    private final BooleanSetting flyMode = new BooleanSetting("Fly режим", "Безопасные криты в полёте без движения")
            .setValue(true)
            .visible(() -> mode.isSelected("Fly"));
    
    private final StopWatch timer = new StopWatch();
    private int matrixCounter = 0;
    private long lastMatrixTime = 0;
    
    public Criticals() {
        super("Criticals", "Критические удары через пакеты", ModuleCategory.COMBAT);
        settings(mode, delay, onlyGround, smartMatrix, flyMode);
    }
    
    @EventHandler
    public void onAttack(AttackEvent event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        
        Entity target = event.getTarget();
        if (!(target instanceof LivingEntity)) return;
        
        if (!timer.finished(delay.getValue())) return;
        
        // Проверка на землю если включена
        if (onlyGround.isValue() && !mc.player.isOnGround()) return;
        
        // Проверка условий для крита
        if (!canCritical()) return;
        
        switch (mode.getSelected()) {
            case "Packet" -> packetCritical();
            case "Jump" -> jumpCritical();
            case "MiniJump" -> miniJumpCritical();
            case "NoGround" -> noGroundCritical();
            case "Vulcan" -> vulcanCritical();
            case "Matrix" -> matrixCritical();
            case "Fly" -> flyCritical();
        }
        
        timer.reset();
    }
    
    private boolean canCritical() {
        // Для Fly режима - всегда можно
        if (mode.isSelected("Fly")) {
            return true;
        }
        
        // Базовые проверки для остальных режимов
        if (mc.player.isTouchingWater() || mc.player.isInLava()) return false;
        if (mc.player.hasVehicle()) return false;
        
        // Для Matrix режима - дополнительные проверки
        if (mode.isSelected("Matrix")) {
            return canMatrixCritical();
        }
        
        return true;
    }
    
    private boolean canMatrixCritical() {
        // Умная логика для Matrix
        if (!smartMatrix.isValue()) return true;
        
        long currentTime = System.currentTimeMillis();
        
        // Ограничиваем частоту критов для Matrix
        if (currentTime - lastMatrixTime < 150) return false;
        
        // Проверяем счётчик для вариации
        matrixCounter++;
        if (matrixCounter > 3) {
            matrixCounter = 0;
            return currentTime - lastMatrixTime > 300; // Пауза каждые 3 крита
        }
        
        return true;
    }
    
    private void packetCritical() {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y + 0.0625, z, yaw, pitch, false, false));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, false, false));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y + 0.01, z, yaw, pitch, false, false));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, true, false));
    }
    
    private void jumpCritical() {
        mc.player.jump();
    }
    
    private void miniJumpCritical() {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y + 0.01, z, yaw, pitch, false, false));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, true, false));
    }
    
    private void noGroundCritical() {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, false, false));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, true, false));
    }
    
    private void vulcanCritical() {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y + 0.16477328182606651, z, yaw, pitch, false, false));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y + 0.08307781780646721, z, yaw, pitch, false, false));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y + 0.0030162615090425808, z, yaw, pitch, false, false));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, true, false));
    }
    
    private void matrixCritical() {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        
        lastMatrixTime = System.currentTimeMillis();
        
        if (smartMatrix.isValue()) {
            // Умный Matrix - адаптивные значения
            double[] offsets = getMatrixOffsets();
            
            for (double offset : offsets) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y + offset, z, yaw, pitch, false, false));
            }
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, true, false));
        } else {
            // Стандартный Matrix
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y + 0.05, z, yaw, pitch, false, false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, false, false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y + 0.03, z, yaw, pitch, false, false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, true, false));
        }
    }
    
    private void matrixFlyCritical() {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        
        lastMatrixTime = System.currentTimeMillis();
        
        // Специальный режим для полёта - микро движения
        if (mc.player.getAbilities().flying || mc.player.isGliding()) {
            // В полёте используем минимальные смещения
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y + 0.001, z, yaw, pitch, false, false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y - 0.001, z, yaw, pitch, false, false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, false, false));
        } else {
            // На земле - обычный Matrix
            matrixCritical();
        }
    }
    
    private void flyCritical() {
        // Безопасный режим для полёта - НЕ двигает игрока по X/Z
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        boolean onGround = mc.player.isOnGround();
        
        if (flyMode.isValue()) {
            // Ультра-безопасный режим - только изменение onGround флага
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, false, false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, onGround, false));
        } else {
            // Стандартный fly режим с микро Y смещением
            if (onGround) {
                // На земле - минимальное поднятие
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y + 0.0001, z, yaw, pitch, false, false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, true, false));
            } else {
                // В воздухе - только флаг onGround
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, false, false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, true, false));
            }
        }
    }
    
    private double[] getMatrixOffsets() {
        // Адаптивные смещения для Matrix в зависимости от счётчика
        return switch (matrixCounter % 4) {
            case 0 -> new double[]{0.042, 0.021, 0.003};
            case 1 -> new double[]{0.051, 0.025, 0.004};
            case 2 -> new double[]{0.038, 0.019, 0.002};
            default -> new double[]{0.045, 0.023, 0.0035};
        };
    }
}