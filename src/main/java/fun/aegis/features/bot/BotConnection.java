package fun.aegis.features.bot;

import net.minecraft.util.math.Vec3d;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class BotConnection {
    private Bot bot;
    private Queue<String> messageQueue;
    private boolean spamming;
    private long spamDelay;
    private Thread spamThread;
    private Vec3d botPosition;
    private float botYaw;
    private float botPitch;
    private boolean connected;

    public BotConnection(Bot bot) {
        this.bot = bot;
        this.messageQueue = new LinkedBlockingQueue<>();
        this.spamming = false;
        this.spamDelay = 500;
        this.botPosition = Vec3d.ZERO;
        this.botYaw = 0;
        this.botPitch = 0;
        this.connected = false;
    }

    public void connect() {
        new Thread(() -> {
            try {
                bot.setConnected(true);
                this.connected = true;

                while (connected && bot.isConnected()) {
                    processTasks();
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                bot.setConnected(false);
                this.connected = false;
            }
        }).start();
    }

    private void processTasks() {
        BotTask task = bot.peekTask();
        if (task == null) return;

        switch (task.getType()) {
            case "say" -> {
                sendMessage(String.join(" ", task.getArgs()));
                bot.pollTask();
            }
            case "chat" -> {
                sendMessage(String.join(" ", task.getArgs()));
                bot.pollTask();
            }
            case "cs" -> {
                if (!spamming) {
                    startSpamming(String.join(" ", task.getArgs()), 500);
                }
            }
            case "csd" -> {
                if (!spamming && task.getArgs().length >= 2) {
                    long delay = (long) (Double.parseDouble(task.getArgs()[0]) * 1000);
                    String message = String.join(" ", java.util.Arrays.copyOfRange(task.getArgs(), 1, task.getArgs().length));
                    startSpamming(message, delay);
                }
            }
            case "csm" -> {
                if (!spamming) {
                    startSpammingMultiple(task.getMessages());
                }
            }
            case "csmd" -> {
                if (!spamming) {
                    startSpammingMultiple(task.getMessages());
                }
            }
            case "css" -> {
                stopSpamming();
                bot.pollTask();
            }
            case "getscreen" -> {
                handleGetScreen(task.getArgs());
                bot.pollTask();
            }
        }
    }

    private void sendMessage(String message) {
        if (connected && bot.isConnected()) {
            messageQueue.offer(message);
        }
    }

    private void startSpamming(String message, long delayMs) {
        if (spamming) return;
        spamming = true;
        spamThread = new Thread(() -> {
            while (spamming && connected && bot.isConnected()) {
                sendMessage(message);
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        spamThread.start();
    }

    private void startSpammingMultiple(String[] messages) {
        if (spamming) return;
        spamming = true;
        spamThread = new Thread(() -> {
            int index = 0;
            while (spamming && connected && bot.isConnected()) {
                sendMessage(messages[index % messages.length]);
                index++;
                try {
                    Thread.sleep(spamDelay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        spamThread.start();
    }

    private void handleGetScreen(String[] args) {
        if (args.length > 0 && args[0].equals("ss")) {
            BotCameraManager.getInstance().stopViewing();
        } else {
            BotCameraManager.getInstance().switchToBotView(bot.getId());
        }
    }

    public void stopSpamming() {
        spamming = false;
        if (spamThread != null) {
            spamThread.interrupt();
        }
    }

    public void disconnect() {
        stopSpamming();
        this.connected = false;
    }

    public boolean isConnected() {
        return connected && bot.isConnected();
    }

    public void setBotPosition(Vec3d pos) {
        this.botPosition = pos;
    }

    public void setBotRotation(float yaw, float pitch) {
        this.botYaw = yaw;
        this.botPitch = pitch;
    }

    public Vec3d getBotPosition() {
        return botPosition;
    }

    public float getBotYaw() {
        return botYaw;
    }

    public float getBotPitch() {
        return botPitch;
    }
}
