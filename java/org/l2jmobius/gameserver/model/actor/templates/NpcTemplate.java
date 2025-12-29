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
package org.l2jmobius.gameserver.model.actor.templates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.ChampionMonstersConfig;
import org.l2jmobius.gameserver.config.custom.NpcStatMultipliersConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.npc.AISkillScope;
import org.l2jmobius.gameserver.model.actor.enums.npc.AIType;
import org.l2jmobius.gameserver.model.actor.enums.npc.DropType;
import org.l2jmobius.gameserver.model.actor.enums.npc.MpRewardAffectType;
import org.l2jmobius.gameserver.model.actor.enums.npc.MpRewardType;
import org.l2jmobius.gameserver.model.actor.enums.player.ElementalSpiritType;
import org.l2jmobius.gameserver.model.actor.enums.player.Sex;
import org.l2jmobius.gameserver.model.actor.holders.npc.DropGroupHolder;
import org.l2jmobius.gameserver.model.actor.holders.npc.DropHolder;
import org.l2jmobius.gameserver.model.actor.holders.npc.FakePlayerHolder;
import org.l2jmobius.gameserver.model.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.Stat;

/**
 * @author NosBit, Nobius
 */
public class NpcTemplate extends CreatureTemplate
{
	private static final Logger LOGGER = Logger.getLogger(NpcTemplate.class.getName());
	
	private int _id;
	private int _displayId;
	private int _level;
	private String _type;
	private String _name;
	private boolean _usingServerSideName;
	private String _title;
	private boolean _usingServerSideTitle;
	private StatSet _parameters;
	private Sex _sex;
	private int _chestId;
	private int _rhandId;
	private int _lhandId;
	private int _weaponEnchant;
	private double _exp;
	private double _sp;
	private double _raidPoints;
	private boolean _unique;
	private boolean _attackable;
	private boolean _targetable;
	private boolean _talkable;
	private boolean _isQuestMonster;
	private boolean _undying;
	private boolean _showName;
	private boolean _randomWalk;
	private boolean _randomAnimation;
	private boolean _flying;
	private boolean _fakePlayer;
	private FakePlayerHolder _fakePlayerInfo;
	private boolean _canMove;
	private boolean _noSleepMode;
	private boolean _passableDoor;
	private boolean _hasSummoner;
	private boolean _canBeSown;
	private boolean _canBeCrt;
	private boolean _isDeathPenalty;
	private int _corpseTime;
	private AIType _aiType;
	private int _aggroRange;
	private int _clanHelpRange;
	private boolean _isChaos;
	private boolean _isAggressive;
	private int _soulShot;
	private int _spiritShot;
	private int _soulShotChance;
	private int _spiritShotChance;
	private int _minSkillChance;
	private int _maxSkillChance;
	private double _hitTimeFactor;
	private double _hitTimeFactorSkill;
	private int _baseAttackAngle;
	private Map<Integer, Skill> _skills;
	private Map<AISkillScope, List<Skill>> _aiSkillLists;
	private Set<Integer> _clans;
	private Set<Integer> _ignoreClanNpcIds;
	private List<DropGroupHolder> _dropGroups;
	private List<DropHolder> _dropListDeath;
	private List<DropHolder> _dropListSpoil;
	private List<DropHolder> _dropListFortune;
	private float _collisionRadiusGrown;
	private float _collisionHeightGrown;
	private int _mpRewardValue;
	private MpRewardType _mpRewardType;
	private int _mpRewardTicks;
	private MpRewardAffectType _mpRewardAffectType;
	private ElementalSpiritType _elementalType;
	private long _attributeExp;
	
	/**
	 * Constructor of Creature.
	 * @param set The StatSet object to transfer data to the method
	 */
	public NpcTemplate(StatSet set)
	{
		super(set);
	}
	
