import java.awt.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;

public class DrawCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum CommandType { STROKE, SHAPE, TEXT, CLEAR, HELLO, BYE, CHAT, KICK, USER, ACTIVE, AUTH, MGRINFO }

    private ArrayList<String> userList;
    private final CommandType type;
    private StrokeData stroke;  // for freehand drawings
    private Shapes shape;       // for shapes (Rectangles, Ovals, etc.)
    private final DrawText textData;  // for text commands
    private final String chatText;
    private boolean intermediate;
    private final String username;

    /** Constructor for freehand stroke */
    public DrawCommand(StrokeData stroke) {
        this.type = CommandType.STROKE;
        this.stroke = stroke;
        this.shape = null;
        this.textData = null;
        this.username = null;
        this.chatText = null;
        this.intermediate = false;
        this.userList = null;
    }

    public DrawCommand(StrokeData stroke, String username) {
        this.type = CommandType.STROKE;
        this.stroke = stroke;
        this.shape = null;
        this.textData = null;
        this.username = username;
        this.chatText = null;
        this.intermediate = false; // You might want to pass this in
        this.userList = null;
    }

    // --- ADD THIS CONSTRUCTOR ---
    /** Constructor for shape drawing with username */
    public DrawCommand(Shapes shape, String username) {
        this.type = CommandType.SHAPE;
        this.stroke = null;
        this.shape = shape;
        this.textData = null;
        this.username = username;
        this.chatText = null;
        this.intermediate = false; // You might want to pass this in
        this.userList = null;
    }

    // --- ADD THIS CONSTRUCTOR ---
    /** Constructor for adding text with username */
    public DrawCommand(DrawText textData, String username) {
        this.type = CommandType.TEXT;
        this.stroke = null;
        this.shape = null;
        this.textData = textData;
        this.username = username;
        this.chatText = null;
        this.intermediate = false;
        this.userList = null;
    }


    /** Constructor for shape drawing */
    public DrawCommand(Shapes shape) {
        this.type = CommandType.SHAPE;
        this.stroke = null;
        this.shape = shape;
        this.textData = null;
        this.username = null;
        this.chatText = null;
        this.intermediate = false;
        this.userList = null;
    }

    /** Constructor for adding text */
    public DrawCommand(DrawText textData) {
        this.type = CommandType.TEXT;
        this.stroke = null;
        this.shape = null;
        this.textData = textData;
        this.username = null;
        this.chatText = null;
        this.intermediate = false;
        this.userList = null;
    }

    /** Constructor for clearing the board
    public static DrawCommand clearCommand() {
        return new DrawCommand(CommandType.CLEAR);
    } */

    /** Private constructor for CLEAR command */
    public DrawCommand(CommandType type) {
        this.type = type;
        this.stroke = null;
        this.shape = null;
        this.textData = null;
        this.username = null;
        this.chatText = null;
        this.intermediate = false;
        this.userList = null;
    }

    // Use for AUTH and KICK
    public DrawCommand(CommandType type, String username) {
        this.type = type;
        this.stroke = null;
        this.shape = null;
        this.textData = null;
        this.username = username;
        this.chatText = null;
        this.intermediate = false;
        this.userList = null;
    }

    // Used for HELLO, USER, ACTIVE and KICK message
    public DrawCommand(CommandType type, String username, ArrayList<String> userList) {
        this.type = type;
        this.username = username;
        this.stroke = null;
        this.shape = null;
        this.textData = null;
        this.chatText = null;
        this.intermediate = false;
        this.userList = userList;
    }

    // Used for chat messages
    public DrawCommand(CommandType type, String username, String chatText) {
        this.type = type;
        this.username = username;
        this.stroke = null;
        this.shape = null;
        this.textData = null;
        this.chatText = chatText;
        this.intermediate = false;
        this.userList = null;
    }

    /** Full constructor (for advanced usage if needed) */
    public DrawCommand(CommandType type, StrokeData stroke, Shapes shape, DrawText textData, String username) {
        this.type = type;
        this.stroke = stroke;
        this.shape = shape;
        this.textData = textData;
        this.username = username;
        this.chatText = null;
        this.intermediate = false;
    }

    // Getters
    public CommandType getType() {
        return type;
    }

    public StrokeData getStroke() {
        return stroke;
    }

    public Shapes getShape() {
        return shape;
    }

    public DrawText getText() {
        return textData;
    }

    public String getUsername() {
        return username;
    }

    public String getChatText() {
        return chatText;
    }

    public Boolean getIntermediate() {
        return intermediate;
    }

    public void setIntermediate(Boolean intermediate) {
        this.intermediate = intermediate;
    }

    public ArrayList<String> getUserList() {
        return userList;
    }


    @Override
    public String toString() {
        return "[DrawCommand: type=" + type + ", from=" + username + "]";
    }
}
