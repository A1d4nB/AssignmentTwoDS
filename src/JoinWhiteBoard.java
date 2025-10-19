import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class JoinWhiteBoard {
    private static Canvas canvas = new Canvas();
    private boolean isConnected = false;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private static String userName;
    private static String serverIP;
    private static int serverPort;
    private ConnectionPanel connectionPanel = new ConnectionPanel();
    private DrawPanel drawPanel = new DrawPanel();
    private ChatPanel chatPanel = new ChatPanel();

    private void connectToServer(String host, int port) {
        try {
            socket = new Socket(host, port);
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
            isConnected = true;
            System.out.println("Connected to whiteboard server at " + host + ":" + port);

            // Send a HELLO command to the server to introduce the user
            sendMessage(new DrawCommand(DrawCommand.CommandType.HELLO, userName));

            new Thread(new SocketListener()).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Failed to connect to server: " + e.getMessage(),
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void disconnectFromServer() {
        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMessage(DrawCommand message) throws IOException {
        if (isConnected && out != null) {
            out.writeObject(message);
        }
    }

    private class SocketListener implements Runnable {
        @Override
        public void run() {
            try {
                DrawCommand msg;
                while ((msg = (DrawCommand) in.readObject()) != null) {
                    handleServerMessage(msg);
                }
            } catch (IOException e) {
                System.err.println("Disconnected from server: " + e.getMessage());
                isConnected = false;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //Need to expand on this
    private void handleServerMessage(DrawCommand msg) {
        SwingUtilities.invokeLater(() -> {
            switch (msg.getType()) {
                case HELLO -> {
                    System.out.println("[Server] Client says HELLO");
                }
                case TEXT -> {System.out.println("[Server] Client says TEXT");}
                case CHAT -> {addMessage(chatPanel.getChatConversation(), msg.getChatText(), false);}
                case CLEAR -> {
                    System.out.println("[Server] Client says CLEAR");
                    canvas.clearWhiteBoard();}
                case SHAPE -> {System.out.println("[Server] Client says SHAPE");}
                case STROKE -> {System.out.println("[Server] Client says STROKE");}
                case BYE -> {
                    System.out.println("[Server] " + msg.getUsername() + " requested disconnect.");
                }
                default -> throw new IllegalStateException("Unexpected value: " + msg.getType());
            }
        });
    }

    private JPanel createToolboxPanel() {
        JPanel toolpanel = new JPanel();
        toolpanel.setLayout(new BoxLayout(toolpanel, BoxLayout.Y_AXIS));
        toolpanel.add(connectionPanel);
        toolpanel.add(drawPanel);
        toolpanel.add(chatPanel);
        return toolpanel;
    }

    public class ConnectionPanel extends JPanel {
        private JButton connectButton;
        private JLabel hostLabel, portLabel;
        private JTextField hostIP, hostPort;

        public ConnectionPanel() {
            setLayout(new GridBagLayout());
            setBorder(new CompoundBorder(new TitledBorder("Connection"), new EmptyBorder(12, 0, 0, 0)));
            GridBagConstraints c = new GridBagConstraints();

            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            add(new JLabel("Server IP:"), c);
            c.gridy++;
            add(new JLabel("Server Port:"), c);

            c.gridx++;
            c.gridy = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            add(hostIP = new JTextField(10), c);
            hostIP.setText(serverIP);
            c.gridy++;
            add(hostPort = new JTextField(5), c);
            hostPort.setText(String.valueOf(serverPort));
            c.gridy++;
            add(connectButton = new JButton("Connect"), c);

            connectButton.addActionListener(e -> {
                if (!isConnected) {
                    // Connect logic
                    try {
                        connectToServer(serverIP, serverPort);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(null, "Invalid port number");
                    }
                    isConnected = true;
                    setConnect();
                } else {
                    // Disconnect logic
                    //JOptionPane.showMessageDialog(null, "Already connected to a server.");
                    disconnectFromServer();
                    isConnected = false;
                    setDisconnect();
                }
            });

            hostIP.addActionListener(e -> {
                serverIP = hostIP.getText();
            });

            hostPort.addActionListener(e -> {
                serverPort = Integer.parseInt(hostPort.getText());
            });
        }

        public void setConnect() {
            connectButton.setText("Disconnect");
        }

        public void setDisconnect() {
            connectButton.setText(" Connect ");
        }
    }

    public class DrawPanel extends JPanel {
        private JComboBox shapeSelector = new JComboBox<>(
                new String[]{"Triangle", "Rectangle", "Oval", "Line"});
        private JButton clearButton;
        private JTextField drawText;
        private JLabel colorLabel;
        private JComboBox<String> colorSelector = new JComboBox<>(
                new String[]{"Black", "Red", "Green", "Blue", "Orange", "Magenta"});
        private JSpinner strokeSizeSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));
        private JSpinner eraserSizeSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));
        private JSpinner fontSizeSpinner = new JSpinner(new SpinnerNumberModel(12, 8, 72, 1));

        public DrawPanel() {
            setLayout(new GridBagLayout());
            setBorder(new CompoundBorder(new TitledBorder("Draw"), new EmptyBorder(12, 0, 0, 0)));
            GridBagConstraints c = new GridBagConstraints();

            JRadioButton modeDraw = new JRadioButton("Draw");
            ButtonGroup modeGroup = new ButtonGroup();
            modeGroup.add(modeDraw);
            JRadioButton modeErase = new JRadioButton("Erase");
            modeGroup.add(modeErase);
            modeDraw.setSelected(true);

            JRadioButton formFree = new JRadioButton("Free");
            ButtonGroup formGroup = new ButtonGroup();
            formGroup.add(formFree);
            JRadioButton formShape = new JRadioButton("Shape");
            formGroup.add(formShape);
            JRadioButton formText = new JRadioButton("Text");
            formGroup.add(formText);
            formFree.setSelected(true);

            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            add(new JLabel("Mode:"), c);
            c.gridy++;
            add(modeDraw, c);
            c.gridx++;
            add(modeErase, c);
            c.gridy++;
            c.gridx--;
            add(new JLabel("Form:"), c);
            c.gridy++;
            add(formFree, c);
            c.gridy++;
            add(formShape, c);
            c.gridx++;
            add(shapeSelector, c);
            c.gridy++;
            c.gridx--;
            add(formText, c);
            c.gridx++;
            add(drawText = new JTextField(10), c);
            c.gridx--;
            c.gridy++;
            add(colorLabel = new JLabel("Colour:"), c);
            c.gridx++;
            add(colorSelector,c);
            c.gridy++;
            c.gridx--;
            strokeSizeSpinner.setPreferredSize(new Dimension(70, 26));
            strokeSizeSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, strokeSizeSpinner.getPreferredSize().height));

            add(new JLabel("Stroke Size:"), c);
            c.gridx++;
            add(strokeSizeSpinner, c);
            c.gridy++;
            c.gridx--;
            add(new JLabel("Eraser Size:"), c);
            c.gridx++;
            eraserSizeSpinner.setPreferredSize(new Dimension(70, 26));
            eraserSizeSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, eraserSizeSpinner.getPreferredSize().height));
            add(eraserSizeSpinner, c);

            c.gridy++;
            c.gridx--;
            add(new JLabel("Font Size:"), c);
            c.gridx++;
            fontSizeSpinner.setPreferredSize(new Dimension(70, 26));
            fontSizeSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, fontSizeSpinner.getPreferredSize().height));
            fontSizeSpinner.addChangeListener(e -> canvas.setFontSize((int) fontSizeSpinner.getValue()));
            add(fontSizeSpinner, c);
            c.gridy++;
            c.gridx--;
            add(clearButton = new JButton("Clear All"), c);

            colorSelector.addActionListener(e -> {
                String selected = (String) colorSelector.getSelectedItem();
                switch (selected) {
                    case "Red" -> canvas.setShapeColor(Color.RED);
                    case "Green" -> canvas.setShapeColor(Color.GREEN);
                    case "Blue" -> canvas.setShapeColor(Color.BLUE);
                    case "Orange" -> canvas.setShapeColor(Color.ORANGE);
                    case "Magenta" -> canvas.setShapeColor(Color.MAGENTA);
                    case null -> {}
                    default -> canvas.setShapeColor(Color.BLACK);
                }
            });

            drawText.addActionListener(e -> {
                canvas.setTextToAdd(drawText.getText());
                formText.setSelected(true);
                canvas.setSelectedShape("Text");
            });

            shapeSelector.addActionListener(e -> {
                String shape = (String) shapeSelector.getSelectedItem();
                canvas.setSelectedShape(shape);
                formShape.setSelected(true);
            });

            eraserSizeSpinner.addChangeListener(e -> {
                canvas.setEraserLength((int) eraserSizeSpinner.getValue());
                if(!canvas.getDrawMode()){
                    canvas.updateEraserCursor(canvas.getEraserLength());
                }
            });

            formShape.addActionListener(e -> {
                String shape = (String) shapeSelector.getSelectedItem();
                canvas.setSelectedShape(shape);
             });

            strokeSizeSpinner.addChangeListener(e -> {
                int value = (int) strokeSizeSpinner.getValue();
                canvas.setStrokeWidth(value);
            });

            formText.addActionListener(e -> {
                canvas.setSelectedShape("Text");
            });

            formFree.addActionListener(e -> {
                canvas.setSelectedShape("Free Draw");
            });

            modeDraw.addActionListener(e -> {
                canvas.setDrawMode(true);
                canvas.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            });

            modeErase.addActionListener(e -> {
                canvas.setDrawMode(false);
                canvas.updateEraserCursor(canvas.getEraserLength());
            });

            clearButton.addActionListener(e -> {
                canvas.clearWhiteBoard();
            });
          }
    }

    public class ChatPanel extends JPanel {
        private JTextPane chatConversation;
        private JTextField chatMsg;
        private JButton chatSend;
        private JScrollPane chatScroll;

        public ChatPanel() {
            setLayout(new GridBagLayout());
            setBorder(new CompoundBorder(new TitledBorder("Chat"), new EmptyBorder(12, 0, 0, 0)));
            GridBagConstraints c = new GridBagConstraints();

            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            chatConversation = new JTextPane();
            chatConversation.setEditable(false);
            chatScroll = new JScrollPane(chatConversation);
            chatScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1.0;
            c.weighty = 1.0;
            c.gridwidth = 2;
            add(chatScroll, c);
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.CENTER;
            c.gridy++;
            c.weightx = 0;
            c.weighty = 0;
            add(chatMsg = new JTextField(10), c);
            c.gridx++;
            add(chatSend = new JButton("Send"), c);

            chatSend.addActionListener(e -> {
                try {
                    sendMessage(new DrawCommand(DrawCommand.CommandType.CHAT, userName, chatMsg.getText()));
                    addMessage(chatConversation, chatMsg.getText(), true);
                } catch (IOException ex) {
                    System.err.println("[Server] Error: " + ex.getMessage());
                    throw new RuntimeException(ex);
                }
            });
        }

        public JTextPane getChatConversation() {
            return chatConversation;
        }

    }

    public void addMessage(JTextPane chat, String message, Boolean isRightAligned) {
        StyledDocument doc = chat.getStyledDocument();
        SimpleAttributeSet attrs = new SimpleAttributeSet();

        // Set alignment
        StyleConstants.setAlignment(attrs, isRightAligned ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);
        doc.setParagraphAttributes(doc.getLength(), 1, attrs, false);

        // Append message
        try {
            doc.insertString(doc.getLength(), message + "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        //message.setText(message);
        //chatMsg.setCaretPosition(0);
    }

    public static void main(String[] args) {

        // Check right number of arguments received
        if (args.length < 2) {
            System.err.println("Usage: java CreateWhiteBoard <ServerIPAddress> <port> <Username>");
            System.exit(1);
        }

        // Parse arguments
        serverIP = args[0];
        serverPort = Integer.parseInt(args[1]);
        userName = args[2];
        
        System.out.println("Server IP: " + serverIP);
        System.out.println("Server Port: " + serverPort);
        System.out.println("Server Username: " + userName);

        // Start the WhiteBoard client, running in the background
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Join White Board");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(900, 700);

            JoinWhiteBoard joinWhiteBoard = new JoinWhiteBoard();
            frame.add(canvas, BorderLayout.CENTER);
            frame.add(joinWhiteBoard.createToolboxPanel(), BorderLayout.EAST);
            frame.setVisible(true);
        });
    }
}
