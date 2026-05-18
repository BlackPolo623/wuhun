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
package org.l2jmobius.discord;

import java.awt.Color;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.l2jmobius.gameserver.config.custom.DiscordConfig;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;

/**
 * 系統介紹面板管理器。
 *
 * 職責：
 *   1. 掃描並解析 config/Custom/SystemGuides/*.md 說明文件
 *   2. 在指定頻道發送/更新帶有按鈕的介紹面板訊息
 *   3. 處理玩家點擊按鈕後，以私訊傳送對應系統說明 Embed
 *
 * ── 新增系統說明（操作流程）──────────────────────────────────────
 * 1. 在 config/Custom/SystemGuides/ 目錄下新建 XXX.md 文件
 * 2. 照範例填寫 Front Matter（--- 區塊）與內容
 * 3. 重啟 Discord Bot（重啟遊戲伺服器）
 * 4. Bot 自動更新面板，新按鈕立即出現
 *
 * 詳細格式請參閱 config/Custom/SystemGuides/ 目錄下的範例文件。
 * ────────────────────────────────────────────────────────────────
 *
 * @author Custom
 */
public class SystemGuideManager extends ListenerAdapter
{
	private static final Logger LOGGER = Logger.getLogger(SystemGuideManager.class.getName());

	// 說明文件目錄路徑（相對於 GameServer 執行目錄）
	private static final String GUIDES_DIR = "./config/Custom/SystemGuides/";

	// 記錄面板訊息 ID 的文字檔，用於重啟後更新現有訊息而非重新發送
	private static final String MESSAGE_ID_FILE = "./config/Custom/SystemGuidePanelMessageId.txt";

	// Discord Embed 面板主色（Discord Blurple）
	private static final Color PANEL_COLOR = new Color(0x58, 0x65, 0xF2);

	// 每行最多放幾個按鈕（Discord 上限 5）
	private static final int BUTTONS_PER_ROW = 5;

	// JDA 實例，在 initialize() 時傳入
	private JDA _jda;

	// 已載入的系統說明列表（照 order 排序）
	private final List<SystemGuideEntry> _guides = new ArrayList<>();

	// ── 初始化 ───────────────────────────────────────────────────────────────

	/**
	 * 初始化：載入說明文件並建立/更新面板訊息。
	 * 必須在 JDA.awaitReady() 完成後呼叫。
	 *
	 * @param jda 已就緒的 JDA 實例
	 */
	public void initialize(JDA jda)
	{
		_jda = jda;
		loadGuides();

		if (_guides.isEmpty())
		{
			LOGGER.warning("SystemGuideManager: 未找到任何 .md 說明文件（" + GUIDES_DIR + "），面板不會發送。");
			LOGGER.warning("SystemGuideManager: 請在該目錄下建立 .md 文件後重啟 Bot。");
			return;
		}

		LOGGER.info("SystemGuideManager: 已載入 " + _guides.size() + " 個系統說明，準備建立面板。");
		setupPanelMessage();
	}

	// ── Markdown 說明文件載入 ─────────────────────────────────────────────────

	/**
	 * 掃描 GUIDES_DIR 目錄，解析所有 .md 文件並填入 _guides。
	 */
	private void loadGuides()
	{
		_guides.clear();

		final Path dir = Paths.get(GUIDES_DIR);
		if (!Files.exists(dir))
		{
			try
			{
				Files.createDirectories(dir);
				LOGGER.info("SystemGuideManager: 已自動建立說明文件目錄：" + GUIDES_DIR);
			}
			catch (IOException e)
			{
				LOGGER.warning("SystemGuideManager: 無法建立目錄 " + GUIDES_DIR + "：" + e.getMessage());
				return;
			}
		}

		try (Stream<Path> stream = Files.list(dir))
		{
			stream
				.filter(p -> p.toString().toLowerCase().endsWith(".md"))
				.sorted() // 依檔名字母順序預排，最終還是以 order 欄位為準
				.forEach(path ->
				{
					final SystemGuideEntry entry = parseMarkdownFile(path);
					if (entry != null)
					{
						_guides.add(entry);
						LOGGER.info("SystemGuideManager: 已載入說明 [" + entry.id + "] " + entry.label + "（排序：" + entry.order + "）");
					}
				});
		}
		catch (IOException e)
		{
			LOGGER.warning("SystemGuideManager: 掃描說明文件目錄時發生錯誤：" + e.getMessage());
		}

		// 依 order 欄位排序（小的排前面）
		_guides.sort(Comparator.comparingInt(g -> g.order));
	}

