/*
 * 武魂天堂2 - 網頁商城系統
 * Web Shop System for Wuhun Lineage 2
 */
package wuhun.web.shop;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.model.Message;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Mail;
import org.l2jmobius.gameserver.network.enums.MailType;

/**
 * 網頁商城郵件管理器
 * 負責讀取網頁商城購買記錄並發送遊戲內郵件
 * @author Wuhun
 */
public class WebShopMailManager
{
	private static final Logger LOGGER = Logger.getLogger(WebShopMailManager.class.getName());

	// SQL Statements
	private static final String SELECT_PENDING_MAILS = "SELECT * FROM web_shop_mail_queue WHERE status = 'pending' ORDER BY created_at ASC LIMIT 50";
	private static final String UPDATE_MAIL_STATUS = "UPDATE web_shop_mail_queue SET status = ?, processed_at = NOW(), error_message = ? WHERE id = ?";

	protected WebShopMailManager()
	{
		// 載入設定
		WebShopMailConfig.load();

		if (!WebShopMailConfig.ENABLED)
		{
			LOGGER.info(getClass().getSimpleName() + ": Disabled by config.");
			return;
		}

		// 啟動定時任務
		ThreadPool.scheduleAtFixedRate(this::processMailQueue, WebShopMailConfig.CHECK_DELAY, WebShopMailConfig.CHECK_DELAY);
		LOGGER.info(getClass().getSimpleName() + ": Enabled. Check delay: " + (WebShopMailConfig.CHECK_DELAY / 1000) + " seconds.");
	}

	/**
	 * 處理郵件佇列
	 */
	private void processMailQueue()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_PENDING_MAILS);
			ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
			{
				final int mailId = rs.getInt("id");
				final int charId = rs.getInt("char_id");
				final String subject = rs.getString("subject");
				final String content = rs.getString("content");
				final int itemId = rs.getInt("item_id");
				final int itemCount = rs.getInt("item_count");
				final int enchantLevel = rs.getInt("enchant_level");

				try
				{
					// 發送郵件（不管玩家是否在線都發送，MailManager 會處理）
					sendMailToPlayer(charId, subject, content, itemId, itemCount, enchantLevel);

					// 更新狀態為已發送
					updateMailStatus(con, mailId, "sent", null);

					// 如果玩家在線，記錄名字
					final Player player = World.getInstance().getPlayer(charId);
					if (player != null)
					{
						LOGGER.info(getClass().getSimpleName() + ": Mail sent to " + player.getName() + " (ID: " + mailId + ")");
					}
					else
					{
						LOGGER.info(getClass().getSimpleName() + ": Mail sent to offline player charId=" + charId + " (ID: " + mailId + ")");
					}
				}
				catch (Exception e)
				{
					// 更新狀態為失敗
					updateMailStatus(con, mailId, "failed", e.getMessage());
					LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed to send mail ID " + mailId + ": ", e);
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error processing mail queue: ", e);
		}
	}

	/**
	 * 發送郵件給玩家
	 * @param charId 角色ID
	 * @param subject 郵件主題
	 * @param content 郵件內容
	 * @param itemId 物品ID
	 * @param itemCount 物品數量
	 * @param enchantLevel 強化等級
	 */
	private void sendMailToPlayer(int charId, String subject, String content, int itemId, int itemCount, int enchantLevel)
	{
		// 創建系統郵件 (使用 PRIME_SHOP_GIFT 類型，這樣玩家不需要支付郵費)
		final Message msg = new Message(charId, subject, content, MailType.PRIME_SHOP_GIFT);

		// 如果有物品附件
		if (itemId > 0 && itemCount > 0)
		{
			final Mail attachments = msg.createAttachments();
			if (attachments != null)
			{
				final Item item = attachments.addItem(ItemProcessType.REWARD, itemId, itemCount, null, null);
				if (item != null && enchantLevel > 0)
				{
					item.setEnchantLevel(enchantLevel);
				}
			}
		}

		// 通過 MailManager 發送郵件
		MailManager.getInstance().sendMessage(msg);
	}

	/**
	 * 更新郵件狀態
	 * @param con 資料庫連接
	 * @param mailId 郵件ID
	 * @param status 狀態
	 * @param errorMessage 錯誤訊息
	 */
	private void updateMailStatus(Connection con, int mailId, String status, String errorMessage)
	{
		try (PreparedStatement ps = con.prepareStatement(UPDATE_MAIL_STATUS))
		{
			ps.setString(1, status);
			ps.setString(2, errorMessage != null ? errorMessage.substring(0, Math.min(errorMessage.length(), 500)) : null);
			ps.setInt(3, mailId);
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error updating mail status: ", e);
		}
	}

	/**
	 * 取得實例
	 * @return WebShopMailManager 實例
	 */
	public static WebShopMailManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final WebShopMailManager INSTANCE = new WebShopMailManager();
	}
}
