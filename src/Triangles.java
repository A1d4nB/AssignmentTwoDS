
import java.awt.*;
import java.awt.geom.Path2D; // Used to create a custom polygon shape

/**
 * Assumes ClosedShapes is the superclass that provides helper methods
 * like getTopLeftX(), getTopLeftY(), getWidth(), getHeight(),
 * getColor(), and getStrokeWidth(), based on the two points p1 and p2.
 */
public class Triangles extends ClosedShapes { // Changed from Shapes to ClosedShapes

    /**
     * Constructor that passes the defining points, stroke, and color
     * to the superclass.
     */
    public Triangles(Point p1, Point p2, float s, Color c, boolean intermediate) {
        super(p1, p2, s, c, intermediate);
    }

    /**
     * Draws the triangle on the whiteboard.
     */
    @Override
    public void draw(Graphics2D g) {
        // Set the color and stroke (line thickness)
        g.setColor(getColor());
        g.setStroke(new BasicStroke(getStrokeWidth()));

        // Get the bounding box coordinates from the superclass
        double topX = getTopLeftX();
        double topY = getTopLeftY();
        double width = getWidth();
        double height = getHeight();

        // Define the 3 points of an isosceles triangle that
        // fits within the bounding box.

        // Point 1: Top-middle
        double x1 = topX + width / 2;
        double y1 = topY;

        // Point 2: Bottom-left
        double x2 = topX;
        double y2 = topY + height;

        // Point 3: Bottom-right
        double x3 = topX + width;
        double y3 = topY + height;

        // Create a triangle path
        Path2D.Double triangle = new Path2D.Double();
        triangle.moveTo(x1, y1); // Start at the top point
        triangle.lineTo(x2, y2); // Draw to bottom-left
        triangle.lineTo(x3, y3); // Draw to bottom-right
        triangle.closePath();    // Connect back to the start

        // Draw the path
        g.draw(triangle);
    }
}
