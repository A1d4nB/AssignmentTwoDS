
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class Canvas extends JPanel{
    private List<List<Point>> strokes;
    private List<Point> currentStroke;
    private boolean drawMode = true;


    public Canvas() {

        setBackground(Color.WHITE);
        strokes = new ArrayList<>();
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if(drawMode){
                    currentStroke = new ArrayList<>();
                    currentStroke.add(e.getPoint());
                    strokes.add(currentStroke);
                } else{
                    eraseStrokes(e.getPoint());
                }
                repaint();
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if(drawMode){
                    if(currentStroke != null){
                        currentStroke.add(e.getPoint());

                    }
                } else {
                    eraseStrokes(e.getPoint());
                }
                repaint();
            }
        });

        setupKeyBindings();
    }

    private void eraseStrokes(Point p){
        Iterator<List<Point>> iterator = strokes.iterator();
        while(iterator.hasNext()){
            List<Point> list = iterator.next();
            for(int i = 0; i < list.size() - 1; i++){
                Point p1 = list.get(i);
                if(p.x == p1.x && p.y == p1.y){
                    //need to make it so it removed points, not entire strokes
                    iterator.remove();
                    return;
                }
            }
        }
    }

    private void setupKeyBindings() {
        // "E" for eraser
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("E"), "eraserMode");
        getActionMap().put("eraserMode", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                setDrawMode(false);
                System.out.println("Switched to ERASER mode");
            }
        });

        // "D" for draw
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("D"), "drawMode");
        getActionMap().put("drawMode", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                setDrawMode(true);
                System.out.println("Switched to DRAW mode");
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        for(List<Point> points : strokes){
            if(points.size() > 1){
                for(int i = 0; i < points.size() - 1; i++){
                    Point p1 = points.get(i);
                    Point p2 = points.get(i + 1);
                    g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }
    }
    public void setDrawMode(boolean dm) {
        this.drawMode = dm;
    }
}
