import java.awt.*;
import java.awt.geom.Ellipse2D;

public class Ovals extends ClosedShapes {

    public Ovals(Point p1, Point p2, Stroke s, Color c) {
        super(p1, p2, s, c);
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(getColor());
        g.setStroke(getStrokeWidth());
        g.draw(new Ellipse2D.Double(getTopLeftX(), getTopLeftY(), getWidth(), getHeight()));
    }
}
