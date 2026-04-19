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
package org.l2jmobius.gameserver.model.morph;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.stats.Stat;

/**
 * 變身系統單條屬性加成條目。 operation： ADD = 固定數值加成（FuncAdd），如 value=500 → +500 MUL = 百分比乘算（FuncMul），如 value=10 → +10%（factor=1.10） 支持屬性名（XML stat name）同時接受英文和中文寫法， 显示時統一使用 {@link #getDisplayName()} 返回中文名。
 * @author Custom
 */
public class MorphStatEntry
{
	// ── 加成方式枚舉 ──────────────────────────────────────────────────────

	public enum Operation
	{
		ADD,
		MUL
	}

	// ── XML stat name → Stat 枚舉（英文 + 中文 雙寫法支持）─────────────────

	/**
	 * XML 中 {@code name} 屬性 → {@link Stat} 枚舉映射。 同時收錄英文簡寫和中文寫法，解析時兩種都能識別。
	 */
	public static final Map<String, Stat> STAT_PARSE_MAP;
	static
	{
		final Map<String, Stat> m = new HashMap<>();

		// ── 攻防基礎 ──────────────────────────────────────────────────────
		// 物理攻擊
		m.put("pAtk", Stat.PHYSICAL_ATTACK);
		m.put("物理攻擊", Stat.PHYSICAL_ATTACK);
		// 魔法攻擊
		m.put("mAtk", Stat.MAGIC_ATTACK);
		m.put("魔法攻擊", Stat.MAGIC_ATTACK);
		// 物理防禦
		m.put("pDef", Stat.PHYSICAL_DEFENCE);
		m.put("物理防禦", Stat.PHYSICAL_DEFENCE);
		// 魔法防禦
		m.put("mDef", Stat.MAGICAL_DEFENCE);
		m.put("魔法防禦", Stat.MAGICAL_DEFENCE);
		// 物理技能威力
		m.put("pSkillPow", Stat.PHYSICAL_SKILL_POWER);
		m.put("物理技能威力", Stat.PHYSICAL_SKILL_POWER);
		// 魔法技能威力
		m.put("mSkillPow", Stat.MAGICAL_SKILL_POWER);
		m.put("魔法技能威力", Stat.MAGICAL_SKILL_POWER);

		// ── 速度 ──────────────────────────────────────────────────────────
		// 攻擊速度（物理）— 正確枚舉名：POWER_ATTACK_SPEED
		m.put("atkSpd", Stat.PHYSICAL_ATTACK_SPEED);
		m.put("攻擊速度", Stat.PHYSICAL_ATTACK_SPEED);
		// 施法速度
		m.put("mAtkSpd", Stat.MAGIC_ATTACK_SPEED);
		m.put("施法速度", Stat.MAGIC_ATTACK_SPEED);
		// 移動速度 — 正確枚舉名：MOVE_SPEED
		m.put("speed", Stat.MOVE_SPEED);
		m.put("moveSpd", Stat.MOVE_SPEED);
		m.put("移動速度", Stat.MOVE_SPEED);

		// ── 暴擊 ──────────────────────────────────────────────────────────
		// 暴擊率
		m.put("critRate", Stat.CRITICAL_RATE);
		m.put("暴擊率", Stat.CRITICAL_RATE);
		// 暴擊傷害
		m.put("critDmg", Stat.CRITICAL_DAMAGE);
		m.put("暴擊傷害", Stat.CRITICAL_DAMAGE);
		// 魔法暴擊率
		m.put("mCritRate", Stat.MAGIC_CRITICAL_RATE);
		m.put("魔法暴擊率", Stat.MAGIC_CRITICAL_RATE);
		// 魔法暴擊傷害
		m.put("mCritPower", Stat.MAGIC_CRITICAL_DAMAGE);
		m.put("魔法暴擊傷害", Stat.MAGIC_CRITICAL_DAMAGE);

		// ── HP / MP / CP ──────────────────────────────────────────────────
		m.put("maxHp", Stat.MAX_HP);
		m.put("最大HP", Stat.MAX_HP);
		m.put("maxMp", Stat.MAX_MP);
		m.put("最大MP", Stat.MAX_MP);
		m.put("maxCp", Stat.MAX_CP);
		m.put("最大CP", Stat.MAX_CP);
		// HP回復
		m.put("hpRegen", Stat.REGENERATE_HP_RATE);
		m.put("HP回復", Stat.REGENERATE_HP_RATE);
		// MP回復
		m.put("mpRegen", Stat.REGENERATE_MP_RATE);
		m.put("MP回復", Stat.REGENERATE_MP_RATE);
		// CP回復
		m.put("cpRegen", Stat.REGENERATE_CP_RATE);
		m.put("CP回復", Stat.REGENERATE_CP_RATE);
		// 治療效果
		m.put("healEffect", Stat.HEAL_EFFECT);
		m.put("治療效果", Stat.HEAL_EFFECT);

		// ── 命中 / 閃避 ────────────────────────────────────────────────────
		m.put("accuracy", Stat.ACCURACY_COMBAT);
		m.put("命中", Stat.ACCURACY_COMBAT);
		m.put("evasion", Stat.EVASION_RATE);
		m.put("閃避", Stat.EVASION_RATE);

		// ── PVP 傷害 — 正確枚舉名：PVP_PHYSICAL_DMG / PVP_MAGICAL_DMG ─────
		m.put("pvpPDmg", Stat.PVP_PHYSICAL_ATTACK_DAMAGE);
		m.put("PVP物理傷害", Stat.PVP_PHYSICAL_ATTACK_DAMAGE);
		m.put("pvpMDmg", Stat.PVP_MAGICAL_SKILL_DAMAGE);
		m.put("PVP魔法傷害", Stat.PVP_MAGICAL_SKILL_DAMAGE);
		m.put("pvpPSkillDmg", Stat.PVP_PHYSICAL_SKILL_DAMAGE);
		m.put("PVP物理技能傷害", Stat.PVP_PHYSICAL_SKILL_DAMAGE);
		// PVP 防禦
		m.put("pvpPDef", Stat.PVP_PHYSICAL_ATTACK_DEFENCE);
		m.put("PVP物理防禦", Stat.PVP_PHYSICAL_ATTACK_DEFENCE);
		m.put("pvpMDef", Stat.PVP_MAGICAL_SKILL_DEFENCE);
		m.put("PVP魔法防禦", Stat.PVP_MAGICAL_SKILL_DEFENCE);
		m.put("pvpSkillDef", Stat.PVP_PHYSICAL_SKILL_DEFENCE);
		m.put("PVP物理技能防禦", Stat.PVP_PHYSICAL_SKILL_DEFENCE);

		// ── PVE 傷害 — 正確枚舉名：PVE_PHYSICAL_DMG / PVE_MAGICAL_DMG ─────
		m.put("pvePDmg", Stat.PVE_PHYSICAL_ATTACK_DAMAGE);
		m.put("PVE物理傷害", Stat.PVE_PHYSICAL_ATTACK_DAMAGE);
		m.put("pveMDmg", Stat.PVE_MAGICAL_SKILL_DAMAGE);
		m.put("PVE魔法傷害", Stat.PVE_MAGICAL_SKILL_DAMAGE);
		m.put("pvePSkillDmg", Stat.PVE_PHYSICAL_SKILL_DAMAGE);
		m.put("PVE物理技能傷害", Stat.PVE_PHYSICAL_SKILL_DAMAGE);

		// ── 基礎屬性（六維）───────────────────────────────────────────────
		m.put("str", Stat.STAT_STR);
		m.put("力量", Stat.STAT_STR);
		m.put("con", Stat.STAT_CON);
		m.put("體質", Stat.STAT_CON);
		m.put("dex", Stat.STAT_DEX);
		m.put("敏捷", Stat.STAT_DEX);
		m.put("int", Stat.STAT_INT);
		m.put("智力", Stat.STAT_INT);
		m.put("wit", Stat.STAT_WIT);
		m.put("精神", Stat.STAT_WIT);
		m.put("men", Stat.STAT_MEN);
		m.put("意志", Stat.STAT_MEN);

		// ── 元素攻擊 ──────────────────────────────────────────────────────
		m.put("fireAtk", Stat.FIRE_POWER);
		m.put("火屬性攻擊", Stat.FIRE_POWER);
		m.put("waterAtk", Stat.WATER_POWER);
		m.put("水屬性攻擊", Stat.WATER_POWER);
		m.put("windAtk", Stat.WIND_POWER);
		m.put("風屬性攻擊", Stat.WIND_POWER);
		m.put("earthAtk", Stat.EARTH_POWER);
		m.put("土屬性攻擊", Stat.EARTH_POWER);
		m.put("holyAtk", Stat.HOLY_POWER);
		m.put("神聖攻擊", Stat.HOLY_POWER);
		m.put("darkAtk", Stat.DARK_POWER);
		m.put("暗黑攻擊", Stat.DARK_POWER);

		// ── 元素抗性 ──────────────────────────────────────────────────────
		m.put("fireRes", Stat.FIRE_RES);
		m.put("火屬性抗性", Stat.FIRE_RES);
		m.put("waterRes", Stat.WATER_RES);
		m.put("水屬性抗性", Stat.WATER_RES);
		m.put("windRes", Stat.WIND_RES);
		m.put("風屬性抗性", Stat.WIND_RES);
		m.put("earthRes", Stat.EARTH_RES);
		m.put("土屬性抗性", Stat.EARTH_RES);
		m.put("holyRes", Stat.HOLY_RES);
		m.put("神聖抗性", Stat.HOLY_RES);
		m.put("darkRes", Stat.DARK_RES);
		m.put("暗黑抗性", Stat.DARK_RES);

		// ── 經驗 / 掉落 ───────────────────────────────────────────────────
		m.put("expRate", Stat.EXPSP_RATE);
		m.put("經驗獲取", Stat.EXPSP_RATE);
		m.put("dropRate", Stat.BONUS_DROP_RATE);
		m.put("掉落率", Stat.BONUS_DROP_RATE);
		m.put("dropAmount", Stat.BONUS_DROP_AMOUNT);
		m.put("掉落數量", Stat.BONUS_DROP_AMOUNT);
		m.put("spoilRate", Stat.BONUS_SPOIL_RATE);
		m.put("採集率", Stat.BONUS_SPOIL_RATE);
		m.put("limit", Stat.unlock_Limit);
		m.put("突破限制", Stat.unlock_Limit);

		STAT_PARSE_MAP = Collections.unmodifiableMap(m);
	}

