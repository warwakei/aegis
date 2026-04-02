package fun.aegis.features.bot;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerAddress;

import java.util.*;

public class Bot {
    private final int id;
    private final String nick;
    private final String serverIP;
    private long createdTime;
    private long playtime;
    private boolean connected;
    private Queue<BotTask> taskQueue;

    public Bot(int id, String nick, String serverIP) {
        this.id = id;
        this.nick = nick;
        this.serverIP = serverIP;
        this.createdTime = System.currentTimeMillis();
        this.playtime = 0;
        this.connected = false;
        this.taskQueue = new LinkedList<>();
    }

    public int getId() {
        return id;
    }

    public String getNick() {
        return nick;
    }

    public String getServerIP() {
        return serverIP;
    }

    public long getPlaytime() {
        return playtime;
    }

    public void addPlaytime(long ms) {
        this.playtime += ms;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public Queue<BotTask> getTaskQueue() {
        return taskQueue;
    }

    public void addTask(BotTask task) {
        taskQueue.offer(task);
    }

    public BotTask pollTask() {
        return taskQueue.poll();
    }

    public BotTask peekTask() {
        return taskQueue.peek();
    }

    public void clearTasks() {
        taskQueue.clear();
    }

    public String getInfo() {
        return nick + " - " + id + " - " + (playtime / 1000) + "s";
    }
}
