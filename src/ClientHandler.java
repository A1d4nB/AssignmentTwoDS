import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final CreateWhiteBoard server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private DrawCommand drawCommand;

    public ClientHandler(Socket socket, CreateWhiteBoard server) {
        this.socket = socket;
        this.server = server;
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
                // FIXME Reject the client connection
            } else {
                System.out.println("[Server] " + drawCommand.getUsername() + " connected.");
                sendCommand(new DrawCommand(DrawCommand.CommandType.CLEAR));
                sendCommand(new DrawCommand(DrawCommand.CommandType.CHAT, drawCommand.getUsername(), "Welcome to the chat."));

                DrawCommand incoming;
                while ((incoming = (DrawCommand) in.readObject()) != null) {
                    switch (incoming.getType()) {
                        case HELLO -> {
                            System.out.println("[Server] Client says HELLO");
                        }
                        case TEXT -> {System.out.println("[Server] Client says TEXT");}
                        case CLEAR -> {System.out.println("[Server] Client says CLEAR");}
                        case SHAPE -> {System.out.println("[Server] Client says SHAPE");}
                        case STROKE -> {System.out.println("[Server] Client says STROKE");}
                        case CHAT -> {System.out.println("[Server] Client says CHAT");}
                        case BYE -> {
                            System.out.println("[Server] " + drawCommand.getUsername() + " requested disconnect.");
                        }
                        default -> throw new IllegalStateException("Unexpected value: " + incoming.getType());
                    }

                }
            }
        } catch(EOFException eof){
            System.out.println("[Server] " + drawCommand.getUsername() + " disconnected.");
        } catch(Exception e){
            System.err.println("[ClientHandler] Error: " + e.getMessage());
            e.printStackTrace();
        } finally{
            try {
                server.removeClient(this);
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void sendCommand(DrawCommand cmd) {
        try {
            if (out != null) {
                out.writeObject(cmd);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("[ClientHandler] Error sending to " + drawCommand.getUsername() + ": " + e.getMessage());
        }
    }

}
