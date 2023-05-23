import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private ServerSocket serverSocket;
    private List<ClientHandler> clients;

    public Server(int port) {
        try {
            serverSocket = new ServerSocket(port);
            clients = new ArrayList<>();
            System.out.println("Server started on port " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getInetAddress().getHostAddress());
                ClientHandler clientHandler = new ClientHandler(socket);
                clients.add(clientHandler);
                clientHandler.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                try {
                    client.sendMessage(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    public static void main(String[] args) {
        int port = 1234;
        Server server = new Server(port);
        server.start();
    }

    private class ClientHandler extends Thread {
        private Socket socket;
        private ObjectInputStream inputStream;
        private ObjectOutputStream outputStream;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                outputStream = new ObjectOutputStream(socket.getOutputStream());
                inputStream = new ObjectInputStream(socket.getInputStream());

                sendMessage("Enter your username: ");
                username = (String) inputStream.readObject();

                String welcomeMessage = "Welcome, " + username + "!";
                sendMessage(welcomeMessage);
                broadcastMessage(welcomeMessage, this);

                String message;
                do {
                    message = (String) inputStream.readObject();
                    if (message.equalsIgnoreCase("[see]")) {
                        listOnlineUsers();
                    } else if (message.startsWith("[private]")) {
                        sendPrivateMessage(message);
                    } else {
                        broadcastMessage(username + ": " + message, this);
                    }
                } while (!message.equalsIgnoreCase("exit"));

                broadcastMessage(username + " has left the chat.", this);
                removeClient(this);
                close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        public void sendMessage(String message) throws IOException {
            outputStream.writeObject(message);
            outputStream.flush();
        }

        private void listOnlineUsers() throws IOException {
            StringBuilder userList = new StringBuilder();
            userList.append("Online users:\n");
            for (ClientHandler client : clients) {
                userList.append(client.username).append("\n");
            }
            sendMessage(userList.toString());
        }

        private void sendPrivateMessage(String message) throws IOException {
            String[] parts = message.split(" ", 3);
            String recipient = parts[1];
            String privateMessage = parts[2];
            for (ClientHandler client : clients) {
                if (client.username.equals(recipient)) {
                    client.sendMessage("[Private from " + username + "]: " + privateMessage);
                    break;
                }
            }
        }

        private void close() {
            try {
                if (outputStream != null)
                    outputStream.close();
                if (inputStream != null)
                    inputStream.close();
                if (socket != null)
                    socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}