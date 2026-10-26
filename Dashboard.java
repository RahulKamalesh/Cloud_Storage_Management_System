import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Dashboard extends JFrame {

    private static final String STORAGE_ROOT = "cloud_storage";
    private static final long QUOTA_BYTES = 100L * 1024 * 1024; // 100 MB per user

    private final int userId;
    private final String userName;

    private DefaultTableModel model;
    private JTable table;
    private JTextField searchField;

    public Dashboard(int userId, String userName) {
        this.userId = userId;
        this.userName = userName;

        setTitle("Cloud Storage Management System - Dashboard");
        setSize(900, 560);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JLabel welcome = new JLabel("  Welcome, " + userName);
        welcome.setFont(new Font("Arial", Font.BOLD, 18));
        JPanel top = new JPanel(new BorderLayout());
        top.add(welcome, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchField = new JTextField(18);
        JButton searchBtn = new JButton("Search");
        JButton clearBtn = new JButton("Show All");
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        searchPanel.add(clearBtn);
        top.add(searchPanel, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        model = new DefaultTableModel(
                new String[]{"ID", "Name", "Type", "Size", "Owner", "Access", "Folder"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(0, 1, 6, 6));
        buttons.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 10));
        JButton upload = new JButton("Upload File");
        JButton download = new JButton("Download");
        JButton rename = new JButton("Rename");
        JButton delete = new JButton("Delete");
        JButton share = new JButton("Share File");
        JButton folder = new JButton("Create Folder");
        JButton info = new JButton("File Information");
        JButton storage = new JButton("Storage Usage");
        JButton refresh = new JButton("Refresh");
        JButton logout = new JButton("Logout");
        for (JButton b : new JButton[]{upload, download, rename, delete, share,
                folder, info, storage, refresh, logout}) buttons.add(b);
        add(buttons, BorderLayout.EAST);

        upload.addActionListener(e -> uploadFile());
        download.addActionListener(e -> downloadFile());
        rename.addActionListener(e -> renameFile());
        delete.addActionListener(e -> deleteFile());
        share.addActionListener(e -> shareFile());
        folder.addActionListener(e -> createFolder());
        info.addActionListener(e -> showInfo());
        storage.addActionListener(e -> showStorage());
        refresh.addActionListener(e -> loadFiles(""));
        searchBtn.addActionListener(e -> loadFiles(searchField.getText().trim()));
        searchField.addActionListener(e -> loadFiles(searchField.getText().trim()));
        clearBtn.addActionListener(e -> { searchField.setText(""); loadFiles(""); });
        logout.addActionListener(e -> { dispose(); new LoginMain(); });

        loadFiles("");
        setVisible(true);
    }

    // ---------------------------------------------------------------- listing / search
    private void loadFiles(String keyword) {
        model.setRowCount(0);
        String sql =
            "SELECT f.file_id, f.file_name, f.file_type, f.file_size, u.name AS owner, " +
            "IF(f.owner_id=?, 'OWNER', s.permission) AS access, fo.folder_name " +
            "FROM files f " +
            "JOIN users u ON u.user_id = f.owner_id " +
            "LEFT JOIN shared_files s ON s.file_id = f.file_id AND s.receiver_id = ? " +
            "LEFT JOIN folders fo ON fo.folder_id = f.folder_id " +
            "WHERE (f.owner_id = ? OR s.receiver_id = ?) AND f.file_name LIKE ? " +
            "ORDER BY f.file_id";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, userId);
            ps.setInt(4, userId);
            ps.setString(5, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    String fo = rs.getString("folder_name");
                    model.addRow(new Object[]{
                            rs.getInt("file_id"), rs.getString("file_name"),
                            rs.getString("file_type"), readable(rs.getLong("file_size")),
                            rs.getString("owner"), rs.getString("access"),
                            fo == null ? "-" : fo});
                }
                if (count == 0 && !keyword.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No File Found");
                }
            }
        } catch (SQLException ex) {
            error("Unable to load files: " + ex.getMessage());
        }
    }

    // ---------------------------------------------------------------- upload
    private void uploadFile() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File src = fc.getSelectedFile();

        if (!src.exists() || !src.isFile()) { error("File not found."); return; }
        if (src.length() == 0) { error("Empty files cannot be uploaded."); return; }

        try (Connection con = DBConnection.getConnection()) {
            if (usedBytes(con) + src.length() > QUOTA_BYTES) {
                error("Storage quota exceeded (" + readable(QUOTA_BYTES) + ").");
                return;
            }

            // choose a folder (optional)
            List<String> names = new ArrayList<>();
            List<Integer> ids = new ArrayList<>();
            names.add("(No folder)");
            ids.add(null);
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT folder_id, folder_name FROM folders WHERE owner_id=?")) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) { ids.add(rs.getInt(1)); names.add(rs.getString(2)); }
            }
            Object choice = JOptionPane.showInputDialog(this, "Upload into folder:",
                    "Upload", JOptionPane.QUESTION_MESSAGE, null, names.toArray(), names.get(0));
            if (choice == null) return;
            int idx = names.indexOf(choice);
            Integer folderId = ids.get(idx);

            Path dir = Paths.get(STORAGE_ROOT, String.valueOf(userId));
            if (folderId != null) dir = dir.resolve(names.get(idx));
            Files.createDirectories(dir);
            Path dest = dir.resolve(src.getName());
            if (Files.exists(dest)) {
                int o = JOptionPane.showConfirmDialog(this,
                        "A file with this name exists. Replace it?", "Confirm",
                        JOptionPane.YES_NO_OPTION);
                if (o != JOptionPane.YES_OPTION) return;
            }
            Files.copy(src.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);

            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO files(file_name,file_type,file_size,owner_id,folder_id,storage_path) " +
                    "VALUES(?,?,?,?,?,?)")) {
                ps.setString(1, src.getName());
                ps.setString(2, extension(src.getName()));
                ps.setLong(3, src.length());
                ps.setInt(4, userId);
                if (folderId == null) ps.setNull(5, Types.INTEGER); else ps.setInt(5, folderId);
                ps.setString(6, dest.toString());
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "File Uploaded Successfully!");
            loadFiles("");
        } catch (IOException | SQLException ex) {
            error("Upload Failed: " + ex.getMessage());
        }
    }

    // ---------------------------------------------------------------- download
    private void downloadFile() {
        Integer id = selectedId();
        if (id == null) return;

        try (Connection con = DBConnection.getConnection()) {
            String[] f = accessibleFile(con, id, true);
            if (f == null) return;
            Path source = Paths.get(f[1]);
            if (!Files.exists(source)) { error("File missing from cloud storage."); return; }

            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File(f[0]));
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            Files.copy(source, fc.getSelectedFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
            JOptionPane.showMessageDialog(this, "File Downloaded Successfully!");
        } catch (IOException | SQLException ex) {
            error("Download Failed: " + ex.getMessage());
        }
    }

    // ---------------------------------------------------------------- rename / update
    private void renameFile() {
        Integer id = selectedId();
        if (id == null) return;
        try (Connection con = DBConnection.getConnection()) {
            String[] f = ownedFile(con, id);
            if (f == null) return;
            String newName = JOptionPane.showInputDialog(this, "New file name:", f[0]);
            if (newName == null || newName.trim().isEmpty()) return;
            newName = newName.trim();
            if (newName.contains("/") || newName.contains("\\")) { error("Invalid name."); return; }

            Path old = Paths.get(f[1]);
            Path target = old.resolveSibling(newName);
            if (Files.exists(old)) Files.move(old, target, StandardCopyOption.REPLACE_EXISTING);

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE files SET file_name=?, file_type=?, storage_path=? WHERE file_id=?")) {
                ps.setString(1, newName);
                ps.setString(2, extension(newName));
                ps.setString(3, target.toString());
                ps.setInt(4, id);
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "File Updated Successfully!");
            loadFiles("");
        } catch (IOException | SQLException ex) {
            error("Update Failed: " + ex.getMessage());
        }
    }

    // ---------------------------------------------------------------- delete
    private void deleteFile() {
        Integer id = selectedId();
        if (id == null) return;
        try (Connection con = DBConnection.getConnection()) {
            String[] f = ownedFile(con, id);
            if (f == null) return;
            int o = JOptionPane.showConfirmDialog(this,
                    "Delete '" + f[0] + "' permanently?", "Confirm Delete",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (o != JOptionPane.YES_OPTION) return;

            Files.deleteIfExists(Paths.get(f[1]));
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM files WHERE file_id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();   // shared_files rows removed by ON DELETE CASCADE
            }
            JOptionPane.showMessageDialog(this, "File Deleted Successfully!");
            loadFiles("");
        } catch (IOException | SQLException ex) {
            error("Delete Failed: " + ex.getMessage());
        }
    }

    // ---------------------------------------------------------------- share
    private void shareFile() {
        Integer id = selectedId();
        if (id == null) return;
        try (Connection con = DBConnection.getConnection()) {
            if (ownedFile(con, id) == null) return;

            String receiver = JOptionPane.showInputDialog(this, "Receiver username or email:");
            if (receiver == null || receiver.trim().isEmpty()) return;

            int receiverId;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT user_id FROM users WHERE username=? OR email=?")) {
                ps.setString(1, receiver.trim());
                ps.setString(2, receiver.trim());
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) { error("Receiver not found."); return; }
                receiverId = rs.getInt(1);
            }
            if (receiverId == userId) { error("You already own this file."); return; }

            Object perm = JOptionPane.showInputDialog(this, "Permission:", "Share",
                    JOptionPane.QUESTION_MESSAGE, null, new String[]{"VIEW", "DOWNLOAD"}, "VIEW");
            if (perm == null) return;

            try (PreparedStatement del = con.prepareStatement(
                    "DELETE FROM shared_files WHERE file_id=? AND receiver_id=?")) {
                del.setInt(1, id); del.setInt(2, receiverId); del.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO shared_files(file_id,receiver_id,permission) VALUES(?,?,?)")) {
                ps.setInt(1, id);
                ps.setInt(2, receiverId);
                ps.setString(3, perm.toString());
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "File Shared Successfully!");
        } catch (SQLException ex) {
            error("Sharing Failed: " + ex.getMessage());
        }
    }

    // ---------------------------------------------------------------- folder
    private void createFolder() {
        String name = JOptionPane.showInputDialog(this, "Folder name:");
        if (name == null) return;
        name = name.trim();
        if (name.isEmpty() || !name.matches("[\\w .-]+")) {
            error("Enter a valid folder name (letters, digits, space, . _ -).");
            return;
        }
        try (Connection con = DBConnection.getConnection()) {
            try (PreparedStatement chk = con.prepareStatement(
                    "SELECT 1 FROM folders WHERE folder_name=? AND owner_id=?")) {
                chk.setString(1, name);
                chk.setInt(2, userId);
                if (chk.executeQuery().next()) { error("Folder already exists."); return; }
            }
            Files.createDirectories(Paths.get(STORAGE_ROOT, String.valueOf(userId), name));
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO folders(folder_name,owner_id) VALUES(?,?)")) {
                ps.setString(1, name);
                ps.setInt(2, userId);
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Folder Created Successfully!");
        } catch (IOException | SQLException ex) {
            error("Folder Creation Failed: " + ex.getMessage());
        }
    }

    // ---------------------------------------------------------------- info / storage
    private void showInfo() {
        Integer id = selectedId();
        if (id == null) return;
        int r = table.getSelectedRow();
        JOptionPane.showMessageDialog(this,
                "Name   : " + model.getValueAt(r, 1) + "\n" +
                "Type   : " + model.getValueAt(r, 2) + "\n" +
                "Size   : " + model.getValueAt(r, 3) + "\n" +
                "Owner  : " + model.getValueAt(r, 4) + "\n" +
                "Access : " + model.getValueAt(r, 5) + "\n" +
                "Folder : " + model.getValueAt(r, 6) + "\n" +
                "Status : Available\nStorage: Cloud",
                "File Information", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showStorage() {
        try (Connection con = DBConnection.getConnection()) {
            long used = usedBytes(con);
            int count;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COUNT(*) FROM files WHERE owner_id=?")) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                rs.next();
                count = rs.getInt(1);
            }
            JOptionPane.showMessageDialog(this,
                    "Total Files     : " + count + "\n" +
                    "Bytes Used      : " + used + "\n" +
                    "Used Storage    : " + readable(used) + "\n" +
                    "Available Space : " + readable(Math.max(0, QUOTA_BYTES - used)) + "\n" +
                    "Quota           : " + readable(QUOTA_BYTES),
                    "Storage Information", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            error("Storage Error: " + ex.getMessage());
        }
    }

    // ---------------------------------------------------------------- access control helpers
    /** Returns {name, path} if the file belongs to the current user, else shows a message. */
    private String[] ownedFile(Connection con, int fileId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT file_name, storage_path FROM files WHERE file_id=? AND owner_id=?")) {
            ps.setInt(1, fileId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new String[]{rs.getString(1), rs.getString(2)};
        }
        error("Access denied: only the owner can perform this operation.");
        return null;
    }

    /** Returns {name, path} if user owns the file or it is shared with the required permission. */
    private String[] accessibleFile(Connection con, int fileId, boolean needDownload) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT f.file_name, f.storage_path, f.owner_id, s.permission FROM files f " +
                "LEFT JOIN shared_files s ON s.file_id=f.file_id AND s.receiver_id=? " +
                "WHERE f.file_id=?")) {
            ps.setInt(1, userId);
            ps.setInt(2, fileId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) { error("File Not Found"); return null; }
            boolean owner = rs.getInt("owner_id") == userId;
            String perm = rs.getString("permission");
            if (owner || (perm != null && (!needDownload || perm.equals("DOWNLOAD")))) {
                return new String[]{rs.getString(1), rs.getString(2)};
            }
        }
        error("Access denied: you do not have DOWNLOAD permission.");
        return null;
    }

    private long usedBytes(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COALESCE(SUM(file_size),0) FROM files WHERE owner_id=?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getLong(1);
        }
    }

    // ---------------------------------------------------------------- utilities
    private Integer selectedId() {
        int r = table.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Please select a file from the list first.");
            return null;
        }
        return (Integer) model.getValueAt(r, 0);
    }

    private static String extension(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "FILE" : name.substring(i + 1).toUpperCase();
    }

    private static String readable(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return String.format("%.2f KB", b / 1024.0);
        return String.format("%.2f MB", b / (1024.0 * 1024.0));
    }

    private void error(String m) {
        JOptionPane.showMessageDialog(this, m, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
