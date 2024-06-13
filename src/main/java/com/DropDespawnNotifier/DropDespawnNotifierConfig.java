package com.DropDespawnNotifier;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("NPCTaunts")
public interface DropDespawnNotifierConfig extends Config
{
	@ConfigItem(
			keyName = "custombosstaunts",
			name = "Custom Boss Taunts",
			description = "Adds your own taunts to a boss. See readme on how to use",
			position = 1
	)
	default String custombosstaunts()
	{
		return "ExactBossname;Custom Taunt 1:Custom Taunt 2\n"+"ExactBossname2;Custom Taunt 3:Custom Taunt 4";
	}

	default boolean texttospeech()
	{
		return false;
	}



}
