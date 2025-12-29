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
package org.l2jmobius.gameserver.network.serverpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.config.custom.MultilingualSupportConfig;
import org.l2jmobius.gameserver.config.custom.OfflinePlayConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.model.VariationInstance;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.olympiad.Hero;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.holders.CharacterInfoHolder;

/**
 * @author Mobius
 */
public class CharSelectionInfo extends ServerPacket
{
	private static final Logger LOGGER = Logger.getLogger(CharSelectionInfo.class.getName());
	
	private static final int[] PAPERDOLL_ORDER =
	{
		Inventory.PAPERDOLL_UNDER,
		Inventory.PAPERDOLL_REAR,
		Inventory.PAPERDOLL_LEAR,
		Inventory.PAPERDOLL_NECK,
		Inventory.PAPERDOLL_RFINGER,
		Inventory.PAPERDOLL_LFINGER,
		Inventory.PAPERDOLL_HEAD,
		Inventory.PAPERDOLL_RHAND,
		Inventory.PAPERDOLL_LHAND,
		Inventory.PAPERDOLL_GLOVES,
		Inventory.PAPERDOLL_CHEST,
		Inventory.PAPERDOLL_LEGS,
		Inventory.PAPERDOLL_FEET,
		Inventory.PAPERDOLL_CLOAK,
		Inventory.PAPERDOLL_RHAND,
		Inventory.PAPERDOLL_HAIR,
		Inventory.PAPERDOLL_HAIR2,
		Inventory.PAPERDOLL_RBRACELET,
		Inventory.PAPERDOLL_LBRACELET,
		Inventory.PAPERDOLL_AGATHION1, // 152
		Inventory.PAPERDOLL_AGATHION2, // 152
		Inventory.PAPERDOLL_AGATHION3, // 152
		Inventory.PAPERDOLL_AGATHION4, // 152
		Inventory.PAPERDOLL_AGATHION5, // 152
		Inventory.PAPERDOLL_DECO1,
		Inventory.PAPERDOLL_DECO2,
		Inventory.PAPERDOLL_DECO3,
		Inventory.PAPERDOLL_DECO4,
		Inventory.PAPERDOLL_DECO5,
		Inventory.PAPERDOLL_DECO6,
		Inventory.PAPERDOLL_BELT,
		Inventory.PAPERDOLL_BROOCH,
		Inventory.PAPERDOLL_BROOCH_JEWEL1,
		Inventory.PAPERDOLL_BROOCH_JEWEL2,
		Inventory.PAPERDOLL_BROOCH_JEWEL3,
		Inventory.PAPERDOLL_BROOCH_JEWEL4,
		Inventory.PAPERDOLL_BROOCH_JEWEL5,
		Inventory.PAPERDOLL_BROOCH_JEWEL6,
		Inventory.PAPERDOLL_ARTIFACT_BOOK, // 152
		Inventory.PAPERDOLL_ARTIFACT1, // 152
		Inventory.PAPERDOLL_ARTIFACT2, // 152
		Inventory.PAPERDOLL_ARTIFACT3, // 152
		Inventory.PAPERDOLL_ARTIFACT4, // 152
		Inventory.PAPERDOLL_ARTIFACT5, // 152
		Inventory.PAPERDOLL_ARTIFACT6, // 152
		Inventory.PAPERDOLL_ARTIFACT7, // 152
		Inventory.PAPERDOLL_ARTIFACT8, // 152
		Inventory.PAPERDOLL_ARTIFACT9, // 152
		Inventory.PAPERDOLL_ARTIFACT10, // 152
		Inventory.PAPERDOLL_ARTIFACT11, // 152
		Inventory.PAPERDOLL_ARTIFACT12, // 152
		Inventory.PAPERDOLL_ARTIFACT13, // 152
		Inventory.PAPERDOLL_ARTIFACT14, // 152
		Inventory.PAPERDOLL_ARTIFACT15, // 152
		Inventory.PAPERDOLL_ARTIFACT16, // 152
		Inventory.PAPERDOLL_ARTIFACT17, // 152
		Inventory.PAPERDOLL_ARTIFACT18, // 152
		Inventory.PAPERDOLL_ARTIFACT19, // 152
		Inventory.PAPERDOLL_ARTIFACT20, // 152
		Inventory.PAPERDOLL_ARTIFACT21 // 152
	};
	private static final int[] PAPERDOLL_ORDER_VISUAL_ID =
	{
		Inventory.PAPERDOLL_RHAND,
		Inventory.PAPERDOLL_LHAND,
		Inventory.PAPERDOLL_GLOVES,
		Inventory.PAPERDOLL_CHEST,
		Inventory.PAPERDOLL_LEGS,
		Inventory.PAPERDOLL_FEET,
		Inventory.PAPERDOLL_RHAND,
		Inventory.PAPERDOLL_HAIR,
		Inventory.PAPERDOLL_HAIR2,
	};
	
