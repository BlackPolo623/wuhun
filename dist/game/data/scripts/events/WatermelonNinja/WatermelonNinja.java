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
package events.WatermelonNinja;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.model.script.LongTimeEvent;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.util.ArrayUtil;

/**
 * ============================================================
 *  西瓜忍者活動腳本 (WatermelonNinja)
 * ============================================================
 *  活動說明：
 *    玩家從活動管理員 NPC 取得西瓜種子，種植後對種子使用
 *    「花蜜技能」澆灌，使其成長進化成不同等級的西瓜。
 *    普通西瓜用一般武器攻擊即可打爆；蜂蜜西瓜（大型）
 *    需搭配指定的「時空武器」才能造成傷害。
 *    打倒西瓜後隨機掉落道具作為獎勵。
 *
 *  活動開啟方式：
 *    修改 config.xml 內的 StartDate / EndDate 即可設定活動期間。
 *    活動期間伺服器啟動後，事件會自動載入。
 *
 * @URL https://eu.4gameforum.com/threads/653089/
 * @author Mobius, vGodFather, Galagard
 */
public class WatermelonNinja extends LongTimeEvent
{
    // ============================================================
    //  NPC 編號設定區
    //  ★ 若要更換 NPC，修改此區對應的 ID 即可
    // ============================================================

    /** 活動管理員 NPC（嗡嗡貓），玩家透過他換取種子與時空武器 */
    private static final int MANAGER = 31860;

    /**
     * 初始西瓜種子 NPC 列表（種植後等待澆灌的狀態）
     *   13271 = 普通西瓜種子
     *   13275 = 蜂蜜西瓜種子
     * 注意：對種子攻擊無效，必須用花蜜技能才能使其成長
     */
    private static final int[] INITIAL_WATERMELONS =
            {
                    13271, // 普通西瓜種子
                    13275  // 蜂蜜西瓜種子
            };

    /**
     * 普通西瓜成長後的 NPC 列表（用一般武器即可攻擊）
     *   13272 = 劣質���瓜（最容易出現，掉落 C 級材料）
     *   13273 = 優質西瓜（掉落 B 級材料）
     *   13274 = 大型優質西瓜（最稀有，存活時間較短，需及早打倒）
     */
    private static final int[] WATERMELONS_LIST =
            {
                    13272, // 劣質西瓜
                    13273, // 優質西瓜
                    13274  // 大型優質西瓜（存活 30 秒，需時空武器無效）
            };

    /**
     * 蜂蜜西瓜成長後的 NPC 列表（必須使用時空武器才能造成傷害）
     *   13276 = 劣質蜂蜜西瓜（最容易出現，掉落 D 級材料）
     *   13277 = 甘霖蜂蜜西瓜（掉落 C 級材料）
     *   13278 = 大型甘霖蜂蜜西瓜（最稀有，掉落 B 級裝備）
     */
    private static final int[] HONEY_WATERMELONS_LIST =
            {
                    13276, // 劣質蜂蜜西瓜
                    13277, // 甘霖蜂蜜西瓜
                    13278  // 大型甘霖蜂蜜西瓜（存活 30 秒）
            };

    // ============================================================
    //  時空武器 ID 設定區
    //  ★ 只有持有這些武器的玩家才能對蜂蜜西瓜造成傷害
    //  ★ 若要新增或移除可用武器，在此列表中增刪 ID 即可
    // ============================================================
    private static final int[] CHRONO_LIST =
            {
                    4202, // 時空武器 1
                    5133, // 時空武器 2
                    5817, // 時空武器 3
                    7058, // 時空武器 4
                    8350  // 時空武器 5
            };

    // ============================================================
    //  技能 ID 設定區
    //  ★ NECTAR_SKILL：玩家對種子使用此技能（ID 2005）後，種子才會成長
    //    若要更換澆灌技能，修改此 ID 即可
    // ============================================================
    /** 花蜜技能 ID，玩家對種子使用此技能使其成長進化 */
    private static final int NECTAR_SKILL = 2005;

    // ============================================================
    //  NPC 對話文字設定區
    //  ★ 以下各陣列中的字串為 NPC 隨機說出的台詞
    //  ★ 可自由新增、修改或刪除台詞；每次觸發時隨機選一句
    // ============================================================

    /**
     * 西瓜種子剛被種植時（初始生成）隨機說出的台詞
     * 觸發時機：種子 NPC 生成（onSpawn）時
     */
    private static final String[] INITIAL_SPAWN_TEXT =
            {
                    "你在看什麼？",
                    "你是我媽媽嗎？",
                    "好好養育我，說不定有獎勵！也可能沒有……",
                    "我很厲害吧？",
                    "臣服於我吧！！",
                    "叮咚！我來了！",
                    "求求你！給我點花蜜吧！我好餓！"
            };

    /**
     * 西瓜種子吸收花蜜成功進化後，新生成的西瓜說出的台詞
     * 觸發時機：普通西瓜 / 蜂蜜西瓜 NPC 生成（onSpawn）時
     */
    private static final String[] EVOLVE_SPAWN_TEXT =
            {
                    "怎麼樣，你讓我進化了！",
                    "來啊，有本事就把我打倒……",
                    "今天手氣不錯？",
                    "你應該捨不得打一顆可憐的西瓜吧？"
            };

    /**
     * 西瓜被打倒（死亡）時說出的台詞
     * 觸發時機：onKill 事件發生時
     */
    private static final String[] DIE_TEXT =
            {
                    "哼！這下你滿意了吧！",
                    "我才不怕你呢。",
                    "下次好好加油吧",
                    "唉，打一顆可憐的西瓜，你開心了嗎……"
            };

