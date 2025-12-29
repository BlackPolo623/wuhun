package org.l2jmobius.gameserver.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;  // ✅ 使用 javax.swing.Timer
import javax.swing.border.LineBorder;

import org.l2jmobius.gameserver.GameServer;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.util.Locator;

public class SystemPanel extends JPanel
{
	protected static final Logger LOGGER = Logger.getLogger(SystemPanel.class.getName());
	protected static final long START_TIME = System.currentTimeMillis();

	private static final Color PANEL_BG = new Color(45, 45, 45);
	private static final Color FG_COLOR = new Color(240, 240, 240);
	private static final Color BORDER_COLOR = new Color(100, 100, 100);
	private static final Color LABEL_COLOR = new Color(200, 200, 200);

	private Timer updateTimer;  // ✅ 保存引用

	public SystemPanel()
	{
		setBackground(PANEL_BG);
		setBounds(500, 20, 284, 140);
		setBorder(new LineBorder(BORDER_COLOR, 1, false));
		setOpaque(true);
		setLayout(null);

		final JLabel lblProtocol = new JLabel("Protocol");
		lblProtocol.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblProtocol.setForeground(LABEL_COLOR);
		lblProtocol.setBounds(10, 5, 264, 17);
		add(lblProtocol);

		final JLabel lblConnected = new JLabel("Connected");
		lblConnected.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblConnected.setForeground(LABEL_COLOR);
		lblConnected.setBounds(10, 23, 264, 17);
		add(lblConnected);

		final JLabel lblMaxConnected = new JLabel("Max connected");
		lblMaxConnected.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblMaxConnected.setForeground(LABEL_COLOR);
		lblMaxConnected.setBounds(10, 41, 264, 17);
		add(lblMaxConnected);

		final JLabel lblOfflineShops = new JLabel("Offline trade");
		lblOfflineShops.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblOfflineShops.setForeground(LABEL_COLOR);
		lblOfflineShops.setBounds(10, 59, 264, 17);
		add(lblOfflineShops);

		final JLabel lblElapsedTime = new JLabel("Elapsed time");
		lblElapsedTime.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblElapsedTime.setForeground(LABEL_COLOR);
		lblElapsedTime.setBounds(10, 77, 264, 17);
		add(lblElapsedTime);

		final JLabel lblJavaVersion = new JLabel("Build JDK");
		lblJavaVersion.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblJavaVersion.setForeground(LABEL_COLOR);
		lblJavaVersion.setBounds(10, 95, 264, 17);
		add(lblJavaVersion);

		final JLabel lblBuildDate = new JLabel("Build date");
		lblBuildDate.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblBuildDate.setForeground(LABEL_COLOR);
		lblBuildDate.setBounds(10, 113, 264, 17);
		add(lblBuildDate);

		// Set initial values
		lblProtocol.setText("Protocol: 0");
		lblConnected.setText("Connected: 0");
		lblMaxConnected.setText("Max connected: 0");
		lblOfflineShops.setText("Offline trade: 0");
		lblElapsedTime.setText("Elapsed: 0 sec");
		lblJavaVersion.setText("Java version: " + System.getProperty("java.version"));
		lblBuildDate.setText("Build date: Unavailable");

		try
		{
			final File jarName = Locator.getClassSource(GameServer.class);
			final JarFile jarFile = new JarFile(jarName);
			final Attributes attrs = jarFile.getManifest().getMainAttributes();
			lblBuildDate.setText("Build date: " + attrs.getValue("Build-Date").split(" ")[0]);
			jarFile.close();
		}
		catch (Exception e)
		{
			// Handled above
		}

		// ✅ 初始化 Protocol 標籤（延遲 4.5 秒）
		Timer protocolTimer = new Timer(4500, new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				lblProtocol.setText((ServerConfig.PROTOCOL_LIST.size() > 1 ? "Protocols: " : "Protocol: ") +
						((ServerConfig.SERVER_LIST_TYPE & 0x2000) == 0x2000 ? "Eva " :
								(ServerConfig.SERVER_LIST_TYPE & 0x1000) == 0x1000 ? "Essence " :
										(ServerConfig.SERVER_LIST_TYPE & 0x400) == 0x400 ? "Classic " : "") +
						ServerConfig.PROTOCOL_LIST.toString());
			}
		});
		protocolTimer.setRepeats(false);  // ✅ 只執行一次
		protocolTimer.start();

		// ✅ 定期更新計時器
		updateTimer = new Timer(1000, new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				final int playerCount = World.getInstance().getPlayers().size();
				if (World.MAX_CONNECTED_COUNT < playerCount)
				{
					World.MAX_CONNECTED_COUNT = playerCount;
					if (playerCount > 1)
					{
						LOGGER.info("New maximum connected count of " + playerCount + "!");
					}
				}

				lblConnected.setText("Connected: " + playerCount);
				lblMaxConnected.setText("Max connected: " + World.MAX_CONNECTED_COUNT);
				lblOfflineShops.setText("Offline trade: " + World.OFFLINE_TRADE_COUNT);
				lblElapsedTime.setText("Elapsed: " + getDurationBreakdown(System.currentTimeMillis() - START_TIME));
			}
		});
		updateTimer.start();
	}

	static String getDurationBreakdown(long millis)
	{
		long remaining = millis;
		final long days = TimeUnit.MILLISECONDS.toDays(remaining);
		remaining -= TimeUnit.DAYS.toMillis(days);
		final long hours = TimeUnit.MILLISECONDS.toHours(remaining);
		remaining -= TimeUnit.HOURS.toMillis(hours);
		final long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining);
		remaining -= TimeUnit.MINUTES.toMillis(minutes);
		final long seconds = TimeUnit.MILLISECONDS.toSeconds(remaining);
		return (days + "d " + hours + "h " + minutes + "m " + seconds + "s");
	}

	// ✅ 添加清理方法
	public void dispose()
	{
		if (updateTimer != null)
		{
			updateTimer.stop();
			updateTimer = null;
		}
	}
}