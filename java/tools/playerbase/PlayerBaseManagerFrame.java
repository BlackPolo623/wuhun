package tools.playerbase;

import java.awt.Color;
import java.awt.Font;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.LineBorder;

import org.l2jmobius.commons.database.DatabaseFactory;

public class PlayerBaseManagerFrame extends JFrame
{
    private static final long serialVersionUID = 1L;

    private static final Color BG_COLOR = new Color(30, 30, 30);
    private static final Color FG_COLOR = new Color(50, 240, 240);
    private static final Color PANEL_BG = new Color(45, 45, 45);
    private static final Color INPUT_BG = new Color(55, 55, 55);
    private static final Color BORDER_COLOR = new Color(100, 100, 100);

    private static final int BASE_TEMPLATE_ID = 900;
    private static int nextInstanceId = 7000;

    private JTextField txtPlayerName;
    private JTextField txtSearchName;
    private JSpinner spnMaxMonsters;
    private JCheckBox chkBossPermission;
    private JTextArea txtLog;
    private JButton btnCreate;
    private JButton btnSearch;
    private JButton btnRefresh;
    private JButton btnDelete;
    private JButton btnToggleBoss;

    public PlayerBaseManagerFrame()
    {
        initializeDatabase();
        loadNextInstanceId();
        initComponents();
    }

