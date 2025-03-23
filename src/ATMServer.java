import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ATMServer {
    private static final int PORT = 2525;
    private static final String DB_URL = "jdbc:sqlite:F:/soft/SQlite/atm.db";

    public static void main(String[] args) throws IOException, SQLException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server started. Listening on port " + PORT);

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.out.println("SQLite JDBC driver not found.");
            e.printStackTrace();
            return;
        }

        while (true) {
            try (Socket clientSocket = serverSocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                String request;
                String username = null;
                boolean isAuthenticated = false;
                while ((request = in.readLine()) != null) {
                    System.out.println("Client: " + request); // 打印客户端发送的消息

                    if (request.startsWith("HELO")) {
                        String regex = "HELO (\\w+)";
                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(request);

                        if (matcher.find()) {
                            username = matcher.group(1);
                            isAuthenticated = false; // 重置认证状态

                            try {
                                if (checkUserExists(username)) {
                                    // 用户存在 → 要求输入密码
                                    out.println("500 AUTH REQUIRED!");
                                    logServiceRecord(username, "HELO", "500 AUTH REQUIRED!");
                                } else {
                                    // 用户不存在 → 直接报错
                                    out.println("401 ERROR!");
                                    logServiceRecord(username, "HELO", "401 ERROR!");
                                }
                            } catch (SQLException e) {
                                // 数据库异常时仍记录日志
                                out.println("401 ERROR!");
                                logServiceRecord(username, "HELO", "401 ERROR!");
                            }
                        } else {
                            // HELO 格式错误（如用户名含特殊字符）
                            out.println("401 ERROR!");
                            logServiceRecord("unknown", "HELO", "401 ERROR!");
                        }
// 处理 PASS 命令
                    } else if (request.startsWith("PASS")) {
                        String regex = "PASS (\\w+)";
                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(request);

                        if (matcher.find()) {
                            String password = matcher.group(1);
                            if (authenticateUser(username, password)) {
                                isAuthenticated = true;
                                out.println("525 OK!");
                                logServiceRecord(username, "PASS", "525 OK!");
                            } else {
                                out.println("401 ERROR!");
                                logServiceRecord(username, "PASS", "401 ERROR!");
                                // 移除 break，允许继续尝试
                            }
                        } else {
                            out.println("401 ERROR!");
                            logServiceRecord(username, "PASS", "401 ERROR!");
                            // 移除 break
                        }

// 处理 BALA/WDRA 等其他命令时：
                    } else if (request.equals("BALA")) {
                        if (isAuthenticated) {
                            out.println("AMNT:" + getBalance(username));
                            logServiceRecord(username, "BALA", "AMNT:" + getBalance(username));
                        } else {
                            out.println("401 ERROR!");
                            logServiceRecord(username, "BALA", "401 ERROR!");
                            // 不 break，保持连接
                        }
                    } else if (request.startsWith("WDRA")) {
                        // 使用正则表达式提取 amount
                        String regex = "WDRA (\\d+)";
                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(request);

                        if (matcher.find()) {
                            double amount = Double.parseDouble(matcher.group(1)); // 提取匹配的 amount
                            if (isAuthenticated) {
                                String response = withdraw(username, amount);
                                out.println(response);
                                logServiceRecord(username, "WDRA", response);
                            } else {
                                out.println("401 ERROR!");
                                logServiceRecord(username, "WDRA", "401 ERROR!");
                            }
                        } else {
                            out.println("401 ERROR!");
                            logServiceRecord(username, "WDRA", "401 ERROR!");
                        }
                    } else if (request.equals("BYE")) {
                        out.println("BYE");
                        logServiceRecord(username, "BYE", "BYE");
                        break;
                    } else {
                        out.println("401 ERROR!");
                        logServiceRecord(username, "Unknown", "401 ERROR!");
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private static boolean checkUserExists(String username) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement("SELECT username FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next(); // 用户存在返回 true
        }
    }
    private static boolean authenticateUser(String username, String password) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?")) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    private static double getBalance(String username) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement("SELECT balance FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getDouble("balance");
        }
    }

    private static String withdraw(String username, double amount) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // 检查输入的金额是否为负数
            if (amount < 0) {
                return "401 ERROR!";
            }
            // 检查余额是否充足
            String checkSql = "SELECT balance FROM users WHERE username = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    double balance = rs.getDouble("balance");
                    if (balance < amount) {
                        return "401 ERROR!";
                    }
                } else {
                    return "401 ERROR!";
                }
            }

            // 更新余额
            String updateSql = "UPDATE users SET balance = balance - ? WHERE username = ? AND balance >= ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setDouble(1, amount);
                updateStmt.setString(2, username);
                updateStmt.setDouble(3, amount);
                int updatedRows = updateStmt.executeUpdate();
                if (updatedRows > 0) {
                    return "525 OK";
                } else {
                    return "401 ERROR!";
                }
            }
        }
    }

    private static void logServiceRecord(String username, String serviceName, String response) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO service_records (username, service_name, response_code, timestamp) VALUES (?, ?, ?, datetime('now', 'localtime', 'milliseconds'))")) {
            stmt.setString(1, username);
            stmt.setString(2, serviceName);
            stmt.setString(3, response);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
