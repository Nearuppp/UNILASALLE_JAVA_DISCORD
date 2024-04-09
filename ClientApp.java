
import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.util.Arrays;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;

public class ClientApp {

    static String identifiant;

    private static void showMessageDialog(Component parent, String message, String title, int messageType,
            Dimension screenSize) {
        JTextArea textArea = new JTextArea(message);
        textArea.setFont(new Font("SansSerif", Font.PLAIN, screenSize.height / 40));
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(screenSize.width / 4, screenSize.height / 4));

        JOptionPane.showMessageDialog(parent, scrollPane, title, messageType);
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            try {
                // Créer le client
                Client client = new Client("localhost", 12345);

                // Obtenir la taille de l'écran
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

                // Créer l'interface graphique
                JFrame loginFrame = new JFrame("Login");
                loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                loginFrame.setSize(screenSize.width / 2, screenSize.height / 2);
                loginFrame.setMinimumSize(new Dimension(screenSize.width / 3, screenSize.height / 4));

                JPanel panel = new JPanel(new GridLayout(6, 2));
                panel.setBackground(Color.white);

                JLabel userLabel = new JLabel("Identifiant");
                userLabel.setFont(new Font("SansSerif", Font.PLAIN, screenSize.height / 40));
                JTextField userField = new JTextField();
                userField.setFont(new Font("SansSerif", Font.PLAIN, screenSize.height / 40));
                JLabel passwordLabel = new JLabel("Mot de passe");
                passwordLabel.setFont(new Font("SansSerif", Font.PLAIN, screenSize.height / 40));
                JPasswordField passwordField = new JPasswordField();
                passwordField.setFont(new Font("SansSerif", Font.PLAIN, screenSize.height / 40));

                JLabel passwordConfirmLabel = new JLabel("Confirmer le mot de passe     ");
                passwordConfirmLabel.setFont(new Font("SansSerif", Font.PLAIN, screenSize.height / 40));
                JPasswordField passwordConfirmField = new JPasswordField();
                passwordConfirmField.setFont(new Font("SansSerif", Font.PLAIN, screenSize.height / 40));
                passwordConfirmLabel.setVisible(false);
                passwordConfirmField.setVisible(false);

                JToggleButton registerCheckBox = new JToggleButton("Nouveau compte");
                registerCheckBox.setBackground(Color.GREEN);
                registerCheckBox.setFont(new Font("SansSerif", Font.PLAIN, screenSize.height / 40));
                registerCheckBox.addActionListener(e -> {
                    boolean selected = registerCheckBox.isSelected();
                    passwordConfirmLabel.setVisible(selected);
                    passwordConfirmField.setVisible(selected);
                    loginFrame.pack();
                    if (selected) {
                        registerCheckBox.setBackground(Color.RED);
                        registerCheckBox.setOpaque(true);
                    } else {
                        registerCheckBox.setBackground(Color.GREEN);
                        registerCheckBox.setOpaque(true);
                    }

                });

                JButton loginButton = new JButton("Se connecter");
                loginButton.setFont(new Font("SansSerif", Font.PLAIN, screenSize.height / 40));
                loginButton.addActionListener(e -> {
                    String username = userField.getText();
                    String password = new String(passwordField.getPassword());

                    if (registerCheckBox.isSelected()) {
                        if (Arrays.equals(passwordField.getPassword(), passwordConfirmField.getPassword())) {
                            client.send("REGISTER " + username + " " + password);
                        } else {
                            showMessageDialog(loginFrame, "Les mots de passe ne correspondent pas!", "Erreur",
                                    JOptionPane.ERROR_MESSAGE, screenSize);
                        }
                    } else {
                        client.send("LOGIN " + username + " " + password);
                    }
                });

                passwordField.addActionListener(e -> {
                    String username = userField.getText();
                    String password = new String(passwordField.getPassword());

                    if (registerCheckBox.isSelected()) {
                        if (Arrays.equals(passwordField.getPassword(), passwordConfirmField.getPassword())) {
                            client.send("REGISTER " + username + " " + password);
                        } else {
                            showMessageDialog(loginFrame, "Les mots de passe ne correspondent pas!", "Erreur",
                                    JOptionPane.ERROR_MESSAGE, screenSize);
                        }
                    } else {
                        client.send("LOGIN " + username + " " + password);
                    }
                });

                passwordConfirmField.addActionListener(e -> {
                    String username = userField.getText();
                    String password = new String(passwordField.getPassword());

                    if (Arrays.equals(passwordField.getPassword(), passwordConfirmField.getPassword())) {
                        client.send("REGISTER " + username + " " + password);
                    } else {
                        showMessageDialog(loginFrame, "Les mots de passe ne correspondent pas!", "Erreur",
                                JOptionPane.ERROR_MESSAGE, screenSize);
                    }
                });

                registerCheckBox.addActionListener(e -> {
                    if (registerCheckBox.isSelected()) {
                        loginButton.setText("S'enregister");
                    } else {
                        loginButton.setText("Se connecter");
                    }
                });
                JButton buttonTheme = new JButton("Change color");
                buttonTheme.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        // Check the panel's current background color
                        if (panel.getBackground() == Color.white) {
                            // If it's white, change it to black
                            panel.setBackground(Color.black);
                            // Change the button's text color to white and background to black
                            buttonTheme.setForeground(Color.white);
                            buttonTheme.setBackground(Color.black);
                            loginButton.setForeground(Color.white);
                            loginButton.setBackground(Color.black);
                            // Change the JLabel's text color to white
                            userLabel.setForeground(Color.white);
                            passwordLabel.setForeground(Color.white);
                            // Change the JTextField's text color to white and background to black
                            userField.setForeground(Color.white);
                            userField.setBackground(Color.black);
                            passwordField.setForeground(Color.white);
                            passwordField.setBackground(Color.black);
                            passwordConfirmLabel.setForeground(Color.white);
                            passwordConfirmLabel.setBackground(Color.black);
                            passwordConfirmField.setForeground(Color.white);
                            passwordConfirmField.setBackground(Color.black);
                        } else {
                            // If it's not white (i.e., it's black), change it to white
                            panel.setBackground(Color.white);
                            // Change the button's text color to black and background to white
                            buttonTheme.setForeground(Color.black);
                            buttonTheme.setBackground(Color.white);
                            loginButton.setForeground(Color.black);
                            loginButton.setBackground(Color.white);
                            // Change the JLabel's text color to black
                            userLabel.setForeground(Color.black);
                            passwordLabel.setForeground(Color.black);
                            // Change the JTextField's text color to black and background to white
                            userField.setForeground(Color.black);
                            userField.setBackground(Color.white);
                            passwordField.setForeground(Color.black);
                            passwordField.setBackground(Color.white);
                            passwordConfirmLabel.setForeground(Color.black);
                            passwordConfirmLabel.setBackground(Color.white);
                            passwordConfirmField.setForeground(Color.black);
                            passwordConfirmField.setBackground(Color.white);
                        }
                    }
                });

                panel.add(userLabel);
                panel.add(userField);
                panel.add(passwordLabel);
                panel.add(passwordField);
                panel.add(passwordConfirmLabel);
                panel.add(passwordConfirmField);
                panel.add(buttonTheme);
                panel.add(new JLabel());
                panel.add(new JLabel());
                panel.add(new JLabel());
                panel.add(registerCheckBox);
                panel.add(loginButton);

                loginFrame.add(panel);
                loginFrame.pack();
                loginFrame.setLocationRelativeTo(null);
                loginFrame.setVisible(true);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String message;
                            boolean running = true;
                            while (running && ((message = client.receive()) != null)) {
                                if (message.startsWith("LOGIN SUCCESS")) {
                                    loginFrame.setVisible(false);
                                    createChatWindow(client, loginFrame);
                                    identifiant = userField.getText();
                                    running = false;
                                } else if (message.startsWith("LOGIN FAILURE - USER ALREADY CONNECTED")) {
                                    showMessageDialog(loginFrame, "L'utilisateur est déjà connecté!",
                                            "Erreur", JOptionPane.ERROR_MESSAGE, screenSize);
                                } else if (message.startsWith("LOGIN FAILURE")) {
                                    showMessageDialog(loginFrame, "Les informations de connexion sont incorrectes!",
                                            "Erreur", JOptionPane.ERROR_MESSAGE, screenSize);
                                } else if (message.startsWith("REGISTER SUCCESS")) {
                                    showMessageDialog(loginFrame, "Utilisateur ajouté avec succès!", "Confirmation",
                                            JOptionPane.INFORMATION_MESSAGE, screenSize);
                                } else if (message.startsWith("REGISTER FAILURE - USERNAME ALREADY EXISTS")) {
                                    showMessageDialog(loginFrame, "Ce nom d'utilisateur est déjà pris !", "Erreur",
                                            JOptionPane.ERROR_MESSAGE, screenSize);
                                } else if (message.startsWith("REGISTER FAILURE")) {
                                    showMessageDialog(loginFrame, "Erreur lors de l'ajout de l'utilisateur!", "Erreur",
                                            JOptionPane.ERROR_MESSAGE, screenSize);
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static void createChatWindow(Client client, JFrame loginFrame) {
        // Obtenir la taille de l'écran
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        JFrame frame = new JFrame("Chat");
        frame.setForeground(Color.black);
        frame.setBackground(Color.white);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize((screenSize.width * 2) / 3, screenSize.height / 2);
        frame.setMinimumSize(new Dimension(screenSize.width / 4, screenSize.height / 4));
        frame.setLocationRelativeTo(null);
        frame.setTitle("Chat - Général");

        JTextArea messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setFont(new Font("SansSerif", Font.PLAIN, screenSize.height / 40));
        messageArea.setLineWrap(true); // Ajout de cette ligne
        messageArea.setWrapStyleWord(true); // Ajout de cette ligne
        frame.add(new JScrollPane(messageArea), BorderLayout.CENTER);

        JTextField textField = new JTextField();
        textField.setFont(new Font("SansSerif", Font.PLAIN, screenSize.height / 40));
        frame.add(textField, BorderLayout.SOUTH);

        DefaultListModel<String> usersModel = new DefaultListModel<>();
        JList<String> usersList = new JList<>(usersModel);
        usersList.setFont(new Font("SansSerif", Font.PLAIN, screenSize.height / 60));
        frame.add(new JScrollPane(usersList), BorderLayout.EAST);

        // Create a new JFrame for the private chat
        JFrame privateChatFrame = new JFrame("Messages privés");
        privateChatFrame.setForeground(frame.getForeground());
        privateChatFrame.setBackground(frame.getBackground());
        privateChatFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        privateChatFrame.setSize(frame.getWidth() / 2, frame.getHeight() / 2);
        privateChatFrame.setLocationRelativeTo(frame);

        // Add a JTextArea for the chat messages
        JTextArea privateChatArea = new JTextArea();
        privateChatArea.setEditable(false);
        privateChatArea.setFont(messageArea.getFont());
        privateChatArea.setLineWrap(true);
        privateChatArea.setWrapStyleWord(true);
        privateChatFrame.add(new JScrollPane(privateChatArea), BorderLayout.CENTER);

        // Add a JTextField for inputting messages
        JTextField privateChatField = new JTextField();
        privateChatField.setFont(textField.getFont());
        privateChatFrame.add(privateChatField, BorderLayout.SOUTH);

        // Ajoutez un MouseListener à usersList
        usersList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) { // Double-click detected
                    int index = usersList.locationToIndex(evt.getPoint());
                    String clickedUser = usersModel.getElementAt(index);

                    client.send("/pm_history " + clickedUser);

                    // Send the message when Enter is pressed
                    privateChatField.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            client.send("/pm " + clickedUser + " " + privateChatField.getText());
                            privateChatField.setText("");
                        }
                    });

                    // Make the private chat window visible
                    privateChatFrame.setVisible(true);
                }
            }
        });

        // Remplacez JTextArea par JList et DefaultListModel
        DefaultListModel<String> channelsModel = new DefaultListModel<>();
        JList<String> channelsList = new JList<>(channelsModel);
        channelsList.setFont(new Font("SansSerif", Font.PLAIN, screenSize.height / 60));

        // Ajoutez un MouseListener à channelsList
        channelsList.addMouseListener((MouseListener) new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) { // Double-click detected
                    int index = channelsList.locationToIndex(evt.getPoint());
                    client.send("/JOIN " + channelsModel.getElementAt(index));
                }
            }
        });

        // Définir la taille préférée pour le JScrollPane
        JScrollPane usersScrollPane = new JScrollPane(usersList);
        usersScrollPane.setPreferredSize(new Dimension(screenSize.width / 8, screenSize.height / 2));
        frame.add(usersScrollPane, BorderLayout.EAST);

        // Définir la taille préférée pour le channelslist
        JScrollPane channelsScrollPane = new JScrollPane(channelsList);
        channelsScrollPane.setPreferredSize(new Dimension(screenSize.width / 8, screenSize.height / 2));
        frame.add(channelsScrollPane, BorderLayout.WEST);

        // Envoyer le message lorsque l'utilisateur appuie sur Entrée
        textField.addActionListener((ActionListener) new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                client.send(identifiant + " : " + textField.getText());
                textField.setText("");
            }
        });

        // Rendre la fenêtre visible après avoir ajouté tous les composants
        frame.setVisible(true);

        // Create a new panel with GridLayout
        JPanel northPanel = new JPanel(new GridLayout(1, 2));

        // Add the disconnectButton to the northPanel
        JButton disconnectButton = new JButton("Déconnexion");
        disconnectButton.setFont(new Font("SansSerif", Font.PLAIN, screenSize.height / 40));
        northPanel.add(disconnectButton);
        disconnectButton.setForeground(Color.black);
        disconnectButton.setBackground(Color.white);

        // Add the buttonTheme to the northPanel
        JButton buttonTheme = new JButton("Change color");
        buttonTheme.setFont(new Font("SansSerif", Font.PLAIN, screenSize.height / 40));
        northPanel.add(buttonTheme);
        buttonTheme.setForeground(Color.black);
        buttonTheme.setBackground(Color.white);

        // Add the northPanel to the NORTH of the frame
        frame.add(northPanel, BorderLayout.NORTH);
        // Create the drop-down menu
        String[] cities = { "New York", "London", "Paris", "Tokyo", "Sydney" };
        JComboBox<String> cityDropDown = new JComboBox<>(cities);
        cityDropDown.setForeground(Color.black);
        cityDropDown.setBackground(Color.white);
        cityDropDown.setSelectedIndex(0); // Set the default selected city

        // Add action listener to the drop-down menu
        cityDropDown.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String apiKey = "36019f91e01af9ac650c64c217110c4c";
                // Get the selected city
                String selectedCity = (String) cityDropDown.getSelectedItem();
                // Print the selected city
                System.out.println("Selected City: " + selectedCity);

                String apiUrl = "http://api.openweathermap.org/data/2.5/weather?q=" + selectedCity + "&appid=" + apiKey;

                try {
                    URL url = new URL(apiUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONObject main = jsonResponse.getJSONObject("main");
                    double temperatureKelvin = main.getDouble("temp");

                    // Convert temperature from Kelvin to Celsius
                    int temperatureCelsius = (int) (temperatureKelvin - 273.15);

                    double humidity = main.getDouble("humidity");

                    System.out.println("Temperature: " + temperatureCelsius + " Celsius");
                    System.out.println("Humidity: " + humidity + "%");
                    SwingUtilities.invokeLater(() -> {
                        messageArea.append("\n\n-- Ville selectionne : " + selectedCity + "\nTemperature: "
                                + temperatureCelsius + " Celsius\n" + "Humidity: " + humidity + "\n\n");
                        messageArea.setCaretPosition(messageArea.getDocument().getLength());
                    });

                } catch (IOException j) {
                    j.printStackTrace();
                }

            }
        });

        // Add the drop-down menu to the northPanel
        northPanel.add(cityDropDown);
        disconnectButton.addActionListener((ActionListener) new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 1. Disconnect the user from the server
                client.send("DISCONNECT");

                // 2. Close the chat window and return to the login page
                frame.dispose(); // Close the chat window

                try {
                    String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString();
                    String classpath = System.getProperty("java.class.path");
                    String className = ClientApp.class.getName();

                    ProcessBuilder builder = new ProcessBuilder(javaBin, "-cp", classpath, className);
                    builder.start();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

            }
        });

        buttonTheme.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Check the panel's current background color
                if (frame.getBackground() == Color.white) {
                    disconnectButton.setForeground(Color.white);
                    disconnectButton.setBackground(Color.black);

                    cityDropDown.setForeground(Color.white);
                    cityDropDown.setBackground(Color.black);

                    buttonTheme.setForeground(Color.white);
                    buttonTheme.setBackground(Color.black);

                    messageArea.setForeground(Color.white);
                    messageArea.setBackground(Color.black);

                    textField.setForeground(Color.white);
                    textField.setBackground(Color.black);

                    usersList.setForeground(Color.white);
                    usersList.setBackground(Color.black);

                    channelsList.setForeground(Color.white);
                    channelsList.setBackground(Color.black);
                    // Change the chat window's text color to white and background to black
                    frame.setForeground(Color.white);
                    frame.setBackground(Color.black);
                } else {
                    disconnectButton.setForeground(Color.black);
                    disconnectButton.setBackground(Color.white);

                    cityDropDown.setForeground(Color.black);
                    cityDropDown.setBackground(Color.white);

                    buttonTheme.setForeground(Color.black);
                    buttonTheme.setBackground(Color.white);

                    messageArea.setForeground(Color.black);
                    messageArea.setBackground(Color.white);

                    textField.setForeground(Color.black);
                    textField.setBackground(Color.white);

                    usersList.setForeground(Color.black);
                    usersList.setBackground(Color.white);

                    channelsList.setForeground(Color.black);
                    channelsList.setBackground(Color.white);
                    // Change the chat window's text color to black and background to white
                    frame.setForeground(Color.black);
                    frame.setBackground(Color.white);
                }
            }
        });

        new Thread(() -> {
            try {
                String message;
                while ((message = client.receive()) != null) {

                    final String finalMessage = message;

                    if (finalMessage.startsWith("CONNECTED USERS:")) {
                        SwingUtilities.invokeLater(() -> {
                            usersModel.clear();
                            String[] channels = finalMessage.substring("AVAILABLE ROOMS:".length()).split(", ");
                            for (String channel : channels) {
                                usersModel.addElement(channel);
                            }
                        });
                    }

                    else if (finalMessage.startsWith("AVAILABLE ROOMS:")) {
                        SwingUtilities.invokeLater(() -> {
                            channelsModel.clear();
                            String[] channels = finalMessage.substring("AVAILABLE ROOMS:".length()).split(", ");
                            for (String channel : channels) {
                                channelsModel.addElement(channel);
                            }
                        });
                    }

                    else if (finalMessage.startsWith("RESET")) {
                        messageArea.setText(""); // Effacer la zone de message
                    } else if (finalMessage.startsWith("JOINED ROOM ")) {
                        SwingUtilities.invokeLater(() -> {
                            frame.setTitle("Chat - " + finalMessage.substring(12));
                            messageArea
                                    .append("\n\n-- Vous avez rejoint le salon " + finalMessage.substring(12)
                                            + " --\n\n");
                            messageArea.setCaretPosition(messageArea.getDocument().getLength());
                        });
                    } else if (finalMessage.startsWith("ROOM DELETED ")) {
                        String deletedRoom = finalMessage.substring("ROOM DELETED ".length());
                        SwingUtilities.invokeLater(() -> {

                            frame.setTitle("Chat - Général");
                            messageArea.append("\n\n-- Le salon " + deletedRoom
                                    + " a été supprimé. Vous avez été déplacé vers le salon général --\n\n");
                            messageArea.setCaretPosition(messageArea.getDocument().getLength());
                        });

                    } else {
                        System.out.println("Received message: " + message);
                        messageArea.append(message + "\n");
                        messageArea.setCaretPosition(messageArea.getDocument().getLength());
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
