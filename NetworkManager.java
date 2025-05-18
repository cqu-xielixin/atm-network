package window1;

import java.io.*;
import java.net.*;

public class NetworkManager {
    private static NetworkManager instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private NetworkManager() {
        try {
            socket = new Socket("192.168.43.232", 2525);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.err.println("连接失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    public static synchronized NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public String receiveMessage() {
        if (in != null) {
            try {
                return in.readLine();
            } catch (IOException e) {
                System.err.println("接收失败：" + e.getMessage());
                e.printStackTrace();
            }
        }
        return null;
    }

    public void closeConnection() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}