	@Override
	public void set(StatSet set)
	{
		super.set(set);
		_id = set.getInt("id");
		_displayId = set.getInt("displayId", _id);
		_level = set.getInt("level", 85);
		_type = set.getString("type", "Folk");
		_name = set.getString("name", "");
		_usingServerSideName = set.getBoolean("usingServerSideName", false);
		_title = set.getString("title", "");
		_usingServerSideTitle = set.getBoolean("usingServerSideTitle", false);
		setRace(set.getEnum("race", Race.class, Race.NONE));
		_sex = set.getEnum("sex", Sex.class, Sex.ETC);
		_elementalType = set.getEnum("elementalType", ElementalSpiritType.class, ElementalSpiritType.NONE);
		_chestId = set.getInt("chestId", 0);
		if ((_chestId > 0) && (ItemData.getInstance().getTemplate(_chestId) == null))
		{
			LOGGER.warning("NpcTemplate " + _id + ": Could not find item for chestId with id " + _chestId + ".");
		}
		
		_rhandId = set.getInt("rhandId", 0);
		if ((_rhandId > 0) && (ItemData.getInstance().getTemplate(_rhandId) == null))
		{
			LOGGER.warning("NpcTemplate " + _id + ": Could not find item for rhandId with id " + _rhandId + ".");
		}
		
		_lhandId = set.getInt("lhandId", 0);
		if ((_lhandId > 0) && (ItemData.getInstance().getTemplate(_lhandId) == null))
		{
			LOGGER.warning("NpcTemplate " + _id + ": Could not find item for lhandId with id " + _lhandId + ".");
		}
		
		_weaponEnchant = set.getInt("weaponEnchant", 0);
		_exp = set.getDouble("exp", 0);
		_sp = set.getDouble("sp", 0);
		_raidPoints = set.getDouble("raidPoints", 0);
		_attributeExp = set.getLong("attributeExp", 0);
		_unique = set.getBoolean("unique", false);
		_attackable = set.getBoolean("attackable", true);
		_targetable = set.getBoolean("targetable", true);
		_talkable = set.getBoolean("talkable", true);
		_isQuestMonster = _title.contains("Quest");
		_undying = set.getBoolean("undying", !_type.equals("Monster") && !_type.equals("RaidBoss") && !_type.equals("GrandBoss"));
		_showName = set.getBoolean("showName", true);
		_randomWalk = set.getBoolean("randomWalk", !_type.equals("Guard"));
		_randomAnimation = set.getBoolean("randomAnimation", true);
		_flying = set.getBoolean("flying", false);
		_fakePlayer = set.getBoolean("fakePlayer", false);
		if (_fakePlayer)
		{
			_fakePlayerInfo = new FakePlayerHolder(set);
			
			// Check if a character with the same name already exists.
			if (CharInfoTable.getInstance().getIdByName(_name) > 0)
			{
				LOGGER.info(getClass().getSimpleName() + ": Fake player id [" + _id + "] conflict. A real player with name [" + _name + "] already exists.");
			}
		}
		
		_canMove = (set.getDouble("baseWalkSpd", 1d) <= 0.1) || set.getBoolean("canMove", true);
		_noSleepMode = set.getBoolean("noSleepMode", false);
		_passableDoor = set.getBoolean("passableDoor", false);
		_hasSummoner = set.getBoolean("hasSummoner", false);
		_canBeSown = set.getBoolean("canBeSown", false);
		_canBeCrt = set.getBoolean("exCrtEffect", true);
		_isDeathPenalty = set.getBoolean("isDeathPenalty", false);
		_corpseTime = set.getInt("corpseTime", NpcConfig.DEFAULT_CORPSE_TIME);
		_aiType = set.getEnum("aiType", AIType.class, AIType.FIGHTER);
		_aggroRange = set.getInt("aggroRange", 0);
		_clanHelpRange = set.getInt("clanHelpRange", 0);
		_isChaos = set.getBoolean("isChaos", false);
		_isAggressive = set.getBoolean("isAggressive", false);
		_soulShot = set.getInt("soulShot", 0);
		_spiritShot = set.getInt("spiritShot", 0);
		_soulShotChance = set.getInt("soulShotChance", 0);
		_spiritShotChance = set.getInt("spiritShotChance", 0);
		_minSkillChance = set.getInt("minSkillChance", 7);
		_maxSkillChance = set.getInt("maxSkillChance", 15);
		_hitTimeFactor = set.getInt("hitTime", 100) / 100d;
		_hitTimeFactorSkill = set.getInt("hitTimeSkill", 100) / 100d;
		_baseAttackAngle = set.getInt("width", 120);
		_collisionRadiusGrown = set.getFloat("collisionRadiusGrown", 0);
		_collisionHeightGrown = set.getFloat("collisionHeightGrown", 0);
		_mpRewardValue = set.getInt("mpRewardValue", 0);
		_mpRewardType = set.getEnum("mpRewardType", MpRewardType.class, MpRewardType.DIFF);
		_mpRewardTicks = set.getInt("mpRewardTicks", 0);
		_mpRewardAffectType = set.getEnum("mpRewardAffectType", MpRewardAffectType.class, MpRewardAffectType.SOLO);
		if (NpcStatMultipliersConfig.ENABLE_NPC_STAT_MULTIPLIERS) // Custom NPC Stat Multipliers
		{
			switch (_type)
			{
				case "Monster":
				{
					_baseValues.put(Stat.MAX_HP, getBaseHpMax() * NpcStatMultipliersConfig.MONSTER_HP_MULTIPLIER);
					_baseValues.put(Stat.MAX_MP, getBaseMpMax() * NpcStatMultipliersConfig.MONSTER_MP_MULTIPLIER);
					_baseValues.put(Stat.PHYSICAL_ATTACK, getBasePAtk() * NpcStatMultipliersConfig.MONSTER_PATK_MULTIPLIER);
					_baseValues.put(Stat.MAGIC_ATTACK, getBaseMAtk() * NpcStatMultipliersConfig.MONSTER_MATK_MULTIPLIER);
					_baseValues.put(Stat.PHYSICAL_DEFENCE, getBasePDef() * NpcStatMultipliersConfig.MONSTER_PDEF_MULTIPLIER);
					_baseValues.put(Stat.MAGICAL_DEFENCE, getBaseMDef() * NpcStatMultipliersConfig.MONSTER_MDEF_MULTIPLIER);
					_aggroRange *= NpcStatMultipliersConfig.MONSTER_AGRRO_RANGE_MULTIPLIER;
					_clanHelpRange *= NpcStatMultipliersConfig.MONSTER_CLAN_HELP_RANGE_MULTIPLIER;
					break;
				}
				case "RaidBoss":
				case "GrandBoss":
				{
					_baseValues.put(Stat.MAX_HP, getBaseHpMax() * NpcStatMultipliersConfig.RAIDBOSS_HP_MULTIPLIER);
					_baseValues.put(Stat.MAX_MP, getBaseMpMax() * NpcStatMultipliersConfig.RAIDBOSS_MP_MULTIPLIER);
					_baseValues.put(Stat.PHYSICAL_ATTACK, getBasePAtk() * NpcStatMultipliersConfig.RAIDBOSS_PATK_MULTIPLIER);
					_baseValues.put(Stat.MAGIC_ATTACK, getBaseMAtk() * NpcStatMultipliersConfig.RAIDBOSS_MATK_MULTIPLIER);
					_baseValues.put(Stat.PHYSICAL_DEFENCE, getBasePDef() * NpcStatMultipliersConfig.RAIDBOSS_PDEF_MULTIPLIER);
					_baseValues.put(Stat.MAGICAL_DEFENCE, getBaseMDef() * NpcStatMultipliersConfig.RAIDBOSS_MDEF_MULTIPLIER);
					_aggroRange *= NpcStatMultipliersConfig.RAIDBOSS_AGRRO_RANGE_MULTIPLIER;
					_clanHelpRange *= NpcStatMultipliersConfig.RAIDBOSS_CLAN_HELP_RANGE_MULTIPLIER;
					break;
				}
				case "Guard":
				{
					_baseValues.put(Stat.MAX_HP, getBaseHpMax() * NpcStatMultipliersConfig.GUARD_HP_MULTIPLIER);
					_baseValues.put(Stat.MAX_MP, getBaseMpMax() * NpcStatMultipliersConfig.GUARD_MP_MULTIPLIER);
					_baseValues.put(Stat.PHYSICAL_ATTACK, getBasePAtk() * NpcStatMultipliersConfig.GUARD_PATK_MULTIPLIER);
					_baseValues.put(Stat.MAGIC_ATTACK, getBaseMAtk() * NpcStatMultipliersConfig.GUARD_MATK_MULTIPLIER);
					_baseValues.put(Stat.PHYSICAL_DEFENCE, getBasePDef() * NpcStatMultipliersConfig.GUARD_PDEF_MULTIPLIER);
					_baseValues.put(Stat.MAGICAL_DEFENCE, getBaseMDef() * NpcStatMultipliersConfig.GUARD_MDEF_MULTIPLIER);
					_aggroRange *= NpcStatMultipliersConfig.GUARD_AGRRO_RANGE_MULTIPLIER;
					_clanHelpRange *= NpcStatMultipliersConfig.GUARD_CLAN_HELP_RANGE_MULTIPLIER;
					break;
				}
				case "Defender":
				{
					_baseValues.put(Stat.MAX_HP, getBaseHpMax() * NpcStatMultipliersConfig.DEFENDER_HP_MULTIPLIER);
					_baseValues.put(Stat.MAX_MP, getBaseMpMax() * NpcStatMultipliersConfig.DEFENDER_MP_MULTIPLIER);
					_baseValues.put(Stat.PHYSICAL_ATTACK, getBasePAtk() * NpcStatMultipliersConfig.DEFENDER_PATK_MULTIPLIER);
					_baseValues.put(Stat.MAGIC_ATTACK, getBaseMAtk() * NpcStatMultipliersConfig.DEFENDER_MATK_MULTIPLIER);
					_baseValues.put(Stat.PHYSICAL_DEFENCE, getBasePDef() * NpcStatMultipliersConfig.DEFENDER_PDEF_MULTIPLIER);
					_baseValues.put(Stat.MAGICAL_DEFENCE, getBaseMDef() * NpcStatMultipliersConfig.DEFENDER_MDEF_MULTIPLIER);
					_aggroRange *= NpcStatMultipliersConfig.DEFENDER_AGRRO_RANGE_MULTIPLIER;
					_clanHelpRange *= NpcStatMultipliersConfig.DEFENDER_CLAN_HELP_RANGE_MULTIPLIER;
					break;
				}
			}
		}
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getDisplayId()
	{
		return _displayId;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public String getType()
	{
		return _type;
	}
	
	public boolean isType(String type)
	{
		return _type.equalsIgnoreCase(type);
	}
	
	public String getName()
	{
		return _name;
	}
	
	public boolean isUsingServerSideName()
	{
		return _usingServerSideName;
	}
	
	public String getTitle()
	{
		return _title;
	}
	
	public boolean isUsingServerSideTitle()
	{
		return _usingServerSideTitle;
	}
	
	public StatSet getParameters()
	{
		return _parameters;
	}
	
	public void setParameters(StatSet set)
	{
		_parameters = set;
	}
	
	public Sex getSex()
	{
		return _sex;
	}
	
	public int getChestId()
	{
		return _chestId;
	}
	
	public int getRHandId()
	{
		return _rhandId;
	}
	
	public int getLHandId()
	{
		return _lhandId;
	}
	
	public int getWeaponEnchant()
	{
		return _weaponEnchant;
	}
	
	public double getExp()
	{
		return _exp;
	}
	
	public double getSP()
	{
		return _sp;
	}
	
	public double getRaidPoints()
	{
		return _raidPoints;
	}
	
	public long getAttributeExp()
	{
		return _attributeExp;
	}
	
	public ElementalSpiritType getElementalSpiritType()
	{
		return _elementalType;
	}
	
	public boolean isUnique()
	{
		return _unique;
	}
	
	public boolean isAttackable()
	{
		return _attackable;
	}
	
	public boolean isTargetable()
	{
		return _targetable;
	}
	
	public boolean isTalkable()
	{
		return _talkable;
	}
	
	public boolean isQuestMonster()
	{
		return _isQuestMonster;
	}
	
	public boolean isUndying()
	{
		return _undying;
	}
	
	public boolean isShowName()
	{
		return _showName;
	}
	
	public boolean isRandomWalkEnabled()
	{
		return _randomWalk;
	}
	
	public boolean isRandomAnimationEnabled()
	{
		return _randomAnimation;
	}
	
	public boolean isFlying()
	{
		return _flying;
	}
	
	public boolean isFakePlayer()
	{
		return _fakePlayer;
	}
	
	public FakePlayerHolder getFakePlayerInfo()
	{
		return _fakePlayerInfo;
	}
	
	public boolean canMove()
	{
		return _canMove;
	}
	
	public boolean isNoSleepMode()
	{
		return _noSleepMode;
	}
	
	public boolean isPassableDoor()
	{
		return _passableDoor;
	}
	
	public boolean hasSummoner()
	{
		return _hasSummoner;
	}
	
	public boolean canBeSown()
	{
		return _canBeSown;
	}
	
	public boolean canBeCrt()
	{
		return _canBeCrt;
	}
	
	public boolean isDeathPenalty()
	{
		return _isDeathPenalty;
	}
	
	public int getCorpseTime()
	{
		return _corpseTime;
	}
	
	public AIType getAIType()
	{
		return _aiType;
	}
	
	public int getAggroRange()
	{
		return _aggroRange;
	}
	
	public int getClanHelpRange()
	{
		return _clanHelpRange;
	}
	
	public boolean isChaos()
	{
		return _isChaos;
	}
	
	public boolean isAggressive()
	{
		return _isAggressive;
	}
	
	public int getSoulShot()
	{
		return _soulShot;
	}
	
	public int getSpiritShot()
	{
		return _spiritShot;
	}
	
	public int getSoulShotChance()
	{
		return _soulShotChance;
	}
	
	public int getSpiritShotChance()
	{
		return _spiritShotChance;
	}
	
	public int getMinSkillChance()
	{
		return _minSkillChance;
	}
	
	public int getMaxSkillChance()
	{
		return _maxSkillChance;
	}
	
	public double getHitTimeFactor()
	{
		return _hitTimeFactor;
	}
	
	public double getHitTimeFactorSkill()
	{
		return _hitTimeFactorSkill;
	}
	
	public int getBaseAttackAngle()
	{
		return _baseAttackAngle;
	}
	
	@Override
	public Map<Integer, Skill> getSkills()
	{
		return _skills;
	}
	
	public void setSkills(Map<Integer, Skill> skills)
	{
		_skills = skills != null ? Collections.unmodifiableMap(skills) : Collections.emptyMap();
	}
	
	public List<Skill> getAISkills(AISkillScope aiSkillScope)
	{
		return _aiSkillLists.getOrDefault(aiSkillScope, Collections.emptyList());
	}
	
	public void setAISkillLists(Map<AISkillScope, List<Skill>> aiSkillLists)
	{
		_aiSkillLists = aiSkillLists != null ? Collections.unmodifiableMap(aiSkillLists) : Collections.emptyMap();
	}
	
	public Set<Integer> getClans()
	{
		return _clans;
	}
	
	public int getMpRewardValue()
	{
		return _mpRewardValue;
	}
	
	public MpRewardType getMpRewardType()
	{
		return _mpRewardType;
	}
	
	public int getMpRewardTicks()
	{
		return _mpRewardTicks;
	}
	
	public MpRewardAffectType getMpRewardAffectType()
	{
		return _mpRewardAffectType;
	}
	
	/**
	 * @param clans A sorted array of clan ids
	 */
	public void setClans(Set<Integer> clans)
	{
		_clans = clans != null ? Collections.unmodifiableSet(clans) : null;
	}
	
	/**
	 * @param clanName clan name to check if it belongs to this NPC template clans.
	 * @param clanNames clan names to check if they belong to this NPC template clans.
	 * @return {@code true} if at least one of the clan names belong to this NPC template clans, {@code false} otherwise.
	 */
	public boolean isClan(String clanName, String... clanNames)
	{
		// Using local variable for the sake of reloading since it can be turned to null.
		final Set<Integer> clans = _clans;
		if (clans == null)
		{
			return false;
		}
		
		int clanId = NpcData.getInstance().getGenericClanId();
		if (clans.contains(clanId))
		{
			return true;
		}
		
		clanId = NpcData.getInstance().getClanId(clanName);
		if (clans.contains(clanId))
		{
			return true;
		}
		
		for (String name : clanNames)
		{
			clanId = NpcData.getInstance().getClanId(name);
			if (clans.contains(clanId))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * @param clans A set of clan names to check if they belong to this NPC template clans.
	 * @return {@code true} if at least one of the clan names belong to this NPC template clans, {@code false} otherwise.
	 */
	public boolean isClan(Set<Integer> clans)
	{
		// Using local variable for the sake of reloading since it can be turned to null.
		final Set<Integer> clanSet = _clans;
		if ((clanSet == null) || (clans == null))
		{
			return false;
		}
		
		final int clanId = NpcData.getInstance().getGenericClanId();
		if (clanSet.contains(clanId))
		{
			return true;
		}
		
		for (Integer id : clans)
		{
			if (clanSet.contains(id))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public Set<Integer> getIgnoreClanNpcIds()
	{
		return _ignoreClanNpcIds;
	}
	
	public boolean hasIgnoreClanNpcIds()
	{
		return _ignoreClanNpcIds != null;
	}
	
	/**
	 * @param ignoreClanNpcIds the ignore clan npc ids
	 */
	public void setIgnoreClanNpcIds(Set<Integer> ignoreClanNpcIds)
	{
		_ignoreClanNpcIds = ignoreClanNpcIds != null ? Collections.unmodifiableSet(ignoreClanNpcIds) : null;
	}
	
	public void removeDropGroups()
	{
		_dropGroups = null;
	}
	
	public void removeDrops()
	{
		_dropListDeath = null;
		_dropListSpoil = null;
		_dropListFortune = null;
	}
	
	public void setDropGroups(List<DropGroupHolder> groups)
	{
		_dropGroups = groups;
	}
	
	public void addDrop(DropHolder dropHolder)
	{
		if (_dropListDeath == null)
		{
			_dropListDeath = new ArrayList<>(1);
		}
		
		_dropListDeath.add(dropHolder);
	}
	
	public void addSpoil(DropHolder dropHolder)
	{
		if (_dropListSpoil == null)
		{
			_dropListSpoil = new ArrayList<>(1);
		}
		
		_dropListSpoil.add(dropHolder);
	}
	
	public void addFortune(DropHolder dropHolder)
	{
		if (_dropListFortune == null)
		{
			_dropListFortune = new ArrayList<>(1);
		}
		
		_dropListFortune.add(dropHolder);
	}
	
	public List<DropGroupHolder> getDropGroups()
	{
		return _dropGroups;
	}
	
	public List<DropHolder> getDropList()
	{
		return _dropListDeath;
	}
	
	public List<DropHolder> getSpoilList()
	{
		return _dropListSpoil;
	}
	
	public List<ItemHolder> calculateDrops(DropType dropType, Creature victim, Creature killer)
	{
		if (dropType == DropType.DROP)
		{
			// calculate group drops
			List<ItemHolder> groupDrops = null;
			if (_dropGroups != null)
			{
				groupDrops = calculateGroupDrops(victim, killer);
			}
			
			// calculate ungrouped drops
			List<ItemHolder> ungroupedDrops = null;
			if (_dropListDeath != null)
			{
				ungroupedDrops = calculateUngroupedDrops(dropType, victim, killer);
			}
			
			// return results
			if ((groupDrops != null) && (ungroupedDrops != null))
			{
				groupDrops.addAll(ungroupedDrops);
				ungroupedDrops.clear();
				return groupDrops;
			}
			
			if (groupDrops != null)
			{
				return groupDrops;
			}
			
			if (ungroupedDrops != null)
			{
				return ungroupedDrops;
			}
		}
		else if (dropType == DropType.SPOIL)
		{
			if (_dropListSpoil != null)
			{
				return calculateUngroupedDrops(dropType, victim, killer);
			}
		}
		else if ((dropType == DropType.FORTUNE) && (_dropListFortune != null))
		{
			return calculateUngroupedDrops(dropType, victim, killer);
		}
		
		// no drops
		return null;
	}

	private List<ItemHolder> calculateGroupDrops(Creature victim, Creature killer)
	{
		final int levelDifference = killer.getLevel() - victim.getLevel();
		final Player player = killer.asPlayer();
		final int dropOccurrenceLimit = victim.isRaid() ? RatesConfig.DROP_MAX_OCCURRENCES_RAIDBOSS : RatesConfig.DROP_MAX_OCCURRENCES_NORMAL;

		if (dropOccurrenceLimit <= 0)
		{
			return null;
		}

		// 收集所有掉落的物品
		List<ItemHolder> potentialDrops = new ArrayList<>();

		for (DropGroupHolder group : _dropGroups)
		{
			// ========================================
			// 第一階段：判斷這個 group 是否觸發
			// ========================================
			double groupChance = group.getChance();

			// 可以在這裡加上 group 的倍率（如果需要）
			if (victim.isRaid())
			{
				groupChance *= RatesConfig.RATE_RAID_DROP_CHANCE_MULTIPLIER;
			}

			// 隨機判定 group 是否觸發
			if (Rnd.get(10000) >= (groupChance * 100))
			{
				continue; // 這個組沒觸發，跳過整組
			}

			// ========================================
			// 第二階段：打亂組內物品順序（重要！）
			// ========================================
			List<DropHolder> shuffledGroupItems = new ArrayList<>(group.getDropList());
			Collections.shuffle(shuffledGroupItems);

			// ========================================
			// 第三階段：組內每個物品獨立判定
			// ========================================
			for (DropHolder dropItem : shuffledGroupItems)
			{
				final int itemId = dropItem.getItemId();
				final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
				final boolean champion = victim.isChampion();

				// === 等級差檢查 ===
				if (levelDifference > (itemId == Inventory.ADENA_ID ?
						RatesConfig.DROP_ADENA_MAX_LEVEL_LOWEST_DIFFERENCE :
						RatesConfig.DROP_ITEM_MAX_LEVEL_LOWEST_DIFFERENCE))
				{
					continue;
				}

				// === 倍率計算 ===
				double rateChance = 1;
				if (RatesConfig.RATE_DROP_CHANCE_BY_ID.get(itemId) != null)
				{
					rateChance *= RatesConfig.RATE_DROP_CHANCE_BY_ID.get(itemId);
					if (champion && (itemId == Inventory.ADENA_ID))
					{
						rateChance *= ChampionMonstersConfig.CHAMPION_ADENAS_REWARDS_CHANCE;
					}

					if ((itemId == Inventory.ADENA_ID) && (rateChance > 100))
					{
						rateChance = 100;
					}
				}
				else if (item.hasExImmediateEffect())
				{
					rateChance *= RatesConfig.RATE_HERB_DROP_CHANCE_MULTIPLIER;
				}
				else if (victim.isRaid())
				{
					rateChance *= RatesConfig.RATE_RAID_DROP_CHANCE_MULTIPLIER;
				}
				else
				{
					rateChance *= RatesConfig.RATE_DEATH_DROP_CHANCE_MULTIPLIER * (champion ? ChampionMonstersConfig.CHAMPION_REWARDS_CHANCE : 1);
				}

				// Premium 倍率
				if (player != null)
				{
					if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus())
					{
						if (PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(itemId) != null)
						{
							rateChance *= PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(itemId);
						}
						else if (item.hasExImmediateEffect())
						{
							// TODO: Premium herb chance? :)
						}
						else if (victim.isRaid())
						{
							// TODO: Premium raid chance? :)
						}
						else
						{
							rateChance *= PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE;
						}
					}

					// 玩家加成
					final PlayerStat stat = player.getStat();
					rateChance *= stat.getMul(Stat.BONUS_DROP_RATE, 1);
					if (itemId == Inventory.LCOIN_ID)
					{
						rateChance *= stat.getMul(Stat.BONUS_DROP_RATE_LCOIN, 1);
					}
				}

				// === 計算物品最終機率（不含 group chance，因為已經判定過了）===
				final double itemChance = dropItem.getChance() * rateChance;

				// === 隨機判定物品是否掉落 ===
				if (Rnd.get(10000) < (itemChance * 100))
				{
					// 計算掉落數量
					final long count;
					if (dropItem.getMin() == dropItem.getMax())
					{
						count = dropItem.getMin();
					}
					else
					{
						count = Rnd.get(dropItem.getMin(), dropItem.getMax());
					}

					if (count > 0)
					{
						potentialDrops.add(new ItemHolder(itemId, count));
					}
				}
			}
		}

		// 第四階段：處理掉落上限
		List<ItemHolder> calculatedDrops = null;

		if (!potentialDrops.isEmpty())
		{
			if (potentialDrops.size() > dropOccurrenceLimit)
			{
				// 超過上限：隨機選擇要保留的物品
				Collections.shuffle(potentialDrops);
				calculatedDrops = new ArrayList<>(potentialDrops.subList(0, dropOccurrenceLimit));
			}
			else
			{
				// 未超過上限：全部保留
				calculatedDrops = potentialDrops;
			}
		}

		// 第五階段：冠軍怪額外獎勵
		if (victim.isChampion() && (Rnd.get(100) < (victim.getLevel() < killer.getLevel() ?
				ChampionMonstersConfig.CHAMPION_REWARD_LOWER_LEVEL_ITEM_CHANCE :
				ChampionMonstersConfig.CHAMPION_REWARD_HIGHER_LEVEL_ITEM_CHANCE)))
		{
			if (calculatedDrops == null)
			{
				calculatedDrops = new ArrayList<>();
			}

			if (!calculatedDrops.containsAll(ChampionMonstersConfig.CHAMPION_REWARD_ITEMS))
			{
				calculatedDrops.addAll(ChampionMonstersConfig.CHAMPION_REWARD_ITEMS);
			}
		}

		return calculatedDrops;
	}

	private List<ItemHolder> calculateUngroupedDrops(DropType dropType, Creature victim, Creature killer)
	{
		final List<DropHolder> dropList = dropType == DropType.SPOIL ? _dropListSpoil : dropType == DropType.FORTUNE ? _dropListFortune : _dropListDeath;
		final int levelDifference = killer.getLevel() - victim.getLevel();
		final int dropOccurrenceLimit = victim.isRaid() ? RatesConfig.DROP_MAX_OCCURRENCES_RAIDBOSS : RatesConfig.DROP_MAX_OCCURRENCES_NORMAL;

		if (dropOccurrenceLimit <= 0)
		{
			return null;
		}

		// ========================================
		// 第一階段：打亂掉落列表順序（重要！）
		// ========================================
		List<DropHolder> shuffledDropList = new ArrayList<>(dropList);
		Collections.shuffle(shuffledDropList);

		// ========================================
		// 第二階段：計算所有可能掉落的物品
		// ========================================
		List<ItemHolder> potentialDrops = new ArrayList<>();

		for (DropHolder dropItem : shuffledDropList)
		{
			// === 等級差檢查 ===
			if (levelDifference > (dropItem.getItemId() == Inventory.ADENA_ID ?
					RatesConfig.DROP_ADENA_MAX_LEVEL_LOWEST_DIFFERENCE :
					RatesConfig.DROP_ITEM_MAX_LEVEL_LOWEST_DIFFERENCE))
			{
				continue;
			}

			// === 隨機判定 ===
			final ItemHolder drop = calculateUngroupedDrop(dropItem, victim, killer);
			if (drop != null)
			{
				potentialDrops.add(drop);
			}
		}

		// 第三階段：處理掉落上限
		List<ItemHolder> calculatedDrops = null;

		if (!potentialDrops.isEmpty())
		{
			if (potentialDrops.size() > dropOccurrenceLimit)
			{
				// 超過上限：隨機選擇要保留的物品
				Collections.shuffle(potentialDrops);
				calculatedDrops = new ArrayList<>(potentialDrops.subList(0, dropOccurrenceLimit));
			}
			else
			{
				// 未超過上限：全部保留
				calculatedDrops = potentialDrops;
			}
		}

		// 第四階段：冠軍怪額外獎勵
		if (victim.isChampion() && (Rnd.get(100) < (victim.getLevel() < killer.getLevel() ?
				ChampionMonstersConfig.CHAMPION_REWARD_LOWER_LEVEL_ITEM_CHANCE :
				ChampionMonstersConfig.CHAMPION_REWARD_HIGHER_LEVEL_ITEM_CHANCE)))
		{
			if (calculatedDrops == null)
			{
				calculatedDrops = new ArrayList<>();
			}

			if (!calculatedDrops.containsAll(ChampionMonstersConfig.CHAMPION_REWARD_ITEMS))
			{
				calculatedDrops.addAll(ChampionMonstersConfig.CHAMPION_REWARD_ITEMS);
			}
		}

		return calculatedDrops;
	}
	
	/**
	 * @param group
	 * @param dropItem
	 * @param victim
	 * @param killer
	 * @param chance
	 * @return ItemHolder
	 */
	private ItemHolder calculateGroupDrop(DropGroupHolder group, DropHolder dropItem, Creature victim, Creature killer, double chance)
	{
		final int itemId = dropItem.getItemId();
		final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
		final boolean champion = victim.isChampion();
		
		// calculate if item will drop
		if ((Rnd.nextDouble() * 100) < chance)
		{
			// amount is calculated after chance returned success
			double rateAmount = 1;
			if (RatesConfig.RATE_DROP_AMOUNT_BY_ID.get(itemId) != null)
			{
				rateAmount *= RatesConfig.RATE_DROP_AMOUNT_BY_ID.get(itemId);
				if (champion && (itemId == Inventory.ADENA_ID))
				{
					rateAmount *= ChampionMonstersConfig.CHAMPION_ADENAS_REWARDS_AMOUNT;
				}
			}
			else if (item.hasExImmediateEffect())
			{
				rateAmount *= RatesConfig.RATE_HERB_DROP_AMOUNT_MULTIPLIER;
			}
			else if (victim.isRaid())
			{
				rateAmount *= RatesConfig.RATE_RAID_DROP_AMOUNT_MULTIPLIER;
			}
			else
			{
				rateAmount *= RatesConfig.RATE_DEATH_DROP_AMOUNT_MULTIPLIER * (champion ? ChampionMonstersConfig.CHAMPION_REWARDS_AMOUNT : 1);
			}
			
			// premium amount
			final Player player = killer.asPlayer();
			if (player != null)
			{
				if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus())
				{
					if (PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(itemId) != null)
					{
						rateAmount *= PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(itemId);
					}
					else if (item.hasExImmediateEffect())
					{
						// TODO: Premium herb amount? :)
					}
					else if (victim.isRaid())
					{
						// TODO: Premium raid amount? :)
					}
					else
					{
						rateAmount *= PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT;
					}
				}
				
				// bonus drop amount effect
				final PlayerStat stat = player.getStat();
				rateAmount *= stat.getMul(Stat.BONUS_DROP_AMOUNT, 1);
				if (itemId == Inventory.ADENA_ID)
				{
					rateAmount *= stat.getMul(Stat.BONUS_DROP_ADENA, 1);
				}
			}
			
			// finally
			return new ItemHolder(itemId, (long) (Rnd.get(dropItem.getMin(), dropItem.getMax()) * rateAmount));
		}
		
		return null;
	}
	
	/**
	 * @param dropItem
	 * @param victim
	 * @param killer
	 * @return ItemHolder
	 */
	private ItemHolder calculateUngroupedDrop(DropHolder dropItem, Creature victim, Creature killer)
	{
		switch (dropItem.getDropType())
		{
			case DROP:
			case LUCKY:
			{
				final int itemId = dropItem.getItemId();
				final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
				final boolean champion = victim.isChampion();
				
				// chance
				double rateChance = 1;
				if (RatesConfig.RATE_DROP_CHANCE_BY_ID.get(itemId) != null)
				{
					rateChance *= RatesConfig.RATE_DROP_CHANCE_BY_ID.get(itemId);
					if (champion && (itemId == Inventory.ADENA_ID))
					{
						rateChance *= ChampionMonstersConfig.CHAMPION_ADENAS_REWARDS_CHANCE;
					}
					
					if ((itemId == Inventory.ADENA_ID) && (rateChance > 100))
					{
						rateChance = 100;
					}
				}
				else if (item.hasExImmediateEffect())
				{
					rateChance *= RatesConfig.RATE_HERB_DROP_CHANCE_MULTIPLIER;
				}
				else if (victim.isRaid())
				{
					rateChance *= RatesConfig.RATE_RAID_DROP_CHANCE_MULTIPLIER;
				}
				else
				{
					rateChance *= RatesConfig.RATE_DEATH_DROP_CHANCE_MULTIPLIER * (champion ? ChampionMonstersConfig.CHAMPION_REWARDS_CHANCE : 1);
				}
				
				// premium chance
				final Player player = killer.asPlayer();
				if (player != null)
				{
					if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus())
					{
						if (PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(itemId) != null)
						{
							rateChance *= PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(itemId);
						}
						else if (item.hasExImmediateEffect())
						{
							// TODO: Premium herb chance? :)
						}
						else if (victim.isRaid())
						{
							// TODO: Premium raid chance? :)
						}
						else
						{
							rateChance *= PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE;
						}
					}
					
					// bonus drop rate effect
					final PlayerStat stat = player.getStat();
					rateChance *= stat.getMul(Stat.BONUS_DROP_RATE, 1);
					if (item.getId() == Inventory.LCOIN_ID)
					{
						rateChance *= stat.getMul(Stat.BONUS_DROP_RATE_LCOIN, 1);
					}
				}
				
				// calculate if item will drop
				if ((Rnd.nextDouble() * 100) < (dropItem.getChance() * rateChance))
				{
					// amount is calculated after chance returned success
					double rateAmount = 1;
					if (RatesConfig.RATE_DROP_AMOUNT_BY_ID.get(itemId) != null)
					{
						rateAmount *= RatesConfig.RATE_DROP_AMOUNT_BY_ID.get(itemId);
						if (champion && (itemId == Inventory.ADENA_ID))
						{
							rateAmount *= ChampionMonstersConfig.CHAMPION_ADENAS_REWARDS_AMOUNT;
						}
					}
					else if (item.hasExImmediateEffect())
					{
						rateAmount *= RatesConfig.RATE_HERB_DROP_AMOUNT_MULTIPLIER;
					}
					else if (victim.isRaid())
					{
						rateAmount *= RatesConfig.RATE_RAID_DROP_AMOUNT_MULTIPLIER;
					}
					else
					{
						rateAmount *= RatesConfig.RATE_DEATH_DROP_AMOUNT_MULTIPLIER * (champion ? ChampionMonstersConfig.CHAMPION_REWARDS_AMOUNT : 1);
					}
					
					// premium amount
					if (player != null)
					{
						if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus())
						{
							if (PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(itemId) != null)
							{
								rateAmount *= PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(itemId);
							}
							else if (item.hasExImmediateEffect())
							{
								// TODO: Premium herb amount? :)
							}
							else if (victim.isRaid())
							{
								// TODO: Premium raid amount? :)
							}
							else
							{
								rateAmount *= PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT;
							}
						}
						
						// bonus drop amount effect
						final PlayerStat stat = player.getStat();
						rateAmount *= stat.getMul(Stat.BONUS_DROP_AMOUNT, 1);
						if (itemId == Inventory.ADENA_ID)
						{
							rateAmount *= stat.getMul(Stat.BONUS_DROP_ADENA, 1);
						}
					}
					
					// finally
					return new ItemHolder(itemId, (long) (Rnd.get(dropItem.getMin(), dropItem.getMax()) * rateAmount));
				}
				break;
			}
			case SPOIL:
			{
				// chance
				double rateChance = RatesConfig.RATE_SPOIL_DROP_CHANCE_MULTIPLIER;
				
				// premium chance
				final Player player = killer.asPlayer();
				if (player != null)
				{
					if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus())
					{
						rateChance *= PremiumSystemConfig.PREMIUM_RATE_SPOIL_CHANCE;
					}
					
					// bonus spoil rate effect
					rateChance *= player.getStat().getMul(Stat.BONUS_SPOIL_RATE, 1);
				}
				
				// calculate if item will be rewarded
				if ((Rnd.nextDouble() * 100) < (dropItem.getChance() * rateChance))
				{
					// amount is calculated after chance returned success
					double rateAmount = RatesConfig.RATE_SPOIL_DROP_AMOUNT_MULTIPLIER;
					
					// premium amount
					if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && (player != null) && player.hasPremiumStatus())
					{
						rateAmount *= PremiumSystemConfig.PREMIUM_RATE_SPOIL_AMOUNT;
					}
					
					// finally
					return new ItemHolder(dropItem.getItemId(), (long) (Rnd.get(dropItem.getMin(), dropItem.getMax()) * rateAmount));
				}
				break;
			}
		}
		
		return null;
	}
	
