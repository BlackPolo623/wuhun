/*
 * Copyright (c) 2013 L2jMobius
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.loginserver.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.l2jmobius.commons.config.InterfaceConfig;
import org.l2jmobius.commons.ui.DarkTheme;
import org.l2jmobius.commons.ui.LineLimitListener;
import org.l2jmobius.commons.ui.SplashScreen;
import org.l2jmobius.loginserver.GameServerTable;
import org.l2jmobius.loginserver.GameServerTable.GameServerInfo;
import org.l2jmobius.loginserver.LoginController;
import org.l2jmobius.loginserver.LoginServer;
import org.l2jmobius.loginserver.network.gameserverpackets.ServerStatus;

/**
 * @author Mobius
 */
public class Gui
{
	// NPC编辑器风格的深色主题配色
	private static final Color BG_COLOR = new Color(30, 30, 30);
	private static final Color FG_COLOR = new Color(240, 240, 240);
	private static final Color PANEL_BG = new Color(45, 45, 45);
	private static final Color INPUT_BG = new Color(55, 55, 55);
	private static final Color BORDER_COLOR = new Color(100, 100, 100);

	private static final String[] SHUTDOWN_OPTIONS =
			{
					"Shutdown",
					"Cancel"
			};
	private static final String[] RESTART_OPTIONS =
			{
					"Restart",
					"Cancel"
			};

	private final JTextArea _txtrConsole;
	private final JCheckBoxMenuItem _chckbxmntmEnabled;
	private JCheckBoxMenuItem _chckbxmntmDisabled;
	private JCheckBoxMenuItem _chckbxmntmGmOnly;

