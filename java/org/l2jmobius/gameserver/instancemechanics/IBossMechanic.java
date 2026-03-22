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

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * 副本 BOSS 機制接口
 * @author Custom
 */
public interface IBossMechanic
{
	/**
	 * 當 BOSS 生成時調用
	 * @param boss BOSS NPC
	 * @param world 副本實例
	 */
	void onBossSpawn(Npc boss, Instance world);

	/**
	 * 當 BOSS 攻擊時調用
	 * @param boss BOSS NPC
	 * @param target 攻擊目標
	 * @param skill 使用的技能（可能為 null）
	 */
	void onBossAttack(Npc boss, Creature target, Skill skill);

	/**
	 * 當 BOSS 受到傷害時調用
	 * @param boss BOSS NPC
	 * @param attacker 攻擊者
	 * @param damage 傷害值
	 */
	void onBossDamaged(Npc boss, Creature attacker, double damage);

	/**
	 * 當 BOSS 血量變化時調用（用於階段切換）
	 * @param boss BOSS NPC
	 * @param oldHpPercent 舊的血量百分比
	 * @param newHpPercent 新的血量百分比
	 */
	void onBossHpChange(Npc boss, double oldHpPercent, double newHpPercent);

	/**
	 * 當 BOSS 死亡時調用
	 * @param boss BOSS NPC
	 * @param killer 擊殺者
	 */
	void onBossDeath(Npc boss, Creature killer);

	/**
	 * 清理機制（定時器、任務等）
	 */
	void cleanup();

	/**
	 * 獲取機制名稱（用於日誌）
	 * @return 機制名稱
	 */
	String getName();
}
