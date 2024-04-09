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
import java.net.SocketException;

// Définition de la classe ChatRoom
class ChatRoom {
    private String name;
    private List<PrintWriter> clients;

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

    public boolean connect(String username) {
        boolean connected = false;

        for (PrintWriter writer : clientWriters) {
            if (clientUsers.get(writer) != null && clientUsers.get(writer).equals(username)) {
                connected = true;
                break;
            }
        }
        return connected;
    }

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

            // Envoyer la liste des utilisateurs connectés et des salons disponibles à
            // intervalles réguliers
            new Thread(() -> {
                while (true) {
                    try {
                        String usersMessage = "CONNECTED USERS:";
                        for (String user : clientUsers.values()) {
                            usersMessage += ", " + user;
                        }

                        for (PrintWriter writer : clientWriters) {
                            writer.println(usersMessage);
                        }

                        String roomsMessage = "AVAILABLE ROOMS:";
                        try (PreparedStatement stmt = connection.prepareStatement(
                                "SELECT * FROM channels")) {
                            try (ResultSet rs = stmt.executeQuery()) {
                                while (rs.next()) {
                                    roomsMessage += ", " + rs.getString("name");
                                }
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                        for (PrintWriter writer : clientWriters) {
                            writer.println(roomsMessage);
                        }

                        Thread.sleep(5000); // Attendre 5 secondes

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        }
    }

    private int getUserId(String username) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT id FROM users WHERE username = ?")) {
            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                } else {
                    throw new SQLException("User not found: " + username);
                }
            }
        }
    }

