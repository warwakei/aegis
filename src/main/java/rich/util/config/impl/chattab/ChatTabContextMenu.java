package rich.util.config.impl.chattab;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatTabContextMenu {
    private static ChatTabContextMenu instance;
    
    private boolean visible;
    private int x;
    private int y;
    private int width;
    private int height;
    private ChatTab targetTab;
    private int targetTabIndex;
    private int hoveredOption = -1;

    private ChatTabContextMenu() {
        this.visible = false;
        this.width = 120;
        this.height = 70;
    }

    public static ChatTabContextMenu getInstance() {
        if (instance == null) {
            instance = new ChatTabContextMenu();
        }
        return instance;
    }

    public void show(int x, int y, ChatTab tab, int tabIndex) {
        this.x = x;
        this.y = y;
        this.targetTab = tab;
        this.targetTabIndex = tabIndex;
        this.visible = true;
        this.hoveredOption = -1;
    }

    public void hide() {
        this.visible = false;
        this.targetTab = null;
        this.targetTabIndex = -1;
        this.hoveredOption = -1;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setHoveredOption(int option) {
        this.hoveredOption = option;
    }

    public int getHoveredOption() {
        return hoveredOption;
    }

    public int getOptionY(int option) {
        return y + 2 + (option * 20);
    }

    public boolean isMouseOverOption(int mouseX, int mouseY, int option) {
        int optionY = getOptionY(option);
        return mouseX >= x && mouseX <= x + width &&
               mouseY >= optionY && mouseY <= optionY + 18;
    }

    public int getOptionCount() {
        return 2; // Удалить, Переименовать
    }
}
