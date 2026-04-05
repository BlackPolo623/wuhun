package custom.chenghao;

import java.util.ArrayList;
import java.util.List;

/**
 * 稱號系統 - 系列配置類
 * 簡化版：方便新增和管理稱號系列
 */
public class TitleSystem
{
	// ==================== 全局配置 ====================
	public static final int NPC_ID = 900003;
	public static final String VAR_PREFIX = "title_system_";

	// 融合材料配置（統一）
	public static final int FUSION_ITEM_ID = 57; // 金幣
	public static final long FUSION_ITEM_COUNT = 5000000; // 500萬

	// 舊版技能ID範圍（用於移除）
	public static final int OLD_SKILL_ID_START = 100001;
	public static final int OLD_SKILL_ID_END = 100035;

	// ==================== 系列定義 ====================
	private static final List<TitleSeries> SERIES_LIST = new ArrayList<>();

	static
	{
		// ==================== 實驗體系列 ====================
		// 成功率設定：第1次100%，第2次90%，第3次80%...第10次10%
		double[] experimentalRates = {1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1};

		SERIES_LIST.add(new TitleSeries(
			"experimental",           // 系列ID
			"實驗體系列",              // 系列名稱
			"終焉武魂",                // 最終稱號名稱
			100000,                   // 最終技能ID
			10,                       // 最大等級（可合成10次）
			50001, 50035,             // BOSS ID範圍：50001~50035
			experimentalRates         // 成功率陣列
		));

		double[] IceRates = {1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1};

		SERIES_LIST.add(new TitleSeries(
				"Ice",           // 系列ID
				"冰凍君主系列",              // 系列名稱
				"極寒領主",                // 最終稱號名稱
				100001,                   // 最終技能ID
				10,                       // 最大等級（可合成10次）
				29136, 29139,             // BOSS ID範圍：50001~50035
				IceRates         // 成功率陣列
		));

		double[] LeonasDungeonRates = {1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1};

		SERIES_LIST.add(new TitleSeries(
				"LeonasDungeon",           // 系列ID
				"蕾歐娜系列",              // 系列名稱
				"蕾歐娜的統治者",                // 最終稱號名稱
				100002,                   // 最終技能ID
				10,                       // 最大等級（可合成10次）
				50041, 50044,             // BOSS ID範圍：50001~50035
				LeonasDungeonRates         // 成功率陣列
		));

		double[] DualClassRates = {1.0, 0.9, 0.8, 0.7, 0.6};

		SERIES_LIST.add(new TitleSeries(
				"DualClass",           // 系列ID
				"雙職業系列",              // 系列名稱
				"雙職業的人",                // 最終稱號名稱
				100003,                   // 最終技能ID
				5,                       // 最大等級（可合成10次）
				61000, 61000,             // BOSS ID範圍：50001~50035
				DualClassRates         // 成功率陣列
		));

		// ==================== 寵物系列 ====================
		// 成功率設定：第1次100%，第2次90%，第3次80%...第10次10%
		double[] PetsRates = {1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1};

		SERIES_LIST.add(new TitleSeries(
				"Pets",           // 系列ID
				"寵物系列",              // 系列名稱
				"兔之守護者",                // 最終稱號名稱
				100004,                   // 最終技能ID
				10,                       // 最大等級（可合成10次）
				51000, 51000,             // BOSS ID範圍：50001~50035
				PetsRates         // 成功率陣列
		));

		// 成功率設定：第1次100%，第2次90%，第3次80%...第10次10%
		double[] FireDragonRates = {1.0, 0.9, 0.8, 0.7, 0.6};

		SERIES_LIST.add(new TitleSeries(
				"FireDragon",           // 系列ID
				"火龍系列",              // 系列名稱
				"火之屠龍者",                // 最終稱號名稱
				100005,                   // 最終技能ID
				5,                       // 最大等級（可合成10次）
				25966, 25966,             // BOSS ID範圍：50001~50035
				FireDragonRates         // 成功率陣列
		));

		// ==================== 未來新增其他系列範例 ====================

		// 冰凍系列（假設有20個BOSS，可升5級，100%成功率）
		// SERIES_LIST.add(new TitleSeries(
		//     "ice",                    // 系列ID
		//     "冰凍系列",                // 系列名稱
		//     "冰封霸主",                // 最終稱號名稱
		//     100100,                   // 最終技能ID
		//     5,                        // 最大等級（可合成5次）
		//     29136, 29155              // BOSS ID範圍：29136~29155
		//     // 不傳入成功率參數，預設100%成功
		// ));

		// 古墓系列（假設有15個BOSS，可升3級，自訂成功率）
		// double[] tombRates = {1.0, 0.8, 0.5}; // 第1次100%，第2次80%，第3次50%
		// SERIES_LIST.add(new TitleSeries(
		//     "tomb",                   // 系列ID
		//     "古墓系列",                // 系列名稱
		//     "亡靈君王",                // 最終稱號名稱
		//     100200,                   // 最終技能ID
		//     3,                        // 最大等級（可合成3次）
		//     21614, 21628,             // BOSS ID範圍：21614~21628
		//     tombRates                 // 成功率陣列
		// ));
	}

	// ==================== 數據類 ====================

