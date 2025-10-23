
import java.awt.*;
import java.awt.geom.Path2D; // Used to create a custom polygon shape

public class Triangles extends ClosedShapes { // Changed from Shapes to ClosedShapes

    public Triangles(Point p1, Point p2, float s, Color c, boolean intermediate) {
        super(p1, p2, s, c, intermediate);
    }

    @Override
    public void draw(Graphics2D g) {
        // Set the color and stroke (line thickness)
        g.setColor(getColor());
        g.setStroke(new BasicStroke(getStrokeWidth()));

        double topX = getTopLeftX();
        double topY = getTopLeftY();
        double width = getWidth();
        double height = getHeight();

        double x1 = topX + width / 2;
        double y1 = topY;

        double x2 = topX;
        double y2 = topY + height;

        double x3 = topX + width;
        double y3 = topY + height;

        Path2D.Double triangle = new Path2D.Double();
        triangle.moveTo(x1, y1); // Start at the top point
        triangle.lineTo(x2, y2); // Draw to bottom-left
        triangle.lineTo(x3, y3); // Draw to bottom-right
        triangle.closePath();    // Connect back to the start

        g.draw(triangle);
    }
}
