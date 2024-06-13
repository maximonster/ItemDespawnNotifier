package com.DropDespawnNotifier;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class DropDespawnNotifierPluginTest
{
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(DropDespawnNotifierPlugin.class);
		RuneLite.main(args);
	}
}