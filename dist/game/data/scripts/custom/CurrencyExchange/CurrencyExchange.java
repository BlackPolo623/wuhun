package custom.CurrencyExchange;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * 貨幣兌換系統
 * @author 黑普羅
 */
public class CurrencyExchange extends Script
{
	// NPC ID
	private static final int EXCHANGE_NPC = 900032;

	// HTML路徑
	private static final String HTML_PATH = "data/scripts/custom/CurrencyExchange/";

	// 貨幣 ID
	private static final int ADENA = 57;        // 金幣
	private static final int L_COIN = 91663;    // L Coin

	// 兌換比例
	private static final long ADENA_TO_LCOIN = 50000;  // 50000 金幣 = 1 L Coin
	private static final long LCOIN_TO_ADENA = 50000;  // 1 L Coin = 50000 金幣

	private CurrencyExchange()
	{
		addStartNpc(EXCHANGE_NPC);
		addTalkId(EXCHANGE_NPC);
		addFirstTalkId(EXCHANGE_NPC);
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return getMainPage(player);
	}

	/**
	 * 獲取主頁面
	 */
	private String getMainPage(Player player)
	{
		long adenaCount = player.getInventory().getInventoryItemCount(ADENA, -1);
		long lcoinCount = player.getInventory().getInventoryItemCount(L_COIN, -1);
		long maxLcoin = adenaCount / ADENA_TO_LCOIN;
		long maxAdena = lcoinCount * LCOIN_TO_ADENA;

		Map<String, String> replacements = new HashMap<>();
		replacements.put("adena_count", formatNumber(adenaCount));
		replacements.put("lcoin_count", formatNumber(lcoinCount));
		replacements.put("max_lcoin", formatNumber(maxLcoin));
		replacements.put("max_adena", formatNumber(maxAdena));

		return showHtmlFile(player, "main.htm", replacements);
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;

		// DEBUG: 記錄收到的 event
		player.sendMessage("[DEBUG] 收到事件: " + event);

		if (event.equals("exchange_adena_to_lcoin"))
		{
			// 顯示金幣換 L Coin 的輸入頁面
			htmltext = getExchangeAdenaPage(player);
		}
		else if (event.equals("exchange_lcoin_to_adena"))
		{
			// 顯示 L Coin 換金幣的輸入頁面
			htmltext = getExchangeLcoinPage(player);
		}
		else if (event.startsWith("convert_adena "))
		{
			// 執行金幣換 L Coin
			// 格式: "convert_adena 100"
			String amountStr = event.substring("convert_adena ".length()).trim();

			player.sendMessage("[DEBUG] 解析的數量字串: '" + amountStr + "'");

			// 檢查是否為空
			if (amountStr.isEmpty())
			{
				htmltext = showHtmlFile(player, "error.htm", Map.of("message", "請輸入要兌換的數量"));
				return htmltext;
			}

			try
			{
				long amount = Long.parseLong(amountStr);
				htmltext = convertAdenaToLcoin(player, amount);
			}
			catch (NumberFormatException e)
			{
				htmltext = showHtmlFile(player, "error.htm", Map.of("message", "輸入格式錯誤，請輸入正整數<br>您輸入的是: [" + amountStr + "]"));
			}
		}
		else if (event.startsWith("convert_lcoin "))
		{
			// 執行 L Coin 換金幣
			// 格式: "convert_lcoin 100"
			String amountStr = event.substring("convert_lcoin ".length()).trim();

			player.sendMessage("[DEBUG] 解析的數量字串: '" + amountStr + "'");

			// 檢查是否為空
			if (amountStr.isEmpty())
			{
				htmltext = showHtmlFile(player, "error.htm", Map.of("message", "請輸入要兌換的數量"));
				return htmltext;
			}

			try
			{
				long amount = Long.parseLong(amountStr);
				htmltext = convertLcoinToAdena(player, amount);
			}
			catch (NumberFormatException e)
			{
				htmltext = showHtmlFile(player, "error.htm", Map.of("message", "輸入格式錯誤，請輸入正整數<br>您輸入的是: [" + amountStr + "]"));
			}
		}
		else if (event.equals("back"))
		{
			htmltext = getMainPage(player);
		}

		return htmltext;
	}

	/**
	 * 獲取金幣換 L Coin 的頁面
	 */
	private String getExchangeAdenaPage(Player player)
	{
		long adenaCount = player.getInventory().getInventoryItemCount(ADENA, -1);
		long maxLcoin = adenaCount / ADENA_TO_LCOIN;

		Map<String, String> replacements = new HashMap<>();
		replacements.put("adena_count", formatNumber(adenaCount));
		replacements.put("max_lcoin", formatNumber(maxLcoin));

		return showHtmlFile(player, "exchange_adena.htm", replacements);
	}