	public float getCollisionRadiusGrown()
	{
		return _collisionRadiusGrown;
	}
	
	public float getCollisionHeightGrown()
	{
		return _collisionHeightGrown;
	}
	
	public static boolean isAssignableTo(Class<?> subValue, Class<?> clazz)
	{
		// If clazz represents an interface
		if (clazz.isInterface())
		{
			// check if obj implements the clazz interface
			for (Class<?> interface1 : subValue.getInterfaces())
			{
				if (clazz.getName().equals(interface1.getName()))
				{
					return true;
				}
			}
		}
		else
		{
			Class<?> sub = subValue;
			do
			{
				if (sub.getName().equals(clazz.getName()))
				{
					return true;
				}
				
				sub = sub.getSuperclass();
			}
			while (sub != null);
		}
		
		return false;
	}
	
	/**
	 * Checks if obj can be assigned to the Class represented by clazz.<br>
	 * This is true if, and only if, obj is the same class represented by clazz, or a subclass of it or obj implements the interface represented by clazz.
	 * @param obj
	 * @param clazz
	 * @return {@code true} if the object can be assigned to the class, {@code false} otherwise
	 */
	public static boolean isAssignableTo(Object obj, Class<?> clazz)
	{
		return isAssignableTo(obj.getClass(), clazz);
	}
}