    /**
     * 玩家對蜂蜜西瓜使用「非時空武器」攻擊時，西瓜說出的嘲諷台詞
     * 觸發時機：onAttack 時，攻擊者未持有時空武器
     * 觸發機率：80%（每次攻擊有 80% 機率說話）
     */
    private static final String[] NOCHRONO_TEXT =
            {
                    "沒有……時空武器，你傷不了我的",
                    "嘿嘿……繼續吧……",
                    "不錯的嘗試……",
                    "累了嗎？",
                    "繼續！哈哈……"
            };

    /**
     * 玩家對蜂蜜西瓜使用「時空武器」攻擊時，西瓜說出的求饒台詞
     * 觸發時機：onAttack 時，攻擊者持有時空武器
     * 觸發機率：80%
     */
    private static final String[] CHRONO_TEXT =
            {
                    "啊啊啊……時空武器……",
                    "我的末日要來了……",
                    "放過我吧！",
                    "救……命……",
                    "有人來救救我啊……"
            };

    /**
     * 玩家對「尚未成長的西瓜種子」進行攻擊時，種子說出的抱怨台詞
     * 觸發時機：onAttack，且被攻擊的是初始種子（INITIAL_WATERMELONS）
     * 觸發機率：80%
     * 注意：對種子攻擊不會造成任何效果，種子不可被打死
     */
    private static final String[] INITIAL_WATERMELONS_TEXT =
            {
                    "別打我！給我花蜜！！",
                    "打我沒用的……\n你知道該怎麼做的……",
                    "別再打我了，我需要花蜜！！",
                    "天啊……好吧，你繼續打吧……"
            };

    /**
     * 玩家對「已成長的普通西瓜」攻擊時，西瓜說出的求饒台詞
     * 觸發時機：onAttack，且被攻擊的是 WATERMELONS_LIST 中的西瓜
     * 觸發機率：80%
     */
    private static final String[] WATERMELON_TEXT =
            {
                    "啊啊啊你要打死我了……",
                    "我的末日要來了……",
                    "放過我吧！",
                    "救……命……",
                    "有人來救救我啊……"
            };

    /**
     * 玩家對種子使用花蜜技能時，種子說出的享受台詞
     * 觸發時機：onSkillSee 任何技能施放於種子上時
     * 觸發機率：80%
     * 注意：即使使用非花蜜技能也會觸發台詞，但只有 NECTAR_SKILL 才能使種子成長
     */
    private static final String[] NECTAR_TEXT =
            {
                    "再多給我一點……",
                    "嗯……再多……我還需要更多……",
                    "你要是多給我一點，我會更喜歡你的……",
                    "嗯嗯嗯嗯嗯……",
                    "這是我最愛的……"
            };

