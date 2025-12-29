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
package instances.LeonasDungeon;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.managers.events.LeonasDungeonManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.item.enums.BodyPart;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.script.InstanceScript;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExSendUIEvent;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.OnEventTrigger;

/**
 * @author Serenitty, Mobius
 */
public class LeonasDungeon extends InstanceScript
{
	// NPC
	private static final int LEONA = 34357;
	
	// Items
	private static final int LEONA_WEAPON_SPEAR = 97508;
	private static final int LEONA_WEAPON_GUN = 97509;
	
	// Skills
	private final SkillHolder FORCE_BUFF = new SkillHolder(4647, 4);
	private final SkillHolder FORCE_DEBUFF = new SkillHolder(48403, 2);
	private final SkillHolder[] LEONA_BUFFS =
	{
		new SkillHolder(48640, 4), // Leona's Blessing - Focus
		new SkillHolder(48641, 4), // Leona's Blessing - Death Whisper
		new SkillHolder(48643, 3), // Leona's Blessing - Haste
		new SkillHolder(48645, 4), // Leona's Blessing - Might
		new SkillHolder(48647, 4), // Leona's Blessing - Shield
		new SkillHolder(48649, 3), // Leona's Blessing - Wind Walk
		new SkillHolder(48651, 3), // Leona's Blessing - Berserker Spirit
		new SkillHolder(48652, 2), // Leona's Blessing - HP Recovery
		new SkillHolder(48653, 2), // Leona's Blessing - MP Recovery
		new SkillHolder(48836, 1), // Leona's XP Blessing
	};
	private static final SkillHolder TRANSFORM_SKILL = new SkillHolder(48634, 1); // Transformation - Leona Blackbird
	// Misc
	private static final int EVENT_TRIGGER_LEONAS_AREA = 18108866;
	private static final int TEMPLATE_ID = 236;
	
