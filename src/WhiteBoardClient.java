

public class WhiteBoardClient {
    private final String username;
    private final int port;
    private final String serverIP;

    public WhiteBoardClient(String serverIP, int port, String username) {
        this.username = username;
        this.serverIP = serverIP;
        this.port = port;
    }

    public int getPort() {
        return port;
    }
    public String getUsername() {
        return username;
    }

    public String getServerIP() {
        return serverIP;
    }

}
