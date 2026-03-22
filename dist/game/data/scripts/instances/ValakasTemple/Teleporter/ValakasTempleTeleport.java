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
package instances.ValakasTemple.Teleporter;

import java.util.List;

import org.l2jmobius.gameserver.managers.events.ValakasTempleManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.script.InstanceScript;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

import instances.ValakasTemple.ValakasTemple;

/**
 * @author Index (simplified version)
 */
public class ValakasTempleTeleport extends InstanceScript
{
	public static final int PARME_NPC_ID = 34258;

	// ========================================
	// 設定區：可自行調整
	// ========================================
	/** 每週最大進入次數（覆蓋 ValakasTempleManager 的設定） */
	private static final int MAX_WEEKLY_ENTRIES = 100;
	// ========================================

	private ValakasTempleTeleport()
	{
		super(ValakasTemple.VALAKAS_TEMPLE_INSTANCE_ID);
		addTalkId(PARME_NPC_ID);
		addFirstTalkId(PARME_NPC_ID);
		addStartNpc(PARME_NPC_ID);
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final ValakasTempleManager manager = ValakasTempleManager.getInstance();
		final int remaining = manager.getRemainingEntries(player);
		final String resetTime = manager.getNextResetString();

		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/instances/ValakasTemple/Teleporter/34258.htm");
		html.replace("%remaining%", String.valueOf(remaining));
		html.replace("%maxEntries%", String.valueOf(MAX_WEEKLY_ENTRIES));
		html.replace("%resetTime%", resetTime);
		player.sendPacket(html);
		return null;
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("enterInstance"))
		{
			handleEnter(npc, player);
		}
		return super.onEvent(event, npc, player);
	}

	private void handleEnter(Npc npc, Player player)
	{
		final ValakasTempleManager manager = ValakasTempleManager.getInstance();

		// GM 可直接進入（單人或帶隊）
		if (player.isGM())
		{
			if (player.isInParty())
			{
				for (Player member : player.getParty().getMembers())
				{
					enterInstance(member, npc, ValakasTemple.VALAKAS_TEMPLE_INSTANCE_ID);
				}
			}
			else
			{
				enterInstance(player, npc, ValakasTemple.VALAKAS_TEMPLE_INSTANCE_ID);
			}
			player.sendPacket(new ExShowScreenMessage("以 GM 身份進入巴拉卡斯神殿", 3000));
			return;
		}

		// 必須組隊
		if (!player.isInParty())
		{
			player.sendPacket(new ExShowScreenMessage("你必須組隊才能進入巴拉卡斯神殿", 3000));
			return;
		}

		// 檢查自己的次數
		if (!manager.canEnter(player))
		{
			final int remaining = manager.getRemainingEntries(player);
			player.sendPacket(new ExShowScreenMessage("你本週的進入次數已用完 (" + remaining + "/" + MAX_WEEKLY_ENTRIES + ")", 3000));
			return;
		}

		final Party party = player.getParty();
		final List<Player> members = party.getMembers();

		// 先檢查所有隊員條件
		for (Player member : members)
		{
			if (!member.isInsideRadius3D(npc, 1500))
			{
				player.sendPacket(new ExShowScreenMessage("隊員 " + member.getName() + " 距離太遠", 3000));
				return;
			}
			if (member.getLevel() < 76)
			{
				player.sendPacket(new ExShowScreenMessage("隊員 " + member.getName() + " 等級不足（需要76級）", 3000));
				return;
			}
		}

		// 全部符合條件，逐一傳送
		for (Player member : members)
		{
			if (manager.canEnter(member))
			{
				enterInstance(member, npc, ValakasTemple.VALAKAS_TEMPLE_INSTANCE_ID);
				manager.incrementEntryCount(member);
			}
			else
			{
				member.sendPacket(new ExShowScreenMessage("你本週的進入次數已用完", 3000));
			}
		}
	}

	public static void main(String[] args)
	{
		new ValakasTempleTeleport();
	}
}
