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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.DevelopmentConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.holders.npc.ChanceLocation;
import org.l2jmobius.gameserver.model.actor.holders.npc.MinionHolder;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.interfaces.IParameterized;
import org.l2jmobius.gameserver.model.interfaces.ITerritorized;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.spawns.NpcSpawnTemplate;
import org.l2jmobius.gameserver.model.spawns.SpawnGroup;
import org.l2jmobius.gameserver.model.spawns.SpawnTemplate;
import org.l2jmobius.gameserver.model.zone.ZoneForm;
import org.l2jmobius.gameserver.model.zone.form.ZoneCuboid;
import org.l2jmobius.gameserver.model.zone.form.ZoneCylinder;
import org.l2jmobius.gameserver.model.zone.form.ZoneNPoly;
import org.l2jmobius.gameserver.model.zone.type.BannedSpawnTerritory;
import org.l2jmobius.gameserver.model.zone.type.SpawnTerritory;

/**
 * @author UnAfraid, Mobius
 */
public class SpawnData implements IXmlReader
{
	protected static final Logger LOGGER = Logger.getLogger(SpawnData.class.getName());
	
	private static final String OTHER_XML_FOLDER = "data/spawns/Others";
	
	private final Collection<SpawnTemplate> _spawnTemplates = ConcurrentHashMap.newKeySet();
	
