package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.holders.PotentialRarityHolder;
import org.l2jmobius.gameserver.data.holders.PotentialSkillRangeHolder;
import org.l2jmobius.gameserver.data.holders.PotentialSlotHolder;

public class PotentialData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(PotentialData.class.getName());

	private final List<PotentialSlotHolder> _slots = new ArrayList<>();
	private final Map<Integer, PotentialRarityHolder> _rarities = new HashMap<>();
	private final List<PotentialSkillRangeHolder> _skillRanges = new ArrayList<>();

	protected PotentialData()
	{
		load();
	}

	@Override
	public void load()
	{
		_slots.clear();
		_rarities.clear();
		_skillRanges.clear();
		parseDatapackFile("data/PotentialData.xml");
		LOGGER.info(getClass().getSimpleName() + ": 載入了 " + _slots.size() + " 個潛能槽位");
		LOGGER.info(getClass().getSimpleName() + ": 載入了 " + _rarities.size() + " 個稀有度等級");
		LOGGER.info(getClass().getSimpleName() + ": 載入了 " + _skillRanges.size() + " 個技能稀有度區間");
	}

	@Override
	public void parseDocument(Document doc, File f)
	{
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("slots".equalsIgnoreCase(d.getNodeName()))
					{
						parseSlots(d);
					}
					else if ("rarities".equalsIgnoreCase(d.getNodeName()))
					{
						parseRarities(d);
					}
					else if ("skillRanges".equalsIgnoreCase(d.getNodeName()))
					{
						parseSkillRanges(d);
					}
				}
			}
		}
	}

	private void parseSlots(Node slotsNode)
	{
		for (Node slotNode = slotsNode.getFirstChild(); slotNode != null; slotNode = slotNode.getNextSibling())
		{
			if ("slot".equalsIgnoreCase(slotNode.getNodeName()))
			{
				final int id = parseInteger(slotNode.getAttributes(), "id");
				final int minSkill = parseInteger(slotNode.getAttributes(), "minSkill");
				final int maxSkill = parseInteger(slotNode.getAttributes(), "maxSkill");
				_slots.add(new PotentialSlotHolder(id, minSkill, maxSkill));
			}
		}
	}

	private void parseRarities(Node raritiesNode)
	{
		for (Node rarityNode = raritiesNode.getFirstChild(); rarityNode != null; rarityNode = rarityNode.getNextSibling())
		{
			if ("rarity".equalsIgnoreCase(rarityNode.getNodeName()))
			{
				final int level = parseInteger(rarityNode.getAttributes(), "level");
				final String color = parseString(rarityNode.getAttributes(), "color");
				final String name = parseString(rarityNode.getAttributes(), "name");
				_rarities.put(level, new PotentialRarityHolder(level, color, name));
			}
		}
	}

	private void parseSkillRanges(Node rangesNode)
	{
		for (Node rangeNode = rangesNode.getFirstChild(); rangeNode != null; rangeNode = rangeNode.getNextSibling())
		{
			if ("range".equalsIgnoreCase(rangeNode.getNodeName()))
			{
				final int slot = parseInteger(rangeNode.getAttributes(), "slot");
				final int minSkill = parseInteger(rangeNode.getAttributes(), "minSkill");
				final int maxSkill = parseInteger(rangeNode.getAttributes(), "maxSkill");
				final int rarity = parseInteger(rangeNode.getAttributes(), "rarity");
				_skillRanges.add(new PotentialSkillRangeHolder(slot, minSkill, maxSkill, rarity));
			}
		}
	}

	public PotentialSlotHolder getSlot(int slotIndex)
	{
		if (slotIndex < 1 || slotIndex > _slots.size())
		{
			return null;
		}
		return _slots.get(slotIndex - 1);
	}

	public List<PotentialSlotHolder> getAllSlots()
	{
		return _slots;
	}

	public PotentialRarityHolder getRarity(int level)
	{
		return _rarities.get(level);
	}

	public int getSkillRarity(int skillId)
	{
		for (PotentialSkillRangeHolder range : _skillRanges)
		{
			if (range.isInRange(skillId))
			{
				return range.getRarity();
			}
		}
		return 1; // 默認普通
	}

	public String getSkillColor(int skillId)
	{
		int rarity = getSkillRarity(skillId);
		PotentialRarityHolder rarityHolder = _rarities.get(rarity);
		return rarityHolder != null ? rarityHolder.getColor() : "FFFFFF";
	}

	public int getRandomSkillForSlot(int slotIndex)
	{
		PotentialSlotHolder slot = getSlot(slotIndex);
		if (slot == null)
		{
			return 0;
		}
		return Rnd.get(slot.getMinSkillId(), slot.getMaxSkillId());
	}

	public static PotentialData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final PotentialData INSTANCE = new PotentialData();
	}
}