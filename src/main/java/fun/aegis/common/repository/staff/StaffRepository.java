package fun.aegis.common.repository.staff;

import fun.aegis.Aegis;
import fun.aegis.utils.client.managers.file.FileController;
import fun.aegis.utils.client.managers.file.impl.StaffFile;

import java.util.ArrayList;
import java.util.List;

public class StaffRepository {
    private static final List<Staff> staffList = new ArrayList<>();

    public static void addStaff(String name) {
        if (!isStaff(name)) {
            staffList.add(new Staff(name));
        }
    }

    public static void removeStaff(String name) {
        staffList.removeIf(staff -> staff.getName().equalsIgnoreCase(name));
    }

    public static boolean isStaff(String name) {
        return staffList.stream().anyMatch(staff -> staff.getName().equalsIgnoreCase(name));
    }

    public static void clear() {
        staffList.clear();
    }

    public static List<Staff> getStaff() {
        return staffList;
    }

    private static void save() {
        FileController fileController = Aegis.getInstance().getFileController();
        if (fileController != null) {
            fileController.saveFile(StaffFile.class);
        }
    }

    public record Staff(String name) {
        public String getName() {
            return name;
        }
    }
}
