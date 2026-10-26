import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class RegisterFrame extends JFrame {

    private JTextField nameField, usernameField, emailField;
    private JPasswordField passwordField, confirmField;

    public RegisterFrame() {
        setTitle("User Registration");
        setSize(480, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel p = new JPanel(null);

        JLabel title = new JLabel("CREATE ACCOUNT");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setBounds(150, 20, 250, 30);
        p.add(title);

        nameField = addRow(p, "Full Name:", 80, false);
        usernameField = addRow(p, "Username:", 120, false);
        emailField = addRow(p, "Email:", 160, false);
        passwordField = (JPasswordField) addRow(p, "Password:", 200, true);
        confirmField = (JPasswordField) addRow(p, "Confirm:", 240, true);

        JButton register = new JButton("Register");
        register.setBounds(180, 300, 110, 35);
        p.add(register);
        register.addActionListener(e -> register());

        add(p);
        setVisible(true);
    }

    private JTextField addRow(JPanel p, String label, int y, boolean password) {
        JLabel l = new JLabel(label);
        l.setBounds(60, y, 110, 30);
        p.add(l);
        JTextField f = password ? new JPasswordField() : new JTextField();
        f.setBounds(180, y, 210, 30);
        p.add(f);
        return f;
    }

    private void register() {
        String name = nameField.getText().trim();
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String pass = new String(passwordField.getPassword());
        String confirm = new String(confirmField.getPassword());

        if (name.isEmpty() || username.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            msg("All fields are required.", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!email.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) {
            msg("Enter a valid email address.", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (pass.length() < 6) {
            msg("Password must be at least 6 characters.", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!pass.equals(confirm)) {
            msg("Passwords do not match.", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            try (PreparedStatement chk = con.prepareStatement(
                    "SELECT 1 FROM users WHERE username=? OR email=?")) {
                chk.setString(1, username);
                chk.setString(2, email);
                if (chk.executeQuery().next()) {
                    msg("User already exists.", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO users(name,username,email,password) VALUES(?,?,?,?)")) {
                ps.setString(1, name);
                ps.setString(2, username);
                ps.setString(3, email);
                ps.setString(4, PasswordUtil.hash(pass));
                ps.executeUpdate();
            }
            msg("Registration Successful!", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (SQLException ex) {
            msg("Database Error: " + ex.getMessage(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void msg(String m, int type) {
        JOptionPane.showMessageDialog(this, m, "Registration", type);
    }
}
