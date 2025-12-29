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
package ai.others.NewbieGuide;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;

/**
 * @author Mobius
 */
public class NewbieGuide extends Script
{
	// NPCs
	private static final int[] NEWBIE_GUIDES =
	{
		30598,
		30599,
		30600,
		30601,
		30602,
		34110, // Kamael
	};
	
	// Items
	private static final ItemHolder SOULSHOT_REWARD = new ItemHolder(91927, 200);
	private static final ItemHolder SPIRITSHOT_REWARD = new ItemHolder(91928, 100);
	
	// Other
	private static final String TUTORIAL_QUEST = "Q00255_Tutorial";
	private static final String SUPPORT_MAGIC_STRING = "<Button ALIGN=LEFT ICON=\"NORMAL\" action=\"bypass -h Link default/SupportMagic.htm\">Receive help from beneficial magic.</Button>";
	
	private NewbieGuide()
	{
		addStartNpc(NEWBIE_GUIDES);
		addTalkId(NEWBIE_GUIDES);
		addFirstTalkId(NEWBIE_GUIDES);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		if (event.equals("0"))
		{
			if (PlayerConfig.MAX_NEWBIE_BUFF_LEVEL > 0)
			{
				htmltext = npc.getId() + ".htm";
			}
			else
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
				html.setFile(player, "data/scripts/ai/others/NewbieGuide/" + npc.getId() + ".htm");
				html.replace(SUPPORT_MAGIC_STRING, "");
				player.sendPacket(html);
				return htmltext;
			}
		}
		else
		{
			htmltext = npc.getId() + "-" + event + (player.isMageClass() ? "m" : "f") + ".htm";
		}
		
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (npc.getRace() != player.getTemplate().getRace())
		{
			return npc.getId() + "-no.htm";
		}
		
		final QuestState qs = player.getQuestState(TUTORIAL_QUEST);
		if ((qs != null) && !PlayerConfig.DISABLE_TUTORIAL && qs.isMemoState(5))
		{
			qs.setMemoState(6);
			if (player.isMageClass() && (player.getRace() != Race.ORC))
			{
				giveItems(player, SPIRITSHOT_REWARD);
				playTutorialVoice(player, "tutorial_voice_027_a");
			}
			else
			{
				giveItems(player, SOULSHOT_REWARD);
				playTutorialVoice(player, "tutorial_voice_026_a");
			}
		}
		
		if (PlayerConfig.MAX_NEWBIE_BUFF_LEVEL > 0)
		{
			return npc.getId() + ".htm";
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/ai/others/NewbieGuide/" + npc.getId() + ".htm");
		html.replace(SUPPORT_MAGIC_STRING, "");
		player.sendPacket(html);
		return null;
	}
	
	public void playTutorialVoice(Player player, String voice)
	{
		player.sendPacket(new PlaySound(2, voice, 0, 0, player.getX(), player.getY(), player.getZ()));
	}
	
	public static void main(String[] args)
	{
		new NewbieGuide();
	}
}