	private LeonasDungeon()
	{
		super(TEMPLATE_ID);
		
		addFirstTalkId(LEONA);
		addTalkId(LEONA);
		addEnterZoneId(EVENT_TRIGGER_LEONAS_AREA);
		addInstanceEnterId(TEMPLATE_ID);
		addInstanceLeaveId(TEMPLATE_ID);
		addInstanceDestroyId(TEMPLATE_ID);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final String html;
		switch (event)
		{
			case "enterInstance":
			{
				if (player.isInParty())
				{
					final Party party = player.getParty();
					final List<Player> members = party.getMembers();
					for (Player member : members)
					{
						if (!member.isInsideRadius3D(npc, 1000))
						{
							player.sendMessage("Player " + member.getName() + " must go closer.");
						}
						
						enterInstance(member, npc, TEMPLATE_ID);
					}
				}
				else if (player.isGM())
				{
					enterInstance(player, npc, TEMPLATE_ID);
					player.sendMessage("SYS: You have entered as GM/Admin to Leona Dungeon.");
				}
				else
				{
					player.sendMessage("You must come with a party.");
				}
				break;
			}
			case "setDifficulty":
			{
				final Instance instance = player.getInstanceWorld();
				final int difficulty = instance.getParameters().getInt("INSTANCE_DIFFICULTY", 0);
				if (difficulty == 0)
				{
					return "setDifficulty.htm";
				}
				
				final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
				packet.setHtml(getHtm(player, "setDifficulty.htm"));
				switch (difficulty)
				{
					case 1:
					{
						packet.replace("<Button ALIGN=LEFT ICON=\"NORMAL\" action=\"bypass -h Quest LeonasDungeon setDifficulty1\">Difficulty - Low</Button>", "");
						break;
					}
					case 2:
					{
						packet.replace("<Button ALIGN=LEFT ICON=\"NORMAL\" action=\"bypass -h Quest LeonasDungeon setDifficulty2\">Difficulty - Medium</Button>", "");
						break;
					}
					case 3:
					{
						packet.replace("<Button ALIGN=LEFT ICON=\"NORMAL\" action=\"bypass -h Quest LeonasDungeon setDifficulty3\">Difficulty - High</Button>", "");
						break;
					}
				}
				
				player.sendPacket(packet);
				return null;
			}
			case "getBuff":
			{
				final Instance instance = player.getInstanceWorld();
				if (instance != null)
				{
					if ((player.getInventory().getItemByItemId(57) != null) && (player.getInventory().getItemByItemId(57).getCount() >= 50000))
					{
						if (player.destroyItemByItemId(ItemProcessType.FEE, 57, 50000, player, true))
						{
							for (SkillHolder holder : LEONA_BUFFS)
							{
								if (holder.getSkillId() == 48836) // Leona's XP Blessing
								{
									final Skill randomXpBlessing = SkillData.getInstance().getSkill(48836, Rnd.get(1, 4));
									if (randomXpBlessing != null)
									{
										randomXpBlessing.applyEffects(npc, player);
									}
								}
								else
								{
									holder.getSkill().applyEffects(npc, player);
								}
							}
						}
					}
					else
					{
						player.sendMessage("You have not enough adena.");
					}
				}
				break;
			}
			case "getTransform":
			{
				final Instance instance = player.getInstanceWorld();
				if (instance != null)
				{
					if (player.getInventory().isItemEquipped(LEONA_WEAPON_SPEAR) || (player.getInventory().isItemEquipped(LEONA_WEAPON_GUN)))
					{
						player.doCast(TRANSFORM_SKILL.getSkill());
					}
					else
					{
						return "notWeapon.htm";
					}
				}
				break;
			}
			case "getWeapon":
			{
				final Instance instance = player.getInstanceWorld();
				if (instance != null)
				{
					final boolean hasItem = (player.getInventory().getItemByItemId(LEONA_WEAPON_GUN) != null) || (player.getInventory().getItemByItemId(LEONA_WEAPON_SPEAR) != null);
					if (!hasItem)
					{
						player.addItem(ItemProcessType.REWARD, player.getRace() == Race.SYLPH ? LEONA_WEAPON_GUN : LEONA_WEAPON_SPEAR, 1, player, true);
						html = "gotWeapon.htm";
					}
					else
					{
						html = "haveWeapon.htm";
					}
					
					return html;
				}
				break;
			}
			case "setDifficulty1":
			{
				final Instance instance = player.getInstanceWorld();
				final int difficulty = instance.getParameters().getInt("INSTANCE_DIFFICULTY", 0);
				instance.getParameters().set("INSTANCE_DIFFICULTY", 1);
				instance.getParameters().set("INSTANCE_DIFFICULTY_LOCK_TIME", System.currentTimeMillis() + 60000);
				instance.despawnGroup("Mobs_" + difficulty);
				
				instance.spawnGroup("MOBS_1");
				if (!instance.getParameters().getBoolean("PlayerEnter", false))
				{
					instance.setParameter("PlayerEnter", true);
					instance.setDuration(30);
					startEvent(npc, player);
				}
				break;
			}
			case "setDifficulty2":
			{
				final Instance instance = player.getInstanceWorld();
				final int difficulty = instance.getParameters().getInt("INSTANCE_DIFFICULTY", 0);
				instance.getParameters().set("INSTANCE_DIFFICULTY", 2);
				instance.getParameters().set("INSTANCE_DIFFICULTY_LOCK_TIME", System.currentTimeMillis() + 60000);
				instance.despawnGroup("Mobs_" + difficulty);
				
				instance.spawnGroup("MOBS_2");
				if (!instance.getParameters().getBoolean("PlayerEnter", false))
				{
					instance.setParameter("PlayerEnter", true);
					instance.setDuration(30);
					startEvent(npc, player);
				}
				break;
			}
			case "setDifficulty3":
			{
				final Instance instance = player.getInstanceWorld();
				final int difficulty = instance.getParameters().getInt("INSTANCE_DIFFICULTY", 0);
				instance.getParameters().set("INSTANCE_DIFFICULTY", 3);
				instance.getParameters().set("INSTANCE_DIFFICULTY_LOCK_TIME", System.currentTimeMillis() + 60000);
				instance.despawnGroup("Mobs_" + difficulty);
				
				instance.spawnGroup("MOBS_3");
				if (!instance.getParameters().getBoolean("PlayerEnter", false))
				{
					instance.setParameter("PlayerEnter", true);
					instance.setDuration(30);
					startEvent(npc, player);
				}
				break;
			}
			case "exitInstance":
			{
				final Instance instance = player.getInstanceWorld();
				if (instance != null)
				{
					instance.ejectPlayer(player);
				}
				break;
			}
		}
		
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance instance = npc.getInstanceWorld();
		if (instance == null)
		{
			return null;
		}
		
		final long lockTime = instance.getParameters().getLong("INSTANCE_DIFFICULTY_LOCK_TIME", 0);
		if (lockTime > System.currentTimeMillis())
		{
			return "34357-03.htm";
		}
		
		final boolean instanceStarted = instance.getParameters().getBoolean("PlayerEnter", false);
		return instanceStarted ? "34357-02.htm" : "34357-01.htm";
	}
	
