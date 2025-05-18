import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ATM服务器主类，处理客户端连接和ATM操作请求
 */
public class ATMServer {
    // 服务器配置常量
    private static final int PORT = 2525;                      // 服务器监听端口
    private static final String DB_URL = "jdbc:sqlite:F:/soft/SQlite/atm.db";  // 数据库连接URL

    // 线程池，用于处理客户端连接
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();

    /**
     * 主方法，启动服务器
     */
    public static void main(String[] args) throws IOException, SQLException {
        // 创建服务器Socket
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server started. Listening on port " + PORT);

        // 加载SQLite JDBC驱动
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.out.println("SQLite JDBC driver not found.");
            e.printStackTrace();
            return;
        }

        // 主循环，持续接受客户端连接
        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("New client connected: " + clientSocket.getInetAddress());

            // 使用线程池处理每个客户端连接
            threadPool.execute(new ClientHandler(clientSocket));
        }
    }

    /**
     * 客户端请求处理类，实现Runnable接口以便在独立线程中运行
     */
    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;  // 客户端Socket连接

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            // 使用try-with-resources确保流和Socket正确关闭
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                String request;            // 客户端请求命令
                String username = null;    // 当前会话用户名
                boolean isAuthenticated = false;  // 认证状态标志

                // 读取并处理客户端请求
                while ((request = in.readLine()) != null) {
                    System.out.println("Received command: " + request);

                    // 处理HELO命令 - 用户名验证
                    if (request.startsWith("HELO")) {
                        username = request.split(" ")[1];
                        if (authenticateUser(username)) {
                            out.println("500 AUTH REQUIRE");  // 需要密码认证
                        } else {
                            out.println("401 ERROR!");        // 用户名不存在
                            logServiceRecord(username, "HELO", "401 ERROR!");
                            break;
                        }
                    }
                    // 处理PASS命令 - 密码验证
                    else if (request.startsWith("PASS")) {
                        String password = request.split(" ")[1];
                        if (isAuthenticated = authenticateUser(username, password)) {
                            out.println("525 OK!");          // 认证成功
                            logServiceRecord(username, "PASS", "525 OK!");
                        } else {
                            out.println("401 ERROR!");       // 认证失败
                            logServiceRecord(username, "PASS", "401 ERROR!");
                            break;
                        }
                    }
                    // 处理BALA命令 - 查询余额
                    else if (request.equals("BALA")) {
                        if (isAuthenticated) {
                            out.println("AMNT:" + getBalance(username));  // 返回余额
                            logServiceRecord(username, "BALA", "AMNT:" + getBalance(username));
                        } else {
                            out.println("401 ERROR!");       // 未认证
                            logServiceRecord(username, "BALA", "401 ERROR!");
                        }
                    }
                    // 处理WDRA命令 - 取款操作
                    else if (request.startsWith("WDRA")) {
                        if (isAuthenticated) {
                            double amount = Double.parseDouble(request.split(" ")[1]);
                            String response = withdraw(username, amount);  // 执行取款
                            out.println(response);  // 向客户端发送响应
                            logServiceRecord(username, "WDRA", response);
                        } else {
                            out.println("401 ERROR!");  // 未认证
                            logServiceRecord(username, "WDRA", "401 ERROR!");
                        }
                    }
                    // 处理BYE命令 - 结束会话
                    else if (request.equals("BYE")) {
                        out.println("BYE");
                        logServiceRecord(username, "BYE", "BYE");
                        break;
                    }
                    // 未知命令处理
                    else {
                        out.println("401 ERROR!");
                        logServiceRecord(username, "Unknown", "401 ERROR!");
                        break;
                    }
                }
            } catch (Exception e) {
                System.out.println("Client disconnected unexpectedly.");
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();  // 确保关闭客户端连接
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 验证用户名是否存在
     * @param username 要验证的用户名
     * @return 存在返回true，否则false
     */
    private static boolean authenticateUser(String username) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next();  // 是否有结果
        }
    }

    /**
     * 验证用户名和密码是否匹配
     * @param username 用户名
     * @param password 密码
     * @return 匹配返回true，否则false
     */
    private static boolean authenticateUser(String username, String password) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?")) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            return rs.next();  // 是否有结果
        }
    }

    /**
     * 获取用户余额
     * @param username 用户名
     * @return 用户余额
     */
    private static double getBalance(String username) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement("SELECT balance FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getDouble("balance");
        }
    }

    /**
     * 执行取款操作
     * @param username 用户名
     * @param amount 取款金额
     * @return 操作结果代码
     */
    private static String withdraw(String username, double amount) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // 查询当前余额
            String balanceSql = "SELECT balance FROM users WHERE username = ?";
            try (PreparedStatement balanceStmt = conn.prepareStatement(balanceSql)) {
                balanceStmt.setString(1, username);
                ResultSet rs = balanceStmt.executeQuery();
                if (rs.next()) {
                    double currentBalance = rs.getDouble("balance");
                    if (currentBalance >= amount) {
                        // 更新用户余额
                        String updateSql = "UPDATE users SET balance = balance - ? WHERE username = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setDouble(1, amount);
                            updateStmt.setString(2, username);
                            int updatedRows = updateStmt.executeUpdate();

                            if (updatedRows > 0) {
                                // 记录取款操作
                                String insertSql = "INSERT INTO service_records (username, service_name, amount, response_code, timestamp) VALUES (?, ?, ?, '525 OK', datetime('now'))";
                                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                                    insertStmt.setString(1, username);
                                    insertStmt.setString(2, "withdraw");
                                    insertStmt.setDouble(3, amount);
                                    insertStmt.executeUpdate();
                                }
                                return "525 OK";  // 操作成功
                            } else {
                                return "401 ERROR!";  // 操作失败
                            }
                        }
                    } else {
                        // 余额不足
                        return "401 ERROR!";  // 余额不足，返回401
                    }
                } else {
                    return "401 ERROR!";  // 用户不存在
                }
            }
        }
    }

    /**
     * 记录服务操作日志
     * @param username 用户名
     * @param serviceName 服务名称
     * @param response 响应代码
     */
    private static void logServiceRecord(String username, String serviceName, String response) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO service_records (username, service_name, response_code, timestamp) VALUES (?, ?, ?, datetime('now'))")) {
            stmt.setString(1, username);
            stmt.setString(2, serviceName);
            stmt.setString(3, response);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}