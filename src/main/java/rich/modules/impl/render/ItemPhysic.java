package rich.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.ItemEntity;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.events.impl.WorldRenderEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.ColorSetting;
import rich.modules.module.setting.implement.SelectSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.Instance;
import rich.util.ColorUtil;
import rich.util.render.Render3D;

import java.awt.*;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ItemPhysic extends ModuleStructure {
    public static ItemPhysic getInstance() {
        return Instance.get(ItemPhysic.class);
    }

    public SelectSetting mode = new SelectSetting("Физика", "Тип физики предметов")
            .value("Обычная", "Плавающая", "Магнитная")
            .selected("Обычная");
    
    BooleanSetting glowEffect = new BooleanSetting("Свечение", "Добавляет свечение вокруг предметов")
            .setValue(true);
    
    ColorSetting glowColor = new ColorSetting("Цвет свечения", "Цвет свечения предметов")
            .value(ColorUtil.getColor(255, 255, 100, 150))
            .visible(() -> glowEffect.isValue());
    
    SliderSettings floatHeight = new SliderSettings("Высота плавания", "Высота плавания предметов")
            .range(0.1f, 2.0f)
            .setValue(0.5f)
            .visible(() -> mode.isSelected("Плавающая"));
    
    SliderSettings magnetRange = new SliderSettings("Радиус магнита", "Радиус притяжения к игроку")
            .range(1.0f, 10.0f)
            .setValue(3.0f)
            .visible(() -> mode.isSelected("Магнитная"));

    public ItemPhysic() {
        super("ItemPhysic", "Item Physic", ModuleCategory.RENDER);
        settings(mode, glowEffect, glowColor, floatHeight, magnetRange);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.world == null || mc.player == null) return;
        
        for (ItemEntity item : mc.world.getEntitiesByClass(ItemEntity.class, 
                mc.player.getBoundingBox().expand(20), entity -> true)) {
            
            switch (mode.getSelected()) {
                case "Плавающая" -> handleFloatingPhysics(item);
                case "Магнитная" -> handleMagneticPhysics(item);
            }
        }
    }
    
    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (!glowEffect.isValue() || mc.world == null || mc.player == null) return;
        
        for (ItemEntity item : mc.world.getEntitiesByClass(ItemEntity.class, 
                mc.player.getBoundingBox().expand(50), entity -> true)) {
            
            renderItemGlow(item);
        }
    }
    
    private void handleFloatingPhysics(ItemEntity item) {
        if (item.isOnGround()) {
            double time = System.currentTimeMillis() * 0.002;
            double floatY = Math.sin(time + item.getId()) * 0.1 + floatHeight.getValue();
            item.setVelocity(item.getVelocity().x * 0.8, floatY * 0.1, item.getVelocity().z * 0.8);
        }
    }
    
    private void handleMagneticPhysics(ItemEntity item) {
        double distance = mc.player.distanceTo(item);
        if (distance <= magnetRange.getValue() && distance > 1.0) {
            double pullStrength = 0.1 * (1.0 - distance / magnetRange.getValue());
            
            double dx = mc.player.getX() - item.getX();
            double dy = mc.player.getY() - item.getY();
            double dz = mc.player.getZ() - item.getZ();
            
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length > 0) {
                dx /= length;
                dy /= length;
                dz /= length;
                
                item.setVelocity(
                    item.getVelocity().x + dx * pullStrength,
                    item.getVelocity().y + dy * pullStrength,
                    item.getVelocity().z + dz * pullStrength
                );
            }
        }
    }
    
    private void renderItemGlow(ItemEntity item) {
        double distance = mc.player.distanceTo(item);
        if (distance > 30) return;
        
        float alpha = (float) Math.max(0.1, 1.0 - distance / 30.0);
        int color = ColorUtil.setAlpha(glowColor.getColor(), (int) (alpha * 255));
        
        // Рендерим свечение вокруг предмета (используем простой куб вместо сферы)
        Render3D.drawBox(item.getBoundingBox().expand(0.3), color, 1.0f, true, true, true);
        
        // Добавляем пульсирующий эффект
        double time = System.currentTimeMillis() * 0.003;
        float pulse = 1.0f + (float) Math.sin(time + item.getId()) * 0.3f;
        int pulseColor = ColorUtil.setAlpha(glowColor.getColor(), (int) (alpha * 100));
        Render3D.drawBox(item.getBoundingBox().expand(0.5 * pulse), pulseColor, 1.0f, true, true, true);
    }
}