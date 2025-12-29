package tools.npceditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import tools.npceditor.NpcDataModel.DropGroup;
import tools.npceditor.NpcDataModel.DropItem;
import tools.npceditor.NpcDataModel.FortuneItem;
import tools.npceditor.NpcDataModel.SkillEntry;
import tools.npceditor.NpcDataModel.Stats;

/**
 * NPC編輯器主視窗 - 優化版
 */
public class NpcEditorFrame extends JFrame {
	private static final String NPC_DIRECTORY = detectNpcDirectory();
	private String currentNpcDirectory = NPC_DIRECTORY;

	// 左側NPC列表
	private JList<String> npcList;
	private DefaultListModel<String> npcListModel;
	private JTextField searchField;

	// 當前編輯的NPC數據
	private NpcDataModel currentNpc;
	private File currentFile;
	private List<File> npcFiles;
	private List<NpcFileEntry> npcEntries;
	private List<NpcFileEntry> allNpcEntries;

	private static class NpcFileEntry {
		File file;
		NpcDataModel npc;
		NpcFileEntry(File file, NpcDataModel npc) {
			this.file = file;
			this.npc = npc;
		}
	}

	// 主題顏色 - 高對比度配色
	private static final Color BG_COLOR = new Color(30, 30, 30);
	private static final Color FG_COLOR = new Color(240, 240, 240);
	private static final Color PANEL_BG = new Color(45, 45, 45);
	private static final Color INPUT_BG = new Color(55, 55, 55);
	private static final Color BORDER_COLOR = new Color(100, 100, 100);
	private static final Color SELECTED_COLOR = new Color(70, 130, 180);
	private static final Color LABEL_COLOR = new Color(200, 200, 200);

	// 所有輸入框
	private JTextField idField, displayIdField, levelField, nameField, titleField;
	private JComboBox<String> typeCombo, elementCombo, raceCombo, sexCombo;
	private JTextField strField, intField, dexField, witField, conField, menField;
	private JTextField maxHpField, hpRegenField, maxMpField, mpRegenField;
	private JTextField pAtkField, mAtkField, pDefField, mDefField;
	private JTextField walkSpeedField, runSpeedField;
	private JTextField collisionRadiusField, collisionHeightField;

	// 表格
	private JTable skillTable, dropTable, fortuneTable;
	private DefaultTableModel skillTableModel, dropTableModel, fortuneTableModel;

	// 數值格式化器 - 避免科學計數法
	private static final DecimalFormat numberFormat = new DecimalFormat("#.##########");

	public NpcEditorFrame() {
		npcEntries = new ArrayList<>();
		allNpcEntries = new ArrayList<>();
		setupDarkTheme();
		updateTitle();
		setSize(1600, 1000);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(null);
		initComponents();

		// 異步加載NPC列表
		loadNpcListAsync();
	}

	private void loadNpcListAsync() {
		// 在後台線程加載
		new Thread(() -> {
			try {
				loadNpcList();
			} catch (Exception e) {
				SwingUtilities.invokeLater(() ->
						JOptionPane.showMessageDialog(this,
								"加載NPC列表失敗: " + e.getMessage()));
			}
		}).start();
	}

