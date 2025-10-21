import java.awt.*;
import java.awt.geom.Rectangle2D;

public class Rectangles extends ClosedShapes {

    public Rectangles(Point p1, Point p2, float s, Color c, boolean intermediate) {
        super(p1, p2, s, c, intermediate);
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(getColor());
        g.setStroke(new BasicStroke(getStrokeWidth()));
        g.draw(new Rectangle2D.Double(getTopLeftX(), getTopLeftY(), getWidth(), getHeight()));
    }
}
