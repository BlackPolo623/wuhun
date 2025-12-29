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
package org.l2jmobius.gameserver.network.clientpackets.quest;

import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerQuestAbort;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.QuestType;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestNotificationAll;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestUI;

/**
 * @author Magik, Mobius
 */
public class RequestExQuestCancel extends ClientPacket
{
	private int _questId;
	
	@Override
	protected void readImpl()
	{
		_questId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Quest quest = ScriptManager.getInstance().getQuest(_questId);
		final QuestState qs = quest.getQuestState(player, false);
		if ((qs != null) && !qs.isCompleted())
		{
			qs.setSimulated(false);
			qs.exitQuest(QuestType.REPEATABLE);
			player.sendPacket(new ExQuestUI(player));
			player.sendPacket(new ExQuestNotificationAll(player));
			
			if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_QUEST_ABORT, player, Containers.Players()))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerQuestAbort(player, _questId), player, Containers.Players());
			}
			
			quest.onQuestAborted(player);
		}
	}
}
