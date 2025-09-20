
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// makes a panel adds a panel too it can make some drawings etc..
// Server going to have a list of users that have joined or connected and allowed connection added into a list
// then if anyone draws need to be sent to the server, server must then block other from edits certain pixels
// as you're drawing send drawing stuff to server to check for concurrency. if drawing passes concurrency, must
// send pixel updates to all users
public class JoinWhiteBoard {
    private static Canvas canvas;
    public static void main(String[] args) {
        canvas = new Canvas();
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Join White Board");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.add(canvas);
            frame.setVisible(true);
        });
    }

}



