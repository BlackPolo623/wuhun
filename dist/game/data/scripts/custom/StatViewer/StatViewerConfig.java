package custom.StatViewer;

import java.util.Arrays;
import java.util.List;

public class StatViewerConfig
{
	public static final int NPC_ID = 900051;
	public static final boolean REQUIRE_GM_FOR_OTHERS = false;

	public static class StatEntry
	{
		public final String statName;
		public final String displayName;
		public final boolean isPercent;

		public StatEntry(String statName, String displayName, boolean isPercent)
		{
			this.statName = statName;
			this.displayName = displayName;
			this.isPercent = isPercent;
		}
	}

	public static final List<StatEntry> STATS = Arrays.asList(
		new StatEntry("unlock_Limit",              "突破上限",        false),
		new StatEntry("FINAL_DAMAGE_RATE",          "最終傷害",        false),
		new StatEntry("FINAL_DAMAGE_REDUCE",        "最終減傷",        false),
		new StatEntry("IGNORE_FINAL_DAMAGE_REDUCE", "無視最終減傷",    false),
		new StatEntry("PVP_PHYSICAL_ATTACK_DAMAGE", "PVP物理傷害",     false),
		new StatEntry("PVP_MAGICAL_SKILL_DAMAGE",   "PVP魔法傷害",     false),
		new StatEntry("PVP_PHYSICAL_SKILL_DAMAGE",  "PVP物理技能傷害", false),
		new StatEntry("PVP_PHYSICAL_ATTACK_DEFENCE","PVP物理防禦",     false),
		new StatEntry("PVP_MAGICAL_SKILL_DEFENCE",  "PVP魔法防禦",     false),
		new StatEntry("PVP_PHYSICAL_SKILL_DEFENCE", "PVP物理技能防禦", false),
		new StatEntry("ABSORB_DAMAGE_CHANCE",       "吸血機率",        false)
	);
}
