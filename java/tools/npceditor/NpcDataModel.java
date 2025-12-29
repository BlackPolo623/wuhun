package tools.npceditor;

import java.util.ArrayList;
import java.util.List;

/**
 * NPC數據模型 - 對應XML中的NPC結構
 */
public class NpcDataModel
{
	// 基本屬性
	private int id;
	private int displayId;
	private int level;
	private String type;
	private String name;
	private String title;
	private String element;

	// 種族和性別
	private String race;
	private String sex;

	// 屬性值
	private Stats stats;

	// 技能列表
	private List<SkillEntry> skills;

	// 掉落列表
	private List<DropGroup> dropGroups;

	// 幸運掉落
	private List<FortuneItem> fortuneItems;

	public NpcDataModel()
	{
		this.skills = new ArrayList<>();
		this.dropGroups = new ArrayList<>();
		this.fortuneItems = new ArrayList<>();
		this.stats = new Stats();
	}

	// ==================== Getters & Setters ====================

	public int getId()
	{
		return id;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public int getDisplayId()
	{
		return displayId;
	}

	public void setDisplayId(int displayId)
	{
		this.displayId = displayId;
	}

	public int getLevel()
	{
		return level;
	}

	public void setLevel(int level)
	{
		this.level = level;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public String getElement()
	{
		return element;
	}

	public void setElement(String element)
	{
		this.element = element;
	}

	public String getRace()
	{
		return race;
	}

	public void setRace(String race)
	{
		this.race = race;
	}

	public String getSex()
	{
		return sex;
	}

	public void setSex(String sex)
	{
		this.sex = sex;
	}

	public Stats getStats()
	{
		return stats;
	}

	public void setStats(Stats stats)
	{
		this.stats = stats;
	}

	public List<SkillEntry> getSkills()
	{
		return skills;
	}

	public void setSkills(List<SkillEntry> skills)
	{
		this.skills = skills;
	}

	public List<DropGroup> getDropGroups()
	{
		return dropGroups;
	}

	public void setDropGroups(List<DropGroup> dropGroups)
	{
		this.dropGroups = dropGroups;
	}

	public List<FortuneItem> getFortuneItems()
	{
		return fortuneItems;
	}

	public void setFortuneItems(List<FortuneItem> fortuneItems)
	{
		this.fortuneItems = fortuneItems;
	}

	// ==================== 內部類 ====================

	/**
	 * 屬性值類 - 使用double支持高精度小數
	 */
	public static class Stats
	{
		// 基礎屬性
		private int str;
		private int intVal;
		private int dex;
		private int wit;
		private int con;
		private int men;

		// 生命值相關 - 改為double支持小數
		private double maxHp;
		private double hpRegen;
		private double maxMp;
		private double mpRegen;

		// 攻擊相關 - 改為double支持小數
		private double physicalAttack;
		private double magicalAttack;
		private int attackRandom;
		private int critical;
		private int accuracy;
		private int attackSpeed;
		private String attackType;
		private int attackRange;

		// 防禦相關 - 改為double支持小數
		private double physicalDefence;
		private double magicalDefence;

		// 速度相關
		private int walkSpeed;
		private int runSpeed;

		// 其他
		private int hitTime;
		private double collisionRadius;
		private double collisionHeight;

		// Getters & Setters
		public int getStr()
		{
			return str;
		}

		public void setStr(int str)
		{
			this.str = str;
		}

		public int getIntVal()
		{
			return intVal;
		}

		public void setIntVal(int intVal)
		{
			this.intVal = intVal;
		}

		public int getDex()
		{
			return dex;
		}

		public void setDex(int dex)
		{
			this.dex = dex;
		}

		public int getWit()
		{
			return wit;
		}

		public void setWit(int wit)
		{
			this.wit = wit;
		}

		public int getCon()
		{
			return con;
		}

		public void setCon(int con)
		{
			this.con = con;
		}

		public int getMen()
		{
			return men;
		}

		public void setMen(int men)
		{
			this.men = men;
		}

		public double getMaxHp()
		{
			return maxHp;
		}

		public void setMaxHp(double maxHp)
		{
			this.maxHp = maxHp;
		}

		public double getHpRegen()
		{
			return hpRegen;
		}

		public void setHpRegen(double hpRegen)
		{
			this.hpRegen = hpRegen;
		}

		public double getMaxMp()
		{
			return maxMp;
		}

		public void setMaxMp(double maxMp)
		{
			this.maxMp = maxMp;
		}

		public double getMpRegen()
		{
			return mpRegen;
		}

		public void setMpRegen(double mpRegen)
		{
			this.mpRegen = mpRegen;
		}

		public double getPhysicalAttack()
		{
			return physicalAttack;
		}

		public void setPhysicalAttack(double physicalAttack)
		{
			this.physicalAttack = physicalAttack;
		}

		public double getMagicalAttack()
		{
			return magicalAttack;
		}

		public void setMagicalAttack(double magicalAttack)
		{
			this.magicalAttack = magicalAttack;
		}

		public int getAttackRandom()
		{
			return attackRandom;
		}

		public void setAttackRandom(int attackRandom)
		{
			this.attackRandom = attackRandom;
		}

		public int getCritical()
		{
			return critical;
		}

		public void setCritical(int critical)
		{
			this.critical = critical;
		}

		public int getAccuracy()
		{
			return accuracy;
		}

		public void setAccuracy(int accuracy)
		{
			this.accuracy = accuracy;
		}

		public int getAttackSpeed()
		{
			return attackSpeed;
		}

		public void setAttackSpeed(int attackSpeed)
		{
			this.attackSpeed = attackSpeed;
		}

		public String getAttackType()
		{
			return attackType;
		}

		public void setAttackType(String attackType)
		{
			this.attackType = attackType;
		}

		public int getAttackRange()
		{
			return attackRange;
		}

		public void setAttackRange(int attackRange)
		{
			this.attackRange = attackRange;
		}

		public double getPhysicalDefence()
		{
			return physicalDefence;
		}

		public void setPhysicalDefence(double physicalDefence)
		{
			this.physicalDefence = physicalDefence;
		}

		public double getMagicalDefence()
		{
			return magicalDefence;
		}

		public void setMagicalDefence(double magicalDefence)
		{
			this.magicalDefence = magicalDefence;
		}

		public int getWalkSpeed()
		{
			return walkSpeed;
		}

		public void setWalkSpeed(int walkSpeed)
		{
			this.walkSpeed = walkSpeed;
		}

		public int getRunSpeed()
		{
			return runSpeed;
		}

		public void setRunSpeed(int runSpeed)
		{
			this.runSpeed = runSpeed;
		}

		public int getHitTime()
		{
			return hitTime;
		}

		public void setHitTime(int hitTime)
		{
			this.hitTime = hitTime;
		}

		public double getCollisionRadius()
		{
			return collisionRadius;
		}

		public void setCollisionRadius(double collisionRadius)
		{
			this.collisionRadius = collisionRadius;
		}

		public double getCollisionHeight()
		{
			return collisionHeight;
		}

		public void setCollisionHeight(double collisionHeight)
		{
			this.collisionHeight = collisionHeight;
		}
	}

	/**
	 * 技能條目
	 */
	public static class SkillEntry
	{
		private int skillId;
		private int level;
		private String skillName;

		public SkillEntry(int skillId, int level)
		{
			this.skillId = skillId;
			this.level = level;
		}

		public int getSkillId()
		{
			return skillId;
		}

		public void setSkillId(int skillId)
		{
			this.skillId = skillId;
		}

		public int getLevel()
		{
			return level;
		}

		public void setLevel(int level)
		{
			this.level = level;
		}

		public String getSkillName()
		{
			return skillName;
		}

		public void setSkillName(String skillName)
		{
			this.skillName = skillName;
		}

		@Override
		public String toString()
		{
			return String.format("[%d] %s (Lv.%d)", skillId, skillName != null ? skillName : "Unknown", level);
		}
	}

	/**
	 * 掉落組
	 */
	public static class DropGroup
	{
		private double chance;
		private List<DropItem> items;

		public DropGroup()
		{
			this.items = new ArrayList<>();
		}

		public double getChance()
		{
			return chance;
		}

		public void setChance(double chance)
		{
			this.chance = chance;
		}

		public List<DropItem> getItems()
		{
			return items;
		}

		public void setItems(List<DropItem> items)
		{
			this.items = items;
		}
	}

	/**
	 * 掉落物品
	 */
	public static class DropItem
	{
		private int itemId;
		private int min;
		private int max;
		private double chance;
		private String itemName;

		public DropItem(int itemId, int min, int max, double chance)
		{
			this.itemId = itemId;
			this.min = min;
			this.max = max;
			this.chance = chance;
		}

		public int getItemId()
		{
			return itemId;
		}

		public void setItemId(int itemId)
		{
			this.itemId = itemId;
		}

		public int getMin()
		{
			return min;
		}

		public void setMin(int min)
		{
			this.min = min;
		}

		public int getMax()
		{
			return max;
		}

		public void setMax(int max)
		{
			this.max = max;
		}

		public double getChance()
		{
			return chance;
		}

		public void setChance(double chance)
		{
			this.chance = chance;
		}

		public String getItemName()
		{
			return itemName;
		}

		public void setItemName(String itemName)
		{
			this.itemName = itemName;
		}

		@Override
		public String toString()
		{
			return String.format("[%d] %s x%d-%d (%.2f%%)", itemId, itemName != null ? itemName : "Unknown", min, max, chance);
		}
	}

	/**
	 * 幸運掉落物品
	 */
	public static class FortuneItem
	{
		private int itemId;
		private int min;
		private int max;
		private double chance;
		private String itemName;

		public FortuneItem(int itemId, int min, int max, double chance)
		{
			this.itemId = itemId;
			this.min = min;
			this.max = max;
			this.chance = chance;
		}

		public int getItemId()
		{
			return itemId;
		}

		public void setItemId(int itemId)
		{
			this.itemId = itemId;
		}

		public int getMin()
		{
			return min;
		}

		public void setMin(int min)
		{
			this.min = min;
		}

		public int getMax()
		{
			return max;
		}

		public void setMax(int max)
		{
			this.max = max;
		}

		public double getChance()
		{
			return chance;
		}

		public void setChance(double chance)
		{
			this.chance = chance;
		}

		public String getItemName()
		{
			return itemName;
		}

		public void setItemName(String itemName)
		{
			this.itemName = itemName;
		}

		@Override
		public String toString()
		{
			return String.format("[%d] %s x%d-%d (%.2f%%)", itemId, itemName != null ? itemName : "Unknown", min, max, chance);
		}
	}

	@Override
	public String toString()
	{
		return String.format("[%d] %s (Lv.%d)", id, name, level);
	}
}