	private void setupDarkTheme() {
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
			UIManager.put("control", PANEL_BG);
			UIManager.put("nimbusBase", new Color(18, 30, 49));
			UIManager.put("nimbusLightBackground", INPUT_BG);
			UIManager.put("nimbusSelectionBackground", SELECTED_COLOR);
			UIManager.put("text", FG_COLOR);
			UIManager.put("Panel.background", PANEL_BG);
			UIManager.put("TextField.background", INPUT_BG);
			UIManager.put("TextField.foreground", FG_COLOR);
		} catch (Exception e) {
			System.err.println("無法設置黑色主題: " + e.getMessage());
		}
		getContentPane().setBackground(BG_COLOR);
	}

	private void updateTitle() {
		String dirName = new File(currentNpcDirectory).getName();
		setTitle("NPC編輯器 - L2J Mobius [" + dirName + "]");
	}

	private void initComponents() {
		JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		mainSplitPane.setDividerLocation(350);
		mainSplitPane.setBackground(BG_COLOR);
		mainSplitPane.setLeftComponent(createLeftPanel());
		mainSplitPane.setRightComponent(createRightPanel());
		add(mainSplitPane);
	}

	private JPanel createLeftPanel() {
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBackground(PANEL_BG);
		panel.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(BORDER_COLOR), "NPC列表", 0, 0, null, FG_COLOR));

		// 搜索面板
		JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
		searchPanel.setBackground(PANEL_BG);
		searchField = new JTextField();
		styleTextField(searchField);

		JButton searchButton = new JButton("搜索");
		styleButton(searchButton);
		searchButton.setPreferredSize(new Dimension(80, 35));
		searchButton.addActionListener(e -> performSearch());
		searchField.addActionListener(e -> performSearch());

		JButton clearButton = new JButton("清除");
		styleButton(clearButton);
		clearButton.setPreferredSize(new Dimension(80, 35));
		clearButton.addActionListener(e -> clearSearch());

		JPanel searchButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 3));
		searchButtonPanel.setBackground(PANEL_BG);
		searchButtonPanel.add(searchButton);
		searchButtonPanel.add(clearButton);

		searchPanel.add(searchField, BorderLayout.CENTER);
		searchPanel.add(searchButtonPanel, BorderLayout.EAST);

		// NPC列表
		npcListModel = new DefaultListModel<>();
		npcList = new JList<>(npcListModel);
		npcList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		npcList.setBackground(INPUT_BG);
		npcList.setForeground(FG_COLOR);
		npcList.setSelectionBackground(SELECTED_COLOR);
		npcList.setSelectionForeground(Color.WHITE);
		npcList.setFont(new Font("微軟正黑體", Font.PLAIN, 13));
		npcList.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent evt) {
				if (evt.getClickCount() == 2) {
					loadSelectedNpc();
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane(npcList);
		scrollPane.getViewport().setBackground(INPUT_BG);

		// 按鈕面板
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
		buttonPanel.setBackground(PANEL_BG);

		JButton refreshButton = new JButton("刷新列表");
		styleButton(refreshButton);
		refreshButton.setPreferredSize(new Dimension(100, 35));
		refreshButton.addActionListener(e -> loadNpcListAsync());

		JButton showNpcsButton = new JButton("只看npcs");
		styleButton(showNpcsButton);
		showNpcsButton.setPreferredSize(new Dimension(100, 35));
		showNpcsButton.addActionListener(e -> filterNpcsOnly());

		JButton customOnlyButton = new JButton("只看custom");
		styleButton(customOnlyButton);
		customOnlyButton.setPreferredSize(new Dimension(100, 35));
		customOnlyButton.addActionListener(e -> filterCustomNpcs());

		buttonPanel.add(refreshButton);
		buttonPanel.add(showNpcsButton);
		buttonPanel.add(customOnlyButton);

		panel.add(searchPanel, BorderLayout.NORTH);
		panel.add(scrollPane, BorderLayout.CENTER);
		panel.add(buttonPanel, BorderLayout.SOUTH);
		return panel;
	}

	private void performSearch() {
		String searchText = searchField.getText().trim().toLowerCase();
		if (searchText.isEmpty()) {
			clearSearch();
			return;
		}

		// 等待加載完成
		if (allNpcEntries.isEmpty()) {
			JOptionPane.showMessageDialog(this,
					"NPC列表為空！\n當前目錄: " + currentNpcDirectory + "\n請檢查目錄是否正確或點擊「刷新列表」",
					"提示", JOptionPane.WARNING_MESSAGE);
			return;
		}

		npcEntries.clear();

		for (NpcFileEntry entry : allNpcEntries) {
			NpcDataModel npc = entry.npc;
			boolean matches = String.valueOf(npc.getId()).contains(searchText) ||
					npc.getName().toLowerCase().contains(searchText) ||
					String.valueOf(npc.getLevel()).contains(searchText) ||
					npc.getType().toLowerCase().contains(searchText);

			if (matches) {
				npcEntries.add(entry);
			}
		}

		// 排序并显示
		sortAndDisplayNpcList();

		int resultCount = npcEntries.size();
		if (resultCount == 0) {
			JOptionPane.showMessageDialog(this, "沒有找到匹配的NPC",
					"搜索結果", JOptionPane.INFORMATION_MESSAGE);
		} else {
			JOptionPane.showMessageDialog(this, "找到 " + resultCount + " 個NPC",
					"搜索結果", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void clearSearch() {
		searchField.setText("");
		npcEntries.clear();
		for (NpcFileEntry entry : allNpcEntries) {
			npcEntries.add(entry);
		}

		// 排序并显示
		sortAndDisplayNpcList();
		updateTitle();
	}

	private void selectNpcDirectory() {
		JFileChooser fileChooser = new JFileChooser(currentNpcDirectory);
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setDialogTitle("選擇NPC目錄");
		if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			currentNpcDirectory = fileChooser.getSelectedFile().getAbsolutePath();
			updateTitle();
			loadNpcList();
		}
	}

	private JPanel createRightPanel() {
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBackground(PANEL_BG);

		// 工具欄
		JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
		toolBar.setBackground(PANEL_BG);

		JButton saveButton = new JButton("保存");
		styleButton(saveButton);
		saveButton.setPreferredSize(new Dimension(100, 40));
		saveButton.setFont(new Font("微軟正黑體", Font.BOLD, 14));
		saveButton.addActionListener(e -> saveCurrentNpc());

		JButton reloadButton = new JButton("重新加載");
		styleButton(reloadButton);
		reloadButton.setPreferredSize(new Dimension(120, 40));
		reloadButton.setFont(new Font("微軟正黑體", Font.BOLD, 14));
		reloadButton.addActionListener(e -> loadSelectedNpc());

		JButton cloneButton = new JButton("複製創建新ID");
		styleButton(cloneButton);
		cloneButton.setPreferredSize(new Dimension(140, 40));
		cloneButton.setFont(new Font("微軟正黑體", Font.BOLD, 14));
		cloneButton.addActionListener(e -> cloneNpcWithNewId());

		toolBar.add(saveButton);
		toolBar.add(reloadButton);
		toolBar.add(cloneButton);

		// 單頁顯示所有內容
		JPanel contentPanel = createAllInOnePanel();
		JScrollPane scrollPane = new JScrollPane(contentPanel);
		scrollPane.getViewport().setBackground(PANEL_BG);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);

		panel.add(toolBar, BorderLayout.NORTH);
		panel.add(scrollPane, BorderLayout.CENTER);
		return panel;
	}

	/**
	 * 創建單頁顯示所有信息的面板
	 */
	private JPanel createAllInOnePanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBackground(PANEL_BG);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(8, 8, 8, 8);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		int row = 0;

		// ========== 基本信息 ==========
		addSectionTitle(panel, "基本信息", gbc, row++);

		idField = new JTextField(15);
		idField.setEditable(false);
		addLabelAndField(panel, "NPC ID:", idField, gbc, row++);

		displayIdField = new JTextField(15);
		addLabelAndField(panel, "Display ID:", displayIdField, gbc, row++);

		nameField = new JTextField(25);
		addLabelAndField(panel, "名稱:", nameField, gbc, row++);

		titleField = new JTextField(25);
		addLabelAndField(panel, "稱號:", titleField, gbc, row++);

		levelField = new JTextField(10);
		addLabelAndField(panel, "等級:", levelField, gbc, row++);

		String[] types = {"Monster", "RaidBoss", "GrandBoss", "FriendlyNpc", "Guard"};
		typeCombo = new JComboBox<>(types);
		styleComboBox(typeCombo);
		addLabelAndCombo(panel, "類型:", typeCombo, gbc, row++);

		String[] elements = {"", "FIRE", "WATER", "WIND", "EARTH", "HOLY", "DARK"};
		elementCombo = new JComboBox<>(elements);
		styleComboBox(elementCombo);
		addLabelAndCombo(panel, "屬性:", elementCombo, gbc, row++);

		String[] races = {"HUMANOID", "UNDEAD", "BEAST", "PLANT", "BUG", "DRAGON", "GIANT", "ELEMENTAL", "CONSTRUCT", "DEMONIC"};
		raceCombo = new JComboBox<>(races);
		styleComboBox(raceCombo);
		addLabelAndCombo(panel, "種族:", raceCombo, gbc, row++);

		String[] sexes = {"MALE", "FEMALE", "ETC"};
		sexCombo = new JComboBox<>(sexes);
		styleComboBox(sexCombo);
		addLabelAndCombo(panel, "性別:", sexCombo, gbc, row++);

		// ========== 基礎屬性 ==========
		addSectionTitle(panel, "基礎屬性", gbc, row++);

		strField = new JTextField(10);
		addLabelAndField(panel, "STR (力量):", strField, gbc, row++);

		intField = new JTextField(10);
		addLabelAndField(panel, "INT (智力):", intField, gbc, row++);

		dexField = new JTextField(10);
		addLabelAndField(panel, "DEX (敏捷):", dexField, gbc, row++);

		witField = new JTextField(10);
		addLabelAndField(panel, "WIT (機智):", witField, gbc, row++);

		conField = new JTextField(10);
		addLabelAndField(panel, "CON (體質):", conField, gbc, row++);

		menField = new JTextField(10);
		addLabelAndField(panel, "MEN (精神):", menField, gbc, row++);

		// ========== 生命值與魔力 ==========
		addSectionTitle(panel, "生命值與魔力", gbc, row++);

		maxHpField = new JTextField(20);
		addLabelAndField(panel, "最大HP:", maxHpField, gbc, row++);

		hpRegenField = new JTextField(15);
		addLabelAndField(panel, "HP回復:", hpRegenField, gbc, row++);

		maxMpField = new JTextField(20);
		addLabelAndField(panel, "最大MP:", maxMpField, gbc, row++);

		mpRegenField = new JTextField(15);
		addLabelAndField(panel, "MP回復:", mpRegenField, gbc, row++);

		// ========== 攻擊與防禦 ==========
		addSectionTitle(panel, "攻擊與防禦", gbc, row++);

		pAtkField = new JTextField(15);
		addLabelAndField(panel, "物理攻擊:", pAtkField, gbc, row++);

		mAtkField = new JTextField(15);
		addLabelAndField(panel, "魔法攻擊:", mAtkField, gbc, row++);

		pDefField = new JTextField(15);
		addLabelAndField(panel, "物理防禦:", pDefField, gbc, row++);

		mDefField = new JTextField(15);
		addLabelAndField(panel, "魔法防禦:", mDefField, gbc, row++);

		// ========== 移動速度 ==========
		addSectionTitle(panel, "移動速度", gbc, row++);

		walkSpeedField = new JTextField(10);
		addLabelAndField(panel, "行走速度:", walkSpeedField, gbc, row++);

		runSpeedField = new JTextField(10);
		addLabelAndField(panel, "奔跑速度:", runSpeedField, gbc, row++);

		// ========== 碰撞體積 ==========
		addSectionTitle(panel, "碰撞體積", gbc, row++);

		collisionRadiusField = new JTextField(10);
		addLabelAndField(panel, "碰撞半徑:", collisionRadiusField, gbc, row++);

		collisionHeightField = new JTextField(10);
		addLabelAndField(panel, "碰撞高度:", collisionHeightField, gbc, row++);

		// ========== 技能列表 ==========
		addSectionTitle(panel, "技能列表", gbc, row++);

		String[] skillColumns = {"技能ID", "技能名稱", "等級"};
		skillTableModel = new DefaultTableModel(skillColumns, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return column == 0 || column == 2; // 只有ID和等級可編輯
			}
		};
		skillTable = new JTable(skillTableModel);
		styleTable(skillTable);
		skillTable.getColumnModel().getColumn(0).setPreferredWidth(80);
		skillTable.getColumnModel().getColumn(1).setPreferredWidth(200);
		skillTable.getColumnModel().getColumn(2).setPreferredWidth(60);

		JScrollPane skillScroll = new JScrollPane(skillTable);
		skillScroll.setPreferredSize(new Dimension(600, 150));
		skillScroll.getViewport().setBackground(INPUT_BG);

		gbc.gridx = 0;
		gbc.gridy = row++;
		gbc.gridwidth = 2;
		panel.add(skillScroll, gbc);
		gbc.gridwidth = 1;

		// ========== 掉落列表 ==========
		addSectionTitle(panel, "掉落列表", gbc, row++);

		String[] dropColumns = {"物品ID", "物品名稱", "最小", "最大", "機率(%)", "組機率(%)"};
		dropTableModel = new DefaultTableModel(dropColumns, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		dropTable = new JTable(dropTableModel);
		styleTable(dropTable);

		JScrollPane dropScroll = new JScrollPane(dropTable);
		dropScroll.setPreferredSize(new Dimension(600, 150));
		dropScroll.getViewport().setBackground(INPUT_BG);

		gbc.gridx = 0;
		gbc.gridy = row++;
		gbc.gridwidth = 2;
		panel.add(dropScroll, gbc);
		gbc.gridwidth = 1;

		// ========== 幸運掉落 ==========
		addSectionTitle(panel, "幸運掉落", gbc, row++);

		String[] fortuneColumns = {"物品ID", "物品名稱", "最小", "最大", "機率(%)"};
		fortuneTableModel = new DefaultTableModel(fortuneColumns, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		fortuneTable = new JTable(fortuneTableModel);
		styleTable(fortuneTable);

		JScrollPane fortuneScroll = new JScrollPane(fortuneTable);
		fortuneScroll.setPreferredSize(new Dimension(600, 150));
		fortuneScroll.getViewport().setBackground(INPUT_BG);

		gbc.gridx = 0;
		gbc.gridy = row++;
		gbc.gridwidth = 2;
		panel.add(fortuneScroll, gbc);

		return panel;
	}

	/**
	 * 添加分區標題
	 */
	private void addSectionTitle(JPanel panel, String title, GridBagConstraints gbc, int row) {
		JLabel titleLabel = new JLabel(title);
		titleLabel.setFont(new Font("微軟正黑體", Font.BOLD, 16));
		titleLabel.setForeground(new Color(100, 180, 255));

		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
		gbc.insets = new Insets(15, 5, 10, 5);
		panel.add(titleLabel, gbc);
		gbc.gridwidth = 1;
		gbc.insets = new Insets(8, 8, 8, 8);
	}

	private void loadNpcList() {
		// 先清空列表
		npcListModel.clear();
		npcEntries.clear();
		allNpcEntries.clear();

		npcFiles = NpcXmlParser.scanNpcFiles(currentNpcDirectory);

		if (npcFiles.isEmpty()) {
			SwingUtilities.invokeLater(() ->
					JOptionPane.showMessageDialog(this,
							"未找到NPC文件！\n當前目錄: " + currentNpcDirectory,
							"警告", JOptionPane.WARNING_MESSAGE));
			return;
		}

		System.out.println("開始加載 " + npcFiles.size() + " 個文件...");

		for (File file : npcFiles) {
			try {
				List<NpcDataModel> npcsInFile = NpcXmlParser.loadAllNpcsFromFile(file);
				for (NpcDataModel npc : npcsInFile) {
					NpcFileEntry entry = new NpcFileEntry(file, npc);
					npcEntries.add(entry);
					allNpcEntries.add(entry);

					final String displayText = String.format("[%d] %s (Lv.%d)",
							npc.getId(), npc.getName(), npc.getLevel());
					SwingUtilities.invokeLater(() -> npcListModel.addElement(displayText));
				}
			} catch (Exception e) {
				System.err.println("無法加載文件: " + file.getName() + " - " + e.getMessage());
			}
		}

		final int totalCount = allNpcEntries.size();

		// 在UI线程中排序并显示
		SwingUtilities.invokeLater(() -> {
			sortAndDisplayNpcList();
			System.out.println("已加載 " + totalCount + " 個NPC");
			updateTitle();
		});
	}

	private void loadSelectedNpc() {
		int selectedIndex = npcList.getSelectedIndex();
		if (selectedIndex < 0 || selectedIndex >= npcEntries.size()) {
			System.err.println("無效的選擇索引: " + selectedIndex + ", 列表大小: " + npcEntries.size());
			return;
		}

		try {
			NpcFileEntry entry = npcEntries.get(selectedIndex);
			currentFile = entry.file;
			currentNpc = entry.npc;
			System.out.println("加載NPC: [" + currentNpc.getId() + "] " + currentNpc.getName());
			displayNpcData();
		} catch (Exception e) {
			System.err.println("加載NPC失敗: " + e.getMessage());
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "加載NPC失敗: " + e.getMessage());
		}
	}

	/**
	 * 格式化數值 - 避免科學計數法
	 */
	private String formatNumber(double value) {
		if (value == 0) return "0";

		// 如果是整數，不顯示小數點
		if (value == (long) value) {
			return String.format("%d", (long) value);
		}

		// 使用DecimalFormat避免科學計數法
		return numberFormat.format(value);
	}

	private void displayNpcData() {
		if (currentNpc == null) {
			System.err.println("currentNpc 為 null，無法顯示數據");
			return;
		}

		System.out.println("顯示NPC數據: " + currentNpc.getName());

		// 基本信息
		idField.setText(String.valueOf(currentNpc.getId()));
		displayIdField.setText(String.valueOf(currentNpc.getDisplayId()));
		levelField.setText(String.valueOf(currentNpc.getLevel()));
		nameField.setText(currentNpc.getName());
		titleField.setText(currentNpc.getTitle() != null ? currentNpc.getTitle() : "");
		typeCombo.setSelectedItem(currentNpc.getType());
		elementCombo.setSelectedItem(currentNpc.getElement() != null ? currentNpc.getElement() : "");
		raceCombo.setSelectedItem(currentNpc.getRace());
		sexCombo.setSelectedItem(currentNpc.getSex());

		// 屬性值 - 使用formatNumber避免科學計數法
		Stats stats = currentNpc.getStats();
		strField.setText(String.valueOf(stats.getStr()));
		intField.setText(String.valueOf(stats.getIntVal()));
		dexField.setText(String.valueOf(stats.getDex()));
		witField.setText(String.valueOf(stats.getWit()));
		conField.setText(String.valueOf(stats.getCon()));
		menField.setText(String.valueOf(stats.getMen()));

		maxHpField.setText(formatNumber(stats.getMaxHp()));
		hpRegenField.setText(formatNumber(stats.getHpRegen()));
		maxMpField.setText(formatNumber(stats.getMaxMp()));
		mpRegenField.setText(formatNumber(stats.getMpRegen()));
		pAtkField.setText(formatNumber(stats.getPhysicalAttack()));
		mAtkField.setText(formatNumber(stats.getMagicalAttack()));
		pDefField.setText(formatNumber(stats.getPhysicalDefence()));
		mDefField.setText(formatNumber(stats.getMagicalDefence()));

		walkSpeedField.setText(String.valueOf(stats.getWalkSpeed()));
		runSpeedField.setText(String.valueOf(stats.getRunSpeed()));
		collisionRadiusField.setText(formatNumber(stats.getCollisionRadius()));
		collisionHeightField.setText(formatNumber(stats.getCollisionHeight()));

		// 技能列表
		skillTableModel.setRowCount(0);
		for (SkillEntry skill : currentNpc.getSkills()) {
			skillTableModel.addRow(new Object[]{
					skill.getSkillId(),
					skill.getSkillName() != null ? skill.getSkillName() : "Unknown Skill",
					skill.getLevel()
			});
		}

		// 掉落列表
		dropTableModel.setRowCount(0);
		for (DropGroup group : currentNpc.getDropGroups()) {
			for (DropItem item : group.getItems()) {
				dropTableModel.addRow(new Object[]{
						item.getItemId(),
						item.getItemName() != null ? item.getItemName() : "Unknown Item",
						item.getMin(), item.getMax(), item.getChance(), group.getChance()
				});
			}
		}

		// 幸運掉落
		fortuneTableModel.setRowCount(0);
		for (FortuneItem item : currentNpc.getFortuneItems()) {
			fortuneTableModel.addRow(new Object[]{
					item.getItemId(),
					item.getItemName() != null ? item.getItemName() : "Unknown Item",
					item.getMin(), item.getMax(), item.getChance()
			});
		}
	}

	private void saveCurrentNpc() {
		if (currentNpc == null || currentFile == null) {
			JOptionPane.showMessageDialog(this, "沒有選中的NPC!");
			return;
		}

		try {
			updateNpcFromUI();
			NpcXmlParser.saveToFile(currentNpc, currentFile);
			JOptionPane.showMessageDialog(this, "保存成功!");
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "保存失敗: " + e.getMessage());
		}
	}

	private void updateNpcFromUI() {
		currentNpc.setDisplayId(Integer.parseInt(displayIdField.getText()));
		currentNpc.setLevel(Integer.parseInt(levelField.getText()));
		currentNpc.setName(nameField.getText());
		currentNpc.setTitle(titleField.getText());
		currentNpc.setType((String) typeCombo.getSelectedItem());
		currentNpc.setElement((String) elementCombo.getSelectedItem());
		currentNpc.setRace((String) raceCombo.getSelectedItem());
		currentNpc.setSex((String) sexCombo.getSelectedItem());

		Stats stats = currentNpc.getStats();
		stats.setStr(Integer.parseInt(strField.getText()));
		stats.setIntVal(Integer.parseInt(intField.getText()));
		stats.setDex(Integer.parseInt(dexField.getText()));
		stats.setWit(Integer.parseInt(witField.getText()));
		stats.setCon(Integer.parseInt(conField.getText()));
		stats.setMen(Integer.parseInt(menField.getText()));
		stats.setMaxHp(Double.parseDouble(maxHpField.getText()));
		stats.setHpRegen(Double.parseDouble(hpRegenField.getText()));
		stats.setMaxMp(Double.parseDouble(maxMpField.getText()));
		stats.setMpRegen(Double.parseDouble(mpRegenField.getText()));
		stats.setPhysicalAttack(Double.parseDouble(pAtkField.getText()));
		stats.setMagicalAttack(Double.parseDouble(mAtkField.getText()));
		stats.setPhysicalDefence(Double.parseDouble(pDefField.getText()));
		stats.setMagicalDefence(Double.parseDouble(mDefField.getText()));
		stats.setWalkSpeed(Integer.parseInt(walkSpeedField.getText()));
		stats.setRunSpeed(Integer.parseInt(runSpeedField.getText()));
		stats.setCollisionRadius(Double.parseDouble(collisionRadiusField.getText()));
		stats.setCollisionHeight(Double.parseDouble(collisionHeightField.getText()));
	}

	private void styleTextField(JTextField field) {
		field.setBackground(INPUT_BG);
		field.setForeground(FG_COLOR);
		field.setCaretColor(FG_COLOR);
		field.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(BORDER_COLOR),
				BorderFactory.createEmptyBorder(3, 5, 3, 5)
		));
		field.setFont(new Font("Consolas", Font.PLAIN, 13));
	}

	private void styleButton(JButton button) {
		button.setBackground(new Color(70, 70, 70));
		button.setForeground(FG_COLOR);
		button.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(BORDER_COLOR),
				BorderFactory.createEmptyBorder(5, 15, 5, 15)
		));
		button.setFocusPainted(false);
		button.setFont(new Font("微軟正黑體", Font.PLAIN, 12));
	}

	private void styleComboBox(JComboBox<?> combo) {
		combo.setBackground(INPUT_BG);
		combo.setForeground(FG_COLOR);
		combo.setFont(new Font("微軟正黑體", Font.PLAIN, 12));
	}

	private void styleTable(JTable table) {
		table.setBackground(INPUT_BG);
		table.setForeground(FG_COLOR);
		table.setGridColor(BORDER_COLOR);
		table.setSelectionBackground(SELECTED_COLOR);
		table.setSelectionForeground(Color.WHITE);
		table.setRowHeight(25);
		table.setFont(new Font("微軟正黑體", Font.PLAIN, 12));
		table.getTableHeader().setBackground(new Color(60, 60, 60));
		table.getTableHeader().setForeground(FG_COLOR);
		table.getTableHeader().setFont(new Font("微軟正黑體", Font.BOLD, 12));
	}

	private void addLabelAndField(JPanel panel, String labelText, JTextField field,
								  GridBagConstraints gbc, int row) {
		JLabel label = new JLabel(labelText);
		label.setForeground(LABEL_COLOR);
		label.setFont(new Font("微軟正黑體", Font.PLAIN, 13));

		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.weightx = 0;
		panel.add(label, gbc);

		styleTextField(field);
		gbc.gridx = 1;
		gbc.weightx = 1.0;
		panel.add(field, gbc);
	}

	private void addLabelAndCombo(JPanel panel, String labelText, JComboBox<String> combo,
								  GridBagConstraints gbc, int row) {
		JLabel label = new JLabel(labelText);
		label.setForeground(LABEL_COLOR);
		label.setFont(new Font("微軟正黑體", Font.PLAIN, 13));

		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.weightx = 0;
		panel.add(label, gbc);

		gbc.gridx = 1;
		gbc.weightx = 1.0;
		panel.add(combo, gbc);
	}

	private static String detectNpcDirectory() {
		String[] possiblePaths = {
				"./game/data/stats/npcs",
				"./dist/game/data/stats/npcs",
				"../game/data/stats/npcs",
				System.getProperty("user.dir") + "/game/data/stats/npcs"
		};

		for (String path : possiblePaths) {
			File dir = new File(path);
			if (dir.exists() && dir.isDirectory()) {
				File[] files = dir.listFiles((d, name) -> name.endsWith(".xml"));
				if (files != null && files.length > 0) {
					return dir.getAbsolutePath();
				}
			}
		}
		return "./game/data/stats/npcs";
	}

	private void filterCustomNpcs() {
		if (allNpcEntries.isEmpty()) {
			JOptionPane.showMessageDialog(this, "請先加載NPC列表",
					"提示", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		npcEntries.clear();

		for (NpcFileEntry entry : allNpcEntries) {
			// 檢查文件路徑是否包含 "custom"
			String filePath = entry.file.getAbsolutePath();
			if (filePath.contains("custom") || filePath.contains("Custom")) {
				npcEntries.add(entry);
			}
		}

		// 排序并显示
		sortAndDisplayNpcList();

		int resultCount = npcEntries.size();
		if (resultCount == 0) {
			JOptionPane.showMessageDialog(this, "沒有找到custom目錄中的NPC",
					"結果", JOptionPane.INFORMATION_MESSAGE);
		} else {
			JOptionPane.showMessageDialog(this, "找到 " + resultCount + " 個custom NPC",
					"結果", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void cloneNpcWithNewId() {
		if (currentNpc == null || currentFile == null) {
			JOptionPane.showMessageDialog(this, "請先選擇要複製的NPC!",
					"錯誤", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// 顯示輸入對話框
		String message = String.format("被複製的NPC ID: %d\n請輸入新的NPC ID (避免重複):",
				currentNpc.getId());
		String input = JOptionPane.showInputDialog(this, message, "複製NPC",
				JOptionPane.QUESTION_MESSAGE);

		if (input == null || input.trim().isEmpty()) {
			return; // 用戶取消
		}

		int newId;
		try {
			newId = Integer.parseInt(input.trim());
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "無效的ID格式，請輸入數字!",
					"錯誤", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// 檢查ID是否重複
		if (isNpcIdExists(newId)) {
			JOptionPane.showMessageDialog(this,
					String.format("NPC ID %d 已存在，無法創建!\n請使用其他ID。", newId),
					"ID重複", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// 執行複製
		try {
			NpcDataModel clonedNpc = cloneNpcData(currentNpc, newId);
			NpcXmlParser.addNpcToFile(clonedNpc, currentFile);

			JOptionPane.showMessageDialog(this,
					String.format("成功複製NPC!\n新ID: %d\n已添加到文件: %s",
							newId, currentFile.getName()),
					"成功", JOptionPane.INFORMATION_MESSAGE);

			// 重新加載列表
			loadNpcListAsync();

		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "複製失敗: " + e.getMessage(),
					"錯誤", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	/**
	 * 檢查NPC ID是否已存在
	 */
	private boolean isNpcIdExists(int id) {
		for (NpcFileEntry entry : allNpcEntries) {
			if (entry.npc.getId() == id) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 複製NPC數據
	 */
	private NpcDataModel cloneNpcData(NpcDataModel source, int newId) {
		NpcDataModel cloned = new NpcDataModel();

		// 基本屬性
		cloned.setId(newId);
		cloned.setDisplayId(source.getDisplayId());
		cloned.setLevel(source.getLevel());
		cloned.setType(source.getType());
		cloned.setName(source.getName() + " (Clone)");
		cloned.setTitle(source.getTitle());
		cloned.setElement(source.getElement());
		cloned.setRace(source.getRace());
		cloned.setSex(source.getSex());

		// 複製Stats
		Stats sourceStats = source.getStats();
		Stats clonedStats = cloned.getStats();
		clonedStats.setStr(sourceStats.getStr());
		clonedStats.setIntVal(sourceStats.getIntVal());
		clonedStats.setDex(sourceStats.getDex());
		clonedStats.setWit(sourceStats.getWit());
		clonedStats.setCon(sourceStats.getCon());
		clonedStats.setMen(sourceStats.getMen());
		clonedStats.setMaxHp(sourceStats.getMaxHp());
		clonedStats.setHpRegen(sourceStats.getHpRegen());
		clonedStats.setMaxMp(sourceStats.getMaxMp());
		clonedStats.setMpRegen(sourceStats.getMpRegen());
		clonedStats.setPhysicalAttack(sourceStats.getPhysicalAttack());
		clonedStats.setMagicalAttack(sourceStats.getMagicalAttack());
		clonedStats.setAttackRandom(sourceStats.getAttackRandom());
		clonedStats.setCritical(sourceStats.getCritical());
		clonedStats.setAccuracy(sourceStats.getAccuracy());
		clonedStats.setAttackSpeed(sourceStats.getAttackSpeed());
		clonedStats.setAttackType(sourceStats.getAttackType());
		clonedStats.setAttackRange(sourceStats.getAttackRange());
		clonedStats.setPhysicalDefence(sourceStats.getPhysicalDefence());
		clonedStats.setMagicalDefence(sourceStats.getMagicalDefence());
		clonedStats.setWalkSpeed(sourceStats.getWalkSpeed());
		clonedStats.setRunSpeed(sourceStats.getRunSpeed());
		clonedStats.setHitTime(sourceStats.getHitTime());
		clonedStats.setCollisionRadius(sourceStats.getCollisionRadius());
		clonedStats.setCollisionHeight(sourceStats.getCollisionHeight());

		// 複製技能
		for (SkillEntry skill : source.getSkills()) {
			SkillEntry clonedSkill = new SkillEntry(skill.getSkillId(), skill.getLevel());
			clonedSkill.setSkillName(skill.getSkillName());
			cloned.getSkills().add(clonedSkill);
		}

		// 複製掉落組
		for (DropGroup group : source.getDropGroups()) {
			DropGroup clonedGroup = new DropGroup();
			clonedGroup.setChance(group.getChance());
			for (DropItem item : group.getItems()) {
				DropItem clonedItem = new DropItem(item.getItemId(), item.getMin(),
						item.getMax(), item.getChance());
				clonedItem.setItemName(item.getItemName());
				clonedGroup.getItems().add(clonedItem);
			}
			cloned.getDropGroups().add(clonedGroup);
		}

		// 複製幸運掉落
		for (FortuneItem item : source.getFortuneItems()) {
			FortuneItem clonedItem = new FortuneItem(item.getItemId(), item.getMin(),
					item.getMax(), item.getChance());
			clonedItem.setItemName(item.getItemName());
			cloned.getFortuneItems().add(clonedItem);
		}

		return cloned;
	}

	/**
	 * 对NPC列表按ID排序并更新显示
	 */
	private void sortAndDisplayNpcList() {
		// 按NPC ID排序
		npcEntries.sort((a, b) -> Integer.compare(a.npc.getId(), b.npc.getId()));

		// 清空并重新添加到显示列表
		npcListModel.clear();
		for (NpcFileEntry entry : npcEntries) {
			NpcDataModel npc = entry.npc;
			npcListModel.addElement(String.format("[%d] %s (Lv.%d)",
					npc.getId(), npc.getName(), npc.getLevel()));
		}
	}

	/**
	 * 只显示npcs目录中的NPC（不包括custom子目录）
	 */
	private void filterNpcsOnly() {
		if (allNpcEntries.isEmpty()) {
			JOptionPane.showMessageDialog(this, "請先加載NPC列表",
					"提示", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		npcEntries.clear();

		for (NpcFileEntry entry : allNpcEntries) {
			// 檢查文件路徑：在npcs目錄下但不在custom子目錄
			String filePath = entry.file.getAbsolutePath();
			if ((filePath.contains("npcs") || filePath.contains("Npcs")) &&
					!filePath.contains("custom") && !filePath.contains("Custom")) {
				npcEntries.add(entry);
			}
		}

		// 排序并显示
		sortAndDisplayNpcList();

		int resultCount = npcEntries.size();
		if (resultCount == 0) {
			JOptionPane.showMessageDialog(this, "沒有找到npcs目錄中的NPC",
					"結果", JOptionPane.INFORMATION_MESSAGE);
		} else {
			JOptionPane.showMessageDialog(this, "找到 " + resultCount + " 個npcs目錄的NPC",
					"結果", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			NpcEditorFrame frame = new NpcEditorFrame();
			frame.setVisible(true);
		});
	}
}