	/**
	 * 解析單一 Markdown 文件，回傳 SystemGuideEntry；解析失敗則回傳 null。
	 *
	 * 文件格式：
	 * <pre>
	 * ---
	 * id: UniqueId       （必填，不可含空格）
	 * label: 🏆 系統名稱  （必填，作為按鈕文字）
	 * order: 1           （選填，排序數字，預設 99）
	 * color: FF6D00      （選填，十六進位顏色，不含 #，預設藍紫色）
	 * ---
	 *
	 * 這裡是系統概述，可以多行。
	 *
	 * ## 📋 使用方式
	 * 1. 步驟一
	 * 2. 步驟二
	 *
	 * ## 💡 小技巧
	 * - 技巧一
	 * - 技巧二
	 * </pre>
	 */
	private SystemGuideEntry parseMarkdownFile(Path path)
	{
		try
		{
			final List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

			// ── Step 1：找出 Front Matter 邊界（--- 行）
			int frontStart = -1;
			int frontEnd = -1;
			for (int i = 0; i < lines.size(); i++)
			{
				if (lines.get(i).trim().equals("---"))
				{
					if (frontStart == -1)
					{
						frontStart = i;
					}
					else
					{
						frontEnd = i;
						break;
					}
				}
			}

			if (frontStart == -1 || frontEnd == -1)
			{
				LOGGER.warning("SystemGuideManager: 文件 [" + path.getFileName() + "] 缺少 Front Matter 區塊（--- ... ---），已跳過。");
				return null;
			}

			// ── Step 2：解析 Front Matter 欄位
			String id = null;
			String label = null;
			int order = 99;
			String colorHex = "5865F2"; // 預設：Discord Blurple

			for (int i = frontStart + 1; i < frontEnd; i++)
			{
				final String line = lines.get(i).trim();
				if (line.isEmpty() || !line.contains(":"))
				{
					continue;
				}

				final int colonIdx = line.indexOf(':');
				final String key = line.substring(0, colonIdx).trim().toLowerCase();
				final String value = line.substring(colonIdx + 1).trim();

				switch (key)
				{
					case "id":
						id = value;
						break;
					case "label":
						label = value;
						break;
					case "order":
						try
						{
							order = Integer.parseInt(value);
						}
						catch (NumberFormatException ignored)
						{
							LOGGER.warning("SystemGuideManager: [" + path.getFileName() + "] order 欄位格式錯誤（" + value + "），使用預設值 99。");
						}
						break;
					case "color":
						colorHex = value.replace("#", "").trim();
						break;
				}
			}

			// 必要欄位驗證
			if ((id == null) || id.isEmpty())
			{
				LOGGER.warning("SystemGuideManager: 文件 [" + path.getFileName() + "] 缺少 id 欄位，已跳過。");
				return null;
			}
			if (id.contains(" "))
			{
				LOGGER.warning("SystemGuideManager: 文件 [" + path.getFileName() + "] 的 id 欄位不可含空格（" + id + "），已跳過。");
				return null;
			}
			if ((label == null) || label.isEmpty())
			{
				LOGGER.warning("SystemGuideManager: 文件 [" + path.getFileName() + "] 缺少 label 欄位，以 id 代替。");
				label = id;
			}

			// 解析顏色
			Color color;
			try
			{
				color = new Color(Integer.parseInt(colorHex, 16));
			}
			catch (NumberFormatException e)
			{
				LOGGER.warning("SystemGuideManager: [" + path.getFileName() + "] color 欄位格式錯誤（" + colorHex + "），使用預設顏色。");
				color = PANEL_COLOR;
			}

			// ── Step 3：解析內容區域（Front Matter 結束行之後）
			final List<String> contentLines = lines.subList(frontEnd + 1, lines.size());

			final StringBuilder currentBlock = new StringBuilder();
			String currentSectionTitle = null;
			String overview = "";
			final List<String[]> sections = new ArrayList<>();
			boolean inOverview = true;

			for (String line : contentLines)
			{
				if (line.startsWith("## "))
				{
					// 遇到新的 ## 標題
					if (inOverview)
					{
						// 概述結束，儲存 overview
						overview = currentBlock.toString().trim();
						currentBlock.setLength(0);
						inOverview = false;
					}
					else if (currentSectionTitle != null)
					{
						// 前一個區塊結束，儲存
						sections.add(new String[]
						{
							currentSectionTitle,
							currentBlock.toString().trim()
						});
						currentBlock.setLength(0);
					}
					currentSectionTitle = line.substring(3).trim();
				}
				else
				{
					// 概述：跳過最前面的空白行（避免 --- 之後的空行進入 overview）
					if (inOverview && line.trim().isEmpty() && (currentBlock.length() == 0))
					{
						continue;
					}
					currentBlock.append(line).append("\n");
				}
			}

			// 處理最後一個區塊
			if (inOverview)
			{
				overview = currentBlock.toString().trim();
			}
			else if ((currentSectionTitle != null) && (currentBlock.length() > 0))
			{
				sections.add(new String[]
				{
					currentSectionTitle,
					currentBlock.toString().trim()
				});
			}

			return new SystemGuideEntry(id, label, order, color, overview, sections);
		}
		catch (IOException e)
		{
			LOGGER.warning("SystemGuideManager: 讀取文件 [" + path.getFileName() + "] 時發生錯誤：" + e.getMessage());
			return null;
		}
	}