	protected SpawnData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackDirectory("data/spawns", true);
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _spawnTemplates.stream().flatMap(c -> c.getGroups().stream()).flatMap(c -> c.getSpawns().stream()).count() + " spawns");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "spawn", spawnNode ->
		{
			try
			{
				parseSpawn(spawnNode, file, _spawnTemplates);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error while processing spawn in file: " + file.getAbsolutePath(), e);
			}
		}));
	}
	
	/**
	 * Parses a spawn configuration from an XML file and adds it to the collection of spawn templates.
	 * <p>
	 * This method iterates through child elements within a "spawn" XML node, creating the relevant {@link SpawnTemplate} and adding groups, territories, NPCs, and parameters based on the node's content.
	 * </p>
	 * @param spawnsNode the {@link Node} positioned at the "spawn" node
	 * @param file the {@link File} representing the source XML file for logging and error reporting
	 * @param spawns the collection of {@link SpawnTemplate} to which the parsed spawn template will be added
	 */
	public void parseSpawn(Node spawnsNode, File file, Collection<SpawnTemplate> spawns)
	{
		final SpawnTemplate spawnTemplate = new SpawnTemplate(new StatSet(parseAttributes(spawnsNode)), file);
		SpawnGroup defaultGroup = null;
		for (Node innerNode = spawnsNode.getFirstChild(); innerNode != null; innerNode = innerNode.getNextSibling())
		{
			switch (innerNode.getNodeName())
			{
				case "territories":
				{
					parseTerritories(innerNode, spawnTemplate.getFile(), spawnTemplate);
					break;
				}
				case "group":
				{
					parseGroup(innerNode, spawnTemplate);
					break;
				}
				case "npc":
				{
					if (defaultGroup == null)
					{
						defaultGroup = new SpawnGroup(StatSet.EMPTY_STATSET);
					}
					
					parseNpc(innerNode, spawnTemplate, defaultGroup);
					break;
				}
				case "parameters":
				{
					parseParameters(innerNode, spawnTemplate);
					break;
				}
			}
		}
		
		// Add the default group if it was populated.
		if (defaultGroup != null)
		{
			spawnTemplate.addGroup(defaultGroup);
		}
		
		spawns.add(spawnTemplate);
	}
	
	/**
	 * Parses territories from the XML stream, adding them to the given spawn template.
	 * <p>
	 * Each "territory" or "banned_territory" node within the XML adds a territory to the spawn template with attributes such as name, shape, and boundary coordinates.
	 * </p>
	 * @param innerNode the {@link Node} positioned at the "territories" node
	 * @param file the {@link File} representing the source XML file, used for default naming if needed
	 * @param spawnTemplate the {@link ITerritorized} template to which the parsed territories will be added
	 */
	private void parseTerritories(Node innerNode, File file, ITerritorized spawnTemplate)
	{
		forEach(innerNode, IXmlReader::isNode, territoryNode ->
		{
			final String name = parseString(territoryNode.getAttributes(), "name", file.getName() + "_" + (spawnTemplate.getTerritories().size() + 1));
			final int minZ = parseInteger(territoryNode.getAttributes(), "minZ");
			final int maxZ = parseInteger(territoryNode.getAttributes(), "maxZ");
			final List<Integer> xNodes = new ArrayList<>();
			final List<Integer> yNodes = new ArrayList<>();
			
			// Iterate over each "node" element within the territory.
			forEach(territoryNode, "node", node ->
			{
				xNodes.add(parseInteger(node.getAttributes(), "x"));
				yNodes.add(parseInteger(node.getAttributes(), "y"));
			});
			
			final int[] x = xNodes.stream().mapToInt(Integer::valueOf).toArray();
			final int[] y = yNodes.stream().mapToInt(Integer::valueOf).toArray();
			
			// Determine the ZoneForm based on the "shape" attribute.
			ZoneForm zoneForm = null;
			final String zoneShape = parseString(territoryNode.getAttributes(), "shape", "NPoly");
			switch (zoneShape)
			{
				case "Cuboid":
				{
					zoneForm = new ZoneCuboid(x[0], x[1], y[0], y[1], minZ, maxZ);
					break;
				}
				case "NPoly":
				{
					zoneForm = new ZoneNPoly(x, y, minZ, maxZ);
					break;
				}
				case "Cylinder":
				{
					final int zoneRad = Integer.parseInt(territoryNode.getAttributes().getNamedItem("rad").getNodeValue());
					zoneForm = new ZoneCylinder(x[0], y[0], minZ, maxZ, zoneRad);
					break;
				}
			}
			
			// Add the territory or banned territory to spawnTemplate.
			switch (territoryNode.getNodeName())
			{
				case "territory":
				{
					spawnTemplate.addTerritory(new SpawnTerritory(name, zoneForm));
					break;
				}
				case "banned_territory":
				{
					spawnTemplate.addBannedTerritory(new BannedSpawnTerritory(name, zoneForm));
					break;
				}
			}
		});
	}
	
	/**
	 * Parses a group node from the XML and adds it to the specified spawn template.
	 * <p>
	 * This method creates a {@link SpawnGroup} with the attributes specified in the "group" node, then parses any child nodes, such as territories or NPCs, that further configure the group.
	 * </p>
	 * @param n the {@link Node} positioned at the "group" node
	 * @param spawnTemplate the {@link SpawnTemplate} to which the parsed group will be added
	 */
	private void parseGroup(Node n, SpawnTemplate spawnTemplate)
	{
		// Initialize SpawnGroup with parsed attributes.
		final SpawnGroup group = new SpawnGroup(new StatSet(parseAttributes(n)));
		
		// Iterate over each child element within the "group" node.
		forEach(n, IXmlReader::isNode, npcNode ->
		{
			switch (npcNode.getNodeName())
			{
				case "territories":
				{
					parseTerritories(npcNode, spawnTemplate.getFile(), group);
					break;
				}
				case "npc":
				{
					parseNpc(npcNode, spawnTemplate, group);
					break;
				}
			}
		});
		
		// Add the group to the spawnTemplate.
		spawnTemplate.addGroup(group);
	}
	
	/**
	 * Parses an NPC node from the XML and adds it to the specified spawn group.
	 * <p>
	 * This method creates an {@link NpcSpawnTemplate} based on the parsed attributes, checking the validity of the NPC type. The NPC is then added to the spawn group, with additional parsing for parameters, minions, and locations as defined in the XML.
	 * </p>
	 * @param n the {@link Node} positioned at the "npc" node
	 * @param spawnTemplate the {@link SpawnTemplate} to which the NPC belongs
	 * @param group the {@link SpawnGroup} to which the NPC spawn template will be added
	 */
	private void parseNpc(Node n, SpawnTemplate spawnTemplate, SpawnGroup group)
	{
		// Initialize the NpcSpawnTemplate.
		final NpcSpawnTemplate npcTemplate = new NpcSpawnTemplate(spawnTemplate, group, new StatSet(parseAttributes(n)));
		final int npcId = npcTemplate.getId();
		final NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
		
		if (template == null)
		{
			// Log a warning for invalid NPC IDs outside the fake player range [80000, 89999].
			// This hardcoded check provides a resource-constrained solution to prevent unnecessary logging.
			if (FakePlayersConfig.FAKE_PLAYERS_ENABLED || ((npcId < 80000) && (npcId > 89999)))
			{
				LOGGER.warning(getClass().getSimpleName() + ": Requested spawn for non-existing NPC: " + npcId + " in file: " + spawnTemplate.getFile().getName());
			}
			return;
		}
		
		// Validate NPC type.
		if (template.isType("Servitor") || template.isType("Pet"))
		{
			LOGGER.warning(getClass().getSimpleName() + ": Requested spawn for " + template.getType() + " " + template.getName() + "(" + template.getId() + ") in file: " + spawnTemplate.getFile().getName());
			return;
		}
		
		if (!FakePlayersConfig.FAKE_PLAYERS_ENABLED && template.isFakePlayer())
		{
			return;
		}
		
		// Iterate through child elements of the NPC node.
		for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
		{
			if ("parameters".equalsIgnoreCase(d.getNodeName()))
			{
				parseParameters(d, npcTemplate);
			}
			else if ("minions".equalsIgnoreCase(d.getNodeName()))
			{
				parseMinions(d, npcTemplate);
			}
			else if ("locations".equalsIgnoreCase(d.getNodeName()))
			{
				parseLocations(d, npcTemplate);
			}
		}
		
		// Add NPC template to the group.
		group.addSpawn(npcTemplate);
	}
	
	/**
	 * Parses parameter nodes from the XML and sets them on the specified template.
	 * <p>
	 * This method processes "parameter" child nodes, extracting name-value pairs, and assigns the parameters to the {@link IParameterized} template. If no parameters are found, an empty {@link StatSet} is used.
	 * </p>
	 * @param node the {@link Node} positioned at the "parameters" node
	 * @param npcTemplate the {@link IParameterized} template to which the parsed parameters will be applied
	 */
	private void parseParameters(Node node, IParameterized<StatSet> npcTemplate)
	{
		final Map<String, Object> parameters = new HashMap<>();
		for (Node parameterNode = node.getFirstChild(); parameterNode != null; parameterNode = parameterNode.getNextSibling())
		{
			NamedNodeMap attributes = parameterNode.getAttributes();
			switch (parameterNode.getNodeName().toLowerCase())
			{
				case "param":
				{
					parameters.put(parseString(attributes, "name"), parseString(attributes, "value"));
					break;
				}
				case "skill":
				{
					parameters.put(parseString(attributes, "name"), new SkillHolder(parseInteger(attributes, "id"), parseInteger(attributes, "level")));
					break;
				}
				case "location":
				{
					parameters.put(parseString(attributes, "name"), new Location(parseInteger(attributes, "x"), parseInteger(attributes, "y"), parseInteger(attributes, "z"), parseInteger(attributes, "heading", 0)));
					break;
				}
				case "minions":
				{
					final List<MinionHolder> minions = new ArrayList<>(1);
					for (Node minionNode = parameterNode.getFirstChild(); minionNode != null; minionNode = minionNode.getNextSibling())
					{
						if (minionNode.getNodeName().equalsIgnoreCase("npc"))
						{
							attributes = minionNode.getAttributes();
							minions.add(new MinionHolder(parseInteger(attributes, "id"), parseInteger(attributes, "count"), parseInteger(attributes, "max", 0), parseInteger(attributes, "respawnTime"), parseInteger(attributes, "weightPoint", 0)));
						}
					}
					
					if (!minions.isEmpty())
					{
						parameters.put(parseString(parameterNode.getAttributes(), "name"), minions);
					}
					break;
				}
			}
		}
		
		// Set the parameters on npcTemplate, or use EMPTY_STATSET if no parameters were parsed.
		npcTemplate.setParameters(!parameters.isEmpty() ? new StatSet(Collections.unmodifiableMap(parameters)) : StatSet.EMPTY_STATSET);
	}
	
	/**
	 * Parses minion data from the XML and adds it to the specified NPC spawn template.
	 * <p>
	 * This method iterates over each "minion" node within the "minions" XML section, creating a {@link MinionHolder} for each and adding it to the {@link NpcSpawnTemplate}.
	 * </p>
	 * @param n the {@link Node} positioned at the "minions" node
	 * @param npcTemplate the {@link NpcSpawnTemplate} to which the parsed minions will be added
	 */
	private void parseMinions(Node n, NpcSpawnTemplate npcTemplate)
	{
		forEach(n, "minion", minionNode -> npcTemplate.addMinion(new MinionHolder(new StatSet(parseAttributes(minionNode)))));
	}
	
	/**
	 * Parses location data from the XML and adds it as a spawn location to the specified NPC template.
	 * <p>
	 * This method iterates over each "location" node, extracting coordinates, heading, and spawn chance, and then adds each as a {@link ChanceLocation} to the {@link NpcSpawnTemplate}.
	 * </p>
	 * @param n the {@link Node} positioned at the "locations" node
	 * @param npcTemplate the {@link NpcSpawnTemplate} to which the parsed locations will be added
	 */
	private void parseLocations(Node n, NpcSpawnTemplate npcTemplate)
	{
		for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
		{
			if ("location".equalsIgnoreCase(d.getNodeName()))
			{
				final int x = parseInteger(d.getAttributes(), "x");
				final int y = parseInteger(d.getAttributes(), "y");
				final int z = parseInteger(d.getAttributes(), "z");
				final int heading = parseInteger(d.getAttributes(), "heading", 0);
				final double chance = parseDouble(d.getAttributes(), "chance");
				npcTemplate.addSpawnLocation(new ChanceLocation(x, y, z, heading, chance));
			}
		}
	}
	
	/**
	 * Initializes all spawn templates by spawning the NPCs defined in each template.
	 * <p>
	 * If configured, this method uses a thread pool to load spawns in parallel. Otherwise, it loads spawns sequentially. The process initializes the spawns by calling {@link SpawnTemplate#spawnAll} and notifies activation for each template if they are configured to spawn by default.
	 * </p>
	 */
	public void init()
	{
		if (DevelopmentConfig.NO_SPAWNS)
		{
			return;
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Initializing spawns...");
		if (ThreadConfig.THREADS_FOR_LOADING)
		{
			final Collection<ScheduledFuture<?>> jobs = ConcurrentHashMap.newKeySet();
			for (SpawnTemplate template : _spawnTemplates)
			{
				if (template.isSpawningByDefault())
				{
					jobs.add(ThreadPool.schedule(() ->
					{
						template.spawnAll(null);
						template.notifyActivate();
					}, 0));
				}
			}
			while (!jobs.isEmpty())
			{
				for (ScheduledFuture<?> job : jobs)
				{
					if ((job == null) || job.isDone() || job.isCancelled())
					{
						jobs.remove(job);
					}
				}
			}
		}
		else
		{
			for (SpawnTemplate template : _spawnTemplates)
			{
				if (template.isSpawningByDefault())
				{
					template.spawnAll(null);
					template.notifyActivate();
				}
			}
		}
		
		LOGGER.info(getClass().getSimpleName() + ": All spawns has been initialized!");
	}
	
	/**
	 * Despawns all active NPCs from the spawn templates, effectively removing all spawns.
	 */
	public void despawnAll()
	{
		LOGGER.info(getClass().getSimpleName() + ": Removing all spawns...");
		_spawnTemplates.forEach(SpawnTemplate::despawnAll);
		LOGGER.info(getClass().getSimpleName() + ": All spawns has been removed!");
	}
	
	/**
	 * Retrieves the collection of all spawn templates.
	 * @return a {@link Collection} of {@link SpawnTemplate} objects representing all available spawns
	 */
	public Collection<SpawnTemplate> getSpawns()
	{
		return _spawnTemplates;
	}
	
	/**
	 * Retrieves a list of spawn templates that match the specified condition.
	 * @param condition a {@link Predicate} to test each {@link SpawnTemplate}
	 * @return a {@link List} of {@link SpawnTemplate} objects that satisfy the condition
	 */
	public List<SpawnTemplate> getSpawns(Predicate<SpawnTemplate> condition)
	{
		final List<SpawnTemplate> result = new ArrayList<>();
		for (SpawnTemplate spawnTemplate : _spawnTemplates)
		{
			if (condition.test(spawnTemplate))
			{
				result.add(spawnTemplate);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves a spawn template by its name.
	 * @param name the name of the spawn template to retrieve
	 * @return the {@link SpawnTemplate} with the specified name, or {@code null} if not found
	 */
	public SpawnTemplate getSpawnByName(String name)
	{
		for (SpawnTemplate spawn : _spawnTemplates)
		{
			if ((spawn.getName() != null) && spawn.getName().equalsIgnoreCase(name))
			{
				return spawn;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves a spawn group by its name from all available spawn templates.
	 * @param name the name of the spawn group to retrieve
	 * @return the {@link SpawnGroup} with the specified name, or {@code null} if not found
	 */
	public SpawnGroup getSpawnGroupByName(String name)
	{
		for (SpawnTemplate spawnTemplate : _spawnTemplates)
		{
			for (SpawnGroup group : spawnTemplate.getGroups())
			{
				if ((group.getName() != null) && group.getName().equalsIgnoreCase(name))
				{
					return group;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves a list of NPC spawn templates that match the specified condition.
	 * @param condition a {@link Predicate} to test each {@link NpcSpawnTemplate}
	 * @return a {@link List} of {@link NpcSpawnTemplate} objects that satisfy the condition
	 */
	public List<NpcSpawnTemplate> getNpcSpawns(Predicate<NpcSpawnTemplate> condition)
	{
		final List<NpcSpawnTemplate> result = new ArrayList<>();
		for (SpawnTemplate template : _spawnTemplates)
		{
			for (SpawnGroup group : template.getGroups())
			{
				for (NpcSpawnTemplate spawn : group.getSpawns())
				{
					if (condition.test(spawn))
					{
						result.add(spawn);
					}
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Adds a new spawn to the {@link SpawnTable} and writes its details to an XML file.<br>
	 * If the XML file for the corresponding coordinates already exists, the spawn is appended to it.<br>
	 * If the file does not exist, a new one is created.
	 * @param spawn the {@link Spawn} object to add.
	 */
	public synchronized void addNewSpawn(Spawn spawn)
	{
		SpawnTable.getInstance().addSpawn(spawn);
		
		// Create output directory if it doesn't exist.
		final File outputDirectory = new File(OTHER_XML_FOLDER);
		if (!outputDirectory.exists())
		{
			boolean result = false;
			try
			{
				outputDirectory.mkdir();
				result = true;
			}
			catch (SecurityException se)
			{
				// Ignore.
			}
			
			if (result)
			{
				LOGGER.info(getClass().getSimpleName() + ": Created directory: " + OTHER_XML_FOLDER);
			}
		}
		
		// XML file for spawn.
		final int x = ((spawn.getX() - World.WORLD_X_MIN) >> 15) + World.TILE_X_MIN;
		final int y = ((spawn.getY() - World.WORLD_Y_MIN) >> 15) + World.TILE_Y_MIN;
		final File spawnFile = new File(OTHER_XML_FOLDER + "/" + x + "_" + y + ".xml");
		
		// Write info to XML.
		final String spawnId = String.valueOf(spawn.getId());
		final String spawnCount = String.valueOf(spawn.getAmount());
		final String spawnX = String.valueOf(spawn.getX());
		final String spawnY = String.valueOf(spawn.getY());
		final String spawnZ = String.valueOf(spawn.getZ());
		final String spawnHeading = String.valueOf(spawn.getHeading());
		final String spawnDelay = String.valueOf(spawn.getRespawnDelay() / 1000);
		if (spawnFile.exists()) // Update.
		{
			final File tempFile = new File(spawnFile.getAbsolutePath().substring(ServerConfig.DATAPACK_ROOT.getAbsolutePath().length() + 1).replace('\\', '/') + ".tmp");
			try
			{
				final BufferedReader reader = new BufferedReader(new FileReader(spawnFile));
				final BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
				String currentLine;
				while ((currentLine = reader.readLine()) != null)
				{
					if (currentLine.contains("</group>"))
					{
						final NpcTemplate template = NpcData.getInstance().getTemplate(spawn.getId());
						final String title = template.getTitle();
						final String name = title.isEmpty() ? template.getName() : template.getName() + " - " + title;
						writer.write("			<npc id=\"" + spawnId + (spawn.getAmount() > 1 ? "\" count=\"" + spawnCount : "") + "\" x=\"" + spawnX + "\" y=\"" + spawnY + "\" z=\"" + spawnZ + (spawn.getHeading() > 0 ? "\" heading=\"" + spawnHeading : "") + "\" respawnTime=\"" + spawnDelay + "sec\" /> <!-- " + name + " -->" + System.lineSeparator());
						writer.write(currentLine + System.lineSeparator());
						continue;
					}
					
					writer.write(currentLine + System.lineSeparator());
				}
				
				writer.close();
				reader.close();
				spawnFile.delete();
				tempFile.renameTo(spawnFile);
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Could not store spawn in the spawn XML files: " + e);
			}
		}
		else // New file.
		{
			try
			{
				final NpcTemplate template = NpcData.getInstance().getTemplate(spawn.getId());
				final String title = template.getTitle();
				final String name = title.isEmpty() ? template.getName() : template.getName() + " - " + title;
				final BufferedWriter writer = new BufferedWriter(new FileWriter(spawnFile));
				writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.lineSeparator());
				writer.write("<list xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"../../xsd/spawns.xsd\">" + System.lineSeparator());
				writer.write("	<spawn name=\"" + x + "_" + y + "\">" + System.lineSeparator());
				writer.write("		<group>" + System.lineSeparator());
				writer.write("			<npc id=\"" + spawnId + (spawn.getAmount() > 1 ? "\" count=\"" + spawnCount : "") + "\" x=\"" + spawnX + "\" y=\"" + spawnY + "\" z=\"" + spawnZ + (spawn.getHeading() > 0 ? "\" heading=\"" + spawnHeading : "") + "\" respawnTime=\"" + spawnDelay + "sec\" /> <!-- " + name + " -->" + System.lineSeparator());
				writer.write("		</group>" + System.lineSeparator());
				writer.write("	</spawn>" + System.lineSeparator());
				writer.write("</list>" + System.lineSeparator());
				writer.close();
				LOGGER.info(getClass().getSimpleName() + ": Created file: " + OTHER_XML_FOLDER + "/" + x + "_" + y + ".xml");
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Spawn " + spawn + " could not be added to the spawn XML files: " + e);
			}
		}
	}
	
	/**
	 * Deletes a spawn from the {@link SpawnTable} and its associated XML entry if it exists.<br>
	 * If the file becomes empty after deletion, it is removed.
	 * @param spawn the {@link Spawn} object to delete.
	 */
	public synchronized void deleteSpawn(Spawn spawn)
	{
		SpawnTable.getInstance().removeSpawn(spawn);
		
		final int x = ((spawn.getX() - World.WORLD_X_MIN) >> 15) + World.TILE_X_MIN;
		final int y = ((spawn.getY() - World.WORLD_Y_MIN) >> 15) + World.TILE_Y_MIN;
		final NpcSpawnTemplate npcSpawnTemplate = spawn.getNpcSpawnTemplate();
		final File spawnFile = npcSpawnTemplate != null ? npcSpawnTemplate.getSpawnTemplate().getFile() : new File(OTHER_XML_FOLDER + "/" + x + "_" + y + ".xml");
		final File tempFile = new File(spawnFile.getAbsolutePath().substring(ServerConfig.DATAPACK_ROOT.getAbsolutePath().length() + 1).replace('\\', '/') + ".tmp");
		try
		{
			final BufferedReader reader = new BufferedReader(new FileReader(spawnFile));
			final BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
			
			boolean found = false; // In XML you can have more than one spawn with same coords.
			boolean isMultiLine = false; // In case spawn has more stats.
			boolean lastLineFound = false; // Used to check for empty file.
			int lineCount = 0;
			String currentLine;
			
			final SpawnGroup group = npcSpawnTemplate != null ? npcSpawnTemplate.getGroup() : null;
			List<SpawnTerritory> territories = group != null ? group.getTerritories() : Collections.emptyList();
			boolean simpleTerritory = false;
			if (territories.isEmpty())
			{
				final SpawnTemplate spawnTemplate = npcSpawnTemplate != null ? npcSpawnTemplate.getSpawnTemplate() : null;
				if (spawnTemplate != null)
				{
					territories = spawnTemplate.getTerritories();
					simpleTerritory = true;
				}
			}
			
			if (territories.isEmpty())
			{
				final String spawnId = String.valueOf(spawn.getId());
				final String spawnX = String.valueOf(npcSpawnTemplate != null ? npcSpawnTemplate.getSpawnLocation().getX() : spawn.getX());
				final String spawnY = String.valueOf(npcSpawnTemplate != null ? npcSpawnTemplate.getSpawnLocation().getY() : spawn.getY());
				final String spawnZ = String.valueOf(npcSpawnTemplate != null ? npcSpawnTemplate.getSpawnLocation().getZ() : spawn.getZ());
				
				while ((currentLine = reader.readLine()) != null)
				{
					if (!found)
					{
						if (isMultiLine)
						{
							if (currentLine.contains("</npc>"))
							{
								found = true;
							}
							continue;
						}
						
						if (currentLine.contains(spawnId) && currentLine.contains(spawnX) && currentLine.contains(spawnY) && currentLine.contains(spawnZ))
						{
							if (!currentLine.contains("/>") && !currentLine.contains("</npc>"))
							{
								isMultiLine = true;
							}
							else
							{
								found = true;
							}
							continue;
						}
					}
					
					writer.write(currentLine + System.lineSeparator());
					if (currentLine.contains("</list>"))
					{
						lastLineFound = true;
					}
					
					if (!lastLineFound)
					{
						lineCount++;
					}
				}
			}
			else
			{
				SEARCH: while ((currentLine = reader.readLine()) != null)
				{
					if (!found)
					{
						if (isMultiLine)
						{
							if (currentLine.contains("</group>") || (simpleTerritory && currentLine.contains("<territories>")))
							{
								found = true;
							}
							continue;
						}
						
						for (SpawnTerritory territory : territories)
						{
							if (currentLine.contains('"' + territory.getName() + '"'))
							{
								isMultiLine = true;
								continue SEARCH;
							}
						}
					}
					
					writer.write(currentLine + System.lineSeparator());
					if (currentLine.contains("</list>"))
					{
						lastLineFound = true;
					}
					
					if (!lastLineFound)
					{
						lineCount++;
					}
				}
			}
			
			writer.close();
			reader.close();
			spawnFile.delete();
			tempFile.renameTo(spawnFile);
			
			// Delete empty file.
			if (lineCount < 8)
			{
				LOGGER.info(getClass().getSimpleName() + ": Deleted empty file: " + spawnFile.getAbsolutePath().substring(ServerConfig.DATAPACK_ROOT.getAbsolutePath().length() + 1).replace('\\', '/'));
				spawnFile.delete();
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Spawn " + spawn + " could not be removed from the spawn XML files: " + e);
		}
	}
	
	public static SpawnData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SpawnData INSTANCE = new SpawnData();
	}
}
