import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final CreateWhiteBoard server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private DrawCommand drawCommand;
    private String username;
    private boolean isApproved = false;
    private static long nextId = 0;
    private final long handlerId;


    public ClientHandler(Socket socket, CreateWhiteBoard server) {
        this.socket = socket;
        this.server = server;

        synchronized (ClientHandler.class) {
            this.handlerId = nextId++;
        }
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            drawCommand = (DrawCommand) in.readObject();

            if (drawCommand.getType() != DrawCommand.CommandType.HELLO) {
                System.out.println("[Server] New client failed to identify. Connection rejected.");
            }

            System.out.println("[Server] " + drawCommand.getUsername() + " connected.");
            this.username = drawCommand.getUsername();

            if (server.userCount() == 0) {
                System.out.println("[Server] " + this.username + " approved as Manager.");
                server.setManagerUsername(this.username);
                this.isApproved = true;
                server.addClient(this);
                setupNewUser();

            } else {
                System.out.println("[Server] " + this.username + " is pending approval.");
                this.isApproved = false;
                server.addPendingClient(this.username, this);

                server.sendToUser(
                        new DrawCommand(DrawCommand.CommandType.AUTH, this.username),
                        server.getManagerUsername()
                );
                sendCommand(new DrawCommand(DrawCommand.CommandType.CHAT, "Server", "Please wait for manager approval..."));
            }

            DrawCommand incoming;
            while ((incoming = (DrawCommand) in.readObject()) != null) {

                if (this.username.equals(server.getManagerUsername()) &&
                        incoming.getType() == DrawCommand.CommandType.AUTH) {
                    handleAuthResponse(incoming);
                    continue;
                }

                if (!this.isApproved) {
                    if (incoming.getType() == DrawCommand.CommandType.BYE) {
                        System.out.println("[Server] Pending user " + this.username + " disconnected.");
                        break;
                    }
                    continue;
                }

                switch (incoming.getType()) {
                    case HELLO:
                        break;

                    case TEXT:
                        server.getStrings().add(incoming.getText());
                        server.broadcast(incoming, this);
                        break;

                    case CLEAR:
                        server.getShapeList().clear();
                        server.getStrings().clear();
                        server.getStrokes().clear();
                        server.clearChats();
                        server.broadcast(incoming, this);
                        break;

                    case SHAPE:
                        Shapes receivedShape = incoming.getShape();

                        if (!incoming.getIntermediate()) {
                            server.getShapeList().add(incoming.getShape());
                        }
                        server.broadcast(incoming, this);
                        break;

                    case STROKE:
                        if (!incoming.getIntermediate()) {
                            server.getStrokes().add(incoming.getStroke());
                        }
                        server.broadcast(incoming, this);
                        break;

                    case CHAT:
                        server.addChat(new ChatData(incoming.getUsername(), incoming.getChatText()));
                        server.broadcast(incoming, this);
                        break;

                    case AUTH:
                        System.err.println("Warning: Approved non-manager client sent AUTH. Ignoring.");
                        break;

                    case ACTIVE:
                        break;

                    case KICK:
                        if (this.username.equals(server.getManagerUsername())) {
                            String userToKick = incoming.getUsername();
                            System.out.println("[Server][ID:" + handlerId + "] Manager " + this.username + " is kicking " + userToKick);
                            server.kickUser(userToKick, this.username, this.handlerId); // Pass manager's ID for logging
                        } else {
                            System.err.println("[Server][ID:" + handlerId + "] Warning: Non-manager " + this.username + " tried to kick " + incoming.getUsername());
                        }
                        break;

                    case BYE:
                        System.out.println("[Server] " + incoming.getUsername() + " requested disconnect.");
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + incoming.getType());
                }
                if (incoming.getType() == DrawCommand.CommandType.BYE) {
                    break;
                }
            }
        } catch(EOFException eof){
            System.out.println("[Server] " + drawCommand.getUsername() + " disconnected.");
        } catch(Exception e){
            System.err.println("[ClientHandler] Error: " + e.getMessage());
        }  finally {
            try {
                String handlerUser = (this.username != null ? this.username : "UNKNOWN");
                String usernameToRemove = this.username;
                boolean wasApproved = this.isApproved;

                server.removeClient(this);
                if (usernameToRemove != null) {
                    server.removePendingClient(usernameToRemove);
                }

                if (wasApproved && usernameToRemove != null) {
                    server.removeUser(usernameToRemove);
                    DrawCommand disconnectMsg = new DrawCommand(DrawCommand.CommandType.BYE, usernameToRemove, (ArrayList<String>) null);
                    server.broadcast(disconnectMsg, this);
                } else if (usernameToRemove != null) {
                    System.out.println("[Server Cleanup][ID:" + handlerId + "] Cleaned up non-approved user: " + usernameToRemove);
                } else {
                    System.out.println("[Server Cleanup][ID:" + handlerId + "] Cleaned up handler with no username.");
                }
                System.out.println("[Server Cleanup][ID:" + handlerId + "] Ensuring socket is closed.");
                closeSocket();
            } catch (Exception e) {
                System.err.println("[Server Cleanup][ID:" + handlerId + "] EXCEPTION in finally block for user " + (this.username != null ? this.username : "UNKNOWN") + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void setupNewUser() {
        server.addUser(drawCommand.getUsername());
        for(String u : server.getUsers()){
            sendCommand(new DrawCommand(DrawCommand.CommandType.USER, u, drawCommand.getUserList()));
        }

        server.broadcast(new DrawCommand(DrawCommand.CommandType.USER, drawCommand.getUsername(), drawCommand.getUserList()), this );
        sendCommand(new DrawCommand(DrawCommand.CommandType.CLEAR));
        sendCommand(new DrawCommand(DrawCommand.CommandType.MGRINFO, server.getManagerUsername()));

        for(ChatData c : server.getChats()) {sendCommand(new DrawCommand(DrawCommand.CommandType.CHAT, c.getUsername(), c.getUsername()));}
        sendCommand(new DrawCommand(DrawCommand.CommandType.CHAT, drawCommand.getUsername(), "Welcome to the chat, " + drawCommand.getUsername() + "."));

        for (Shapes s : server.getShapeList()) {
            if (!s.getIntermediate()) {
                sendCommand(new DrawCommand(s));
            }
        }
        for(DrawText t : server.getStrings()){sendCommand(new DrawCommand(t));}
        for(StrokeData s : server.getStrokes()){sendCommand(new DrawCommand(s));}
    }

    private void handleAuthResponse(DrawCommand cmd) {
        String targetUsername = cmd.getUsername();
        String response = cmd.getChatText();
        ClientHandler pendingHandler = server.getPendingClient(targetUsername);

        if (pendingHandler == null) {
            System.out.println("[Server] Manager responded for " + targetUsername + ", but they are no longer pending.");
            return;
        }

        server.removePendingClient(targetUsername);

        if ("YES".equals(response)) {
            System.out.println("[Server] Manager APPROVED " + targetUsername);
            pendingHandler.isApproved = true;
            server.addClient(pendingHandler);
            pendingHandler.setupNewUser();

        } else {
            System.out.println("[Server] Manager DENIED " + targetUsername);
            DrawCommand denial = new DrawCommand(DrawCommand.CommandType.BYE, "Server", "Your request was denied by the manager.");
            pendingHandler.sendCommand(denial);

            try {
                pendingHandler.socket.close();
            } catch (IOException e) {
                System.err.println("Error closing denied client socket: " + e.getMessage());
            }
        }
    }

    public void closeSocket() throws IOException {
        if (socket != null && !socket.isClosed()) {
            System.out.println("[Server Action] Closing socket for handler: " + (this.username != null ? this.username : "UNKNOWN")); // Add log
            socket.close();
        } else {
            System.out.println("[Server Action] Socket already closed or null for handler: " + (this.username != null ? this.username : "UNKNOWN")); // Add log
        }
    }

    public String getUsername() {
        return username;
    }

    public void sendCommand(DrawCommand cmd) {
        try {
            if (out != null) {
                out.reset();
                out.writeObject(cmd);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("[ClientHandler] Error sending to " + drawCommand.getUsername() + ": " + e.getMessage());
        }
    }
}

