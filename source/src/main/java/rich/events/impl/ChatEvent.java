package rich.events.impl;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import rich.events.api.events.callables.EventCancellable;

@AllArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatEvent extends EventCancellable {
    String message;
    
    private static boolean processing = true;
    
    public static void setProcessing(boolean processing) {
        ChatEvent.processing = processing;
    }
    
    public static boolean isProcessing() {
        return processing;
    }
}
