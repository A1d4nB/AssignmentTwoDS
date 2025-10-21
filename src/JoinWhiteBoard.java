import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class JoinWhiteBoard {
    private static Canvas canvas;
    private ConnectionPanel connectionPanel = new ConnectionPanel(JoinWhiteBoard.this);
    private DrawPanel drawPanel = new DrawPanel();
    private ChatPanel chatPanel = new ChatPanel();
    private UserPanel userPanel = new UserPanel();

    private boolean isConnected = false;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private static String userName;
    private static String serverIP;
    private static int serverPort;
    private String managerUsername;

    private List<StrokeData> strokes = new ArrayList<>();         // Stores the freehand stroke data
    private List<Shapes> shapeList = new ArrayList<>();                     // Stores the shapes
    private List<Shapes> intermediateShapes = new ArrayList<>();
    private List<DrawText> strings = new ArrayList<>();                     // Stores the text data
    private List<ChatData> chats = new ArrayList<>();

    // Connect to the Whiteboard server
    private void connectToServer(String host, int port) {
        try {
            socket  = new Socket(host, port);
            out     = new ObjectOutputStream(socket.getOutputStream());
            in      = new ObjectInputStream(socket.getInputStream());
            setConnected(true);
            System.out.println("Connected to whiteboard server at " + host + ":" + port);

            // Send a HELLO command to introduce myself
            sendMessage(new DrawCommand(DrawCommand.CommandType.HELLO, userName, (ArrayList<String>) null));

            new Thread(new SocketListener()).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Failed to connect to server: " + e.getMessage(),
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
            setConnected(false);
        }
    }

    public void disconnectFromServer() {
        try {
            if (isConnected && out != null) {
                // Send a message to everyone telling them I've left
                sendMessage(new DrawCommand(DrawCommand.CommandType.BYE, userName, (ArrayList<String>) null));
                out.flush();
                //Thread.sleep(50); // allow small time for message to send
            }
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        } finally {
            setConnected(false);
        }
    }

    private void sendMessage(DrawCommand message) throws IOException {
        if (isConnected && out != null) {
            out.reset();
            out.writeObject(message);
            out.flush();
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
                //connectionPanel.setConnect();
                //isConnected = false;
                setConnected(false);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleServerMessage(DrawCommand msg) {
        SwingUtilities.invokeLater(() -> {
            if (msg.getUsername() != null && msg.getUsername().equals(userName)) {
                switch (msg.getType()) {
                    case SHAPE -> {
                        // Only add final shapes to the master list
                        if (msg.getShape() != null && !msg.getShape().getIntermediate()) {
                            shapeList.add(msg.getShape());
                        }}
                    case STROKE -> {
                        // Only add final strokes to the master list
                        if (msg.getStroke() != null && !msg.getStroke().isIntermediate()) {
                            strokes.add(msg.getStroke());
                        }}
                    case TEXT -> {
                        // Text is always final
                        if (msg.getText() != null) {
                            strings.add(msg.getText());
                        }}
                }
            }

            switch (msg.getType()) {
                case HELLO -> {
                    System.out.println("[Server] Client says HELLO");
                }

                case AUTH -> {
                    int choice = JOptionPane.showConfirmDialog(
                            null,
                            msg.getUsername() + " wants to join this Whiteboard. Allow?",
                            "Confirmation",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE
                    );
                    try {
                        if (choice == JOptionPane.YES_OPTION) {
                            // Tell server: AUTH, for_user="Bob", response="YES"
                            sendMessage(new DrawCommand(DrawCommand.CommandType.AUTH, msg.getUsername(), "YES"));
                        } else {
                            // Tell server: AUTH, for_user="Bob", response="NO"
                            sendMessage(new DrawCommand(DrawCommand.CommandType.AUTH, msg.getUsername(), "NO"));
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                case MGRINFO -> {
                    String manager = msg.getUsername();
                    this.setManagerUsername(manager); // Store it locally
                    userPanel.setManager(manager); // Pass it to the panel
                }

                case TEXT -> {
                    DrawText dt = msg.getText();
                    strings.add(dt);            // Add to list
                    canvas.drawTextOnBuffer(dt);  // Draw on buffer
                    canvas.repaint();
                }

                case CHAT -> {addMessage(chatPanel.getChatConversation(), msg.getUsername() + "> " + msg.getChatText() + "\n", false);}

                case CLEAR -> {
                    canvas.clearWhiteBoard();
                }

                case USER -> {
                    if (msg.getUserList() != null) {
                        userPanel.setUsers(msg.getUserList());
                    } else if (msg.getUsername() != null) {
                        userPanel.addUser(msg.getUsername());
                    }
                    canvas.repaint();
                }

                case SHAPE -> {
                    Shapes shape = msg.getShape();
                    boolean isIntermediate = shape.getIntermediate();

                    if (isIntermediate) {
                        // Intermediate shapes just get shown, not added to list
                        intermediateShapes.removeIf(prev ->
                                prev.getClass() == shape.getClass() &&
                                        prev.getStartPoint().equals(shape.getStartPoint())
                        );
                        intermediateShapes.add(shape);
                    } else {
                        // This is a FINAL shape from another user
                        intermediateShapes.removeIf(prev ->
                                prev.getClass() == shape.getClass() &&
                                        prev.getStartPoint().equals(shape.getStartPoint())
                        );

                        shapeList.add(shape);                 // Add final shape to list
                        canvas.drawShapeOnBuffer(shape);    // Draw final shape on buffer
                    }
                    canvas.repaint();
                }

                case STROKE -> {
                    StrokeData stroke = msg.getStroke();

                    // Only add final strokes to the master list
                    if (!stroke.isIntermediate()) {
                        strokes.add(stroke);
                    }

                    // Draw ALL strokes (intermediate and final) to the buffer
                    canvas.drawStrokeOnBuffer(stroke);
                    canvas.repaint();

                }

                case BYE -> {
                    if (msg.getChatText() != null && !msg.getChatText().isEmpty()) {
                        JOptionPane.showMessageDialog(null,
                                "Disconnected: " + msg.getChatText(),
                                "Connection Closed", JOptionPane.WARNING_MESSAGE);
                        // The SocketListener's 'finally' block will call setConnected(false)
                        // which handles the UI cleanup
                    } else {
                        // This is a normal disconnect notification
                        userPanel.removeUser(msg.getUsername());
                        canvas.repaint();
                    }
                }

                default -> throw new IllegalStateException("Unexpected value: " + msg.getType());
            }
        });
    }

    private JPanel createToolboxPanel() {
        JPanel toolpanel = new JPanel();
        toolpanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        c.weightx = 1;
        toolpanel.add(connectionPanel, c);
        c.gridy++;
        toolpanel.add(drawPanel, c);
        c.gridy++;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        userPanel = new UserPanel();
        toolpanel.add(userPanel, c);
        c.gridy++;
        c.anchor = GridBagConstraints.SOUTH;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        chatPanel.setPreferredSize(null);
        toolpanel.add(chatPanel, c);
        c.gridy++;

        toolpanel.setPreferredSize(new Dimension(260, 0));

        return toolpanel;
    }

    public class ConnectionPanel extends JPanel {
        private JButton connectButton;
        private JLabel hostLabel, portLabel;
        private JTextField hostIP, hostPort;

        public ConnectionPanel(JoinWhiteBoard jwb) {
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
            c.weightx = 1.0;
            add(hostIP = new JTextField(10), c);
            hostIP.setText(serverIP);
            c.gridy++;
            add(hostPort = new JTextField(5), c);
            hostPort.setText(String.valueOf(serverPort));
            c.gridy++;
            add(connectButton = new JButton("Connect"), c);

            connectButton.addActionListener(e -> {
                if (!jwb.isConnected()) {
                    // Connect logic
                    try {
                        jwb.connectToServer(serverIP, serverPort);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(null, "Invalid port number");
                    }
                } else {
                    // Disconnect logic
                    jwb.disconnectFromServer();
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
            repaint();
        }

        public void setDisconnect() {
            connectButton.setText(" Connect ");
            repaint();
        }
    }

    public class DrawPanel extends JPanel {
        private JComboBox shapeSelector = new JComboBox<>(
                new String[]{"Triangle", "Rectangle", "Oval", "Line"});
        private JButton clearButton;
        private JTextField drawText;
        private JLabel colorLabel;
        private JComboBox<String> colorSelector = new JComboBox<>(
                new String[]{"Black", "Red", "Green", "Blue", "Orange", "Magenta", "Cyan", "Dark Gray", "Gray", "Light Gray"
                        ,"Pink", "Yellow", "Indigo", "Gold", "Tan", "Lavender"});
        private JSpinner strokeSizeSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));
        private JSpinner eraserSizeSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));
        private JSpinner fontSizeSpinner = new JSpinner(new SpinnerNumberModel(12, 8, 72, 1));

        private void applySelectedColor() {
            String selected = (String) colorSelector.getSelectedItem();
            switch (selected) {
                case "Red" -> canvas.setShapeColor(Color.RED);
                case "Green" -> canvas.setShapeColor(Color.GREEN);
                case "Blue" -> canvas.setShapeColor(Color.BLUE);
                case "Orange" -> canvas.setShapeColor(Color.ORANGE);
                case "Magenta" -> canvas.setShapeColor(Color.MAGENTA);
                case "Yellow" -> canvas.setShapeColor(Color.YELLOW);
                case "Dark Gray" -> canvas.setShapeColor(Color.DARK_GRAY);
                case "Gray" -> canvas.setShapeColor(Color.GRAY);
                case "Cyan" -> canvas.setShapeColor(Color.CYAN);
                case "Light Gray" -> canvas.setShapeColor(Color.LIGHT_GRAY);
                case "Pink" -> canvas.setShapeColor(Color.PINK);
                case "Indigo" -> canvas.setShapeColor(new Color(75, 0 , 130));
                case "Gold" -> canvas.setShapeColor(new Color(255, 215 , 0));
                case "Tan" -> canvas.setShapeColor(new Color(210, 180 , 140));
                case "Lavender" -> canvas.setShapeColor(new Color(230, 230 , 250));
                case null -> {}
                default -> canvas.setShapeColor(Color.BLACK);
            }
        }

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
                applySelectedColor(); // Just call the helper
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
                int size = (int) eraserSizeSpinner.getValue();
                if (modeErase.isSelected()) {
                    canvas.setStrokeWidth(size);
                    canvas.updateEraserCursor(size); // Update cursor
                }
            });


            formShape.addActionListener(e -> {
                String shape = (String) shapeSelector.getSelectedItem();
                canvas.setSelectedShape(shape);
            });

            strokeSizeSpinner.addChangeListener(e -> {
                if (modeDraw.isSelected()) {
                    int value = (int) strokeSizeSpinner.getValue();
                    canvas.setStrokeWidth(value);
                }
            });

            formText.addActionListener(e -> {
                canvas.setSelectedShape("Text");
            });

            formFree.addActionListener(e -> {
                canvas.setSelectedShape("Free Draw");
            });

            modeDraw.addActionListener(e -> {
                canvas.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

                // Re-enable all drawing controls
                colorSelector.setEnabled(true);
                shapeSelector.setEnabled(true);
                formFree.setEnabled(true);
                formShape.setEnabled(true);
                formText.setEnabled(true);
                drawText.setEnabled(true);
                strokeSizeSpinner.setEnabled(true);

                // Restore the selected color and stroke
                applySelectedColor();
                canvas.setStrokeWidth((int) strokeSizeSpinner.getValue());

                // Restore the selected shape mode
                if (formFree.isSelected()) formFree.doClick();
                else if (formShape.isSelected()) formShape.doClick();
                else if (formText.isSelected()) formText.doClick();
            });


            modeErase.addActionListener(e -> {
                int size = (int) eraserSizeSpinner.getValue();

                // Set eraser properties
                canvas.setShapeColor(Color.WHITE); // Core logic
                canvas.setSelectedShape("Free Draw"); // Core logic
                canvas.setStrokeWidth(size);
                canvas.updateEraserCursor(size); // Use custom cursor

                // Disable all other controls
                colorSelector.setEnabled(false);
                shapeSelector.setEnabled(false);
                formFree.setEnabled(false);
                formShape.setEnabled(false);
                formText.setEnabled(false);
                drawText.setEnabled(false);
                strokeSizeSpinner.setEnabled(false);
            });

            clearButton.addActionListener(e -> {
                try {
                    sendMessage(new DrawCommand(DrawCommand.CommandType.CLEAR));
                    canvas.clearWhiteBoard(); // clear local immediately
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
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
            add(chatMsg = new JTextField(6), c);
            c.gridx++;
            add(chatSend = new JButton("Send"), c);

            chatMsg.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        try {
                            sendMessage(new DrawCommand(DrawCommand.CommandType.CHAT, userName, chatMsg.getText()));
                            addMessage(chatConversation, chatMsg.getText() + " <me\n", true);
                            chatMsg.setText("");
                        } catch (IOException ex) {
                            System.err.println("[Server] Error: " + ex.getMessage());
                            throw new RuntimeException(ex);
                        }
                    }
                }
            });

            chatSend.addActionListener(e -> {
                try {
                    sendMessage(new DrawCommand(DrawCommand.CommandType.CHAT, userName, chatMsg.getText()));
                    addMessage(chatConversation, chatMsg.getText() + " <me\n", true);
                    chatMsg.setText("");
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

        // Append chat message
        try {
            doc.insertString(doc.getLength(), message + "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public class UserPanel extends JPanel {
        private final DefaultTableModel tableModel;
        private final JTable userTable;
        private String managerUsername;

        public UserPanel() {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createTitledBorder("Connected Users"));

            // Column names for your table
            String[] columns = {"Username", "Kick"};
            tableModel = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            userTable = new JTable(tableModel);
            userTable.getColumnModel().getColumn(0).setCellRenderer(new BoldManagerRenderer());
            userTable.setRowHeight(25);
            userTable.setFillsViewportHeight(true);
            userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            userTable.getColumnModel().getColumn(0).setPreferredWidth(20); // first column
            userTable.getColumnModel().getColumn(1).setPreferredWidth(10);  // second column

            userTable.setPreferredScrollableViewportSize(
                    new Dimension(400, userTable.getRowHeight() * 3)
            );

            JScrollPane scrollPane = new JScrollPane(userTable);
            add(scrollPane, BorderLayout.CENTER);

            userTable.getTableHeader().setReorderingAllowed(false);
            userTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            userTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        }

        public void setManager(String username) {
            this.managerUsername = username;
            if (userTable != null) {
                userTable.repaint(); // Redraw the table
            }
        }

        public void addUser(String username) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if (tableModel.getValueAt(i, 0).equals(username)) {
                    return;
                }
            }
            tableModel.addRow(new Object[]{username, "Kick"});
        }

        public void removeUser(String username) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if (tableModel.getValueAt(i, 0).equals(username)) {
                    tableModel.removeRow(i);
                    break;
                }
            }
        }

        // Used to bold the manager's user name in the table
        private class BoldManagerRenderer extends DefaultTableCellRenderer {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {

                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String username = (String) value;

                // Check if this is the manager
                if (managerUsername != null && managerUsername.equals(username)) {
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else {
                    c.setFont(c.getFont().deriveFont(Font.PLAIN));
                }

                return c;
            }
        }

        public void clearUsers() {
            tableModel.setRowCount(0);
        }

        public void setUsers(List<String> usernames) {
            clearUsers();
            for (String name : usernames) {
                tableModel.addRow(new Object[]{name});
            }
        }

        public ArrayList<String> getUserList() {
            ArrayList<String> users = new ArrayList<>();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                users.add((String) tableModel.getValueAt(i, 0));
            }
            return users;
        }

    }

    public List<StrokeData> getStrokes() {
        return strokes;
    }

    public void setStrokes(List<StrokeData> sd) {
        this.strokes = sd;
    }

    public void addStroke(StrokeData s) {
        // strokes.add(s); // --- DELETE THIS LINE ---
        try {
            // Send the command *with* our local userName
            sendMessage(new DrawCommand(s, userName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void clearStrokes() {
        if(strokes != null){
            strokes.clear();
        }
    }

    public void clearShapes() {
        if(shapeList != null){
            shapeList.clear();
            intermediateShapes.clear();
        }
    }

    public void setManagerUsername(String username) {
        this.managerUsername = username;
    }

    public void clearStrings() {
        if(strings != null){
            strings.clear();
        }
    }

    public List<Shapes> getShapeList() {
        return shapeList;
    }

    public List<Shapes> getIntermediateShapes() {
        return intermediateShapes;
    }

    public void setShapeList(List<Shapes> shapeList) {
        this.shapeList = shapeList;
    }

    public void addShape(Shapes s) {
        // shapeList.add(s); // --- DELETE THIS LINE ---
        try {
            // Send the command *with* our local userName
            sendMessage(new DrawCommand(s, userName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public List<DrawText> getStrings() {
        return strings;
    }

    public void setStrings(List<DrawText> strings) {
        this.strings = strings;
    }

    public void addString(DrawText s) {
        // strings.add(s); // --- DELETE THIS LINE ---
        try {
            // Send the command *with* our local userName
            sendMessage(new DrawCommand(s, userName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public List<ChatData> getChatData() {
        return chats;
    }

    public void setChatData(List<ChatData> chatData) {
        this.chats = chatData;
    }

    public void addChat(ChatData s) {
        chats.add(s);
    }

    public void clearChats() {
        chats.clear();
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        this.isConnected = connected;

        SwingUtilities.invokeLater(() -> {
            if (connected) {
                connectionPanel.setConnect();
            } else {
                connectionPanel.setDisconnect();
                userPanel.clearUsers();
                if (canvas != null) {
                    canvas.clearWhiteBoard();
                }
                chatPanel.getChatConversation().setText("");
            }
        });
    }

    public static void main(String[] args) {
        // Check right number of arguments provided
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

        // Start the WhiteBoard client, running in a thread
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Join White Board");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(900, 700);

            JoinWhiteBoard joinWhiteBoard = new JoinWhiteBoard();
            frame.add(canvas = new Canvas(joinWhiteBoard), BorderLayout.CENTER);
            frame.add(joinWhiteBoard.createToolboxPanel(), BorderLayout.EAST);
            frame.setVisible(true);
        });
    }
}

