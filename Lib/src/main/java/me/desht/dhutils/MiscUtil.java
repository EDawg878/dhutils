package me.desht.dhutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class MiscUtil {
	private static Map<String, ChatColor> prevColours = new HashMap<String, ChatColor>();

//	private static String messagePrefix = "";
	private static final String BROADCAST_PREFIX = ChatColor.RED + "\u2731&- ";
	private static boolean colouredConsole = true;

	public static void init(Plugin plugin) {
//		messagePrefix = "[" + plugin.getDescription().getName() + "]: ";
	}

	public static void setColouredConsole(boolean coloured) {
		colouredConsole = coloured;
	}

	public static void errorMessage(CommandSender sender, String string) {
		setPrevColour(sender.getName(), ChatColor.RED);
		message(sender, ChatColor.RED + string, Level.WARNING);
	}

	public static void statusMessage(CommandSender sender, String string) {
		setPrevColour(sender.getName(), ChatColor.AQUA);
		message(sender, ChatColor.AQUA + string, Level.INFO);
	}

	public static void alertMessage(CommandSender sender, String string) {
		setPrevColour(sender.getName(), ChatColor.YELLOW);
		message(sender, ChatColor.YELLOW + string, Level.INFO);
	}

	public static void generalMessage(CommandSender sender, String string) {
		setPrevColour(sender.getName(), ChatColor.WHITE);
		message(sender, string, Level.INFO);
	}

	public static void broadcastMessage(String string) {
		CommandSender sender = Bukkit.getConsoleSender();
		setPrevColour(sender.getName(), ChatColor.YELLOW);
		Bukkit.getServer().broadcastMessage(parseColourSpec(sender, BROADCAST_PREFIX + string));
	}

	private static void setPrevColour(String name, ChatColor colour) {
		prevColours.put(name, colour);
	}

	private static ChatColor getPrevColour(String name) {
		if (!prevColours.containsKey(name)) {
			setPrevColour(name, ChatColor.WHITE);
		}
		return prevColours.get(name);
	}

	public static void rawMessage(CommandSender sender, String string) {
		boolean strip = sender instanceof ConsoleCommandSender && !colouredConsole;
		for (String line : string.split("\\n")) {
			if (strip) {
				sender.sendMessage(ChatColor.stripColor(line));
			} else {
				sender.sendMessage(line);
			}
		}
	}

	private static void message(CommandSender sender, String string, Level level) {
		boolean strip = sender instanceof ConsoleCommandSender && !colouredConsole;
		for (String line : string.split("\\n")) {
			if (strip) {
				LogUtils.log(level, ChatColor.stripColor(parseColourSpec(sender, line)));
			} else {
				sender.sendMessage(parseColourSpec(sender, line));
			}
		}
	}

	public static String formatLocation(Location loc) {
		return String.format("%d,%d,%d,%s", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName());
	}

	public static Location parseLocation(String arglist) {
		return parseLocation(arglist, null);
	}

	public static Location parseLocation(String arglist, CommandSender sender) {
		String s = sender instanceof Player ? "" : ",worldname";
		String args[] = arglist.split(",");

		try {
			int x = Integer.parseInt(args[0]);
			int y = Integer.parseInt(args[1]);
			int z = Integer.parseInt(args[2]);
			World w = (sender instanceof Player) ? findWorld(args[3]) : ((Player)sender).getWorld();
			return new Location(w, x, y, z);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException("You must specify all of x,y,z" + s + ".");
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid number in " + arglist);
		}
	}

	private static final Pattern colourPat = Pattern.compile("(?<!&)&(?=[0-9a-fA-Fk-oK-OrR])");

	public static String parseColourSpec(String spec) {
		return parseColourSpec(null, spec);
	}

	public static String parseColourSpec(CommandSender sender, String spec) {
		String who = sender == null ? "*" : sender.getName();
		String res = colourPat.matcher(spec).replaceAll("\u00A7");
		return res.replace("&-", getPrevColour(who).toString()).replace("&&", "&");
	}

	public static String unParseColourSpec(String spec) {
		return spec.replaceAll("\u00A7", "&");
	}

	/**
	 * Find the given world by name.
	 *
	 * @param worldName name of the world to find
	 * @return the World object representing the world name
	 * @throws IllegalArgumentException if the given world cannot be found
	 */
	public static World findWorld(String worldName) {
		World w = Bukkit.getServer().getWorld(worldName);
		if (w != null) {
			return w;
		} else {
			throw new IllegalArgumentException("World " + worldName + " was not found on the server.");
		}
	}

	/**
	 * Split the given string, but ensure single & double quoted sections of the string are 
	 * kept together.
	 * <p>
	 * E.g. the String 'one "two three" four' will be split into [ "one", "two three", "four" ]
	 *
	 * @param s the String to split
	 * @return a List of items
	 */
	public static List<String> splitQuotedString(String s) {
		List<String> matchList = new ArrayList<String>();

		Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
		Matcher regexMatcher = regex.matcher(s);

		while (regexMatcher.find()) {
			if (regexMatcher.group(1) != null) {
				// Add double-quoted string without the quotes
				matchList.add(regexMatcher.group(1));
			} else if (regexMatcher.group(2) != null) {
				// Add single-quoted string without the quotes
				matchList.add(regexMatcher.group(2));
			} else {
				// Add unquoted word
				matchList.add(regexMatcher.group());
			}
		}

		return matchList;
	}

	/**
	 * Return the given collection (of Comparable items) as a sorted list.
	 *
	 * @param c	the collection to sort
	 * @return a list of the sorted items in the collection
	 */
	public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
		List<T> list = new ArrayList<T>(c);
		java.util.Collections.sort(list);
		return list;
	}

    /**
     * @param list
     * @param nLists
     * @return
     */
    public static <T> List<T>[] splitList(List<T> list, int nLists) {
    	@SuppressWarnings("unchecked")
		List<T>[] res = (ArrayList<T>[]) new ArrayList[nLists];
    	Collections.shuffle(list);
    	for (int i = 0; i < list.size(); i++) {
    		res[i % nLists].add(list.get(i));
    	}
    	return res;
    }

	/**
	 * Get a list of all files in the given JAR (or ZIP) file within the given path, and with the
	 * given extension.
	 * 
	 * @param jarFile	the JAR file to search
	 * @param path	the path within the JAR file to search
	 * @param ext	desired extension, may be null
	 * @return	an array of path names to the found resources
	 * @throws IOException
	 */
	public static String[] listFilesinJAR(File jarFile, String path, String ext) throws IOException {
		ZipInputStream zip = new ZipInputStream(new FileInputStream(jarFile));
		ZipEntry ze = null;

		List<String> list = new ArrayList<String>();
		while ((ze = zip.getNextEntry()) != null ) {
			String entryName = ze.getName();
			if (entryName.startsWith(path) && ext != null && entryName.endsWith(ext)) {
				list.add(entryName);
			}
		}
		zip.close();

		return list.toArray(new String[list.size()]);
	}

	/**
	 * Load a YAML file, enforcing UTF-8 encoding, and get the YAML configuration from it.
	 * 
	 * @param file the file to load
	 * @return the YAML configuration from that file
	 * @throws InvalidConfigurationException
	 * @throws IOException
	 */
	public static YamlConfiguration loadYamlUTF8(File file) throws InvalidConfigurationException, IOException {		
		StringBuilder sb = new StringBuilder((int) file.length());

		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
		char[] buf = new char[1024];
		int l;
		while ((l = in.read(buf, 0, buf.length)) > -1) {
			sb = sb.append(buf, 0, l);
		}
		in.close();

		YamlConfiguration yaml = new YamlConfiguration();
		yaml.loadFromString(sb.toString());

		return yaml;
	}
}
