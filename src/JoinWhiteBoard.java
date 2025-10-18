import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
        //toolpanel.setBorder(new TitledBorder("Tools"));

        //JButton connectButton = getJButton();

        JButton drawButton = new JButton("Draw Mode");
        drawButton.addActionListener(e -> {
            canvas.setDrawMode(true);
            canvas.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            //sendMessage("MODE,DRAW");
        });

        JButton eraserButton = new JButton("Eraser Mode");
        eraserButton.addActionListener(e -> {
            canvas.setDrawMode(false);
            canvas.updateEraserCursor(canvas.getEraserLength());
            //sendMessage("MODE,ERASE");
        });

        JButton clearButton = new JButton("Clear White Board");
        clearButton.addActionListener(e -> {
            canvas.clearWhiteBoard();
            //sendMessage("CLEAR");
        });

        JLabel eraserLabel = new JLabel("Eraser Size:");
        JSpinner eraserSizeSpinner = getEraserSizeSpinner();

        JLabel shapeLabel = new JLabel("Shape:");
        JComboBox<String> shapeSelector = new JComboBox<>(
                new String[]{"Free Draw", "Rectangle", "Oval", "Line", "Text"}
        );

        //Draw text inserter
        JTextField textField = new JTextField();
        textField.setMaximumSize(new Dimension(150, 25));
        textField.addActionListener(e -> canvas.setTextToAdd(textField.getText()));
        JSpinner fontSizeSpinner = new JSpinner(new SpinnerNumberModel(12, 8, 72, 1));
        fontSizeSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, fontSizeSpinner.getPreferredSize().height));
        fontSizeSpinner.addChangeListener(e -> canvas.setStrokeWidth((int) fontSizeSpinner.getValue()));
        fontSizeSpinner.addChangeListener(e -> canvas.setFontSize((int) fontSizeSpinner.getValue()));

        shapeSelector.addActionListener(e -> {
            String shape = (String) shapeSelector.getSelectedItem();
            canvas.setSelectedShape(shape);
            //sendMessage("SHAPE," + shape);
        });

        JLabel colorLabel = new JLabel("Colour:");
        JComboBox<String> colorSelector = getStringJComboBox();

        JLabel strokeLabel = new JLabel("Stroke Size:");
        JSpinner strokeSizeSpinner = getStrokeSizeSpinner();

        toolpanel.add(new ConnectionPanel());
        //toolpanel.add(connectButton);
        //toolpanel.add(Box.createVerticalStrut(10));
        toolpanel.add(drawButton);
        toolpanel.add(eraserButton);
        toolpanel.add(clearButton);
        //toolpanel.add(Box.createVerticalStrut(10));
        toolpanel.add(shapeLabel);
        toolpanel.add(shapeSelector);
        //toolpanel.add(Box.createVerticalStrut(10));
        toolpanel.add(colorLabel);
        toolpanel.add(colorSelector);
        //toolpanel.add(Box.createVerticalStrut(10));
        toolpanel.add(strokeLabel);
        toolpanel.add(strokeSizeSpinner);

        toolpanel.add(eraserLabel);
        toolpanel.add(Box.createVerticalStrut(10));
        toolpanel.add(eraserSizeSpinner);
        toolpanel.add(new JLabel("Text:"));
        toolpanel.add(textField);
        toolpanel.add(new JLabel("Font Size:"));
        toolpanel.add(fontSizeSpinner);

        return toolpanel;
    }

    private static JSpinner getEraserSizeSpinner() {
        JSpinner eraserSizeSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));
        eraserSizeSpinner.setPreferredSize(new Dimension(70, 26));
        eraserSizeSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, eraserSizeSpinner.getPreferredSize().height));
        eraserSizeSpinner.addChangeListener(e -> {
            canvas.setEraserLength((int) eraserSizeSpinner.getValue());
            if(!canvas.getDrawMode()){
                canvas.updateEraserCursor(canvas.getEraserLength());
            }
        });
        return eraserSizeSpinner;
    }

    private static JSpinner getStrokeSizeSpinner() {
        JSpinner strokeSizeSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));
        // Prevent the spinner from growing in height
        strokeSizeSpinner.setPreferredSize(new Dimension(70, 26));
        strokeSizeSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, strokeSizeSpinner.getPreferredSize().height));
        strokeSizeSpinner.addChangeListener(e -> {
            int value = (int) strokeSizeSpinner.getValue();
            canvas.setStrokeWidth(value);
            //sendMessage("STROKE," + value);
        });
        return strokeSizeSpinner;
    }

    private JButton getJButton() {
        JButton connectButton = new JButton("Connect to Server");
        connectButton.addActionListener(e -> {
            if (!isConnected) {
                try {
                    connectToServer(serverIP, serverPort);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "Invalid port number");
                }
            } else {
                JOptionPane.showMessageDialog(null, "Already connected to a server.");
            }
        });
        return connectButton;
    }

    private static JComboBox<String> getStringJComboBox() {
        JComboBox<String> colorSelector = new JComboBox<>(
                new String[]{"Black", "Red", "Green", "Blue", "Orange", "Magenta"}
        );

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

        return colorSelector;
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
            add(hostIP = new JTextField(10),c);
            hostIP.setText(serverIP);
            c.gridy++;
            add(hostPort = new JTextField(5 ),c);
            hostPort.setText(String.valueOf(serverPort));
            c.gridy++;
            add(connectButton = new JButton("Connect"),c);

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
                serverIP =  hostIP.getText();
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

        public class DrawPanel extends JPanel {
            //private JButton connectButton;
            //private JLabel hostLabel, portLabel;
            //private JTextField hostIP, hostPort;

            public DrawPanel() {
                setLayout(new GridBagLayout());
                setBorder(new CompoundBorder(new TitledBorder("Draw"), new EmptyBorder(12, 0, 0, 0)));
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

            }
        }
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
