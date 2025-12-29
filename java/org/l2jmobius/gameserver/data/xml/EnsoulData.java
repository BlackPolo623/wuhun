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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.ensoul.EnsoulFee;
import org.l2jmobius.gameserver.model.ensoul.EnsoulOption;
import org.l2jmobius.gameserver.model.ensoul.EnsoulStone;
import org.l2jmobius.gameserver.model.item.EtcItem;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;

/**
 * @author UnAfraid
 */
public class EnsoulData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(EnsoulData.class.getName());
	
	private final Map<Integer, EnsoulFee> _ensoulFees = new ConcurrentHashMap<>();
	private final Map<Integer, EnsoulOption> _ensoulOptions = new ConcurrentHashMap<>();
	private final Map<Integer, EnsoulStone> _ensoulStones = new ConcurrentHashMap<>();
	
	protected EnsoulData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackDirectory("data/stats/ensoul", true);
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _ensoulFees.size() + " fees.");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _ensoulOptions.size() + " options.");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _ensoulStones.size() + " stones.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, IXmlReader::isNode, ensoulNode ->
		{
			switch (ensoulNode.getNodeName())
			{
				case "fee":
				{
					parseFees(ensoulNode);
					break;
				}
				case "option":
				{
					parseOptions(ensoulNode);
					break;
				}
				case "stone":
				{
					parseStones(ensoulNode);
					break;
				}
			}
		}));
	}
	
	private void parseFees(Node ensoulNode)
	{
		final Integer stoneId = parseInteger(ensoulNode.getAttributes(), "stoneId");
		final EnsoulFee fee = new EnsoulFee(stoneId);
		forEach(ensoulNode, IXmlReader::isNode, feeNode ->
		{
			switch (feeNode.getNodeName())
			{
				case "first":
				{
					parseFee(feeNode, fee, 0);
					break;
				}
				case "secondary":
				{
					parseFee(feeNode, fee, 1);
					break;
				}
				case "third":
				{
					parseFee(feeNode, fee, 2);
					break;
				}
				case "reNormal":
				{
					parseReFee(feeNode, fee, 0);
					break;
				}
				case "reSecondary":
				{
					parseReFee(feeNode, fee, 1);
					break;
				}
				case "reThird":
				{
					parseReFee(feeNode, fee, 2);
					break;
				}
				case "remove":
				{
					parseRemove(feeNode, fee);
					break;
				}
			}
		});
	}
	
	private void parseFee(Node ensoulNode, EnsoulFee fee, int index)
	{
		final NamedNodeMap attrs = ensoulNode.getAttributes();
		final int id = parseInteger(attrs, "itemId");
		final int count = parseInteger(attrs, "count");
		fee.setEnsoul(index, new ItemHolder(id, count));
		_ensoulFees.put(fee.getStoneId(), fee);
	}
	
	private void parseReFee(Node ensoulNode, EnsoulFee fee, int index)
	{
		final NamedNodeMap attrs = ensoulNode.getAttributes();
		final int id = parseInteger(attrs, "itemId");
		final int count = parseInteger(attrs, "count");
		fee.setResoul(index, new ItemHolder(id, count));
	}
	
	private void parseRemove(Node ensoulNode, EnsoulFee fee)
	{
		final NamedNodeMap attrs = ensoulNode.getAttributes();
		final int id = parseInteger(attrs, "itemId");
		final int count = parseInteger(attrs, "count");
		fee.addRemovalFee(new ItemHolder(id, count));
	}
	
	private void parseOptions(Node ensoulNode)
	{
		final NamedNodeMap attrs = ensoulNode.getAttributes();
		final int id = parseInteger(attrs, "id");
		final String name = parseString(attrs, "name");
		final String desc = parseString(attrs, "desc");
		final int skillId = parseInteger(attrs, "skillId");
		final int skillLevel = parseInteger(attrs, "skillLevel");
		final EnsoulOption option = new EnsoulOption(id, name, desc, skillId, skillLevel);
		_ensoulOptions.put(option.getId(), option);
	}
	
	private void parseStones(Node ensoulNode)
	{
		final NamedNodeMap attrs = ensoulNode.getAttributes();
		final int id = parseInteger(attrs, "id");
		final int slotType = parseInteger(attrs, "slotType");
		final EnsoulStone stone = new EnsoulStone(id, slotType);
		forEach(ensoulNode, "option", optionNode -> stone.addOption(parseInteger(optionNode.getAttributes(), "id")));
		_ensoulStones.put(stone.getId(), stone);
		((EtcItem) ItemData.getInstance().getTemplate(stone.getId())).setEnsoulStone();
	}
	
	/**
	 * Retrieves the ensoul fee for a specified stone ID and index.
	 * @param stoneId the ID of the stone.
	 * @param index the index of the ensoul fee within the fee structure.
	 * @return the {@link ItemHolder} representing the ensoul fee, or {@code null} if none is found.
	 */
	public ItemHolder getEnsoulFee(int stoneId, int index)
	{
		final EnsoulFee fee = _ensoulFees.get(stoneId);
		return fee != null ? fee.getEnsoul(index) : null;
	}
	
	/**
	 * Retrieves the resoul fee for a specified stone ID and index.
	 * @param stoneId the ID of the stone.
	 * @param index the index of the resoul fee within the fee structure.
	 * @return the {@link ItemHolder} representing the resoul fee, or {@code null} if none is found.
	 */
	public ItemHolder getResoulFee(int stoneId, int index)
	{
		final EnsoulFee fee = _ensoulFees.get(stoneId);
		return fee != null ? fee.getResoul(index) : null;
	}
	
	/**
	 * Retrieves the removal fee for a specified stone ID.
	 * @param stoneId the ID of the stone.
	 * @return a collection of {@link ItemHolder} representing the removal fees, or an empty collection if none are found.
	 */
	public Collection<ItemHolder> getRemovalFee(int stoneId)
	{
		final EnsoulFee fee = _ensoulFees.get(stoneId);
		return fee != null ? fee.getRemovalFee() : Collections.emptyList();
	}
	
	/**
	 * Retrieves the ensoul option associated with the specified option ID.
	 * @param id the ID of the ensoul option.
	 * @return the {@link EnsoulOption} for the specified ID, or {@code null} if none exists.
	 */
	public EnsoulOption getOption(int id)
	{
		return _ensoulOptions.get(id);
	}
	
	/**
	 * Retrieves the ensoul stone associated with the specified stone ID.
	 * @param id the ID of the ensoul stone.
	 * @return the {@link EnsoulStone} for the specified ID, or {@code null} if none exists.
	 */
	public EnsoulStone getStone(int id)
	{
		return _ensoulStones.get(id);
	}
	
	/**
	 * Searches for an ensoul stone that matches the specified slot type and option ID.
	 * @param type the slot type of the stone.
	 * @param optionId the option ID to match.
	 * @return the ID of the matching ensoul stone, or 0 if no matching stone is found.
	 */
	public int getStone(int type, int optionId)
	{
		for (EnsoulStone stone : _ensoulStones.values())
		{
			if (stone.getSlotType() == type)
			{
				for (int id : stone.getOptions())
				{
					if (id == optionId)
					{
						return stone.getId();
					}
				}
			}
		}
		
		return 0;
	}
	
	public static EnsoulData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EnsoulData INSTANCE = new EnsoulData();
	}
}
