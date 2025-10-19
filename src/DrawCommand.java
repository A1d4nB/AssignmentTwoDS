import java.awt.*;
import java.io.Serial;
import java.io.Serializable;

/**
 * DrawCommand
 * ------------
 * A serializable message representing a drawing action sent between
 * clients and the whiteboard server.
 *
 * It supports four types of actions:
 *  - STROKE (freehand)
 *  - SHAPE (rectangle, oval, line, etc.)
 *  - TEXT (typed text)
 *  - CLEAR (clear entire board)
 */
public class DrawCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum CommandType { STROKE, SHAPE, TEXT, CLEAR, HELLO, BYE, CHAT }

    private final CommandType type;
    private final StrokeData stroke;  // for freehand drawings
    private final Shapes shape;       // for shapes (Rectangles, Ovals, etc.)
    private final DrawText textData;  // for text commands
    private final String chatText;

    // Optional: who sent the command
    private final String username;

    /** Constructor for freehand stroke */
    public DrawCommand(StrokeData stroke) {
        this.type = CommandType.STROKE;
        this.stroke = stroke;
        this.shape = null;
        this.textData = null;
        this.username = null;
        this.chatText = null;
    }

    /** Constructor for shape drawing */
    public DrawCommand(Shapes shape) {
        this.type = CommandType.SHAPE;
        this.stroke = null;
        this.shape = shape;
        this.textData = null;
        this.username = null;
        this.chatText = null;
    }

    /** Constructor for adding text */
    public DrawCommand(DrawText textData) {
        this.type = CommandType.TEXT;
        this.stroke = null;
        this.shape = null;
        this.textData = textData;
        this.username = null;
        this.chatText = null;
    }

    /** Constructor for clearing the board */
    public static DrawCommand clearCommand() {
        return new DrawCommand(CommandType.CLEAR);
    }

    /** Private constructor for CLEAR command */
    public DrawCommand(CommandType type) {
        this.type = type;
        this.stroke = null;
        this.shape = null;
        this.textData = null;
        this.username = null;
        this.chatText = null;
    }

    // Used for HELLO message
    public DrawCommand(CommandType type, String username) {
        this.type = type;
        this.username = username;
        this.stroke = null;
        this.shape = null;
        this.textData = null;
        this.chatText = null;
    }

    // Used for chat messages
    public DrawCommand(CommandType type, String username, String chatText) {
        this.type = type;
        this.username = username;
        this.stroke = null;
        this.shape = null;
        this.textData = null;
        this.chatText = chatText;
    }

    /** Full constructor (for advanced usage if needed) */
    public DrawCommand(CommandType type, StrokeData stroke, Shapes shape, DrawText textData, String username) {
        this.type = type;
        this.stroke = stroke;
        this.shape = shape;
        this.textData = textData;
        this.username = username;
        this.chatText = null;
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

    @Override
    public String toString() {
        return "[DrawCommand: type=" + type + ", from=" + username + "]";
    }
}
