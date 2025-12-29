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
package org.l2jmobius.gameserver.data.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.enums.EvolveLevel;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.PetDataTable;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.PetData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.holders.creature.PetEvolveHolder;
import org.l2jmobius.gameserver.model.actor.instance.Guardian;
import org.l2jmobius.gameserver.model.actor.instance.Pet;
import org.l2jmobius.gameserver.model.actor.instance.Servitor;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;

/**
 * @author Nyaran
 */
public class CharSummonTable
{
	private static final Logger LOGGER = Logger.getLogger(CharSummonTable.class.getName());
	private static final Map<Integer, Integer> _pets = new ConcurrentHashMap<>();
	private static final Map<Integer, Set<Integer>> _servitors = new ConcurrentHashMap<>();
	
	// SQL
	private static final String INIT_PET = "SELECT ownerId, item_obj_id FROM pets WHERE restore = 'true'";
	private static final String INIT_SUMMONS = "SELECT ownerId, summonId FROM character_summons";
	private static final String LOAD_SUMMON = "SELECT summonSkillId, summonId, curHp, curMp, time FROM character_summons WHERE ownerId = ?";
	private static final String REMOVE_SUMMON = "DELETE FROM character_summons WHERE ownerId = ? and summonId = ?";
	private static final String SAVE_SUMMON = "REPLACE INTO character_summons (ownerId,summonId,summonSkillId,curHp,curMp,time) VALUES (?,?,?,?,?,?)";
	
	public Map<Integer, Integer> getPets()
	{
		return _pets;
	}
	
	public Map<Integer, Set<Integer>> getServitors()
	{
		return _servitors;
	}
	
	public void init()
	{
		if (PlayerConfig.RESTORE_SERVITOR_ON_RECONNECT)
		{
			try (Connection con = DatabaseFactory.getConnection();
				Statement s = con.createStatement();
				ResultSet rs = s.executeQuery(INIT_SUMMONS))
			{
				while (rs.next())
				{
					_servitors.computeIfAbsent(rs.getInt("ownerId"), _ -> ConcurrentHashMap.newKeySet()).add(rs.getInt("summonId"));
				}
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Error while loading saved servitor: " + e);
			}
		}
		
		if (PlayerConfig.RESTORE_PET_ON_RECONNECT)
		{
			try (Connection con = DatabaseFactory.getConnection();
				Statement s = con.createStatement();
				ResultSet rs = s.executeQuery(INIT_PET))
			{
				while (rs.next())
				{
					_pets.put(rs.getInt("ownerId"), rs.getInt("item_obj_id"));
				}
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Error while loading saved pet: " + e);
			}
		}
	}
	
