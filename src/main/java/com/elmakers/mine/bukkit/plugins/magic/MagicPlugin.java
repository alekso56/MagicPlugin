package com.elmakers.mine.bukkit.plugins.magic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.elmakers.mine.bukkit.api.magic.Automaton;
import com.elmakers.mine.bukkit.api.magic.LostWand;
import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import com.elmakers.mine.bukkit.block.BlockData;
import com.elmakers.mine.bukkit.block.MaterialBrush;
import com.elmakers.mine.bukkit.plugins.magic.commands.MagicCommandExecutor;
import com.elmakers.mine.bukkit.plugins.magic.wand.Wand;
import com.elmakers.mine.bukkit.utilities.InventoryUtils;
import com.elmakers.mine.bukkit.utilities.Messages;
import com.elmakers.mine.bukkit.utilities.URLMap;
import com.elmakers.mine.bukkit.utilities.borrowed.ConfigurationNode;

public class MagicPlugin extends JavaPlugin implements MagicAPI
{	
	/*
	 * Public API
	 */
	public MagicController getController()
	{
		return controller;
	}

	/*
	 * Plugin interface
	 */

	public void onEnable() 
	{
		if (controller == null) {
			controller = new MagicController(this);
		}
		initialize();

		BlockData.setServer(getServer());
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(controller, this);
		
		getCommand("magic").setExecutor(new MagicCommandExecutor(this));
	}

	protected void initialize()
	{
		controller.initialize();
	}

	@SuppressWarnings("deprecation")
	protected void handleWandCommandTab(List<String> options, Mage player, CommandSender sender, Command cmd, String alias, String[] args)
	{
		if (args.length == 0) {
			return;
		}
		if (args.length == 1) {
			addIfPermissible(sender, options, "Magic.commands." + cmd + ".", "add");
			addIfPermissible(sender, options, "Magic.commands." + cmd + ".", "remove");
			addIfPermissible(sender, options, "Magic.commands." + cmd + ".", "name");
			addIfPermissible(sender, options, "Magic.commands." + cmd + ".", "fill");
			addIfPermissible(sender, options, "Magic.commands." + cmd + ".", "configure");
			addIfPermissible(sender, options, "Magic.commands." + cmd + ".", "organize");
			addIfPermissible(sender, options, "Magic.commands." + cmd + ".", "combine");
			addIfPermissible(sender, options, "Magic.commands." + cmd + ".", "upgrade");
			addIfPermissible(sender, options, "Magic.commands." + cmd + ".", "describe");
			Collection<String> allWands = Wand.getWandKeys();
			for (String wandKey : allWands) {
				addIfPermissible(sender, options, "Magic.commands." + cmd.getName() + ".wand.", wandKey, true);
			}
			return;
		}
		
		if (args.length == 2) {
			String subCommand = args[0];
			String subCommandPNode = "Magic.commands." + cmd.getName() + "." + subCommand;
			
			if (!controller.hasPermission(sender, subCommandPNode)) {
				return;
			}
			
			subCommandPNode += ".";
			
			if (subCommand.equalsIgnoreCase("add")) {
				List<Spell> spellList = controller.getAllSpells();
				for (Spell spell : spellList) {
					addIfPermissible(sender, options, subCommandPNode, spell.getKey(), true);
				}
				addIfPermissible(sender, options, subCommandPNode, "material", true);
			}
			
			if (subCommand.equalsIgnoreCase("configure")) {
				for (String key : Wand.PROPERTY_KEYS) {
					options.add(key);
				}
			}
			
			if (subCommand.equalsIgnoreCase("remove")) {
				Wand activeWand = player == null ? null : player.getActiveWand();
				if (activeWand != null) {
					Collection<String> spellNames = activeWand.getSpells();
					for (String spellName : spellNames) {
						options.add(spellName);
					}
					
					options.add("material");
				}
			}
			
			if (subCommand.equalsIgnoreCase("combine")) {
				Collection<String> allWands = Wand.getWandKeys();
				for (String wandKey : allWands) {
					addIfPermissible(sender, options, "Magic.commands." + cmd.getName() + ".combine.", wandKey, true);
				}
			}
		}
		
		if (args.length == 3)
		{
			String subCommand = args[0];
			String subCommand2 = args[1];
			
			String subCommandPNode = "Magic.commands." + cmd.getName() + "." + subCommand + "." + subCommand2;
			
			if (!controller.hasPermission(sender, subCommandPNode, true)) {
				return;
			}
			
			if (subCommand.equalsIgnoreCase("remove") && subCommand2.equalsIgnoreCase("material")) {
				Wand activeWand = player == null ? null : player.getActiveWand();
				if (activeWand != null) {
					Collection<String> materialNames = activeWand.getMaterialKeys();
					for (String materialName : materialNames) {
						options.add(materialName);
					}
				}
			}
			
			if (subCommand.equalsIgnoreCase("add") && subCommand2.equalsIgnoreCase("material")) {
				Material[] materials = Material.values();
				for (Material material : materials) {
					// Kind of a hack..
					if (material.getId() < 256) {
						options.add(material.name().toLowerCase());
					}
				}
			}
		}
		
		// TODO : Custom completion for configure, upgrade
	}

