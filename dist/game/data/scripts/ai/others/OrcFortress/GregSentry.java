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
package ai.others.OrcFortress;

import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.siege.FortSiege;

/**
 * @author Serenitty
 */
public class GregSentry extends Script
{
	
	private static final int GREG_SENTRY = 22156;
	private static final int FLAG = 93331;
	private static final int FLAG_MAX_COUNT = 3;
	
	private GregSentry()
	{
		addKillId(GREG_SENTRY);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final FortSiege siege = FortManager.getInstance().getFortById(FortManager.ORC_FORTRESS).getSiege();
		if ((siege != null) && (siege.getFlagCount() < FLAG_MAX_COUNT))
		{
			final Item flag = npc.dropItem(killer, FLAG, 1);
			if (flag != null)
			{
				String spawnGroup = npc.getSpawn().getNpcSpawnTemplate().getGroup().getName();
				if ((spawnGroup == null) || spawnGroup.isEmpty())
				{
					spawnGroup = FortSiege.ORC_FORTRESS_GREG_BOTTOM_RIGHT_SPAWN;
				}
				
				flag.getVariables().set(FortSiege.GREG_SPAWN_VAR, spawnGroup);
				siege.addFlagCount(1);
			}
		}
	}
	
	public static void main(String[] args)
	{
		new GregSentry();
	}
}