    private String getUsername(int userId) {
        String username = null;
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT username FROM users WHERE id = ?")) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    username = rs.getString("username");
                }
            }
        } catch (SQLException e) {
            System.out.println("ERROR - COULD NOT RETRIEVE USERNAME");
        }
        return username;
    }

    private void handleClient(Socket clientSocket, PrintWriter out) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

            String inputLine;
            while ((inputLine = in.readLine()) != null) {

                String[] parts = inputLine.split(" ");
                String username;
                String password;
                if (parts.length < 3) {
                    username = "";
                    password = "";
                } else {
                    username = parts[1];
                    password = parts[2];
                }

                if (inputLine.startsWith("LOGIN")) {
                    try (PreparedStatement stmt = connection.prepareStatement(
                            "SELECT password FROM users WHERE username = ?")) {
                        stmt.setString(1, username);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next() && BCrypt.checkpw(password, rs.getString("password"))) {

                                if (!connect(username)) {

                                    out.println("LOGIN SUCCESS");
                                    clientUsers.put(out, username);
                                    System.out.println("Client connecté avec succès : " + username);

                                    // Envoyer l'historique des messages du salon "général" au client
                                    try (PreparedStatement stmt2 = connection.prepareStatement(
                                            "SELECT * FROM messages WHERE channel_id = ? ORDER BY timestamp ASC LIMIT 50")) {
                                        stmt2.setInt(1, 1); // ID du salon "général"
                                        try (ResultSet rs2 = stmt2.executeQuery()) {
                                            while (rs2.next()) {
                                                out.println(
                                                        rs2.getString("message"));
                                            }
                                            out.println(
                                                    "\n\n-- Bienvenue! Pour voir les commandes disponibles, tapez /help --\n\n");
                                        }
                                    }
                                } else {
                                    out.println("LOGIN FAILURE - USER ALREADY CONNECTED");
                                    System.out.println("La connexion a échoué pour le client : " + username);
                                }
                            } else {
                                out.println("LOGIN FAILURE");
                                System.out.println("La connexion a échoué pour le client : " + username);

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
                } else if (inputLine.startsWith("DISCONNECT")) {
                    // Retirer le client de son salon actuel
                    ChatRoom currentRoom = clientRooms.get(out);
                    currentRoom.getClients().remove(out);
                    System.out.println("Client déconnecté : " + clientUsers.get(out));

                    clientRooms.remove(out);
                    clientUsers.remove(out);

                } else if (inputLine.matches(".*\\s/HELP.*") || inputLine.matches(".*\\s/help.*")) {
                    out.println("Commandes disponibles : \n/nb_users : Affiche le nombre d'utilisateurs connectés\n");

                } else if (inputLine.startsWith("/pm_history ")) {
                    String recipientName = inputLine.split(" ")[1];
                    username = clientUsers.get(out);

                    int userId = getUserId(username);
                    int recipientId = getUserId(recipientName);

                    try (PreparedStatement stmt = connection.prepareStatement(
                            "SELECT * FROM private_messages WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?) ORDER BY timestamp DESC LIMIT 50")) {
                        stmt.setInt(1, userId);
                        stmt.setInt(2, recipientId);
                        stmt.setInt(3, recipientId);
                        stmt.setInt(4, userId);

                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                String message = rs.getString("message");
                                int senderId = rs.getInt("sender_id");
                                String senderName = getUsername(senderId);
                                out.println("PM_HISTORY " + senderName + ": " + message);
                            }
                        }
                    } catch (SQLException e) {
                        out.println("ERROR - COULD NOT RETRIEVE PM HISTORY");
                    }
                } else if (inputLine.startsWith("/PM, ")) {
                    String[] parts2 = inputLine.split(", ", 3);
                    if (parts2.length < 3) {
                        out.println("ERROR - INVALID PM FORMAT");
                    } else {
                        String recipientName = parts2[1];
                        String message = parts2[2];

                        // Récupérer l'ID de l'utilisateur à partir de son nom d'utilisateur
                        int senderId = getUserId(clientUsers.get(out));
                        int recipientId = getUserId(recipientName);

                        // Ajouter le message privé à la base de données
                        try (PreparedStatement stmt = connection.prepareStatement(
                                "INSERT INTO private_messages (sender_id, receiver_id, message, timestamp) VALUES (?, ?, ?, CURRENT_TIMESTAMP)")) {
                            stmt.setInt(1, senderId);
                            stmt.setInt(2, recipientId);
                            stmt.setString(3, message);
                            stmt.executeUpdate();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                        // Envoyer le message au destinataire
                        for (PrintWriter writer : clientWriters) {
                            if (clientUsers.get(writer).equals(recipientName)) {
                                writer.println("PM: " + clientUsers.get(out) + ": " + message);
                            }
                        }
                    }
                }

                else if (inputLine.matches("admin : /create .*")) {
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
                                int channelId = rs.getInt("id"); // Récupérer l'ID du salon

                                // Supprimer tous les messages du salon
                                String sql = "DELETE FROM messages WHERE channel_id = ?";
                                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                                    pstmt.setInt(1, channelId);
                                    pstmt.executeUpdate();
                                }

                                try (PreparedStatement stmt2 = connection.prepareStatement(
                                        "DELETE FROM channels WHERE name = ?")) {
                                    stmt2.setString(1, roomName);
                                    stmt2.executeUpdate();
                                    out.println("Le salon " + roomName + " a été supprimé.");

                                    // Déplacer les clients vers le salon "général" ou les déconnecter
                                    ChatRoom deletedRoom = rooms.get(roomName);
                                    if (deletedRoom != null) {
                                        for (PrintWriter client : deletedRoom.getClients()) {
                                            // Récupérer le salon "général" de la base de données
                                            ChatRoom generalRoom = rooms.get("général");
                                            if (generalRoom != null) {
                                                // Pour déplacer les clients vers le salon "général"
                                                generalRoom.getClients().add(client);
                                                clientRooms.put(client, generalRoom);

                                                client.println("RESET");

                                                // Envoyer l'historique des messages dans le général aux clients
                                                try (PreparedStatement stmt3 = connection.prepareStatement(
                                                        "SELECT * FROM messages WHERE channel_id = ? ORDER BY timestamp ASC LIMIT 50")) {
                                                    stmt3.setInt(1, 1); // ID du salon "général"
                                                    try (ResultSet rs3 = stmt3.executeQuery()) {
                                                        while (rs3.next()) {
                                                            client.println(
                                                                    rs3.getString("message"));
                                                        }
                                                    }
                                                }

                                                client.println("ROOM DELETED ");

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
                } else if ((inputLine.startsWith("/JOIN"))) {
                    String roomName = parts[1];

                    boolean roomExists = false;
                    try (PreparedStatement stmt = connection.prepareStatement(
                            "SELECT * FROM channels WHERE name = ?")) {
                        stmt.setString(1, roomName);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                roomExists = true;
                            }
                        }
                    }

                    if (roomExists) {
                        // Retirer le client de son salon actuel
                        // Avant de retirer le client de son salon actuel, vérifier si le salon est null
                        ChatRoom currentRoom = clientRooms.get(out);
                        if (currentRoom != null) {
                            currentRoom.getClients().remove(out);
                        }

                        // Ajouter le client au nouveau salon
                        ChatRoom newRoom = rooms.get(roomName);
                        if (newRoom == null) {
                            newRoom = new ChatRoom(roomName);
                            rooms.put(roomName, newRoom);
                        }
                        newRoom.getClients().add(out);
                        clientRooms.put(out, newRoom);

                        // Récupérer l'ID du canal à partir de son nom
                        int channelId = -1;
                        try (PreparedStatement stmt2 = connection.prepareStatement(
                                "SELECT id FROM channels WHERE name = ?")) {
                            stmt2.setString(1, roomName);
                            try (ResultSet rs2 = stmt2.executeQuery()) {
                                if (rs2.next()) {
                                    channelId = rs2.getInt("id");
                                }
                            }
                        }

                        out.println("RESET");

                        // Envoyer l'historique des messages du salon
                        if (channelId != -1) {
                            try (PreparedStatement stmt3 = connection.prepareStatement(
                                    "SELECT * FROM messages WHERE channel_id = ? ORDER BY timestamp ASC LIMIT 50")) {
                                stmt3.setInt(1, channelId);
                                try (ResultSet rs3 = stmt3.executeQuery()) {
                                    while (rs3.next()) {
                                        out.println(
                                                rs3.getString("message"));
                                    }
                                }
                            }
                        }

                        out.println("JOINED ROOM " + roomName);

                    } else {
                        out.println("Le salon " + roomName + " n'existe pas.");
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
        } catch (

        SocketException e) {
            System.out.println("Client déconnecté : " + clientUsers.get(out));
            // Retirer le client de son salon actuel
            ChatRoom currentRoom = clientRooms.get(out);
            if (currentRoom != null) {
                currentRoom.getClients().remove(out);
            }
            clientRooms.remove(out);
            clientUsers.remove(out);
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        } finally {
            // Retirer le client de son salon actuel
            ChatRoom currentRoom = clientRooms.get(out);
            if (currentRoom != null) {
                currentRoom.getClients().remove(out);
            }

            System.out.println("Client déconnecté : " + clientUsers.get(out));

            clientRooms.remove(out);
            clientUsers.remove(out);
        }
    }

    public static void main(String[] args) throws IOException, SQLException {
        Server server = new Server(12345, "jdbc:mysql://localhost:3306/database", "root", "admin1234");
        server.start();
    }
}