    // ============================================================
    //  掉落獎勵設定區（DROPLIST）
    //  格式：new ItemChanceHolder(物品ID, 掉落機率%, 掉落數量)
    //  ★ 可自由修改各西瓜的掉落物品、機率與數量
    //  ★ 掉落邏輯：每次最多掉落 MAX_DROP_COUNT 件物品（見下方設定）
    //    系統從隨機位置開始遍歷清單，按機率逐一判斷是否掉落
    // ============================================================
    private static final Map<Integer, List<ItemChanceHolder>> DROPLIST = new HashMap<>();
    static
    {
        // ── 劣質西瓜（ID: 13272）掉落表 ──────────────────────────────
        // 主要掉落：C 級武器零件與製作配方
        final List<ItemChanceHolder> drops13272 = new ArrayList<>();
        drops13272.add(new ItemChanceHolder(1539,  70, 1)); // 頂級 HP 回復藥水（70% 機率）
        drops13272.add(new ItemChanceHolder(49080, 60, 1)); // 戰意糕（60% 機率）
        drops13272.add(new ItemChanceHolder(1870,  50, 1)); // 煤炭
        drops13272.add(new ItemChanceHolder(1872,  50, 1)); // 動物骨頭
        drops13272.add(new ItemChanceHolder(1865,  50, 1)); // 亮漆
        drops13272.add(new ItemChanceHolder(2287,  50, 1)); // 配方：阿圖巴錘（100%成功率）
        drops13272.add(new ItemChanceHolder(2267,  50, 1)); // 配方：加斯特拉菲提斯弩（100%）
        drops13272.add(new ItemChanceHolder(2276,  50, 1)); // 配方：匕首（100%）
        drops13272.add(new ItemChanceHolder(2289,  50, 1)); // 配方：生命之杖（100%）
        drops13272.add(new ItemChanceHolder(2272,  50, 1)); // 配方：革命之劍（100%）
        drops13272.add(new ItemChanceHolder(2049,  50, 1)); // 阿圖巴錘頭
        drops13272.add(new ItemChanceHolder(2029,  50, 1)); // 加斯特拉菲提斯弩軸
        drops13272.add(new ItemChanceHolder(2038,  50, 1)); // 匕首刃
        drops13272.add(new ItemChanceHolder(2051,  50, 1)); // 生命之杖軸
        drops13272.add(new ItemChanceHolder(2034,  50, 1)); // 革命之劍刃
        DROPLIST.put(13272, drops13272);

        // ── 優質西瓜（ID: 13273）掉落表 ──────────────────────────────
        // 主要掉落：B 級武器零件、裝甲零件與製作配方
        final List<ItemChanceHolder> drops13273 = new ArrayList<>();
        drops13273.add(new ItemChanceHolder(1539,  70, 1)); // 頂級 HP 回復藥水
        drops13273.add(new ItemChanceHolder(49080, 60, 1)); // 戰意糕
        drops13273.add(new ItemChanceHolder(1880,  50, 1)); // 鋼鐵
        drops13273.add(new ItemChanceHolder(1877,  50, 1)); // 金剛石礦塊
        drops13273.add(new ItemChanceHolder(1876,  50, 1)); // 秘銀礦石
        drops13273.add(new ItemChanceHolder(1882,  50, 1)); // 皮革
        drops13273.add(new ItemChanceHolder(1879,  50, 1)); // 焦炭
        drops13273.add(new ItemChanceHolder(1881,  50, 1)); // 粗骨粉
        drops13273.add(new ItemChanceHolder(1875,  50, 1)); // 純淨之石
        drops13273.add(new ItemChanceHolder(2060,  50, 1)); // 風暴劍刃
        drops13273.add(new ItemChanceHolder(2068,  50, 1)); // 信仰之杖軸
        drops13273.add(new ItemChanceHolder(4096,  50, 1)); // 封印藍狼手套布料
        drops13273.add(new ItemChanceHolder(4090,  50, 1)); // 封印藍狼靴子設計圖
        drops13273.add(new ItemChanceHolder(4073,  50, 1)); // 封印阿瓦頓手套碎片
        drops13273.add(new ItemChanceHolder(4098,  50, 1)); // 封印阿瓦頓靴子設計圖
        drops13273.add(new ItemChanceHolder(2075,  50, 1)); // 獸人大刀刃
        drops13273.add(new ItemChanceHolder(2059,  50, 1)); // 浮焰劍刃
        drops13273.add(new ItemChanceHolder(2074,  50, 1)); // 冰霜弓軸
        drops13273.add(new ItemChanceHolder(2067,  50, 1)); // 水晶法杖頭
        drops13273.add(new ItemChanceHolder(4080,  50, 1)); // 藍狼護腿材料
        drops13273.add(new ItemChanceHolder(2063,  50, 1)); // 戰斧頭
        drops13273.add(new ItemChanceHolder(2301,  50, 1)); // 配方：戰斧（100%）
        drops13273.add(new ItemChanceHolder(4982,  50, 1)); // 配方：藍狼護腿（60%）
        drops13273.add(new ItemChanceHolder(2305,  50, 1)); // 配方：水晶法杖（100%）
        drops13273.add(new ItemChanceHolder(2312,  50, 1)); // 配方：冰霜弓（100%）
        drops13273.add(new ItemChanceHolder(3017,  50, 1)); // 配方：神聖手套（100%）
        drops13273.add(new ItemChanceHolder(2234,  50, 1)); // 配方：神聖絲襪（100%）
        drops13273.add(new ItemChanceHolder(2297,  50, 1)); // 配方：浮焰劍（100%）
        drops13273.add(new ItemChanceHolder(3012,  50, 1)); // 配方：全身板甲頭盔（100%）
        drops13273.add(new ItemChanceHolder(3019,  50, 1)); // 配方：全身板甲盾牌（100%）
        drops13273.add(new ItemChanceHolder(2317,  50, 1)); // 配方：貝德科賓（100%）
        drops13273.add(new ItemChanceHolder(4959,  50, 1)); // 配方：封印阿瓦頓靴子（60%）
        drops13273.add(new ItemChanceHolder(4953,  50, 1)); // 配方：封印阿瓦頓手套（60%）
        drops13273.add(new ItemChanceHolder(4992,  50, 1)); // 配方：封印藍狼靴子（60%）
        drops13273.add(new ItemChanceHolder(4998,  50, 1)); // 配方：封印藍狼手套（60%）
        drops13273.add(new ItemChanceHolder(2306,  50, 1)); // 配方：信仰之杖（100%）
        drops13273.add(new ItemChanceHolder(2298,  50, 1)); // 配方：風暴劍（100%）
        DROPLIST.put(13273, drops13273);

        // ── 大型優質西瓜（ID: 13274）掉落表 ──────────────────────────
        // 最稀有的普通西瓜，掉落完整 B 級武器與裝備
        // 注意：此西瓜只存活 30 秒，要趕快打！
        final List<ItemChanceHolder> drops13274 = new ArrayList<>();
        drops13274.add(new ItemChanceHolder(160,   5,  1)); // 戰斧（5% 機率）
        drops13274.add(new ItemChanceHolder(192,   5,  1)); // 水晶法杖
        drops13274.add(new ItemChanceHolder(281,   5,  1)); // 冰霜弓
        drops13274.add(new ItemChanceHolder(71,    5,  1)); // 浮焰劍
        drops13274.add(new ItemChanceHolder(298,   5,  1)); // 獸人大刀
        drops13274.add(new ItemChanceHolder(193,   5,  1)); // 信仰之杖
        drops13274.add(new ItemChanceHolder(72,    5,  1)); // 風暴劍
        drops13274.add(new ItemChanceHolder(2463,  5,  1)); // 神聖手套
        drops13274.add(new ItemChanceHolder(473,   5,  1)); // 神聖絲襪
        drops13274.add(new ItemChanceHolder(442,   5,  1)); // 神聖長袍
        drops13274.add(new ItemChanceHolder(401,   5,  1)); // 龍皮鎧甲
        drops13274.add(new ItemChanceHolder(2437,  5,  1)); // 龍皮靴子
        drops13274.add(new ItemChanceHolder(356,   5,  1)); // 全身板甲
        drops13274.add(new ItemChanceHolder(2414,  5,  1)); // 全身板甲頭盔
        drops13274.add(new ItemChanceHolder(2497,  5,  1)); // 全身板甲盾牌
        drops13274.add(new ItemChanceHolder(1538,  50, 1)); // 改良逃脫卷軸（50%）
        drops13274.add(new ItemChanceHolder(3936,  50, 1)); // 祝福復活卷軸
        drops13274.add(new ItemChanceHolder(5592,  50, 1)); // 高級 CP 藥水
        drops13274.add(new ItemChanceHolder(1540,  50, 1)); // 高級 HP 回復藥水
        drops13274.add(new ItemChanceHolder(49081, 50, 1)); // 經驗值成長卷軸
        drops13274.add(new ItemChanceHolder(49518, 50, 1)); // 特殊海盜水果
        drops13274.add(new ItemChanceHolder(1459,  50, 1)); // C 級水晶
        drops13274.add(new ItemChanceHolder(952,   50, 1)); // 強化 C 級防具卷軸
        drops13274.add(new ItemChanceHolder(951,   50, 1)); // 強化 C 級武器卷軸
        drops13274.add(new ItemChanceHolder(1890,  50, 1)); // 秘銀合金
        drops13274.add(new ItemChanceHolder(4041,  50, 1)); // 模具硬化劑
        drops13274.add(new ItemChanceHolder(1893,  50, 1)); // 奧利哈鋼
        drops13274.add(new ItemChanceHolder(1886,  50, 1)); // 銀模
        DROPLIST.put(13274, drops13274);

        // ── 劣質蜂蜜西瓜（ID: 13276）掉落表 ──────────────────────────
        // 需使用時空武器攻擊，掉落 D 級完整武器與強化材料
        final List<ItemChanceHolder> drops13276 = new ArrayList<>();
        drops13276.add(new ItemChanceHolder(187,   20, 1)); // 阿圖巴錘（20%）
        drops13276.add(new ItemChanceHolder(278,   20, 1)); // 加斯特拉菲提斯弩
        drops13276.add(new ItemChanceHolder(224,   20, 1)); // 匕首
        drops13276.add(new ItemChanceHolder(189,   20, 1)); // 生命之杖
        drops13276.add(new ItemChanceHolder(129,   20, 1)); // 革命之劍
        drops13276.add(new ItemChanceHolder(294,   20, 1)); // 戰鎬
        drops13276.add(new ItemChanceHolder(5592,  50, 1)); // 高級 CP 藥水（50%）
        drops13276.add(new ItemChanceHolder(49080, 50, 1)); // 戰意糕
        drops13276.add(new ItemChanceHolder(49518, 50, 1)); // 特殊海盜水果
        drops13276.add(new ItemChanceHolder(1458,  50, 1)); // D 級水晶
        drops13276.add(new ItemChanceHolder(956,   50, 1)); // 強化 D 級防具卷軸
        drops13276.add(new ItemChanceHolder(955,   50, 1)); // 強化 D 級武器卷軸
        drops13276.add(new ItemChanceHolder(3929,  70, 1)); // 卷軸：敏銳（70%）
        drops13276.add(new ItemChanceHolder(49435, 70, 1)); // 卷軸：狂戰士之魂
        drops13276.add(new ItemChanceHolder(3927,  70, 1)); // 卷軸：死亡低語
        drops13276.add(new ItemChanceHolder(3926,  70, 1)); // 卷軸：指引
        drops13276.add(new ItemChanceHolder(3930,  70, 1)); // 卷軸：急速
        drops13276.add(new ItemChanceHolder(4218,  70, 1)); // 卷軸：魔力再生
        drops13276.add(new ItemChanceHolder(4042,  50, 1)); // 精神素（50%）
        drops13276.add(new ItemChanceHolder(1890,  50, 1)); // 秘銀合金
        drops13276.add(new ItemChanceHolder(4041,  50, 1)); // 模具硬化劑
        drops13276.add(new ItemChanceHolder(4040,  50, 1)); // 模具潤滑劑
        drops13276.add(new ItemChanceHolder(1886,  50, 1)); // 銀模
        drops13276.add(new ItemChanceHolder(1887,  50, 1)); // 純淨亮漆
        DROPLIST.put(13276, drops13276);

        // ── 甘霖蜂蜜西瓜（ID: 13277）掉落表 ──────────────────────────
        // 需使用時空武器攻擊，掉落 B 級武器零件與裝甲製作材料
        final List<ItemChanceHolder> drops13277 = new ArrayList<>();
        drops13277.add(new ItemChanceHolder(5592,  60, 1)); // 高級 CP 藥水（60%）
        drops13277.add(new ItemChanceHolder(1540,  60, 1)); // 高級 HP 回復藥水
        drops13277.add(new ItemChanceHolder(49080, 60, 1)); // 戰意糕
        drops13277.add(new ItemChanceHolder(1877,  50, 1)); // 金剛石礦塊（50%）
        drops13277.add(new ItemChanceHolder(4043,  50, 1)); // 阿索菲
        drops13277.add(new ItemChanceHolder(1881,  50, 1)); // 粗骨粉
        drops13277.add(new ItemChanceHolder(1879,  50, 1)); // 焦炭
        drops13277.add(new ItemChanceHolder(1885,  50, 1)); // 高級麂皮
        drops13277.add(new ItemChanceHolder(1876,  50, 1)); // 秘銀礦石
        drops13277.add(new ItemChanceHolder(4039,  50, 1)); // 模具膠
        drops13277.add(new ItemChanceHolder(1874,  50, 1)); // 奧利哈鋼礦石
        drops13277.add(new ItemChanceHolder(1880,  50, 1)); // 鋼鐵
        drops13277.add(new ItemChanceHolder(1883,  50, 1)); // 鋼模
        drops13277.add(new ItemChanceHolder(1875,  50, 1)); // 純淨之石
        drops13277.add(new ItemChanceHolder(1889,  50, 1)); // 合成編織物
        drops13277.add(new ItemChanceHolder(1888,  50, 1)); // 合成焦炭
        drops13277.add(new ItemChanceHolder(1887,  50, 1)); // 純淨亮漆
        drops13277.add(new ItemChanceHolder(4071,  50, 1)); // 阿瓦頓長袍布料
        drops13277.add(new ItemChanceHolder(5530,  50, 1)); // 狂戰士刃刃緣
        drops13277.add(new ItemChanceHolder(4078,  50, 1)); // 藍狼胸甲部件
        drops13277.add(new ItemChanceHolder(2107,  50, 1)); // 暗黑尖叫者刃緣
        drops13277.add(new ItemChanceHolder(1988,  50, 1)); // 神聖長袍布料
        drops13277.add(new ItemChanceHolder(2121,  50, 1)); // 優越弓軸
        drops13277.add(new ItemChanceHolder(2108,  50, 1)); // 拳刃碎片
        drops13277.add(new ItemChanceHolder(1986,  50, 1)); // 全身板甲回火材料
        drops13277.add(new ItemChanceHolder(2093,  50, 1)); // 長斧刃
        drops13277.add(new ItemChanceHolder(2109,  50, 1)); // 阿卡特長弓軸
        drops13277.add(new ItemChanceHolder(4072,  50, 1)); // 封印阿瓦頓王冠花紋
        drops13277.add(new ItemChanceHolder(4088,  50, 1)); // 封印藍狼頭盔設計圖
        drops13277.add(new ItemChanceHolder(4089,  50, 1)); // 封印厄運頭盔花紋
        drops13277.add(new ItemChanceHolder(2095,  50, 1)); // 惡夢之劍刃
        drops13277.add(new ItemChanceHolder(4951,  50, 1)); // 配方：阿瓦頓長袍（60%）
        drops13277.add(new ItemChanceHolder(5436,  50, 1)); // 配方：狂戰士刃（100%）
        drops13277.add(new ItemChanceHolder(4981,  50, 1)); // 配方：藍狼胸甲（60%）
        drops13277.add(new ItemChanceHolder(2345,  50, 1)); // 配方：暗黑尖叫者（100%）
        drops13277.add(new ItemChanceHolder(2233,  50, 1)); // 配方：神聖長袍（100%）
        drops13277.add(new ItemChanceHolder(2359,  50, 1)); // 配方：優越弓（100%）
        drops13277.add(new ItemChanceHolder(2346,  50, 1)); // 配方：拳刃（100%）
        drops13277.add(new ItemChanceHolder(2231,  50, 1)); // 配方：全身板甲（100%）
        drops13277.add(new ItemChanceHolder(2330,  50, 1)); // 配方：荷姆庫魯斯之劍（100%）
        drops13277.add(new ItemChanceHolder(4985,  50, 1)); // 配方：厄運皮甲（60%）
        drops13277.add(new ItemChanceHolder(2331,  50, 1)); // 配方：長斧（100%）
        drops13277.add(new ItemChanceHolder(2341,  50, 1)); // 配方：賢者之杖（100%）
        drops13277.add(new ItemChanceHolder(4952,  50, 1)); // 配方：封印阿瓦頓王冠（60%）
        drops13277.add(new ItemChanceHolder(4990,  50, 1)); // 配方：封印藍狼頭盔（60%）
        drops13277.add(new ItemChanceHolder(4991,  50, 1)); // 配方：封印厄運頭盔（60%）
        drops13277.add(new ItemChanceHolder(2333,  50, 1)); // 配方：惡夢之劍（100%）
        DROPLIST.put(13277, drops13277);

        // ── 大型甘霖蜂蜜西瓜（ID: 13278）掉落表 ──────────────────────
        // 最稀有的蜂蜜西瓜，需使用時空武器，掉落 B 級完整裝備
        // 注意：此西瓜只存活 30 秒，強烈建議多人合作！
        final List<ItemChanceHolder> drops13278 = new ArrayList<>();
        drops13278.add(new ItemChanceHolder(5286,  5,  1)); // 狂戰士刃（5% 機率）
        drops13278.add(new ItemChanceHolder(233,   5,  1)); // 暗黑尖叫者
        drops13278.add(new ItemChanceHolder(286,   5,  1)); // 優越弓
        drops13278.add(new ItemChanceHolder(265,   5,  1)); // 拳刃
        drops13278.add(new ItemChanceHolder(84,    5,  1)); // 荷姆庫魯斯之劍
        drops13278.add(new ItemChanceHolder(95,    5,  1)); // 長斧
        drops13278.add(new ItemChanceHolder(200,   5,  1)); // 賢者之杖
        drops13278.add(new ItemChanceHolder(134,   5,  1)); // 惡夢之劍
        drops13278.add(new ItemChanceHolder(2406,  5,  1)); // 阿瓦頓長袍
        drops13278.add(new ItemChanceHolder(358,   5,  1)); // 藍狼胸甲
        drops13278.add(new ItemChanceHolder(2380,  5,  1)); // 藍狼護腿
        drops13278.add(new ItemChanceHolder(2392,  5,  1)); // 厄運皮甲
        drops13278.add(new ItemChanceHolder(600,   10, 1)); // 封印阿瓦頓靴子（10%）
        drops13278.add(new ItemChanceHolder(2415,  10, 1)); // 封印阿瓦頓王冠
        drops13278.add(new ItemChanceHolder(2464,  10, 1)); // 封印阿瓦頓手套
        drops13278.add(new ItemChanceHolder(2439,  10, 1)); // 封印藍狼靴子
        drops13278.add(new ItemChanceHolder(2487,  10, 1)); // 封印藍狼手套
        drops13278.add(new ItemChanceHolder(2416,  10, 1)); // 封印藍狼頭盔
        drops13278.add(new ItemChanceHolder(601,   10, 1)); // 封印厄運靴子
        drops13278.add(new ItemChanceHolder(2475,  10, 1)); // 封印厄運手套
        drops13278.add(new ItemChanceHolder(2417,  10, 1)); // 封印厄運頭盔
        drops13278.add(new ItemChanceHolder(1538,  50, 1)); // 改良逃脫卷軸（50%）
        drops13278.add(new ItemChanceHolder(3936,  50, 1)); // 祝福復活卷軸
        drops13278.add(new ItemChanceHolder(1460,  50, 1)); // B 級水晶
        drops13278.add(new ItemChanceHolder(1459,  50, 1)); // C 級水晶
        drops13278.add(new ItemChanceHolder(5592,  50, 1)); // 高級 CP 藥水
        drops13278.add(new ItemChanceHolder(1539,  50, 1)); // 頂級 HP 回復藥水
        drops13278.add(new ItemChanceHolder(1540,  70, 1)); // 高級 HP 回復藥水（70%）
        drops13278.add(new ItemChanceHolder(49081, 60, 1)); // 經驗值成長卷軸（60%）
        drops13278.add(new ItemChanceHolder(952,   50, 1)); // 強化 C 級防具卷軸
        drops13278.add(new ItemChanceHolder(951,   40, 1)); // 強化 C 級武器卷軸（40%）
        drops13278.add(new ItemChanceHolder(49518, 60, 1)); // 特殊海盜水果（60%）
        DROPLIST.put(13278, drops13278);
    }