	public Gui()
	{
		// 统一深色主题
		applyDarkTheme();

		// Disable hardware acceleration.
		System.setProperty("sun.java2d.opengl", "false");
		System.setProperty("sun.java2d.d3d", "false");
		System.setProperty("sun.java2d.noddraw", "true");

		if (InterfaceConfig.DARK_THEME)
		{
			DarkTheme.activate();
		}

		// Initialize console.
		_txtrConsole = new JTextArea();
		_txtrConsole.setEditable(false);
		_txtrConsole.setLineWrap(true);
		_txtrConsole.setWrapStyleWord(true);
		_txtrConsole.setDropMode(DropMode.INSERT);
		_txtrConsole.setFont(new Font("Monospaced", Font.PLAIN, 16));
		_txtrConsole.setBackground(INPUT_BG);
		_txtrConsole.setForeground(FG_COLOR);
		_txtrConsole.setCaretColor(FG_COLOR);
		_txtrConsole.getDocument().addDocumentListener(new LineLimitListener(500));

		// Initialize menu items.
		final JMenuBar menuBar = new JMenuBar();
		menuBar.setFont(new Font("Segoe UI", Font.PLAIN, 14));
//		menuBar.setBackground(new Color(190, 190, 190));
//		menuBar.setOpaque(true);
//		menuBar.setBorderPainted(false);

		final JMenu mnActions = new JMenu("Actions");
		mnActions.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mnActions.setForeground(Color.WHITE);
		menuBar.add(mnActions);

		final JMenuItem mntmShutdown = new JMenuItem("Shutdown");
		mntmShutdown.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmShutdown.addActionListener(_ ->
		{
			if (JOptionPane.showOptionDialog(null, "Shutdown LoginServer?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, SHUTDOWN_OPTIONS, SHUTDOWN_OPTIONS[1]) == 0)
			{
				LoginServer.getInstance().shutdown(false);
			}
		});
		mnActions.add(mntmShutdown);

		final JMenuItem mntmRestart = new JMenuItem("Restart");
		mntmRestart.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmRestart.addActionListener(_ ->
		{
			if (JOptionPane.showOptionDialog(null, "Restart LoginServer?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, RESTART_OPTIONS, RESTART_OPTIONS[1]) == 0)
			{
				LoginServer.getInstance().shutdown(true);
			}
		});
		mnActions.add(mntmRestart);

		final JMenu mnReload = new JMenu("Reload");
		mnReload.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mnReload.setForeground(Color.WHITE);
		menuBar.add(mnReload);

		final JMenuItem mntmBannedIps = new JMenuItem("Banned IPs");
		mntmBannedIps.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmBannedIps.addActionListener(_ ->
		{
			LoginController.getInstance().getBannedIps().clear();
			LoginServer.getInstance().loadBanFile();
		});
		mnReload.add(mntmBannedIps);

		final JMenu mnStatus = new JMenu("Status");
		mnStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mnStatus.setForeground(Color.WHITE);
		menuBar.add(mnStatus);

		_chckbxmntmEnabled = new JCheckBoxMenuItem("Enabled");
		_chckbxmntmEnabled.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		_chckbxmntmEnabled.addActionListener(_ ->
		{
			_chckbxmntmEnabled.setSelected(true);
			_chckbxmntmDisabled.setSelected(false);
			_chckbxmntmGmOnly.setSelected(false);
			LoginServer.getInstance().setStatus(ServerStatus.STATUS_NORMAL);
			for (GameServerInfo gsi : GameServerTable.getInstance().getRegisteredGameServers().values())
			{
				gsi.setStatus(ServerStatus.STATUS_NORMAL);
			}

			LoginServer.LOGGER.info("Status changed to enabled.");
		});
		_chckbxmntmEnabled.setSelected(true);
		mnStatus.add(_chckbxmntmEnabled);

		_chckbxmntmDisabled = new JCheckBoxMenuItem("Disabled");
		_chckbxmntmDisabled.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		_chckbxmntmDisabled.addActionListener(_ ->
		{
			_chckbxmntmEnabled.setSelected(false);
			_chckbxmntmDisabled.setSelected(true);
			_chckbxmntmGmOnly.setSelected(false);
			LoginServer.getInstance().setStatus(ServerStatus.STATUS_DOWN);
			for (GameServerInfo gsi : GameServerTable.getInstance().getRegisteredGameServers().values())
			{
				gsi.setStatus(ServerStatus.STATUS_DOWN);
			}

			LoginServer.LOGGER.info("Status changed to disabled.");
		});
		mnStatus.add(_chckbxmntmDisabled);

		_chckbxmntmGmOnly = new JCheckBoxMenuItem("GM only");
		_chckbxmntmGmOnly.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		_chckbxmntmGmOnly.addActionListener(_ ->
		{
			_chckbxmntmEnabled.setSelected(false);
			_chckbxmntmDisabled.setSelected(false);
			_chckbxmntmGmOnly.setSelected(true);
			LoginServer.getInstance().setStatus(ServerStatus.STATUS_GM_ONLY);
			for (GameServerInfo gsi : GameServerTable.getInstance().getRegisteredGameServers().values())
			{
				gsi.setStatus(ServerStatus.STATUS_GM_ONLY);
			}

			LoginServer.LOGGER.info("Status changed to GM only.");
		});
		mnStatus.add(_chckbxmntmGmOnly);

		final JMenu mnFont = new JMenu("Font");
		mnFont.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mnFont.setForeground(Color.WHITE);
		menuBar.add(mnFont);

		final String[] fonts =
				{
						"16",
						"21",
						"27",
						"33"
				};
		for (String font : fonts)
		{
			final JMenuItem mntmFont = new JMenuItem(font);
			mntmFont.setFont(new Font("Segoe UI", Font.PLAIN, 13));
			mntmFont.addActionListener(_ -> _txtrConsole.setFont(new Font("Monospaced", Font.PLAIN, Integer.parseInt(font))));
			mnFont.add(mntmFont);
		}

		final JMenu mnHelp = new JMenu("Help");
		mnHelp.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mnHelp.setForeground(Color.WHITE);
		menuBar.add(mnHelp);

		final JMenuItem mntmAbout = new JMenuItem("About");
		mntmAbout.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmAbout.addActionListener(_ -> new frmAbout());
		mnHelp.add(mntmAbout);

		// Set icons.
		final List<Image> icons = new ArrayList<>();
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_16x16.png").getImage());
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_32x32.png").getImage());
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_64x64.png").getImage());
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_128x128.png").getImage());

		final JScrollPane scrollPanel = new JScrollPane(_txtrConsole);
		scrollPanel.setBounds(0, 0, 800, 550);
		scrollPanel.getViewport().setBackground(INPUT_BG);

		// Set frame.
		final JFrame frame = new JFrame("Mobius - LoginServer");
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.getContentPane().setBackground(BG_COLOR);
		frame.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent ev)
			{
				if (JOptionPane.showOptionDialog(null, "Shutdown LoginServer?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, SHUTDOWN_OPTIONS, SHUTDOWN_OPTIONS[1]) == 0)
				{
					LoginServer.getInstance().shutdown(false);
				}
			}
		});
		frame.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent ev)
			{
				scrollPanel.setSize(frame.getContentPane().getSize());
			}
		});
		frame.setJMenuBar(menuBar);
		frame.setIconImages(icons);
		frame.add(scrollPanel, BorderLayout.CENTER);
		frame.getContentPane().setPreferredSize(new Dimension(InterfaceConfig.DARK_THEME ? 815 : 800, 550));
		frame.pack();
		frame.setLocationRelativeTo(null);

		// Redirect output to text area.
		redirectSystemStreams();

		// Show SplashScreen.
		new SplashScreen(".." + File.separator + "images" + File.separator + "splash.png", 5000, frame);
	}

