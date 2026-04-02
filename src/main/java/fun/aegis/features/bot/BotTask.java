package fun.aegis.features.bot;

public class BotTask {
    private final int id;
    private final String type;
    private final String[] args;
    private long createdTime;
    private boolean completed;
    private long delay;
    private int messageIndex;
    private long lastMessageTime;
    private String[] messages;

    public BotTask(int id, String type, String... args) {
        this.id = id;
        this.type = type;
        this.args = args;
        this.createdTime = System.currentTimeMillis();
        this.completed = false;
        this.delay = 0;
        this.messageIndex = 0;
        this.lastMessageTime = System.currentTimeMillis();
        this.messages = new String[0];
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String[] getArgs() {
        return args;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setDelay(long delayMs) {
        this.delay = delayMs;
    }

    public long getDelay() {
        return delay;
    }

    public void setMessages(String[] messages) {
        this.messages = messages;
        this.messageIndex = 0;
    }

    public String[] getMessages() {
        return messages;
    }

    public String getNextMessage() {
        if (messages.length == 0) return "";
        String msg = messages[messageIndex];
        messageIndex = (messageIndex + 1) % messages.length;
        return msg;
    }

    public boolean shouldSendMessage() {
        long now = System.currentTimeMillis();
        if (now - lastMessageTime >= delay) {
            lastMessageTime = now;
            return true;
        }
        return false;
    }

    public String getInfo() {
        return type + " (id: " + id + ", delay: " + delay + "ms)";
    }
}