	/**
	 * 稱號系列
	 */
	public static class TitleSeries
	{
		private final String seriesId;
		private final String seriesName;
		private final String finalTitleName;
		private final int finalSkillId;
		private final int maxLevel;
		private final List<SmallTitle> smallTitles;
		private final double[] fusionSuccessRates; // 每個等級的合成成功率

		/**
		 * 簡化構造器 - 使用ID範圍自動生成
		 * @param seriesId 系列ID
		 * @param seriesName 系列名稱
		 * @param finalTitleName 最終稱號名稱
		 * @param finalSkillId 最終技能ID
		 * @param maxLevel 最大等級
		 * @param bossIdStart BOSS ID起始
		 * @param bossIdEnd BOSS ID結束
		 */
		public TitleSeries(String seriesId, String seriesName, String finalTitleName,
		                   int finalSkillId, int maxLevel,
		                   int bossIdStart, int bossIdEnd)
		{
			this(seriesId, seriesName, finalTitleName, finalSkillId, maxLevel, bossIdStart, bossIdEnd, null);
		}

		/**
		 * 完整構造器 - 包含成功率設定
		 * @param seriesId 系列ID
		 * @param seriesName 系列名稱
		 * @param finalTitleName 最終稱號名稱
		 * @param finalSkillId 最終技能ID
		 * @param maxLevel 最大等級
		 * @param bossIdStart BOSS ID起始
		 * @param bossIdEnd BOSS ID結束
		 * @param successRates 每個等級的成功率陣列 (0.0-1.0)，null表示100%成功
		 */
		public TitleSeries(String seriesId, String seriesName, String finalTitleName,
		                   int finalSkillId, int maxLevel,
		                   int bossIdStart, int bossIdEnd, double[] successRates)
		{
			this.seriesId = seriesId;
			this.seriesName = seriesName;
			this.finalTitleName = finalTitleName;
			this.finalSkillId = finalSkillId;
			this.maxLevel = maxLevel;
			this.smallTitles = new ArrayList<>();
			this.fusionSuccessRates = successRates;

			// 自動生成小BOSS列表
			int count = bossIdEnd - bossIdStart + 1;
			for (int i = 0; i < count; i++)
			{
				int num = i + 1;
				String titleName = generateTitleName(seriesId, num);
				int bossId = bossIdStart + i;
				smallTitles.add(new SmallTitle(titleName, bossId));
			}
		}

		private String generateTitleName(String seriesId, int num)
		{
			switch (seriesId)
			{
				case "experimental":
					return "實驗體" + getChineseNumber(num) + "號";
				case "Ice":
					return getChineseNumber(num) + "級冰之領主";
				case "LeonasDungeon":
					return "蕾歐娜" + getChineseNumber(num) + "層";
				case "DualClass":
					return "雙職業" + getChineseNumber(num) + "層";
				default:
					return seriesId + "_" + num;
			}
		}

		public String getSeriesId()
		{
			return seriesId;
		}

		public String getSeriesName()
		{
			return seriesName;
		}

		public String getFinalTitleName()
		{
			return finalTitleName;
		}

		public int getFinalSkillId()
		{
			return finalSkillId;
		}

		public int getMaxLevel()
		{
			return maxLevel;
		}

		public List<SmallTitle> getSmallTitles()
		{
			return smallTitles;
		}

		public int getTotalCount()
		{
			return smallTitles.size();
		}

		/**
		 * 獲取指定等級的合成成功率
		 * @param level 目標等級 (1-based)
		 * @return 成功率 (0.0-1.0)，null表示100%成功
		 */
		public Double getFusionSuccessRate(int level)
		{
			if (fusionSuccessRates == null || fusionSuccessRates.length == 0)
			{
				return null; // 100% 成功
			}

			// level是1-based，陣列是0-based
			int index = level - 1;
			if (index >= 0 && index < fusionSuccessRates.length)
			{
				return fusionSuccessRates[index];
			}

			return null; // 超出範圍，預設100%成功
		}

		/**
		 * 檢查是否有設定成功率
		 */
		public boolean hasSuccessRates()
		{
			return fusionSuccessRates != null && fusionSuccessRates.length > 0;
		}
	}

	/**
	 * 小BOSS稱號
	 */
	public static class SmallTitle
	{
		private final String titleName;
		private final int bossNpcId;

		public SmallTitle(String titleName, int bossNpcId)
		{
			this.titleName = titleName;
			this.bossNpcId = bossNpcId;
		}

		public String getTitleName()
		{
			return titleName;
		}

		public int getBossNpcId()
		{
			return bossNpcId;
		}
	}

	// ==================== 公共方法 ====================

	public static List<TitleSeries> getAllSeries()
	{
		return SERIES_LIST;
	}

	public static TitleSeries getSeriesById(String seriesId)
	{
		for (TitleSeries series : SERIES_LIST)
		{
			if (series.getSeriesId().equals(seriesId))
			{
				return series;
			}
		}
		return null;
	}

	// ==================== 輔助方法 ====================

	private static String getChineseNumber(int num)
	{
		String[] numbers = {"", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};
		String[] tens = {"", "十", "二十", "三十"};

		if (num <= 10)
		{
			return numbers[num];
		}
		else if (num < 20)
		{
			return "十" + numbers[num - 10];
		}
		else if (num < 40)
		{
			int ten = num / 10;
			int one = num % 10;
			return tens[ten] + (one > 0 ? numbers[one] : "");
		}
		return String.valueOf(num);
	}
}
