package custom.aggrolist;

import java.util.Set;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * 仇恨列表顯示系統
 * 當玩家攻擊指定 NPC 時，顯示該 NPC 的仇恨列表
 */
public class aggrolist extends Script
{
	// 配置參數
	private static final Set<Integer> TARGET_NPCS = Set.of(
			50001, 50002, 50003, 50004, 50005, 50006, 50007, 50008, 50009, 50010,
			50011, 50012, 50013, 50014, 50015, 50016, 50017, 50018, 50019, 50020,
			50021, 50022, 50023, 50024, 50025, 50026, 50027, 50028, 50029, 50030,
			50031, 50032, 50033, 50034, 50035
	);

	private static final String HTML_PATH = "data/scripts/custom/aggrolist/aggrolist.htm";

	// HTML 表格樣式
	private static final String TABLE_BG = "L2UI_CT1.Windows.Windows_DF_TooltipBG";
	private static final int TABLE_WIDTH = 277;
	private static final int TABLE_HEIGHT = 32;

	public aggrolist()
	{
		addAttackId(TARGET_NPCS);
	}

	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if (TARGET_NPCS.contains(npc.getId()))
		{
			sendAggroListView(attacker, npc);
		}
	}

	/**
	 * 發送仇恨列表視圖給玩家
	 */
	private void sendAggroListView(Player player, Npc npc)
	{
		if ((player == null) || (npc == null))
		{
			return;
		}

		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(player, HTML_PATH);
		html.replace("%aggrolist%", buildAggroListContent(npc));
		html.replace("%npc_name%", npc.getName());
		html.replace("%npcId%", String.valueOf(npc.getId()));
		html.replace("%objid%", String.valueOf(npc.getObjectId()));
		player.sendPacket(html);
	}

	/**
	 * 構建仇恨列表內容
	 */
	private String buildAggroListContent(Npc npc)
	{
		if (!npc.isAttackable())
		{
			return "<table width=" + TABLE_WIDTH + "><tr><td align=center>此 NPC 無仇恨列表</td></tr></table>";
		}

		final StringBuilder sb = new StringBuilder();
		npc.asAttackable().getAggroList().values().forEach(aggroInfo ->
		{
			final String attackerName = (aggroInfo.getAttacker() != null) ? aggroInfo.getAttacker().getName() : "未知";
			final long hate = aggroInfo.getHate();
			final long damage = aggroInfo.getDamage();

			sb.append("<table width=").append(TABLE_WIDTH).append(" height=").append(TABLE_HEIGHT);
			sb.append(" cellspacing=0 background=\"").append(TABLE_BG).append("\">");
			sb.append("<tr>");
			sb.append("<td width=110>").append(attackerName).append("</td>");
			sb.append("<td width=60 align=center>").append(hate).append("</td>");
			sb.append("<td width=60 align=center>").append(damage).append("</td>");
			sb.append("</tr></table>");
		});

		return sb.toString();
	}

	public static void main(String[] args)
	{
		new aggrolist();
		System.out.println("仇恨傷害列表顯示系統已載入");
	}
}