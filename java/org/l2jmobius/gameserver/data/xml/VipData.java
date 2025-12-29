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
import org.l2jmobius.gameserver.config.VipSystemConfig;
import org.l2jmobius.gameserver.model.vip.VipInfo;

/**
 * @author marciox25
 */
public class VipData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(VipData.class.getName());
	
	private final Map<Byte, VipInfo> _vipTiers = new HashMap<>();
	
	protected VipData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		if (!VipSystemConfig.VIP_SYSTEM_ENABLED)
		{
			return;
		}
		
		_vipTiers.clear();
		parseDatapackFile("data/Vip.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _vipTiers.size() + " vips.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				VIP_FILE: for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("vip".equalsIgnoreCase(d.getNodeName()))
					{
						final NamedNodeMap attrs = d.getAttributes();
						Node att;
						byte tier = -1;
						int required = -1;
						int lose = -1;
						
						att = attrs.getNamedItem("tier");
						if (att == null)
						{
							LOGGER.severe(getClass().getSimpleName() + ": Missing tier for vip, skipping");
							continue;
						}
						
						tier = Byte.parseByte(att.getNodeValue());
						
						att = attrs.getNamedItem("points-required");
						if (att == null)
						{
							LOGGER.severe(getClass().getSimpleName() + ": Missing points-required for vip: " + tier + ", skipping");
							continue;
						}
						
						required = Integer.parseInt(att.getNodeValue());
						
						att = attrs.getNamedItem("points-lose");
						if (att == null)
						{
							LOGGER.severe(getClass().getSimpleName() + ": Missing points-lose for vip: " + tier + ", skipping");
							continue;
						}
						
						lose = Integer.parseInt(att.getNodeValue());
						
						final VipInfo vipInfo = new VipInfo(tier, required, lose);
						for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
						{
							if ("bonus".equalsIgnoreCase(c.getNodeName()))
							{
								final int skill = Integer.parseInt(c.getAttributes().getNamedItem("skill").getNodeValue());
								try
								{
									vipInfo.setSkill(skill);
								}
								catch (Exception e)
								{
									LOGGER.severe(getClass().getSimpleName() + ": Error in bonus parameter for vip: " + tier + ", skipping");
									continue VIP_FILE;
								}
							}
						}
						
						_vipTiers.put(tier, vipInfo);
					}
				}
			}
		}
	}
	
	/**
	 * Gets the single instance of VipData.
	 * @return single instance of VipData
	 */
	public static VipData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	/**
	 * The Class SingletonHolder.
	 */
	private static class SingletonHolder
	{
		protected static final VipData INSTANCE = new VipData();
	}
	
	public int getSkillId(byte tier)
	{
		return _vipTiers.get(tier).getSkill();
	}
	
	public Map<Byte, VipInfo> getVipTiers()
	{
		return _vipTiers;
	}
}
