import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.awt.image.BufferedImage;

public class Canvas extends JPanel {
    private List<StrokeData> strokes;       // Stores the freehand stroke data
    private StrokeData currentStroke;       // Stores the points of the current freehand stroke
    private List<Shapes> shapeList;         // Stores the shapes
    private Shapes currentShape;            // Holds the current shape
    private boolean drawMode = true;        // true = free draw, false = eraser
    private int eraserLength = 5;           // Set default eraser length
    private float strokeWidth = 5;          // Set default stroke width
    private List<DrawText> strings;         // Stores the text data
    private final String font = "Ariel";
    private String textToAdd = "";
    private int textFontSize = 12;

    private Stroke currentStrokeStyle = new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    private String selectedShape = "Free Draw"; // can be "Free Draw", "Rectangle", "Oval", "Line", etc.
    private Color shapeColor = Color.BLACK; // Set default colour

    public Canvas() {
        setBackground(Color.WHITE);

        strokes = new ArrayList<>();
        shapeList = new ArrayList<>();
        strings = new ArrayList<>();

        MouseAdapter handler = new MouseAdapter() {
        // Capture specific mouse events to start, continue, or end a stroke or shape
            @Override
            public void mousePressed(MouseEvent e) {
                if ("Text".equals(selectedShape) && drawMode) {
                    if (textToAdd != null && !textToAdd.isEmpty()) {
                        addText(textToAdd, e.getPoint(), textFontSize, shapeColor);
                    }
                } else if (!selectedShape.equals("Free Draw") && drawMode) {
                    // Shape mode
                    Point startPoint = e.getPoint();
                    currentShape = createShape(selectedShape, startPoint, startPoint, shapeColor);
                } else if (drawMode) {
                    // Free draw mode
                    currentStroke = new StrokeData(shapeColor, strokeWidth);
                    currentStroke.addPoint(e.getPoint());
                    strokes.add(currentStroke);
                } else {
                    // Eraser mode
                    eraseAt(e.getPoint());
                }
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!selectedShape.equals("Free Draw") && drawMode) {
                    if (currentShape != null) {
                        currentShape.setEndPoint(e.getPoint());
                    }
                } else if (drawMode) {
                    if (currentStroke != null) {
                        currentStroke.addPoint(e.getPoint());
                    }
                } else {
                    eraseAt(e.getPoint());
                }
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!selectedShape.equals("Free Draw") && drawMode) {
                    if (currentShape != null) {
                        currentShape.setEndPoint(e.getPoint());
                        shapeList.add(currentShape);
                        currentShape = null;
                    }
                }
                repaint();
            }
        };

        addMouseListener(handler);
        addMouseMotionListener(handler);
    }

    private Shapes createShape(String type, Point start, Point end, Color color) {

        return switch (type) {
            case "Rectangle" -> new Rectangles(start, end, currentStrokeStyle, color);
            case "Oval" ->      new Ovals(start, end, currentStrokeStyle, color);
            case "Line" ->      new Lines(start, end, currentStrokeStyle, color);
            case "Triangle" ->  new Triangles(start, end, currentStrokeStyle, color);
            default ->          null;
        };
    }

    private void eraseAt(Point p) {
        List<StrokeData> newStrokes = new ArrayList<>();

        for (StrokeData stroke : strokes) {
            StrokeData currentSegment = new StrokeData(stroke.getColor(), stroke.getWidth());
            for (Point point : stroke.getPoints()) {
                if (point.distance(p) > eraserLength) {
                    currentSegment.addPoint(point);
                } else {
                    if (!currentSegment.isEmpty()) {
                        newStrokes.add(currentSegment);
                        currentSegment = new StrokeData(stroke.getColor(), stroke.getWidth());
                    }
                }
            }
            if (!currentSegment.isEmpty()) newStrokes.add(currentSegment);
        }
        strokes = newStrokes;

        List<Shapes> remainingShapes = new ArrayList<>();
        for (Shapes shape : shapeList) {
            if (!isPointNearShape(p, shape)) {
                remainingShapes.add(shape);
            }
        }
        shapeList = remainingShapes;

        Rectangle eraserBounds = new Rectangle(p.x - eraserLength, p.y - eraserLength, eraserLength * 2, eraserLength * 2);
        List<DrawText> remainingTexts = new ArrayList<>();
        for (DrawText text : strings) {
            Font textFont = new Font(font, Font.PLAIN, text.fontSize());
            FontMetrics fm = getFontMetrics(textFont);
            Rectangle textBounds = new Rectangle(text.pos().x, text.pos().y - fm.getAscent(), fm.stringWidth(text.text()), fm.getAscent() + fm.getDescent());

            if(!textBounds.intersects(eraserBounds)){
                remainingTexts.add(text);
            }
        }
        strings = remainingTexts;
    }

    private boolean isPointNearShape(Point p, Shapes s) {
        if (s instanceof ClosedShapes cs) {
            Rectangle bounds = new Rectangle(
                    cs.getTopLeftX(), cs.getTopLeftY(), cs.getWidth(), cs.getHeight());
            return bounds.contains(p.x, p.y) ||
                    p.distance(bounds.getCenterX(), bounds.getCenterY()) < eraserLength;
        } else {

            Point start = s.getStartPoint();
            Point end = s.getEndPoint();
            double dist = pointToSegmentDistance(p, start, end);
            return dist <= eraserLength;
        }
    }

    private double pointToSegmentDistance(Point p, Point a, Point b) {
        double px = p.x, py = p.y;
        double ax = a.x, ay = a.y;
        double bx = b.x, by = b.y;

        double dx = bx - ax;
        double dy = by - ay;

        if (dx == 0 && dy == 0) return p.distance(a);

        double t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));

        double projX = ax + t * dx;
        double projY = ay + t * dy;

        return Point.distance(px, py, projX, projY);
    }

    protected void clearWhiteBoard() {
        strokes = new ArrayList<>();
        shapeList = new ArrayList<>();
        strings.clear();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Draw freehand strokes
        for (StrokeData points : strokes) {
            g2d.setColor(points.getColor());
            g2d.setStroke(new BasicStroke(points.getWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            if (points.size() > 1) {
                for (int i = 0; i < points.size() - 1; i++) {
                    Point p1 = points.getPoint(i);
                    Point p2 = points.getPoint(i + 1);
                    g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            } else if (points.size() == 1) {
                Point p = points.getStartPoint();
                g2d.fillOval(   p.x - (int)strokeWidth/2,
                                p.y - (int)strokeWidth/2,
                                (int)strokeWidth,
                                (int)strokeWidth);
            }
        }

        //Draw text
        for(DrawText dt: strings){
            g2d.setColor(dt.color());
            g2d.setFont(new Font(font, Font.PLAIN, dt.fontSize()));
            g2d.drawString(dt.text(), dt.pos().x, dt.pos().y);

        }

        // Draw shapes
        for (Shapes s : shapeList) {
            s.draw(g2d);
        }

        // Draw current shape (preview)
        if (currentShape != null) {
            currentShape.draw(g2d);
        }
    }

    protected void addText(String text, Point p, int fontSize, Color color) {
        if(text == null || text.isEmpty() || !drawMode) return;
        DrawText currentText = new DrawText(text, p, fontSize, shapeColor);
        strings.add(currentText);
        repaint();
    }

    protected void updateEraserCursor(int eraserSize) {
        int size = eraserSize * 2;
        BufferedImage cursorImg = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = cursorImg.createGraphics();
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(0, 0, size - 1, size - 1);
        g2.dispose();

        Point hotspot = new Point(size / 2, size / 2);

        Cursor customCursor = Toolkit.getDefaultToolkit()
                .createCustomCursor(cursorImg, hotspot, "Eraser");

        this.setCursor(customCursor);
    }

    public void setDrawMode(boolean dm) {
        this.drawMode = dm;
    }

    public void setSelectedShape(String shape) {
        this.selectedShape = shape;
    }

    public void setShapeColor(Color c) {
        this.shapeColor = c;
    }

    public void setStrokeWidth(Stroke s) {
        if (s == null) return;
        this.currentStrokeStyle = s;
        if (s instanceof BasicStroke bs) {
            this.strokeWidth = bs.getLineWidth();
        }
    }

    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
        this.currentStrokeStyle = new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    public int getEraserLength() {
        return eraserLength;
    }

    public void setEraserLength(int eraserLength) {
        this.eraserLength = eraserLength;
    }

    public void setTextToAdd(String text) {
        this.textToAdd = text;
    }

    public void setFontSize(int fontSize) {
        this.textFontSize = fontSize;
    }

    public boolean getDrawMode() {
        return drawMode;
    }

    // Called when a client sends a new stroke to the server
    public synchronized void addRemoteStroke(StrokeData stroke) {
        SwingUtilities.invokeLater(() -> {
            strokes.add(stroke);
            repaint();
        });
    }

    // Called when a client sends a new shape
    public synchronized void addRemoteShape(Shapes shape) {
        SwingUtilities.invokeLater(() -> {
            shapeList.add(shape);
            repaint();
        });
    }

    // Called when a client adds text
    public synchronized void addRemoteText(DrawText text) {
        SwingUtilities.invokeLater(() -> {
            strings.add(text);
            repaint();
        });
    }

    // Called when the whiteboard is cleared (e.g., from server broadcast)
    public synchronized void clearRemote() {
        SwingUtilities.invokeLater(() -> {
            strokes.clear();
            shapeList.clear();
            strings.clear();
            repaint();
        });
    }

    public synchronized List<StrokeData> getStrokesCopy() {
        return new ArrayList<>(strokes);
    }

    public synchronized List<Shapes> getShapesCopy() {
        return new ArrayList<>(shapeList);
    }

    public synchronized List<DrawText> getTextsCopy() {
        return new ArrayList<>(strings);
    }


}
