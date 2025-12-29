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
package org.l2jmobius.gameserver.model.itemcontainer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.cache.PaperdollCache;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.TransmogConfig;
import org.l2jmobius.gameserver.data.xml.AgathionData;
import org.l2jmobius.gameserver.data.xml.AppearanceItemData;
import org.l2jmobius.gameserver.data.xml.ArmorSetData;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.model.ArmorSet;
import org.l2jmobius.gameserver.model.VariationInstance;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerItemUnequip;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.appearance.AppearanceStone;
import org.l2jmobius.gameserver.model.item.appearance.AppearanceType;
import org.l2jmobius.gameserver.model.item.enums.BodyPart;
import org.l2jmobius.gameserver.model.item.enums.ItemLocation;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.enums.ItemSkillType;
import org.l2jmobius.gameserver.model.item.holders.AgathionSkillHolder;
import org.l2jmobius.gameserver.model.item.holders.ArmorsetSkillHolder;
import org.l2jmobius.gameserver.model.item.holders.ItemSkillHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.type.ArmorType;
import org.l2jmobius.gameserver.model.item.type.EtcItemType;
import org.l2jmobius.gameserver.model.item.type.WeaponType;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.SkillConditionScope;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.network.enums.InventorySlot;
import org.l2jmobius.gameserver.network.serverpackets.ExUserInfoEquipSlot;
import org.l2jmobius.gameserver.network.serverpackets.SkillCoolTime;

/**
 * This class manages inventory
 * @version $Revision: 1.13.2.9.2.12 $ $Date: 2005/03/29 23:15:15 $ rewritten 23.2.2006 by Advi
 */
public abstract class Inventory extends ItemContainer
{
	protected static final Logger LOGGER = Logger.getLogger(Inventory.class.getName());
	
	public interface PaperdollListener
	{
		void notifyEquiped(int slot, Item inst, Inventory inventory);
		
		void notifyUnequiped(int slot, Item inst, Inventory inventory);
	}
	
	// Common Items
	public static final int ADENA_ID = 57;
	public static final int ANCIENT_ADENA_ID = 5575;
	public static final int BEAUTY_TICKET_ID = 36308;
	public static final int AIR_STONE_ID = 39461;
	public static final int TEMPEST_STONE_ID = 39592;
	public static final int ELCYUM_CRYSTAL_ID = 36514;
	public static final int LCOIN_ID = 91663;
	public static final long MAX_ADENA = PlayerConfig.MAX_ADENA;
	public static final int CLAN_EXP = 94481;
	public static final int SP_POUCH = 101571;
	public static final int SP_POINTS = 15624;
	
	public static final int PAPERDOLL_UNDER = 0;
	public static final int PAPERDOLL_HEAD = 1;
	public static final int PAPERDOLL_HAIR = 2;
	public static final int PAPERDOLL_HAIR2 = 3;
	public static final int PAPERDOLL_NECK = 4;
	public static final int PAPERDOLL_RHAND = 5;
	public static final int PAPERDOLL_CHEST = 6;
	public static final int PAPERDOLL_LHAND = 7;
	public static final int PAPERDOLL_REAR = 8;
	public static final int PAPERDOLL_LEAR = 9;
	public static final int PAPERDOLL_GLOVES = 10;
	public static final int PAPERDOLL_LEGS = 11;
	public static final int PAPERDOLL_FEET = 12;
	public static final int PAPERDOLL_RFINGER = 13;
	public static final int PAPERDOLL_LFINGER = 14;
	public static final int PAPERDOLL_LBRACELET = 15;
	public static final int PAPERDOLL_RBRACELET = 16;
	public static final int PAPERDOLL_AGATHION1 = 17;
	public static final int PAPERDOLL_AGATHION2 = 18;
	public static final int PAPERDOLL_AGATHION3 = 19;
	public static final int PAPERDOLL_AGATHION4 = 20;
	public static final int PAPERDOLL_AGATHION5 = 21;
	public static final int PAPERDOLL_DECO1 = 22;
	public static final int PAPERDOLL_DECO2 = 23;
	public static final int PAPERDOLL_DECO3 = 24;
	public static final int PAPERDOLL_DECO4 = 25;
	public static final int PAPERDOLL_DECO5 = 26;
	public static final int PAPERDOLL_DECO6 = 27;
	public static final int PAPERDOLL_CLOAK = 28;
	public static final int PAPERDOLL_BELT = 29;
	public static final int PAPERDOLL_BROOCH = 30;
	public static final int PAPERDOLL_BROOCH_JEWEL1 = 31;
	public static final int PAPERDOLL_BROOCH_JEWEL2 = 32;
	public static final int PAPERDOLL_BROOCH_JEWEL3 = 33;
	public static final int PAPERDOLL_BROOCH_JEWEL4 = 34;
	public static final int PAPERDOLL_BROOCH_JEWEL5 = 35;
	public static final int PAPERDOLL_BROOCH_JEWEL6 = 36;
	public static final int PAPERDOLL_ARTIFACT_BOOK = 37;
	public static final int PAPERDOLL_ARTIFACT1 = 38; // Artifact Balance
	public static final int PAPERDOLL_ARTIFACT2 = 39; // Artifact Balance
	public static final int PAPERDOLL_ARTIFACT3 = 40; // Artifact Balance
	public static final int PAPERDOLL_ARTIFACT4 = 41; // Artifact Balance
	public static final int PAPERDOLL_ARTIFACT5 = 42; // Artifact Balance
	public static final int PAPERDOLL_ARTIFACT6 = 43; // Artifact Balance
	public static final int PAPERDOLL_ARTIFACT7 = 44; // Artifact Balance
	public static final int PAPERDOLL_ARTIFACT8 = 45; // Artifact Balance
	public static final int PAPERDOLL_ARTIFACT9 = 46; // Artifact Balance
	public static final int PAPERDOLL_ARTIFACT10 = 47; // Artifact Balance
	public static final int PAPERDOLL_ARTIFACT11 = 48; // Artifact Balance
	public static final int PAPERDOLL_ARTIFACT12 = 49; // Artifact Balance
	public static final int PAPERDOLL_ARTIFACT13 = 50; // Artifact Spirit
	public static final int PAPERDOLL_ARTIFACT14 = 51; // Artifact Spirit
	public static final int PAPERDOLL_ARTIFACT15 = 52; // Artifact Spirit
	public static final int PAPERDOLL_ARTIFACT16 = 53; // Artifact Protection
	public static final int PAPERDOLL_ARTIFACT17 = 54; // Artifact Protection
	public static final int PAPERDOLL_ARTIFACT18 = 55; // Artifact Protection
	public static final int PAPERDOLL_ARTIFACT19 = 56; // Artifact Support
	public static final int PAPERDOLL_ARTIFACT20 = 57; // Artifact Support
	public static final int PAPERDOLL_ARTIFACT21 = 58; // Artifact Support
	
	public static final int PAPERDOLL_TOTALSLOTS = 59;
	
	// Speed percentage mods
	public static final double MAX_ARMOR_WEIGHT = 12000;
	
	private final Item[] _paperdoll;
	private final List<PaperdollListener> _paperdollListeners;
	private final PaperdollCache _paperdollCache = new PaperdollCache();
	
	// protected to be accessed from child classes only
	protected int _totalWeight;
	
	// used to quickly check for using of items of special type
	private int _wearedMask;
	
	private long _blockedItemSlotsMask;
	
	// Recorder of alterations in inventory
	private static class ChangeRecorder implements PaperdollListener
	{
		private final Inventory _inventory;
		private final List<Item> _changed = new ArrayList<>(1);
		
		/**
		 * Constructor of the ChangeRecorder
		 * @param inventory
		 */
		ChangeRecorder(Inventory inventory)
		{
			_inventory = inventory;
			_inventory.addPaperdollListener(this);
		}
		
		/**
		 * Add alteration in inventory when item equipped
		 * @param slot
		 * @param item
		 * @param inventory
		 */
		@Override
		public void notifyEquiped(int slot, Item item, Inventory inventory)
		{
			_changed.add(item);
		}
		
		/**
		 * Add alteration in inventory when item unequipped
		 * @param slot
		 * @param item
		 * @param inventory
		 */
		@Override
		public void notifyUnequiped(int slot, Item item, Inventory inventory)
		{
			_changed.add(item);
		}
		
		/**
		 * Returns alterations in inventory
		 * @return Item[] : array of altered items
		 */
		public List<Item> getChangedItems()
		{
			return _changed;
		}
	}
	
	private static class BowCrossRodListener implements PaperdollListener
	{
		private static BowCrossRodListener instance = new BowCrossRodListener();
		
		public static BowCrossRodListener getInstance()
		{
			return instance;
		}
		
		@Override
		public void notifyUnequiped(int slot, Item item, Inventory inventory)
		{
			if ((slot != PAPERDOLL_RHAND) || !item.isWeapon())
			{
				return;
			}
			
			switch (item.getWeaponItem().getItemType())
			{
				case BOW:
				case CROSSBOW:
				case TWOHANDCROSSBOW:
				{
					final Item leftHandItem = inventory.getPaperdollItem(PAPERDOLL_LHAND);
					if (((leftHandItem != null) && ((leftHandItem.getItemType()) != ArmorType.SIGIL)))
					{
						inventory.setPaperdollItem(PAPERDOLL_LHAND, null);
					}
					
					final Player owner = inventory.getOwner().asPlayer();
					if (owner != null)
					{
						owner.removeAmmunitionSkills();
					}
					break;
				}
				case PISTOLS:
				{
					final Player owner = inventory.getOwner().asPlayer();
					if (owner != null)
					{
						owner.removeAmmunitionSkills();
					}
					break;
				}
				case FISHINGROD:
				{
					final Item leftHandItem = inventory.getPaperdollItem(PAPERDOLL_LHAND);
					if (leftHandItem != null)
					{
						inventory.setPaperdollItem(PAPERDOLL_LHAND, null);
					}
					break;
				}
			}
		}
		
