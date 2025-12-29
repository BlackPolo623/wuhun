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
package org.l2jmobius.gameserver.ui;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.*;

import org.l2jmobius.commons.config.InterfaceConfig;
import org.l2jmobius.commons.ui.DarkTheme;
import org.l2jmobius.commons.ui.LineLimitListener;
import org.l2jmobius.commons.ui.SplashScreen;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.Shutdown;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.ConfigLoader;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.data.xml.BuyListData;
import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.data.xml.PrimeShopData;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * @author Mobius
 */
public class Gui
{
	private static final Logger LOGGER = Logger.getLogger(Gui.class.getName());

	// NPC编辑器风格的深色主题配色
	private static final Color BG_COLOR = new Color(30, 30, 30);
	private static final Color FG_COLOR = new Color(240, 240, 240);
	private static final Color PANEL_BG = new Color(45, 45, 45);
	private static final Color INPUT_BG = new Color(55, 55, 55);
	private static final Color BORDER_COLOR = new Color(100, 100, 100);
	private static final Color BUTTON_BG = new Color(70, 70, 70);
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
	private static final String[] ABORT_OPTIONS =
	{
		"Abort",
		"Cancel"
	};
	private static final String[] CONFIRM_OPTIONS =
	{
		"Confirm",
		"Cancel"
	};
	
	private final JTextArea _txtrConsole;
	