	private final String _loginName;
	private final int _sessionId;
	private int _activeId;
	private final List<CharacterInfoHolder> _characterPackages;
	
	/**
	 * Constructor for CharSelectionInfo.
	 * @param loginName
	 * @param sessionId
	 */
	public CharSelectionInfo(String loginName, int sessionId)
	{
		_sessionId = sessionId;
		_loginName = loginName;
		_characterPackages = loadCharacterSelectInfo(_loginName);
		_activeId = -1;
	}
	
	public CharSelectionInfo(String loginName, int sessionId, int activeId)
	{
		_sessionId = sessionId;
		_loginName = loginName;
		_characterPackages = loadCharacterSelectInfo(_loginName);
		_activeId = activeId;
	}
	
	public List<CharacterInfoHolder> getCharInfo()
	{
		return _characterPackages;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.CHARACTER_SELECTION_INFO.writeId(this, buffer);
		final int size = _characterPackages.size();
		buffer.writeInt(size); // Created character count
		buffer.writeInt(ServerConfig.MAX_CHARACTERS_NUMBER_PER_ACCOUNT); // Can prevent players from creating new characters (if 0); (if 1, the client will ask if chars may be created (0x13) Response: (0x0D) )
		buffer.writeByte(size == ServerConfig.MAX_CHARACTERS_NUMBER_PER_ACCOUNT); // if 1 can't create new char
		buffer.writeByte(1); // 0=can't play, 1=can play free until level 85, 2=100% free play
		buffer.writeInt(2); // if 1, Korean client
		buffer.writeByte(0); // Gift message for inactive accounts // 152
		buffer.writeByte(0); // Balthus Knights, if 1 suggests premium account
		
		long lastAccess = 0;
		if (_activeId == -1)
		{
			for (int i = 0; i < size; i++)
			{
				if (lastAccess < _characterPackages.get(i).getLastAccess())
				{
					lastAccess = _characterPackages.get(i).getLastAccess();
					_activeId = i;
				}
			}
		}
		
		for (int i = 0; i < size; i++)
		{
			final CharacterInfoHolder charInfoPackage = _characterPackages.get(i);
			final int baseClassId = charInfoPackage.getBaseClassId();
			
			buffer.writeString(charInfoPackage.getName()); // Character name
			buffer.writeInt(charInfoPackage.getObjectId()); // Character ID
			buffer.writeString(_loginName); // Account name
			buffer.writeInt(_sessionId); // Account ID
			buffer.writeInt(0); // Clan ID
			buffer.writeInt(0); // Builder level
			buffer.writeInt(charInfoPackage.getSex()); // Sex
			buffer.writeInt(charInfoPackage.getRace()); // Race
			buffer.writeInt(baseClassId);
			buffer.writeInt(ServerConfig.SERVER_ID);
			buffer.writeInt(charInfoPackage.getX());
			buffer.writeInt(charInfoPackage.getY());
			buffer.writeInt(charInfoPackage.getZ());
			buffer.writeDouble(charInfoPackage.getCurrentHp());
			buffer.writeDouble(charInfoPackage.getCurrentMp());
			buffer.writeLong(charInfoPackage.getSp());
			buffer.writeLong(charInfoPackage.getExp());
			buffer.writeDouble((float) (charInfoPackage.getExp() - ExperienceData.getInstance().getExpForLevel(charInfoPackage.getLevel())) / (ExperienceData.getInstance().getExpForLevel(charInfoPackage.getLevel() + 1) - ExperienceData.getInstance().getExpForLevel(charInfoPackage.getLevel())));
			buffer.writeInt(charInfoPackage.getLevel());
			buffer.writeInt(charInfoPackage.getReputation());
			buffer.writeInt(charInfoPackage.getPkKills());
			buffer.writeInt(charInfoPackage.getPvPKills());
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0); // Ertheia
			buffer.writeInt(0); // Ertheia
			for (int slot : getPaperdollOrder())
			{
				buffer.writeInt(charInfoPackage.getPaperdollItemId(slot));
			}
			
			for (int slot : getPaperdollOrderVisualId())
			{
				buffer.writeInt(charInfoPackage.getPaperdollItemVisualId(slot));
			}
			
			buffer.writeShort(charInfoPackage.getEnchantEffect(Inventory.PAPERDOLL_CHEST)); // Upper Body enchant level
			buffer.writeShort(charInfoPackage.getEnchantEffect(Inventory.PAPERDOLL_LEGS)); // Lower Body enchant level
			buffer.writeShort(charInfoPackage.getEnchantEffect(Inventory.PAPERDOLL_HEAD)); // Headgear enchant level
			buffer.writeShort(charInfoPackage.getEnchantEffect(Inventory.PAPERDOLL_GLOVES)); // Gloves enchant level
			buffer.writeShort(charInfoPackage.getEnchantEffect(Inventory.PAPERDOLL_FEET)); // Boots enchant level
			buffer.writeInt(charInfoPackage.getHairStyle());
			buffer.writeInt(charInfoPackage.getHairColor());
			buffer.writeInt(charInfoPackage.getFace());
			buffer.writeDouble(charInfoPackage.getMaxHp()); // Maximum HP
			buffer.writeDouble(charInfoPackage.getMaxMp()); // Maximum MP
			
			if (charInfoPackage.getAccessLevel() > -1)
			{
				// Deleted.
				if (charInfoPackage.getDeleteTimer() > 0)
				{
					buffer.writeInt((int) ((charInfoPackage.getDeleteTimer() - System.currentTimeMillis()) / 1000));
				}
				else // Normal.
				{
					buffer.writeInt(0);
				}
			}
			else // Banned.
			{
				buffer.writeInt(-1);
			}
			
			buffer.writeInt(charInfoPackage.getClassId());
			buffer.writeInt(i == _activeId);
			buffer.writeByte(charInfoPackage.getEnchantEffect(Inventory.PAPERDOLL_RHAND) > 127 ? 127 : charInfoPackage.getEnchantEffect(Inventory.PAPERDOLL_RHAND));
			
			final VariationInstance augment = charInfoPackage.getAugmentation();
			if (augment != null)
			{
				buffer.writeInt(augment.getOption1Id());
				buffer.writeInt(augment.getOption2Id());
				buffer.writeInt(augment.getOption3Id());
			}
			else
			{
				buffer.writeInt(0);
				buffer.writeInt(0);
				buffer.writeInt(0);
			}
			
			buffer.writeInt(0); // Transformation: Currently on retail when you are on character select you don't see your transformation.
			buffer.writeInt(0); // Pet NpcId
			buffer.writeInt(0); // Pet level
			buffer.writeInt(0); // Pet Food
			buffer.writeInt(0); // Pet Food Level
			buffer.writeDouble(0); // Current pet HP
			buffer.writeDouble(0); // Current pet MP
			buffer.writeInt(charInfoPackage.getVitalityPoints()); // Vitality
			buffer.writeInt((int) RatesConfig.RATE_VITALITY_EXP_MULTIPLIER * 100); // Vitality Percent
			buffer.writeInt(charInfoPackage.getVitalityItemsUsed()); // Remaining vitality item uses
			buffer.writeInt(charInfoPackage.getAccessLevel() != -100); // Char is active or not
			buffer.writeByte(charInfoPackage.isNoble());
			buffer.writeByte(Hero.getInstance().isHero(charInfoPackage.getObjectId()) ? 2 : 0); // Hero glow
			buffer.writeByte(charInfoPackage.isHairAccessoryEnabled()); // Show hair accessory if enabled
			buffer.writeInt(0); // 235 - ban time left
			buffer.writeInt((int) (charInfoPackage.getLastAccess() / 1000)); // 235 - last play time
			buffer.writeByte(0); // 338
			
			switch (baseClassId)
			{
				case 247: // Warg 0
				case 248: // Warg 1
				case 249: // Warg 2
				case 250: // Warg 3
				case 251: // Blood Rose 0
				case 252: // Blood Rose 1
				case 253: // Blood Rose 2
				case 254: // Blood Rose 3
				{
					buffer.writeInt(charInfoPackage.getHairColor() - 1);
					break;
				}
				default: // 338 - DK color.
				{
					buffer.writeInt(charInfoPackage.getHairColor() + 1);
					break;
				}
			}
			
			buffer.writeByte(charInfoPackage.getClassId() == 217 ? 1 : charInfoPackage.getClassId() == 218 ? 2 : charInfoPackage.getClassId() == 219 ? 3 : charInfoPackage.getClassId() == 220 ? 4 : 0); // 362
			buffer.writeByte(0); // 464
			buffer.writeByte(0); // 464
			buffer.writeByte(0); // 464
			buffer.writeLong(0); // 493
			buffer.writeLong(0); // 493
			buffer.writeLong(0); // 493
			buffer.writeLong(0); // 493
			buffer.writeInt(0); // 493
			buffer.writeInt(0); // 507
		}
	}
	
	private static List<CharacterInfoHolder> loadCharacterSelectInfo(String loginName)
	{
		CharacterInfoHolder charInfopackage;
		final List<CharacterInfoHolder> characterList = new LinkedList<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM characters WHERE account_name=? ORDER BY createDate"))
		{
			statement.setString(1, loginName);
			try (ResultSet charList = statement.executeQuery())
			{
				while (charList.next()) // fills the package
				{
					charInfopackage = restoreChar(charList);
					if (charInfopackage != null)
					{
						characterList.add(charInfopackage);
						
						// Disconnect offline trader.
						if (OfflineTradeConfig.OFFLINE_DISCONNECT_SAME_ACCOUNT)
						{
							final Player player = World.getInstance().getPlayer(charInfopackage.getObjectId());
							if ((player != null) && player.isInStoreMode())
							{
								Disconnection.of(player).storeAndDelete();
								continue;
							}
						}
						
						// Disconnect offline play.
						if (OfflinePlayConfig.OFFLINE_PLAY_DISCONNECT_SAME_ACCOUNT)
						{
							final Player player = World.getInstance().getPlayer(charInfopackage.getObjectId());
							if ((player != null) && player.isOfflinePlay())
							{
								Disconnection.of(player).storeAndDelete();
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore char info: " + e.getMessage(), e);
		}
		
		return characterList;
	}
	
	private static void loadCharacterSubclassInfo(CharacterInfoHolder charInfopackage, int objectId, int activeClassId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT exp, sp, level, vitality_points FROM character_subclasses WHERE charId=? AND class_id=? ORDER BY charId"))
		{
			statement.setInt(1, objectId);
			statement.setInt(2, activeClassId);
			try (ResultSet charList = statement.executeQuery())
			{
				if (charList.next())
				{
					charInfopackage.setExp(charList.getLong("exp"));
					charInfopackage.setSp(charList.getLong("sp"));
					charInfopackage.setLevel(charList.getInt("level"));
					charInfopackage.setVitalityPoints(charList.getInt("vitality_points"));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore char subclass info: " + e.getMessage(), e);
		}
	}
	
	private static CharacterInfoHolder restoreChar(ResultSet chardata) throws Exception
	{
		final int objectId = chardata.getInt("charId");
		final String name = chardata.getString("char_name");
		
		// See if the char must be deleted
		final long deletetime = chardata.getLong("deletetime");
		if ((deletetime > 0) && (System.currentTimeMillis() > deletetime))
		{
			final Clan clan = ClanTable.getInstance().getClan(chardata.getInt("clanid"));
			if (clan != null)
			{
				clan.removeClanMember(objectId, 0);
			}
			
			GameClient.deleteCharByObjId(objectId);
			return null;
		}
		
		final CharacterInfoHolder charInfopackage = new CharacterInfoHolder(objectId, name);
		charInfopackage.setAccessLevel(chardata.getInt("accesslevel"));
		charInfopackage.setLevel(chardata.getInt("level"));
		charInfopackage.setMaxHp(chardata.getInt("maxhp"));
		charInfopackage.setCurrentHp(chardata.getDouble("curhp"));
		charInfopackage.setMaxMp(chardata.getInt("maxmp"));
		charInfopackage.setCurrentMp(chardata.getDouble("curmp"));
		charInfopackage.setReputation(chardata.getInt("reputation"));
		charInfopackage.setPkKills(chardata.getInt("pkkills"));
		charInfopackage.setPvPKills(chardata.getInt("pvpkills"));
		charInfopackage.setFace(chardata.getInt("face"));
		charInfopackage.setHairStyle(chardata.getInt("hairstyle"));
		charInfopackage.setHairColor(chardata.getInt("haircolor"));
		charInfopackage.setSex(chardata.getInt("sex"));
		charInfopackage.setExp(chardata.getLong("exp"));
		charInfopackage.setSp(chardata.getLong("sp"));
		charInfopackage.setVitalityPoints(chardata.getInt("vitality_points"));
		charInfopackage.setClanId(chardata.getInt("clanid"));
		charInfopackage.setRace(chardata.getInt("race"));
		final int baseClassId = chardata.getInt("base_class");
		final int activeClassId = chardata.getInt("classid");
		charInfopackage.setX(chardata.getInt("x"));
		charInfopackage.setY(chardata.getInt("y"));
		charInfopackage.setZ(chardata.getInt("z"));
		final int faction = chardata.getInt("faction");
		if (faction == 1)
		{
			charInfopackage.setGood();
		}
		
		if (faction == 2)
		{
			charInfopackage.setEvil();
		}
		
		if (MultilingualSupportConfig.MULTILANG_ENABLE)
		{
			String lang = chardata.getString("language");
			if (!MultilingualSupportConfig.MULTILANG_ALLOWED.contains(lang))
			{
				lang = MultilingualSupportConfig.MULTILANG_DEFAULT;
			}
			
			charInfopackage.setHtmlPrefix("data/lang/" + lang + "/");
		}
		
		// if is in subclass, load subclass exp, sp, level info
		if (baseClassId != activeClassId)
		{
			loadCharacterSubclassInfo(charInfopackage, objectId, activeClassId);
		}
		
		charInfopackage.setClassId(activeClassId);
		
		// Get the augmentation id for equipped weapon
		int weaponObjId = charInfopackage.getPaperdollObjectId(Inventory.PAPERDOLL_RHAND);
		if (weaponObjId < 1)
		{
			weaponObjId = charInfopackage.getPaperdollObjectId(Inventory.PAPERDOLL_RHAND);
		}
		
		if (weaponObjId > 0)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement("SELECT mineralId,option1,option2,option3 FROM item_variations WHERE itemId=?"))
			{
				statement.setInt(1, weaponObjId);
				try (ResultSet result = statement.executeQuery())
				{
					if (result.next())
					{
						final int mineralId = result.getInt("mineralId");
						final int option1 = result.getInt("option1");
						final int option2 = result.getInt("option2");
						final int option3 = result.getInt("option3");
						if ((option1 > 0) && (option2 > 0) && (option3 > 0))
						{
							charInfopackage.setAugmentation(new VariationInstance(mineralId, option1, option2, option3));
						}
					}
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Could not restore augmentation info: " + e.getMessage(), e);
			}
		}
		
		// Check if the base class is set to zero and also doesn't match with the current active class, otherwise send the base class ID. This prevents chars created before base class was introduced from being displayed incorrectly.
		if ((baseClassId == 0) && (activeClassId > 0))
		{
			charInfopackage.setBaseClassId(activeClassId);
		}
		else
		{
			charInfopackage.setBaseClassId(baseClassId);
		}
		
		charInfopackage.setDeleteTimer(deletetime);
		charInfopackage.setLastAccess(chardata.getLong("lastAccess"));
		charInfopackage.setNoble(chardata.getInt("nobless") == 1);
		return charInfopackage;
	}
	
	@Override
	public int[] getPaperdollOrder()
	{
		return PAPERDOLL_ORDER;
	}
	
	@Override
	public int[] getPaperdollOrderVisualId()
	{
		return PAPERDOLL_ORDER_VISUAL_ID;
	}
}
