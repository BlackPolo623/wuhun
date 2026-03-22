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
package org.l2jmobius.gameserver.instancemechanics;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * 副本 BOSS 機制抽象基類，提供通用工具方法
 * @author Custom
 */
public abstract class AbstractBossMechanic implements IBossMechanic
{
	protected static final Logger LOGGER = Logger.getLogger(AbstractBossMechanic.class.getName());

	protected final Npc _boss;
	protected final Instance _world;
	protected volatile boolean _active = false;

	protected AbstractBossMechanic(Npc boss, Instance world)
	{
		_boss = boss;
		_world = world;
	}

	/**
	 * 獲取副本內所有玩家
	 */
	protected List<Player> getPlayersInInstance()
	{
		return new ArrayList<>(_world.getPlayers());
	}

	/**
	 * 對副本內所有玩家廣播聊天訊息
	 */
	protected void broadcastMessage(String message)
	{
		_world.getPlayers().forEach(p -> p.sendMessage(message));
	}

	/**
	 * 對副本內所有玩家廣播屏幕訊息
	 * @param message 訊息內容
	 * @param duration 顯示時間（毫秒）
	 */
	protected void broadcastScreenMessage(String message, int duration)
	{
		_world.getPlayers().forEach(p -> p.sendPacket(new ExShowScreenMessage(message, duration)));
	}

	// 默認空實現，子類按需覆寫
	@Override
	public void onBossSpawn(Npc boss, Instance world)
	{
	}

	@Override
	public void onBossAttack(Npc boss, Creature target, Skill skill)
	{
	}

	@Override
	public void onBossDamaged(Npc boss, Creature attacker, double damage)
	{
	}

	@Override
	public void onBossHpChange(Npc boss, double oldHpPercent, double newHpPercent)
	{
	}

	@Override
	public void onBossDeath(Npc boss, Creature killer)
	{
	}

	@Override
	public void cleanup()
	{
		_active = false;
	}
}
