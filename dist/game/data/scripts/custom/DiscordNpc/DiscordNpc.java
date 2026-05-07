package custom.DiscordNpc;

import org.l2jmobius.discord.DiscordBindingManager;
import org.l2jmobius.discord.DiscordDAO;
import org.l2jmobius.discord.DiscordManager;
import org.l2jmobius.gameserver.config.custom.DiscordConfig;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Discord 綁定 NPC。
 *
 * 功能：
 *   - 未綁定：顯示說明，提供「生成驗證碼」按鈕
 *   - 已綁定：顯示目前綁定狀態，提供「解除綁定」按鈕
 *
 * NPC_ID 請對應您資料庫中的 NPC 編號（預設 900100）。
 *
 * @author Custom
 */
public class DiscordNpc extends Script
{
	/** 對應資料庫的 NPC ID，請依實際情況修改 */
	private static final int NPC_ID = 900053;

	private static final String HTML_PATH = "data/scripts/custom/DiscordNpc/";

	private DiscordNpc()
	{
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);
	}

	// ── 進入對話 ──────────────────────────────────────────────────────────────

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		showMainPage(player);
		return null;
	}

	// ── 按鈕事件 ──────────────────────────────────────────────────────────────

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "generate_code":
			{
				showCodePage(player);
				break;
			}
			case "unbind_confirm":
			{
				showPage(player, "unbind_confirm.htm");
				break;
			}
			case "unbind_do":
			{
				doUnbind(player);
				break;
			}
			case "back":
			{
				showMainPage(player);
				break;
			}
		}
		return null;
	}

	// ── 頁面邏輯 ──────────────────────────────────────────────────────────────

	private void showMainPage(Player player)
	{
		// Bot 未啟用
		if (!DiscordManager.getInstance().isRunning())
		{
			showPage(player, "disabled.htm");
			return;
		}

		final String discordId = DiscordDAO.getDiscordId(player.getObjectId());

		if (discordId == null)
		{
			// 未綁定 → 顯示引導頁面
			showPage(player, "main_unbound.htm");
		}
		else
		{
			// 已綁定 → 顯示狀態並提供解除選項
			final String masked = "****" + discordId.substring(Math.max(0, discordId.length() - 4));
			final NpcHtmlMessage html = new NpcHtmlMessage();
			html.setFile(player, HTML_PATH + "main_bound.htm");
			html.replace("%discord_id%", masked);
			player.sendPacket(html);
		}
	}

	private void showCodePage(Player player)
	{
		// 已有綁定，不需要再生成
		if (DiscordDAO.getDiscordId(player.getObjectId()) != null)
		{
			showMainPage(player);
			return;
		}

		final String code = DiscordBindingManager.generateCode(player);
		if (code == null)
		{
			// generateCode 回傳 null 代表已綁定（二次確認）
			showMainPage(player);
			return;
		}

		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(player, HTML_PATH + "code.htm");
		html.replace("%code%", code);
		html.replace("%expire%", String.valueOf(DiscordConfig.DISCORD_BIND_CODE_EXPIRE_MINUTES));
		player.sendPacket(html);
	}

	private void doUnbind(Player player)
	{
		final boolean success = DiscordBindingManager.unbind(player.getObjectId());
		if (success)
		{
			showPage(player, "unbind_success.htm");
		}
		else
		{
			showPage(player, "unbind_fail.htm");
		}
	}

	// ── 工具 ─────────────────────────────────────────────────────────────────

	private void showPage(Player player, String file)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(player, HTML_PATH + file);
		player.sendPacket(html);
	}

	// ── 註冊 ─────────────────────────────────────────────────────────────────

	public static void main(String[] args)
	{
		new DiscordNpc();
	}
}