    private void initializeDatabase()
    {
        try (Connection con = DatabaseFactory.getConnection())
        {
            try (PreparedStatement ps = con.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS player_base (" +
                            "player_id INT PRIMARY KEY, " +
                            "player_name VARCHAR(35) NOT NULL, " +
                            "instance_id INT NOT NULL UNIQUE, " +
                            "template_id INT NOT NULL, " +
                            "max_monster_count INT DEFAULT 50, " +
                            "can_summon_boss BOOLEAN DEFAULT FALSE, " +
                            "created_time BIGINT NOT NULL, " +
                            "INDEX idx_player_name (player_name), " +
                            "INDEX idx_instance (instance_id)" +
                            ")"))
            {
                ps.execute();
            }

            // 檢查並新增 can_summon_boss 欄位（相容舊資料庫）
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COUNT(*) as cnt FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'player_base' AND COLUMN_NAME = 'can_summon_boss'"))
            {
                try (ResultSet rs = ps.executeQuery())
                {
                    if (rs.next() && rs.getInt("cnt") == 0)
                    {
                        try (PreparedStatement alter = con.prepareStatement(
                                "ALTER TABLE player_base ADD COLUMN can_summon_boss BOOLEAN DEFAULT FALSE"))
                        {
                            alter.execute();
                            System.out.println("[基地管理系統] 已新增 can_summon_boss 欄位");
                        }
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS player_base_visitors (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY, " +
                            "base_owner_id INT NOT NULL, " +
                            "visitor_id INT NOT NULL, " +
                            "visitor_name VARCHAR(35) NOT NULL, " +
                            "added_time BIGINT NOT NULL, " +
                            "UNIQUE KEY unique_visitor (base_owner_id, visitor_id), " +
                            "INDEX idx_owner (base_owner_id)" +
                            ")"))
            {
                ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS player_base_monsters (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY, " +
                            "base_owner_id INT NOT NULL, " +
                            "spawn_index INT NOT NULL, " +
                            "monster_id INT NOT NULL, " +
                            "monster_count INT NOT NULL, " +
                            "UNIQUE KEY unique_spawn (base_owner_id, spawn_index), " +
                            "INDEX idx_owner (base_owner_id)" +
                            ")"))
            {
                ps.execute();
            }

            System.out.println("[基地管理系統] 資料表檢查完成");
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    private void loadNextInstanceId()
    {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT MAX(instance_id) as max_id FROM player_base"))
        {
            try (ResultSet rs = ps.executeQuery())
            {
                if (rs.next())
                {
                    int maxId = rs.getInt("max_id");
                    if (maxId > 0)
                    {
                        nextInstanceId = maxId + 1;
                    }
                }
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    private void initComponents()
    {
        setTitle("基地管理系統");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 700, 680);
        setResizable(false);

        JPanel contentPane = new JPanel();
        contentPane.setBackground(BG_COLOR);
        contentPane.setBorder(new LineBorder(BORDER_COLOR, 1));
        contentPane.setLayout(null);
        setContentPane(contentPane);

        JLabel lblTitle = new JLabel("基地管理系統");
        lblTitle.setFont(new Font("微軟正黑體", Font.BOLD, 18));
        lblTitle.setForeground(FG_COLOR);
        lblTitle.setBounds(20, 10, 660, 30);
        contentPane.add(lblTitle);

        JLabel lblCreate = new JLabel("═══ 創建基地 ═══");
        lblCreate.setFont(new Font("微軟正黑體", Font.BOLD, 14));
        lblCreate.setForeground(FG_COLOR);
        lblCreate.setBounds(20, 50, 660, 25);
        contentPane.add(lblCreate);

        JLabel lblPlayerName = new JLabel("角色名稱:");
        lblPlayerName.setFont(new Font("微軟正黑體", Font.PLAIN, 13));
        lblPlayerName.setForeground(FG_COLOR);
        lblPlayerName.setBounds(20, 80, 80, 25);
        contentPane.add(lblPlayerName);

        txtPlayerName = new JTextField();
        txtPlayerName.setFont(new Font("微軟正黑體", Font.PLAIN, 13));
        txtPlayerName.setBackground(INPUT_BG);
        txtPlayerName.setForeground(FG_COLOR);
        txtPlayerName.setCaretColor(FG_COLOR);
        txtPlayerName.setBorder(new LineBorder(BORDER_COLOR, 1));
        txtPlayerName.setBounds(100, 80, 150, 25);
        contentPane.add(txtPlayerName);

        JLabel lblMaxMonsters = new JLabel("怪物上限:");
        lblMaxMonsters.setFont(new Font("微軟正黑體", Font.PLAIN, 13));
        lblMaxMonsters.setForeground(FG_COLOR);
        lblMaxMonsters.setBounds(260, 80, 80, 25);
        contentPane.add(lblMaxMonsters);

        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(50, 10, 200, 10);
        spnMaxMonsters = new JSpinner(spinnerModel);
        spnMaxMonsters.setFont(new Font("微軟正黑體", Font.PLAIN, 13));
        spnMaxMonsters.setBounds(340, 80, 70, 25);
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spnMaxMonsters.getEditor();
        editor.getTextField().setBackground(INPUT_BG);
        editor.getTextField().setForeground(FG_COLOR);
        editor.getTextField().setCaretColor(FG_COLOR);
        spnMaxMonsters.setBorder(new LineBorder(BORDER_COLOR, 1));
        contentPane.add(spnMaxMonsters);

        chkBossPermission = new JCheckBox("BOSS權限");
        chkBossPermission.setFont(new Font("微軟正黑體", Font.PLAIN, 13));
        chkBossPermission.setForeground(FG_COLOR);
        chkBossPermission.setBackground(BG_COLOR);
        chkBossPermission.setFocusPainted(false);
        chkBossPermission.setBounds(420, 80, 100, 25);
        contentPane.add(chkBossPermission);

        btnCreate = createButton("創建基地", 530, 80, 100, 25);
        btnCreate.addActionListener(e -> createBase());
        contentPane.add(btnCreate);

        JLabel lblSearch = new JLabel("═══ 查詢基地 ═══");
        lblSearch.setFont(new Font("微軟正黑體", Font.BOLD, 14));
        lblSearch.setForeground(FG_COLOR);
        lblSearch.setBounds(20, 120, 660, 25);
        contentPane.add(lblSearch);

        JLabel lblSearchName = new JLabel("角色名稱:");
        lblSearchName.setFont(new Font("微軟正黑體", Font.PLAIN, 13));
        lblSearchName.setForeground(FG_COLOR);
        lblSearchName.setBounds(20, 150, 80, 25);
        contentPane.add(lblSearchName);

        txtSearchName = new JTextField();
        txtSearchName.setFont(new Font("微軟正黑體", Font.PLAIN, 13));
        txtSearchName.setBackground(INPUT_BG);
        txtSearchName.setForeground(FG_COLOR);
        txtSearchName.setCaretColor(FG_COLOR);
        txtSearchName.setBorder(new LineBorder(BORDER_COLOR, 1));
        txtSearchName.setBounds(100, 150, 150, 25);
        contentPane.add(txtSearchName);

        btnSearch = createButton("查詢", 260, 150, 80, 25);
        btnSearch.addActionListener(e -> searchBase());
        contentPane.add(btnSearch);

        btnToggleBoss = createButton("切換BOSS權限", 350, 150, 120, 25);
        btnToggleBoss.addActionListener(e -> toggleBossPermission());
        contentPane.add(btnToggleBoss);

        btnDelete = createButton("刪除基地", 480, 150, 90, 25);
        btnDelete.addActionListener(e -> deleteBase());
        contentPane.add(btnDelete);

        btnRefresh = createButton("刷新", 580, 150, 80, 25);
        btnRefresh.addActionListener(e -> refreshLog());
        contentPane.add(btnRefresh);

        JLabel lblLog = new JLabel("基地列表:");
        lblLog.setFont(new Font("微軟正黑體", Font.BOLD, 14));
        lblLog.setForeground(FG_COLOR);
        lblLog.setBounds(20, 190, 100, 25);
        contentPane.add(lblLog);

        txtLog = new JTextArea();
        txtLog.setFont(new Font("微軟正黑體", Font.PLAIN, 12));
        txtLog.setBackground(INPUT_BG);
        txtLog.setForeground(FG_COLOR);
        txtLog.setCaretColor(FG_COLOR);
        txtLog.setEditable(false);
        txtLog.setLineWrap(true);
        txtLog.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(txtLog);
        scrollPane.setBounds(20, 220, 660, 410);
        scrollPane.setBorder(new LineBorder(BORDER_COLOR, 1));
        contentPane.add(scrollPane);

        refreshLog();
    }

    private JButton createButton(String text, int x, int y, int width, int height)
    {
        JButton btn = new JButton(text);
        btn.setFont(new Font("微軟正黑體", Font.PLAIN, 12));
        btn.setBounds(x, y, width, height);
        btn.setBackground(new Color(40, 40, 40));
        btn.setForeground(Color.WHITE);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(60, 60, 60));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(40, 40, 40));
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(30, 30, 30));
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(60, 60, 60));
            }
        });

