import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class StrokeData implements Serializable {
    private final Color color;
    private final float width;
    private final List<Point> points = new ArrayList<>();
    private boolean intermediate;

    public StrokeData(Color color, float width, boolean intermediate) {
        this.color = color;
        this.width = width;
        this.intermediate = intermediate;
    }

    public void addPoint(Point p) { points.add(p); }

    public Point getPoint(int i) { return points.get(i); }

    public List<Point> getPoints() { return points; }

    public Color getColor() { return color; }

    public float getWidth() { return width; }

    public boolean isEmpty() { return points.isEmpty(); }

    public int size() { return points.size(); }

    public Point getStartPoint() { return points.getFirst(); }

    public boolean isIntermediate() {
        return intermediate;
    }

    public void setIntermediate(boolean intermediate) {
        this.intermediate = intermediate;
    }

}
