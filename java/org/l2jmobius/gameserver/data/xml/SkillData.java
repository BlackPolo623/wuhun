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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.handler.EffectHandler;
import org.l2jmobius.gameserver.handler.SkillConditionHandler;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.skill.CommonSkill;
import org.l2jmobius.gameserver.model.skill.EffectScope;
import org.l2jmobius.gameserver.model.skill.ISkillCondition;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.SkillConditionScope;

/**
 * The {@code SkillData} class is responsible for parsing, loading, and managing skill data within the game server.
 * @author NosBit, Mobius
 */
public class SkillData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(SkillData.class.getName());
	
	private final Map<Long, Skill> _skillsByHash = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> _maxSkillLevels = new ConcurrentHashMap<>();
	
	private class NamedParamInfo
	{
		private final String _name;
		private final Integer _fromLevel;
		private final Integer _toLevel;
		private final Integer _fromSubLevel;
		private final Integer _toSubLevel;
		private final Map<Integer, Map<Integer, StatSet>> _info;
		
		public NamedParamInfo(String name, Integer fromLevel, Integer toLevel, Integer fromSubLevel, Integer toSubLevel, Map<Integer, Map<Integer, StatSet>> info)
		{
			_name = name;
			_fromLevel = fromLevel;
			_toLevel = toLevel;
			_fromSubLevel = fromSubLevel;
			_toSubLevel = toSubLevel;
			_info = info;
		}
		
		public String getName()
		{
			return _name;
		}
		
		public Integer getFromLevel()
		{
			return _fromLevel;
		}
		
		public Integer getToLevel()
		{
			return _toLevel;
		}
		
		public Integer getFromSubLevel()
		{
			return _fromSubLevel;
		}
		
		public Integer getToSubLevel()
		{
			return _toSubLevel;
		}
		
		public Map<Integer, Map<Integer, StatSet>> getInfo()
		{
			return _info;
		}
	}
	
	protected SkillData()
	{
		load();
	}
	
	@Override
	public boolean isValidating()
	{
		return false;
	}
	
	@Override
	public synchronized void load()
	{
		_skillsByHash.clear();
		_maxSkillLevels.clear();
		
		parseDatapackDirectory("data/stats/skills/", false);
		if (GeneralConfig.CUSTOM_SKILLS_LOAD)
		{
			parseDatapackDirectory("data/stats/skills/custom", false);
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _skillsByHash.size() + " Skills.");
	}
	
	public void reload()
	{
		load();
		
		// Reload Skill Tree as well.
		SkillTreeData.getInstance().load();
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node node = document.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if ("list".equalsIgnoreCase(node.getNodeName()))
			{
				for (Node listNode = node.getFirstChild(); listNode != null; listNode = listNode.getNextSibling())
				{
					if ("skill".equalsIgnoreCase(listNode.getNodeName()))
					{
						NamedNodeMap attributes = listNode.getAttributes();
						final Map<Integer, Set<Integer>> levels = new HashMap<>();
						final Map<Integer, Map<Integer, StatSet>> skillInfo = new HashMap<>();
						final StatSet generalSkillInfo = skillInfo.computeIfAbsent(-1, _ -> new HashMap<>()).computeIfAbsent(-1, _ -> new StatSet());
						parseAttributes(attributes, "", generalSkillInfo);
						
						final Map<String, Map<Integer, Map<Integer, Object>>> variableValues = new HashMap<>();
						final Map<EffectScope, List<NamedParamInfo>> effectParamInfo = new EnumMap<>(EffectScope.class);
						final Map<SkillConditionScope, List<NamedParamInfo>> conditionParamInfo = new EnumMap<>(SkillConditionScope.class);
						for (Node skillNode = listNode.getFirstChild(); skillNode != null; skillNode = skillNode.getNextSibling())
						{
							final String skillNodeName = skillNode.getNodeName();
							switch (skillNodeName.toLowerCase())
							{
								case "variable":
								{
									attributes = skillNode.getAttributes();
									final String name = "@" + parseString(attributes, "name");
									variableValues.put(name, parseValues(skillNode));
									break;
								}
								case "#text":
								{
									break;
								}
								default:
								{
									final EffectScope effectScope = EffectScope.findByXmlNodeName(skillNodeName);
									if (effectScope != null)
									{
										for (Node effectsNode = skillNode.getFirstChild(); effectsNode != null; effectsNode = effectsNode.getNextSibling())
										{
											if ("effect".equalsIgnoreCase(effectsNode.getNodeName()))
											{
												effectParamInfo.computeIfAbsent(effectScope, _ -> new LinkedList<>()).add(parseNamedParamInfo(effectsNode, variableValues));
											}
										}
										break;
									}
									
									final SkillConditionScope skillConditionScope = SkillConditionScope.findByXmlNodeName(skillNodeName);
									if (skillConditionScope != null)
									{
										for (Node conditionNode = skillNode.getFirstChild(); conditionNode != null; conditionNode = conditionNode.getNextSibling())
										{
											if ("condition".equalsIgnoreCase(conditionNode.getNodeName()))
											{
												conditionParamInfo.computeIfAbsent(skillConditionScope, _ -> new LinkedList<>()).add(parseNamedParamInfo(conditionNode, variableValues));
											}
										}
									}
									else
									{
										parseInfo(skillNode, variableValues, skillInfo);
									}
									break;
								}
							}
						}
						
						final int fromLevel = generalSkillInfo.getInt(".fromLevel", 1);
						final int toLevel = generalSkillInfo.getInt(".toLevel", 0);
						for (int i = fromLevel; i <= toLevel; i++)
						{
							levels.computeIfAbsent(i, _ -> new HashSet<>()).add(0);
						}
						
						skillInfo.forEach((level, subLevelMap) ->
						{
							if (level == -1)
							{
								return;
							}
							
							subLevelMap.forEach((subLevel, _) ->
							{
								if (subLevel == -1)
								{
									return;
								}
								
								levels.computeIfAbsent(level, _ -> new HashSet<>()).add(subLevel);
							});
						});
						
						Stream.concat(effectParamInfo.values().stream(), conditionParamInfo.values().stream()).forEach(namedParamInfos -> namedParamInfos.forEach(namedParamInfo ->
						{
							namedParamInfo.getInfo().forEach((level, subLevelMap) ->
							{
								if (level == -1)
								{
									return;
								}
								
								subLevelMap.forEach((subLevel, _) ->
								{
									if (subLevel == -1)
									{
										return;
									}
									
									levels.computeIfAbsent(level, _ -> new HashSet<>()).add(subLevel);
								});
							});
							
							if ((namedParamInfo.getFromLevel() != null) && (namedParamInfo.getToLevel() != null))
							{
								for (int i = namedParamInfo.getFromLevel(); i <= namedParamInfo.getToLevel(); i++)
								{
									if ((namedParamInfo.getFromSubLevel() != null) && (namedParamInfo.getToSubLevel() != null))
									{
										for (int j = namedParamInfo.getFromSubLevel(); j <= namedParamInfo.getToSubLevel(); j++)
										{
											levels.computeIfAbsent(i, _ -> new HashSet<>()).add(j);
										}
									}
									else
									{
										levels.computeIfAbsent(i, _ -> new HashSet<>()).add(0);
									}
								}
							}
						}));
						
						levels.forEach((level, subLevels) -> subLevels.forEach(subLevel ->
						{
							final StatSet statSet = Optional.ofNullable(skillInfo.getOrDefault(level, Collections.emptyMap()).get(subLevel)).orElseGet(StatSet::new);
							skillInfo.getOrDefault(level, Collections.emptyMap()).getOrDefault(-1, StatSet.EMPTY_STATSET).getSet().forEach(statSet.getSet()::putIfAbsent);
							skillInfo.getOrDefault(-1, Collections.emptyMap()).getOrDefault(-1, StatSet.EMPTY_STATSET).getSet().forEach(statSet.getSet()::putIfAbsent);
							statSet.set(".level", level);
							statSet.set(".subLevel", subLevel);
							final Skill skill = new Skill(statSet);
							forEachNamedParamInfoParam(effectParamInfo, level, subLevel, ((effectScope, params) ->
							{
								final String effectName = params.getString(".name");
								params.remove(".name");
								try
								{
									final Function<StatSet, AbstractEffect> effectFunction = EffectHandler.getInstance().getHandlerFactory(effectName);
									if (effectFunction != null)
									{
										skill.addEffect(effectScope, effectFunction.apply(params));
									}
									else
									{
										LOGGER.warning(getClass().getSimpleName() + ": Missing effect for Skill Id[" + statSet.getInt(".id") + "] Level[" + level + "] SubLevel[" + subLevel + "] Effect Scope[" + effectScope + "] Effect Name[" + effectName + "]");
									}
								}
								catch (Exception e)
								{
									LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed loading effect for Skill Id[" + statSet.getInt(".id") + "] Level[" + level + "] SubLevel[" + subLevel + "] Effect Scope[" + effectScope + "] Effect Name[" + effectName + "]", e);
								}
							}));
							
							forEachNamedParamInfoParam(conditionParamInfo, level, subLevel, ((skillConditionScope, params) ->
							{
								final String conditionName = params.getString(".name");
								params.remove(".name");
								try
								{
									final Function<StatSet, ISkillCondition> conditionFunction = SkillConditionHandler.getInstance().getHandlerFactory(conditionName);
									if (conditionFunction != null)
									{
										if (skill.isPassive())
										{
											if (skillConditionScope != SkillConditionScope.PASSIVE)
											{
												LOGGER.warning(getClass().getSimpleName() + ": Non passive condition for passive Skill Id[" + statSet.getInt(".id") + "] Level[" + level + "] SubLevel[" + subLevel + "]");
											}
										}
										else if (skillConditionScope == SkillConditionScope.PASSIVE)
										{
											LOGGER.warning(getClass().getSimpleName() + ": Passive condition for non passive Skill Id[" + statSet.getInt(".id") + "] Level[" + level + "] SubLevel[" + subLevel + "]");
										}
										
										skill.addCondition(skillConditionScope, conditionFunction.apply(params));
									}
									else
									{
										LOGGER.warning(getClass().getSimpleName() + ": Missing condition for Skill Id[" + statSet.getInt(".id") + "] Level[" + level + "] SubLevel[" + subLevel + "] Effect Scope[" + skillConditionScope + "] Effect Name[" + conditionName + "]");
									}
								}
								catch (Exception e)
								{
									LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed loading condition for Skill Id[" + statSet.getInt(".id") + "] Level[" + level + "] SubLevel[" + subLevel + "] Condition Scope[" + skillConditionScope + "] Condition Name[" + conditionName + "]", e);
								}
							}));
							
							_skillsByHash.put(getSkillHashCode(skill), skill);
							_maxSkillLevels.merge(skill.getId(), skill.getLevel(), Integer::max);
							if ((skill.getSubLevel() % 1000) == 1)
							{
								EnchantSkillGroupsData.getInstance().addRouteForSkill(skill.getId(), skill.getLevel(), skill.getSubLevel());
							}
						}));
					}
				}
			}
		}
	}
	
	/**
	 * Iterates over a map of parameter info entries and applies a specified consumer action on each entry that matches the provided level and subLevel.
	 * <p>
	 * The method filters each {@link NamedParamInfo} entry based on level and subLevel ranges. If a match is found, it creates or retrieves a {@link StatSet} for the given levels, fills in parameter values, and applies the specified action.
	 * </p>
	 * @param paramInfo The map containing scope keys and corresponding lists of {@link NamedParamInfo}.
	 * @param level The level to filter and process the named parameter information.
	 * @param subLevel The sub-level to filter and process the named parameter information.
	 * @param consumer The consumer action to apply on each matching parameter information and {@link StatSet}.
	 * @param <T> The type of the scope keys used in the map.
	 */
	private <T> void forEachNamedParamInfoParam(Map<T, List<NamedParamInfo>> paramInfo, int level, int subLevel, BiConsumer<T, StatSet> consumer)
	{
		paramInfo.forEach((scope, namedParamInfos) -> namedParamInfos.forEach(namedParamInfo ->
		{
			if ((((namedParamInfo.getFromLevel() == null) && (namedParamInfo.getToLevel() == null)) || ((namedParamInfo.getFromLevel() <= level) && (namedParamInfo.getToLevel() >= level))) //
				&& (((namedParamInfo.getFromSubLevel() == null) && (namedParamInfo.getToSubLevel() == null)) || ((namedParamInfo.getFromSubLevel() <= subLevel) && (namedParamInfo.getToSubLevel() >= subLevel))))
			{
				final StatSet params = Optional.ofNullable(namedParamInfo.getInfo().getOrDefault(level, Collections.emptyMap()).get(subLevel)).orElseGet(StatSet::new);
				namedParamInfo.getInfo().getOrDefault(level, Collections.emptyMap()).getOrDefault(-1, StatSet.EMPTY_STATSET).getSet().forEach(params.getSet()::putIfAbsent);
				namedParamInfo.getInfo().getOrDefault(-1, Collections.emptyMap()).getOrDefault(-1, StatSet.EMPTY_STATSET).getSet().forEach(params.getSet()::putIfAbsent);
				params.set(".name", namedParamInfo.getName());
				consumer.accept(scope, params);
			}
		}));
	}
	
	/**
	 * Parses a {@link NamedParamInfo} from a given XML node, setting up ranges and level mappings.
	 * <p>
	 * This method reads attribute data such as name, level, and sub-level ranges from the node to create a {@link NamedParamInfo}. Nested XML elements are also parsed into a structure of {@link StatSet} entries mapped by level and sub-level.
	 * </p>
	 * @param node The XML node representing the parameter information.
	 * @param variableValues A map containing variable values for substitution.
	 * @return A {@link NamedParamInfo} object containing parsed data.
	 */
	private NamedParamInfo parseNamedParamInfo(Node node, Map<String, Map<Integer, Map<Integer, Object>>> variableValues)
	{
		Node n = node;
		final NamedNodeMap attributes = n.getAttributes();
		final String name = parseString(attributes, "name");
		final Integer level = parseInteger(attributes, "level");
		final Integer fromLevel = parseInteger(attributes, "fromLevel", level);
		final Integer toLevel = parseInteger(attributes, "toLevel", level);
		final Integer subLevel = parseInteger(attributes, "subLevel");
		final Integer fromSubLevel = parseInteger(attributes, "fromSubLevel", subLevel);
		final Integer toSubLevel = parseInteger(attributes, "toSubLevel", subLevel);
		final Map<Integer, Map<Integer, StatSet>> info = new HashMap<>();
		for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if (!n.getNodeName().equals("#text"))
			{
				parseInfo(n, variableValues, info);
			}
		}
		
		return new NamedParamInfo(name, fromLevel, toLevel, fromSubLevel, toSubLevel, info);
	}
	
	/**
	 * Parses detailed information from a node into a level-subLevel mapping structure, applying variable values if defined.
	 * <p>
	 * The method interprets nested nodes as specific level and sub-level entries. Values can reference variables with "@" syntax, which are resolved using the provided variable values map.
	 * </p>
	 * @param node The XML node containing information entries.
	 * @param variableValues A map containing variable values for substitution within the node's attributes.
	 * @param info A map that organizes {@link StatSet} entries by levels and sub-levels.
	 */
	private void parseInfo(Node node, Map<String, Map<Integer, Map<Integer, Object>>> variableValues, Map<Integer, Map<Integer, StatSet>> info)
	{
		Map<Integer, Map<Integer, Object>> values = parseValues(node);
		final Object generalValue = values.getOrDefault(-1, Collections.emptyMap()).get(-1);
		if (generalValue != null)
		{
			final String stringGeneralValue = String.valueOf(generalValue);
			if (stringGeneralValue.startsWith("@"))
			{
				final Map<Integer, Map<Integer, Object>> variableValue = variableValues.get(stringGeneralValue);
				if (variableValue != null)
				{
					values = variableValue;
				}
				else
				{
					throw new IllegalArgumentException("undefined variable " + stringGeneralValue);
				}
			}
		}
		
		values.forEach((level, subLevelMap) -> subLevelMap.forEach((subLevel, value) -> info.computeIfAbsent(level, _ -> new HashMap<>()).computeIfAbsent(subLevel, _ -> new StatSet()).set(node.getNodeName(), value)));
	}
	
	/**
	 * Parses values from an XML node into a hierarchical map of levels and sub-levels, supporting variable substitution.
	 * <p>
	 * This method processes each nested "value" node to build a mapping of level and sub-level to specific values. It supports referencing base values and calculating new ones using variables such as "index" and "subIndex".
	 * </p>
	 * @param node The XML node containing value definitions.
	 * @return A map with values organized by level and sub-level, potentially using variable substitutions.
	 */
	private Map<Integer, Map<Integer, Object>> parseValues(Node node)
	{
		Node n = node;
		final Map<Integer, Map<Integer, Object>> values = new HashMap<>();
		Object parsedValue = parseValue(n, true, false, Collections.emptyMap());
		if (parsedValue != null)
		{
			values.computeIfAbsent(-1, _ -> new HashMap<>()).put(-1, parsedValue);
		}
		else
		{
			for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if (n.getNodeName().equalsIgnoreCase("value"))
				{
					final NamedNodeMap attributes = n.getAttributes();
					final Integer level = parseInteger(attributes, "level");
					if (level != null)
					{
						parsedValue = parseValue(n, false, false, Collections.emptyMap());
						if (parsedValue != null)
						{
							final Integer subLevel = parseInteger(attributes, "subLevel", -1);
							values.computeIfAbsent(level, _ -> new HashMap<>()).put(subLevel, parsedValue);
						}
					}
					else
					{
						final int fromLevel = parseInteger(attributes, "fromLevel");
						final int toLevel = parseInteger(attributes, "toLevel");
						final int fromSubLevel = parseInteger(attributes, "fromSubLevel", -1);
						final int toSubLevel = parseInteger(attributes, "toSubLevel", -1);
						for (int i = fromLevel; i <= toLevel; i++)
						{
							for (int j = fromSubLevel; j <= toSubLevel; j++)
							{
								final Map<Integer, Object> subValues = values.computeIfAbsent(i, _ -> new HashMap<>());
								final Map<String, Double> variables = new HashMap<>();
								variables.put("index", (i - fromLevel) + 1d);
								variables.put("subIndex", (j - fromSubLevel) + 1d);
								final Object base = values.getOrDefault(i, Collections.emptyMap()).get(-1);
								final String baseText = String.valueOf(base);
								if ((base != null) && !(base instanceof StatSet) && (!baseText.equalsIgnoreCase("true") && !baseText.equalsIgnoreCase("false")))
								{
									variables.put("base", Double.parseDouble(baseText));
								}
								
								parsedValue = parseValue(n, false, false, variables);
								if (parsedValue != null)
								{
									subValues.put(j, parsedValue);
								}
							}
						}
					}
				}
			}
		}
		
		return values;
	}
	
	/**
	 * Parses a single value or nested values from an XML node into a corresponding data structure.
	 * <p>
	 * This method examines the node's content and attributes to construct a {@link StatSet}, list, or basic text. If expressions are enclosed in "{}" within the value, they are evaluated with the given variables.
	 * </p>
	 * @param node The node containing the value or nested values.
	 * @param blockValue If {@code true}, prevents further parsing of nested values within the node.
	 * @param parseAttributes Whether to parse the node's attributes into the result.
	 * @param variables A map of variables for evaluating expressions within the value.
	 * @return An object representing the parsed value, which may be a {@link StatSet}, list, or plain text.
	 */
	protected Object parseValue(Node node, boolean blockValue, boolean parseAttributes, Map<String, Double> variables)
	{
		Node n = node;
		StatSet statSet = null;
		List<Object> list = null;
		Object text = null;
		if (parseAttributes && (!n.getNodeName().equals("value") || !blockValue) && (n.getAttributes().getLength() > 0))
		{
			statSet = new StatSet();
			parseAttributes(n.getAttributes(), "", statSet, variables);
		}
		
		for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
		{
			final String nodeName = n.getNodeName();
			switch (n.getNodeName())
			{
				case "#text":
				{
					final String value = n.getNodeValue().trim();
					if (!value.isEmpty())
					{
						text = parseNodeValue(value, variables);
					}
					break;
				}
				case "item":
				{
					if (list == null)
					{
						list = new LinkedList<>();
					}
					
					final Object value = parseValue(n, false, true, variables);
					if (value != null)
					{
						list.add(value);
					}
					break;
				}
				case "value":
				{
					if (blockValue)
					{
						break;
					}
					// fallthrough
				}
				default:
				{
					final Object value = parseValue(n, false, true, variables);
					if (value != null)
					{
						if (statSet == null)
						{
							statSet = new StatSet();
						}
						
						statSet.set(nodeName, value);
					}
				}
			}
		}
		
		if (list != null)
		{
			if (text != null)
			{
				throw new IllegalArgumentException("Text and list in same node are not allowed. Node[" + n + "]");
			}
			
			if (statSet != null)
			{
				statSet.set(".", list);
			}
			else
			{
				return list;
			}
		}
		
		if (text != null)
		{
			if (list != null)
			{
				throw new IllegalArgumentException("Text and list in same node are not allowed. Node[" + n + "]");
			}
			
			if (statSet != null)
			{
				statSet.set(".", text);
			}
			else
			{
				return text;
			}
		}
		
		return statSet;
	}
	
	/**
	 * Parses the attributes of an XML node into a {@link StatSet} without any variable substitutions.
	 * <p>
	 * This method iterates through each attribute of the node, storing them in the specified {@link StatSet}. Each attribute name is prefixed with the specified string. This overload of the method does not support variable evaluation within attribute values.
	 * </p>
	 * @param attributes The attributes to parse.
	 * @param prefix A prefix to add to each attribute name when storing in the {@link StatSet}.
	 * @param statSet The {@link StatSet} where parsed attributes will be stored.
	 */
	private void parseAttributes(NamedNodeMap attributes, String prefix, StatSet statSet)
	{
		parseAttributes(attributes, prefix, statSet, Collections.emptyMap());
	}
	
	/**
	 * Parses the attributes of an XML node into a {@link StatSet}, optionally using variable substitution.
	 * <p>
	 * This method iterates over the attributes of the node, creating entries in the StatSet for each one. If any attribute value is an expression enclosed in "{}", it is evaluated using the provided variables map.
	 * </p>
	 * @param attributes The attributes of the XML node.
	 * @param prefix A prefix to prepend to each attribute name in the StatSet.
	 * @param statSet The StatSet in which to store parsed attributes.
	 * @param variables A map of variables for evaluating expressions within attribute values.
	 */
	private void parseAttributes(NamedNodeMap attributes, String prefix, StatSet statSet, Map<String, Double> variables)
	{
		for (int i = 0; i < attributes.getLength(); i++)
		{
			final Node attributeNode = attributes.item(i);
			statSet.set(prefix + "." + attributeNode.getNodeName(), parseNodeValue(attributeNode.getNodeValue(), variables));
		}
	}
	
	/**
	 * Parses a value string and evaluates it if it contains an expression in curly braces.
	 * <p>
	 * If the value string is enclosed in "{}", the inner expression is extracted, trimmed, and evaluated using the provided variables. Otherwise, the method returns the original value string.
	 * </p>
	 * @param value The value string to be parsed, which may contain an expression in curly braces.
	 * @param variables A map of variables to be used in expression evaluation.
	 * @return The evaluated expression result if in curly braces; otherwise, the original value string.
	 * @throws IllegalArgumentException if the expression within curly braces is empty.
	 */
	private Object parseNodeValue(String value, Map<String, Double> variables)
	{
		if (value.startsWith("{") && value.endsWith("}"))
		{
			final String expression = value.substring(1, value.length() - 1).trim();
			if (expression.isEmpty())
			{
				throw new IllegalArgumentException("Empty expression inside {}.");
			}
			
			return evaluateExpression(expression, variables);
		}
		
		return value;
	}
	
	/**
	 * Evaluates a mathematical expression given in infix notation using variables.
	 * <p>
	 * The method first converts the infix expression to postfix notation, then evaluates it to return the result.
	 * </p>
	 * @param expression The mathematical expression in infix notation.
	 * @param variables A map of variable names and their values to be used in the expression.
	 * @return The evaluated result of the expression.
	 */
	private double evaluateExpression(String expression, Map<String, Double> variables)
	{
		final String postfix = toPostfix(expression, variables);
		return evaluatePostfix(postfix);
	}
	
	/**
	 * Converts an infix expression to postfix notation for easier evaluation.
	 * <p>
	 * This method uses the Shunting Yard algorithm to handle operator precedence and parentheses. Variable names are replaced by their corresponding values from the variables map.
	 * </p>
	 * @param expression The infix expression to convert.
	 * @param variables A map of variable names and their values for substitution in the expression.
	 * @return The expression in postfix notation as a space-separated string.
	 * @throws IllegalArgumentException if there is an invalid syntax, such as an isolated minus sign.
	 */
	private String toPostfix(String expression, Map<String, Double> variables)
	{
		final Deque<String> operators = new ArrayDeque<>();
		final StringBuilder postfix = new StringBuilder();
		
		final StringTokenizer tokenizer = new StringTokenizer(expression, "+-*/() ", true);
		boolean expectNumber = true;
		
		while (tokenizer.hasMoreTokens())
		{
			String token = tokenizer.nextToken().trim();
			if (token.isEmpty())
			{
				continue;
			}
			
			if (variables.containsKey(token))
			{
				token = variables.get(token).toString();
			}
			
			if (isNumeric(token))
			{
				postfix.append(token).append(' ');
				expectNumber = false;
			}
			else if (token.equals("-") && expectNumber)
			{
				final String nextToken = tokenizer.hasMoreTokens() ? tokenizer.nextToken().trim() : null;
				if ((nextToken != null) && isNumeric(nextToken))
				{
					postfix.append("-").append(nextToken).append(" ");
					expectNumber = false;
				}
				else
				{
					throw new IllegalArgumentException("Invalid syntax near '-' in expression.");
				}
			}
			else if (isOperator(token))
			{
				while (!operators.isEmpty() && (precedence(operators.peek()) >= precedence(token)))
				{
					postfix.append(operators.pop()).append(' ');
				}
				
				operators.push(token);
				expectNumber = true;
			}
			else if (token.equals("("))
			{
				operators.push(token);
				expectNumber = true;
			}
			else if (token.equals(")"))
			{
				while (!operators.isEmpty() && !operators.peek().equals("("))
				{
					postfix.append(operators.pop()).append(' ');
				}
				
				operators.pop();
				expectNumber = false;
			}
		}
		
		while (!operators.isEmpty())
		{
			postfix.append(operators.pop()).append(' ');
		}
		
		return postfix.toString().trim();
	}
	
	/**
	 * Evaluates a mathematical expression in postfix notation.
	 * <p>
	 * Operators are applied sequentially to operands using a stack. The final result is left on top of the stack if the postfix expression is valid.
	 * </p>
	 * @param postfix The postfix notation expression as a space-separated string.
	 * @return The evaluated result of the postfix expression.
	 * @throws IllegalStateException if there are insufficient operands for an operator or if the result is not a single value.
	 */
	private double evaluatePostfix(String postfix)
	{
		final Deque<Double> stack = new ArrayDeque<>();
		final StringTokenizer tokenizer = new StringTokenizer(postfix);
		while (tokenizer.hasMoreTokens())
		{
			final String token = tokenizer.nextToken();
			if (isNumeric(token))
			{
				stack.push(Double.parseDouble(token));
			}
			else if (isOperator(token))
			{
				if (stack.size() < 2)
				{
					throw new IllegalStateException("Not enough operands for the operator: " + token);
				}
				
				final double b = stack.pop();
				final double a = stack.pop();
				switch (token)
				{
					case "+":
					{
						stack.push(a + b);
						break;
					}
					case "-":
					{
						stack.push(a - b);
						break;
					}
					case "*":
					{
						stack.push(a * b);
						break;
					}
					case "/":
					{
						stack.push(a / b);
						break;
					}
				}
			}
		}
		
		if (stack.size() != 1)
		{
			throw new IllegalStateException("The postfix expression did not evaluate to a single result.");
		}
		
		return stack.pop();
	}
	
	/**
	 * Checks if a given string is numeric, representing a valid double value.
	 * @param token The string to check.
	 * @return {@code true} if the string is numeric; {@code false} otherwise.
	 */
	private boolean isNumeric(String token)
	{
		try
		{
			Double.parseDouble(token);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}
	
	/**
	 * Checks if a given string is a mathematical operator.
	 * @param token The string to check.
	 * @return {@code true} if the string is one of "+", "-", "*", or "/"; {@code false} otherwise.
	 */
	private boolean isOperator(String token)
	{
		return "+-*/".contains(token);
	}
	
	/**
	 * Returns the precedence level of a mathematical operator.
	 * <p>
	 * Operators "*" and "/" have higher precedence (2) than "+" and "-" (1).
	 * </p>
	 * @param operator The operator whose precedence level is to be determined.
	 * @return The precedence level of the operator, or -1 if it is not a recognized operator.
	 */
	private int precedence(String operator)
	{
		switch (operator)
		{
			case "+":
			case "-":
			{
				return 1;
			}
			case "*":
			case "/":
			{
				return 2;
			}
			default:
			{
				return -1;
			}
		}
	}
	
	/**
	 * Generates a unique hash code for a skill based on its ID, level, and sub-level.
	 * @param skill The {@link Skill} instance to be hashed.
	 * @return A unique hash code combining skill ID, level, and sub-level.
	 */
	public static long getSkillHashCode(Skill skill)
	{
		return getSkillHashCode(skill.getId(), skill.getLevel(), skill.getSubLevel());
	}
	
	/**
	 * Generates a unique hash code for a skill based on its ID and level, with sub-level defaulted to zero.
	 * @param skillId The ID of the skill.
	 * @param skillLevel The level of the skill.
	 * @return A unique hash code combining skill ID and level.
	 */
	public static long getSkillHashCode(int skillId, int skillLevel)
	{
		return getSkillHashCode(skillId, skillLevel, 0);
	}
	
	/**
	 * Generates a unique hash code for a skill based on its ID, level, and sub-level.
	 * @param skillId The ID of the skill.
	 * @param skillLevel The level of the skill.
	 * @param subSkillLevel The sub-level of the skill.
	 * @return A unique hash code combining skill ID, level, and sub-level.
	 */
	public static long getSkillHashCode(int skillId, int skillLevel, int subSkillLevel)
	{
		return (skillId * 4294967296L) + (subSkillLevel * 65536) + skillLevel;
	}
	
	/**
	 * Retrieves a skill based on its ID and level, defaulting the sub-level to zero.
	 * @param skillId The ID of the skill.
	 * @param level The level of the skill.
	 * @return The {@link Skill} object if found, or null if not found.
	 */
	public Skill getSkill(int skillId, int level)
	{
		return getSkill(skillId, level, 0);
	}
	
	/**
	 * Retrieves a skill based on its ID, level, and sub-level.
	 * <p>
	 * If the specified skill level is not found, and the requested level is greater than the maximum level for the skill ID, the method will return the highest available level. Logs a warning if the requested skill level exceeds the maximum available level or if no skill data is found.
	 * </p>
	 * @param skillId The ID of the skill.
	 * @param level The level of the skill.
	 * @param subLevel The sub-level of the skill.
	 * @return The {@link Skill} object if found, or null if not found.
	 */
	public Skill getSkill(int skillId, int level, int subLevel)
	{
		final Skill result = _skillsByHash.get(getSkillHashCode(skillId, level, subLevel));
		if (result != null)
		{
			return result;
		}
		
		// Skill/level not found, fix for transformation scripts.
		final int maxLevel = getMaxLevel(skillId);
		
		// Requested level too high.
		if ((maxLevel > 0) && (level > maxLevel))
		{
			LOGGER.warning(StringUtil.concat(getClass().getSimpleName(), ": Call to unexisting skill level id: ", String.valueOf(skillId), " requested level: ", String.valueOf(level), " max level: ", String.valueOf(maxLevel), ".", System.lineSeparator(), TraceUtil.getStackTrace(new Exception())));
			return _skillsByHash.get(getSkillHashCode(skillId, maxLevel));
		}
		
		LOGGER.warning(StringUtil.concat(getClass().getSimpleName(), ": No skill info found for skill id ", String.valueOf(skillId), " and skill level ", String.valueOf(level), ".", System.lineSeparator(), TraceUtil.getStackTrace(new Exception())));
		return null;
	}
	
	/**
	 * Retrieves the maximum level available for a skill based on its ID.
	 * @param skillId The ID of the skill.
	 * @return The maximum level for the specified skill ID, or 0 if the skill ID is not found.
	 */
	public int getMaxLevel(int skillId)
	{
		final Integer maxLevel = _maxSkillLevels.get(skillId);
		return maxLevel != null ? maxLevel : 0;
	}
	
	/**
	 * Retrieves a list of siege-related skills based on certain conditions.
	 * <p>
	 * If {@code addNoble} is true, the list will include the Advanced Headquarters skill. If {@code hasCastle} is true, additional castle-specific skills such as Outpost Construction and Outpost Demolition are added.
	 * </p>
	 * @param addNoble Whether to include Advanced Headquarters skill.
	 * @param hasCastle Whether to include castle-related skills.
	 * @return A {@link List} of siege-related {@link Skill} objects.
	 */
	public List<Skill> getSiegeSkills(boolean addNoble, boolean hasCastle)
	{
		final List<Skill> result = new LinkedList<>();
		result.add(_skillsByHash.get(getSkillHashCode(CommonSkill.SEAL_OF_RULER.getId(), 1)));
		result.add(_skillsByHash.get(getSkillHashCode(247, 1))); // Build Headquarters
		if (addNoble)
		{
			result.add(_skillsByHash.get(getSkillHashCode(326, 1))); // Build Advanced Headquarters
		}
		
		if (hasCastle)
		{
			result.add(_skillsByHash.get(getSkillHashCode(844, 1))); // Outpost Construction
			result.add(_skillsByHash.get(getSkillHashCode(845, 1))); // Outpost Demolition
		}
		
		return result;
	}
	
	public static SkillData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SkillData INSTANCE = new SkillData();
	}
}