	private void applyDarkTheme()
	{
		try
		{
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");


			// Nimbus 基础颜色
			UIManager.put("control", PANEL_BG);
			UIManager.put("nimbusBase", PANEL_BG);
			UIManager.put("nimbusBlueGrey", PANEL_BG);
			UIManager.put("nimbusLightBackground", PANEL_BG);
			UIManager.put("nimbusSelectionBackground", new Color(70, 130, 180));
			UIManager.put("text", FG_COLOR);
			UIManager.put("textForeground", FG_COLOR);
			UIManager.put("textBackground", PANEL_BG);
			UIManager.put("info", PANEL_BG);
			UIManager.put("nimbusInfoBlue", PANEL_BG);
			UIManager.put("nimbusFocus", new Color(70, 130, 180));

			// 面板
			UIManager.put("Panel.background", PANEL_BG);
			UIManager.put("Panel[Enabled].background", PANEL_BG);
			UIManager.put("Panel.opaque", true);

			// 文本框
			UIManager.put("TextField.background", INPUT_BG);
			UIManager.put("TextField.foreground", FG_COLOR);
			UIManager.put("TextField.caretForeground", FG_COLOR);
			UIManager.put("TextArea.background", INPUT_BG);
			UIManager.put("TextArea.foreground", FG_COLOR);

			// 菜单栏
			UIManager.put("MenuBar.background", new Color(190, 190, 190));
			UIManager.put("MenuBar.foreground", FG_COLOR);
			UIManager.put("Menu.background", PANEL_BG);
			UIManager.put("Menu.foreground", FG_COLOR);
			UIManager.put("Menu[Enabled].background", PANEL_BG);
			UIManager.put("Menu[Enabled].foreground", FG_COLOR);
			UIManager.put("MenuItem.background", PANEL_BG);
			UIManager.put("MenuItem.foreground", FG_COLOR);
			UIManager.put("MenuItem[MouseOver].background", new Color(70, 70, 70));
			UIManager.put("MenuItem.selectionBackground", new Color(70, 70, 70));
			UIManager.put("MenuItem.selectionForeground", FG_COLOR);
			UIManager.put("CheckBoxMenuItem.background", PANEL_BG);
			UIManager.put("CheckBoxMenuItem.foreground", FG_COLOR);
			UIManager.put("CheckBoxMenuItem.selectionBackground", new Color(70, 70, 70));
			UIManager.put("CheckBoxMenuItem.selectionForeground", FG_COLOR);
			UIManager.put("Menu:MenuItemAccelerator[MouseOver].textForeground", FG_COLOR);

			// 对话框
			UIManager.put("OptionPane.background", PANEL_BG);
			UIManager.put("OptionPane.messageBackground", PANEL_BG);
			UIManager.put("OptionPane.messageForeground", FG_COLOR);
			UIManager.put("OptionPane.contentAreaColor", PANEL_BG);
			UIManager.put("OptionPane.buttonAreaColor", PANEL_BG);
			UIManager.put("OptionPane[Enabled].background", PANEL_BG);
			UIManager.put("OptionPane.questionDialog.titlePane.background", PANEL_BG);
			UIManager.put("OptionPane.errorDialog.titlePane.background", PANEL_BG);
			UIManager.put("OptionPane.warningDialog.titlePane.background", PANEL_BG);
			UIManager.put("OptionPane.questionDialog.titlePane.shadow", PANEL_BG);
			UIManager.put("OptionPane.errorDialog.titlePane.shadow", PANEL_BG);
			UIManager.put("OptionPane.warningDialog.titlePane.shadow", PANEL_BG);
			UIManager.put("OptionPane.questionDialog.border.background", PANEL_BG);
			UIManager.put("OptionPane.errorDialog.border.background", PANEL_BG);
			UIManager.put("OptionPane.warningDialog.border.background", PANEL_BG);

			// 按钮
			UIManager.put("Button.background", new Color(70, 70, 70));
			UIManager.put("Button.foreground", FG_COLOR);
			UIManager.put("Button.select", new Color(90, 90, 90));
			UIManager.put("Button.focus", new Color(100, 100, 100));
			UIManager.put("Button[Default].background", new Color(70, 70, 70));
			UIManager.put("Button[Default+Focused].background", new Color(90, 90, 90));
			UIManager.put("Button[Enabled].background", new Color(70, 70, 70));

			// 标签
			UIManager.put("Label.background", PANEL_BG);
			UIManager.put("Label.foreground", FG_COLOR);
			UIManager.put("Label.disabledForeground", new Color(150, 150, 150));
			UIManager.put("Label[Enabled].background", PANEL_BG);
			UIManager.put("Label[Enabled].foreground", FG_COLOR);

			// 组合框
			UIManager.put("ComboBox.background", INPUT_BG);
			UIManager.put("ComboBox.foreground", FG_COLOR);
			UIManager.put("ComboBox.selectionBackground", new Color(70, 130, 180));
			UIManager.put("ComboBox.selectionForeground", Color.WHITE);

			// 列表
			UIManager.put("List.background", INPUT_BG);
			UIManager.put("List.foreground", FG_COLOR);

			// 滚动条
			UIManager.put("ScrollBar.background", PANEL_BG);
			UIManager.put("ScrollBar.thumb", new Color(100, 100, 100));
			UIManager.put("ScrollBar.track", INPUT_BG);

			// 工具提示
			UIManager.put("ToolTip.background", INPUT_BG);
			UIManager.put("ToolTip.foreground", FG_COLOR);

			// 分隔符
			UIManager.put("Separator.background", PANEL_BG);
			UIManager.put("Separator.foreground", new Color(100, 100, 100));
		}
		catch (Exception e)
		{
			System.err.println("无法设置深色主题: " + e.getMessage());
		}
	}

	// Set where the text is redirected. In this case, txtrConsole.
	void updateTextArea(String text)
	{
		SwingUtilities.invokeLater(() ->
		{
			_txtrConsole.append(text);
			_txtrConsole.setCaretPosition(_txtrConsole.getText().length());
		});
	}

	// Method that manages the redirect.
	private void redirectSystemStreams()
	{
		final OutputStream out = new OutputStream()
		{
			@Override
			public void write(int b)
			{
				updateTextArea(String.valueOf((char) b));
			}

			@Override
			public void write(byte[] b, int off, int len)
			{
				updateTextArea(new String(b, off, len));
			}

			@Override
			public void write(byte[] b)
			{
				write(b, 0, b.length);
			}
		};

		System.setOut(new PrintStream(out, true));
		System.setErr(new PrintStream(out, true));
	}
}