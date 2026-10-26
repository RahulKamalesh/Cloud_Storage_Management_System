import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LoginMain extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;

    public LoginMain() {
        setTitle("Cloud Storage Management System - Login");
        setSize(500, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(null);

        JLabel title = new JLabel("CLOUD STORAGE MANAGEMENT");
        title.setFont(new Font("Arial", Font.BOLD, 22));
        title.setBounds(80, 30, 350, 35);
        panel.add(title);

        JLabel userLabel = new JLabel("Username / Email:");
        userLabel.setBounds(70, 100, 130, 30);
        panel.add(userLabel);

        usernameField = new JTextField();
        usernameField.setBounds(210, 100, 200, 30);
        panel.add(usernameField);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setBounds(70, 150, 130, 30);
        panel.add(passwordLabel);

        passwordField = new JPasswordField();
        passwordField.setBounds(210, 150, 200, 30);
        panel.add(passwordField);

        loginButton = new JButton("Login");
        loginButton.setBounds(120, 220, 110, 35);
        panel.add(loginButton);

        registerButton = new JButton("Register");
        registerButton.setBounds(260, 220, 110, 35);
        panel.add(registerButton);

        loginButton.addActionListener(e -> login());
        passwordField.addActionListener(e -> login());
        registerButton.addActionListener(e -> new RegisterFrame());

        add(panel);
        setVisible(true);
    }

    private void login() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter username and password.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "SELECT user_id, name FROM users " +
                     "WHERE (username=? OR email=?) AND password=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, username);
            ps.setString(3, PasswordUtil.hash(password));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("user_id");
                    String name = rs.getString("name");
                    JOptionPane.showMessageDialog(this, "Login Successful!",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    new Dashboard(userId, name);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid Username or Password.",
                            "Login Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginMain::new);
    }
}
