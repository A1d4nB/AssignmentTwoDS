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
    private boolean isApproved = false; // Tracks if this client is in the lobby
    private static long nextId = 0; // Static counter for unique IDs
    private final long handlerId;     // Unique ID for this instance


    public ClientHandler(Socket socket, CreateWhiteBoard server) {
        this.socket = socket;
        this.server = server;

        synchronized (ClientHandler.class) { // Synchronize access to static counter
            this.handlerId = nextId++;
        }

    }

    public long getHandlerId() {
        return handlerId;
    }

    @Override
    public void run() {
        try {
            // Important: create ObjectOutputStream FIRST
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush(); // force header out
            in = new ObjectInputStream(socket.getInputStream());

            drawCommand = (DrawCommand) in.readObject(); // The first command must be a HELLO to introduce user

            if (drawCommand.getType() != DrawCommand.CommandType.HELLO) {
                System.out.println("[Server] New client failed to identify. Connection rejected.");

            }

            System.out.println("[Server] " + drawCommand.getUsername() + " connected.");
            this.username = drawCommand.getUsername();

            if (server.userCount() == 0) {
                // This is the FIRST user. Auto-approve them as manager.
                System.out.println("[Server] " + this.username + " approved as Manager.");
                server.setManagerUsername(this.username);
                this.isApproved = true;

                server.addClient(this); // Add to main list
                setupNewUser(); // Send them the whiteboard state

            } else {
                // This is a SUBSEQUENT user. Put them in the lobby.
                System.out.println("[Server] " + this.username + " is pending approval.");
                this.isApproved = false;
                server.addPendingClient(this.username, this); // Add to pending list

                // Send AUTH request TO THE MANAGER
                server.sendToUser(
                        new DrawCommand(DrawCommand.CommandType.AUTH, this.username),
                        server.getManagerUsername()
                );

                // Send a "waiting" message TO THIS PENDING CLIENT
                sendCommand(new DrawCommand(DrawCommand.CommandType.CHAT, "Server", "Please wait for manager approval..."));
            }

            DrawCommand incoming;
            while ((incoming = (DrawCommand) in.readObject()) != null) {

                // Check if this is the MANAGER sending an AUTH response
                if (this.username.equals(server.getManagerUsername()) &&
                        incoming.getType() == DrawCommand.CommandType.AUTH) {

                    handleAuthResponse(incoming); // This handler (manager) processes the response
                    continue; // Don't process this command any further
                }

                // Check if this client is NOT approved
                if (!this.isApproved) {
                    if (incoming.getType() == DrawCommand.CommandType.BYE) {
                        System.out.println("[Server] Pending user " + this.username + " disconnected.");
                        break; // Go to 'finally' block
                    }
                    // Ignore all other commands (SHAPE, CHAT, etc.) if not approved
                    continue;
                }

                // --- Client is APPROVED. Process commands normally ---
                switch (incoming.getType()) {
                    case HELLO:
                        System.out.println("[Server] Client says HELLO");
                        break;
                    case TEXT:
                        System.out.println("[Server] Client says TEXT");
                        server.getStrings().add(incoming.getText());
                        server.broadcast(incoming, this);
                        break;
                    case CLEAR:
                        System.out.println("[Server] Client says CLEAR");
                        server.getShapeList().clear();
                        server.getStrings().clear();
                        server.getStrokes().clear();
                        server.clearChats();
                        server.broadcast(incoming, this);
                        break;
                    case SHAPE:
                        System.out.println("[Server] Client says SHAPE: " + incoming.getShape().getEndPoint());
                        if (!incoming.getIntermediate()) {
                            server.getShapeList().add(incoming.getShape());
                        }
                        server.broadcast(incoming, this);
                        break;
                    case STROKE:
                        System.out.println("[Server] Client says STROKE");
                        if (!incoming.getIntermediate()) {
                            server.getStrokes().add(incoming.getStroke());
                        }
                        server.broadcast(incoming, this);
                        break;
                    case CHAT:
                        System.out.println("[Server] Client says CHAT");
                        server.addChat(new ChatData(incoming.getUsername(), incoming.getChatText()));
                        server.broadcast(incoming, this);
                        break;
                    case AUTH:
                        // A non-manager client should not be sending AUTH. Ignore it.
                        System.err.println("Warning: Approved non-manager client sent AUTH. Ignoring.");
                        break;
                    case ACTIVE:
                        System.out.println("[Server] Client says ACTIVE");
                        break;
                    case KICK:
                        if (this.username.equals(server.getManagerUsername())) {
                            String userToKick = incoming.getUsername();
                            // --- Add ID to log ---
                            System.out.println("[Server][ID:" + handlerId + "] Manager " + this.username + " is kicking " + userToKick);
                            server.kickUser(userToKick, this.username, this.handlerId); // Pass manager's ID for logging
                        } else {
                            System.err.println("[Server][ID:" + handlerId + "] Warning: Non-manager " + this.username + " tried to kick " + incoming.getUsername());
                        }
                        break;

                    case BYE:
                        System.out.println("[Server] " + incoming.getUsername() + " requested disconnect.");
                        break; // Go to 'finally' block
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
            //e.printStackTrace();
        }  finally {
            try {
                String handlerUser = (this.username != null ? this.username : "UNKNOWN");
                System.out.println("[Server Cleanup][ID:" + handlerId + "] Finally block executing for user: " + handlerUser);

                String usernameToRemove = this.username;
                boolean wasApproved = this.isApproved;

                server.removeClient(this);
                System.out.println("[Server Cleanup][ID:" + handlerId + "] Removed handler instance.");

                if (usernameToRemove != null) {
                    server.removePendingClient(usernameToRemove);
                }

                if (wasApproved && usernameToRemove != null) {
                    server.removeUser(usernameToRemove); // This logs list state
                    System.out.println("[Server Cleanup][ID:" + handlerId + "] Removed username from server list: " + usernameToRemove);

                    DrawCommand disconnectMsg = new DrawCommand(DrawCommand.CommandType.BYE, usernameToRemove, (ArrayList<String>) null);
                    System.out.println("[Server Cleanup][ID:" + handlerId + "] Broadcasting BYE for: " + usernameToRemove);
                    server.broadcast(disconnectMsg, this);
                } else if (usernameToRemove != null) {
                    System.out.println("[Server Cleanup][ID:" + handlerId + "] Cleaned up non-approved user: " + usernameToRemove);
                } else {
                    System.out.println("[Server Cleanup][ID:" + handlerId + "] Cleaned up handler with no username.");
                }

                System.out.println("[Server Cleanup][ID:" + handlerId + "] Ensuring socket is closed.");
                closeSocket(); // Use the existing method which logs
            } catch (Exception e) {
                System.err.println("[Server Cleanup][ID:" + handlerId + "] EXCEPTION in finally block for user " + (this.username != null ? this.username : "UNKNOWN") + ": " + e.getMessage());
                e.printStackTrace();
            }

    }
    }

    public void setupNewUser() {
        // Add the new user to the user list
        server.addUser(drawCommand.getUsername());
        for(String u : server.getUsers()){
            sendCommand(new DrawCommand(DrawCommand.CommandType.USER, u, drawCommand.getUserList()));
        }

        // Broadcast the new user to other clients
        server.broadcast(new DrawCommand(DrawCommand.CommandType.USER, drawCommand.getUsername(), drawCommand.getUserList()), this );

        // Send a CLEAR command to the newly joined user so they can receive the shared whiteboard
        sendCommand(new DrawCommand(DrawCommand.CommandType.CLEAR));

        sendCommand(new DrawCommand(DrawCommand.CommandType.MGRINFO, server.getManagerUsername()));

        // Send the CHAT history to the new user
        for(ChatData c : server.getChats()) {sendCommand(new DrawCommand(DrawCommand.CommandType.CHAT, c.getUsername(), c.getUsername()));}
        sendCommand(new DrawCommand(DrawCommand.CommandType.CHAT, drawCommand.getUsername(), "Welcome to the chat, " + drawCommand.getUsername() + "."));

        // Send list of all server shapes to the new user
        for (Shapes s : server.getShapeList()) {
            if (!s.getIntermediate()) {
                sendCommand(new DrawCommand(s));
            }
        }

        for(DrawText t : server.getStrings()){sendCommand(new DrawCommand(t));}
        for(StrokeData s : server.getStrokes()){sendCommand(new DrawCommand(s));}
    }

    private void handleAuthResponse(DrawCommand cmd) {
        // 'cmd' is the manager's response.
        String targetUsername = cmd.getUsername(); // The user to approve/deny
        String response = cmd.getChatText();       // The "YES" or "NO" string

        // Find the pending client
        ClientHandler pendingHandler = server.getPendingClient(targetUsername);
        if (pendingHandler == null) {
            System.out.println("[Server] Manager responded for " + targetUsername + ", but they are no longer pending.");
            return;
        }

        // Remove from pending list
        server.removePendingClient(targetUsername);

        if ("YES".equals(response)) {
            System.out.println("[Server] Manager APPROVED " + targetUsername);

            // 1. "Promote" the client
            pendingHandler.isApproved = true;
            server.addClient(pendingHandler); // Add to main broadcast list

            // 2. Call the setup method for the newly approved client
            // This will send them the user list, chat history, and all shapes
            pendingHandler.setupNewUser();

        } else { // Response was "NO"
            System.out.println("[Server] Manager DENIED " + targetUsername);

            // 1. Tell the client they were denied
            DrawCommand denial = new DrawCommand(DrawCommand.CommandType.BYE, "Server", "Your request was denied by the manager.");
            pendingHandler.sendCommand(denial);

            // 2. Close their socket. Their own 'finally' block will clean them up.
            try {
                pendingHandler.socket.close();
            } catch (IOException e) {
                System.err.println("Error closing denied client socket: " + e.getMessage());
            }
        }
    }

    public void closeSocket() throws IOException { // <-- Ensure 'public' keyword
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