	// ── 面板訊息管理 ─────────────────────────────────────────────────────────

	/**
	 * 依據設定決定更新現有面板訊息或發送全新訊息。
	 */
	private void setupPanelMessage()
	{
		final String channelId = DiscordConfig.SYSTEM_GUIDE_CHANNEL_ID;
		if ((channelId == null) || channelId.isEmpty())
		{
			LOGGER.warning("SystemGuideManager: SystemGuideChannelId 未設定，面板訊息無法發送。");
			return;
		}

		final TextChannel channel = _jda.getTextChannelById(channelId);
		if (channel == null)
		{
			LOGGER.warning("SystemGuideManager: 找不到頻道 ID：" + channelId + "，請確認 Bot 已加入伺服器且 ID 正確。");
			return;
		}

		final MessageEmbed panelEmbed = buildPanelEmbed();
		final List<ActionRow> buttonRows = buildButtonRows();
		final long storedMessageId = loadPanelMessageId();

		if (storedMessageId > 0)
		{
			// 嘗試取回舊訊息並更新（避免重複發送）
			channel.retrieveMessageById(storedMessageId).queue(
				message -> message.editMessageEmbeds(panelEmbed)
					.setComponents(buttonRows)
					.queue(
						success -> LOGGER.info("SystemGuideManager: 面板訊息已更新（Message ID：" + storedMessageId + "）"),
						error ->
						{
							LOGGER.warning("SystemGuideManager: 更新面板訊息失敗，重新發送：" + error.getMessage());
							sendNewPanelMessage(channel, panelEmbed, buttonRows);
						}
					),
				error ->
				{
					LOGGER.info("SystemGuideManager: 舊面板訊息不存在，重新發送。");
					sendNewPanelMessage(channel, panelEmbed, buttonRows);
				}
			);
		}
		else
		{
			sendNewPanelMessage(channel, panelEmbed, buttonRows);
		}
	}

	/**
	 * 發送全新的面板訊息並儲存訊息 ID。
	 */
	private void sendNewPanelMessage(TextChannel channel, MessageEmbed embed, List<ActionRow> buttonRows)
	{
		channel.sendMessageEmbeds(embed)
			.setComponents(buttonRows)
			.queue(
				message ->
				{
					savePanelMessageId(message.getIdLong());
					LOGGER.info("SystemGuideManager: 面板訊息已發送（Message ID：" + message.getIdLong() + "）");
				},
				error -> LOGGER.warning("SystemGuideManager: 發送面板訊息失敗：" + error.getMessage())
			);
	}

	/**
	 * 建立面板的 Embed 主體（不含按鈕）。
	 */
	private MessageEmbed buildPanelEmbed()
	{
		return new EmbedBuilder()
			.setColor(PANEL_COLOR)
			.setTitle(DiscordConfig.SYSTEM_GUIDE_PANEL_TITLE)
			.setDescription(
				DiscordConfig.SYSTEM_GUIDE_PANEL_DESC + "\n\n" +
					"📌 **目前收錄 " + _guides.size() + " 個系統說明**")
			.setFooter("武魂天堂2")
			.build();
	}

