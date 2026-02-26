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
package handlers.voicedcommandhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.custom.WeddingConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.managers.CoupleManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerAction;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.serverpackets.ConfirmDlg;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.SetupGauge;

/**
 * Wedding voiced commands handler.
 * @author evill33t
 */
public class Wedding implements IVoicedCommandHandler
{
	static final Logger LOGGER = Logger.getLogger(Wedding.class.getName());
	private static final String[] _voicedCommands =
	{
		"離婚",
		"訂婚",
		"尋找愛人"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar, String params)
	{
		if (activeChar == null)
		{
			return false;
		}

		if (command.startsWith("訂婚"))
		{
			return engage(activeChar);
		}
		else if (command.startsWith("離婚"))
		{
			return divorce(activeChar);
		}
		else if (command.startsWith("尋找愛人"))
		{
			return goToLove(activeChar);
		}

		return false;
	}
	
	public boolean divorce(Player activeChar)
	{
		if (activeChar.getPartnerId() == 0)
		{
			return false;
		}

		final int partnerId = activeChar.getPartnerId();
		final int coupleId = activeChar.getCoupleId();
		long adenaAmount = 0;
		if (activeChar.isMarried())
		{
			activeChar.sendMessage("你現在已經離婚了。");
			adenaAmount = (activeChar.getAdena() / 100) * WeddingConfig.WEDDING_DIVORCE_COSTS;
			activeChar.getInventory().reduceAdena(ItemProcessType.FEE, adenaAmount, activeChar, null);
		}
		else
		{
			activeChar.sendMessage("你已經解除了訂婚關係。");
		}

		final Player partner = World.getInstance().getPlayer(partnerId);
		if (partner != null)
		{
			partner.setPartnerId(0);
			if (partner.isMarried())
			{
				partner.sendMessage("你的配偶決定與你離婚。");
			}
			else
			{
				partner.sendMessage("你的未婚夫/妻決定解除與你的訂婚。");
			}

			// give adena
			if (adenaAmount > 0)
			{
				partner.addAdena(ItemProcessType.REFUND, adenaAmount, null, false);
			}
		}

		CoupleManager.getInstance().deleteCouple(coupleId);
		return true;
	}
	
	public boolean engage(Player activeChar)
	{
		if (activeChar.getTarget() == null)
		{
			activeChar.sendMessage("你沒有選定目標。");
			return false;
		}
		else if (!activeChar.getTarget().isPlayer())
		{
			activeChar.sendMessage("你只能向其他玩家求婚。");
			return false;
		}
		else if (activeChar.getPartnerId() != 0)
		{
			activeChar.sendMessage("你已經訂婚了。");
			if (WeddingConfig.WEDDING_PUNISH_INFIDELITY)
			{
				activeChar.getEffectList().startAbnormalVisualEffect(AbnormalVisualEffect.BIG_HEAD); // Give player a Big Head

				// lets recycle the sevensigns debuffs
				int skillId;
				int skillLevel = 1;
				if (activeChar.getLevel() > 40)
				{
					skillLevel = 2;
				}

				if (activeChar.isMageClass())
				{
					skillId = 4362;
				}
				else
				{
					skillId = 4361;
				}

				final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
				if (!activeChar.isAffectedBySkill(skillId))
				{
					skill.applyEffects(activeChar, activeChar);
				}
			}

			return false;
		}

		final Player ptarget = activeChar.getTarget().asPlayer();

		// check if player target himself
		if (ptarget.getObjectId() == activeChar.getObjectId())
		{
			activeChar.sendMessage("你是不是哪裡不對勁？你想和自己訂婚嗎？");
			return false;
		}

		if (ptarget.isMarried())
		{
			activeChar.sendMessage("該玩家已經結婚了。");
			return false;
		}

		if (ptarget.isEngageRequest())
		{
			activeChar.sendMessage("該玩家已經被其他人求婚了。");
			return false;
		}

		if (ptarget.getPartnerId() != 0)
		{
			activeChar.sendMessage("該玩家已經和其他人訂婚了。");
			return false;
		}

		if ((ptarget.getAppearance().isFemale() == activeChar.getAppearance().isFemale()) && !WeddingConfig.WEDDING_SAMESEX)
		{
			activeChar.sendMessage("本伺服器不允許同性婚姻！");
			return false;
		}

		// check if target has player on friendlist
		boolean foundOnFriendList = false;
		int objectId;
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement statement = con.prepareStatement("SELECT friendId FROM character_friends WHERE charId=?");
			statement.setInt(1, ptarget.getObjectId());
			final ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				objectId = rset.getInt("friendId");
				if (objectId == activeChar.getObjectId())
				{
					foundOnFriendList = true;
				}
			}