    // ============================================================
    //  掉落數量上限設定
    //  ★ MAX_DROP_COUNT：每隻西瓜死亡後最多掉落幾件道具
    //    目前設定為 1，即每次只掉一件
    //    若想讓西瓜掉更多，可以調高此數值（建議不超過 3）
    // ============================================================
    private static final int MAX_DROP_COUNT = 1;

    // ============================================================
    //  建構子：註冊各事件監聽
    //  此區塊決定哪些 NPC 的哪些行為會觸發對應的事件方法
    // ============================================================
    public WatermelonNinja()
    {
        // 攻擊事件監聽：以下 NPC 被攻擊時會觸發 onAttack()
        addAttackId(INITIAL_WATERMELONS);   // 初始種子被攻擊（會顯示抱怨台詞，但無傷害）
        addAttackId(WATERMELONS_LIST);       // 普通西瓜被攻擊
        addAttackId(HONEY_WATERMELONS_LIST); // 蜂蜜西瓜被攻擊

        // 擊殺事件監聽：以下 NPC 被打倒時會觸發 onKill()
        addKillId(WATERMELONS_LIST);         // 普通西瓜死亡 → 發放獎勵
        addKillId(HONEY_WATERMELONS_LIST);   // 蜂蜜西瓜死亡 → 發放獎勵

        // 生成事件監聽：以下 NPC 生成時會觸發 onSpawn()
        addSpawnId(INITIAL_WATERMELONS);     // 種子生成 → 設為無敵＋固定不動
        addSpawnId(WATERMELONS_LIST);        // 普通西瓜生成 → 設為無敵＋固定不動＋播放進化台詞
        addSpawnId(HONEY_WATERMELONS_LIST);  // 蜂蜜西瓜生成 → 同上

        // 技能偵測監聽：以下 NPC 周圍有技能被施放時觸發 onSkillSee()
        addSkillSeeId(INITIAL_WATERMELONS);  // 對種子施放花蜜技能 → 觸發成長進化

        // 管理員 NPC 對話監聽
        addStartNpc(MANAGER);       // 玩家右鍵點擊管理員 NPC 時開啟對話
        addFirstTalkId(MANAGER);    // 第一次對話觸發 onFirstTalk()
        addTalkId(MANAGER);         // 後續對話選項觸發 onEvent()
    }