	private void startEvent(Npc npc, Player player)
	{
		final Instance instance = player.getInstanceWorld();
		instance.setParameter("Leona_Running", true);
		player.getInstanceWorld().broadcastPacket(new ExSendUIEvent(player, false, false, (int) (instance.getRemainingTime() / 1000), 0, NpcStringId.TIME_LEFT));
		instance.broadcastPacket(new OnEventTrigger(EVENT_TRIGGER_LEONAS_AREA, true));
		if (npc.getId() == LEONA)
		{
			npc.setDisplayEffect(2);
		}
		
		final ScheduledFuture<?> scheduledTask = ThreadPool.scheduleAtFixedRate(() ->
		{
			final Collection<Player> members = instance.getPlayers();
			for (Player member : members)
			{
				LeonasDungeonManager.getInstance().addPointsForPlayer(member, 60);
			}
		}, 60000, 60000);
		
		instance.setParameter("RankingPointTask", scheduledTask);
	}
	
	@Override
	public void onEnterZone(Creature creature, ZoneType zone)
	{
		final Instance instance = creature.getInstanceWorld();
		if ((instance != null) && creature.isPlayer())
		{
			FORCE_BUFF.getSkill().applyEffects(creature, creature);
		}
		else
		{
			FORCE_DEBUFF.getSkill().applyEffects(creature, creature);
		}
	}
	
	@Override
	public void onInstanceEnter(Player player, Instance instance)
	{
		final boolean running = instance.getParameters().getBoolean("Leona_Running", false);
		if ((instance.getRemainingTime() > 0) && running)
		{
			player.sendPacket(new ExSendUIEvent(player, false, false, (int) (instance.getRemainingTime() / 1000), 0, NpcStringId.TIME_LEFT));
		}
	}
	
	@Override
	public void onInstanceLeave(Player player, Instance instance)
	{
		player.sendPacket(new ExSendUIEvent(player, false, false, 0, 0, NpcStringId.TIME_LEFT));
		
		if (player.isTransformed())
		{
			player.untransform();
		}
		
		player.stopAllEffects();
		
		final Item itemToDisarm = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (itemToDisarm == null)
		{
			return;
		}
		
		if ((itemToDisarm == player.getInventory().getItemByItemId(LEONA_WEAPON_SPEAR)) || (itemToDisarm == player.getInventory().getItemByItemId(LEONA_WEAPON_GUN)))
		{
			final BodyPart bodyPart = BodyPart.fromItem(itemToDisarm);
			player.getInventory().unEquipItemInBodySlot(bodyPart);
			
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(itemToDisarm);
			player.sendInventoryUpdate(iu);
			player.broadcastUserInfo();
		}
	}
	
	@Override
	public void onInstanceDestroy(Instance instance)
	{
		final ScheduledFuture<?> task = instance.getParameters().getObject("RankingPointTask", ScheduledFuture.class);
		if ((task != null) && !task.isDone())
		{
			task.cancel(true);
		}
		
		instance.setParameter("RankingPointTask", null);
	}
	
	public static void main(String[] args)
	{
		new LeonasDungeon();
	}
}
