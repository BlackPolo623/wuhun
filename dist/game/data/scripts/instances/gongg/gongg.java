/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package instances.gongg;

import java.util.Calendar;

import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.InstanceScript;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author RobikBobik, Mobius
 * @NOTE: Party instance retail like work.
 * @TODO: Find what all drops from GOLBERG_TREASURE_CHEST
 * @TODO: Golberg skills
 */
public class gongg extends InstanceScript
{
	// NPCs
	private static final int SORA = 900004;// 進入npc
	// Misc
	private static int TEMPLATE_ID = 322;
	
	private static int X = -82165;
	private static int Y = 245149;
	private static int Z = -3712;
	
	public gongg()
	{
		super(TEMPLATE_ID);
		addStartNpc(SORA);
		addInstanceLeaveId(TEMPLATE_ID);
		addFirstTalkId(SORA);
		addTalkId(SORA);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = "";
		if (event.startsWith("enterInstance"))
		{
			String newBC = event.substring(14);
			int Instanceid = 600;
			if (newBC.startsWith("武魂共通修練場一樓"))
			{
				TEMPLATE_ID = 800;
				Instanceid = 600;
				X = -82165;// 坐标
				Y = 245149;// 坐标
				Z = -3712; // 坐标
			}
			if (newBC.startsWith("武魂共通修練場二樓"))
			{
				TEMPLATE_ID = 801;
				Instanceid = 601;
				X = 136951;// 坐标
				Y = 101757;// 坐标
				Z = -720; // 坐标
			}

			Calendar c = Calendar.getInstance();
			long nowMills = c.getTimeInMillis();
			c.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE), 23, 59, 59);
			long setMills = c.getTimeInMillis();
			long time = player.getVariables().getLong("修練場副本時間" + Instanceid + "存在時間", 0);
			long starttime = player.getVariables().getLong("修練場副本時間" + Instanceid + "", 0);
			
			if ((starttime + (time * 60 * 1000)) < setMills)
			{
				player.getVariables().set("修練場副本時間" + Instanceid + "存在時間", String.valueOf(0));
			}
			if ((starttime + (time * 60 * 1000)) >= nowMills)
			{
				player.sendPacket(new ExShowScreenMessage("今天時間已經用完了", 3000));
				return htmltext;
			}
			if (player.getInventory().getInventoryItemCount(57, 0) >= 5000)
			{
				takeItems(player, 57, 5000);
				boolean instance = InstanceManager.getInstance().createInstanceFromTemplate(Instanceid, TEMPLATE_ID);
				if (instance == true)
				{
					Instance instancec = InstanceManager.getInstance().createInstance(TEMPLATE_ID, Instanceid, player);
					player.setInstance(instancec);
					player.teleToLocation(X, Y, Z);// 修改按地點
					player.getVariables().set("修練場副本時間" + Instanceid + "", String.valueOf(nowMills));
				}
				else
				{
					Instance instancea = InstanceManager.getInstance().getInstance(Instanceid);
					player.teleToLocation(X, Y, Z, 0, instancea);// 修改按地點
					player.getVariables().set("修練場副本時間" + Instanceid + "", String.valueOf(nowMills));
				}
			}
			else
			{
				player.sendPacket(new ExShowScreenMessage("所需道具不足，無法進入修練場！", 3000));
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = "";
		NpcHtmlMessage rateReplya = new NpcHtmlMessage(0, 1);
		rateReplya.setFile(player, "data/scripts/instances/gongg/gongg.htm");
		player.sendPacket(rateReplya);
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new gongg();
		System.out.println("武魂共通修練場載入完畢！");
		
	}
}