    // ============================================================
    //  對話事件處理（玩家點選 HTM 按鈕時觸發）
    // ============================================================
    @Override
    public String onEvent(String event, Npc npc, Player player)
    {
        String htmltext = null;

        // 玩家點選「告訴我更多活動詳情」按鈕時，開啟說明頁面
        if (event.equals("31860-1.htm"))
        {
            htmltext = event;
        }

        return htmltext;
    }

    // ============================================================
    //  初次對話處理（玩家第一次點擊管理員 NPC 時觸發）
    //  回傳 NPC ID 對應的 HTM 檔案（31860.htm）
    // ============================================================
    @Override
    public String onFirstTalk(Npc npc, Player player)
    {
        return npc.getId() + ".htm";
    }

    // ============================================================
    //  攻擊事件處理（NPC 被玩家攻擊時觸發）
    // ============================================================
    @Override
    public void onAttack(Npc npc, Player attacker, int damage, boolean isPet)
    {
        // 情況 1：被攻擊的是初始種子 → 顯示抗議台詞，不做其他處理
        if (ArrayUtil.contains(INITIAL_WATERMELONS, npc.getId()))
        {
            dontHitMeText(npc);
            return;
        }

        // 情況 2：被攻擊的是蜂蜜西瓜，且攻擊者持有時空武器
        //         → 解除無敵 + 扣減固定 HP（最低保留 1 HP，避免直接死亡）
        //         → 顯示被時空武器攻擊的台詞
        if ((attacker.getActiveWeaponItem() != null) && ArrayUtil.contains(CHRONO_LIST, attacker.getActiveWeaponItem().getId()) && ArrayUtil.contains(HONEY_WATERMELONS_LIST, npc.getId()))
        {
            chronoText(npc);
            npc.setInvul(false); // 解除無敵狀態，讓此次攻擊有效

            // 每次攻擊固定扣 30 HP（注意：實際傷害由此決定，與玩家攻擊力無關）
            // ★ 若想調整每次傷害量，修改此處的數值（目前固定扣 30）
            final double currentHp = npc.getCurrentHp();
            final double newHp = Math.max(currentHp - 30, 1);
            npc.getStatus().setCurrentHp(newHp);
        }
        // 情況 3：被攻擊的是普通西瓜（任何武器均有效）
        //         → 解除無敵 + 扣減固定 HP
        //         → 顯示普通受擊台詞
        else if ((attacker.getActiveWeaponItem() != null) && ArrayUtil.contains(WATERMELONS_LIST, npc.getId()))
        {
            watermelonText(npc);
            npc.setInvul(false);

            // ★ 若想調整普通西瓜每次受到的傷害，修改此處數值（目前固定扣 30）
            final double currentHp = npc.getCurrentHp();
            final double newHp = Math.max(currentHp - 30, 1);
            npc.getStatus().setCurrentHp(newHp);
        }
        // 情況 4：攻擊者未持時空武器，卻攻擊蜂蜜西瓜 → 顯示嘲諷台詞
        //         注意：此處仍會扣 HP，但 setInvul(false) 沒有被呼叫，所以正常是無效的
        //         實際上因為 noChronoText 後直接扣 HP，這裡的傷害會讓 HP 降到 0（可能有 Bug）
        else
        {
            noChronoText(npc);
            npc.getStatus().setCurrentHp(Math.max(npc.getCurrentHp() - 30, 0));
        }
    }