			statement.close();
		}
		catch (Exception e)
		{
			LOGGER.warning("could not read friend data:" + e);
		}

		if (!foundOnFriendList)
		{
			activeChar.sendMessage("你想求婚的玩家不在你的好友名單中，你們必須先互相加為好友才能訂婚。");
			return false;
		}

		ptarget.setEngageRequest(true, activeChar.getObjectId());
		ptarget.addAction(PlayerAction.USER_ENGAGE);

		final ConfirmDlg dlg = new ConfirmDlg(activeChar.getName() + " 向你求婚。你願意開始一段新的關係嗎？");
		dlg.addTime(15 * 1000);
		ptarget.sendPacket(dlg);
		return true;
	}
	
	public boolean goToLove(Player activeChar)
	{
		final int teleportTimer = WeddingConfig.WEDDING_TELEPORT_DURATION * 1000;

		if (!WeddingConfig.WEDDING_TELEPORT)
		{
			activeChar.sendMessage("「傳送到愛人身邊」功能已被停用。");
			return false;
		}

		if (!activeChar.isMarried())
		{
			activeChar.sendMessage("你還沒有結婚。");
			return false;
		}

		if (activeChar.getPartnerId() == 0)
		{
			activeChar.sendMessage("在資料庫中找不到你的配偶 - 請通知管理員。");
			LOGGER.severe("Married but couldn't find parter for " + activeChar.getName());
			return false;
		}

		if (activeChar.isCombatFlagEquipped())
		{
			activeChar.sendMessage("當你持有戰旗或領地旗幟時，無法傳送到愛人身邊！");
			return false;
		}

		if (activeChar.isCursedWeaponEquipped())
		{
			activeChar.sendMessage("當你持有詛咒武器時，無法傳送到愛人身邊！");
			return false;
		}

		if (activeChar.isJailed())
		{
			activeChar.sendMessage("你在監獄中！");
			return false;
		}

		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendMessage("你正在參加奧林匹亞競技場。");
			return false;
		}

		if (activeChar.isRegisteredOnEvent())
		{
			activeChar.sendMessage("你已經註冊了活動。");
			return false;
		}

		if (activeChar.isInDuel())
		{
			activeChar.sendMessage("你正在決鬥中！");
			return false;
		}

		if (activeChar.inObserverMode())
		{
			activeChar.sendMessage("你正在觀察模式中。");
			return false;
		}

		if ((SiegeManager.getInstance().getSiege(activeChar) != null) && SiegeManager.getInstance().getSiege(activeChar).isInProgress())
		{
			activeChar.sendMessage("你正在攻城戰中，無法傳送到配偶身邊。");
			return false;
		}

		if (activeChar.isInsideZone(ZoneId.NO_SUMMON_FRIEND))
		{
			activeChar.sendMessage("你在禁止召喚的區域中。");
			return false;
		}

		final Player partner = World.getInstance().getPlayer(activeChar.getPartnerId());

		if ((partner == null) || !partner.isOnline())
		{
			activeChar.sendMessage("你的配偶不在線上。");
			return false;
		}

		if (activeChar.getInstanceId() != partner.getInstanceId())
		{
			activeChar.sendMessage("你的配偶在另一個世界中！");
			return false;
		}

		if (partner.isJailed())
		{
			activeChar.sendMessage("你的配偶在監獄中。");
			return false;
		}

		if (partner.isCursedWeaponEquipped())
		{
			activeChar.sendMessage("你的配偶持有詛咒武器，你無法傳送到愛人身邊！");
			return false;
		}

		if (partner.isInOlympiadMode())
		{
			activeChar.sendMessage("你的配偶正在參加奧林匹亞競技場。");
			return false;
		}

		if (partner.isRegisteredOnEvent())
		{
			activeChar.sendMessage("你的配偶已經註冊了活動。");
			return false;
		}

		if (partner.isInDuel())
		{
			activeChar.sendMessage("你的配偶正在決鬥中。");
			return false;
		}

		if (partner.inObserverMode())
		{
			activeChar.sendMessage("你的配偶正在觀察模式中。");
			return false;
		}

		if ((SiegeManager.getInstance().getSiege(partner) != null) && SiegeManager.getInstance().getSiege(partner).isInProgress())
		{
			activeChar.sendMessage("你的配偶正在攻城戰中，你無法傳送到配偶身邊。");
			return false;
		}

		if (partner.isInsideZone(ZoneId.NO_SUMMON_FRIEND))
		{
			activeChar.sendMessage("你的配偶在禁止召喚的區域中。");
			return false;
		}

		if (!activeChar.reduceAdena(ItemProcessType.FEE, WeddingConfig.WEDDING_TELEPORT_PRICE, null, true))
		{
			return false; // Player already informed by system message inside reduceAdena.
		}

		String formattedTime = TimeUtil.formatDuration(teleportTimer);
		activeChar.sendMessage("你將在 " + formattedTime + " 後傳送到配偶身邊。");

		activeChar.getAI().setIntention(Intention.IDLE);

		// SoE Animation section.
		activeChar.setTarget(activeChar);
		activeChar.disableAllSkills();

		activeChar.broadcastSkillPacket(new MagicSkillUse(activeChar, 1050, 1, teleportTimer, 0), activeChar);
		activeChar.sendPacket(new SetupGauge(activeChar.getObjectId(), 0, teleportTimer));
		// End SoE Animation section.

		final EscapeFinalizer ef = new EscapeFinalizer(activeChar, partner.getLocation());

		// continue execution later
		// activeChar.setSkillCast(ThreadPool.schedule(ef, teleportTimer));
		// activeChar.forceIsCasting(GameTimeTaskManager.getInstance().getGameTicks() + (teleportTimer / GameTimeTaskManager.MILLIS_IN_TICK));
		ThreadPool.schedule(ef, teleportTimer);
		return true;
	}
	
	private static class EscapeFinalizer implements Runnable
	{
		private final Player _player;
		private final Location _partnerLoc;
		
		EscapeFinalizer(Player activeChar, Location loc)
		{
			_player = activeChar;
			_partnerLoc = loc;
		}
		
		@Override
		public void run()
		{
			if (_player.isDead())
			{
				return;
			}

			if ((SiegeManager.getInstance().getSiege(_partnerLoc) != null) && SiegeManager.getInstance().getSiege(_partnerLoc).isInProgress())
			{
				_player.sendMessage("你的配偶正在攻城戰中，你無法傳送到配偶身邊。");
				return;
			}

			_player.enableAllSkills();

			try
			{
				_player.teleToLocation(_partnerLoc);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "", e);
			}
		}
	}
	
	@Override
	public String[] getCommandList()
	{
		return _voicedCommands;
	}
}
