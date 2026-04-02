package fun.aegis.features.bot;

import java.util.*;

public class BotManager {
    private static BotManager instance;
    private Map<Integer, Bot> bots;
    private Map<Integer, BotConnection> connections;
    private int nextId;

    private BotManager() {
        this.bots = new HashMap<>();
        this.connections = new HashMap<>();
        this.nextId = 1;
    }

    public static BotManager getInstance() {
        if (instance == null) {
            instance = new BotManager();
        }
        return instance;
    }

    public Bot createBot(String nick, String serverIP) {
        Bot bot = new Bot(nextId++, nick, serverIP);
        bots.put(bot.getId(), bot);
        
        BotConnection connection = new BotConnection(bot);
        connections.put(bot.getId(), connection);
        connection.connect();
        
        return bot;
    }

    public Bot getBot(int id) {
        return bots.get(id);
    }

    public void removeBot(int id) {
        BotConnection conn = connections.get(id);
        if (conn != null) {
            conn.disconnect();
            connections.remove(id);
        }
        bots.remove(id);
    }

    public Collection<Bot> getAllBots() {
        return bots.values();
    }

    public List<String> listBots() {
        List<String> list = new ArrayList<>();
        for (Bot bot : bots.values()) {
            list.add(bot.getInfo());
        }
        return list;
    }

    public void addTask(int botId, String type, String... args) {
        Bot bot = getBot(botId);
        if (bot != null) {
            BotTask task = new BotTask(botId, type, args);
            bot.addTask(task);
        }
    }

    public List<String> listTasks(int botId) {
        Bot bot = getBot(botId);
        if (bot == null) return new ArrayList<>();
        
        List<String> tasks = new ArrayList<>();
        Queue<BotTask> queue = bot.getTaskQueue();
        for (BotTask task : queue) {
            tasks.add(task.getType() + " " + task.getId());
        }
        return tasks;
    }

    public void killTask(int botId, int taskId) {
        Bot bot = getBot(botId);
        if (bot != null) {
            Queue<BotTask> queue = bot.getTaskQueue();
            queue.removeIf(task -> task.getId() == taskId);
        }
    }

    public void stopAllTasks(int botId) {
        Bot bot = getBot(botId);
        if (bot != null) {
            bot.clearTasks();
            BotConnection conn = connections.get(botId);
            if (conn != null) {
                conn.stopSpamming();
            }
        }
    }
}
