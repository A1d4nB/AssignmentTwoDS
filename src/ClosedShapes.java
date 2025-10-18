import java.awt.*;

public abstract class ClosedShapes  extends Shapes {
    public ClosedShapes(Point p1, Point p2, Stroke s, Color c)  {
        super(p1, p2, s, c);
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
