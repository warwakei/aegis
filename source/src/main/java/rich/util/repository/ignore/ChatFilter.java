package rich.util.repository.ignore;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ChatFilter {
    public enum FilterType {
        CONTAINS,
        STARTS_WITH,
        ENDS_WITH,
        SENDER,
        WARPS
    }
    
    private FilterType type;
    private String value;
    
    public boolean matches(String message, String sender) {
        if (message == null) return false;
        
        String lowerMessage = message.toLowerCase();
        String lowerValue = value != null ? value.toLowerCase() : "";
        
        return switch (type) {
            case CONTAINS -> lowerMessage.contains(lowerValue);
            case STARTS_WITH -> lowerMessage.startsWith(lowerValue);
            case ENDS_WITH -> lowerMessage.endsWith(lowerValue);
            case SENDER -> sender != null && sender.equalsIgnoreCase(value);
            case WARPS -> lowerMessage.contains("warp") || lowerMessage.contains("/warp");
        };
    }
}