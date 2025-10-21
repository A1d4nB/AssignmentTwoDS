import java.awt.*;
import java.io.Serializable;

public abstract class Shapes implements Serializable {
    private Point startPoint = new Point();
    private Point endPoint = new Point();
    private float strokeWidth;
    private Color color;
    private boolean intermediate;

    public Shapes() {
        this.strokeWidth = 5;
        color = Color.BLACK;

    }

    public Shapes(Point a, Point b, float s, Color c, boolean intermediate) {
        this.startPoint = a;
        this.endPoint = b;
        this.strokeWidth = s;
        this.color = c;
        this.intermediate = intermediate;
    }

    public abstract void draw(Graphics2D g);

    public Color getColor() {
        return this.color;
    }

    public Point getStartPoint() {
        return startPoint;
    }

    public Point getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(Point endPoint) {
        this.endPoint = endPoint;
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public boolean getIntermediate() {
        return intermediate;
    }

    public void setIntermediate(boolean intermediate) {
        this.intermediate = intermediate;
    }
}

