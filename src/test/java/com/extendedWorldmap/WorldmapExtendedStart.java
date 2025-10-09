package com.extendedWorldmap;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class WorldmapExtendedStart
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(WorldmapExtendedPlugin.class);
		RuneLite.main(args);
	}
}