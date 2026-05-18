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
import java.util.List;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

/**
 * 系統介紹面板 — 單一系統的資料模型。
 *
 * 由 SystemGuideManager 從 config/Custom/SystemGuides/*.md 解析後產生。
 * 每個實例對應一個按鈕與一則私訊 Embed。
 *
 * @author Custom
 */
public class SystemGuideEntry
{
	// Discord Embed field value 最大長度
	private static final int FIELD_VALUE_LIMIT = 1020;

	/** 唯一識別碼，對應 .md 的 id 欄位，用於 Discord 按鈕 ID（不可含空格）*/
	public final String id;

	/** 按鈕顯示文字（含 emoji）*/
	public final String label;

	/** 排序順序，數字小的排前面 */
	public final int order;

	/** Embed 主色調 */
	public final Color color;

	/** 系統概述，顯示於 Embed description 區域 */
	public final String overview;

	/**
	 * 各說明區塊列表。
	 * 每個元素為 String[2]：[0] = 區塊標題（對應 ## 行），[1] = 區塊內容。
	 */
	public final List<String[]> sections;

	public SystemGuideEntry(String id, String label, int order, Color color, String overview, List<String[]> sections)
	{
		this.id = id;
		this.label = label;
		this.order = order;
		this.color = color;
		this.overview = overview;
		this.sections = sections;
	}

	/**
	 * 回傳此系統對應的 Discord 按鈕 Component ID。
	 * 格式：{@code guide:<id>}，例如 {@code guide:BossAuction}。
	 */
	public String getButtonId()
	{
		return "guide:" + id;
	}

	/**
	 * 組裝並回傳私訊用的 Embed。
	 * 每次點擊按鈕時即時建立，確保資料為最新。
	 */
	public MessageEmbed buildEmbed()
	{
		final EmbedBuilder eb = new EmbedBuilder()
			.setColor(color)
			.setTitle(label)
			.setFooter("武魂天堂2  ·  如有疑問請洽 GM");

		// Overview 為空時不加 description（Embed 仍合法）
		if (!overview.isEmpty())
		{
			// Embed description 上限 4096 字元
			final String desc = overview.length() > 4000 ? overview.substring(0, 4000) + "..." : overview;
			eb.setDescription(desc);
		}

		// 各說明區塊 → Embed Field
		for (String[] section : sections)
		{
			String content = section[1];
			if (content.length() > FIELD_VALUE_LIMIT)
			{
				content = content.substring(0, FIELD_VALUE_LIMIT) + "...";
			}
			// inline = false：每個區塊獨立佔一整行，排版較清晰
			eb.addField(section[0], content, false);
		}

		return eb.build();
	}
}
