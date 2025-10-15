package com.worldmapextended;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class worldMapExtendedStart
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(WorldMapExtendedPlugin.class);
		RuneLite.main(args);
	}
}