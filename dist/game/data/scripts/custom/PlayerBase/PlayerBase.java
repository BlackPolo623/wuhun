package custom.PlayerBase;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class PlayerBase extends Script
{
	// NPC
	private static final int BASE_MANAGER_NPC = 900025;

	// 基地模板ID
	private static final int BASE_TEMPLATE_ID = 900;

	// 基地進入座標
	private static final Location BASE_ENTER_LOC = new Location(58232, -90853, -1385);

	// 離開座標
	private static final Location EXIT_LOC = new Location(148694, 214058, -2063);

	// 創建基地消耗
	private static final int CREATE_COST_ITEM = 57;
	private static final long CREATE_COST_COUNT = 100000000;

	// 實例ID分配器
	private static int nextInstanceId = 7000;
	private static final Map<Integer, Integer> PLAYER_BASE_INSTANCES = new ConcurrentHashMap<>();

	public PlayerBase()
	{
		super();
		addStartNpc(BASE_MANAGER_NPC);
		addTalkId(BASE_MANAGER_NPC);
		addFirstTalkId(BASE_MANAGER_NPC);
		addInstanceLeaveId(BASE_TEMPLATE_ID);
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "create_base":
			{
				player.sendMessage("禁止自行創建基地!");
				return showMainPage(player);
			}
			case "enter_my_base":  // 改名:明確表示進入自己的基地
			{
				return handleEnterBase(player, player.getObjectId());
			}
			case "enter_other_base":  // 新增:顯示可訪問的基地列表
			{
				return showVisitableBasesList(player);
			}
			case "leave_base":
			{
				return handleLeaveBase(player);
			}
		}

		// 處理進入指定玩家的基地
		if (event.startsWith("visit_base "))
		{
			int ownerId = Integer.parseInt(event.substring(11));
			return handleEnterBase(player, ownerId);
		}

		return null;
	}

	private String handleCreateBase(Player player)
	{
		if (PlayerBaseDAO.hasBase(player.getObjectId()))
		{
			player.sendMessage("您已經擁有基地了!");
			return null;
		}

		if (player.getInventory().getInventoryItemCount(CREATE_COST_ITEM, 0) < CREATE_COST_COUNT)
		{
			player.sendPacket(new ExShowScreenMessage("金幣不足!需要 " + CREATE_COST_COUNT + " 金幣", 3000));
			return null;
		}

		takeItems(player, CREATE_COST_ITEM, CREATE_COST_COUNT);

		int instanceId = nextInstanceId++;

		if (PlayerBaseDAO.createBase(player.getObjectId(), player.getName(), instanceId, BASE_TEMPLATE_ID))
		{
			PLAYER_BASE_INSTANCES.put(player.getObjectId(), instanceId);
			player.sendMessage("========================================");
			player.sendMessage("恭喜!基地創建成功!");
			player.sendMessage("您現在可以進入您的專屬基地了");
			player.sendMessage("========================================");
			return handleEnterBase(player, player.getObjectId());
		}

		player.sendMessage("基地創建失敗,請聯繫管理員");
		return null;
	}

	private String handleEnterBase(Player player, int ownerId)
	{
		Map<String, Object> baseInfo = PlayerBaseDAO.getBaseInfo(ownerId);

		if (baseInfo.isEmpty())
		{
			player.sendMessage("基地不存在!");
			return null;
		}

		// 檢查權限
		if (player.getObjectId() != ownerId && !PlayerBaseDAO.canVisit(ownerId, player.getObjectId()))
		{
			player.sendMessage("您沒有訪問此基地的權限!");
			return null;
		}

		int instanceId = (int) baseInfo.get("instance_id");
		int templateId = (int) baseInfo.get("template_id");

		Instance instance = InstanceManager.getInstance().getInstance(instanceId);

		if (instance == null)
		{
			InstanceManager.getInstance().createInstanceFromTemplate(instanceId, templateId);
			instance = InstanceManager.getInstance().createInstance(templateId, instanceId, player);
		}

		player.setInstance(instance);
		player.teleToLocation(BASE_ENTER_LOC, instance);

		player.sendMessage("========================================");
		player.sendMessage("歡迎來到" + (player.getObjectId() == ownerId ? "您的" : baseInfo.get("player_name") + "的") + "基地");
		player.sendMessage("========================================");

		return null;
	}

	/**
	 * 顯示可訪問的基地列表
	 */
	private String showVisitableBasesList(Player player)
	{
		// 獲取有訪問權限的基地列表
		List<Map<String, Object>> visitableBases = PlayerBaseDAO.getVisitableBases(player.getObjectId());

		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/PlayerBase/visitable_bases.htm");

		StringBuilder list = new StringBuilder();

		if (visitableBases.isEmpty())
		{
			list.append("<tr><td align=center height=60><font color=\"808080\">您目前沒有可訪問的基地</font></td></tr>");
			list.append("<tr><td align=center><font color=\"666666\">請聯繫基地主人添加您為訪客</font></td></tr>");
		}
		else
		{
			for (Map<String, Object> base : visitableBases)
			{
				int ownerId = (int) base.get("player_id");
				String ownerName = (String) base.get("player_name");

				// 檢查基地主人是否在線
				Player owner = World.getInstance().getPlayer(ownerId);
				String onlineStatus = owner != null ? "<font color=\"00FF00\">在線</font>" : "<font color=\"808080\">離線</font>";

				list.append("<tr bgcolor=\"222222\">");
				list.append("<td width=120 align=left><font color=\"FFCC33\">").append(ownerName).append("</font></td>");
				list.append("<td width=60 align=center>").append(onlineStatus).append("</td>");
				list.append("<td width=100 align=center>");
				list.append("<button value=\"進入基地\" action=\"bypass -h Quest PlayerBase visit_base ").append(ownerId);
				list.append("\" width=80 height=22 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
				list.append("</td></tr>");
			}
		}

		html.replace("%base_list%", list.toString());
		player.sendPacket(html);
		return null;
	}

	private String handleLeaveBase(Player player)
	{
		player.setInstance(null);
		player.teleToLocation(EXIT_LOC);
		player.sendMessage("您已離開基地");
		return null;
	}

	@Override
	public void onInstanceLeave(Player player, Instance instance)
	{
		player.setInstance(null);
		player.teleToLocation(EXIT_LOC);
	}

	/**
	 * 顯示主頁面
	 */
	private String showMainPage(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/PlayerBase/base_manager.htm");

		boolean hasBase = PlayerBaseDAO.hasBase(player.getObjectId());
		html.replace("%has_base%", hasBase ? "是" : "否");
		html.replace("%create_cost%", String.valueOf(CREATE_COST_COUNT));

		// 獲取可訪問的基地數量
		List<Map<String, Object>> visitableBases = PlayerBaseDAO.getVisitableBases(player.getObjectId());
		html.replace("%visitable_count%", String.valueOf(visitableBases.size()));

		player.sendPacket(html);
		return null;
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return showMainPage(player);
	}

	public static void main(String[] args)
	{
		new PlayerBase();
		System.out.println("【系統】玩家基地系統載入完畢!");
	}
}