	public Gui()
	{
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
		
		/// Initialize menu items.
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
			if (JOptionPane.showOptionDialog(null, "Shutdown GameServer?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, SHUTDOWN_OPTIONS, SHUTDOWN_OPTIONS[1]) == 0)
			{
				final Object answer = JOptionPane.showInputDialog(null, "Shutdown delay in seconds", "Input", JOptionPane.INFORMATION_MESSAGE, null, null, "600");
				if (answer != null)
				{
					final String input = ((String) answer).trim();
					if (StringUtil.isNumeric(input))
					{
						final int delay = Integer.parseInt(input);
						if (delay > 0)
						{
							Shutdown.getInstance().startShutdown(null, delay, false);
						}
					}
				}
			}
		});
		mnActions.add(mntmShutdown);
		
		final JMenuItem mntmRestart = new JMenuItem("Restart");
		mntmRestart.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmRestart.addActionListener(_ ->
		{
			if (JOptionPane.showOptionDialog(null, "Restart GameServer?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, RESTART_OPTIONS, RESTART_OPTIONS[1]) == 0)
			{
				final Object answer = JOptionPane.showInputDialog(null, "Restart delay in seconds", "Input", JOptionPane.INFORMATION_MESSAGE, null, null, "600");
				if (answer != null)
				{
					final String input = ((String) answer).trim();
					if (StringUtil.isNumeric(input))
					{
						final int delay = Integer.parseInt(input);
						if (delay > 0)
						{
							Shutdown.getInstance().startShutdown(null, delay, true);
						}
					}
				}
			}
		});
		mnActions.add(mntmRestart);
		
		final JMenuItem mntmAbort = new JMenuItem("Abort");
		mntmAbort.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmAbort.addActionListener(_ ->
		{
			if (JOptionPane.showOptionDialog(null, "Abort server shutdown?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, ABORT_OPTIONS, ABORT_OPTIONS[1]) == 0)
			{
				Shutdown.getInstance().abort(null);
			}
		});
		mnActions.add(mntmAbort);
		
		final JMenu mnReload = new JMenu("Reload");
		mnReload.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mnReload.setForeground(Color.WHITE);
		menuBar.add(mnReload);
		
		final JMenuItem mntmConfigs = new JMenuItem("Configs");
		mntmConfigs.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmConfigs.addActionListener(_ ->
		{
			if (JOptionPane.showOptionDialog(null, "Reload configs?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, CONFIRM_OPTIONS, CONFIRM_OPTIONS[1]) == 0)
			{
				ConfigLoader.init();
			}
		});
		mnReload.add(mntmConfigs);
		
		final JMenuItem mntmAccess = new JMenuItem("Access");
		mntmAccess.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmAccess.addActionListener(_ ->
		{
			if (JOptionPane.showOptionDialog(null, "Reload admin access levels?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, CONFIRM_OPTIONS, CONFIRM_OPTIONS[1]) == 0)
			{
				AdminData.getInstance().load();
			}
		});
		mnReload.add(mntmAccess);
		
		final JMenuItem mntmHtml = new JMenuItem("HTML");
		mntmHtml.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmHtml.addActionListener(_ ->
		{
			if (JOptionPane.showOptionDialog(null, "Reload HTML files?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, CONFIRM_OPTIONS, CONFIRM_OPTIONS[1]) == 0)
			{
				HtmCache.getInstance().reload();
			}
		});
		mnReload.add(mntmHtml);
		
		final JMenuItem mntmMultisells = new JMenuItem("Multisells");
		mntmMultisells.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmMultisells.addActionListener(_ ->
		{
			if (JOptionPane.showOptionDialog(null, "Reload multisells?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, CONFIRM_OPTIONS, CONFIRM_OPTIONS[1]) == 0)
			{
				MultisellData.getInstance().load();
			}
		});
		mnReload.add(mntmMultisells);
		
		final JMenuItem mntmBuylists = new JMenuItem("Buylists");
		mntmBuylists.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmBuylists.addActionListener(_ ->
		{
			if (JOptionPane.showOptionDialog(null, "Reload buylists?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, CONFIRM_OPTIONS, CONFIRM_OPTIONS[1]) == 0)
			{
				BuyListData.getInstance().load();
			}
		});
		mnReload.add(mntmBuylists);
		
		final JMenuItem mntmPrimeShop = new JMenuItem("PrimeShop");
		mntmPrimeShop.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmPrimeShop.addActionListener(_ ->
		{
			if (JOptionPane.showOptionDialog(null, "Reload PrimeShop?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, CONFIRM_OPTIONS, CONFIRM_OPTIONS[1]) == 0)
			{
				PrimeShopData.getInstance().load();
			}
		});
		mnReload.add(mntmPrimeShop);
		
		final JMenu mnAnnounce = new JMenu("Announce");
		mnAnnounce.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mnAnnounce.setForeground(Color.WHITE);
		menuBar.add(mnAnnounce);
		
		final JMenuItem mntmNormal = new JMenuItem("Normal");
		mntmNormal.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmNormal.addActionListener(_ ->
		{
			final Object input = JOptionPane.showInputDialog(null, "Announce message", "Input", JOptionPane.INFORMATION_MESSAGE, null, null, "");
			if (input != null)
			{
				final String message = ((String) input).trim();
				if (!message.isEmpty())
				{
					Broadcast.toAllOnlinePlayers(message, false);
				}
			}
		});
		mnAnnounce.add(mntmNormal);
		
		final JMenuItem mntmCritical = new JMenuItem("Critical");
		mntmCritical.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmCritical.addActionListener(_ ->
		{
			final Object input = JOptionPane.showInputDialog(null, "Critical announce message", "Input", JOptionPane.INFORMATION_MESSAGE, null, null, "");
			if (input != null)
			{
				final String message = ((String) input).trim();
				if (!message.isEmpty())
				{
					Broadcast.toAllOnlinePlayers(message, true);
				}
			}
		});
		mnAnnounce.add(mntmCritical);
		
		final JMenu mnLogs = new JMenu("Logs");
		mnLogs.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mnLogs.setForeground(Color.WHITE);
		menuBar.add(mnLogs);
		
		final JMenuItem mntmLogs = new JMenuItem("View");
		mntmLogs.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmLogs.addActionListener(_ -> new LogPanel(false));
		mnLogs.add(mntmLogs);
		
		final JMenuItem mntmDeleteLogs = new JMenuItem("Delete");
		mntmDeleteLogs.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmDeleteLogs.addActionListener(_ -> new LogPanel(true));
		mnLogs.add(mntmDeleteLogs);
		
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

		// Set Panels.
		final JPanel systemPanel = new SystemPanel();
		final JPanel toolsPanel = new ToolsPanel();
		final JPanel infoPanel = new InfoPanel("開發人員：黑普羅、Sakura\n版本：1.0.0");
		final JScrollPane scrollPanel = new JScrollPane(_txtrConsole);
		scrollPanel.setBounds(0, 0, 800, 550);
		final JLayeredPane layeredPanel = new JLayeredPane();
		layeredPanel.add(scrollPanel, 0, 0);
		layeredPanel.add(systemPanel, 1, 0);
		layeredPanel.add(toolsPanel, 1, 0);
		layeredPanel.add(infoPanel, 1, 0);
		
		// Set frame.
		final JFrame frame = new JFrame("Mobius - GameServer");
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent ev)
			{
				if (JOptionPane.showOptionDialog(null, "Shutdown server immediately?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null, SHUTDOWN_OPTIONS, SHUTDOWN_OPTIONS[1]) == 0)
				{
					Shutdown.getInstance().startShutdown(null, 1, false);
				}
			}
		});
		frame.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent ev)
			{
				scrollPanel.setSize(frame.getContentPane().getSize());
				final int rightX = frame.getContentPane().getWidth() - systemPanel.getWidth() - 34;
				systemPanel.setLocation(rightX, systemPanel.getY());
				toolsPanel.setLocation(rightX, toolsPanel.getY());
				infoPanel.setLocation(10, 10);  // 固定在左上角
			}
		});
		frame.setJMenuBar(menuBar);
		frame.setIconImages(icons);
		frame.add(layeredPanel, BorderLayout.CENTER);
		frame.getContentPane().setPreferredSize(new Dimension(InterfaceConfig.DARK_THEME ? 815 : 800, 550));
		frame.pack();
		frame.setLocationRelativeTo(null);
		
		// Redirect output to text area.
		redirectSystemStreams();
		
		// Show SplashScreen.
		new SplashScreen(".." + File.separator + "images" + File.separator + "splash.png", 5000, frame);
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
			// 添加這些設置來修正菜單項的內邊距和對齊
			UIManager.put("MenuItem.contentMargins", new java.awt.Insets(2, 12, 2, 12));
			UIManager.put("Menu.contentMargins", new java.awt.Insets(2, 12, 2, 5));
			UIManager.put("MenuItem.margin", new java.awt.Insets(2, 0, 2, 0));
			UIManager.put("Menu.margin", new java.awt.Insets(2, 0, 2, 0));
			UIManager.put("MenuItem.background", PANEL_BG);
			UIManager.put("MenuItem.foreground", FG_COLOR);
			UIManager.put("MenuItem[MouseOver].background", new Color(70, 70, 70));
			UIManager.put("MenuItem.selectionBackground", new Color(70, 70, 70));
			UIManager.put("MenuItem.selectionForeground", FG_COLOR);
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
			UIManager.put("Button.background", BUTTON_BG);
			UIManager.put("Button.foreground", FG_COLOR);
			UIManager.put("Button.select", new Color(90, 90, 90));
			UIManager.put("Button.focus", new Color(100, 100, 100));
			UIManager.put("Button[Default].background", BUTTON_BG);
			UIManager.put("Button[Default+Focused].background", new Color(90, 90, 90));
			UIManager.put("Button[Enabled].background", BUTTON_BG);

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
}
