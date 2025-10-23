import java.awt.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public final class DrawText implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final String text;
    private final Point pos;
    private final int fontSize;
    private final Color color;
    private transient String drawingUsername = null;

    public DrawText(String text, Point pos, int fontSize, Color color) {
        this.text = text;
        this.pos = pos;
        this.fontSize = fontSize;
        this.color = color;
    }

    public String text() {
        return text;
    }

    public Point pos() {
        return pos;
    }

    public int fontSize() {
        return fontSize;
    }

    public Color getColor() {
        return color;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (DrawText) obj;
        return Objects.equals(this.text, that.text) &&
                Objects.equals(this.pos, that.pos) &&
                this.fontSize == that.fontSize &&
                Objects.equals(this.color, that.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, pos, fontSize, color);
    }

    @Override
    public String toString() {
        return "DrawText[" +
                "text=" + text + ", " +
                "pos=" + pos + ", " +
                "fontSize=" + fontSize + ", " +
                "color=" + color + ']';
    }

}