	public void removeServitor(Player player, int summonObjectId)
	{
		_servitors.computeIfPresent(player.getObjectId(), (_, v) ->
		{
			v.remove(summonObjectId);
			return !v.isEmpty() ? v : null;
		});
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(REMOVE_SUMMON))
		{
			ps.setInt(1, player.getObjectId());
			ps.setInt(2, summonObjectId);
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Summon cannot be removed: " + e);
		}
	}
	
	public void restorePet(Player player)
	{
		final Item item = player.getInventory().getItemByObjectId(_pets.get(player.getObjectId()));
		if (item == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Null pet summoning item for: " + player);
			return;
		}
		
		final PetEvolveHolder evolveData = player.getPetEvolve(item.getObjectId());
		final PetData petData = evolveData.getEvolve() == EvolveLevel.None ? PetDataTable.getInstance().getPetDataByEvolve(item.getId(), evolveData.getEvolve()) : PetDataTable.getInstance().getPetDataByEvolve(item.getId(), evolveData.getEvolve(), evolveData.getIndex());
		if (petData == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Null pet data for: " + player + " and summoning item: " + item);
			return;
		}
		
		final NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(petData.getNpcId());
		if (npcTemplate == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Null pet NPC template for: " + player + " and pet Id:" + petData.getNpcId());
			return;
		}
		
		final Pet pet = Pet.spawnPet(npcTemplate, player, item);
		if (pet == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Null pet instance for: " + player + " and pet NPC template:" + npcTemplate);
			return;
		}
		
		player.setPet(pet);
		pet.setShowSummonAnimation(true);
		pet.setTitle(player.getName());
		
		if (!pet.isRespawned())
		{
			pet.setCurrentHp(pet.getMaxHp());
			pet.setCurrentMp(pet.getMaxMp());
			pet.getStat().setExp(pet.getExpForThisLevel());
			pet.setCurrentFed(pet.getMaxFed());
			pet.storeMe();
		}
		
		pet.setRunning();
		item.setEnchantLevel(pet.getLevel());
		pet.spawnMe(player.getX() + 50, player.getY() + 100, player.getZ());
		pet.startFeed();
	}
	
	public void restoreServitor(Player player)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(LOAD_SUMMON))
		{
			ps.setInt(1, player.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				Skill skill;
				while (rs.next())
				{
					final int summonObjId = rs.getInt("summonId");
					final int skillId = rs.getInt("summonSkillId");
					final int curHp = rs.getInt("curHp");
					final int curMp = rs.getInt("curMp");
					final int time = rs.getInt("time");
					
					removeServitor(player, summonObjId);
					skill = SkillData.getInstance().getSkill(skillId, player.getSkillLevel(skillId));
					if (skill == null)
					{
						return;
					}
					
					skill.applyEffects(player, player);
					
					if (player.hasServitors())
					{
						Servitor servitor = null;
						for (Summon summon : player.getServitors().values())
						{
							if (summon instanceof Servitor)
							{
								final Servitor s = summon.asServitor();
								if (s.getReferenceSkill() == skillId)
								{
									servitor = s;
									break;
								}
							}
						}
						
						if (servitor != null)
						{
							servitor.setCurrentHp(curHp);
							servitor.setCurrentMp(curMp);
							servitor.setLifeTimeRemaining(time);
						}
					}
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Servitor cannot be restored: " + e);
		}
	}
	
	public void saveSummon(Servitor summon)
	{
		if (summon == null)
		{
			return;
		}
		
		_servitors.computeIfAbsent(summon.getOwner().getObjectId(), _ -> ConcurrentHashMap.newKeySet()).add(summon.getObjectId());
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SAVE_SUMMON))
		{
			ps.setInt(1, summon.getOwner().getObjectId());
			ps.setInt(2, summon.getObjectId());
			ps.setInt(3, summon.getReferenceSkill());
			ps.setInt(4, (int) Math.round(summon.getCurrentHp()));
			ps.setInt(5, (int) Math.round(summon.getCurrentMp()));
			ps.setInt(6, Math.max(0, summon.getLifeTimeRemaining()));
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Failed to store summon: " + summon + " from " + summon.getOwner() + ", error: " + e);
		}
	}
	
	public void storeGuardians(Player player)
	{
		final Collection<Npc> summonedNpcs = player.getSummonedNpcs();
		if (summonedNpcs.isEmpty())
		{
			return;
		}
		
		final List<Integer> summonedNpcIds = new LinkedList<>();
		for (Npc npc : summonedNpcs)
		{
			if (!npc.isDead() && npc.isSpawned() && (npc instanceof Guardian) && (npc.getCloneObjId() == 0))
			{
				summonedNpcIds.add(npc.getId());
			}
		}
		
		if (!summonedNpcIds.isEmpty())
		{
			player.getVariables().setIntegerList(PlayerVariables.SUMMONED_GUARDIAN_NPC_IDS, summonedNpcIds);
		}
	}
	
	public void restoreGuardians(Player player)
	{
		final List<Integer> summonedNpcIds = player.getVariables().getIntegerList(PlayerVariables.SUMMONED_GUARDIAN_NPC_IDS);
		if (!summonedNpcIds.isEmpty())
		{
			player.getVariables().remove(PlayerVariables.SUMMONED_GUARDIAN_NPC_IDS);
			
			for (int npcId : summonedNpcIds)
			{
				final NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(npcId);
				if (npcTemplate == null)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Spawn of the nonexisting NPC ID: " + npcId + ", skill ID:" + npcId);
					return;
				}
				
				int x = player.getX();
				int y = player.getY();
				final int z = player.getZ();
				
				x += (Rnd.nextBoolean() ? Rnd.get(0, 20) : Rnd.get(-20, 0));
				y += (Rnd.nextBoolean() ? Rnd.get(0, 20) : Rnd.get(-20, 0));
				
				final Guardian guardian = new Guardian(npcTemplate, player, false);
				guardian.setCurrentHp(guardian.getMaxHp());
				guardian.setCurrentMp(guardian.getMaxMp());
				guardian.setSummoner(player);
				player.addSummonedNpc(guardian);
				guardian.spawnMe(x, y, z);
				guardian.setInstance(player.getInstanceWorld());
				guardian.setRunning();
				guardian.scheduleDespawn(1200000); // 20 minutes.
				guardian.setShowSummonAnimation(true);
				guardian.startAttackTask();
			}
			
			// Update skill shortcut icons with a delay.
			ThreadPool.schedule(() -> player.sendSkillList(), 3000);
		}
	}
	
	public static CharSummonTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CharSummonTable INSTANCE = new CharSummonTable();
	}
}
