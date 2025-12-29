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
package org.l2jmobius.gameserver.model.vip;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

import org.l2jmobius.gameserver.config.VipSystemConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.VipData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLoad;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.variables.AccountVariables;
import org.l2jmobius.gameserver.network.serverpackets.ExBRNewIconCashBtnWnd;
import org.l2jmobius.gameserver.network.serverpackets.vip.ReceiveVipInfo;

/**
 * @author marciox25
 */
public class VipManager
{
	private static final byte VIP_MAX_TIER = (byte) VipSystemConfig.VIP_SYSTEM_MAX_TIER;
	
	private final ConsumerEventListener _vipLoginListener = new ConsumerEventListener(null, EventType.ON_PLAYER_LOGIN, (Consumer<OnPlayerLogin>) this::onVipLogin, this);
	
	protected VipManager()
	{
		if (!VipSystemConfig.VIP_SYSTEM_ENABLED)
		{
			return;
		}
		
		Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_PLAYER_LOAD, (Consumer<OnPlayerLoad>) this::onPlayerLoaded, this));
	}
	
	private void onPlayerLoaded(OnPlayerLoad event)
	{
		final Player player = event.getPlayer();
		player.setVipTier(getVipTier(player));
		if (player.getVipTier() > 0)
		{
			manageTier(player);
			player.addListener(_vipLoginListener);
		}
		else
		{
			player.sendPacket(new ReceiveVipInfo(player));
			player.sendPacket(new ExBRNewIconCashBtnWnd((byte) 0));
		}
	}
	
	private boolean canReceiveGift(Player player)
	{
		if (!VipSystemConfig.VIP_SYSTEM_ENABLED)
		{
			return false;
		}
		
		if (player.getVipTier() <= 0)
		{
			return false;
		}
		
		return player.getAccountVariables().getLong(AccountVariables.VIP_ITEM_BOUGHT, 0) <= 0;
	}
	
	private void onVipLogin(OnPlayerLogin event)
	{
		final Player player = event.getPlayer();
		if (canReceiveGift(player))
		{
			player.sendPacket(new ExBRNewIconCashBtnWnd((byte) 1));
		}
		else
		{
			player.sendPacket(new ExBRNewIconCashBtnWnd((byte) 0));
		}
		
		player.removeListener(_vipLoginListener);
		player.sendPacket(new ReceiveVipInfo(player));
	}
	
	public void manageTier(Player player)
	{
		if (!checkVipTierExpiration(player))
		{
			player.sendPacket(new ReceiveVipInfo(player));
		}
		
		if (player.getVipTier() > 1)
		{
			final int oldSkillId = VipData.getInstance().getSkillId((byte) (player.getVipTier() - 1));
			if (oldSkillId > 0)
			{
				final Skill oldSkill = SkillData.getInstance().getSkill(oldSkillId, 1);
				if (oldSkill != null)
				{
					player.removeSkill(oldSkill);
				}
			}
		}
		
		final int skillId = VipData.getInstance().getSkillId(player.getVipTier());
		if (skillId > 0)
		{
			final Skill skill = SkillData.getInstance().getSkill(skillId, 1);
			if (skill != null)
			{
				player.addSkill(skill);
			}
		}
	}
	
	public byte getVipTier(Player player)
	{
		return getVipInfo(player).getTier();
	}
	
	public byte getVipTier(long points)
	{
		byte temp = getVipInfo(points).getTier();
		if (temp > VIP_MAX_TIER)
		{
			temp = VIP_MAX_TIER;
		}
		
		return temp;
	}
	
	private VipInfo getVipInfo(Player player)
	{
		return getVipInfo(player.getVipPoints());
	}
	
	private VipInfo getVipInfo(long points)
	{
		for (byte i = 0; i < VipData.getInstance().getVipTiers().size(); i++)
		{
			if (points < VipData.getInstance().getVipTiers().get(i).getPointsRequired())
			{
				byte temp = (byte) (i - 1);
				if (temp > VIP_MAX_TIER)
				{
					temp = VIP_MAX_TIER;
				}
				
				return VipData.getInstance().getVipTiers().get(temp);
			}
		}
		
		return VipData.getInstance().getVipTiers().get(VIP_MAX_TIER);
	}
	
	public long getPointsDepreciatedOnLevel(byte vipTier)
	{
		final VipInfo tier = VipData.getInstance().getVipTiers().get(vipTier);
		if (tier == null)
		{
			return 0;
		}
		
		return tier.getPointsDepreciated();
	}
	
	public long getPointsToLevel(byte vipTier)
	{
		final VipInfo tier = VipData.getInstance().getVipTiers().get(vipTier);
		if (tier == null)
		{
			return 0;
		}
		
		return tier.getPointsRequired();
	}
	
	public boolean checkVipTierExpiration(Player player)
	{
		final Instant now = Instant.now();
		if (now.isAfter(Instant.ofEpochMilli(player.getVipTierExpiration())))
		{
			player.updateVipPoints(-getPointsDepreciatedOnLevel(player.getVipTier()));
			player.setVipTierExpiration(Instant.now().plus(30, ChronoUnit.DAYS).toEpochMilli());
			return true;
		}
		
		return false;
	}
	
	public static VipManager getInstance()
	{
		return Singleton.INSTANCE;
	}
	
	private static class Singleton
	{
		protected static final VipManager INSTANCE = new VipManager();
	}
}
