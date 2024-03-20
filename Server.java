import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.mindrot.jbcrypt.BCrypt;

// Définition de la classe ChatRoom
class ChatRoom {
    private String name;
    private List<PrintWriter> clients;
    // Ajoutez d'autres attributs si nécessaire

    public ChatRoom(String name) {
        this.name = name;
        this.clients = new ArrayList<>();
    }

    public List<PrintWriter> getClients() {
        return clients;
    }

    public String getName() {
        return this.name;
    }
}

public class Server {
    private ServerSocket serverSocket;
    private Connection connection;

    private Set<PrintWriter> clientWriters = new HashSet<>();
    private Map<PrintWriter, String> clientUsers = new HashMap<>();

    private Map<PrintWriter, ChatRoom> clientRooms = new HashMap<>();
    private Map<String, ChatRoom> rooms = new HashMap<>();

    public Server(int port, String dbUrl, String dbUser, String dbPassword) throws IOException, SQLException {
        serverSocket = new ServerSocket(port);
        connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        rooms.put("général", new ChatRoom("général"));

    }

    public void start() throws IOException {

        // Charger tous les salons de la base de données dans la mémoire du serveur
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM channels")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String roomName = rs.getString("name");
                    rooms.put(roomName, new ChatRoom(roomName));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        while (true) {
            Socket clientSocket = serverSocket.accept();
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Ajouter le client au salon "général"
            ChatRoom generalRoom = rooms.get("général");
            generalRoom.getClients().add(out);
            clientRooms.put(out, generalRoom);

            clientWriters.add(out);

            new Thread(() -> handleClient(clientSocket, out)).start();

            // Envoyer la liste des utilisateurs connectés à intervalles réguliers
            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(5000); // Attendre 5 secondes

                        String usersMessage = "CONNECTED USERS:";
                        for (String user : clientUsers.values()) {
                            usersMessage += ", " + user;
                        }

                        for (PrintWriter writer : clientWriters) {
                            writer.println(usersMessage);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        }
    }

    private void handleClient(Socket clientSocket, PrintWriter out) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                String[] parts = inputLine.split(" ");
                String username = parts[1];
                String password = parts[2];

                if (inputLine.startsWith("LOGIN")) {
                    try (PreparedStatement stmt = connection.prepareStatement(
                            "SELECT password FROM users WHERE username = ?")) {
                        stmt.setString(1, username);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next() && BCrypt.checkpw(password, rs.getString("password"))) {
                                out.println("LOGIN SUCCESS");
                                clientUsers.put(out, username);
                            } else {
                                out.println("LOGIN FAILURE");
                            }
                        }
                    }
                } else if (inputLine.startsWith("REGISTER")) {
                    try (PreparedStatement stmt = connection.prepareStatement(
                            "INSERT INTO users (username, password) VALUES (?, ?)")) {
                        stmt.setString(1, username);
                        stmt.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()));
                        stmt.executeUpdate();
                        out.println("REGISTER SUCCESS");
                    } catch (SQLIntegrityConstraintViolationException e) {
                        out.println("REGISTER FAILURE - USERNAME ALREADY EXISTS");
                    } catch (SQLException e) {
                        out.println("REGISTER FAILURE");
                    }
                } else if (inputLine.matches(".*\\s/HELP.*") || inputLine.matches(".*\\s/help.*")) {
                    out.println("Commandes disponibles : \n/nb_users : Affiche le nombre d'utilisateurs connectés\n");

                } else if (inputLine.matches("admin : /create .*")) {
                    String roomName = parts[3];

                    // Vérifier si le salon existe dans la base de données
                    try (PreparedStatement stmt = connection.prepareStatement(
                            "SELECT * FROM channels WHERE name = ?")) {
                        stmt.setString(1, roomName);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                out.println("Le salon " + roomName + " existe déjà.");
                            } else {
                                try (PreparedStatement stmt2 = connection.prepareStatement(
                                        "INSERT INTO channels (name) VALUES (?)")) {
                                    stmt2.setString(1, roomName);
                                    stmt2.executeUpdate();
                                    out.println("Le salon " + roomName + " a été créé.");
                                }
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } else if (inputLine.matches("admin : /delete .*")) {
                    String roomName = parts[3];

                    // Vérifier si le salon existe dans la base de données
                    try (PreparedStatement stmt = connection.prepareStatement(
                            "SELECT * FROM channels WHERE name = ?")) {
                        stmt.setString(1, roomName);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                try (PreparedStatement stmt2 = connection.prepareStatement(
                                        "DELETE FROM channels WHERE name = ?")) {
                                    stmt2.setString(1, roomName);
                                    stmt2.executeUpdate();
                                    out.println("Le salon " + roomName + " a été supprimé.");
                                }
                            } else {
                                out.println("Le salon " + roomName + " n'existe pas.");
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } else if (inputLine.matches(".*/list_channels.*")) {
                    try (PreparedStatement stmt = connection.prepareStatement(
                            "SELECT * FROM channels")) {
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                out.println("- " + rs.getString("name"));
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } else if (inputLine.matches(".*\\/join.*") || inputLine.matches(".*\\/JOIN.*")) {
                    String roomName = parts[3];

                    // Vérifier si le salon existe dans la base de données
                    try (PreparedStatement stmt = connection.prepareStatement(
                            "SELECT * FROM channels WHERE name = ?")) {
                        stmt.setString(1, roomName);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                // Retirer le client de son salon actuel
                                ChatRoom currentRoom = clientRooms.get(out);
                                currentRoom.getClients().remove(out);

                                // Ajouter le client au nouveau salon
                                ChatRoom newRoom = rooms.get(roomName);
                                newRoom.getClients().add(out);
                                clientRooms.put(out, newRoom);

                                out.println("JOINED ROOM " + roomName);

                                // Récupérer l'ID du canal à partir de son nom
                                int channelId = -1;
                                try (PreparedStatement stmt2 = connection.prepareStatement(
                                        "SELECT id FROM channels WHERE name = ?")) {
                                    stmt2.setString(1, roomName);
                                    try (ResultSet rs2 = stmt.executeQuery()) {
                                        if (rs2.next()) {
                                            channelId = rs.getInt("id");
                                        }
                                    }
                                }

                                // Envoyer l'historique des messages du salon
                                if (channelId != -1) {
                                    try (PreparedStatement stmt3 = connection.prepareStatement(
                                            "SELECT * FROM messages WHERE channel_id = ? ORDER BY timestamp")) {
                                        stmt3.setInt(1, channelId);
                                        try (ResultSet rs3 = stmt3.executeQuery()) {
                                            while (rs3.next()) {
                                                out.println(
                                                        rs3.getString("username") + ": " + rs3.getString("message"));
                                            }
                                        }
                                    }
                                }

                            } else {
                                out.println("Le salon " + roomName + " n'existe pas.");
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } else if (inputLine.matches(".*\\s/NB_USERS.*") || inputLine.matches(".*\\s/nb_users.*")) {
                    out.println(clientUsers.size() + " UTILISATEURS CONNECTÉ(S)");
                } else {
                    System.out.println("Received message: " + inputLine);

                    // Récupérer l'ID de l'utilisateur à partir de son nom d'utilisateur
                    int userId = -1;
                    try (PreparedStatement stmt = connection.prepareStatement(
                            "SELECT id FROM users WHERE username = ?")) {
                        stmt.setString(1, clientUsers.get(out));
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                userId = rs.getInt("id");
                            }
                        }
                    }

                    // Récupérer l'ID du canal à partir de son nom
                    int channelId = -1;
                    try (PreparedStatement stmt = connection.prepareStatement(
                            "SELECT id FROM channels WHERE name = ?")) {
                        stmt.setString(1, clientRooms.get(out).getName());
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                channelId = rs.getInt("id");
                            }
                        }
                    }

                    // Ajouter le message à la base de données
                    if (userId != -1 && channelId != -1) {
                        try (PreparedStatement stmt = connection.prepareStatement(
                                "INSERT INTO messages (user_id, channel_id, message) VALUES (?, ?, ?)")) {
                            stmt.setInt(1, userId);
                            stmt.setInt(2, channelId);
                            stmt.setString(3, inputLine);
                            stmt.executeUpdate();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }

                    // Diffuser le message aux clients du salon
                    ChatRoom currentRoom = clientRooms.get(out);
                    for (PrintWriter writer : currentRoom.getClients()) {
                        writer.println(inputLine);
                    }
                }
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        } finally {
            // Retirer le client de son salon actuel
            ChatRoom currentRoom = clientRooms.get(out);
            currentRoom.getClients().remove(out);

            clientRooms.remove(out);
            clientUsers.remove(out);
        }
    }

    public static void main(String[] args) throws IOException, SQLException {
        Server server = new Server(12345, "jdbc:mysql://localhost:3306/database", "root", "admin1234");
        server.start();
    }
}