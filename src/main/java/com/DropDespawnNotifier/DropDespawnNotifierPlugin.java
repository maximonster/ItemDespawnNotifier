package com.DropDespawnNotifier;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.grounditems.GroundItemsConfig;
import net.runelite.client.plugins.grounditems.GroundItemsPlugin;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.client.util.RSTimeUnit;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.runelite.api.ItemID.COINS;

@Slf4j
@PluginDescriptor(
        name = "DropDespawnNotifier"
)
public class DropDespawnNotifierPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private ItemManager itemManager;
    @Inject
    private DropDespawnNotifierConfig config;
    @Inject
    private Notifier notifier;

    @Getter
    private final Table<WorldPoint, Integer, DSNGroundItem> collectedGroundItems = HashBasedTable.create();
    private String[] NotifyItems;
    private List<String> highlightedItemsList = new CopyOnWriteArrayList<>();

    @Override
    protected void startUp() throws Exception {


            NotifyItems =  config.highlightedItems().split(",");

    }
    @Override
    protected void shutDown()
    {
        collectedGroundItems.clear();
    }
    @Subscribe
    public void onConfigChanged(ConfigChanged event){

            NotifyItems =  config.highlightedItems().split(",");

    }
    @Subscribe
    public void onGameTick(GameTick event) {
        DSNGroundItem[] items = collectedGroundItems.values().toArray(new DSNGroundItem[collectedGroundItems.values().size()]);
        for (int i = 0; i < items.length; i++) {
            if (items[i].notified){
                continue;
            }
            Instant despawnt = calculateDespawnTime(items[i]);
            long s =config.NotifySeconds();
            if (despawnt == null){
                continue;
            }

            Instant despawntime = despawnt.minusSeconds(s);
            if (Instant.now().compareTo(despawntime)>=0){
                final StringBuilder notificationStringBuilder = new StringBuilder()
                        .append("Your dropped item: ")
                        .append(items[i].getName());

                if (items[i].getQuantity() > 1)
                {
                    notificationStringBuilder.append(" (")
                            .append(QuantityFormatter.quantityToStackSize(items[i].getQuantity()))
                            .append(')');
                }
                notificationStringBuilder.append(" will despawn in ")
                        .append(config.NotifySeconds())
                        .append(" seconds.");
                notifier.notify(notificationStringBuilder.toString());
                items[i].notified = true;
            }
        }
    }
    @Subscribe
    public void onItemSpawned(ItemSpawned itemSpawned)
    {
        TileItem item = itemSpawned.getItem();
        Tile tile = itemSpawned.getTile();

        DSNGroundItem groundItem = buildGroundItem(tile, item);
        if (groundItem.getGePrice()>config.GEValue()||groundItem.getHaPrice()>config.HAValue()||NotifyItemsContains(groundItem)){
            if(groundItem.isMine()){
            DSNGroundItem existing = collectedGroundItems.get(tile.getWorldLocation(), item.getId());
            if (existing != null)
            {
                existing.setQuantity(existing.getQuantity() + groundItem.getQuantity());
                // The spawn time remains set at the oldest spawn
                existing.reset();
            }
            else
            {
                collectedGroundItems.put(tile.getWorldLocation(), item.getId(), groundItem);
            }
            }
        }
    }


    @Subscribe
    public void onItemDespawned(ItemDespawned itemDespawned)
    {
        TileItem item = itemDespawned.getItem();
        Tile tile = itemDespawned.getTile();

        DSNGroundItem groundItem = collectedGroundItems.get(tile.getWorldLocation(), item.getId());
        if (groundItem == null)
        {
            return;
        }

        if (groundItem.getQuantity() <= item.getQuantity())
        {
            collectedGroundItems.remove(tile.getWorldLocation(), item.getId());
        }
        else
        {
            groundItem.setQuantity(groundItem.getQuantity() - item.getQuantity());
            // When picking up an item when multiple stacks appear on the ground,
            // it is not known which item is picked up, so we invalidate the spawn
            // time
            groundItem.setSpawnTime(null);
            groundItem.reset();
        }

    }

    @Subscribe
    public void onItemQuantityChanged(ItemQuantityChanged itemQuantityChanged)
    {
        TileItem item = itemQuantityChanged.getItem();
        Tile tile = itemQuantityChanged.getTile();
        int oldQuantity = itemQuantityChanged.getOldQuantity();
        int newQuantity = itemQuantityChanged.getNewQuantity();

        int diff = newQuantity - oldQuantity;
        DSNGroundItem groundItem = collectedGroundItems.get(tile.getWorldLocation(), item.getId());
        if (groundItem != null)
        {
            groundItem.setQuantity(groundItem.getQuantity() + diff);
            groundItem.reset();
        }
    }

    private DSNGroundItem buildGroundItem(final Tile tile, final TileItem item)
    {
        // Collect the data for the item
        final int itemId = item.getId();
        final ItemComposition itemComposition = itemManager.getItemComposition(itemId);
        final int realItemId = itemComposition.getNote() != -1 ? itemComposition.getLinkedNoteId() : itemId;
        final int alchPrice = itemComposition.getHaPrice();
        final int despawnTime = item.getDespawnTime() - client.getTickCount();
        final int visibleTime = item.getVisibleTime() - client.getTickCount();
        int ownership = item.getOwnership();
        final int accountType = client.getVarbitValue(Varbits.ACCOUNT_TYPE);
        boolean isGim = accountType >= 4 && accountType <= 6; // ~is_group_iron
        boolean notified = false;
        // from ~script7240
        if (ownership == TileItem.OWNERSHIP_GROUP && !isGim)
        {
            // non-gims see loot from other people as "group" loop since they are both group -1.
            ownership = TileItem.OWNERSHIP_OTHER;
        }

        final DSNGroundItem groundItem = DSNGroundItem.builder()
                .id(itemId)
                .location(tile.getWorldLocation())
                .itemId(realItemId)
                .quantity(item.getQuantity())
                .name(itemComposition.getName())
                .haPrice(alchPrice)
                .height(tile.getItemLayer().getHeight())
                .tradeable(itemComposition.isTradeable())
                .ownership(ownership)
                .isPrivate(item.isPrivate())
                .spawnTime(Instant.now())
                .stackable(itemComposition.isStackable())
                .despawnTime(Duration.of(despawnTime, RSTimeUnit.GAME_TICKS))
                .visibleTime(Duration.of(visibleTime, RSTimeUnit.GAME_TICKS))
                .notified(notified)
                .build();

        // Update item price in case it is coins
        if (realItemId == COINS)
        {
            groundItem.setHaPrice(1);
            groundItem.setGePrice(1);
        }
        else
        {
            groundItem.setGePrice(itemManager.getItemPrice(realItemId));
        }

        return groundItem;
    }
    private Instant calculateDespawnTime(DSNGroundItem groundItem)
    {
        Instant spawnTime = groundItem.getSpawnTime();
        if (spawnTime == null)
        {
            return null;
        }

        Instant despawnTime = spawnTime.plus(groundItem.getDespawnTime());
        if (Instant.now().isAfter(despawnTime))
        {
            // that's weird
            return null;
        }

        return despawnTime;
    }

    private boolean NotifyItemsContains(DSNGroundItem item){

        for (int i = 0; i < NotifyItems.length; i++) {
            if (NotifyItems[i].contains(item.getName())){
                return true;
            }
        }
        return false;
    }
    @Provides
    DropDespawnNotifierConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(DropDespawnNotifierConfig.class);
    }
}
