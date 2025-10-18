import java.net.ServerSocket;
import java.util.ArrayList;

public class WhiteBoardServer {
    private ServerSocket serverSocket;
    //private final List<ClientHandler> clients = new ArrayList<>();
    //private final List<DrawCommand> whiteboardState = new ArrayList<>();
    private String managerUsername;

    public WhiteBoardServer(int port, String managerUsername) {
        this.managerUsername = managerUsername;

    }

    public void start() {  } // Accept connections in a loop
    public synchronized void broadcast(String message, ClientHandler exclude) {  }
    public synchronized void addDrawCommand(DrawCommand cmd) {  }
    //public synchronized List<DrawCommand> getCurrentState() {  }
    public synchronized void removeClient(ClientHandler client) {  }

    public void requestJoin(ClientHandler clientHandler, String joinMsg) {
    }
}