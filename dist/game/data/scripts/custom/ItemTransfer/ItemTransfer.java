package custom.ItemTransfer;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerFreight;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.PackageToList;
import org.l2jmobius.gameserver.network.serverpackets.WareHouseWithdrawalList;

/**
 * 物品轉移NPC - 帳號內角色物品轉移系統
 * @author 黑普羅
 */
public class ItemTransfer extends Script
{
	// NPC ID (請根據你的需求修改)
	private static final int NPC_ID = 900019;
	
	// HTML 路徑
	private static final String HTML_PATH = "data/scripts/custom/ItemTransfer/";
	
	public ItemTransfer()
	{
		addStartNpc(NPC_ID);
		addFirstTalkId(NPC_ID);
		addTalkId(NPC_ID);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ((event == null) || (player == null))
		{
			return null;
		}
		
		switch (event)
		{
			case "main":
			{
				showMainPage(player, npc);
				break;
			}
			case "package_deposit":
			{
				handleDeposit(player);
				break;
			}
			case "package_withdraw":
			{
				handleWithdraw(player);
				break;
			}
		}
		
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		showMainPage(player, npc);
		return null;
	}
	
	/**
	 * 顯示主頁面
	 */
	private void showMainPage(Player player, Npc npc)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, HTML_PATH + "main.htm");
		player.sendPacket(html);
	}
	
	/**
	 * 處理物品轉移（存入）
	 */
	private void handleDeposit(Player player)
	{
		// 檢查帳號是否有其他角色
		if (player.getAccountChars().size() < 1)
		{
			player.sendPacket(SystemMessageId.THAT_CHARACTER_DOES_NOT_EXIST);
			return;
		}
		
		// 打開角色列表選擇視窗
		player.sendPacket(new PackageToList(player.getAccountChars()));
	}
	
	/**
	 * 處理物品領取（取出）
	 */
	private void handleWithdraw(Player player)
	{
		final PlayerFreight freight = player.getFreight();
		
		// 檢查是否有物品
		if ((freight == null) || (freight.getSize() <= 0))
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_NOT_DEPOSITED_ANY_ITEMS_IN_YOUR_WAREHOUSE);
			return;
		}
		
		// 設置活動倉庫
		player.setActiveWarehouse(freight);
		
		// 清理過期限時物品
		for (Item item : player.getActiveWarehouse().getItems())
		{
			if (item.isTimeLimitedItem() && (item.getRemainingTime() <= 0))
			{
				player.getActiveWarehouse().destroyItem(ItemProcessType.DESTROY, item, player, null);
			}
		}
		
		// 發送倉庫視窗
		player.sendPacket(new WareHouseWithdrawalList(1, player, WareHouseWithdrawalList.FREIGHT));
		player.sendPacket(new WareHouseWithdrawalList(2, player, WareHouseWithdrawalList.FREIGHT));
	}
	
	public static void main(String[] args)
	{
		new ItemTransfer();
		System.out.println("【系統】物品轉移NPC載入完畢！");
	}
}