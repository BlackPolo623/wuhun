package tools.npceditor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import tools.npceditor.NpcDataModel.DropGroup;
import tools.npceditor.NpcDataModel.DropItem;
import tools.npceditor.NpcDataModel.FortuneItem;
import tools.npceditor.NpcDataModel.SkillEntry;
import tools.npceditor.NpcDataModel.Stats;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
/**
 * NPC XML解析器
 */
public class NpcXmlParser {
	// 技能名称缓存
	private static final Map<String, String> SKILL_NAME_CACHE = new HashMap<>();
	private static final Map<String, String> ITEM_NAME_CACHE = new HashMap<>();

	/**
	 * 從文件加載NPC數據（只加載第一個NPC）
	 */
	public static NpcDataModel loadFromFile(File file) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		// 使用UTF-8編碼讀取
		InputSource is = new InputSource(new InputStreamReader(new FileInputStream(file), "UTF-8"));
		Document doc = builder.parse(is);
		doc.getDocumentElement().normalize();

		Element rootElement = doc.getDocumentElement();
		Element npcElement = null;

		// 檢查根元素類型
		if ("npc".equalsIgnoreCase(rootElement.getTagName())) {
			// 情況A: 根元素就是<npc>
			npcElement = rootElement;
		} else if ("list".equalsIgnoreCase(rootElement.getTagName())) {
			// 情況B: 根元素是<list>，找第一個<npc>子元素
			NodeList npcNodes = rootElement.getElementsByTagName("npc");
			if (npcNodes.getLength() > 0) {
				npcElement = (Element) npcNodes.item(0);
			}
		}

		if (npcElement == null) {
			throw new Exception("無法找到<npc>元素，文件格式可能不正確: " + file.getName());
		}