	protected void handleCastCommandTab(List<String> options, CommandSender sender, Command cmd, String alias, String[] args)
	{
		if (args.length == 1) {
			List<Spell> spellList = controller.getAllSpells();
			for (Spell spell : spellList) {
				addIfPermissible(sender, options, "Magic." + cmd.getName() + ".", spell.getKey(), true);
			}
			
			return;
		}
		
		// TODO : Custom completion for spell parameters
	}
	
	protected void addIfPermissible(CommandSender sender, List<String> options, String permissionPrefix, String option, boolean defaultValue)
	{
		if (controller.hasPermission(sender, permissionPrefix + option, defaultValue))
		{
			options.add(option);
		}
	}
	
	protected void addIfPermissible(CommandSender sender, List<String> options, String permissionPrefix, String option)
	{
		addIfPermissible(sender, options, permissionPrefix, option, false);
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args)
	{
		Mage mage = null;
		if (sender instanceof Player) {
			mage = controller.getMage((Player)sender);
		}
		String completeCommand = args.length > 0 ? args[args.length - 1] : "";
		List<String> options = new ArrayList<String>();
		if (cmd.getName().equalsIgnoreCase("magic"))
		{
			if (args.length == 1) {
				addIfPermissible(sender, options, "Magic.commands.magic.", "clean");
				addIfPermissible(sender, options, "Magic.commands.magic.", "clearcache");
				addIfPermissible(sender, options, "Magic.commands.magic.", "cancel");
				addIfPermissible(sender, options, "Magic.commands.magic.", "load");
				addIfPermissible(sender, options, "Magic.commands.magic.", "save");
				addIfPermissible(sender, options, "Magic.commands.magic.", "commit");
				addIfPermissible(sender, options, "Magic.commands.magic.", "give");
				addIfPermissible(sender, options, "Magic.commands.magic.", "list");
			} else if (args.length == 2) {
				if (args[1].equalsIgnoreCase("list")) {
					addIfPermissible(sender, options, "Magic.commands.magic.list", "maps");
					addIfPermissible(sender, options, "Magic.commands.magic.list", "wands");
					addIfPermissible(sender, options, "Magic.commands.magic.list", "automata");
				}
			}
		}
		else if (cmd.getName().equalsIgnoreCase("wand")) 
		{
			handleWandCommandTab(options, mage, sender, cmd, alias, args);
		}
		else if (cmd.getName().equalsIgnoreCase("wandp")) 
		{
			if (args.length == 1) {
				options.addAll(MagicController.getPlayerNames());
			} else if (args.length > 1) {
				String[] args2 = Arrays.copyOfRange(args, 1, args.length);
				handleWandCommandTab(options, mage, sender, cmd, alias, args2);
			}
		}
		else if (cmd.getName().equalsIgnoreCase("cast")) 
		{
			handleCastCommandTab(options, sender, cmd, alias, args);
		}
		else if (cmd.getName().equalsIgnoreCase("castp")) 
		{
			if (args.length == 1) {
				options.addAll(MagicController.getPlayerNames());
			} else if (args.length > 1) {
				String[] args2 = Arrays.copyOfRange(args, 1, args.length);
				handleCastCommandTab(options, sender, cmd, alias, args2);
			}
		}
		
		if (completeCommand.length() > 0) {
			completeCommand = completeCommand.toLowerCase();
			List<String> allOptions = options;
			options = new ArrayList<String>();
			for (String option : allOptions) {
				String lowercase = option.toLowerCase();
				if (lowercase.startsWith(completeCommand)) {
					options.add(option);
				}
			}
		}
		
		Collections.sort(options);
		
		return options;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
	{
		if (commandLabel.equalsIgnoreCase("wandp"))
		{
			if (args.length == 0) {
				sender.sendMessage("Usage: /wandp [player] [wand name/command]");
				return true;
			}
			Player player = Bukkit.getPlayer(args[0]);
			if (player == null) {
				sender.sendMessage("Can't find player " + args[0]);
				return true;
			}
			if (!player.isOnline()) {
				sender.sendMessage("Player " + args[0] + " is not online");
				return true;
			}
			String[] args2 = Arrays.copyOfRange(args, 1, args.length);
			return processWandCommand("wandp", sender, player, args2);
		}

		if (commandLabel.equalsIgnoreCase("castp"))
		{
			if (args.length == 0) {
				sender.sendMessage("Usage: /castp [player] [spell] <parameters>");
				return true;
			}
			Player player = Bukkit.getPlayer(args[0]);
			if (player == null) {
				sender.sendMessage("Can't find player " + args[0]);
				return true;
			}
			if (!player.isOnline()) {
				sender.sendMessage("Player " + args[0] + " is not online");
				return true;
			}
			String[] args2 = Arrays.copyOfRange(args, 1, args.length);
			return processCastCommand(sender, player, args2);
		}

		if (commandLabel.equalsIgnoreCase("cast"))
		{
			Player player = null;
			if (sender instanceof Player) {
				player = (Player)sender;
			}
			if (!controller.hasPermission(player, "Magic.commands.cast")) return false;
			return processCastCommand(sender, player, args);
		}

		if (!(sender instanceof Player)) {
			if (commandLabel.equalsIgnoreCase("spells"))
			{
				listSpells(sender, -1, args.length > 0 ? args[0] : null);
				return true;
			}
			if (commandLabel.equalsIgnoreCase("wand") && args.length > 0 && args[0].equalsIgnoreCase("list"))
			{
				onWandList(sender);
				return true;
			}
			
			return false;
		}

		// Everything beyond this point is is-game only
		Player player = (Player)sender;
		if (commandLabel.equalsIgnoreCase("wand"))
		{
			return processWandCommand("wand", sender, player, args);
		}

		if (commandLabel.equalsIgnoreCase("spells"))
		{
			if (!controller.hasPermission(player, "Magic.commands.spells")) return false;
			return onSpells(player, args);
		}

		return false;
	}
	
	protected boolean processWandCommand(String command, CommandSender sender, Player player, String[] args)
	{
		String subCommand = "";
		String[] args2 = args;

		if (args.length > 0) {
			subCommand = args[0];
			args2 = new String[args.length - 1];
			for (int i = 1; i < args.length; i++) {
				args2[i - 1] = args[i];
			}
		}
		if (subCommand.equalsIgnoreCase("list"))
		{
			if (!controller.hasPermission(sender, "Magic.commands." + command + "." + subCommand)) return true;

			onWandList(sender);
			return true;
		}
		if (subCommand.equalsIgnoreCase("add"))
		{
			if (!controller.hasPermission(sender, "Magic.commands." + command + "." + subCommand)) return true;
			if (args2.length > 0 && args2[0].equals("material") && !controller.hasPermission(sender,"Magic.commands.wand.add." + args2[0], true)) return true;
			if (args2.length > 0 && !controller.hasPermission(sender,"Magic.commands.wand.add.spell." + args2[0], true)) return true;
			onWandAdd(sender, player, args2);
			return true;
		}
		if (subCommand.equalsIgnoreCase("configure"))
		{
			if (!controller.hasPermission(sender, "Magic.commands." + command + "." + subCommand)) return true;

			onWandConfigure(sender, player, args2, false);
			return true;
		}
		if (subCommand.equalsIgnoreCase("enchant"))
		{
			if (!controller.hasPermission(sender, "Magic.commands." + command + "." + subCommand)) return true;

			onWandEnchant(sender, player);
			return true;
		}
		if (subCommand.equalsIgnoreCase("unenchant"))
		{
			if (!controller.hasPermission(sender, "Magic.commands." + command + "." + subCommand)) return true;

			onWandUnenchant(sender, player);
			return true;
		}
		if (subCommand.equalsIgnoreCase("duplicate"))
		{
			if (!controller.hasPermission(sender, "Magic.commands." + command + "." + subCommand)) return true;

			onWandDuplicate(sender, player);
			return true;
		}
		if (subCommand.equalsIgnoreCase("organize"))
		{
			if (!controller.hasPermission(sender, "Magic.commands." + command + "." + subCommand)) return true;

			onWandOrganize(sender, player);
			return true;
		}
		if (subCommand.equalsIgnoreCase("combine"))
		{
			if (!controller.hasPermission(sender, "Magic.commands." + command + "." + subCommand)) return true;
			if (args.length > 0 && !controller.hasPermission(sender,"Magic.commands." + command + ".combine." + args[0], true)) return true;
			
			onWandCombine(sender, player, args2);
			return true;
		}
		if (subCommand.equalsIgnoreCase("describe"))
		{
			if (!controller.hasPermission(sender, "Magic.commands." + command + "." + subCommand)) return true;

			onWandDescribe(sender, player);
			return true;
		}
		if (subCommand.equalsIgnoreCase("upgrade"))
		{
			if (!controller.hasPermission(sender, "Magic.commands." + command + "." + subCommand)) return true;

			onWandConfigure(sender, player, args2, true);
			return true;
		}
		if (subCommand.equalsIgnoreCase("organize"))
		{
			if (!controller.hasPermission(sender, "Magic.commands." + command + "." + subCommand)) return true;

			onWandOrganize(sender, player);
			return true;
		}
		if (subCommand.equalsIgnoreCase("fill"))
		{
			if (!controller.hasPermission(sender, "Magic.commands." + command + "." + subCommand)) return true;

			onWandFill(sender, player);
			return true;
		}
		if (subCommand.equalsIgnoreCase("remove"))
		{   
			if (!controller.hasPermission(sender, "Magic.commands." + command + "." + subCommand)) return true;

			onWandRemove(sender, player, args2);
			return true;
		}

		if (subCommand.equalsIgnoreCase("name"))
		{
			if (!controller.hasPermission(sender, "Magic.commands." + command + "." + subCommand)) return true;

			onWandName(sender, player, args2);
			return true;
		}

		if (subCommand.length() == 0) 
		{
			if (!controller.hasPermission(sender, "Magic.commands." + command)) return true;
			if (!controller.hasPermission(sender, "Magic.commands." + command + ".wand.default", true)) return true;
		} 
		else 
		{
			if (!controller.hasPermission(sender,"Magic.commands." + command +".wand." + subCommand, true)) return true;
		}
		
		return onWand(sender, player, args);
	}

	public boolean onWandList(CommandSender sender) {
		Collection<ConfigurationNode> templates = Wand.getWandTemplates();
		Map<String, ConfigurationNode> nameMap = new TreeMap<String, ConfigurationNode>();
		for (ConfigurationNode templateConfig : templates)
		{
			nameMap.put(templateConfig.getString("key"), templateConfig);
		}
		for (ConfigurationNode templateConfig : nameMap.values())
		{
			if (templateConfig.getBoolean("hidden", false)) continue;
			
			String key = templateConfig.getString("key");
			String name = Messages.get("wands." + key + ".name", Messages.get("wand.default_name"));
			String description = Messages.get("wands." + key + ".description", "");
			description = ChatColor.YELLOW + description; 
			if (!name.equals(key)) {
				description = ChatColor.BLUE + name + ChatColor.WHITE + " : " + description;
			}
			sender.sendMessage(ChatColor.AQUA + key + ChatColor.WHITE + " : " + description);
		}

		return true;
	}

	public boolean onWandDescribe(CommandSender sender, Player player) {
		if (!checkWand(sender, player, true, true)) {
			return true;
		}
		Mage mage = controller.getMage(player);
		Wand wand = mage.getActiveWand();
		wand.describe(sender);

		return true;
	}
	
	public boolean onWandOrganize(CommandSender sender, Player player)
	{
		// Allow reorganizing modifiable wands
		if (!checkWand(sender, player, true)) {
			return true;
		}
		Mage mage = controller.getMage(player);
		Wand wand = mage.getActiveWand();
		wand.deactivate();
		wand.organizeInventory(mage);
		wand.activate(mage);
		mage.sendMessage(Messages.get("wand.reorganized"));
		if (sender != player) {
			sender.sendMessage(Messages.getParameterized("wand.player_reorganized", "$name", player.getName()));
		}
		
		return true;
	}
	
	public boolean onWandEnchant(CommandSender sender, Player player)
	{
		Mage mage = controller.getMage(player);
		ItemStack heldItem = player.getItemInHand();
		if (heldItem == null || heldItem.getType() == Material.AIR)
		{
			mage.sendMessage(Messages.get("wand.no_item"));
			if (sender != player) {
				sender.sendMessage(Messages.getParameterized("wand.player_no_item", "$name", player.getName()));
			}
			return false;
		}
		
		Wand wand = new Wand(controller, heldItem.getType(), heldItem.getDurability());
		player.setItemInHand(wand.getItem());
		wand.activate(mage);
		
		mage.sendMessage(Messages.getParameterized("wand.enchanted", "$item", MaterialBrush.getMaterialName(heldItem.getType(), (byte)heldItem.getDurability())));
				
		if (sender != player) {
			sender.sendMessage(Messages.getParameterized("wand.player_enchanted", 
					"$item", MaterialBrush.getMaterialName(heldItem.getType(), (byte)heldItem.getDurability()),
					"$name", player.getName()
			));
		}
		
		return true;
	}
	
	public boolean onWandUnenchant(CommandSender sender, Player player)
	{
		if (!checkWand(sender, player)) {
			return true;
		}
		Mage mage = controller.getMage(player);
		Wand wand = mage.getActiveWand();
		
		// Trying to make sure the player is actually holding the active wand
		// Just in case. This isn't fool-proof though, if they have more than one wand.
		if (wand == null || !Wand.isWand(player.getItemInHand())) {
			mage.sendMessage(Messages.get("wand.no_wand"));
			if (sender != player) {
				sender.sendMessage(Messages.getParameterized("wand.player_no_wand", "$name", player.getName()));
			}
			return false;
		}

		wand.unenchant();
		player.setItemInHand(wand.getItem());
		mage.setActiveWand(null);
		
		mage.sendMessage(Messages.get("wand.unenchanted"));
		if (sender != player) {
			sender.sendMessage(Messages.getParameterized("wand.player_unenchanted", "$name", player.getName()));
		}
		return true;
	}

	public boolean onWandDuplicate(CommandSender sender, Player player)
	{
		if (!checkWand(sender, player, false, false)) {
			return true;
		}
		Mage mage = controller.getMage(player);
		Wand wand = mage.getActiveWand();

		ItemStack newItem = InventoryUtils.getCopy(wand.getItem());
		Wand newWand = new Wand(controller, newItem);
		newWand.generateId();
		giveItemToPlayer(player, newWand.getItem());
		
		mage.sendMessage(Messages.get("wand.duplicated"));
		if (sender != player) {
			sender.sendMessage(Messages.getParameterized("wand.player_duplicated", "$name", player.getName()));
		}
		return true;
	}
	
	public boolean onWandConfigure(CommandSender sender, Player player, String[] parameters, boolean safe)
	{
		if (parameters.length < 1) {
			sender.sendMessage("Use: /wand configure <property> <value>");
			sender.sendMessage("Properties: " + StringUtils.join(Wand.PROPERTY_KEYS, ", "));
			return false;
		}
		
		// TODO: A way to make wands modifiable again... ?
		// Probably need to handle that separately, or maybe lock/unlock commands?
		if (!checkWand(sender, player)) {
			return true;
		}

		Mage mage = controller.getMage(player);
		Wand wand = mage.getActiveWand();
		ConfigurationNode node = new ConfigurationNode();
		String value = "";
		for (int i = 1; i < parameters.length; i++) {
			if (i != 1) value = value + " ";
			value = value + parameters[i];
		}
		node.setProperty(parameters[0], value);
		wand.deactivate();
		wand.loadProperties(node, safe);
		wand.activate(mage);
		mage.sendMessage(Messages.get("wand.reconfigured"));
		if (sender != player) {
			sender.sendMessage(Messages.getParameterized("wand.player_reconfigured", "$name", player.getName()));
		}
		return true;
	}
	
	protected boolean checkWand(CommandSender sender, Player player)
	{
		return checkWand(sender, player, false, false);
	}
	
	protected boolean checkWand(CommandSender sender, Player player, boolean skipModifiable)
	{
		return checkWand(sender, player, skipModifiable, false);
	}
	
	protected boolean checkWand(CommandSender sender, Player player, boolean skipModifiable, boolean skipBound)
	{
		Mage mage = controller.getMage(player);
		Wand wand = mage.getActiveWand();
		
		if (wand == null) {
			mage.sendMessage(Messages.get("wand.no_wand"));
			if (sender != player) {
				sender.sendMessage(Messages.getParameterized("wand.player_no_wand", "$name", player.getName()));
			}
			return false;
		}
		if (!skipModifiable && !wand.isModifiable()) {
			mage.sendMessage(Messages.get("wand.unmodifiable"));
			if (sender != player) {
				sender.sendMessage(Messages.getParameterized("wand.player_unmodifiable", "$name", player.getName()));
			}
			return false;
		}
		if (!skipBound && !wand.canUse(mage.getPlayer()) ) {
			mage.sendMessage(Messages.get("wand.bound_to_other"));
			if (sender != player) {
				sender.sendMessage(Messages.getParameterized("wand.player_unmodifiable", "$name", player.getName()));
			}
			return false;
		}
		
		return true;
	}

	public boolean onWandCombine(CommandSender sender, Player player, String[] parameters)
	{
		if (parameters.length < 1) {
			sender.sendMessage("Use: /wand combine <wandname>");
			return false;
		}
		
		if (!checkWand(sender, player)) {
			return true;
		}

		Mage mage = controller.getMage(player);
		Wand wand = mage.getActiveWand();
		
		String wandName = parameters[0];
		Wand newWand = Wand.createWand(controller, wandName);
		if (newWand == null) {
			sender.sendMessage(Messages.getParameterized("wand.unknown_template", "$name", wandName));
			return false;
		}
		wand.deactivate();
		wand.add(newWand);
		wand.activate(mage);
		
		mage.sendMessage(Messages.get("wand.upgraded"));
		if (sender != player) {
			sender.sendMessage(Messages.getParameterized("wand.player_upgraded", "$name", player.getName()));
		}
		return true;
	}

	public boolean onWandFill(CommandSender sender, Player player)
	{
		if (!checkWand(sender, player)) {
			return true;
		}
		
		Mage mage = controller.getMage(player);
		Wand wand = mage.getActiveWand();
		
		wand.fill(player);
		mage.sendMessage(Messages.get("wand.filled"));
		if (sender != player) {
			sender.sendMessage(Messages.getParameterized("wand.player_filled", "$name", player.getName()));
		}
		
		return true;
	}
	
	public boolean onWandAdd(CommandSender sender, Player player, String[] parameters)
	{
		if (parameters.length < 1) {
			sender.sendMessage("Use: /wand add <spell|material> [material:data]");
			return true;
		}
		
		if (!checkWand(sender, player)) {
			return true;
		}

		Mage mage = controller.getMage(player);
		Wand wand = mage.getActiveWand();

		String spellName = parameters[0];
		if (spellName.equals("material")) {
			if (parameters.length < 2) {
				sender.sendMessage("Use: /wand add material <material:data>");
				return true;
			}
			
			String materialKey = parameters[1];
			if (!MaterialBrush.isValidMaterial(materialKey, false)) {
				sender.sendMessage(materialKey + " is not a valid material");
				return true;
			}
			
			if (wand.addMaterial(materialKey, true, false)) {
				mage.sendMessage("Material '" + materialKey + "' has been added to your wand");
				if (sender != player) {
					sender.sendMessage("Added material '" + materialKey + "' to " + player.getName() + "'s wand");
				}
			} else {
				mage.sendMessage("Material activated: " + materialKey);
				if (sender != player) {
					sender.sendMessage(player.getName() + "'s wand already has material " + materialKey);
				}
			}
			return true;
		}
		Spell spell = mage.getSpell(spellName);
		if (spell == null)
		{
			sender.sendMessage("Spell '" + spellName + "' unknown, Use /spells for spell list");
			return true;
		}

		if (wand.addSpell(spellName, true)) {
			mage.sendMessage("Spell '" + spell.getName() + "' has been added to your wand");
			if (sender != player) {
				sender.sendMessage("Added '" + spell.getName() + "' to " + player.getName() + "'s wand");
			}
		} else {
			mage.sendMessage(spell.getName() + " activated");
			if (sender != player) {
				sender.sendMessage(player.getName() + "'s wand already has " + spell.getName());
			}
		}

		return true;
	}

	public boolean onWandRemove(CommandSender sender, Player player, String[] parameters)
	{
		if (parameters.length < 1) {
			sender.sendMessage("Use: /wand remove <spell|material> [material:data]");
			return true;
		}

		if (!checkWand(sender, player)) {
			return true;
		}

		Mage mage = controller.getMage(player);
		Wand wand = mage.getActiveWand();

		String spellName = parameters[0];	
		if (spellName.equals("material")) {
			if (parameters.length < 2) {
				sender.sendMessage("Use: /wand remove material <material:data>");
				return true;
			}
			String materialKey = parameters[1];
			if (wand.removeMaterial(materialKey)) {
				mage.sendMessage("Material '" + materialKey + "' has been removed from your wand");
				if (sender != player) {
					sender.sendMessage("Removed material '" + materialKey + "' from " + player.getName() + "'s wand");
				}
			} else {
				if (sender != player) {
					sender.sendMessage(player.getName() + "'s wand does not have material " + materialKey);
				}
			}
			return true;
		}
		if (wand.removeSpell(spellName)) {
			mage.sendMessage("Spell '" + spellName + "' has been removed from your wand");
			if (sender != player) {
				sender.sendMessage("Removed '" + spellName + "' from " + player.getName() + "'s wand");
			}
		} else {
			if (sender != player) {
				sender.sendMessage(player.getName() + "'s wand does not have " + spellName);
			}
		}

		return true;
	}

	public boolean onWandName(CommandSender sender, Player player, String[] parameters)
	{
		if (parameters.length < 1) {
			sender.sendMessage("Use: /wand name <name>");
			return true;
		}

		if (!checkWand(sender, player)) {
			return true;
		}
		
		Mage mage = controller.getMage(player);
		Wand wand = mage.getActiveWand();
		
		wand.setName(StringUtils.join(parameters, " "));
		mage.sendMessage(Messages.get("wand.renamed"));
		if (sender != player) {
			sender.sendMessage(Messages.getParameterized("wand.player_renamed", "$name", player.getName()));
		}

		return true;
	}

	public boolean onGiveWand(CommandSender sender, Player player, String wandKey)
	{
		Mage mage = controller.getMage(player);
		Wand currentWand =  mage.getActiveWand();
		if (currentWand != null) {
			currentWand.closeInventory();
		}
	
		Wand wand = Wand.createWand(controller, wandKey);
		if (wand != null) {
			giveItemToPlayer(player, wand.getItem());
			if (sender != player) {
				sender.sendMessage("Gave wand " + wand.getName() + " to " + player.getName());
			}
		} else {
			sender.sendMessage(Messages.getParameterized("wand.unknown_template", "$name", wandKey));
		}
		return true;
	}
	
	public boolean onWand(CommandSender sender, Player player, String[] parameters)
	{
		String wandName = null;
		if (parameters.length > 0)
		{
			wandName = parameters[0];
		}
		
		return onGiveWand(sender, player, wandName);
	}
	
	public boolean processCastCommand(CommandSender sender, Player player, String[] castParameters)
	{
		if (castParameters.length < 1) return false;

		String spellName = castParameters[0];
		String[] parameters = new String[castParameters.length - 1];
		for (int i = 1; i < castParameters.length; i++)
		{
			parameters[i - 1] = castParameters[i];
		}
		return controller.cast(null, spellName, parameters, sender, player);
	}
	
	public boolean onSpells(Player player, String[] parameters)
	{
		int pageNumber = 1;
		String category = null;
		if (parameters.length > 0)
		{
			try
			{
				pageNumber = Integer.parseInt(parameters[0]);
			}
			catch (NumberFormatException ex)
			{
				pageNumber = 1;
				category = parameters[0];
			}
		}
		listSpells(player, pageNumber, category);

		return true;
	}


	/* 
	 * Help commands
	 */

	public void listSpellsByCategory(CommandSender sender, String category)
	{
		List<Spell> categorySpells = new ArrayList<Spell>();
		List<Spell> spellVariants = controller.getAllSpells();
		Player player = sender instanceof Player ? (Player)sender : null;
		for (Spell spell : spellVariants)
		{
			String spellCategory = spell.getCategory();
			if (spellCategory != null && spellCategory.equalsIgnoreCase(category) 
				&& (player == null || spell.hasSpellPermission(player)))
			{
				categorySpells.add(spell);
			}
		}

		if (categorySpells.size() == 0)
		{
			String message = Messages.get("general.no_spells_in_category");
			message = message.replace("$category", category);
			sender.sendMessage(message);
			return;
		}
		sender.sendMessage(category + ":");
		Collections.sort(categorySpells);
		for (Spell spell : categorySpells)
		{
			String name = spell.getName();
			String description = spell.getDescription();
			if (!name.equals(spell.getKey())) {
				description = name + " : " + description;
			}
			sender.sendMessage(ChatColor.AQUA + spell.getKey() + ChatColor.BLUE + " [" + spell.getIcon().getMaterial().name().toLowerCase() + "] : " + ChatColor.YELLOW + description);
		}
	}

	public void listCategories(Player player)
	{
		HashMap<String, Integer> spellCounts = new HashMap<String, Integer>();
		List<String> spellGroups = new ArrayList<String>();
		List<Spell> spellVariants = controller.getAllSpells();

		for (Spell spell : spellVariants)
		{
			if (player != null && !spell.hasSpellPermission(player)) continue;
			if (spell.getCategory() == null) continue;
			
			Integer spellCount = spellCounts.get(spell.getCategory());
			if (spellCount == null || spellCount == 0)
			{
				spellCounts.put(spell.getCategory(), 1);
				spellGroups.add(spell.getCategory());
			}
			else
			{
				spellCounts.put(spell.getCategory(), spellCount + 1);
			}
		}
		if (spellGroups.size() == 0)
		{
			player.sendMessage(Messages.get("general.no_spells"));
			return;
		}

		Collections.sort(spellGroups);
		for (String group : spellGroups)
		{
			player.sendMessage(group + " [" + spellCounts.get(group) + "]");
		}
	}

	public void listSpells(CommandSender sender, int pageNumber, String category)
	{
		if (category != null)
		{
			listSpellsByCategory(sender, category);
			return;
		}
		Player player = sender instanceof Player ? (Player)sender : null;

		HashMap<String, SpellGroup> spellGroups = new HashMap<String, SpellGroup>();
		List<Spell> spellVariants = controller.getAllSpells();

		int spellCount = 0;
		for (Spell spell : spellVariants)
		{
			if (player != null && !spell.hasSpellPermission(player))
			{
				continue;
			}
			if (spell.getCategory() == null) continue;
			spellCount++;
			SpellGroup group = spellGroups.get(spell.getCategory());
			if (group == null)
			{
				group = new SpellGroup();
				group.groupName = spell.getCategory();
				spellGroups.put(group.groupName, group);	
			}
			group.spells.add(spell);
		}

		List<SpellGroup> sortedGroups = new ArrayList<SpellGroup>();
		sortedGroups.addAll(spellGroups.values());
		Collections.sort(sortedGroups);

		int maxLines = -1;
		if (pageNumber >= 0) {
			maxLines = 5;
			int maxPages = spellCount / maxLines + 1;
			if (pageNumber > maxPages)
			{
				pageNumber = maxPages;
			}
			String message = Messages.get("general.spell_list_page");
			message = message.replace("$count", Integer.toString(spellCount));
			message = message.replace("$pages", Integer.toString(maxPages));
			message = message.replace("$page", Integer.toString(pageNumber));
			sender.sendMessage(message);
		} else {
			String message = Messages.get("general.spell_list");
			message = message.replace("$count", Integer.toString(spellCount));
			sender.sendMessage(message);	
		}

		int currentPage = 1;
		int lineCount = 0;
		int printedCount = 0;
		for (SpellGroup group : sortedGroups)
		{
			if (printedCount > maxLines && maxLines > 0) break;

			boolean isFirst = true;
			Collections.sort(group.spells);
			for (Spell spell : group.spells)
			{
				if (printedCount > maxLines && maxLines > 0) break;

				if (currentPage == pageNumber || maxLines < 0)
				{
					if (isFirst)
					{
						sender.sendMessage(group.groupName + ":");
						isFirst = false;
					}
					String name = spell.getName();
					String description = spell.getDescription();
					if (!name.equals(spell.getKey())) {
						description = name + " : " + description;
					}
					sender.sendMessage(ChatColor.AQUA + spell.getKey() + ChatColor.BLUE + " [" + spell.getIcon().getMaterial().name().toLowerCase() + "] : " + ChatColor.YELLOW + description);
					printedCount++;
				}
				lineCount++;
				if (lineCount == maxLines)
				{
					lineCount = 0;
					currentPage++;
				}	
			}
		}
	}

	public void onDisable() 
	{
		controller.save();
		controller.clear();
	}

	/*
	 * Private data
	 */	
	private MagicController controller = null;

	/*
	 * API Implementation
	 */
	
	@Override
	public Plugin getPlugin() {
		return this;
	}

	@Override
	public boolean hasPermission(CommandSender sender, String pNode) {
		return controller.hasPermission(sender, pNode);
	}

	@Override
	public void save() {
		controller.save();
		URLMap.save();
	}

	@Override
	public void reload() {
		controller.loadConfiguration();
		URLMap.loadConfiguration();
	}

	@Override
	public void clearCache() {
		controller.clearCache();
		URLMap.clearCache();
	}
	
	@Override
	public boolean commit() {
		return controller.commitAll();
	}
	
	@Override
	public Collection<com.elmakers.mine.bukkit.api.magic.Mage> getMages() {
		Collection<com.elmakers.mine.bukkit.api.magic.Mage> mages = new ArrayList<com.elmakers.mine.bukkit.api.magic.Mage>();
		Collection<Mage> internal = controller.getMages();
		for (Mage mage : internal) {
			mages.add(mage);
		}
		return mages;
	}
	
	@Override
	public Collection<com.elmakers.mine.bukkit.api.magic.Mage> getMagesWithPendingBatches() {
		Collection<com.elmakers.mine.bukkit.api.magic.Mage> mages = new ArrayList<com.elmakers.mine.bukkit.api.magic.Mage>();
		Collection<Mage> internal = controller.getPending();
		mages.addAll(internal);
		return mages;
	}
	
	@Override
	public Collection<LostWand> getLostWands() {
		Collection<LostWand> lostWands = new ArrayList<LostWand>();
		lostWands.addAll(controller.getLostWands());
		return lostWands;
	}
	
	@Override
	public Collection<Automaton> getAutomata() {
		Collection<Automaton> automata = new ArrayList<Automaton>();
		automata.addAll(controller.getAutomata());
		return automata;
	}

	@Override
	public void removeLostWand(String id) {
		controller.removeLostWand(id);
	}

	@Override
	public com.elmakers.mine.bukkit.api.magic.Wand getWand(ItemStack itemStack) {
		return new Wand(controller, itemStack);
	}
	
	public boolean isWand(ItemStack item) {
		return Wand.isWand(item);
	}

	@Override
	public void giveItemToPlayer(Player player, ItemStack itemStack) {
		// Place directly in hand if possible
		PlayerInventory inventory = player.getInventory();
		ItemStack inHand = inventory.getItemInHand();
		if (inHand == null || inHand.getType() == Material.AIR) {
			inventory.setItem(inventory.getHeldItemSlot(), itemStack);
			if (Wand.isWand(itemStack)) {
				Wand wand = new Wand(controller, itemStack);
				wand.activate(controller.getMage(player));
			}
		} else {
			HashMap<Integer, ItemStack> returned = player.getInventory().addItem(itemStack);
			if (returned.size() > 0) {
				player.getWorld().dropItem(player.getLocation(), itemStack);
			}
		}
	}

	@Override
	public com.elmakers.mine.bukkit.api.magic.Mage getMage(CommandSender sender) {
		return controller.getMage(sender);
	}

	@Override
	public com.elmakers.mine.bukkit.api.magic.Wand createWand(String wandKey) {
		return Wand.createWand(controller, wandKey);
	}

	@Override
	public ItemStack createSpellItem(String spellKey) {
		return Wand.createSpellItem(spellKey, controller, null, true);
	}

	@Override
	public ItemStack createBrushItem(String brushKey) {
		return Wand.createMaterialItem(brushKey, controller, null, true);
	}

	@Override
	public void cast(String spellName, String[] parameters) {
		controller.cast(null, spellName, parameters, null, null);
	}
}