    // ============================================================
    //  技能偵測事件（在種子周圍施放技能時觸發）
    //  主要用途：花蜜技能澆灌種子，使種子成長
    // ============================================================
    @Override
    public void onSkillSee(Npc npc, Player caster, Skill skill, Collection<WorldObject> targets, boolean isPet)
    {
        // 任何技能施放於種子附近都會顯示台詞（80% 機率）
        nectarText(npc);

        // 只有使用「花蜜技能」（NECTAR_SKILL）且目標為種子本身，才會觸發成長
        if ((skill.getId() == NECTAR_SKILL) && (caster.getTarget() == npc))
        {
            switch (npc.getId())
            {
                case 13271: // 普通西瓜種子 → 有機率生成普通西瓜（13272/13273/13274）
                {
                    randomSpawn(13274, 13273, 13272, npc, caster);
                    break;
                }
                case 13275: // 蜂蜜西瓜種子 → 有機率生成蜂蜜西瓜（13276/13277/13278）
                {
                    randomSpawn(13278, 13277, 13276, npc, caster);
                    break;
                }
            }
        }
    }

    // ============================================================
    //  生成事件（NPC 生成時觸發）
    //  所有西瓜生成後皆設為：固定不動 + 關閉 AI + 初始無敵
    // ============================================================
    @Override
    public void onSpawn(Npc npc)
    {
        npc.setImmobilized(true);   // 固定位置，無法移動
        npc.disableCoreAI(true);    // 關閉 AI，不會主動反擊
        npc.setInvul(true);         // 初始無敵（攻擊蜂蜜西瓜需持時空武器才能解除）

        if (ArrayUtil.contains(INITIAL_WATERMELONS, npc.getId()))
        {
            initialSpawnText(npc); // 種子生成台詞
        }
        else
        {
            evolveSpawnText(npc);  // 進化西瓜生成台詞
        }
    }