		@Override
		public void notifyEquiped(int slot, Item item, Inventory inventory)
		{
		}
	}
	
	private static class StatsListener implements PaperdollListener
	{
		private static StatsListener instance = new StatsListener();
		
		public static StatsListener getInstance()
		{
			return instance;
		}
		
		@Override
		public void notifyUnequiped(int slot, Item item, Inventory inventory)
		{
			inventory.getOwner().getStat().recalculateStats(true);
		}
		
		@Override
		public void notifyEquiped(int slot, Item item, Inventory inventory)
		{
			inventory.getOwner().getStat().recalculateStats(true);
		}
	}
	
	private static class ItemSkillsListener implements PaperdollListener
	{
		private static ItemSkillsListener instance = new ItemSkillsListener();
		
		public static ItemSkillsListener getInstance()
		{
			return instance;
		}
		
		@Override
		public void notifyUnequiped(int slot, Item item, Inventory inventory)
		{
			if (!inventory.getOwner().isPlayable())
			{
				return;
			}
			
			final Playable playable = inventory.getOwner().asPlayable();
			final ItemTemplate it = item.getTemplate();
			final Map<Integer, Skill> addedSkills = new HashMap<>(1);
			final Map<Integer, Skill> removedSkills = new HashMap<>(1);
			boolean update = false;
			boolean updateTimestamp = false;
			
			// Remove augmentation bonuses on unequip
			if (item.isAugmented())
			{
				item.getAugmentation().removeBonus(playable);
			}
			
			// Recalculate all stats
			playable.getStat().recalculateStats(true);
			
			// Clear enchant bonus
			item.clearEnchantStats();
			
			// Clear SA Bonus
			item.clearSpecialAbilities();
			
			if (it.hasSkills())
			{
				// Retain item skill if an item with the same id is still equipped.
				final long remainingItemCount = inventory.getPaperdollItems(equippedItem -> equippedItem.getId() == item.getId()).size();
				if (remainingItemCount == 0)
				{
					final List<ItemSkillHolder> onEnchantSkills = it.getSkills(ItemSkillType.ON_ENCHANT);
					if (onEnchantSkills != null)
					{
						for (ItemSkillHolder holder : onEnchantSkills)
						{
							if (item.getEnchantLevel() < holder.getValue())
							{
								continue;
							}
							
							final Skill skill = holder.getSkill();
							if (skill != null)
							{
								removedSkills.putIfAbsent(skill.getId(), skill);
								update = true;
							}
						}
					}
					
					final List<ItemSkillHolder> normalSkills = it.getSkills(ItemSkillType.NORMAL);
					if (normalSkills != null)
					{
						for (ItemSkillHolder holder : normalSkills)
						{
							final Skill skill = holder.getSkill();
							if (skill != null)
							{
								removedSkills.putIfAbsent(skill.getId(), skill);
								update = true;
							}
						}
					}
				}
				
				if (item.isArmor())
				{
					for (Item itm : inventory.getItems())
					{
						if (!itm.isEquipped() || itm.equals(item))
						{
							continue;
						}
						
						final List<ItemSkillHolder> otherNormalSkills = itm.getTemplate().getSkills(ItemSkillType.NORMAL);
						if (otherNormalSkills == null)
						{
							continue;
						}
						
						for (ItemSkillHolder holder : otherNormalSkills)
						{
							if (playable.getSkillLevel(holder.getSkillId()) != 0)
							{
								continue;
							}
							
							final Skill skill = holder.getSkill();
							if (skill == null)
							{
								continue;
							}
							
							final Skill existingSkill = addedSkills.get(skill.getId());
							if (existingSkill != null)
							{
								if (existingSkill.getLevel() < skill.getLevel())
								{
									addedSkills.put(skill.getId(), skill);
								}
							}
							else
							{
								addedSkills.put(skill.getId(), skill);
							}
							
							if (skill.isActive() && !playable.hasSkillReuse(skill.getReuseHashCode()))
							{
								final int equipDelay = item.getEquipReuseDelay();
								if (equipDelay > 0)
								{
									playable.addTimeStamp(skill, equipDelay);
									playable.disableSkill(skill, equipDelay);
								}
								
								updateTimestamp = true;
							}
							
							update = true;
						}
					}
				}
			}
			
			// Must check all equipped items for enchant conditions.
			for (Item equipped : inventory.getPaperdollItems())
			{
				if (!equipped.getTemplate().hasSkills())
				{
					continue;
				}
				
				final List<ItemSkillHolder> otherEnchantSkills = equipped.getTemplate().getSkills(ItemSkillType.ON_ENCHANT);
				final List<ItemSkillHolder> otherBlessingSkills = equipped.getTemplate().getSkills(ItemSkillType.ON_BLESSING);
				if ((otherEnchantSkills == null) && (otherBlessingSkills == null))
				{
					continue;
				}
				
				if (otherEnchantSkills != null)
				{
					for (ItemSkillHolder holder : otherEnchantSkills)
					{
						if (equipped.getEnchantLevel() < holder.getValue())
						{
							continue;
						}
						
						final Skill skill = holder.getSkill();
						if (skill == null)
						{
							continue;
						}
						
						// Check passive skill conditions.
						if (skill.isPassive() && !skill.checkConditions(SkillConditionScope.PASSIVE, playable, playable))
						{
							removedSkills.putIfAbsent(skill.getId(), skill);
							update = true;
						}
					}
				}
				
				if ((otherBlessingSkills != null) && equipped.isBlessed())
				{
					for (ItemSkillHolder holder : otherBlessingSkills)
					{
						final Skill skill = holder.getSkill();
						if (skill == null)
						{
							continue;
						}
						
						// Check passive skill conditions.
						if (skill.isPassive() && !skill.checkConditions(SkillConditionScope.PASSIVE, playable, playable))
						{
							removedSkills.putIfAbsent(skill.getId(), skill);
							update = true;
						}
					}
				}
			}
			
			// Must check for toggle and isRemovedOnUnequipWeapon skill item conditions.
			for (Skill skill : playable.getAllSkills())
			{
				if ((skill.isToggle() && playable.isAffectedBySkill(skill.getId()) && !skill.checkConditions(SkillConditionScope.GENERAL, playable, playable)) //
					|| (it.isWeapon() && skill.isRemovedOnUnequipWeapon()))
				{
					playable.stopSkillEffects(SkillFinishType.REMOVED, skill.getId());
					update = true;
				}
			}
			
			// Apply skill, if item has "skills on unequip" and it is not a secondary agathion.
			if ((slot < PAPERDOLL_AGATHION2) || (slot > PAPERDOLL_AGATHION5))
			{
				it.forEachSkill(ItemSkillType.ON_UNEQUIP, holder -> holder.getSkill().activateSkill(playable, playable));
			}
			
			if (update)
			{
				for (Skill skill : removedSkills.values())
				{
					playable.removeSkill(skill, skill.isPassive());
				}
				
				for (Skill skill : addedSkills.values())
				{
					playable.addSkill(skill);
				}
				
				if (playable.isPlayer())
				{
					playable.asPlayer().sendSkillList();
				}
			}
			
			if (updateTimestamp && playable.isPlayer())
			{
				playable.sendPacket(new SkillCoolTime(playable.asPlayer()));
			}
			
			if (item.isWeapon())
			{
				playable.unchargeAllShots();
			}
		}
		
