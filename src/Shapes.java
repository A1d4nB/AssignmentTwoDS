import java.awt.*;

public abstract class Shapes {
    private Point startPoint = new Point();
    private Point endPoint = new Point();
    private Stroke strokeWidth;
    private Color color;

    public Shapes() {
        this.strokeWidth = new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        color = Color.BLACK;

    }

    public Shapes(Point a, Point b, Stroke s, Color c) {
        this.startPoint = a;
        this.endPoint = b;
        this.strokeWidth = s;
        this.color = c;
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

    public Stroke getStrokeWidth() {
        return strokeWidth;
    }
}

