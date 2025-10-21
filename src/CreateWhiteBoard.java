import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class CreateWhiteBoard {

    private final int port;
    private String managerUsername;
    private final Canvas canvas;                      // Shared drawing surface
    private final List<ClientHandler> clients;        // Connected clients
    private ServerSocket serverSocket;                // Main server socket
    private boolean running = false;

    private List<StrokeData> strokes    = new ArrayList<>();       // Stores the freehand stroke data
    private List<Shapes> shapeList      = new ArrayList<>();       // Stores the shapes
    private List<DrawText> strings      = new ArrayList<>();       // Stores the strings
    private List<ChatData> chats        = new ArrayList<>();       // Stores the chats
    private List<String> users          = new ArrayList<>();       // Stores the list of users connected
    private final Map<String, ClientHandler> pendingClients;

    public CreateWhiteBoard(int port, String managerUsername) {
        this.port = port;
        this.managerUsername = managerUsername;
        this.canvas = null;
        this.clients = Collections.synchronizedList(new ArrayList<>());
        this.pendingClients = Collections.synchronizedMap(new HashMap<>());
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;

            System.out.println("[Server] Whiteboard created by manager: " + managerUsername);
            System.out.println("[Server] Listening on port " + port + " ...");

            // Loop, listening for incoming connections
            while (running) {
                Socket clientSocket = serverSocket.accept();

                // Spawn handler thread to handle new connection
                ClientHandler handler = new ClientHandler(clientSocket, this);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("[Server] Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            stopServer();
        }
    }

    public void broadcast(DrawCommand cmd, ClientHandler exclude) {
        synchronized (clients) {
            for (ClientHandler c : clients) {
                if (c != exclude) { c.sendCommand(cmd); }
            }
        }
    }

    public synchronized boolean sendToUser(DrawCommand cmd, String target) {
        synchronized (clients) {
            for (ClientHandler c : clients) {
                if(c.getUsername() != null && c.getUsername().equals(target)) {
                    try {
                        c.sendCommand(cmd);
                        return true;
                    } catch (Exception e) {
                        System.err.println("[Server] Failed to send private message: " + e.getMessage());
                        return false;
                    }
                }
            }
        }
        return false;
    }



    public void addClient(ClientHandler client) {
        synchronized (clients) {
            clients.add(client);
        }
    }

    public void removeClient(ClientHandler client) {
        synchronized (clients) {
            clients.remove(client);
        }
        System.out.println("[Server] Client disconnected. Active clients: " + clients.size());
    }

    public synchronized void kickUser(String targetUsername, String managerName, long managerHandlerId) {
        ClientHandler handlerToKick = null;

        if (targetUsername.equals(managerName)) {
            System.err.println("[Server] Kick Error: Manager attempted to kick themselves (" + targetUsername + "). Ignoring.");
            return;
        }

        // Find the handler for the target user from the *main* client list
        synchronized (clients) {
            for (ClientHandler c : clients) {
                if (c.getUsername() != null && c.getUsername().equals(targetUsername)) {
                    handlerToKick = c;
                    break;
                }
            }
        }

        if (handlerToKick != null) {
            // Send them a "you were kicked" message
            // We re-use the CHAT field for the "reason"
            DrawCommand kickMsg = new DrawCommand(DrawCommand.CommandType.BYE, "Server", "You were kicked by the manager (" + managerName + ").");
            handlerToKick.sendCommand(kickMsg);

            // Close their socket. Their own ClientHandler 'finally' block
            // will then execute, which removes them from the server lists
            // and broadcasts a final BYE message to all other clients.
            try {
                handlerToKick.closeSocket();
            } catch (IOException e) {
                System.err.println("Error closing socket for kicked user: " + e.getMessage());
            }
        } else {
            System.err.println("[Server] Manager tried to kick non-existent user: " + targetUsername);
        }
    }


    public void addPendingClient(String username, ClientHandler handler) {
        pendingClients.put(username, handler);
    }

    public ClientHandler removePendingClient(String username) {
        return pendingClients.remove(username);
    }

    public ClientHandler getPendingClient(String username) {
        return pendingClients.get(username);
    }

    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

        } catch (IOException e) {
            System.err.println("[Server] Error stopping server: " + e.getMessage());
        }
        System.out.println("[Server] Whiteboard server stopped.");
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public List<Shapes> getShapeList() {
        return shapeList;
    }

    public List<StrokeData> getStrokes() {
        return strokes;
    }

    public List<DrawText> getStrings() {
        return strings;
    }

    public List<ChatData> getChats() {
        return chats;
    }

    public void setChats(List<ChatData> chats) {
        this.chats = chats;
    }

    public void clearChats() {
        chats.clear();
    }

    public void addChat(ChatData chat) {
        chats.add(chat);
    }

    public synchronized int userCount() {
        return users.size();
    }

    public synchronized List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    public void clearUsers() {
        users.clear();
    }

    public synchronized void addUser(String user) {
        users.add(user);
    }

    public synchronized void removeUser(String user) {
        users.remove(user);
    }

    public void setManagerUsername(String username) {
        this.managerUsername = username;
    }

    public String getManagerUsername() {
        return managerUsername;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java CreateWhiteBoard <ServerIPAddress> <port> <managerUsername>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        String managerUsername = args[1];

        CreateWhiteBoard server = new CreateWhiteBoard(port, managerUsername);
        server.startServer();
    }
}