		@Override
		public void notifyEquiped(int slot, Item item, Inventory inventory)
		{
			if (!inventory.getOwner().isPlayable())
			{
				return;
			}
			
			final Playable playable = inventory.getOwner().asPlayable();
			final Map<Integer, Skill> addedSkills = new HashMap<>(1);
			boolean updateTimestamp = false;
			
			// Apply augmentation bonuses on equip
			if (item.isAugmented())
			{
				item.getAugmentation().applyBonus(playable);
			}
			
			// Recalculate all stats
			playable.getStat().recalculateStats(true);
			
			// Apply enchant stats
			item.applyEnchantStats();
			
			// Apply SA skill
			item.applySpecialAbilities();
			
			if (item.getTemplate().hasSkills())
			{
				final List<ItemSkillHolder> onEnchantSkills = item.getTemplate().getSkills(ItemSkillType.ON_ENCHANT);
				if (onEnchantSkills != null)
				{
					for (ItemSkillHolder holder : onEnchantSkills)
					{
						if (playable.getSkillLevel(holder.getSkillId()) >= holder.getSkillLevel())
						{
							continue;
						}
						
						if (item.getEnchantLevel() < holder.getValue())
						{
							continue;
						}
						
						final Skill skill = holder.getSkill();
						if (skill == null)
						{
							continue;
						}
						
						// Check passive skill conditions.
						if (skill.isPassive() && !skill.checkConditions(SkillConditionScope.PASSIVE, playable, playable))
						{
							continue;
						}
						
						final Skill existingSkill = addedSkills.get(skill.getId());
						if (existingSkill != null)
						{
							if (existingSkill.getLevel() < skill.getLevel())
							{
								addedSkills.put(skill.getId(), skill);
							}
						}
						else
						{
							addedSkills.put(skill.getId(), skill);
						}
						
						// Active, non offensive, skills start with reuse on equip.
						if (skill.isActive() && !skill.hasNegativeEffect() && !skill.isTransformation() && (PlayerConfig.ITEM_EQUIP_ACTIVE_SKILL_REUSE > 0) && playable.asPlayer().hasEnteredWorld())
						{
							playable.addTimeStamp(skill, skill.getReuseDelay() > 0 ? skill.getReuseDelay() : PlayerConfig.ITEM_EQUIP_ACTIVE_SKILL_REUSE);
							updateTimestamp = true;
						}
					}
				}
				
				if (item.isBlessed())
				{
					final List<ItemSkillHolder> onBlessingSkills = item.getTemplate().getSkills(ItemSkillType.ON_BLESSING);
					if (onBlessingSkills != null)
					{
						for (ItemSkillHolder holder : onBlessingSkills)
						{
							if (playable.getSkillLevel(holder.getSkillId()) >= holder.getSkillLevel())
							{
								continue;
							}
							
							if (item.getEnchantLevel() < holder.getValue())
							{
								continue;
							}
							
							final Skill skill = holder.getSkill();
							if (skill == null)
							{
								continue;
							}
							
							// Check passive skill conditions.
							if (skill.isPassive() && !skill.checkConditions(SkillConditionScope.PASSIVE, playable, playable))
							{
								continue;
							}
							
							final Skill existingSkill = addedSkills.get(skill.getId());
							if (existingSkill != null)
							{
								if (existingSkill.getLevel() < skill.getLevel())
								{
									addedSkills.put(skill.getId(), skill);
								}
							}
							else
							{
								addedSkills.put(skill.getId(), skill);
							}
						}
					}
				}
				
				final List<ItemSkillHolder> normalSkills = item.getTemplate().getSkills(ItemSkillType.NORMAL);
				if (normalSkills != null)
				{
					for (ItemSkillHolder holder : normalSkills)
					{
						if (playable.getSkillLevel(holder.getSkillId()) >= holder.getSkillLevel())
						{
							continue;
						}
						
						final Skill skill = holder.getSkill();
						if (skill == null)
						{
							continue;
						}
						
						// Check passive skill conditions.
						if (skill.isPassive() && !skill.checkConditions(SkillConditionScope.PASSIVE, playable, playable))
						{
							continue;
						}
						
						final Skill existingSkill = addedSkills.get(skill.getId());
						if (existingSkill != null)
						{
							if (existingSkill.getLevel() < skill.getLevel())
							{
								addedSkills.put(skill.getId(), skill);
							}
						}
						else
						{
							addedSkills.put(skill.getId(), skill);
						}
						
						if (skill.isActive())
						{
							if (!playable.hasSkillReuse(skill.getReuseHashCode()))
							{
								final int equipDelay = item.getEquipReuseDelay();
								if (equipDelay > 0)
								{
									playable.addTimeStamp(skill, equipDelay);
									playable.disableSkill(skill, equipDelay);
								}
							}
							
							// Active, non offensive, skills start with reuse on equip.
							if (!skill.hasNegativeEffect() && !skill.isTransformation() && (PlayerConfig.ITEM_EQUIP_ACTIVE_SKILL_REUSE > 0) && playable.asPlayer().hasEnteredWorld())
							{
								playable.addTimeStamp(skill, skill.getReuseDelay() > 0 ? skill.getReuseDelay() : PlayerConfig.ITEM_EQUIP_ACTIVE_SKILL_REUSE);
							}
							
							updateTimestamp = true;
						}
					}
				}
			}
			
			// Must check all equipped items for enchant conditions.
			for (Item equipped : inventory.getPaperdollItems())
			{
				if (!equipped.getTemplate().hasSkills())
				{
					continue;
				}
				
				final List<ItemSkillHolder> otherEnchantSkills = equipped.getTemplate().getSkills(ItemSkillType.ON_ENCHANT);
				final List<ItemSkillHolder> otherBlessingSkills = equipped.getTemplate().getSkills(ItemSkillType.ON_BLESSING);
				if ((otherEnchantSkills == null) && (otherBlessingSkills == null))
				{
					continue;
				}
				
				if (otherEnchantSkills != null)
				{
					for (ItemSkillHolder holder : otherEnchantSkills)
					{
						if (playable.getSkillLevel(holder.getSkillId()) >= holder.getSkillLevel())
						{
							continue;
						}
						
						if (equipped.getEnchantLevel() < holder.getValue())
						{
							continue;
						}
						
						final Skill skill = holder.getSkill();
						if (skill == null)
						{
							continue;
						}
						
						// Check passive skill conditions.
						if (skill.isPassive() && !skill.checkConditions(SkillConditionScope.PASSIVE, playable, playable))
						{
							continue;
						}
						
						final Skill existingSkill = addedSkills.get(skill.getId());
						if (existingSkill != null)
						{
							if (existingSkill.getLevel() < skill.getLevel())
							{
								addedSkills.put(skill.getId(), skill);
							}
						}
						else
						{
							addedSkills.put(skill.getId(), skill);
						}
						
						// Active, non offensive, skills start with reuse on equip.
						if (skill.isActive() && !skill.hasNegativeEffect() && !skill.isTransformation() && (PlayerConfig.ITEM_EQUIP_ACTIVE_SKILL_REUSE > 0) && playable.asPlayer().hasEnteredWorld())
						{
							playable.addTimeStamp(skill, skill.getReuseDelay() > 0 ? skill.getReuseDelay() : PlayerConfig.ITEM_EQUIP_ACTIVE_SKILL_REUSE);
							updateTimestamp = true;
						}
					}
				}
				
				if (otherBlessingSkills != null)
				{
					for (ItemSkillHolder holder : otherBlessingSkills)
					{
						if (playable.getSkillLevel(holder.getSkillId()) >= holder.getSkillLevel())
						{
							continue;
						}
						
						if (equipped.isBlessed())
						{
							final Skill skill = holder.getSkill();
							if (skill == null)
							{
								continue;
							}
							
							// Check passive skill conditions.
							if (skill.isPassive() && !skill.checkConditions(SkillConditionScope.PASSIVE, playable, playable))
							{
								continue;
							}
							
							final Skill existingSkill = addedSkills.get(skill.getId());
							if (existingSkill != null)
							{
								if (existingSkill.getLevel() < skill.getLevel())
								{
									addedSkills.put(skill.getId(), skill);
								}
							}
							else
							{
								addedSkills.put(skill.getId(), skill);
							}
						}
					}
				}
			}
			
			// Apply skill, if item has "skills on equip" and it is not a secondary agathion.
			if ((slot < PAPERDOLL_AGATHION2) || (slot > PAPERDOLL_AGATHION5))
			{
				item.getTemplate().forEachSkill(ItemSkillType.ON_EQUIP, holder -> holder.getSkill().activateSkill(playable, playable));
			}
			
			if (!addedSkills.isEmpty())
			{
				for (Skill skill : addedSkills.values())
				{
					playable.addSkill(skill);
				}
				
				if (playable.isPlayer())
				{
					playable.asPlayer().sendSkillList();
				}
			}
			
			if (updateTimestamp && playable.isPlayer())
			{
				playable.sendPacket(new SkillCoolTime(playable.asPlayer()));
			}
		}
	}
	
	private static class ArmorSetListener implements PaperdollListener
	{
		private static ArmorSetListener instance = new ArmorSetListener();
		
		public static ArmorSetListener getInstance()
		{
			return instance;
		}
		
		@Override
		public void notifyEquiped(int slot, Item item, Inventory inventory)
		{
			if (!inventory.getOwner().isPlayable())
			{
				return;
			}
			
			final Playable playable = inventory.getOwner().asPlayable();
			boolean update = false;
			
			// Verify and apply normal set
			if (verifyAndApply(playable, item, Item::getId))
			{
				update = true;
			}
			
			// Verify and apply visual set
			final int itemVisualId = item.getVisualId();
			if (itemVisualId > 0)
			{
				final int appearanceStoneId = item.getAppearanceStoneId();
				final AppearanceStone stone = AppearanceItemData.getInstance().getStone(appearanceStoneId > 0 ? appearanceStoneId : itemVisualId);
				if ((stone != null) && (stone.getType() == AppearanceType.FIXED) && verifyAndApply(playable, item, Item::getVisualId))
				{
					update = true;
				}
			}
			
			if (playable.isPlayer())
			{
				if (update)
				{
					playable.asPlayer().sendSkillList();
				}
				
				if ((item.getTemplate().getBodyPart() == BodyPart.BROOCH_JEWEL) || (item.getTemplate().getBodyPart() == BodyPart.BROOCH))
				{
					playable.asPlayer().updateActiveBroochJewel();
				}
			}
		}
		