        return btn;
    }

    private void createBase()
    {
        String playerName = txtPlayerName.getText().trim();

        if (playerName.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "請輸入角色名稱!", "錯誤", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int playerId = getCharacterObjectId(playerName);
        if (playerId == -1)
        {
            JOptionPane.showMessageDialog(this, "角色 '" + playerName + "' 不存在!", "錯誤", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (hasBase(playerId))
        {
            JOptionPane.showMessageDialog(this, "該角色已經擁有基地!", "錯誤", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int maxMonsters = (Integer) spnMaxMonsters.getValue();
        boolean canSummonBoss = chkBossPermission.isSelected();
        int instanceId = nextInstanceId++;

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO player_base (player_id, player_name, instance_id, template_id, max_monster_count, can_summon_boss, created_time) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?)"))
        {
            ps.setInt(1, playerId);
            ps.setString(2, playerName);
            ps.setInt(3, instanceId);
            ps.setInt(4, BASE_TEMPLATE_ID);
            ps.setInt(5, maxMonsters);
            ps.setBoolean(6, canSummonBoss);
            ps.setLong(7, System.currentTimeMillis());
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this,
                    "成功為 " + playerName + " 創建基地!\n" +
                            "副本ID: " + instanceId + "\n" +
                            "怪物上限: " + maxMonsters + "\n" +
                            "BOSS權限: " + (canSummonBoss ? "開啟" : "關閉"),
                    "成功",
                    JOptionPane.INFORMATION_MESSAGE);

            txtPlayerName.setText("");
            spnMaxMonsters.setValue(50);
            chkBossPermission.setSelected(false);
            refreshLog();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "創建基地失敗: " + e.getMessage(), "錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleBossPermission()
    {
        String playerName = txtSearchName.getText().trim();

        if (playerName.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "請輸入角色名稱!", "錯誤", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int playerId = getCharacterObjectId(playerName);
        if (playerId == -1)
        {
            JOptionPane.showMessageDialog(this, "角色 '" + playerName + "' 不存在!", "錯誤", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!hasBase(playerId))
        {
            JOptionPane.showMessageDialog(this, "該角色沒有基地!", "錯誤", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT can_summon_boss FROM player_base WHERE player_id = ?"))
        {
            ps.setInt(1, playerId);
            try (ResultSet rs = ps.executeQuery())
            {
                if (rs.next())
                {
                    boolean currentStatus = rs.getBoolean("can_summon_boss");
                    boolean newStatus = !currentStatus;

                    try (PreparedStatement update = con.prepareStatement(
                            "UPDATE player_base SET can_summon_boss = ? WHERE player_id = ?"))
                    {
                        update.setBoolean(1, newStatus);
                        update.setInt(2, playerId);
                        update.executeUpdate();

                        JOptionPane.showMessageDialog(this,
                                playerName + " 的BOSS召喚權限已" + (newStatus ? "開啟" : "關閉"),
                                "成功",
                                JOptionPane.INFORMATION_MESSAGE);

                        refreshLog();
                    }
                }
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "操作失敗: " + e.getMessage(), "錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void searchBase()
    {
        String playerName = txtSearchName.getText().trim();

        if (playerName.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "請輸入要查詢的角色名稱!", "錯誤", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int playerId = getCharacterObjectId(playerName);
        if (playerId == -1)
        {
            JOptionPane.showMessageDialog(this, "角色 '" + playerName + "' 不存在!", "錯誤", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM player_base WHERE player_id = ?"))
        {
            ps.setInt(1, playerId);
            try (ResultSet rs = ps.executeQuery())
            {
                if (!rs.next())
                {
                    JOptionPane.showMessageDialog(this, "該角色沒有基地!", "查詢結果", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                int instanceId = rs.getInt("instance_id");
                int templateId = rs.getInt("template_id");
                int maxMonsters = rs.getInt("max_monster_count");
                boolean canSummonBoss = rs.getBoolean("can_summon_boss");
                long createdTime = rs.getLong("created_time");

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String createDate = sdf.format(new Date(createdTime));

                int visitorCount = getVisitorCount(playerId);
                int monsterSpawnCount = getMonsterSpawnCount(playerId);
                int totalMonsters = getTotalMonsterCount(playerId);

                String info = String.format(
                        "基地資訊:\n\n" +
                                "角色名稱: %s\n" +
                                "副本ID: %d\n" +
                                "模板ID: %d\n" +
                                "怪物上限: %d\n" +
                                "BOSS權限: %s\n" +
                                "創建時間: %s\n\n" +
                                "訪客數量: %d 人\n" +
                                "怪物配置: %d 個生成點\n" +
                                "怪物總數: %d / %d",
                        playerName, instanceId, templateId, maxMonsters,
                        (canSummonBoss ? "已開啟" : "未開啟"),
                        createDate,
                        visitorCount, monsterSpawnCount, totalMonsters, maxMonsters
                );

                JOptionPane.showMessageDialog(this, info, "基地詳情", JOptionPane.INFORMATION_MESSAGE);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "查詢失敗: " + e.getMessage(), "錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteBase()
    {
        String playerName = txtSearchName.getText().trim();

        if (playerName.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "請輸入要刪除的角色名稱!", "錯誤", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int playerId = getCharacterObjectId(playerName);
        if (playerId == -1)
        {
            JOptionPane.showMessageDialog(this, "角色 '" + playerName + "' 不存在!", "錯誤", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!hasBase(playerId))
        {
            JOptionPane.showMessageDialog(this, "該角色沒有基地!", "錯誤", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "確定要刪除 " + playerName + " 的基地嗎?\n此操作無法恢復!",
                "確認刪除",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION)
        {
            return;
        }

        try (Connection con = DatabaseFactory.getConnection())
        {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM player_base_monsters WHERE base_owner_id = ?"))
            {
                ps.setInt(1, playerId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM player_base_visitors WHERE base_owner_id = ?"))
            {
                ps.setInt(1, playerId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM player_base WHERE player_id = ?"))
            {
                ps.setInt(1, playerId);
                ps.executeUpdate();
            }

            con.commit();

            JOptionPane.showMessageDialog(this, "成功刪除 " + playerName + " 的基地!", "成功", JOptionPane.INFORMATION_MESSAGE);
            txtSearchName.setText("");
            refreshLog();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "刪除失敗: " + e.getMessage(), "錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshLog()
    {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT b.*, " +
                             "(SELECT COUNT(*) FROM player_base_visitors v WHERE v.base_owner_id = b.player_id) as visitor_count, " +
                             "(SELECT COUNT(*) FROM player_base_monsters m WHERE m.base_owner_id = b.player_id) as spawn_count, " +
                             "(SELECT COALESCE(SUM(monster_count), 0) FROM player_base_monsters m WHERE m.base_owner_id = b.player_id) as total_monsters " +
                             "FROM player_base b ORDER BY b.created_time DESC"))
        {
            try (ResultSet rs = ps.executeQuery())
            {
                sb.append("=== 基地列表 (共 ");

                int totalCount = 0;
                StringBuilder content = new StringBuilder();

                while (rs.next())
                {
                    totalCount++;
                    String playerName = rs.getString("player_name");
                    int instanceId = rs.getInt("instance_id");
                    int maxMonsters = rs.getInt("max_monster_count");
                    boolean canSummonBoss = rs.getBoolean("can_summon_boss");
                    long createdTime = rs.getLong("created_time");
                    int visitorCount = rs.getInt("visitor_count");
                    int spawnCount = rs.getInt("spawn_count");
                    int totalMonsters = rs.getInt("total_monsters");

                    content.append(String.format(
                            "[%d] %s | 副本ID: %d | BOSS權限: %s\n" +
                                    "    創建時間: %s\n" +
                                    "    訪客: %d 人 | 生成點: %d 個 | 怪物: %d/%d\n\n",
                            totalCount, playerName, instanceId,
                            (canSummonBoss ? "✓" : "✗"),
                            sdf.format(new Date(createdTime)),
                            visitorCount, spawnCount, totalMonsters, maxMonsters
                    ));
                }

                sb.append(totalCount).append(" 個) ===\n\n");
                sb.append(content);

                if (totalCount == 0)
                {
                    sb.append("目前還沒有任何基地。\n");
                }
            }
        }
        catch (SQLException e)
        {
            sb.append("讀取基地列表時發生錯誤: ").append(e.getMessage()).append("\n");
            e.printStackTrace();
        }

        txtLog.setText(sb.toString());
        txtLog.setCaretPosition(0);
    }

    private int getCharacterObjectId(String charName)
    {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT charId FROM characters WHERE char_name = ?"))
        {
            ps.setString(1, charName);
            try (ResultSet rs = ps.executeQuery())
            {
                if (rs.next())
                {
                    return rs.getInt("charId");
                }
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return -1;
    }

    private boolean hasBase(int playerId)
    {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT player_id FROM player_base WHERE player_id = ?"))
        {
            ps.setInt(1, playerId);
            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next();
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    private int getVisitorCount(int playerId)
    {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) as cnt FROM player_base_visitors WHERE base_owner_id = ?"))
        {
            ps.setInt(1, playerId);
            try (ResultSet rs = ps.executeQuery())
            {
                if (rs.next())
                {
                    return rs.getInt("cnt");
                }
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return 0;
    }

    private int getMonsterSpawnCount(int playerId)
    {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) as cnt FROM player_base_monsters WHERE base_owner_id = ?"))
        {
            ps.setInt(1, playerId);
            try (ResultSet rs = ps.executeQuery())
            {
                if (rs.next())
                {
                    return rs.getInt("cnt");
                }
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return 0;
    }

    private int getTotalMonsterCount(int playerId)
    {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COALESCE(SUM(monster_count), 0) as total FROM player_base_monsters WHERE base_owner_id = ?"))
        {
            ps.setInt(1, playerId);
            try (ResultSet rs = ps.executeQuery())
            {
                if (rs.next())
                {
                    return rs.getInt("total");
                }
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return 0;
    }
}