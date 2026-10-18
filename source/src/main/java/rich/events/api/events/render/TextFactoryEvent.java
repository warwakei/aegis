package rich.events.api.events.render;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import rich.events.api.events.Event;

@Setter
@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TextFactoryEvent implements Event {
    String text;

    public void replaceText(String protect, String replaced) {
        if (text == null || text.isEmpty()) return;

        if (text.contains(protect)) {
            if (text.equalsIgnoreCase(protect) || text.contains(protect + " ") || text.contains(" " + protect) || text.contains("⏏" + protect) || text.contains(protect + "§")) {
                text = text.replace(protect, replaced);
            }
        }
    }



}
