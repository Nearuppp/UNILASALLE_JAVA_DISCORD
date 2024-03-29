
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.io.*;

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

                JCheckBox registerCheckBox = new JCheckBox("Nouveau compte");
                registerCheckBox.setFont(new Font("SansSerif", Font.PLAIN, screenSize.height / 40));
                registerCheckBox.addActionListener(e -> {
                    boolean selected = registerCheckBox.isSelected();
                    passwordConfirmLabel.setVisible(selected);
                    passwordConfirmField.setVisible(selected);
                    loginFrame.pack();
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

                panel.add(userLabel);
                panel.add(userField);
                panel.add(passwordLabel);
                panel.add(passwordField);
                panel.add(passwordConfirmLabel);
                panel.add(passwordConfirmField);
                panel.add(new JLabel());
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

        // Ajouter une zone pour les utilisateurs connectés
        JTextArea usersArea = new JTextArea();
        usersArea.setEditable(false);
        usersArea.setFont(new Font("SansSerif", Font.PLAIN, screenSize.height / 60));
        frame.add(new JScrollPane(usersArea), BorderLayout.EAST);

        // Définir la taille préférée pour le JScrollPane
        JScrollPane usersScrollPane = new JScrollPane(usersArea);
        usersScrollPane.setPreferredSize(new Dimension(screenSize.width / 8, screenSize.height / 2));
        frame.add(usersScrollPane, BorderLayout.EAST);

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

        JButton disconnectButton = new JButton("Déconnexion");
        disconnectButton.setFont(new Font("SansSerif", Font.PLAIN, screenSize.height / 40));
        frame.add(disconnectButton, BorderLayout.NORTH);

        disconnectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 1. Disconnect the user from the server
                client.send("DISCONNECT " + identifiant);

                // 2. Close the chat window and return to the login page
                frame.dispose(); // Close the chat window
                loginFrame.setVisible(true); // Show the login page
            }
        });

        new Thread(() -> {
            try {
                String message;
                while ((message = client.receive()) != null) {

                    final String finalMessage = message;

                    if (finalMessage.startsWith("CONNECTED USERS:")) {
                        SwingUtilities.invokeLater(() -> {
                            usersArea.setText("Utilisateurs connectés : \n");
                            String[] users = finalMessage.substring("CONNECTED USERS:".length()).split(", ");
                            for (String user : users) {
                                usersArea.append(user + "\n");
                            }
                        });
                    } else if (finalMessage.startsWith("RESET")) {
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
