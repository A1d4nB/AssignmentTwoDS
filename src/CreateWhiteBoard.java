import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CreateWhiteBoard
 * ----------------
 * Acts as the headless whiteboard server.
 * - Accepts client connections.
 * - Receives drawing commands via ClientHandler.
 * - Updates the shared Canvas (server-side state).
 * - Broadcasts updates to all connected clients.
 */
public class CreateWhiteBoard {

    private final int port;
    private final String managerUsername;
    private final Canvas canvas;                      // Shared drawing surface
    private final List<ClientHandler> clients;        // Connected clients
    private ServerSocket serverSocket;                // Main server socket
    private boolean running = false;

    public CreateWhiteBoard(int port, String managerUsername) {
        this.port = port;
        this.managerUsername = managerUsername;
        this.canvas = new Canvas();
        this.clients = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * Starts the headless server and begins accepting client connections.
     */
    public void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;

            System.out.println("[Server] Whiteboard created by manager: " + managerUsername);
            System.out.println("[Server] Listening on port " + port + " ...");

            // Loop, listening for incoming connections
            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Server] New client connection from " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());

                // Spawn handler thread to handle new connection
                ClientHandler handler = new ClientHandler(clientSocket, this);
                synchronized (clients) {
                    clients.add(handler);
                }

                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("[Server] Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            stopServer();
        }
    }

    /**
     * Called by a ClientHandler when it receives a drawing command.
     * The command is applied to the shared canvas and broadcast to others.
     */
    public synchronized void handleDrawCommand(DrawCommand cmd, ClientHandler source) {
        if (cmd == null) return;

        // update the shared canvas depending on command type
        switch (cmd.getType()) {
            case STROKE -> canvas.addRemoteStroke(cmd.getStroke());
            case SHAPE -> canvas.addRemoteShape(cmd.getShape());
            case TEXT -> canvas.addRemoteText(cmd.getText());
            case CLEAR -> canvas.clearRemote();
            default -> System.err.println("[Server] Unknown or unexpected draw command: " + cmd);
        }

        // Broadcast to all other clients
        broadcast(cmd, source);
    }

    /**
     * Broadcast a message to all connected clients except the source.
     */
    public void broadcast(DrawCommand cmd, ClientHandler exclude) {
        synchronized (clients) {
            for (ClientHandler c : clients) {
                if (c != exclude) {
                    c.sendCommand(cmd);
                }
            }
        }
    }

    /**
     * Removes a disconnected client from the active list.
     */
    public void removeClient(ClientHandler client) {
        synchronized (clients) {
            clients.remove(client);
        }
        System.out.println("[Server] Client disconnected. Active clients: " + clients.size());
    }

    /**
     * Sends the current whiteboard state to a new client that just joined.
     */
    public void sendCurrentState(ClientHandler client) {
        synchronized (canvas) {
            for (StrokeData s : canvas.getStrokesCopy()) {
                client.sendCommand(new DrawCommand(s));
            }
            for (Shapes sh : canvas.getShapesCopy()) {
                client.sendCommand(new DrawCommand(sh));
            }
            for (DrawText t : canvas.getTextsCopy()) {
                client.sendCommand(new DrawCommand(t));
            }
        }
    }

    /**
     * Stops the server and closes all sockets.
     */
    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            /*synchronized (clients) {
                for (ClientHandler c : clients) {
                    c.close();
                }
                clients.clear();
            }*/
        } catch (IOException e) {
            System.err.println("[Server] Error stopping server: " + e.getMessage());
        }
        System.out.println("[Server] Whiteboard server stopped.");
    }

    public Canvas getCanvas() {
        return canvas;
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