    // ============================================================
    //  擊殺事件（西瓜被打倒時觸發，發放掉落獎勵）
    // ============================================================
    @Override
    public void onKill(Npc npc, Player killer, boolean isPet)
    {
        // 取得此 NPC 對應的掉落表，若無掉落設定則直接結束
        final List<ItemChanceHolder> drops = DROPLIST.get(npc.getId());
        if ((drops == null) || drops.isEmpty())
        {
            return;
        }

        final int size = drops.size();
        final int startIndex = Rnd.get(size); // 隨機起始索引，確保每次掉落都不固定從頭開始
        int dropCount = 0;
        final int maxDrops = Rnd.get(1, MAX_DROP_COUNT); // 本次最多掉落數（1 ~ MAX_DROP_COUNT）
        final Monster monster = npc.asMonster();

        // 從隨機位置開始遍歷掉落表，依機率決定是否掉落
        // 滿足 maxDrops 數量後停止，確保不會超量掉落
        for (int i = 0; (i < size) && (dropCount < maxDrops); i++)
        {
            final int currentIndex = (startIndex + i) % size; // 使用模數確保循環不超出邊界
            final ItemChanceHolder drop = drops.get(currentIndex);
            if (Rnd.get(100) < drop.getChance())
            {
                monster.dropItem(killer, drop);
                dropCount++;
            }
        }

        dieText(npc); // 西瓜死亡台詞
    }

