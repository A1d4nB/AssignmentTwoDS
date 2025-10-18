import java.awt.*;
import java.awt.geom.Line2D;

public class Lines extends Shapes {

    public Lines(Point a, Point b, Stroke s, Color c) {
        super(a, b, s, c);
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(getColor());
        g.setStroke(getStrokeWidth());
        g.draw(new Line2D.Double(   (int)(getStartPoint().getX()),
                                    (int)(getStartPoint().getY()),
                                    (int)(getEndPoint().getX()),
                                    (int)(getEndPoint().getY()))
        );
    }
}
