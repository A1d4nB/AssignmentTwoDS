import java.awt.*;
import java.awt.geom.Line2D;

public class Lines extends Shapes {

    public Lines(Point a, Point b, float s, Color c, boolean intermediate) {
        super(a, b, s, c, intermediate);
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(getColor());
        g.setStroke(new BasicStroke(getStrokeWidth()));
        g.draw(new Line2D.Double(   (int)(getStartPoint().getX()),
                                    (int)(getStartPoint().getY()),
                                    (int)(getEndPoint().getX()),
                                    (int)(getEndPoint().getY()))
        );
    }
}