		private static boolean applySkills(Playable playable, Item item, ArmorSet armorSet, Function<Item, Integer> idProvider)
		{
			final long piecesCount = armorSet.getPieceCount(playable, idProvider);
			if (piecesCount >= armorSet.getMinimumPieces())
			{
				// Applying all skills that matching the conditions
				boolean updateTimeStamp = false;
				boolean update = false;
				for (ArmorsetSkillHolder holder : armorSet.getSkills())
				{
					if (playable.getSkillLevel(holder.getSkillId()) >= holder.getSkillLevel())
					{
						continue;
					}
					
					if (holder.validateConditions(playable, armorSet, idProvider))
					{
						final Skill itemSkill = holder.getSkill();
						if (itemSkill == null)
						{
							LOGGER.warning("Inventory.ArmorSetListener.addSkills: Incorrect skill: " + holder);
							continue;
						}
						
						if (itemSkill.isPassive() && !itemSkill.checkConditions(SkillConditionScope.PASSIVE, playable, playable))
						{
							continue;
						}
						
						playable.addSkill(itemSkill);
						if (itemSkill.isActive())
						{
							if ((item != null) && !playable.hasSkillReuse(itemSkill.getReuseHashCode()))
							{
								final int equipDelay = item.getEquipReuseDelay();
								if (equipDelay > 0)
								{
									playable.addTimeStamp(itemSkill, equipDelay);
									playable.disableSkill(itemSkill, equipDelay);
								}
							}
							
							// Active, non offensive, skills start with reuse on equip.
							if (!itemSkill.hasNegativeEffect() && !itemSkill.isTransformation() && (PlayerConfig.ARMOR_SET_EQUIP_ACTIVE_SKILL_REUSE > 0) && playable.asPlayer().hasEnteredWorld())
							{
								playable.addTimeStamp(itemSkill, itemSkill.getReuseDelay() > 0 ? itemSkill.getReuseDelay() : PlayerConfig.ARMOR_SET_EQUIP_ACTIVE_SKILL_REUSE);
							}
							
							updateTimeStamp = true;
						}
						
						update = true;
					}
				}
				
				if (updateTimeStamp && playable.isPlayer())
				{
					playable.sendPacket(new SkillCoolTime(playable.asPlayer()));
				}
				
				return update;
			}
			
			return false;
		}
		
		private static boolean verifyAndApply(Playable playable, Item item, Function<Item, Integer> idProvider)
		{
			boolean update = false;
			final List<ArmorSet> armorSets = ArmorSetData.getInstance().getSets(idProvider.apply(item));
			for (ArmorSet armorSet : armorSets)
			{
				if (applySkills(playable, item, armorSet, idProvider))
				{
					update = true;
				}
			}
			
			return update;
		}
		
		private static boolean verifyAndRemove(Playable playable, Item item, Function<Item, Integer> idProvider)
		{
			boolean update = false;
			final List<ArmorSet> armorSets = ArmorSetData.getInstance().getSets(idProvider.apply(item));
			for (ArmorSet armorSet : armorSets)
			{
				// Remove all skills that doesn't matches the conditions
				for (ArmorsetSkillHolder holder : armorSet.getSkills())
				{
					if (!holder.validateConditions(playable, armorSet, idProvider))
					{
						final Skill itemSkill = holder.getSkill();
						if (itemSkill == null)
						{
							LOGGER.warning("Inventory.ArmorSetListener.removeSkills: Incorrect skill: " + holder);
							continue;
						}
						
						// Update if a skill has been removed.
						if (playable.removeSkill(itemSkill, itemSkill.isPassive()) != null)
						{
							update = true;
						}
					}
				}
				
				// Attempt to apply lower level skills if possible
				if (applySkills(playable, item, armorSet, idProvider))
				{
					update = true;
				}
			}
			
			return update;
		}
		
		@Override
		public void notifyUnequiped(int slot, Item item, Inventory inventory)
		{
			if (!inventory.getOwner().isPlayable())
			{
				return;
			}
			
			final Playable playable = inventory.getOwner().asPlayable();
			boolean remove = false;
			
			// Verify and remove normal set bonus
			if (verifyAndRemove(playable, item, Item::getId))
			{
				remove = true;
			}
			
			// Verify and remove visual set bonus
			final int itemVisualId = item.getVisualId();
			if (itemVisualId > 0)
			{
				final int appearanceStoneId = item.getAppearanceStoneId();
				final AppearanceStone stone = AppearanceItemData.getInstance().getStone(appearanceStoneId > 0 ? appearanceStoneId : itemVisualId);
				if ((stone != null) && (stone.getType() == AppearanceType.FIXED) && verifyAndRemove(playable, item, Item::getVisualId))
				{
					remove = true;
				}
			}
			
			if (!playable.isPlayer())
			{
				return;
			}
			
			if (remove)
			{
				final Player player = playable.asPlayer();
				player.checkItemRestriction();
				player.sendSkillList();
			}
			
			if ((item.getTemplate().getBodyPart() == BodyPart.BROOCH_JEWEL) || (item.getTemplate().getBodyPart() == BodyPart.BROOCH))
			{
				playable.asPlayer().updateActiveBroochJewel();
			}
		}
	}
	
	private static class BraceletListener implements PaperdollListener
	{
		private static BraceletListener instance = new BraceletListener();
		
		public static BraceletListener getInstance()
		{
			return instance;
		}
		
		@Override
		public void notifyUnequiped(int slot, Item item, Inventory inventory)
		{
			final Player player = item.asPlayer();
			if ((player != null) && player.isChangingClass())
			{
				return;
			}
			
			if (item.getTemplate().getBodyPart() == BodyPart.R_BRACELET)
			{
				inventory.unEquipItemInSlot(PAPERDOLL_DECO1);
				inventory.unEquipItemInSlot(PAPERDOLL_DECO2);
				inventory.unEquipItemInSlot(PAPERDOLL_DECO3);
				inventory.unEquipItemInSlot(PAPERDOLL_DECO4);
				inventory.unEquipItemInSlot(PAPERDOLL_DECO5);
				inventory.unEquipItemInSlot(PAPERDOLL_DECO6);
			}
		}
		
		// Note (April 3, 2009): Currently on equip, talismans do not display properly, do we need checks here to fix this?
		@Override
		public void notifyEquiped(int slot, Item item, Inventory inventory)
		{
		}
	}
	
	private static class BroochListener implements PaperdollListener
	{
		private static BroochListener instance = new BroochListener();
		
		public static BroochListener getInstance()
		{
			return instance;
		}
		
		@Override
		public void notifyUnequiped(int slot, Item item, Inventory inventory)
		{
			final Player player = item.asPlayer();
			if ((player != null) && player.isChangingClass())
			{
				return;
			}
			
			if (item.getTemplate().getBodyPart() == BodyPart.BROOCH)
			{
				inventory.unEquipItemInSlot(PAPERDOLL_BROOCH_JEWEL1);
				inventory.unEquipItemInSlot(PAPERDOLL_BROOCH_JEWEL2);
				inventory.unEquipItemInSlot(PAPERDOLL_BROOCH_JEWEL3);
				inventory.unEquipItemInSlot(PAPERDOLL_BROOCH_JEWEL4);
				inventory.unEquipItemInSlot(PAPERDOLL_BROOCH_JEWEL5);
				inventory.unEquipItemInSlot(PAPERDOLL_BROOCH_JEWEL6);
			}
		}
		
		// Note (April 3, 2009): Currently on equip, talismans do not display properly, do we need checks here to fix this?
		@Override
		public void notifyEquiped(int slot, Item item, Inventory inventory)
		{
		}
	}
	
	private static class AgathionBraceletListener implements PaperdollListener
	{
		private static AgathionBraceletListener instance = new AgathionBraceletListener();
		
		public static AgathionBraceletListener getInstance()
		{
			return instance;
		}
		
		@Override
		public void notifyUnequiped(int slot, Item item, Inventory inventory)
		{
			final Player player = item.asPlayer();
			if ((player != null) && player.isChangingClass())
			{
				return;
			}
			
			if (item.getTemplate().getBodyPart() == BodyPart.L_BRACELET)
			{
				inventory.unEquipItemInSlot(PAPERDOLL_AGATHION1);
				inventory.unEquipItemInSlot(PAPERDOLL_AGATHION2);
				inventory.unEquipItemInSlot(PAPERDOLL_AGATHION3);
				inventory.unEquipItemInSlot(PAPERDOLL_AGATHION4);
				inventory.unEquipItemInSlot(PAPERDOLL_AGATHION5);
			}
		}
		
		@Override
		public void notifyEquiped(int slot, Item item, Inventory inventory)
		{
		}
	}
	
	private static class ArtifactBookListener implements PaperdollListener
	{
		private static ArtifactBookListener instance = new ArtifactBookListener();
		
		public static ArtifactBookListener getInstance()
		{
			return instance;
		}
		
		@Override
		public void notifyUnequiped(int slot, Item item, Inventory inventory)
		{
			final Player player = item.asPlayer();
			if ((player != null) && player.isChangingClass())
			{
				return;
			}
			
			if (item.getTemplate().getBodyPart() == BodyPart.ARTIFACT_BOOK)
			{
				inventory.unEquipItemInSlot(PAPERDOLL_ARTIFACT1);
				inventory.unEquipItemInSlot(PAPERDOLL_ARTIFACT2);
				inventory.unEquipItemInSlot(PAPERDOLL_ARTIFACT3);
				inventory.unEquipItemInSlot(PAPERDOLL_ARTIFACT4);
				inventory.unEquipItemInSlot(PAPERDOLL_ARTIFACT5);
				inventory.unEquipItemInSlot(PAPERDOLL_ARTIFACT6);
				inventory.unEquipItemInSlot(PAPERDOLL_ARTIFACT7);
				inventory.unEquipItemInSlot(PAPERDOLL_ARTIFACT8);
				inventory.unEquipItemInSlot(PAPERDOLL_ARTIFACT9);
				inventory.unEquipItemInSlot(PAPERDOLL_ARTIFACT10);
				inventory.unEquipItemInSlot(PAPERDOLL_ARTIFACT11);
				inventory.unEquipItemInSlot(PAPERDOLL_ARTIFACT12);
				inventory.unEquipItemInSlot(PAPERDOLL_ARTIFACT13);
				inventory.unEquipItemInSlot(PAPERDOLL_ARTIFACT14);
				inventory.unEquipItemInSlot(PAPERDOLL_ARTIFACT15);
				inventory.unEquipItemInSlot(PAPERDOLL_ARTIFACT16);
				inventory.unEquipItemInSlot(PAPERDOLL_ARTIFACT17);
				inventory.unEquipItemInSlot(PAPERDOLL_ARTIFACT18);
				inventory.unEquipItemInSlot(PAPERDOLL_ARTIFACT19);
				inventory.unEquipItemInSlot(PAPERDOLL_ARTIFACT20);
				inventory.unEquipItemInSlot(PAPERDOLL_ARTIFACT21);
			}
		}
		
