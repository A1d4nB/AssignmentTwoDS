import java.awt.*;
import java.io.Serializable;

public abstract class ClosedShapes extends Shapes implements Serializable {
    public ClosedShapes(Point p1, Point p2, float s, Color c, boolean intermediate)  {
        super(p1, p2, s, c, intermediate);
    }

    public int getTopLeftX()
    {
        return Math.min((int)(getStartPoint().getX()), (int)(getEndPoint().getX()));
    }

    public int getTopLeftY()
    {
        return Math.min((int)(getStartPoint().getY()), (int)(getEndPoint().getY()));
    }

    public int getWidth()
    {
        return Math.abs((int)getStartPoint().getX() - (int)getEndPoint().getX());
    }

    public int getHeight()
    {
        return Math.abs((int)getStartPoint().getY() - (int)getEndPoint().getY());
    }
}
