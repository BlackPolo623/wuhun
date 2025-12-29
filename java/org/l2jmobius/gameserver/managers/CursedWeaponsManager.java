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
package org.l2jmobius.gameserver.managers;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.model.CursedWeapon;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Defender;
import org.l2jmobius.gameserver.model.actor.instance.FeedableBeast;
import org.l2jmobius.gameserver.model.actor.instance.FortCommander;
import org.l2jmobius.gameserver.model.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.model.actor.instance.Guard;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * @author Micht
 */
public class CursedWeaponsManager implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(CursedWeaponsManager.class.getName());
	
	private final Map<Integer, CursedWeapon> _cursedWeapons = new HashMap<>();
	
	protected CursedWeaponsManager()
	{
		load();
	}
	
	@Override
	public void load()
	{
		if (!GeneralConfig.ALLOW_CURSED_WEAPONS)
		{
			return;
		}
		
		parseDatapackFile("data/CursedWeapons.xml");
		restore();
		controlPlayers();
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _cursedWeapons.size() + " cursed weapons.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("item".equalsIgnoreCase(d.getNodeName()))
					{
						NamedNodeMap attrs = d.getAttributes();
						final int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
						final int skillId = Integer.parseInt(attrs.getNamedItem("skillId").getNodeValue());
						final String name = attrs.getNamedItem("name").getNodeValue();
						final CursedWeapon cw = new CursedWeapon(id, skillId, name);
						int val;
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							if ("dropRate".equalsIgnoreCase(cd.getNodeName()))
							{
								attrs = cd.getAttributes();
								val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
								cw.setDropRate(val);
							}
							else if ("duration".equalsIgnoreCase(cd.getNodeName()))
							{
								attrs = cd.getAttributes();
								val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
								cw.setDuration(val);
							}
							else if ("durationLost".equalsIgnoreCase(cd.getNodeName()))
							{
								attrs = cd.getAttributes();
								val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
								cw.setDurationLost(val);
							}
							else if ("disapearChance".equalsIgnoreCase(cd.getNodeName()))
							{
								attrs = cd.getAttributes();
								val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
								cw.setDisapearChance(val);
							}
							else if ("stageKills".equalsIgnoreCase(cd.getNodeName()))
							{
								attrs = cd.getAttributes();
								val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
								cw.setStageKills(val);
							}
						}
						
						// Store cursed weapon
						_cursedWeapons.put(id, cw);
					}
				}
			}
		}
	}
	
	private void restore()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT itemId, charId, playerReputation, playerPkKills, nbKills, endTime FROM cursed_weapons"))
		{
			// Restore data for each cursed weapon from the database.
			CursedWeapon cw;
			while (rs.next())
			{
				cw = _cursedWeapons.get(rs.getInt("itemId"));
				cw.setPlayerId(rs.getInt("charId"));
				cw.setPlayerReputation(rs.getInt("playerReputation"));
				cw.setPlayerPkKills(rs.getInt("playerPkKills"));
				cw.setNbKills(rs.getInt("nbKills"));
				cw.setEndTime(rs.getLong("endTime"));
				cw.reActivate();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not restore CursedWeapons data: ", e);
		}
	}
	
	private void controlPlayers()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT owner_id FROM items WHERE item_id=?"))
		{
			for (CursedWeapon cw : _cursedWeapons.values())
			{
				if (cw.isActivated())
				{
					continue; // Skip activated cursed weapons.
				}
				
				// Verify if the cursed weapon is held by a player.
				final int itemId = cw.getItemId();
				ps.setInt(1, itemId);
				try (ResultSet rset = ps.executeQuery())
				{
					if (rset.next())
					{
						final int playerId = rset.getInt("owner_id");
						LOGGER.info("PROBLEM : Player " + playerId + " owns the cursed weapon " + itemId + " but he shouldn't.");
						
						// Delete the item from the player's inventory.
						try (PreparedStatement delete = con.prepareStatement("DELETE FROM items WHERE owner_id=? AND item_id=?"))
						{
							delete.setInt(1, playerId);
							delete.setInt(2, itemId);
							if (delete.executeUpdate() != 1)
							{
								LOGGER.warning("Error while deleting cursed weapon " + itemId + " from userId " + playerId);
							}
						}
						
						// Restore the player's old karma and PK count.
						try (PreparedStatement update = con.prepareStatement("UPDATE characters SET reputation=?, pkkills=? WHERE charId=?"))
						{
							update.setInt(1, cw.getPlayerReputation());
							update.setInt(2, cw.getPlayerPkKills());
							update.setInt(3, playerId);
							if (update.executeUpdate() != 1)
							{
								LOGGER.warning("Error while updating karma & pkkills for userId " + cw.getPlayerId());
							}
						}
						
						// Clean up the cursed weapons table.
						removeFromDb(itemId);
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not check CursedWeapons data: ", e);
		}
	}
	
	public synchronized void checkDrop(Attackable attackable, Player player)
	{
		if ((attackable instanceof Defender) || (attackable instanceof Guard) || (attackable instanceof GrandBoss) || (attackable instanceof FeedableBeast) || (attackable instanceof FortCommander))
		{
			return;
		}
		
		for (CursedWeapon cw : _cursedWeapons.values())
		{
			if (cw.isActive())
			{
				continue;
			}
			
			if (cw.checkDrop(attackable, player))
			{
				break;
			}
		}
	}
	
	public void activate(Player player, Item item)
	{
		final CursedWeapon cw = _cursedWeapons.get(item.getId());
		
		if (player.isCursedWeaponEquipped()) // Cannot own two cursed swords.
		{
			final CursedWeapon cw2 = _cursedWeapons.get(player.getCursedWeaponEquippedId());
			
			// Temporary fix to adjust the bonus level for the existing cursed weapon.
			// Update the kill count based on the stage kills.
			cw2.setNbKills(cw2.getStageKills() - 1);
			cw2.increaseKills();
			
			// Erase the newly obtained cursed weapon.
			cw.setPlayer(player); // Necessary to identify the inventory location.
			cw.endOfLife(); // Expire the weapon and clean up.
		}
		else
		{
			cw.activate(player, item); // Activate the cursed weapon for the player.
		}
	}
	
	public void drop(int itemId, Creature killer)
	{
		final CursedWeapon cw = _cursedWeapons.get(itemId);
		cw.dropIt(killer);
	}
	
	public void increaseKills(int itemId)
	{
		final CursedWeapon cw = _cursedWeapons.get(itemId);
		cw.increaseKills();
	}
	
	public int getLevel(int itemId)
	{
		final CursedWeapon cw = _cursedWeapons.get(itemId);
		return cw.getLevel();
	}
	
	public static void announce(SystemMessage sm)
	{
		Broadcast.toAllOnlinePlayers(sm);
	}
	
	public void checkPlayer(Player player)
	{
		if (player == null)
		{
			return;
		}
		
		for (CursedWeapon cw : _cursedWeapons.values())
		{
			if (cw.isActivated() && (player.getObjectId() == cw.getPlayerId()))
			{
				cw.setPlayer(player);
				cw.setItem(player.getInventory().getItemByItemId(cw.getItemId()));
				cw.giveSkill();
				player.setCursedWeaponEquippedId(cw.getItemId());
				
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_S2_MIN_OF_USAGE_TIME_REMAINING);
				sm.addString(cw.getName());
				// sm.addItemName(cw.getItemId());
				sm.addInt((int) ((cw.getEndTime() - System.currentTimeMillis()) / 60000));
				player.sendPacket(sm);
			}
		}
	}
	
	public int checkOwnsWeaponId(int ownerId)
	{
		for (CursedWeapon cw : _cursedWeapons.values())
		{
			if (cw.isActivated() && (ownerId == cw.getPlayerId()))
			{
				return cw.getItemId();
			}
		}
		
		return -1;
	}
	
	public static void removeFromDb(int itemId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM cursed_weapons WHERE itemId = ?"))
		{
			ps.setInt(1, itemId);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Failed to remove data: " + e.getMessage(), e);
		}
	}
	
	public void saveData()
	{
		for (CursedWeapon cw : _cursedWeapons.values())
		{
			cw.saveData();
		}
	}
	
	public boolean isCursed(int itemId)
	{
		return _cursedWeapons.containsKey(itemId);
	}
	
	public Collection<CursedWeapon> getCursedWeapons()
	{
		return _cursedWeapons.values();
	}
	
	public Set<Integer> getCursedWeaponsIds()
	{
		return _cursedWeapons.keySet();
	}
	
	public CursedWeapon getCursedWeapon(int itemId)
	{
		return _cursedWeapons.get(itemId);
	}
	
	public void givePassive(int itemId)
	{
		try
		{
			_cursedWeapons.get(itemId).giveSkill();
		}
		catch (Exception e)
		{
		}
	}
	
	public static CursedWeaponsManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CursedWeaponsManager INSTANCE = new CursedWeaponsManager();
	}
}
