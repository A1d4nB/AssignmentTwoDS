import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.awt.image.BufferedImage;

public class Canvas extends JPanel {
    private StrokeData currentStroke;       // Stores the points of the current freehand stroke
    private Shapes currentShape;            // Holds the current shape
    private float strokeWidth = 5;          // Set default stroke width
    private BufferedImage buffer;
    private Graphics2D bufferGraphics;
    private final String font = "Arial";
    private String textToAdd = "";
    private int textFontSize = 12;
    private JoinWhiteBoard joinWhiteBoard;
    private long lastSent = 0;
    private float currentStrokeStyle = 5;

    private String selectedShape = "Free Draw"; // can be "Free Draw", "Rectangle", "Oval", "Line", etc.
    private Color shapeColor = Color.BLACK; // Set default colour

    public Canvas(JoinWhiteBoard jb) {
        this.joinWhiteBoard = jb;
        canvasSetup();
    }

    public void canvasSetup() {
        setBackground(Color.WHITE);

        MouseAdapter handler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if ("Text".equals(selectedShape)) {
                    if (textToAdd != null && !textToAdd.isEmpty()) {
                        DrawText dt = new DrawText(textToAdd, e.getPoint(), textFontSize, shapeColor);
                        joinWhiteBoard.addString(dt); // Send to others
                        drawTextOnBuffer(dt);       // Draw locally
                        repaint();
                    }
                } else if (!selectedShape.equals("Free Draw")) {
                    // Shape mode
                    Point startPoint = e.getPoint();
                    currentShape = createShape(selectedShape, startPoint, startPoint, shapeColor);
                    currentShape.setIntermediate(true);
                    joinWhiteBoard.addShape(currentShape); // Send intermediate
                    repaint(); // Repaint to show intermediate
                } else {
                    // Free draw mode (or Erase mode)
                    currentStroke = new StrokeData(shapeColor, strokeWidth, true);
                    currentStroke.addPoint(e.getPoint());
                    joinWhiteBoard.addStroke(currentStroke); // Send intermediate
                    drawStrokeOnBuffer(currentStroke);       // Draw locally
                    repaint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!selectedShape.equals("Free Draw")) {
                    if (currentShape != null) {
                        currentShape.setEndPoint(e.getPoint());
                        currentShape.setIntermediate(true);

                        long now = System.currentTimeMillis();
                        if (now - lastSent > 20) {
                            joinWhiteBoard.addShape(currentShape);
                            lastSent = now;
                        }
                        repaint(); // Repaint to show intermediate
                    }
                } else {
                    if (currentStroke != null) {
                        currentStroke.addPoint(e.getPoint());
                        joinWhiteBoard.addStroke(currentStroke); // Send intermediate
                        drawStrokeOnBuffer(currentStroke);       // Draw locally
                        repaint();
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!selectedShape.equals("Free Draw")) {
                    if (currentShape != null) {
                        currentShape.setEndPoint(e.getPoint());
                        currentShape.setIntermediate(false);
                        joinWhiteBoard.addShape(currentShape); // Send final
                        // The final shape is drawn by addRemoteShape
                        drawShapeOnBuffer(currentShape); // Draw final shape locally
                        currentShape = null;
                        repaint();

                    }
                } else {
                    if (currentStroke != null) {
                        currentStroke.setIntermediate(false);
                        joinWhiteBoard.addStroke(currentStroke); // Send final
                        // The final stroke is drawn by addRemoteStroke
                        currentStroke = null;
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
            case "Rectangle" -> new Rectangles(start, end, currentStrokeStyle, color, false);
            case "Oval" ->      new Ovals(start, end, currentStrokeStyle, color, false);
            case "Line" ->      new Lines(start, end, currentStrokeStyle, color, false);
            case "Triangle" ->  new Triangles(start, end, currentStrokeStyle, color, false);
            default ->          null;
        };
    }

    protected void clearWhiteBoard() {
        joinWhiteBoard.clearStrokes();
        joinWhiteBoard.clearShapes();
        joinWhiteBoard.clearStrings();
        joinWhiteBoard.clearChats();

        if (bufferGraphics != null) {
            bufferGraphics.setColor(Color.WHITE);
            bufferGraphics.fillRect(0, 0, getWidth(), getHeight());
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // --- ADD THIS: Buffer creation logic ---
        // If buffer doesn't exist, or window was resized, create a new one
        if (buffer == null || buffer.getWidth() != getWidth() || buffer.getHeight() != getHeight()) {
            buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            bufferGraphics = buffer.createGraphics();

            // IMPORTANT: Clear the new buffer to white
            bufferGraphics.setColor(Color.WHITE);
            bufferGraphics.fillRect(0, 0, getWidth(), getHeight());

            // Redraw everything from the lists onto the new buffer
            redrawAllOnBuffer();
        }
        // --- END ADD ---

        // Draw the entire buffer to the screen in one go
        g.drawImage(buffer, 0, 0, null);

        // Draw intermediate shapes (like the rubber band) on top
        Graphics2D g2d = (Graphics2D) g;
        if (currentShape != null) {
            currentShape.draw(g2d);
        }
        for (Shapes s : joinWhiteBoard.getIntermediateShapes()) {
            s.draw(g2d);
        }
    }


    private void redrawAllOnBuffer() {
        if (bufferGraphics == null) return;

        // 1. Clear buffer to white
        bufferGraphics.setColor(Color.WHITE);
        bufferGraphics.fillRect(0, 0, getWidth(), getHeight());

        // --- CHANGE DRAW ORDER ---

        // 2. Draw all permanent shapes FIRST
        for (Shapes shape : joinWhiteBoard.getShapeList()) {
            drawShapeOnBuffer(shape);
        }

        // 3. Draw all text SECOND
        for (DrawText dt : joinWhiteBoard.getStrings()) {
            drawTextOnBuffer(dt);
        }

        // 4. Draw all strokes (including erasers) LAST
        for (StrokeData stroke : joinWhiteBoard.getStrokes()) {
            drawStrokeOnBuffer(stroke);
        }

    }


    public void drawStrokeOnBuffer(StrokeData stroke) {
        if (bufferGraphics == null || stroke == null) return;

        bufferGraphics.setColor(stroke.getColor());
        bufferGraphics.setStroke(new BasicStroke(stroke.getWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        if (stroke.size() > 1) {
            Point p1 = stroke.getPoint(0);
            for (int i = 1; i < stroke.size(); i++) {
                Point p2 = stroke.getPoint(i);
                bufferGraphics.drawLine(p1.x, p1.y, p2.x, p2.y);
                p1 = p2; // Move to the next point
            }
        } else if (stroke.size() == 1) {
            Point p = stroke.getStartPoint();
            int w = (int) stroke.getWidth();
            bufferGraphics.fillOval(p.x - w/2, p.y - w/2, w, w);
        }
    }

    public void drawShapeOnBuffer(Shapes shape) {
        if (bufferGraphics == null || shape == null) return;
        shape.draw(bufferGraphics);
    }

    public void drawTextOnBuffer(DrawText dt) {
        if (bufferGraphics == null || dt == null) return;
        bufferGraphics.setColor(dt.getColor());
        bufferGraphics.setFont(new Font(font, Font.PLAIN, dt.fontSize()));
        bufferGraphics.drawString(dt.text(), dt.pos().x, dt.pos().y);
    }



    protected void addText(String text, Point p, int fontSize, Color color) {
        if(text == null || text.isEmpty()) return;
        //DrawText currentText = new DrawText(text, p, fontSize, shapeColor);
        joinWhiteBoard.addString(new DrawText(text, p, fontSize, color));
        repaint();
    }

    public void setSelectedShape(String shape) {
        this.selectedShape = shape;
    }

    public void setShapeColor(Color c) {
        this.shapeColor = c;
    }

    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
        this.currentStrokeStyle = strokeWidth;
    }

    public void setTextToAdd(String text) {
        this.textToAdd = text;
    }

    public void setFontSize(int fontSize) {
        this.textFontSize = fontSize;
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

    public synchronized List<StrokeData> getStrokesCopy() {
        return new ArrayList<>(joinWhiteBoard.getStrokes());
    }

    public synchronized List<Shapes> getShapesCopy() {
        return new ArrayList<>(joinWhiteBoard.getShapeList());
    }

    public synchronized List<DrawText> getTextsCopy() {
        return new ArrayList<>(joinWhiteBoard.getStrings());
    }
}