	// ── Stat → 中文显示名（UI 展示用）────────────────────────────────────

	/**
	 * {@link Stat} → 中文显示名，用於玩家查看屬性時展示。
	 */
	public static final Map<Stat, String> STAT_DISPLAY_MAP;
	static
	{
		final Map<Stat, String> m = new HashMap<>();

		// 攻防
		m.put(Stat.PHYSICAL_ATTACK, "物理攻擊");
		m.put(Stat.MAGIC_ATTACK, "魔法攻擊");
		m.put(Stat.PHYSICAL_DEFENCE, "物理防禦");
		m.put(Stat.MAGICAL_DEFENCE, "魔法防禦");
		m.put(Stat.PHYSICAL_SKILL_POWER, "物理技能威力");
		m.put(Stat.MAGICAL_SKILL_POWER, "魔法技能威力");

		// 速度
		m.put(Stat.PHYSICAL_ATTACK_SPEED, "攻擊速度");
		m.put(Stat.MAGIC_ATTACK_SPEED, "施法速度");
		m.put(Stat.MOVE_SPEED, "移動速度");

		// 暴擊
		m.put(Stat.CRITICAL_RATE, "暴擊率");
		m.put(Stat.CRITICAL_DAMAGE, "暴擊傷害");
		m.put(Stat.MAGIC_CRITICAL_RATE, "魔法暴擊率");
		m.put(Stat.MAGIC_CRITICAL_DAMAGE, "魔法暴擊傷害");

		// HP/MP/CP
		m.put(Stat.MAX_HP, "最大HP");
		m.put(Stat.MAX_MP, "最大MP");
		m.put(Stat.MAX_CP, "最大CP");
		m.put(Stat.REGENERATE_HP_RATE, "HP回復");
		m.put(Stat.REGENERATE_MP_RATE, "MP回復");
		m.put(Stat.REGENERATE_CP_RATE, "CP回復");
		m.put(Stat.HEAL_EFFECT, "治療效果");

		// 命中/閃避
		m.put(Stat.ACCURACY_COMBAT, "命中");
		m.put(Stat.EVASION_RATE, "閃避");

		// PVP 傷害/防禦
		m.put(Stat.PVP_PHYSICAL_ATTACK_DAMAGE, "PVP物理傷害");
		m.put(Stat.PVP_MAGICAL_SKILL_DAMAGE, "PVP魔法傷害");
		m.put(Stat.PVP_PHYSICAL_SKILL_DAMAGE, "PVP物理技能傷害");
		m.put(Stat.PVP_PHYSICAL_ATTACK_DEFENCE, "PVP物理防禦");
		m.put(Stat.PVP_MAGICAL_SKILL_DEFENCE, "PVP魔法防禦");
		m.put(Stat.PVP_PHYSICAL_SKILL_DEFENCE, "PVP物理技能防禦");

		// PVE 傷害
		m.put(Stat.PVE_PHYSICAL_ATTACK_DAMAGE, "PVE物理傷害");
		m.put(Stat.PVE_MAGICAL_SKILL_DAMAGE, "PVE魔法傷害");
		m.put(Stat.PVE_PHYSICAL_SKILL_DAMAGE, "PVE物理技能傷害");

		// 六維基礎屬性
		m.put(Stat.STAT_STR, "力量");
		m.put(Stat.STAT_CON, "體質");
		m.put(Stat.STAT_DEX, "敏捷");
		m.put(Stat.STAT_INT, "智力");
		m.put(Stat.STAT_WIT, "精神");
		m.put(Stat.STAT_MEN, "意志");

		// 元素攻擊
		m.put(Stat.FIRE_POWER, "火屬性攻擊");
		m.put(Stat.WATER_POWER, "水屬性攻擊");
		m.put(Stat.WIND_POWER, "風屬性攻擊");
		m.put(Stat.EARTH_POWER, "土屬性攻擊");
		m.put(Stat.HOLY_POWER, "神聖攻擊");
		m.put(Stat.DARK_POWER, "暗黑攻擊");

		// 元素抗性
		m.put(Stat.FIRE_RES, "火屬性抗性");
		m.put(Stat.WATER_RES, "水屬性抗性");
		m.put(Stat.WIND_RES, "風屬性抗性");
		m.put(Stat.EARTH_RES, "土屬性抗性");
		m.put(Stat.HOLY_RES, "神聖抗性");
		m.put(Stat.DARK_RES, "暗黑抗性");

		// 經驗/掉落
		m.put(Stat.EXPSP_RATE, "經驗獲取");
		m.put(Stat.BONUS_DROP_RATE, "掉落率");
		m.put(Stat.BONUS_DROP_AMOUNT, "掉落數量");
		m.put(Stat.BONUS_SPOIL_RATE, "回收率");
		m.put(Stat.unlock_Limit, "攻防限制");

		STAT_DISPLAY_MAP = Collections.unmodifiableMap(m);
	}