    // ============================================================
    //  西瓜成長機率計算
    //  ★ 這是活動的核心機率設定，可在此調整各西瓜的出現機率
    //
    //  參數說明（以普通種子 13271 為例）：
    //    low    = 13274（大型優質西瓜）→ 最稀有，機率 5%
    //    medium = 13273（優質西瓜）    → 次級，機率 5%
    //    high   = 13272（劣質西瓜）    → 最常見，機率 20%
    //    其餘 70% 機率：花蜜澆灌失敗，種子不成長
    //
    //  ★ 修改說明：
    //    若想提高大型西瓜機率，調高第一個 if 的閾值（目前 < 5）
    //    若想提高優質西瓜機率，調整第二個 else if 的閾值（目前 < 10）
    //    若想提高劣質西瓜機率，調整第三個 else if 的閾值（目前 < 30）
    //    三者之和不超過 100 即可（剩餘部分為失敗機率）
    // ============================================================
    private void randomSpawn(int low, int medium, int high, Npc npc, Player attacker)
    {
        final int npcId = npc.getId();
        // 若已是大型成熟西瓜，不再進化
        if ((npcId == 13274) || (npcId == 13278))
        {
            return;
        }

        final int random = getRandom(100);
        if (random < 5)
        {
            // 5% 機率：進化為大型西瓜（最稀有，存活 30 秒）
            spawnNext(low, npc);
            attacker.sendPacket(new PlaySound("ItemSound3.sys_sow_success"));
        }
        else if (random < 10)
        {
            // 5% 機率：進化為優質西瓜（存活 3 分鐘）
            spawnNext(medium, npc);
            attacker.sendPacket(new PlaySound("ItemSound3.sys_sow_success"));
        }
        else if (random < 30)
        {
            // 20% 機率：進化為劣質西瓜（存活 3 分鐘）
            spawnNext(high, npc);
            attacker.sendPacket(new PlaySound("ItemSound3.sys_sow_success"));
        }
        // 剩餘 70% 機率：澆灌失敗，種子繼續等待
    }

    // ──────────────────────────────────────────────────────────────
    //  以下為各台詞觸發輔助方法
    //  觸發機率皆為 80%（每次有 20% 機率保持沉默）
    // ──────────────────────────────────────────────────────────────

    /** 對初始種子攻擊時，種子隨機說出抗議台詞（80% 機率） */
    private void dontHitMeText(Npc npc)
    {
        if (getRandom(100) < 80)
        {
            npc.broadcastPacket(new CreatureSay(npc, ChatType.NPC_GENERAL, npc.getName(), getRandomEntry(INITIAL_WATERMELONS_TEXT)));
        }
    }

    /** 種子剛被種植時（初始生成）說出的歡迎台詞（100% 觸發） */
    private void initialSpawnText(Npc npc)
    {
        npc.broadcastPacket(new CreatureSay(npc, ChatType.NPC_GENERAL, npc.getName(), getRandomEntry(INITIAL_SPAWN_TEXT)));
    }

    /** 西瓜進化成功生成時說出的挑釁台詞（100% 觸發） */
    private void evolveSpawnText(Npc npc)
    {
        npc.broadcastPacket(new CreatureSay(npc, ChatType.NPC_GENERAL, npc.getName(), getRandomEntry(EVOLVE_SPAWN_TEXT)));
    }

    /** 西瓜死亡時說出的臨終台詞（100% 觸發） */
    private void dieText(Npc npc)
    {
        npc.broadcastPacket(new CreatureSay(npc, ChatType.NPC_GENERAL, npc.getName(), getRandomEntry(DIE_TEXT)));
    }

    /** 普通西瓜被攻擊時說出的求饒台詞（80% 機率） */
    private void watermelonText(Npc npc)
    {
        if (getRandom(100) < 80)
        {
            npc.broadcastPacket(new CreatureSay(npc, ChatType.NPC_GENERAL, npc.getName(), getRandomEntry(WATERMELON_TEXT)));
        }
    }

    /** 蜂蜜西瓜被時空武器攻擊時說出的懼怕台詞（80% 機率） */
    private void chronoText(Npc npc)
    {
        if (getRandom(100) < 80)
        {
            npc.broadcastPacket(new CreatureSay(npc, ChatType.NPC_GENERAL, npc.getName(), getRandomEntry(CHRONO_TEXT)));
        }
    }

    /** 蜂蜜西瓜被非時空武器攻擊時說出的嘲諷台詞（80% 機率） */
    private void noChronoText(Npc npc)
    {
        if (getRandom(100) < 80)
        {
            npc.broadcastPacket(new CreatureSay(npc, ChatType.NPC_GENERAL, npc.getName(), getRandomEntry(NOCHRONO_TEXT)));
        }
    }

    /** 對種子施放技能時，種子說出的享受台詞（80% 機率） */
    private void nectarText(Npc npc)
    {
        if (getRandom(100) < 80)
        {
            npc.broadcastPacket(new CreatureSay(npc, ChatType.NPC_GENERAL, npc.getName(), getRandomEntry(NECTAR_TEXT)));
        }
    }

    // ============================================================
    //  生成下一階段西瓜並刪除當前種子 / 舊西瓜
    //
    //  ★ 存活時間設定（毫秒）：
    //    大型西瓜（13274、13278）：30,000 ms = 30 秒
    //    其他西瓜（劣質、優質）  ：180,000 ms = 3 分鐘
    //
    //  ★ 若要調整存活時間，修改下方 addSpawn 的最後一個參數（毫秒值）
    //    例如：改為 90000 表示 90 秒（1 分 30 秒）
    // ============================================================
    private void spawnNext(int npcId, Npc npc)
    {
        addSpawn(
            npcId,
            npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(),
            false,
            (npcId == 13274) || (npcId == 13278) ? 30000 : 180000 // 大型 30 秒，其他 3 分鐘
        );
        npc.deleteMe(); // 刪除當前種子 / 舊西瓜
    }

    // ============================================================
    //  程式進入點：載入並啟動活動
    // ============================================================
    public static void main(String[] args)
    {
        new WatermelonNinja();
    }
}