		@Override
		public void notifyEquiped(int slot, Item item, Inventory inventory)
		{
		}
	}
	
	/**
	 * Constructor of the inventory
	 */
	protected Inventory()
	{
		_paperdoll = new Item[PAPERDOLL_TOTALSLOTS];
		_paperdollListeners = new ArrayList<>();
		
		if (this instanceof PlayerInventory)
		{
			addPaperdollListener(ArmorSetListener.getInstance());
			addPaperdollListener(BowCrossRodListener.getInstance());
			addPaperdollListener(ItemSkillsListener.getInstance());
			addPaperdollListener(BraceletListener.getInstance());
			addPaperdollListener(BroochListener.getInstance());
			addPaperdollListener(AgathionBraceletListener.getInstance());
			addPaperdollListener(ArtifactBookListener.getInstance());
		}
		else if (this instanceof PetInventory)
		{
			addPaperdollListener(ArmorSetListener.getInstance());
			addPaperdollListener(ItemSkillsListener.getInstance());
		}
		
		// common
		addPaperdollListener(StatsListener.getInstance());
	}
	
	protected abstract ItemLocation getEquipLocation();
	
	/**
	 * Returns the instance of new ChangeRecorder
	 * @return ChangeRecorder
	 */
	private ChangeRecorder newRecorder()
	{
		return new ChangeRecorder(this);
	}
	
	/**
	 * Drop item from inventory and updates database
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param item : Item to be dropped
	 * @param actor : Player Player requesting the item drop
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return Item corresponding to the destroyed item or the updated item in inventory
	 */
	public Item dropItem(ItemProcessType process, Item item, Player actor, Object reference)
	{
		if (item == null)
		{
			return null;
		}
		
		synchronized (item)
		{
			if (!_items.contains(item))
			{
				return null;
			}
			
			removeItem(item);
			item.setOwnerId(process, 0, actor, reference);
			item.setItemLocation(ItemLocation.VOID);
			item.setLastChange(Item.REMOVED);
			
			item.updateDatabase();
			refreshWeight();
		}
		
		return item;
	}
	
	/**
	 * Drop item from inventory by using its <b>objectID</b> and updates database
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be dropped
	 * @param count : int Quantity of items to be dropped
	 * @param actor : Player Player requesting the item drop
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return Item corresponding to the destroyed item or the updated item in inventory
	 */
	public Item dropItem(ItemProcessType process, int objectId, long count, Player actor, Object reference)
	{
		Item item = getItemByObjectId(objectId);
		if (item == null)
		{
			return null;
		}
		
		synchronized (item)
		{
			if (!_items.contains(item))
			{
				return null;
			}
			
			// Adjust item quantity and create new instance to drop
			// Directly drop entire item
			if (item.getCount() > count)
			{
				item.changeCount(process, -count, actor, reference);
				item.setLastChange(Item.MODIFIED);
				item.updateDatabase();
				
				final Item newItem = ItemManager.createItem(process, item.getId(), count, actor, reference);
				newItem.updateDatabase();
				refreshWeight();
				return newItem;
			}
		}
		
		return dropItem(process, item, actor, reference);
	}
	
	/**
	 * Adds item to inventory for further adjustments and Equip it if necessary (itemlocation defined)
	 * @param item : Item to be added from inventory
	 */
	@Override
	protected void addItem(Item item)
	{
		super.addItem(item);
		if (item.isEquipped())
		{
			equipItem(item);
		}
	}
	
	/**
	 * Removes item from inventory for further adjustments.
	 * @param item : Item to be removed from inventory
	 */
	@Override
	protected boolean removeItem(Item item)
	{
		// Unequip item if equiped
		for (int i = 0; i < _paperdoll.length; i++)
		{
			if (_paperdoll[i] == item)
			{
				unEquipItemInSlot(i);
			}
		}
		
		return super.removeItem(item);
	}
	
	/**
	 * @param slot the slot.
	 * @return the item in the paperdoll slot
	 */
	public Item getPaperdollItem(int slot)
	{
		return _paperdoll[slot];
	}
	
	/**
	 * @param slot the slot.
	 * @return {@code true} if specified paperdoll slot is empty, {@code false} otherwise
	 */
	public boolean isPaperdollSlotEmpty(int slot)
	{
		return _paperdoll[slot] == null;
	}
	
	public boolean isPaperdollSlotNotEmpty(int slot)
	{
		return _paperdoll[slot] != null;
	}
	