	// ── 字段 ──────────────────────────────────────────────────────────────

	private final Stat _stat;
	private final double _value;
	private final Operation _operation;

	// ── 構造器 ────────────────────────────────────────────────────────────

	public MorphStatEntry(Stat stat, double value, Operation operation)
	{
		_stat = stat;
		_value = value;
		_operation = operation;
	}

	// ── Getters ──────────────────────────────────────────────────────────

	public Stat getStat()
	{
		return _stat;
	}

	public double getValue()
	{
		return _value;
	}

	public Operation getOperation()
	{
		return _operation;
	}

	/** 是否是百分比乘算 */
	public boolean isMultiply()
	{
		return _operation == Operation.MUL;
	}

	/**
	 * MUL 時，將 value 轉換為 FuncMul factor。 value=10 → 1.10（+10%）
	 */
	public double getMulFactor()
	{
		return 1.0 + (_value / 100.0);
	}

	/**
	 * 返回該屬性的中文显示名（用於玩家界面）。 若無對應中文名，回退到 Stat.name()。
	 */
	public String getDisplayName()
	{
		return STAT_DISPLAY_MAP.getOrDefault(_stat, _stat.name());
	}

	/**
	 * 格式化显示字符串，玩家查看屬性時使用。 示例： add → "物理攻擊 +500" mul → "暴擊傷害 +15%"
	 */
	public String toDisplayString()
	{
		if (isMultiply())
		{
			return getDisplayName() + " +" + (int) _value + "%";
		}
		return getDisplayName() + " +" + (int) _value;
	}
}
