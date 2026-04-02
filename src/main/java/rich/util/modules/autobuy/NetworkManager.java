package rich.util.modules.autobuy;

import net.minecraft.client.MinecraftClient;
import rich.util.string.chat.ChatMessage;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class NetworkManager {
    private static final int PORT = 25566;

    private volatile ServerSocket serverSocket;
    private volatile Socket clientSocket;
    private final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private volatile PrintWriter out;
    private volatile BufferedReader in;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean stopping = new AtomicBoolean(false);
    private volatile ExecutorService executor;
    private final ConcurrentLinkedQueue<BuyRequest> buyQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<String> serverSwitchQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Boolean> pauseQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Boolean> updateQueue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger clientsInAuction = new AtomicInteger(0);

    private static class ClientHandler {
        final Socket socket;
        final PrintWriter out;
        final BufferedReader in;
        volatile boolean inAuction = false;
        volatile boolean closed = false;

        ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        void close() {
            if (closed) return;
            closed = true;
            try { in.close(); } catch (Exception ignored) {}
            try { out.close(); } catch (Exception ignored) {}
            try { socket.close(); } catch (Exception ignored) {}
        }

        void send(String message) {
            if (!closed && out != null) {
                try {
                    out.println(message);
                    out.flush();
                } catch (Exception ignored) {
                    closed = true;
                }
            }
        }
    }

    public void startAsServer() {
        stop();
        sleep(300);

        running.set(true);
        stopping.set(false);
        clients.clear();
        clientsInAuction.set(0);
        executor = Executors.newCachedThreadPool();

        executor.execute(() -> {
            int attempts = 0;
            while (running.get() && serverSocket == null && attempts < 5) {
                try {
                    ServerSocket ss = new ServerSocket();
                    ss.setReuseAddress(true);
                    ss.bind(new InetSocketAddress(PORT));
                    ss.setSoTimeout(1000);
                    serverSocket = ss;
                    msg("§a[ПОКУПАТЕЛЬ] Сервер запущен на порту " + PORT);
                } catch (IOException e) {
                    attempts++;
                    if (attempts < 5) {
                        msg("§e[ПОКУПАТЕЛЬ] Порт занят, попытка " + attempts + "/5...");
                        sleep(1000);
                    } else {
                        msg("§c[ПОКУПАТЕЛЬ] Не удалось запустить сервер");
                        return;
                    }
                }
            }

            while (running.get() && !stopping.get()) {
                ServerSocket ss = serverSocket;
                if (ss == null || ss.isClosed()) break;

                try {
                    Socket client = ss.accept();
                    client.setTcpNoDelay(true);
                    client.setKeepAlive(true);
                    client.setSoTimeout(5000);

                    ClientHandler handler = new ClientHandler(client);
                    clients.add(handler);
                    connected.set(true);
                    msg("§a[ПОКУПАТЕЛЬ] Проверяющий #" + clients.size() + " подключился!");

                    executor.execute(() -> handleClient(handler));
                } catch (SocketTimeoutException ignored) {
                } catch (IOException e) {
                    if (running.get() && !stopping.get()) {
                        sleep(100);
                    }
                }
            }
        });
    }

    private void handleClient(ClientHandler handler) {
        try {
            while (running.get() && !stopping.get() && !handler.closed) {
                String line;
                try {
                    line = handler.in.readLine();
                } catch (SocketTimeoutException e) {
                    continue;
                }
                if (line == null) break;
                processServerMessage(line, handler);
            }
        } catch (IOException ignored) {
        } finally {
            if (handler.inAuction) {
                clientsInAuction.decrementAndGet();
            }
            handler.close();
            clients.remove(handler);
            updateConnectedState();
            if (running.get() && !stopping.get()) {
                msg("§c[ПОКУПАТЕЛЬ] Проверяющий отключился. Осталось: " + clients.size());
            }
        }
    }

    private void updateConnectedState() {
        connected.set(!clients.isEmpty());
    }

    public void startAsClient() {
        stop();
        sleep(300);

        running.set(true);
        stopping.set(false);
        executor = Executors.newCachedThreadPool();

        executor.execute(() -> {
            while (running.get() && !stopping.get()) {
                if (!connected.get()) {
                    try {
                        Socket socket = new Socket();
                        socket.connect(new InetSocketAddress("localhost", PORT), 2000);
                        socket.setTcpNoDelay(true);
                        socket.setKeepAlive(true);
                        socket.setSoTimeout(5000);

                        clientSocket = socket;
                        out = new PrintWriter(socket.getOutputStream(), true);
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        connected.set(true);
                        msg("§a[ПРОВЕРЯЮЩИЙ] Подключился к покупателю!");

                        clientReadLoop();
                    } catch (IOException e) {
                        connected.set(false);
                    }
                }
                sleep(2000);
            }
        });
    }

    private void clientReadLoop() {
        try {
            while (running.get() && connected.get() && !stopping.get()) {
                BufferedReader reader = in;
                if (reader == null) break;

                String line;
                try {
                    line = reader.readLine();
                } catch (SocketTimeoutException e) {
                    continue;
                }
                if (line == null) break;
                processClientMessage(line);
            }
        } catch (IOException ignored) {
        } finally {
            connected.set(false);
            if (running.get() && !stopping.get()) {
                msg("§c[ПРОВЕРЯЮЩИЙ] Соединение потеряно");
            }
            closeClientSocket();
        }
    }

    private void processServerMessage(String line, ClientHandler handler) {
        if (line.startsWith("BUY:")) {
            try {
                String data = line.substring(4);
                String[] parts = data.split("\\|\\|\\|");
                if (parts.length == 7) {
                    int price = Integer.parseInt(parts[0]);
                    String itemId = parts[1];
                    String displayName = parts[2];
                    int count = Integer.parseInt(parts[3]);
                    String loreHash = parts[4];
                    int maxPrice = Integer.parseInt(parts[5]);
                    int minQuantity = Integer.parseInt(parts[6]);
                    buyQueue.add(new BuyRequest(price, itemId, displayName, count, loreHash, maxPrice, minQuantity));
                }
            } catch (Exception ignored) {}
        } else if (line.equals("ENTER_AUCTION")) {
            if (!handler.inAuction) {
                handler.inAuction = true;
                clientsInAuction.incrementAndGet();
            }
        } else if (line.equals("LEAVE_AUCTION")) {
            if (handler.inAuction) {
                handler.inAuction = false;
                clientsInAuction.decrementAndGet();
            }
        } else if (line.equals("PAUSE:true")) {
            pauseQueue.add(true);
        } else if (line.equals("PAUSE:false")) {
            pauseQueue.add(false);
        }
    }

    private void processClientMessage(String line) {
        if (line.startsWith("SWITCH:")) {
            String server = line.substring(7);
            serverSwitchQueue.add(server);
        } else if (line.equals("PAUSE:true")) {
            pauseQueue.add(true);
        } else if (line.equals("PAUSE:false")) {
            pauseQueue.add(false);
        } else if (line.equals("UPDATE")) {
            updateQueue.add(true);
        }
    }

    public void sendUpdateCommand() {
        for (ClientHandler handler : clients) {
            if (handler.inAuction && !handler.closed) {
                handler.send("UPDATE");
            }
        }
    }

    public boolean pollUpdateCommand() {
        return updateQueue.poll() != null;
    }

    public int getClientsInAuctionCount() {
        return clientsInAuction.get();
    }

    public void sendBuyCommand(int price, String itemId, String displayName, int count, String loreHash, int maxPrice, int minQuantity) {
        if (connected.get() && out != null) {
            try {
                out.println("BUY:" + price + "|||" + itemId + "|||" + displayName + "|||" + count + "|||" + loreHash + "|||" + maxPrice + "|||" + minQuantity);
                out.flush();
            } catch (Exception ignored) {}
        }
    }

    public void sendServerSwitch(String server) {
        for (ClientHandler handler : clients) {
            handler.send("SWITCH:" + server);
        }
    }

    public void sendPauseState(boolean paused) {
        String msg = "PAUSE:" + paused;
        if (out != null) {
            try {
                out.println(msg);
                out.flush();
            } catch (Exception ignored) {}
        }
        for (ClientHandler handler : clients) {
            handler.send(msg);
        }
    }

    public void sendEnterAuction() {
        if (connected.get() && out != null) {
            try {
                out.println("ENTER_AUCTION");
                out.flush();
            } catch (Exception ignored) {}
        }
    }

    public void sendLeaveAuction() {
        if (connected.get() && out != null) {
            try {
                out.println("LEAVE_AUCTION");
                out.flush();
            } catch (Exception ignored) {}
        }
    }

    public BuyRequest pollBuyRequest() {
        return buyQueue.poll();
    }

    public String pollServerSwitch() {
        return serverSwitchQueue.poll();
    }

    public Boolean pollPauseState() {
        return pauseQueue.poll();
    }

    public boolean isConnected() {
        return connected.get();
    }

    public int getConnectedClientCount() {
        return clients.size();
    }

    public boolean isConnectedToServer() {
        return connected.get() && clientSocket != null;
    }

    public boolean isServerRunning() {
        return serverSocket != null && !serverSocket.isClosed();
    }

    private void closeClientSocket() {
        PrintWriter tempOut = out;
        BufferedReader tempIn = in;
        Socket tempClient = clientSocket;

        out = null;
        in = null;
        clientSocket = null;

        try { if (tempIn != null) tempIn.close(); } catch (Exception ignored) {}
        try { if (tempOut != null) tempOut.close(); } catch (Exception ignored) {}
        try { if (tempClient != null) tempClient.close(); } catch (Exception ignored) {}
    }

    private void closeServerSocket() {
        ServerSocket temp = serverSocket;
        serverSocket = null;
        if (temp != null) {
            try { temp.close(); } catch (Exception ignored) {}
        }
    }

    public void stop() {
        stopping.set(true);
        running.set(false);
        connected.set(false);
        clientsInAuction.set(0);

        buyQueue.clear();
        serverSwitchQueue.clear();
        pauseQueue.clear();
        updateQueue.clear();

        for (ClientHandler handler : clients) {
            handler.close();
        }
        clients.clear();

        closeClientSocket();
        closeServerSocket();

        ExecutorService temp = executor;
        executor = null;
        if (temp != null) {
            temp.shutdownNow();
            try {
                temp.awaitTermination(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {}
        }
    }

    private void msg(String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null) {
            mc.execute(() -> ChatMessage.autobuymessage(text));
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}