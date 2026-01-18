package custom.PlayerBase;

import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class BaseVisitorNpc extends Script
{
	private static final int VISITOR_NPC_ID = 900026;
	
	public BaseVisitorNpc()
	{
		addStartNpc(VISITOR_NPC_ID);
		addTalkId(VISITOR_NPC_ID);
		addFirstTalkId(VISITOR_NPC_ID);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (!isBaseOwner(player))
		{
			player.sendMessage("只有基地主人才能使用此功能！");
			return null;
		}
		
		if (event.startsWith("add_visitor "))
		{
			String visitorName = event.substring(12);
			return handleAddVisitor(player, visitorName);
		}
		else if (event.startsWith("remove_visitor "))
		{
			int visitorId = Integer.parseInt(event.substring(15));
			return handleRemoveVisitor(player, visitorId);
		}
		else if (event.startsWith("kick_player "))
		{
			int targetId = Integer.parseInt(event.substring(12));
			return handleKickPlayer(player, targetId);
		}
		else if (event.equals("view_visitors"))
		{
			return showVisitorsList(player);
		}
		else if (event.equals("view_online"))
		{
			return showOnlinePlayers(player);
		}
		else if (event.equals("main"))
		{
			return onFirstTalk(npc,player);
		}
		
		return null;
	}
	
	private boolean isBaseOwner(Player player)
	{
		Instance instance = player.getInstanceWorld();
		if (instance == null)
		{
			return false;
		}
		
		Map<String, Object> baseInfo = PlayerBaseDAO.getBaseInfo(player.getObjectId());
		return !baseInfo.isEmpty() && (int) baseInfo.get("instance_id") == instance.getId();
	}
	
	private String handleAddVisitor(Player player, String visitorName)
	{
		Player visitor = World.getInstance().getPlayer(visitorName);
		
		if (visitor == null)
		{
			player.sendMessage("玩家 " + visitorName + " 不在線上！");
			return null;
		}
		
		if (visitor.getObjectId() == player.getObjectId())
		{
			player.sendMessage("不能添加自己！");
			return null;
		}
		
		if (PlayerBaseDAO.addVisitor(player.getObjectId(), visitor.getObjectId(), visitor.getName()))
		{
			player.sendMessage("成功添加訪客：" + visitorName);
		}
		else
		{
			player.sendMessage("添加訪客失敗！");
		}
		
		return showVisitorsList(player);
	}
	
	private String handleRemoveVisitor(Player player, int visitorId)
	{
		if (PlayerBaseDAO.removeVisitor(player.getObjectId(), visitorId))
		{
			player.sendMessage("訪客已移除！");
		}
		else
		{
			player.sendMessage("移除訪客失敗！");
		}
		
		return showVisitorsList(player);
	}
	
	private String handleKickPlayer(Player player, int targetId)
	{
		Player target = World.getInstance().getPlayer(targetId);
		
		if (target == null)
		{
			player.sendMessage("玩家不在線！");
			return null;
		}
		
		Instance instance = player.getInstanceWorld();
		if (target.getInstanceWorld() != instance)
		{
			player.sendMessage("該玩家不在您的基地中！");
			return null;
		}
		
		target.setInstance(null);
		target.teleToLocation(147931, 213039, -2177);
		target.sendMessage("您已被基地主人踢出！");
		player.sendMessage("已踢出玩家：" + target.getName());
		
		return showOnlinePlayers(player);
	}
	
	private String showVisitorsList(Player player)
	{
		List<Map<String, Object>> visitors = PlayerBaseDAO.getVisitors(player.getObjectId());
		
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/PlayerBase/visitor_list.htm");
		
		StringBuilder list = new StringBuilder();
		
		if (visitors.isEmpty())
		{
			list.append("<tr><td align=center height=30><font color=\"808080\">暫無訪客</font></td></tr>");
		}
		else
		{
			for (Map<String, Object> visitor : visitors)
			{
				String name = (String) visitor.get("visitor_name");
				int id = (int) visitor.get("visitor_id");
				
				list.append("<tr bgcolor=\"222222\">");
				list.append("<td width=150 align=left><font color=\"00FF66\">").append(name).append("</font></td>");
				list.append("<td width=130 align=center>");
				list.append("<button value=\"移除\" action=\"bypass -h Quest BaseVisitorNpc remove_visitor ").append(id);
				list.append("\" width=70 height=22 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
				list.append("</td></tr>");
			}
		}
		
		html.replace("%visitor_list%", list.toString());
		player.sendPacket(html);
		return null;
	}
	
	private String showOnlinePlayers(Player player)
	{
		Instance instance = player.getInstanceWorld();
		
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/PlayerBase/online_players.htm");
		
		StringBuilder list = new StringBuilder();
		boolean hasPlayers = false;
		
		for (Player p : instance.getPlayers())
		{
			if (p.getObjectId() == player.getObjectId())
			{
				continue;
			}
			
			hasPlayers = true;
			list.append("<tr bgcolor=\"222222\">");
			list.append("<td width=150 align=left><font color=\"00FF66\">").append(p.getName()).append("</font></td>");
			list.append("<td width=130 align=center>");
			list.append("<button value=\"踢出\" action=\"bypass -h Quest BaseVisitorNpc kick_player ").append(p.getObjectId());
			list.append("\" width=70 height=22 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
			list.append("</td></tr>");
		}
		
		if (!hasPlayers)
		{
			list.append("<tr><td align=center height=30><font color=\"808080\">暫無其他玩家</font></td></tr>");
		}
		
		html.replace("%player_list%", list.toString());
		player.sendPacket(html);
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/PlayerBase/visitor_manager.htm");
		
		boolean isOwner = isBaseOwner(player);
		html.replace("%is_owner%", isOwner ? "是" : "否");
		
		player.sendPacket(html);
		return null;
	}
	
	public static void main(String[] args)
	{
		new BaseVisitorNpc();
		System.out.println("【系統】基地訪客管理系統載入完畢！");
	}
}