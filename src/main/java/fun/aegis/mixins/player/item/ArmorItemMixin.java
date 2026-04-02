package fun.aegis.mixins.player.item;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fun.aegis.utils.display.interfaces.IArmorItem;

@Mixin(ArmorItem.class)
public abstract class ArmorItemMixin implements IArmorItem {

    @Unique
    private ArmorMaterial armorMaterial;

    @Unique
    private EquipmentType type;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void hookCatchArgs(ArmorMaterial material, EquipmentType type, Item.Settings settings, CallbackInfo ci) {
        this.armorMaterial = material;
        this.type = type;
    }

    @Override
    public ArmorMaterial zov_pidarok$getMaterial() {
        return armorMaterial;
    }

    @Override
    public EquipmentType zov_pidarok$getType() {
        return type;
    }
}