	public boolean isItemEquipped(int itemId)
	{
		for (Item item : _paperdoll)
		{
			if ((item != null) && (item.getId() == itemId))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public static int getPaperdollIndex(BodyPart bodyPart)
	{
		if (bodyPart == null)
		{
			return -1;
		}
		
		return bodyPart.getPaperdollSlot();
	}
	
	/**
	 * Gets the paperdoll item in the specified body part slot.
	 * @param bodyPart the body part
	 * @return the paperdoll item, or {@code null} if none is equipped
	 */
	public Item getPaperdollItemByBodyPart(BodyPart bodyPart)
	{
		final int index = getPaperdollIndex(bodyPart);
		if (index == -1)
		{
			return null;
		}
		
		return _paperdoll[index];
	}
	
	/**
	 * Returns the first paperdoll item with the specific id
	 * @param itemId the item id
	 * @return Item
	 */
	public Item getPaperdollItemByItemId(int itemId)
	{
		for (int i = 0; i < _paperdoll.length; i++)
		{
			final Item item = _paperdoll[i];
			if ((item != null) && (item.getId() == itemId))
			{
				return item;
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the ID of the item in the paperdoll slot
	 * @param slot : int designating the slot
	 * @return int designating the ID of the item
	 */
	public int getPaperdollItemId(int slot)
	{
		final Item item = _paperdoll[slot];
		if (item != null)
		{
			if (TransmogConfig.ENABLE_TRANSMOG)
			{
				final int transmogId = item.getTransmogId();
				if (transmogId > 0)
				{
					return transmogId;
				}
			}
			
			return item.getId();
		}
		
		return 0;
	}
	
	/**
	 * Returns the ID of the item in the paperdoll slot
	 * @param slot : int designating the slot
	 * @return int designating the ID of the item
	 */
	public int getPaperdollItemDisplayId(int slot)
	{
		final Item item = _paperdoll[slot];
		if (item != null)
		{
			if (TransmogConfig.ENABLE_TRANSMOG)
			{
				final int transmogId = item.getTransmogId();
				if (transmogId > 0)
				{
					return transmogId;
				}
			}
			if ((item.getTemplate().getBodyPart() == BodyPart.BACK) && ((item.getId() == 97452) || (item.getId() == 99504)) && (item.getEnchantLevel() == 5))
			{
				return 103634;
			}
			if ((item.getTemplate().getBodyPart() == BodyPart.BACK) && ((item.getId() == 97452) || (item.getId() == 99504)) && (item.getEnchantLevel() == 6))
			{
				return 103635;
			}
			if ((item.getTemplate().getBodyPart() == BodyPart.BACK) && ((item.getId() == 97452) || (item.getId() == 99504)) && (item.getEnchantLevel() == 7))
			{
				return 103636;
			}
			if ((item.getTemplate().getBodyPart() == BodyPart.BACK) && ((item.getId() == 97452) || (item.getId() == 99504)) && (item.getEnchantLevel() == 8))
			{
				return 103637;
			}
			return item.getDisplayId();
		}
		
		return 0;
	}
	
	/**
	 * Returns the visual id of the item in the paperdoll slot
	 * @param slot : int designating the slot
	 * @return int designating the ID of the item
	 */
	public int getPaperdollItemVisualId(int slot)
	{
		final Item item = _paperdoll[slot];

		if (getOwner().isPlayer())
		{
			final Player player = getOwner().asPlayer();

			// 武器外觀
			if (slot == InventorySlot.RHAND.getSlot())
			{
				return player.getWeaponShiftedDisplayId();
			}

			// 防具外觀(胸甲)
			if (slot == InventorySlot.CHEST.getSlot())
			{
				return player.getChestShiftedDisplayId();
			}
		}

		return (item != null) ? item.getVisualId() : 0;
	}
	
	public VariationInstance getPaperdollAugmentation(int slot)
	{
		final Item item = _paperdoll[slot];
		return (item != null) ? item.getAugmentation() : null;
	}
	
	/**
	 * Returns the objectID associated to the item in the paperdoll slot
	 * @param slot : int pointing out the slot
	 * @return int designating the objectID
	 */
	public int getPaperdollObjectId(int slot)
	{
		final Item item = _paperdoll[slot];
		return (item != null) ? item.getObjectId() : 0;
	}
	
	/**
	 * Adds new inventory's paperdoll listener.
	 * @param listener the new listener
	 */
	public synchronized void addPaperdollListener(PaperdollListener listener)
	{
		if (!_paperdollListeners.contains(listener))
		{
			_paperdollListeners.add(listener);
		}
	}
	
	/**
	 * Removes a paperdoll listener.
	 * @param listener the listener to be deleted
	 */
	public synchronized void removePaperdollListener(PaperdollListener listener)
	{
		_paperdollListeners.remove(listener);
	}
	
	/**
	 * Equips an item in the given slot of the paperdoll.<br>
	 * <u><i>Remark :</i></u> The item <b>must be</b> in the inventory already.
	 * @param slot : int pointing out the slot of the paperdoll
	 * @param item : Item pointing out the item to add in slot
	 * @return Item designating the item placed in the slot before
	 */
	public synchronized Item setPaperdollItem(int slot, Item item)
	{
		final Creature owner = getOwner();
		final Item old = _paperdoll[slot];
		if (old != item)
		{
			if (old != null)
			{
				if (owner.isPlayer())
				{
					owner.asPlayer().getCombatPower().removeItemCombatPower(old);
				}
				
				_paperdoll[slot] = null;
				_paperdollCache.getPaperdollItems().remove(old);
				
				// Put old item from paperdoll slot to base location
				old.setItemLocation(getBaseLocation());
				old.setLastChange(Item.MODIFIED);
				
				// Get the mask for paperdoll
				int mask = 0;
				for (int i = 0; i < PAPERDOLL_TOTALSLOTS; i++)
				{
					final Item pi = _paperdoll[i];
					if (pi != null)
					{
						mask |= pi.getTemplate().getItemMask();
					}
				}
				
				_wearedMask = mask;
				
				// Notify all paperdoll listener in order to unequip old item in slot
				for (PaperdollListener listener : _paperdollListeners)
				{
					if (listener == null)
					{
						continue;
					}
					
					listener.notifyUnequiped(slot, old, this);
				}
				
				old.updateDatabase();
				
				// Remove agathion skills.
				if ((slot >= PAPERDOLL_AGATHION1) && (slot <= PAPERDOLL_AGATHION5) && owner.isPlayer())
				{
					final AgathionSkillHolder agathionSkills = AgathionData.getInstance().getSkills(old.getId());
					if (agathionSkills != null)
					{
						boolean update = false;
						final Player player = owner.asPlayer();
						for (Skill skill : agathionSkills.getMainSkills(old.getEnchantLevel()))
						{
							player.removeSkill(skill, false, skill.isPassive());
							update = true;
						}
						
						for (Skill skill : agathionSkills.getSubSkills(old.getEnchantLevel()))
						{
							player.removeSkill(skill, false, skill.isPassive());
							update = true;
						}
						
						if (update)
						{
							player.sendSkillList();
						}
					}
				}
			}
			
			// Add new item in slot of paperdoll
			if (item != null)
			{
				if (owner.isPlayer())
				{
					owner.asPlayer().getCombatPower().addItemCombatPower(item);
				}
				
				_paperdoll[slot] = item;
				_paperdollCache.getPaperdollItems().add(item);
				
				// Put item to equip location
				item.setItemLocation(getEquipLocation(), slot);
				item.setLastChange(Item.MODIFIED);
				
				// Notify all paperdoll listener in order to equip item in slot
				_wearedMask |= item.getTemplate().getItemMask();
				for (PaperdollListener listener : _paperdollListeners)
				{
					if (listener == null)
					{
						continue;
					}
					
					listener.notifyEquiped(slot, item, this);
				}
				
				item.updateDatabase();
				
				// Add agathion skills.
				if ((slot >= PAPERDOLL_AGATHION1) && (slot <= PAPERDOLL_AGATHION5) && owner.isPlayer())
				{
					final AgathionSkillHolder agathionSkills = AgathionData.getInstance().getSkills(item.getId());
					if (agathionSkills != null)
					{
						boolean update = false;
						final Player player = owner.asPlayer();
						if (slot == PAPERDOLL_AGATHION1)
						{
							for (Skill skill : agathionSkills.getMainSkills(item.getEnchantLevel()))
							{
								if (skill.isPassive() && !skill.checkConditions(SkillConditionScope.PASSIVE, player, player))
								{
									continue;
								}
								
								player.addSkill(skill, false);
								update = true;
							}
						}
						
						for (Skill skill : agathionSkills.getSubSkills(item.getEnchantLevel()))
						{
							if (skill.isPassive() && !skill.checkConditions(SkillConditionScope.PASSIVE, player, player))
							{
								continue;
							}
							
							player.addSkill(skill, false);
							update = true;
						}
						
						if (update)
						{
							player.sendSkillList();
						}
					}
				}
			}
			
			_paperdollCache.clearCachedStats();
			owner.getStat().recalculateStats(!owner.isPlayer());
			
			if (owner.isPlayer())
			{
				owner.sendPacket(new ExUserInfoEquipSlot(owner.asPlayer()));
			}
		}
		
		if (old != null)
		{
			if ((owner != null) && owner.isPlayer())
			{
				// Proper talisman display on login.
				final Player player = owner.asPlayer();
				if ((slot == PAPERDOLL_RBRACELET) && !player.hasEnteredWorld())
				{
					for (ItemSkillHolder skill : old.getTemplate().getAllSkills())
					{
						player.addSkill(skill.getSkill(), false);
					}
				}
				
				// Notify to scripts.
				if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_UNEQUIP, old.getTemplate()))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemUnequip(player, old), old.getTemplate());
				}
			}
		}
		
		return old;
	}
	
	/**
	 * @return the mask of wore item
	 */
	public int getWearedMask()
	{
		return _wearedMask;
	}
	
	/**
	 * Unequips item in body slot and returns alterations.<br>
	 * <b>If you do not need return value use {@link Inventory#unEquipItemInBodySlot(BodyPart)} instead</b>
	 * @param bodyPart : the body part enum
	 * @return List<Item> : List of changes
	 */
	public List<Item> unEquipItemInBodySlotAndRecord(BodyPart bodyPart)
	{
		final ChangeRecorder recorder = newRecorder();
		try
		{
			unEquipItemInBodySlot(bodyPart);
		}
		finally
		{
			removePaperdollListener(recorder);
		}
		
		return recorder.getChangedItems();
	}
	
	/**
	 * Sets item in slot of the paperdoll to null value
	 * @param pdollSlot : int designating the slot
	 * @return Item designating the item in slot before change
	 */
	public Item unEquipItemInSlot(int pdollSlot)
	{
		return setPaperdollItem(pdollSlot, null);
	}
	
	/**
	 * Unequips item in slot and returns alterations<br>
	 * <b>If you do not need return value use {@link Inventory#unEquipItemInSlot(int)} instead</b>
	 * @param slot : int designating the slot
	 * @return List<Item> : List of items altered
	 */
	public List<Item> unEquipItemInSlotAndRecord(int slot)
	{
		final ChangeRecorder recorder = newRecorder();
		try
		{
			unEquipItemInSlot(slot);
		}
		finally
		{
			removePaperdollListener(recorder);
		}
		
		return recorder.getChangedItems();
	}
	
	/**
	 * Unequips item in body slot
	 * @param bodyPart the body part enum
	 * @return {@link Item} designating the item placed in the slot
	 */
	public Item unEquipItemInBodySlot(BodyPart bodyPart)
	{
		// Special handling for HAIRALL.
		if (bodyPart == BodyPart.HAIRALL)
		{
			setPaperdollItem(PAPERDOLL_HAIR, null);
		}
		
		final int pdollSlot = BodyPart.getPaperdollIndex(bodyPart);
		if (pdollSlot >= 0)
		{
			return setPaperdollItem(pdollSlot, null);
		}
		
		return null;
	}
	
	/**
	 * Equips item and returns list of alterations<br>
	 * <b>If you don't need return value use {@link Inventory#equipItem(Item)} instead</b>
	 * @param item : Item corresponding to the item
	 * @return List<Item> : List of alterations
	 */
	public List<Item> equipItemAndRecord(Item item)
	{
		final ChangeRecorder recorder = newRecorder();
		try
		{
			equipItem(item);
		}
		finally
		{
			removePaperdollListener(recorder);
		}
		
		return recorder.getChangedItems();
	}
	
	/**
	 * Equips item in slot of paperdoll.
	 * @param item : Item designating the item and slot used.
	 */
	public void equipItem(Item item)
	{
		if (getOwner().isPlayer())
		{
			if (getOwner().asPlayer().isInStoreMode())
			{
				return;
			}
			
			final Player player = getOwner().asPlayer();
			if (!player.isGM() && !player.isHero() && item.isHeroItem())
			{
				return;
			}
		}
		
		final BodyPart bodyPart = item.getTemplate().getBodyPart();
		
		// Check if player is using Formal Wear and item isn't Wedding Bouquet.
		final Item formal = getPaperdollItem(PAPERDOLL_CHEST);
		if ((item.getId() != 21163 /* Wedding Bouquet */) && (formal != null) && (formal.getTemplate().getBodyPart() == BodyPart.ALLDRESS))
		{
			// Only chest target can pass this.
			switch (bodyPart)
			{
				case LR_HAND:
				case L_HAND:
				case R_HAND:
				case LEGS:
				case FEET:
				case GLOVES:
				case HEAD:
				{
					return;
				}
			}
		}
		
		// Handle special cases.
		switch (bodyPart)
		{
			case LR_HAND:
			{
				final Item lh = getPaperdollItem(PAPERDOLL_LHAND);
				if ((lh != null) && lh.isArmor() && (lh.getArmorItem().getItemType() == ArmorType.SHIELD))
				{
					setPaperdollItem(PAPERDOLL_LHAND, null);
				}
				
				setPaperdollItem(PAPERDOLL_RHAND, item);
				break;
			}
			case L_HAND:
			{
				final Item rh = getPaperdollItem(PAPERDOLL_RHAND);
				if ((rh != null) && (rh.getTemplate().getBodyPart() == BodyPart.LR_HAND) && !((rh.getItemType() == WeaponType.FISHINGROD) && (item.getItemType() == EtcItemType.LURE)))
				{
					if (!item.isArmor() || (item.getArmorItem().getItemType() != ArmorType.SIGIL))
					{
						setPaperdollItem(PAPERDOLL_RHAND, null);
					}
				}
				
				setPaperdollItem(PAPERDOLL_LHAND, item);
				break;
			}
			case R_HAND:
			{
				setPaperdollItem(PAPERDOLL_RHAND, item);
				break;
			}
			case L_EAR:
			case R_EAR:
			case LR_EAR:
			{
				if (_paperdoll[PAPERDOLL_LEAR] == null)
				{
					setPaperdollItem(PAPERDOLL_LEAR, item);
				}
				else if (_paperdoll[PAPERDOLL_REAR] == null)
				{
					setPaperdollItem(PAPERDOLL_REAR, item);
				}
				else
				{
					setPaperdollItem(PAPERDOLL_LEAR, item);
				}
				break;
			}
			case L_FINGER:
			case R_FINGER:
			case LR_FINGER:
			{
				if (_paperdoll[PAPERDOLL_LFINGER] == null)
				{
					setPaperdollItem(PAPERDOLL_LFINGER, item);
				}
				else if (_paperdoll[PAPERDOLL_RFINGER] == null)
				{
					setPaperdollItem(PAPERDOLL_RFINGER, item);
				}
				else
				{
					setPaperdollItem(PAPERDOLL_LFINGER, item);
				}
				break;
			}
			case NECK:
			{
				setPaperdollItem(PAPERDOLL_NECK, item);
				break;
			}
			case FULL_ARMOR:
			{
				setPaperdollItem(PAPERDOLL_LEGS, null);
				setPaperdollItem(PAPERDOLL_CHEST, item);
				break;
			}
			case CHEST:
			{
				setPaperdollItem(PAPERDOLL_CHEST, item);
				break;
			}
			case LEGS:
			{
				final Item chest = getPaperdollItem(PAPERDOLL_CHEST);
				if ((chest != null) && (chest.getTemplate().getBodyPart() == BodyPart.FULL_ARMOR))
				{
					setPaperdollItem(PAPERDOLL_CHEST, null);
				}
				
				setPaperdollItem(PAPERDOLL_LEGS, item);
				break;
			}
			case FEET:
			{
				setPaperdollItem(PAPERDOLL_FEET, item);
				break;
			}
			case GLOVES:
			{
				setPaperdollItem(PAPERDOLL_GLOVES, item);
				break;
			}
			case HEAD:
			{
				setPaperdollItem(PAPERDOLL_HEAD, item);
				break;
			}
			case HAIR:
			{
				final Item hair = getPaperdollItem(PAPERDOLL_HAIR);
				if ((hair != null) && (hair.getTemplate().getBodyPart() == BodyPart.HAIRALL))
				{
					setPaperdollItem(PAPERDOLL_HAIR2, null);
				}
				else
				{
					setPaperdollItem(PAPERDOLL_HAIR, null);
				}
				
				setPaperdollItem(PAPERDOLL_HAIR, item);
				break;
			}
			case HAIR2:
			{
				final Item hair2 = getPaperdollItem(PAPERDOLL_HAIR);
				if ((hair2 != null) && (hair2.getTemplate().getBodyPart() == BodyPart.HAIRALL))
				{
					setPaperdollItem(PAPERDOLL_HAIR, null);
				}
				else
				{
					setPaperdollItem(PAPERDOLL_HAIR2, null);
				}
				
				setPaperdollItem(PAPERDOLL_HAIR2, item);
				break;
			}
			case HAIRALL:
			{
				setPaperdollItem(PAPERDOLL_HAIR2, null);
				setPaperdollItem(PAPERDOLL_HAIR, item);
				break;
			}
			case UNDERWEAR:
			{
				setPaperdollItem(PAPERDOLL_UNDER, item);
				break;
			}
			case BACK:
			{
				setPaperdollItem(PAPERDOLL_CLOAK, item);
				break;
			}
			case L_BRACELET:
			{
				setPaperdollItem(PAPERDOLL_LBRACELET, item);
				break;
			}
			case R_BRACELET:
			{
				setPaperdollItem(PAPERDOLL_RBRACELET, item);
				break;
			}
			case DECO:
			{
				equipTalisman(item);
				break;
			}
			case BELT:
			{
				setPaperdollItem(PAPERDOLL_BELT, item);
				break;
			}
			case ALLDRESS:
			{
				setPaperdollItem(PAPERDOLL_LEGS, null);
				setPaperdollItem(PAPERDOLL_LHAND, null);
				setPaperdollItem(PAPERDOLL_RHAND, null);
				setPaperdollItem(PAPERDOLL_HEAD, null);
				setPaperdollItem(PAPERDOLL_FEET, null);
				setPaperdollItem(PAPERDOLL_GLOVES, null);
				setPaperdollItem(PAPERDOLL_CHEST, item);
				break;
			}
			case BROOCH:
			{
				setPaperdollItem(PAPERDOLL_BROOCH, item);
				break;
			}
			case BROOCH_JEWEL:
			{
				equipBroochJewel(item);
				break;
			}
			case AGATHION:
			{
				equipAgathion(item);
				break;
			}
			case ARTIFACT_BOOK:
			{
				setPaperdollItem(PAPERDOLL_ARTIFACT_BOOK, item);
				break;
			}
			case ARTIFACT:
			{
				equipArtifact(item);
				break;
			}
			default:
			{
				LOGGER.warning("Unknown body slot " + bodyPart + " for Item ID: " + item.getId());
				break;
			}
		}
	}
	
	/**
	 * Refresh the weight of equipment loaded
	 */
	@Override
	protected void refreshWeight()
	{
		long weight = 0;
		for (Item item : _items)
		{
			if ((item != null) && (item.getTemplate() != null))
			{
				weight += item.getTemplate().getWeight() * item.getCount();
			}
		}
		
		_totalWeight = (int) Math.min(weight, Integer.MAX_VALUE);
	}
	
	/**
	 * @return the totalWeight.
	 */
	public int getTotalWeight()
	{
		return _totalWeight;
	}
	
	/**
	 * Reduce the arrow number of the Creature.<br>
	 * <br>
	 * <b><u>Overridden in</u>:</b>
	 * <li>Player</li><br>
	 * @param type
	 */
	public void reduceAmmunitionCount(EtcItemType type)
	{
		// Default is to do nothing.
	}
	
	/**
	 * Return the Item of the arrows needed for this bow.
	 * @param bow : Item designating the bow
	 * @return Item pointing out arrows for bow
	 */
	public Item findArrowForBow(ItemTemplate bow)
	{
		if (bow == null)
		{
			return null;
		}
		
		Item arrow = null;
		for (Item item : _items)
		{
			if (item.isEtcItem() && (item.getEtcItem().getItemType() == EtcItemType.ARROW) && (item.getTemplate().getCrystalTypePlus() == bow.getCrystalTypePlus()))
			{
				arrow = item;
				break;
			}
		}
		
		// Get the Item corresponding to the item identifier and return it
		return arrow;
	}
	
	/**
	 * Return the Item of the bolts needed for this crossbow.
	 * @param crossbow : Item designating the crossbow
	 * @return Item pointing out bolts for crossbow
	 */
	public Item findBoltForCrossBow(ItemTemplate crossbow)
	{
		Item bolt = null;
		for (Item item : _items)
		{
			if (item.isEtcItem() && (item.getEtcItem().getItemType() == EtcItemType.BOLT) && (item.getTemplate().getCrystalTypePlus() == crossbow.getCrystalTypePlus()))
			{
				bolt = item;
				break;
			}
		}
		
		// Get the Item corresponding to the item identifier and return it
		return bolt;
	}
	
	/**
	 * Return the Item of the bolts needed for these pistols.
	 * @param pistols : Item designating the pistols
	 * @return Item pointing out elemental orb for pistols
	 */
	public Item findElementalOrbForPistols(ItemTemplate pistols)
	{
		Item orb = null;
		for (Item item : _items)
		{
			if (item.isEtcItem() && (item.getEtcItem().getItemType() == EtcItemType.ELEMENTAL_ORB) && (item.getTemplate().getCrystalTypePlus() == pistols.getCrystalTypePlus()))
			{
				orb = item;
				break;
			}
		}
		
		// Get the Item corresponding to the item identifier and return it
		return orb;
	}
	
	/**
	 * Get back items in inventory from database
	 */
	@Override
	public void restore()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM items WHERE owner_id=? AND (loc=? OR loc=?) ORDER BY loc_data"))
		{
			ps.setInt(1, getOwnerId());
			ps.setString(2, getBaseLocation().name());
			ps.setString(3, getEquipLocation().name());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					try
					{
						final Item item = new Item(rs);
						if (getOwner().isPlayer())
						{
							final Player player = getOwner().asPlayer();
							if (!player.isGM() && !player.isHero() && item.isHeroItem())
							{
								item.setItemLocation(ItemLocation.INVENTORY);
							}
						}
						
						World.getInstance().addObject(item);
						
						// If stackable item is found in inventory just add to current quantity
						if (item.isStackable() && (getItemByItemId(item.getId()) != null))
						{
							addItem(ItemProcessType.RESTORE, item, getOwner().asPlayer(), null);
						}
						else
						{
							addItem(item);
						}
					}
					catch (Exception e)
					{
						LOGGER.warning("Could not restore item " + rs.getInt("item_id") + " for " + getOwner());
					}
				}
			}
			