	/**
	 * 將系統說明列表轉換為 Discord 按鈕排列（每行最多 BUTTONS_PER_ROW 個）。
	 */
	private List<ActionRow> buildButtonRows()
	{
		final List<ActionRow> rows = new ArrayList<>();
		final List<Button> rowBuffer = new ArrayList<>();

		for (SystemGuideEntry guide : _guides)
		{
			rowBuffer.add(Button.primary(guide.getButtonId(), guide.label));

			if (rowBuffer.size() == BUTTONS_PER_ROW)
			{
				rows.add(ActionRow.of(new ArrayList<>(rowBuffer)));
				rowBuffer.clear();
			}
		}

		// 最後一行（不滿 BUTTONS_PER_ROW 個）
		if (!rowBuffer.isEmpty())
		{
			rows.add(ActionRow.of(rowBuffer));
		}

		return rows;
	}

	// ── 面板訊息 ID 持久化 ────────────────────────────────────────────────────

	/**
	 * 從文字檔讀取上次發送的面板訊息 ID。
	 * 文件不存在或格式錯誤時回傳 0。
	 */
	private long loadPanelMessageId()
	{
		final Path filePath = Paths.get(MESSAGE_ID_FILE);
		if (!Files.exists(filePath))
		{
			return 0L;
		}

		try
		{
			final String content = Files.readString(filePath, StandardCharsets.UTF_8).trim();
			return Long.parseLong(content);
		}
		catch (Exception e)
		{
			return 0L;
		}
	}

	/**
	 * 將面板訊息 ID 寫入文字檔供下次啟動使用。
	 */
	private void savePanelMessageId(long messageId)
	{
		try
		{
			Files.writeString(Paths.get(MESSAGE_ID_FILE), String.valueOf(messageId), StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			LOGGER.warning("SystemGuideManager: 無法儲存面板訊息 ID：" + e.getMessage());
		}
	}

	// ── 按鈕互動處理 ─────────────────────────────────────────────────────────

	/**
	 * 處理玩家點擊介紹面板按鈕的事件。
	 *
	 * 流程：
	 *   1. 確認 Component ID 以 "guide:" 開頭
	 *   2. 找出對應的 SystemGuideEntry
	 *   3. 以 Ephemeral 方式回應（只有點擊者可見）
	 *   4. 嘗試傳送私訊 Embed
	 *   5. 依傳送結果更新 Ephemeral 訊息
	 */
	@Override
	public void onButtonInteraction(ButtonInteractionEvent event)
	{
		final String componentId = event.getComponentId();
		if (!componentId.startsWith("guide:"))
		{
			// 非系統介紹按鈕，交由其他監聽器處理
			return;
		}

		final String guideId = componentId.substring(6); // 去掉 "guide:" 前綴

		// 查找對應說明
		SystemGuideEntry guide = null;
		for (SystemGuideEntry g : _guides)
		{
			if (g.id.equals(guideId))
			{
				guide = g;
				break;
			}
		}

		if (guide == null)
		{
			// 說明文件已被移除或 ID 不符
			event.reply("❌ 找不到此系統的說明，可能已被移除，請洽 GM。")
				.setEphemeral(true)
				.queue();
			return;
		}

		// Ephemeral Defer：先回應「處理中」，只有點擊者看得到，不會刷頻道
		event.deferReply(true).queue();

		final MessageEmbed embed = guide.buildEmbed();
		final String guideName = guide.label;

		// 嘗試傳送私訊
		event.getUser().openPrivateChannel().queue(
			dmChannel -> dmChannel.sendMessageEmbeds(embed).queue(
				// 私訊發送成功
				success ->
				{
					event.getHook()
						.editOriginal("✅ **" + guideName + "** 的說明已傳送到您的私訊，請查看！")
						.queue();
					LOGGER.info("SystemGuideManager: 已傳送 [" + guideId + "] 說明給 " + event.getUser().getName());
				},
				// 私訊發送失敗（玩家關閉 DM）
				error ->
				{
					event.getHook()
						.editOriginal(
							"❌ 無法傳送私訊！\n\n" +
								"請至 **Discord 設定 → 隱私與安全** 開啟\n" +
								"「✅ 允許伺服器成員傳送私訊」後再試一次。")
						.queue();
					LOGGER.info("SystemGuideManager: 無法傳送私訊給 " + event.getUser().getName() + "（可能已關閉 DM）：" + error.getMessage());
				}
			),
			// 無法開啟 DM 頻道
			error ->
			{
				event.getHook()
					.editOriginal("❌ 無法開啟私訊頻道，請稍後再試。")
					.queue();
				LOGGER.warning("SystemGuideManager: 無法對 " + event.getUser().getName() + " 開啟私訊頻道：" + error.getMessage());
			}
		);
	}
}
