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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.item.holders.ElementalSpiritTemplateHolder;

/**
 * @author JoeAlisson
 */
public class ElementalSpiritData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ElementalSpiritData.class.getName());
	
	public static final float FRAGMENT_XP_CONSUME = 50000.0f;
	public static final int TALENT_INIT_FEE = 50000;
	public static final int[] EXTRACT_FEES =
	{
		100000,
		200000,
		300000,
		600000,
		1500000
	};
	
	private static final Map<Byte, Map<Byte, ElementalSpiritTemplateHolder>> SPIRIT_DATA = new HashMap<>(4);
	
	protected ElementalSpiritData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("data/ElementalSpiritData.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + SPIRIT_DATA.size() + " elemental spirit templates.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", list -> forEach(list, "spirit", this::parseSpirit));
	}
	
	private void parseSpirit(Node spiritNode)
	{
		final NamedNodeMap attributes = spiritNode.getAttributes();
		final byte type = parseByte(attributes, "type");
		final byte stage = parseByte(attributes, "stage");
		final int npcId = parseInteger(attributes, "npcId");
		final int extractItem = parseInteger(attributes, "extractItem");
		final int maxCharacteristics = parseInteger(attributes, "maxCharacteristics");
		final ElementalSpiritTemplateHolder template = new ElementalSpiritTemplateHolder(type, stage, npcId, extractItem, maxCharacteristics);
		SPIRIT_DATA.computeIfAbsent(type, HashMap::new).put(stage, template);
		
		forEach(spiritNode, "level", levelNode ->
		{
			final NamedNodeMap levelInfo = levelNode.getAttributes();
			final int level = parseInteger(levelInfo, "id");
			final int attack = parseInteger(levelInfo, "atk");
			final int defense = parseInteger(levelInfo, "def");
			final int criticalRate = parseInteger(levelInfo, "critRate");
			final int criticalDamage = parseInteger(levelInfo, "critDam");
			final long maxExperience = parseLong(levelInfo, "maxExp");
			template.addLevelInfo(level, attack, defense, criticalRate, criticalDamage, maxExperience);
		});
		
		forEach(spiritNode, "itemToEvolve", itemNode ->
		{
			final NamedNodeMap itemInfo = itemNode.getAttributes();
			final int itemId = parseInteger(itemInfo, "id");
			final int count = parseInteger(itemInfo, "count", 1);
			template.addItemToEvolve(itemId, count);
		});
		
		forEach(spiritNode, "absorbItem", absorbItemNode ->
		{
			final NamedNodeMap absorbInfo = absorbItemNode.getAttributes();
			final int itemId = parseInteger(absorbInfo, "id");
			final int experience = parseInteger(absorbInfo, "experience");
			template.addAbsorbItem(itemId, experience);
		});
	}
	
	public ElementalSpiritTemplateHolder getSpirit(byte type, byte stage)
	{
		if (SPIRIT_DATA.containsKey(type))
		{
			return SPIRIT_DATA.get(type).get(stage);
		}
		
		return null;
	}
	
	public static ElementalSpiritData getInstance()
	{
		return Singleton.INSTANCE;
	}
	
	private static class Singleton
	{
		static final ElementalSpiritData INSTANCE = new ElementalSpiritData();
	}
}