	/**
	 * 獲取 L Coin 換金幣的頁面
	 */
	private String getExchangeLcoinPage(Player player)
	{
		long lcoinCount = player.getInventory().getInventoryItemCount(L_COIN, -1);
		long maxAdena = lcoinCount * LCOIN_TO_ADENA;

		Map<String, String> replacements = new HashMap<>();
		replacements.put("lcoin_count", formatNumber(lcoinCount));
		replacements.put("max_adena", formatNumber(maxAdena));

		return showHtmlFile(player, "exchange_lcoin.htm", replacements);
	}

	/**
	 * 執行金幣換 L Coin
	 */
	private String convertAdenaToLcoin(Player player, long lcoinAmount)
	{
		// 檢查輸入
		if (lcoinAmount <= 0)
		{
			return showHtmlFile(player, "error.htm", Map.of("message", "兌換數量必須大於 0"));
		}

		// 計算需要的金幣
		long adenaNeeded = lcoinAmount * ADENA_TO_LCOIN;
		long adenaCount = player.getInventory().getInventoryItemCount(ADENA, -1);

		// 檢查是否有足夠的金幣
		if (adenaCount < adenaNeeded)
		{
			return showHtmlFile(player, "error.htm", Map.of("message",
				"金幣不足！<br>需要 " + formatNumber(adenaNeeded) + " 金幣<br>目前擁有 " + formatNumber(adenaCount) + " 金幣"));
		}

		// 扣除金幣
		if (!player.destroyItemByItemId(ItemProcessType.NONE, ADENA, adenaNeeded, player, true))
		{
			return showHtmlFile(player, "error.htm", Map.of("message", "扣除金幣失敗，請重試"));
		}

		// 給予 L Coin
		player.addItem(ItemProcessType.NONE, L_COIN, lcoinAmount, player, true);

		// 顯示成功頁面
		Map<String, String> replacements = new HashMap<>();
		replacements.put("title", "兌換成功！");
		replacements.put("line1", "支付 " + formatNumber(adenaNeeded) + " 金幣");
		replacements.put("line2", "獲得 " + formatNumber(lcoinAmount) + " L Coin");

		return showHtmlFile(player, "success.htm", replacements);
	}

	/**
	 * 執行 L Coin 換金幣
	 */
	private String convertLcoinToAdena(Player player, long lcoinAmount)
	{
		// 檢查輸入
		if (lcoinAmount <= 0)
		{
			return showHtmlFile(player, "error.htm", Map.of("message", "兌換數量必須大於 0"));
		}

		// 計算可獲得的金幣
		long adenaToGet = lcoinAmount * LCOIN_TO_ADENA;
		long lcoinCount = player.getInventory().getInventoryItemCount(L_COIN, -1);

		// 檢查是否有足夠的 L Coin
		if (lcoinCount < lcoinAmount)
		{
			return showHtmlFile(player, "error.htm", Map.of("message",
				"L Coin 不足！<br>需要 " + formatNumber(lcoinAmount) + " L Coin<br>目前擁有 " + formatNumber(lcoinCount) + " L Coin"));
		}

		// 扣除 L Coin
		if (!player.destroyItemByItemId(ItemProcessType.NONE, L_COIN, lcoinAmount, player, true))
		{
			return showHtmlFile(player, "error.htm", Map.of("message", "扣除 L Coin 失敗，請重試"));
		}

		// 給予金幣
		player.addItem(ItemProcessType.NONE, ADENA, adenaToGet, player, true);

		// 顯示成功頁面
		Map<String, String> replacements = new HashMap<>();
		replacements.put("title", "兌換成功！");
		replacements.put("line1", "支付 " + formatNumber(lcoinAmount) + " L Coin");
		replacements.put("line2", "獲得 " + formatNumber(adenaToGet) + " 金幣");

		return showHtmlFile(player, "success.htm", replacements);
	}

	/**
	 * 顯示HTML檔案並替換變數
	 */
	private String showHtmlFile(Player player, String fileName, Map<String, String> replacements)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(player, HTML_PATH + fileName);

		if (replacements != null)
		{
			for (Map.Entry<String, String> entry : replacements.entrySet())
			{
				html.replace("%" + entry.getKey() + "%", entry.getValue());
			}
		}

		player.sendPacket(html);
		return null;
	}

	/**
	 * 格式化數字 (加入千分位逗號)
	 */
	private String formatNumber(long number)
	{
		return String.format("%,d", number);
	}

	public static void main(String[] args)
	{
		new CurrencyExchange();
	}
}
