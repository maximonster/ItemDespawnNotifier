package com.DropDespawnNotifier;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("DropDespawnNotifier")
public interface DropDespawnNotifierConfig extends Config
{
	@ConfigItem(
			keyName = "NotifySeconds",
			name = "Seconds before despawn",
			description = "The amount of seconds before the notification should trigger",
			position = 0
	)
	default int NotifySeconds()
	{
		return 15;
	}
	@ConfigItem(
			keyName = "GEValue",
			name = "Minimum GE Value",
			description = "GEValue",
			position = 2
	)
	default int GEValue()
	{
		return 20000;
	}
	@ConfigItem(
			keyName = "HAValue",
			name = "Minimum HA Value",
			description = "HAValue",
			position = 3
	)
	default int HAValue()
	{
		return 20000;
	}
	@ConfigItem(
			keyName = "highlightedItems",
			name = "Highlighted Items",
			description = "Configures ground items to notify for no matter the GE or HA value. Format: (item), (item)",
			position = 1
	)
	default String highlightedItems()
	{
		return "";
	}
	@ConfigItem(
			keyName = "UseGroundItemList",
			name = "Use Ground Items Highlight List",
			description = "If enabled uses the highlighted items from Ground items instead of the one configured in this plugin",
			position = 4
	)
	default boolean UseGroundItemList()
	{
		return false;
	}


}
