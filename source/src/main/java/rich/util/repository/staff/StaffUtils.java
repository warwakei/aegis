package rich.util.repository.staff;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import rich.util.config.impl.staff.StaffConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@UtilityClass
public class StaffUtils {
    @Getter
    private final List<Staff> staffList = new ArrayList<>();

    public void addStaff(String name) {
        if (!isStaff(name)) {
            staffList.add(new Staff(name));
        }
    }

    public void addStaffAndSave(String name) {
        addStaff(name);
        StaffConfig.getInstance().save();
    }

    public void removeStaff(String name) {
        staffList.removeIf(staff -> staff.getName().equalsIgnoreCase(name));
    }

    public void removeStaffAndSave(String name) {
        removeStaff(name);
        StaffConfig.getInstance().save();
    }

    public boolean isStaff(Entity entity) {
        if (entity instanceof PlayerEntity player) {
            return isStaff(player.getName().getString());
        }
        return false;
    }

    public boolean isStaff(String name) {
        return staffList.stream().anyMatch(staff -> staff.getName().equalsIgnoreCase(name));
    }

    public void clear() {
        staffList.clear();
    }

    public void clearAndSave() {
        clear();
        StaffConfig.getInstance().save();
    }

    public List<String> getStaffNames() {
        return staffList.stream().map(Staff::getName).collect(Collectors.toList());
    }

    public int size() {
        return staffList.size();
    }

    public void setStaff(List<String> names) {
        staffList.clear();
        for (String name : names) {
            staffList.add(new Staff(name));
        }
    }
}