			refreshWeight();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore inventory: " + e.getMessage(), e);
		}
	}
	
	public int getTalismanSlots()
	{
		return getOwner().asPlayer().getStat().getTalismanSlots();
	}
	
	private void equipTalisman(Item item)
	{
		if (getTalismanSlots() == 0)
		{
			return;
		}
		
		// find same (or incompatible) talisman type
		for (int i = PAPERDOLL_DECO1; i < (PAPERDOLL_DECO1 + getTalismanSlots()); i++)
		{
			if ((_paperdoll[i] != null) && (getPaperdollItemId(i) == item.getId()))
			{
				// overwrite
				setPaperdollItem(i, item);
				return;
			}
		}
		
		// no free slot found - put on first free
		for (int i = PAPERDOLL_DECO1; i < (PAPERDOLL_DECO1 + getTalismanSlots()); i++)
		{
			if (_paperdoll[i] == null)
			{
				setPaperdollItem(i, item);
				return;
			}
		}
	}
	
	public int getArtifactSlots()
	{
		return getOwner().asPlayer().getStat().getArtifactSlots();
	}
	
	private void equipArtifact(Item item)
	{
		final int artifactSlots = getArtifactSlots();
		if (artifactSlots == 0)
		{
			return;
		}
		
		final int locationSlot = item.getLocationSlot();
		if ((locationSlot >= PAPERDOLL_ARTIFACT1) && (locationSlot <= PAPERDOLL_ARTIFACT21) && (BodyPart.fromPaperdollSlot(locationSlot) == null))
		{
			setPaperdollItem(locationSlot, item);
			item.setItemLocation(ItemLocation.PAPERDOLL, locationSlot);
		}
		else
		{
			switch (item.getTemplate().getArtifactSlot())
			{
				case 1: // Attack
				{
					for (int slot = PAPERDOLL_ARTIFACT13; slot < (PAPERDOLL_ARTIFACT13 + artifactSlots); slot++)
					{
						if ((slot <= PAPERDOLL_ARTIFACT15) && (_paperdoll[slot] == null))
						{
							setPaperdollItem(slot, item);
							item.setItemLocation(ItemLocation.PAPERDOLL, slot);
							return;
						}
					}
					break;
				}
				case 2: // Protection
				{
					for (int slot = PAPERDOLL_ARTIFACT16; slot < (PAPERDOLL_ARTIFACT16 + artifactSlots); slot++)
					{
						if ((slot <= PAPERDOLL_ARTIFACT18) && (_paperdoll[slot] == null))
						{
							setPaperdollItem(slot, item);
							item.setItemLocation(ItemLocation.PAPERDOLL, slot);
							return;
						}
					}
					break;
				}
				case 3: // Support
				{
					for (int slot = PAPERDOLL_ARTIFACT19; slot < (PAPERDOLL_ARTIFACT19 + artifactSlots); slot++)
					{
						if ((slot <= PAPERDOLL_ARTIFACT21) && (_paperdoll[slot] == null))
						{
							setPaperdollItem(slot, item);
							item.setItemLocation(ItemLocation.PAPERDOLL, slot);
							return;
						}
					}
					break;
				}
				case 4: // Balance
				{
					for (int slot = PAPERDOLL_ARTIFACT1; slot < (PAPERDOLL_ARTIFACT1 + (4 * artifactSlots)); slot++)
					{
						if ((slot <= PAPERDOLL_ARTIFACT12) && (_paperdoll[slot] == null))
						{
							setPaperdollItem(slot, item);
							item.setItemLocation(ItemLocation.PAPERDOLL, slot);
							return;
						}
					}
					break;
				}
			}
		}
	}
	
	public int getBroochJewelSlots()
	{
		return getOwner().asPlayer().getStat().getBroochJewelSlots();
	}
	
	private void equipBroochJewel(Item item)
	{
		if (getBroochJewelSlots() == 0)
		{
			return;
		}
		
		// find same (or incompatible) brooch jewel type
		for (int i = PAPERDOLL_BROOCH_JEWEL1; i < (PAPERDOLL_BROOCH_JEWEL1 + getBroochJewelSlots()); i++)
		{
			if ((_paperdoll[i] != null) && (getPaperdollItemId(i) == item.getId()))
			{
				// overwrite
				setPaperdollItem(i, item);
				return;
			}
		}
		
		// no free slot found - put on first free
		for (int i = PAPERDOLL_BROOCH_JEWEL1; i < (PAPERDOLL_BROOCH_JEWEL1 + getBroochJewelSlots()); i++)
		{
			if (_paperdoll[i] == null)
			{
				setPaperdollItem(i, item);
				return;
			}
		}
	}
	
	public int getAgathionSlots()
	{
		return getOwner().asPlayer().getStat().getAgathionSlots();
	}
	
	private void equipAgathion(Item item)
	{
		if (getAgathionSlots() == 0)
		{
			return;
		}
		
		// find same (or incompatible) agathion type
		for (int i = PAPERDOLL_AGATHION1; i < (PAPERDOLL_AGATHION1 + getAgathionSlots()); i++)
		{
			if ((_paperdoll[i] != null) && (getPaperdollItemId(i) == item.getId()))
			{
				// overwrite
				setPaperdollItem(i, item);
				return;
			}
		}
		
		// no free slot found - put on first free
		for (int i = PAPERDOLL_AGATHION1; i < (PAPERDOLL_AGATHION1 + getAgathionSlots()); i++)
		{
			if (_paperdoll[i] == null)
			{
				setPaperdollItem(i, item);
				return;
			}
		}
	}
	
	public boolean canEquipCloak()
	{
		return getOwner().asPlayer().getStat().canEquipCloak();
	}
	
	/**
	 * Re-notify to paperdoll listeners every equipped item.<br>
	 * Only used by player ClassId set methods.
	 */
	public void reloadEquippedItems()
	{
		int slot;
		for (Item item : _paperdoll)
		{
			if (item == null)
			{
				continue;
			}
			
			slot = item.getLocationSlot();
			for (PaperdollListener listener : _paperdollListeners)
			{
				if (listener == null)
				{
					continue;
				}
				
				listener.notifyUnequiped(slot, item, this);
				listener.notifyEquiped(slot, item, this);
			}
		}
		
		if (getOwner().isPlayer())
		{
			getOwner().sendPacket(new ExUserInfoEquipSlot(getOwner().asPlayer()));
		}
	}
	
	public int getArmorSetEnchant()
	{
		final Creature creature = getOwner();
		if ((creature == null) || !creature.isPlayable())
		{
			return 0;
		}
		
		return _paperdollCache.getArmorSetEnchant(creature.asPlayable());
	}
	
	public int getWeaponEnchant()
	{
		final Item item = getPaperdollItem(PAPERDOLL_RHAND);
		return item != null ? item.getEnchantLevel() : 0;
	}
	
	/**
	 * Blocks the given item slot from being equipped.
	 * @param bodyPart from Item
	 */
	public void blockItemSlot(BodyPart bodyPart)
	{
		_blockedItemSlotsMask |= bodyPart.getMask();
	}
	
	/**
	 * Unblocks the given item slot so it can be equipped.
	 * @param bodyPart from Item
	 */
	public void unblockItemSlot(BodyPart bodyPart)
	{
		_blockedItemSlotsMask &= ~bodyPart.getMask();
	}
	
	/**
	 * @param bodyPart from Item
	 * @return if the given item slot is blocked or not.
	 */
	public boolean isItemSlotBlocked(BodyPart bodyPart)
	{
		final long bodyPartValue = bodyPart.getMask();
		return (_blockedItemSlotsMask & bodyPartValue) == bodyPartValue;
	}
	
	/**
	 * @param bodyPart from Item. Use BodyPart.NONE to unset all blocked item slots.
	 */
	public void setBlockedItemSlotsMask(BodyPart bodyPart)
	{
		_blockedItemSlotsMask = bodyPart.getMask();
	}
	
	/**
	 * Gets the items in paperdoll slots filtered by filter.
	 * @param filters multiple filters
	 * @return the filtered items in inventory
	 */
	@SafeVarargs
	public final Collection<Item> getPaperdollItems(Predicate<Item>... filters)
	{
		if (filters.length == 0)
		{
			return _paperdollCache.getPaperdollItems();
		}
		
		Predicate<Item> filter = Objects::nonNull;
		for (Predicate<Item> additionalFilter : filters)
		{
			filter = filter.and(additionalFilter);
		}
		
		final List<Item> items = new LinkedList<>();
		for (Item item : _paperdoll)
		{
			if (filter.test(item))
			{
				items.add(item);
			}
		}
		
		return items;
	}
	
	@SafeVarargs
	public final int getPaperdollItemCount(Predicate<Item>... filters)
	{
		if (filters.length == 0)
		{
			return _paperdollCache.getPaperdollItems().size();
		}
		
		Predicate<Item> filter = Objects::nonNull;
		for (Predicate<Item> additionalFilter : filters)
		{
			filter = filter.and(additionalFilter);
		}
		
		int count = 0;
		for (Item item : _paperdoll)
		{
			if (filter.test(item))
			{
				count++;
			}
		}
		
		return count;
	}
	
	public PaperdollCache getPaperdollCache()
	{
		return _paperdollCache;
	}
}