		return parseNpcElement(npcElement);
	}

	/**
	 * 解析單個NPC元素
	 */
	private static NpcDataModel parseNpcElement(Element npcElement) throws Exception {
		NpcDataModel npc = new NpcDataModel();

		// 解析基本屬性
		parseBasicAttributes(npcElement, npc);

		// 解析子元素
		NodeList childNodes = npcElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			Element element = (Element) node;
			String tagName = element.getTagName();

			switch (tagName) {
				case "race":
					npc.setRace(element.getTextContent().trim());
					break;
				case "sex":
					npc.setSex(element.getTextContent().trim());
					break;
				case "stats":
					parseStats(element, npc.getStats());
					break;
				case "collision":
					parseCollision(element, npc.getStats());
					break;
				case "skillList":
					parseSkillList(element, npc);
					break;
				case "dropLists":
					parseDropLists(element, npc);
					break;
			}
		}

		return npc;
	}

	/**
	 * 解析collision碰撞體積
	 */
	private static void parseCollision(Element collisionElement, Stats stats) {
		Element radiusElement = getFirstChildElement(collisionElement, "radius");
		if (radiusElement != null) {
			stats.setCollisionRadius(getDoubleAttribute(radiusElement, "normal", 0));
		}
		Element heightElement = getFirstChildElement(collisionElement, "height");
		if (heightElement != null) {
			stats.setCollisionHeight(getDoubleAttribute(heightElement, "normal", 0));
		}
	}

	/**
	 * 解析基本屬性
	 */
	private static void parseBasicAttributes(Element npcElement, NpcDataModel npc) {
		npc.setId(getIntAttribute(npcElement, "id", 0));
		npc.setDisplayId(getIntAttribute(npcElement, "displayId", 0));
		npc.setLevel(getIntAttribute(npcElement, "level", 1));
		npc.setType(getAttribute(npcElement, "type", "Monster"));
		npc.setName(getAttribute(npcElement, "name", "Unknown"));
		npc.setTitle(getAttribute(npcElement, "title", ""));
		npc.setElement(getAttribute(npcElement, "element", ""));
	}

	/**
	 * 解析屬性值 - 使用double支持高精度小數
	 */
	private static void parseStats(Element statsElement, Stats stats) {
		// 基礎屬性
		stats.setStr(getIntAttribute(statsElement, "str", 0));
		stats.setIntVal(getIntAttribute(statsElement, "int", 0));
		stats.setDex(getIntAttribute(statsElement, "dex", 0));
		stats.setWit(getIntAttribute(statsElement, "wit", 0));
		stats.setCon(getIntAttribute(statsElement, "con", 0));
		stats.setMen(getIntAttribute(statsElement, "men", 0));

		// 解析子元素
		NodeList childNodes = statsElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			Element element = (Element) node;
			String tagName = element.getTagName();

			switch (tagName) {
				case "vitals":
					// 改用getDoubleAttribute支持高精度小數
					stats.setMaxHp(getDoubleAttribute(element, "hp", 0));
					stats.setHpRegen(getDoubleAttribute(element, "hpRegen", 0));
					stats.setMaxMp(getDoubleAttribute(element, "mp", 0));
					stats.setMpRegen(getDoubleAttribute(element, "mpRegen", 0));
					break;
				case "attack":
					// 改用getDoubleAttribute支持小數攻擊力
					stats.setPhysicalAttack(getDoubleAttribute(element, "physical", 0));
					stats.setMagicalAttack(getDoubleAttribute(element, "magical", 0));
					stats.setAttackRandom(getIntAttribute(element, "random", 0));
					stats.setCritical(getIntAttribute(element, "critical", 0));
					stats.setAccuracy(getIntAttribute(element, "accuracy", 0));
					stats.setAttackSpeed(getIntAttribute(element, "attackSpeed", 0));
					stats.setAttackType(getAttribute(element, "type", "SWORD"));
					stats.setAttackRange(getIntAttribute(element, "range", 0));
					break;
				case "defence":
					// 改用getDoubleAttribute支持小數防禦力
					stats.setPhysicalDefence(getDoubleAttribute(element, "physical", 0));
					stats.setMagicalDefence(getDoubleAttribute(element, "magical", 0));
					break;
				case "speed":
					NodeList speedNodes = element.getChildNodes();
					for (int j = 0; j < speedNodes.getLength(); j++) {
						Node speedNode = speedNodes.item(j);
						if (speedNode.getNodeType() == Node.ELEMENT_NODE) {
							Element speedElement = (Element) speedNode;
							if ("walk".equals(speedElement.getTagName())) {
								stats.setWalkSpeed(getIntAttribute(speedElement, "ground", 0));
							} else if ("run".equals(speedElement.getTagName())) {
								stats.setRunSpeed(getIntAttribute(speedElement, "ground", 0));
							}
						}
					}
					break;
				case "hitTime":
					stats.setHitTime(Integer.parseInt(element.getTextContent().trim()));
					break;
			}
		}
	}

	/**
	 * 解析技能列表並查詢技能名稱
	 */
	private static void parseSkillList(Element skillListElement, NpcDataModel npc) {
		NodeList skillNodes = skillListElement.getElementsByTagName("skill");
		for (int i = 0; i < skillNodes.getLength(); i++) {
			Element skillElement = (Element) skillNodes.item(i);
			int skillId = getIntAttribute(skillElement, "id", 0);
			int level = getIntAttribute(skillElement, "level", 1);

			SkillEntry skill = new SkillEntry(skillId, level);

			// 查詢技能名稱
			String cacheKey = skillId + "_" + level;
			String skillName = SKILL_NAME_CACHE.get(cacheKey);

			if (skillName == null) {
				skillName = lookupSkillName(skillId, level);
				if (skillName != null) {
					SKILL_NAME_CACHE.put(cacheKey, skillName);
				} else {
					skillName = "Skill " + skillId;
				}
			}

			skill.setSkillName(skillName);
			npc.getSkills().add(skill);
		}
	}

	/**
	 * 查詢技能名稱（使用SkillData）
	 */
	private static String lookupSkillName(int skillId, int level) {
		try {
			// 檢查 SkillData 是否已初始化
			if (SkillData.getInstance() == null) {
				return null;
			}

			Skill skill = SkillData.getInstance().getSkill(skillId, level);
			if (skill != null) {
				return skill.getName();
			}
		} catch (Exception e) {
			// 靜默處理異常
		}
		return null;
	}

	/**
	 * 從技能文件解析技能名稱
	 */
	private static String parseSkillNameFromFile(File skillFile) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(skillFile);

			Element root = doc.getDocumentElement();

			// 嘗試從根元素獲取name屬性
			if ("skill".equals(root.getTagName())) {
				String name = root.getAttribute("name");
				if (name != null && !name.isEmpty()) {
					return name;
				}
			}

			// 嘗試從skill子元素獲取
			NodeList skillNodes = root.getElementsByTagName("skill");
			if (skillNodes.getLength() > 0) {
				Element skillElement = (Element) skillNodes.item(0);
				String name = skillElement.getAttribute("name");
				if (name != null && !name.isEmpty()) {
					return name;
				}
			}

			// 嘗試從table元素獲取（某些版本的格式）
			NodeList tableNodes = root.getElementsByTagName("table");
			for (int i = 0; i < tableNodes.getLength(); i++) {
				Element tableElement = (Element) tableNodes.item(i);
				if ("name".equals(tableElement.getAttribute("name"))) {
					String value = tableElement.getTextContent();
					if (value != null && !value.isEmpty()) {
						return value.trim();
					}
				}
			}

		} catch (Exception e) {
			// 忽略錯誤
		}
		return null;
	}

	/**
	 * 掃描skills目錄查找技能文件
	 */
	private static String scanSkillsDirectory(String skillsDir, int skillId) {
		try {
			File dir = new File(skillsDir);
			if (!dir.exists() || !dir.isDirectory()) {
				return null;
			}

			// 遞歸搜索
			return searchSkillInDirectory(dir, skillId);
		} catch (Exception e) {
			// 忽略
		}
		return null;
	}

	/**
	 * 遞歸搜索目錄
	 */
	private static String searchSkillInDirectory(File dir, int skillId) {
		File[] files = dir.listFiles();
		if (files == null) {
			return null;
		}

		for (File file : files) {
			if (file.isDirectory()) {
				String result = searchSkillInDirectory(file, skillId);
				if (result != null) {
					return result;
				}
			} else if (file.getName().endsWith(".xml")) {
				// 檢查文件名是否包含技能ID
				String fileName = file.getName();
				if (fileName.contains(String.valueOf(skillId))) {
					String name = parseSkillNameFromFile(file);
					if (name != null) {
						return name;
					}
				}
			}
		}
		return null;
	}

	/**
	 * 檢測技能目錄
	 */
	private static String detectSkillsDirectory() {
		String[] possiblePaths = {
				"./game/data/stats/skills",
				"./dist/game/data/stats/skills",
				"../game/data/stats/skills",
				"../dist/game/data/stats/skills",
				System.getProperty("user.dir") + "/game/data/stats/skills",
				System.getProperty("user.dir") + "/dist/game/data/stats/skills"
		};

		for (String path : possiblePaths) {
			File dir = new File(path);
			if (dir.exists() && dir.isDirectory()) {
				// 檢查是否有XML文件
				File[] files = dir.listFiles((d, name) -> name.endsWith(".xml"));
				if (files != null && files.length > 0) {
					return dir.getAbsolutePath();
				}

				// 檢查子目錄
				File[] subdirs = dir.listFiles(File::isDirectory);
				if (subdirs != null && subdirs.length > 0) {
					return dir.getAbsolutePath();
				}
			}
		}
		return null;
	}

	/**
	 * 解析掉落列表
	 */
	private static void parseDropLists(Element dropListsElement, NpcDataModel npc) {
		NodeList childNodes = dropListsElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			Element element = (Element) node;
			if ("drop".equals(element.getTagName())) {
				parseDropElement(element, npc);
			} else if ("fortune".equals(element.getTagName())) {
				parseFortuneElement(element, npc);
			}
		}
	}

	/**
	 * 解析普通掉落
	 */
	private static void parseDropElement(Element dropElement, NpcDataModel npc) {
		NodeList groupNodes = dropElement.getElementsByTagName("group");
		for (int i = 0; i < groupNodes.getLength(); i++) {
			Element groupElement = (Element) groupNodes.item(i);
			DropGroup group = new DropGroup();
			group.setChance(getDoubleAttribute(groupElement, "chance", 0));

			NodeList itemNodes = groupElement.getElementsByTagName("item");
			for (int j = 0; j < itemNodes.getLength(); j++) {
				Element itemElement = (Element) itemNodes.item(j);
				int itemId = getIntAttribute(itemElement, "id", 0);
				int min = getIntAttribute(itemElement, "min", 1);
				int max = getIntAttribute(itemElement, "max", 1);
				double chance = getDoubleAttribute(itemElement, "chance", 0);

				DropItem item = new DropItem(itemId, min, max, chance);

				// 查詢物品名稱
				String itemName = lookupItemName(itemId);
				item.setItemName(itemName != null ? itemName : "Unknown Item");

				group.getItems().add(item);
			}

			npc.getDropGroups().add(group);
		}
	}

	/**
	 * 解析幸運掉落
	 */
	private static void parseFortuneElement(Element fortuneElement, NpcDataModel npc) {
		NodeList itemNodes = fortuneElement.getElementsByTagName("item");
		for (int i = 0; i < itemNodes.getLength(); i++) {
			Element itemElement = (Element) itemNodes.item(i);
			int itemId = getIntAttribute(itemElement, "id", 0);
			int min = getIntAttribute(itemElement, "min", 1);
			int max = getIntAttribute(itemElement, "max", 1);
			double chance = getDoubleAttribute(itemElement, "chance", 0);

			FortuneItem item = new FortuneItem(itemId, min, max, chance);

			// 查詢物品名稱
			String itemName = lookupItemName(itemId);
			item.setItemName(itemName != null ? itemName : "Unknown Item");

			npc.getFortuneItems().add(item);
		}
	}

	/**
	 * 查詢物品名稱（使用ItemData）
	 */
	private static String lookupItemName(int itemId) {
		String cached = ITEM_NAME_CACHE.get(String.valueOf(itemId));
		if (cached != null) {
			return cached;
		}

		try {
			ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
			if (item != null) {
				String name = item.getName();
				ITEM_NAME_CACHE.put(String.valueOf(itemId), name);
				return name;
			}
		} catch (Exception e) {
			System.err.println("查詢物品名稱失敗: ID=" + itemId);
		}
		return null;
	}

	/**
	 * 保存NPC數據到文件
	 */
	public static void saveToFile(NpcDataModel npc, File file) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.newDocument();

		// 創建根元素
		Element npcElement = doc.createElement("npc");
		doc.appendChild(npcElement);

		// 設置基本屬性
		npcElement.setAttribute("id", String.valueOf(npc.getId()));
		npcElement.setAttribute("displayId", String.valueOf(npc.getDisplayId()));
		npcElement.setAttribute("level", String.valueOf(npc.getLevel()));
		npcElement.setAttribute("type", npc.getType());
		npcElement.setAttribute("name", npc.getName());
		npcElement.setAttribute("usingServerSideName", "true");
		if ((npc.getTitle() != null) && !npc.getTitle().isEmpty()) {
			npcElement.setAttribute("title", npc.getTitle());
			npcElement.setAttribute("usingServerSideTitle", "true");
		}
		if ((npc.getElement() != null) && !npc.getElement().isEmpty()) {
			npcElement.setAttribute("element", npc.getElement());
		}

		// 添加種族
		if ((npc.getRace() != null) && !npc.getRace().isEmpty()) {
			Element raceElement = doc.createElement("race");
			raceElement.setTextContent(npc.getRace());
			npcElement.appendChild(raceElement);
		}

		// 添加性別
		if ((npc.getSex() != null) && !npc.getSex().isEmpty()) {
			Element sexElement = doc.createElement("sex");
			sexElement.setTextContent(npc.getSex());
			npcElement.appendChild(sexElement);
		}

		// 添加屬性值（這裡簡化處理，完整版需要添加所有子元素）
		// TODO: 實現完整的stats序列化

		// 添加技能列表
		if (!npc.getSkills().isEmpty()) {
			Element skillListElement = doc.createElement("skillList");
			for (SkillEntry skill : npc.getSkills()) {
				Element skillElement = doc.createElement("skill");
				skillElement.setAttribute("id", String.valueOf(skill.getSkillId()));
				skillElement.setAttribute("level", String.valueOf(skill.getLevel()));
				skillListElement.appendChild(skillElement);
			}
			npcElement.appendChild(skillListElement);
		}

		// 保存到文件
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(file);
		transformer.transform(source, result);
	}

	// ==================== 輔助方法 ====================

	private static String getAttribute(Element element, String attrName, String defaultValue) {
		String value = element.getAttribute(attrName);
		return (value != null) && !value.isEmpty() ? value : defaultValue;
	}

	private static int getIntAttribute(Element element, String attrName, int defaultValue) {
		String value = element.getAttribute(attrName);
		if ((value == null) || value.isEmpty()) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * 獲取double屬性 - 支持高精度小數
	 */
	private static double getDoubleAttribute(Element element, String attrName, double defaultValue) {
		String value = element.getAttribute(attrName);
		if ((value == null) || value.isEmpty()) {
			return defaultValue;
		}
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			System.err.println("無法解析double值: " + attrName + "=" + value + ", 使用默認值: " + defaultValue);
			return defaultValue;
		}
	}

	private static Element getFirstChildElement(Node parent, String tagName) {
		if (parent == null) {
			return null;
		}

		NodeList nodes = parent.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if ((node.getNodeType() == Node.ELEMENT_NODE) && tagName.equals(node.getNodeName())) {
				return (Element) node;
			}
		}
		return null;
	}

	/**
	 * 掃描目錄下的所有NPC文件
	 */
	public static List<File> scanNpcFiles(String directory) {
		List<File> npcFiles = new ArrayList<>();
		File dir = new File(directory);

		if (!dir.exists() || !dir.isDirectory()) {
			System.err.println("目錄不存在: " + directory);
			return npcFiles;
		}

		System.out.println("開始掃描NPC目錄: " + directory);
		scanDirectory(dir, npcFiles);
		System.out.println("共找到 " + npcFiles.size() + " 個XML文件");

		return npcFiles;
	}

	private static void scanDirectory(File dir, List<File> npcFiles) {
		File[] files = dir.listFiles();
		if (files == null) {
			return;
		}

		for (File file : files) {
			if (file.isDirectory()) {
				// 確保遞歸掃描子目錄（包括 custom）
				System.out.println("掃描子目錄: " + file.getAbsolutePath());
				scanDirectory(file, npcFiles);
			} else if (file.getName().endsWith(".xml")) {
				npcFiles.add(file);
			}
		}
	}

	/**
	 * 從文件加載所有NPC（處理一個文件多個NPC的情況）
	 */
	public static List<NpcDataModel> loadAllNpcsFromFile(File file) throws Exception {
		List<NpcDataModel> npcs = new ArrayList<>();

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(file);
		doc.getDocumentElement().normalize();

		Element rootElement = doc.getDocumentElement();

		if ("npc".equalsIgnoreCase(rootElement.getTagName())) {
			// 單個NPC文件
			npcs.add(parseNpcElement(rootElement));
		} else if ("list".equalsIgnoreCase(rootElement.getTagName())) {
			// 多個NPC的列表
			NodeList npcNodes = rootElement.getElementsByTagName("npc");
			for (int i = 0; i < npcNodes.getLength(); i++) {
				Element npcElement = (Element) npcNodes.item(i);
				try {
					npcs.add(parseNpcElement(npcElement));
				} catch (Exception e) {
					System.err.println("解析NPC失敗 (文件: " + file.getName() + ", 索引: " + i + "): " + e.getMessage());
				}
			}
		} else {
			System.err.println("未知的XML根元素: " + rootElement.getTagName() + " (文件: " + file.getName() + ")");
		}

		return npcs;
	}

	/**
	 * 將新NPC添加到現有文件中
	 */
	public static void addNpcToFile(NpcDataModel npc, File file) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(file);
		doc.getDocumentElement().normalize();

		Element rootElement = doc.getDocumentElement();
		Element parentElement = null;

		// 確定父元素
		if ("list".equalsIgnoreCase(rootElement.getTagName())) {
			parentElement = rootElement;
		} else if ("npc".equalsIgnoreCase(rootElement.getTagName())) {
			// 需要創建list包裝
			Element listElement = doc.createElement("list");
			doc.replaceChild(listElement, rootElement);
			listElement.appendChild(rootElement);
			parentElement = listElement;
		}

		// 創建新NPC元素
		Element npcElement = createNpcElement(doc, npc);
		parentElement.appendChild(npcElement);

		// 保存文件
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(file);
		transformer.transform(source, result);
	}

	/**
	 * 創建NPC元素（完整版本）
	 */
	private static Element createNpcElement(Document doc, NpcDataModel npc) {
		Element npcElement = doc.createElement("npc");

		// 設置基本屬性
		npcElement.setAttribute("id", String.valueOf(npc.getId()));
		npcElement.setAttribute("displayId", String.valueOf(npc.getDisplayId()));
		npcElement.setAttribute("level", String.valueOf(npc.getLevel()));
		npcElement.setAttribute("type", npc.getType());
		npcElement.setAttribute("name", npc.getName());
		npcElement.setAttribute("usingServerSideName", "true");

		if (npc.getTitle() != null && !npc.getTitle().isEmpty()) {
			npcElement.setAttribute("title", npc.getTitle());
			npcElement.setAttribute("usingServerSideTitle", "true");
		}

		if (npc.getElement() != null && !npc.getElement().isEmpty()) {
			npcElement.setAttribute("element", npc.getElement());
		}

		// 添加種族
		if (npc.getRace() != null && !npc.getRace().isEmpty()) {
			Element raceElement = doc.createElement("race");
			raceElement.setTextContent(npc.getRace());
			npcElement.appendChild(raceElement);
		}

		// 添加性別
		if (npc.getSex() != null && !npc.getSex().isEmpty()) {
			Element sexElement = doc.createElement("sex");
			sexElement.setTextContent(npc.getSex());
			npcElement.appendChild(sexElement);
		}

		// 添加技能列表
		if (!npc.getSkills().isEmpty()) {
			Element skillListElement = doc.createElement("skillList");
			for (SkillEntry skill : npc.getSkills()) {
				Element skillElement = doc.createElement("skill");
				skillElement.setAttribute("id", String.valueOf(skill.getSkillId()));
				skillElement.setAttribute("level", String.valueOf(skill.getLevel()));
				skillListElement.appendChild(skillElement);
			}
			npcElement.appendChild(skillListElement);
		}

		return npcElement;
	}
}