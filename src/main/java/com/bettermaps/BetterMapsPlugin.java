package com.bettermaps;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GroundObject;
import net.runelite.api.Scene;
import net.runelite.api.ScriptID;
import net.runelite.api.SpritePixels;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.worldmap.MapElementConfig;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.api.worldmap.WorldMapIcon;
import net.runelite.api.worldmap.WorldMapRegion;
import net.runelite.api.worldmap.WorldMapRenderer;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

/**
 * It is not yet possible to fully hide the world map icons, and a pull request has been open for > 5 years to modify
 * the runelite 'World Map' client plugin (https://github.com/runelite/runelite/pull/11599) to add this functionality.
 * However, within the current API, there is an ability to move the icon sprites by setting their offset positions,
 * giving the illusion of them being hidden. This plugin has therefore been generated to provide a middle-ground solution
 * and work in conjunction with the existing 'World Map' client plugin, however as a result of the natural overlap
 * between this and the existing 'World Map' client plugin, some segments of code have been re-used to more effectively
 * represent the customisations available.
 * <p>
 * Moving the sprites leaves the icon in place therefore the default tooltips would still be visible when hovering over
 * their normal locations (this remains also true with the 'World Map' plugin tooltips, and is part of the reason for
 * some duplication). To alleviate this issue when considering the default tooltips, the standard tooltip generation is
 * disabled and custom tooltips are required for each icon type.
 */

@Slf4j
@PluginDescriptor(
	name = "Better Maps",
	description = "A more customisable map system with the ability to hide icons, to be used in conjunction with the existing client plugins.",
	tags = {"worldmap", "minimap", "map", "icon", "hide", "clean"}
)
public class BetterMapsPlugin extends Plugin
{
	static final String CONFIG_GROUP = "worldMapExtended" ;

	private static final int HOLIDAY_EVENT_ICON_CATEGORY = 1119;
	private static final int OFFSET_TO_HIDE_ICON_SPRITES = 25000;
	private static final int WORLDMAP_ELEMENTS_MARKER_SCRIPT_ID = 1757;
	private static final int WORLDMAP_ELEMENTS_TOOLTIP_SCRIPT_ID = 1847;

	/**
	 * Code copied and modified from Runelite worldmap client plugin, used to generate blank icons to hold the tooltips.
	 */
	private static final BufferedImage BLANK_ICON;
	private static final BufferedImage BLANK_QUEST_ICON;

	static
	{
		// Original size of world map icons
		final int worldMapIconsize = 15;
		//A size of 17 gives us a buffer when triggering tooltips
		final int iconOffset = 1;
		final int iconBufferSize = worldMapIconsize + iconOffset * 2;
		// Quest icons are a bit bigger than regular icons
		// A size of 25 aligns the quest icons when converting the world map point to pixel coordinates
		// The new quest icons must be offset by 5, for a size of 25, to align when drawing on top of the original icon
		final int questIconOffset = 5;
		final int questIconBufferSize = worldMapIconsize + questIconOffset * 2;

		BLANK_ICON = new BufferedImage(iconBufferSize, iconBufferSize, BufferedImage.TYPE_INT_ARGB);
		BLANK_QUEST_ICON = new BufferedImage(questIconBufferSize, questIconBufferSize, BufferedImage.TYPE_INT_ARGB);
	}

	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private ConfigManager configManager;
	@Inject
	private PluginManager pluginManager;
	@Inject
	private BetterMapsConfig config;
	@Inject
	private WorldMapPointManager worldMapPointManager;

	private boolean showTooltips;

	private boolean worldMapTransportationTooltips;
	private boolean worldMapAgilityCourseTooltips;
	private boolean worldMapAgilityShortcutTooltips;
	private boolean worldMapMinigameTooltips;
	private boolean worldMapFarmingTooltips;
	private boolean worldMapRareTreesTooltips;
	private boolean worldMapMiningTooltips;
	private boolean worldMapDungeonTooltips;
	private boolean worldMapHunterTooltips;
	private boolean worldMapFishingTooltips;

	@Override
	protected void startUp() throws Exception
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			checkConfigForClashes();
			addTooltipsToWorldMap();
			updateWorldMapIcons();
			refreshSceneGroundObjects();
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		resetWorldMapIcons();
		refreshSceneGroundObjects();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals(CONFIG_GROUP))
		{
			checkConfigForClashes();
			addTooltipsToWorldMap();
			updateWorldMapIcons();
			refreshSceneGroundObjects();
		}
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired scriptPreFired)
	{
		if (scriptPreFired.getScriptId() == WORLDMAP_ELEMENTS_TOOLTIP_SCRIPT_ID)
		{
			client.setVarcIntValue(VarClientID.TOOLTIP_BUILT, 1);
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired scriptPostFired)
	{
		if (scriptPostFired.getScriptId() == ScriptID.WORLDMAP_LOADMAP)
		{
			checkConfigForClashes();
			addTooltipsToWorldMap();
		}
		else if (scriptPostFired.getScriptId() == WORLDMAP_ELEMENTS_MARKER_SCRIPT_ID)
		{
			// this is called whenever the map is changed, since it needs to dynamically load the map area viewed
			updateWorldMapIcons();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged varbitChanged)
	{
		if (varbitChanged.getVarbitId() == VarbitID.WORLDMAP_TOGGLES)
		{
			getWhetherTooltipsShouldBeShown();
			checkConfigForClashes();
			addTooltipsToWorldMap();
		}
	}

	private Set<Integer> wantedCategories = new HashSet<>();

	private void checkConfigForClashes()
	{
		// Firstly, gets the config information from the existing client 'World Map' plugin. Then, collects the settings
		// specified for this plugin, and compares to inform the user if there is likely to be a clash.
		checkRuneliteWorldMapClientPluginSettings();

		wantedCategories.clear();
		// Always add the map link category back in, since the icons do not actually appear to be icons like the others
		wantedCategories.add(MapIcons.MAP_LINK.getCategory());

		if (config.agilityCoursesIcon())
		{
			wantedCategories.add(MapIcons.AGILITY_TRAINING.getCategory());
		}
		if (config.agilityShortcutsIcon())
		{
			wantedCategories.add(MapIcons.AGILITY_SHORTCUT.getCategory());
		}
		if (config.altarIcon())
		{
			wantedCategories.add(MapIcons.ALTAR.getCategory());
		}
		if (config.amuletshopIcon())
		{
			wantedCategories.add(MapIcons.AMULET_SHOP.getCategory());
		}
		if (config.anvilIcon())
		{
			wantedCategories.add(MapIcons.ANVIL.getCategory());
		}
		if (config.apothecaryIcon())
		{
			wantedCategories.add(MapIcons.APOTHECARY.getCategory());
		}
		if (config.archeryshopIcon())
		{
			wantedCategories.add(MapIcons.ARCHERY_SHOP.getCategory());
		}
		if (config.axeshopIcon())
		{
			wantedCategories.add(MapIcons.AXE_SHOP.getCategory());
		}
		if (config.bankIcon())
		{
			wantedCategories.add(MapIcons.BANK.getCategory());
		}
		if (config.bankTutorIcon())
		{
			wantedCategories.add(MapIcons.BANK_TUTOR.getCategory());
		}
		if (config.birdHouseIcon())
		{
			wantedCategories.add(MapIcons.BIRD_HOUSE_SITE.getCategory());
		}
		if (config.bondTutorIcon())
		{
			wantedCategories.add(MapIcons.BOND_TUTOR.getCategory());
		}
		if (config.bountyHunterIcon())
		{
			wantedCategories.add(MapIcons.BOUNTY_HUNTER_TRADER.getCategory());
		}
		if (config.breweryIcon())
		{
			wantedCategories.add(MapIcons.BREWERY.getCategory());
		}
		if (config.candleShopIcon())
		{
			wantedCategories.add(MapIcons.CANDLE_SHOP.getCategory());
		}
		if (config.chainbodyShopIcon())
		{
			wantedCategories.add(MapIcons.CHAINMAIL_SHOP.getCategory());
		}
		if (config.clanHubIcon())
		{
			wantedCategories.add(MapIcons.CLAN_HUB.getCategory());
		}
		if (config.clothesShopIcon())
		{
			wantedCategories.add(MapIcons.CLOTHES_SHOP.getCategory());
		}
		if (config.clueTutorIcon())
		{
			wantedCategories.add(MapIcons.CLUE_TUTOR.getCategory());
		}
		if (config.combatAchievementsIcon())
		{
			wantedCategories.add(MapIcons.COMBAT_ACHIEVEMENTS.getCategory());
		}
		if (config.combatTrainingIcon())
		{
			wantedCategories.add(MapIcons.COMBAT_TRAINING.getCategory());
		}
		if (config.combatTutorIcon())
		{
			wantedCategories.add(MapIcons.COMBAT_TUTOR.getCategory());
		}
		if (config.cookingRangeIcon())
		{
			wantedCategories.add(MapIcons.COOKING_RANGE.getCategory());
		}
		if (config.cookingTutorIcon())
		{
			wantedCategories.add(MapIcons.COOKING_TUTOR.getCategory());
		}
		if (config.craftingShopIcon())
		{
			wantedCategories.add(MapIcons.CRAFTING_SHOP.getCategory());
		}
		if (config.craftingTutorIcon())
		{
			wantedCategories.add(MapIcons.CRAFTING_TUTOR.getCategory());
		}
		if (config.dairyChurnIcon())
		{
			wantedCategories.add(MapIcons.DAIRY_CHURN.getCategory());
		}
		if (config.dairyCowIcon())
		{
			wantedCategories.add(MapIcons.DAIRY_COW.getCategory());
		}
		if (config.dangerTutorIcon())
		{
			wantedCategories.add(MapIcons.DANGER_TUTOR.getCategory());
		}
		if (config.deadmanTutorIcon())
		{
			wantedCategories.add(MapIcons.DEADMAN_TUTOR.getCategory());
		}
		if (config.deathsOfficeIcon())
		{
			wantedCategories.add(MapIcons.DEATHS_OFFICE.getCategory());
		}
		if (config.distractionAndDiversionIcon())
		{
			wantedCategories.add(MapIcons.DISTRACTION_AND_DIVERSION.getCategory());
		}
		if (config.dungeonIcon())
		{
			wantedCategories.add(MapIcons.DUNGEON.getCategory());
		}
		if (config.dyeTraderIcon())
		{
			wantedCategories.add(MapIcons.DYE_TRADER.getCategory());
		}
		if (config.estateAgentIcon())
		{
			wantedCategories.add(MapIcons.ESTATE_AGENT.getCategory());
		}
		if (config.farmingPatchIcon())
		{
			wantedCategories.add(MapIcons.FARMING_PATCH.getCategory());
		}
		if (config.farmingShopIcon())
		{
			wantedCategories.add(MapIcons.FARMING_SHOP.getCategory());
		}
		if (config.fishingShopIcon())
		{
			wantedCategories.add(MapIcons.FISHING_SHOP.getCategory());
		}
		if (config.fishingSpotIcon())
		{
			wantedCategories.add(MapIcons.FISHING_SPOT.getCategory());
		}
		if (config.fishingTutorIcon())
		{
			wantedCategories.add(MapIcons.FISHING_TUTOR.getCategory());
		}
		if (config.foodShopIcon())
		{
			wantedCategories.add(MapIcons.FOOD_SHOP.getCategory());
		}
		if (config.forestryShopIcon())
		{
			wantedCategories.add(MapIcons.FORESTRY_SHOP.getCategory());
		}
		if (config.furTraderIcon())
		{
			wantedCategories.add(MapIcons.FUR_TRADER.getCategory());
		}
		if (config.furnaceIcon())
		{
			wantedCategories.add(MapIcons.FURNACE.getCategory());
		}
		if (config.gardenSupplierIcon())
		{
			wantedCategories.add(MapIcons.GARDEN_SUPPLIER.getCategory());
		}
		if (config.gemShopIcon())
		{
			wantedCategories.add(MapIcons.GEM_SHOP.getCategory());
		}
		if (config.generalStoreIcon())
		{
			wantedCategories.add(MapIcons.GENERAL_STORE.getCategory());
		}
		if (config.grandExchangeIcon())
		{
			wantedCategories.add(MapIcons.GRAND_EXCHANGE.getCategory());
		}
		if (config.grindstoneIcon())
		{
			wantedCategories.add(MapIcons.GRINDSTONE.getCategory());
		}
		if (config.hairdresserIcon())
		{
			wantedCategories.add(MapIcons.HAIRDRESSER.getCategory());
		}
		if (config.helmetShopIcon())
		{
			wantedCategories.add(MapIcons.HELMET_SHOP.getCategory());
		}
		if (config.herbalistIcon())
		{
			wantedCategories.add(MapIcons.HERBALIST.getCategory());
		}
		if (config.holidayEventIcon())
		{
			wantedCategories.add(HOLIDAY_EVENT_ICON_CATEGORY); // treated differently to avoid needing coordinates
		}
		if (config.holidayItemTraderIcon())
		{
			wantedCategories.add(MapIcons.HOLIDAY_ITEM_TRADER.getCategory());
		}
		if (config.housePortalIcon())
		{
			wantedCategories.add(MapIcons.HOUSE_PORTAL.getCategory());
		}
		if (config.hunterShopIcon())
		{
			wantedCategories.add(MapIcons.HUNTER_SHOP.getCategory());
		}
		if (config.hunterTrainingIcon())
		{
			wantedCategories.add(MapIcons.HUNTER_TRAINING.getCategory());
		}
		if (config.hunterTutorIcon())
		{
			wantedCategories.add(MapIcons.HUNTER_TUTOR.getCategory());
		}
		if (config.ironmanTutorIcon())
		{
			wantedCategories.add(MapIcons.IRONMAN_TUTOR.getCategory());
		}
		if (config.jewelleryShopIcon())
		{
			wantedCategories.add(MapIcons.JEWELLERY_SHOP.getCategory());
		}
		if (config.junkCheckerIcon())
		{
			wantedCategories.add(MapIcons.JUNK_CHECKER.getCategory());
		}
		if (config.leaguesTutorIcon())
		{
			wantedCategories.add(MapIcons.LEAGUES_TUTOR.getCategory());
		}
		if (config.loomIcon())
		{
			wantedCategories.add(MapIcons.LOOM.getCategory());
		}
		if (config.lumbridgeGuideIcon())
		{
			wantedCategories.add(MapIcons.LUMBRIDGE_GUIDE.getCategory());
		}
		if (config.maceShopIcon())
		{
			wantedCategories.add(MapIcons.MACE_SHOP.getCategory());
		}
		if (config.magicShopIcon())
		{
			wantedCategories.add(MapIcons.MAGIC_SHOP.getCategory());
		}
		if (config.makeoverMageIcon())
		{
			wantedCategories.add(MapIcons.MAKEOVER_MAGE.getCategory());
		}
		if (config.minigameIcon())
		{
			wantedCategories.add(MapIcons.MINIGAME.getCategory());
		}
		if (config.miningShopIcon())
		{
			wantedCategories.add(MapIcons.MINING_SHOP.getCategory());
		}
		if (config.miningSiteIcon())
		{
			wantedCategories.add(MapIcons.MINING_SITE.getCategory());
		}
		if (config.miningTutorIcon())
		{
			wantedCategories.add(MapIcons.MINING_TUTOR.getCategory());
		}
		if (config.newspaperTraderIcon())
		{
			wantedCategories.add(MapIcons.NEWSPAPER_TRADER.getCategory());
		}
		if (config.petShopIcon())
		{
			wantedCategories.add(MapIcons.PET_SHOP.getCategory());
		}
		if (config.platebodyShopIcon())
		{
			wantedCategories.add(MapIcons.PLATEBODY_SHOP.getCategory());
		}
		if (config.platelegsShopIcon())
		{
			wantedCategories.add(MapIcons.PLATELEGS_SHOP.getCategory());
		}
		if (config.plateskirtShopIcon())
		{
			wantedCategories.add(MapIcons.PLATESKIRT_SHOP.getCategory());
		}
		if (config.polishingWheelIcon())
		{
			wantedCategories.add(MapIcons.POLISHING_WHEEL.getCategory());
		}
		if (config.pollBoothIcon())
		{
			wantedCategories.add(MapIcons.POLL_BOOTH.getCategory());
		}
		if (config.potteryWheelIcon())
		{
			wantedCategories.add(MapIcons.POTTERY_WHEEL.getCategory());
		}
		if (config.prayerTutorIcon())
		{
			wantedCategories.add(MapIcons.PRAYER_TUTOR.getCategory());
		}
		if (config.pricingExpertIcon())
		{
			wantedCategories.add(MapIcons.PRICING_EXPERT.getCategory());
		}
		if (config.pubIcon())
		{
			wantedCategories.add(MapIcons.PUB.getCategory());
		}
		if (config.questStartIcon())
		{
			wantedCategories.add(MapIcons.QUEST_START.getCategory());
		}
		if (config.raidIcon())
		{
			wantedCategories.add(MapIcons.RAID.getCategory());
		}
		if (config.rareTreesIcon())
		{
			wantedCategories.add(MapIcons.RARE_TREES.getCategory());
		}
		if (config.ropeTraderIcon())
		{
			wantedCategories.add(MapIcons.ROPE_TRADER.getCategory());
		}
		if (config.sandpitIcon())
		{
			wantedCategories.add(MapIcons.SANDPIT.getCategory());
		}
		if (config.sawmillIcon())
		{
			wantedCategories.add(MapIcons.SAWMILL.getCategory());
		}
		if (config.scimitarShopIcon())
		{
			wantedCategories.add(MapIcons.SCIMITAR_SHOP.getCategory());
		}
		if (config.securityTutorIcon())
		{
			wantedCategories.add(MapIcons.SECURITY_TUTOR.getCategory());
		}
		if (config.shieldShopIcon())
		{
			wantedCategories.add(MapIcons.SHIELD_SHOP.getCategory());
		}
		if (config.silkTraderIcon())
		{
			wantedCategories.add(MapIcons.SILK_TRADER.getCategory());
		}
		if (config.silverShopIcon())
		{
			wantedCategories.add(MapIcons.SILVER_SHOP.getCategory());
		}
		if (config.singingBowlIcon())
		{
			wantedCategories.add(MapIcons.SINGING_BOWL.getCategory());
		}
		if (config.slayerMasterIcon())
		{
			wantedCategories.add(MapIcons.SLAYER_MASTER.getCategory());
		}
		if (config.smithingTutorIcon())
		{
			wantedCategories.add(MapIcons.SMITHING_TUTOR.getCategory());
		}
		if (config.speedrunningShopIcon())
		{
			wantedCategories.add(MapIcons.SPEEDRUNNING_SHOP.getCategory());
		}
		if (config.spiceShopIcon())
		{
			wantedCategories.add(MapIcons.SPICE_SHOP.getCategory());
		}
		if (config.spinningWheelIcon())
		{
			wantedCategories.add(MapIcons.SPINNING_WHEEL.getCategory());
		}
		if (config.staffShopIcon())
		{
			wantedCategories.add(MapIcons.STAFF_SHOP.getCategory());
		}
		if (config.stagnantWaterSourceIcon())
		{
			wantedCategories.add(MapIcons.STAGNANT_WATER_SOURCE.getCategory());
		}
		if (config.stonemasonIcon())
		{
			wantedCategories.add(MapIcons.STONEMASON.getCategory());
		}
		if (config.swordShopIcon())
		{
			wantedCategories.add(MapIcons.SWORD_SHOP.getCategory());
		}
		if (config.tanneryIcon())
		{
			wantedCategories.add(MapIcons.TANNERY.getCategory());
		}
		if (config.taskMasterIcon())
		{
			wantedCategories.add(MapIcons.TASK_MASTER.getCategory());
		}
		if (config.taxidermistIcon())
		{
			wantedCategories.add(MapIcons.TAXIDERMIST.getCategory());
		}
		if (config.teaTraderIcon())
		{
			wantedCategories.add(MapIcons.TEA_TRADER.getCategory());
		}
		if (config.thievingActivityIcon())
		{
			wantedCategories.add(MapIcons.THIEVING_ACTIVITY.getCategory());
		}
		if (config.transportationIcon())
		{
			wantedCategories.add(MapIcons.TRANSPORTATION.getCategory());
		}
		if (config.tripHammerIcon())
		{
			wantedCategories.add(MapIcons.TRIP_HAMMER.getCategory());
		}
		if (config.valeTotemIcon())
		{
			wantedCategories.add(MapIcons.VALE_TOTEM.getCategory());
		}
		if (config.waterSourceIcon())
		{
			wantedCategories.add(MapIcons.WATER_SOURCE.getCategory());
		}
		if (config.windmillIcon())
		{
			wantedCategories.add(MapIcons.WINDMILL.getCategory());
		}
		if (config.wineTraderIcon())
		{
			wantedCategories.add(MapIcons.WINE_TRADER.getCategory());
		}
		if (config.woodcuttingStumpIcon())
		{
			wantedCategories.add(MapIcons.WOODCUTTING_STUMP.getCategory());
		}
		if (config.woodcuttingTutorIcon())
		{
			wantedCategories.add(MapIcons.WOODCUTTING_TUTOR.getCategory());
		}


	}

	private void addTooltipsToWorldMap()
	{
		worldMapPointManager.removeIf(MapPoint.class::isInstance);
		if (!showTooltips)
		{
			return;
		}

		String tooltip;
		BufferedImage tooltipImage;
		int mapIconCategory;
		for (MapIcons icon : MapIcons.values())
		{
			tooltip = null;
			tooltipImage = BLANK_ICON;
			mapIconCategory = icon.getCategory();
			if (wantedCategories.contains(mapIconCategory))
			{
				for (WorldPoint mapIconLocation : icon.getLocation())
				{
					if (mapIconCategory == MapIcons.QUEST_START.getCategory())
					{
						if (QuestLocationLookup.locationsToQuests.containsKey(mapIconLocation))
						{
							tooltip = QuestLocationLookup.locationsToQuests.get(mapIconLocation).getName();
						}
						else
						{
							tooltip = "Quest not yet identified in 'World Map Extended' plugin." ;
							log.debug("Quest icon at location {} not yet included.", mapIconLocation);
						}
						tooltipImage = BLANK_QUEST_ICON;
					}

					else if (mapIconCategory == MapIcons.AGILITY_TRAINING.getCategory())
					{
						tooltip = worldMapAgilityCourseTooltips ? null : MapIcons.AGILITY_TRAINING.getDefaultTooltip();
					}
					else if (mapIconCategory == MapIcons.AGILITY_SHORTCUT.getCategory())
					{
						tooltip = worldMapAgilityShortcutTooltips ? null : MapIcons.AGILITY_SHORTCUT.getDefaultTooltip();
					}
					else if (mapIconCategory == MapIcons.DUNGEON.getCategory())
					{
						tooltip = worldMapDungeonTooltips ? null : MapIcons.DUNGEON.getDefaultTooltip();
					}
					else if (mapIconCategory == MapIcons.FARMING_PATCH.getCategory())
					{
						tooltip = worldMapFarmingTooltips ? null : MapIcons.FARMING_PATCH.getDefaultTooltip();
					}
					else if (mapIconCategory == MapIcons.FISHING_SPOT.getCategory())
					{
						tooltip = worldMapFishingTooltips ? null : MapIcons.FISHING_SPOT.getDefaultTooltip();
					}
					else if (mapIconCategory == MapIcons.HUNTER_TRAINING.getCategory())
					{
						tooltip = worldMapHunterTooltips ? null : MapIcons.HUNTER_TRAINING.getDefaultTooltip();
					}
					else if (mapIconCategory == MapIcons.MINIGAME.getCategory())
					{
						tooltip = worldMapMinigameTooltips ? null : MapIcons.MINIGAME.getDefaultTooltip();
					}
					else if (mapIconCategory == MapIcons.MINING_SITE.getCategory())
					{
						tooltip = worldMapMiningTooltips ? null : MapIcons.MINING_SITE.getDefaultTooltip();
					}
					else if (mapIconCategory == MapIcons.RARE_TREES.getCategory())
					{
						tooltip = worldMapRareTreesTooltips ? null : MapIcons.RARE_TREES.getDefaultTooltip();
					}
					else if (mapIconCategory == MapIcons.TRANSPORTATION.getCategory())
					{
						tooltip = worldMapTransportationTooltips ? null : MapIcons.TRANSPORTATION.getDefaultTooltip();
					}
					else
					{
						tooltip = icon.getDefaultTooltip();
					}

					WorldMapPoint customTooltipMapPoint = MapPoint.builder()
						.type(MapPoint.Type.DEFAULT)
						.worldPoint(mapIconLocation)
						.image(tooltipImage)
						.tooltip(tooltip)
						.build();

					worldMapPointManager.add(customTooltipMapPoint);
				}
			}
		}


	}

	private void getWhetherTooltipsShouldBeShown()
	{
		clientThread.invoke(() -> {
			showTooltips = (client.getVarbitValue(VarbitID.WORLDMAP_TOGGLES) & 0b1000) == 0;
		});
	}

	private void updateWorldMapIcons()
	{
		WorldMap worldMap = client.getWorldMap();
		if (worldMap == null)
		{
			return;
		}

		WorldMapRenderer wmm = worldMap.getWorldMapRenderer();
		if (!wmm.isLoaded())
		{
			return;
		}

		WorldMapRegion[][] regions = wmm.getMapRegions();
		for (WorldMapRegion[] worldMapRegions : regions)
		{
			for (WorldMapRegion region : worldMapRegions)
			{
				for (WorldMapIcon icon : region.getMapIcons())
				{
					MapElementConfig iconConfig = client.getMapElementConfig(icon.getType());
					SpritePixels iconSprite = iconConfig.getMapIcon(false); // Must be false otherwise nothing happens
					if (wantedCategories.contains(iconConfig.getCategory()))
					{
						iconSprite.setOffsetX(0);
						iconSprite.setOffsetY(0);
					}
					else
					{
						iconSprite.setOffsetX(OFFSET_TO_HIDE_ICON_SPRITES);
						iconSprite.setOffsetY(OFFSET_TO_HIDE_ICON_SPRITES);
					}
				}
			}
		}
	}

	private void resetWorldMapIcons()
	{
		worldMapPointManager.removeIf(MapPoint.class::isInstance);

		wantedCategories.clear();
		for (MapIcons iconType : MapIcons.values())
		{
			wantedCategories.add(iconType.getCategory());
		}
		updateWorldMapIcons();
	}

	private void checkRuneliteWorldMapClientPluginSettings()
	{
		String worldmapClientPluginGroupName = "worldmap" ;
		boolean worldMapClientPluginEnabled = false;

		for (Plugin p : pluginManager.getPlugins())
		{
			if (p.getName().equals(worldmapClientPluginGroupName))
			{
				worldMapClientPluginEnabled = pluginManager.isPluginEnabled(p);
				break;
			}
		}

		if (worldMapClientPluginEnabled)
		{
			worldMapTransportationTooltips = configManager.getConfiguration(worldmapClientPluginGroupName, "transportationTooltips").equals("true");
			worldMapAgilityCourseTooltips = configManager.getConfiguration(worldmapClientPluginGroupName, "agilityCourseTooltips").equals("true");
			worldMapAgilityShortcutTooltips = configManager.getConfiguration(worldmapClientPluginGroupName, "agilityShortcutTooltips").equals("true");
			worldMapMinigameTooltips = configManager.getConfiguration(worldmapClientPluginGroupName, "minigameTooltip").equals("true");
			worldMapFarmingTooltips = configManager.getConfiguration(worldmapClientPluginGroupName, "farmingpatchTooltips").equals("true");
			worldMapRareTreesTooltips = configManager.getConfiguration(worldmapClientPluginGroupName, "rareTreeTooltips").equals("true");
			worldMapMiningTooltips = configManager.getConfiguration(worldmapClientPluginGroupName, "miningSiteTooltips").equals("true");
			worldMapDungeonTooltips = configManager.getConfiguration(worldmapClientPluginGroupName, "dungeonTooltips").equals("true");
			worldMapHunterTooltips = configManager.getConfiguration(worldmapClientPluginGroupName, "hunterAreaTooltips").equals("true");
			worldMapFishingTooltips = configManager.getConfiguration(worldmapClientPluginGroupName, "fishingSpotTooltips").equals("true");
		}
		else
		{
			worldMapTransportationTooltips = false;
			worldMapAgilityCourseTooltips = false;
			worldMapAgilityShortcutTooltips = false;
			worldMapMinigameTooltips = false;
			worldMapFarmingTooltips = false;
			worldMapRareTreesTooltips = false;
			worldMapMiningTooltips = false;
			worldMapDungeonTooltips = false;
			worldMapHunterTooltips = false;
			worldMapFishingTooltips = false;
		}
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		checkObjects(event.getGroundObject(), event.getTile());
	}

	private void checkObjects(GroundObject obj, Tile tile)
	{
		if (obj == null || tile == null)
		{
			return;
		}

		int groundObjectID = obj.getId();
		if (QuestObjectLookup.objectsToQuests.containsKey(groundObjectID))
		{
			if (!config.questStartIcon())
			{
				tile.setGroundObject(null);
			}
		}
		else
		{
			switch (groundObjectID)
			{
				case ObjectID.AGILITY_SHORTCUT_ICON:
					if (!config.agilityShortcutsIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.ALTAR_ICON:
				case ObjectID.ZALCANO_ALTAR:
					if (!config.altarIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.BANK_STORE_ICON:
					if (!config.bankIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.CLAN_HUB_ICON:
					if (!config.clanHubIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.COMBAT_ACHIEVEMENTS_ICON:
					if (!config.combatAchievementsIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.DEATH_OFFICE_ICON:
					if (!config.deathsOfficeIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.DANDD_ICON:
					if (!config.distractionAndDiversionIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.DUNGEONENTRANCE_ICON_CLICKABLE:
				case ObjectID.MAPLINK_ICON: // Should be exclusive to the WorldMap but included just in case
				case ObjectID.DUNGEONENTRANCE_ICON:
					if (!config.dungeonIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.HAIRDRESSER_ICON:
					if (!config.hairdresserIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.HOLIDAY_EVENT_ICON:
					if (!config.holidayEventIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.POH_PORTAL_ICON:
					if (!config.housePortalIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.MAKEOVERMAGE_ICON:
					if (!config.makeoverMageIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.MINIGAME_START_ICON:
					if (!config.minigameIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.POLL_BOOTH_ICON:
					if (!config.pollBoothIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.RAID_ICON:
					if (!config.raidIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.TASK_ICON:
					if (!config.taskMasterIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.TRANSPORTATION_ICON:
				case ObjectID.TRANSPORTATION_ICON_AIP:
				case ObjectID.TRANSPORTATION_ICON_AIS:
				case ObjectID.TRANSPORTATION_ICON_AIR:
				case ObjectID.TRANSPORTATION_ICON_AIQ:
				case ObjectID.TRANSPORTATION_ICON_ALP:
				case ObjectID.TRANSPORTATION_ICON_ALS:
				case ObjectID.TRANSPORTATION_ICON_ALR:
				case ObjectID.TRANSPORTATION_ICON_AKP:
				case ObjectID.TRANSPORTATION_ICON_AKS:
				case ObjectID.TRANSPORTATION_ICON_AKR:
				case ObjectID.TRANSPORTATION_ICON_AKQ:
				case ObjectID.TRANSPORTATION_ICON_AJP:
				case ObjectID.TRANSPORTATION_ICON_AJS:
				case ObjectID.TRANSPORTATION_ICON_AJR:
				case ObjectID.TRANSPORTATION_ICON_AJQ:
				case ObjectID.TRANSPORTATION_ICON_DIP:
				case ObjectID.TRANSPORTATION_ICON_DIS:
				case ObjectID.TRANSPORTATION_ICON_DIR:
				case ObjectID.TRANSPORTATION_ICON_DIQ:
				case ObjectID.TRANSPORTATION_ICON_DLP:
				case ObjectID.TRANSPORTATION_ICON_DLS:
				case ObjectID.TRANSPORTATION_ICON_DLR:
				case ObjectID.TRANSPORTATION_ICON_DLQ:
				case ObjectID.TRANSPORTATION_ICON_DKP:
				case ObjectID.TRANSPORTATION_ICON_DKS:
				case ObjectID.TRANSPORTATION_ICON_DKR:
				case ObjectID.TRANSPORTATION_ICON_DKQ:
				case ObjectID.TRANSPORTATION_ICON_DJP:
				case ObjectID.TRANSPORTATION_ICON_DJS:
				case ObjectID.TRANSPORTATION_ICON_DJR:
				case ObjectID.TRANSPORTATION_ICON_DJQ:
				case ObjectID.TRANSPORTATION_ICON_CIP:
				case ObjectID.TRANSPORTATION_ICON_CIS:
				case ObjectID.TRANSPORTATION_ICON_CIR:
				case ObjectID.TRANSPORTATION_ICON_CIQ:
				case ObjectID.TRANSPORTATION_ICON_CLP:
				case ObjectID.TRANSPORTATION_ICON_CLS:
				case ObjectID.TRANSPORTATION_ICON_CLR:
				case ObjectID.TRANSPORTATION_ICON_CLQ:
				case ObjectID.TRANSPORTATION_ICON_CKP:
				case ObjectID.TRANSPORTATION_ICON_CKS:
				case ObjectID.TRANSPORTATION_ICON_CKR:
				case ObjectID.TRANSPORTATION_ICON_CKQ:
				case ObjectID.TRANSPORTATION_ICON_CJP:
				case ObjectID.TRANSPORTATION_ICON_CJS:
				case ObjectID.TRANSPORTATION_ICON_CJR:
				case ObjectID.TRANSPORTATION_ICON_CJQ:
				case ObjectID.TRANSPORTATION_ICON_BIP:
				case ObjectID.TRANSPORTATION_ICON_BIS:
				case ObjectID.TRANSPORTATION_ICON_BIR:
				case ObjectID.TRANSPORTATION_ICON_BIQ:
				case ObjectID.TRANSPORTATION_ICON_BLP:
				case ObjectID.TRANSPORTATION_ICON_BLS:
				case ObjectID.TRANSPORTATION_ICON_BLR:
				case ObjectID.TRANSPORTATION_ICON_BLQ:
				case ObjectID.TRANSPORTATION_ICON_BKP:
				case ObjectID.TRANSPORTATION_ICON_BKS:
				case ObjectID.TRANSPORTATION_ICON_BKR:
				case ObjectID.TRANSPORTATION_ICON_BKQ:
				case ObjectID.TRANSPORTATION_ICON_BJP:
				case ObjectID.TRANSPORTATION_ICON_BJS:
				case ObjectID.TRANSPORTATION_ICON_BJR:
				case ObjectID.TRANSPORTATION_ICON_BJQ:
					if (!config.transportationIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.TUTOR_BANK_ICON:
					if (!config.bankTutorIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.TUTOR_BOND_ICON:
					if (!config.bondTutorIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.CLUESCROLL_TUTOR_ICON:
					if (!config.clueTutorIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.TUTOR_COMBAT_ICON:
					if (!config.combatTutorIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.TUTOR_COOKING_ICON:
					if (!config.cookingTutorIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.TUTOR_CRAFTING_ICON:
					if (!config.craftingTutorIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.TUTOR_DANGER_ICON:
					if (!config.dangerTutorIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.TUTOR_DEADMAN_ICON:
					if (!config.deadmanTutorIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.TUTOR_FISHING_ICON:
					if (!config.fishingTutorIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.TUTOR_HUNTER_ICON:
					if (!config.hunterTutorIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.TUTOR_IRONMAN_ICON:
					if (!config.ironmanTutorIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.LEAGUE_TUTOR_ICON:
					if (!config.leaguesTutorIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.TUTOR_MAIN_ICON:
					if (!config.lumbridgeGuideIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.TUTOR_MINING_ICON:
					if (!config.miningTutorIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.TUTOR_PRAYER_ICON:
					if (!config.prayerTutorIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.TUTOR_SECURITY_ICON:
					if (!config.securityTutorIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.TUTOR_SMITHING_ICON:
					if (!config.smithingTutorIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.TUTOR_WOODCUTTING_ICON:
					if (!config.woodcuttingTutorIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.AGILITY_TRAINING_ICON:
					if (!config.agilityCoursesIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.ANVIL_ICON:
					if (!config.anvilIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.BIRD_HOUSE_ICON:
					if (!config.birdHouseIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.BREWING_ICON:
					if (!config.breweryIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.COMBATTRAINING_STORE_ICON:
					if (!config.combatTrainingIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.RANGE_ICON_KITCHEN:
				case ObjectID.RANGE_ICON:
					if (!config.cookingRangeIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.MILK_CHURN_ICON:
					if (!config.dairyChurnIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.DAIRY_COW_ICON:
					if (!config.dairyCowIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.FARMING_PATCH_ICON:
					if (!config.farmingPatchIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.FISHING_POINT_ICON:
					if (!config.fishingSpotIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.FURNACE_ICON:
					if (!config.furnaceIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.GIANTS_FOUNDRY_TOOL_GRINDSTONE:
					if (!config.grindstoneIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.HUNTING_AREA_ICON:
					if (!config.hunterTrainingIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.LOOM_ICON:
					if (!config.loomIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.MINING_SITE_ICON:
					if (!config.miningSiteIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.GIANTS_FOUNDRY_TOOL_POLISHING_WHEEL:
					if (!config.polishingWheelIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.POTTERY_ICON:
					if (!config.potteryWheelIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.RARE_TREES_ICON:
					if (!config.rareTreesIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.SANDPIT_ICON:
					if (!config.sandpitIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.SINGING_BOWL_ICON:
					if (!config.singingBowlIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.SLAYER_MASTER_ICON:
					if (!config.slayerMasterIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.SPINNINGWHEEL_ICON:
					if (!config.spinningWheelIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.STAGNENT_WATER_ICON:
					if (!config.stagnantWaterSourceIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.THIEVING_ICON:
					if (!config.thievingActivityIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.GIANTS_FOUNDRY_TOOL_TRIP_HAMMER:
					if (!config.tripHammerIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.V3_TOTEM_ICON:
					if (!config.valeTotemIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.WATER_SOURCE_ICON_KITCHEN:
				case ObjectID.WATER_SOURCE_ICON:
					if (!config.waterSourceIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.FLOUR_MILL_ICON:
					if (!config.windmillIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.FRIS_TREESTUMP_ICON:
					if (!config.woodcuttingStumpIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.AMULET_STORE_ICON:
					if (!config.amuletshopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.POTIONS_STORE_ICON:
					if (!config.apothecaryIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.ARCHERY_STORE_ICON:
					if (!config.archeryshopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.AXE_STORE_ICON:
					if (!config.axeshopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.BH_ICON:
					if (!config.bountyHunterIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.CANDLE_STORE_ICON:
					if (!config.candleShopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.CHAIN_SHOP_ICON:
					if (!config.chainbodyShopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.CLOTHING_STORE_ICON:
					if (!config.clothesShopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.CRAFTING_STORE_ICON:
					if (!config.craftingShopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.DYE_TRADER_ICON:
					if (!config.dyeTraderIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.POH_ESTATEAGENT_ICON:
					if (!config.estateAgentIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.FARM_SHOP_ICON:
					if (!config.farmingShopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.FISHING_STORE_ICON:
					if (!config.fishingShopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.COOKING_STORE_ICON:
				case ObjectID.FOOD_STORE_ICON:
				case ObjectID.KEBAB_STORE_ICON:
				case ObjectID.VEG_STORE_ICON:
					if (!config.foodShopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.FORESTRY_SHOP_ICON:
					if (!config.forestryShopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.FUR_STORE_ICON:
					if (!config.furTraderIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.GARDEN_SUPPLIER_ICON:
					if (!config.gardenSupplierIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.GEM_STORE_ICON:
					if (!config.gemShopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.GENERAL_STORE_ICON:
					if (!config.generalStoreIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.GE_MAPICON_EXCHANGE:
				case ObjectID.GE_MAPICON_RUNES:
				case ObjectID.GE_MAPICON_HERBS:
				case ObjectID.GE_MAPICON_LOGS:
				case ObjectID.GE_MAPICON_ORES:
				case ObjectID.GE_MAPICON_COMBAT:
					if (!config.grandExchangeIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.HELMET_STORE_ICON:
					if (!config.helmetShopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.HERBALIST_STORE_ICON:
					if (!config.herbalistIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.HOLIDAY_SHOP_ICON:
					if (!config.holidayItemTraderIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.HUNTING_SHOP_ICON:
					if (!config.hunterShopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.JEWLERY_STORE_ICON:
					if (!config.jewelleryShopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.QUEST_SHOP_ICON:
					if (!config.junkCheckerIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.MACE_STORE_ICON:
					if (!config.maceShopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.MAGIC_STORE_ICON:
					if (!config.magicShopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.MINING_SHOP_ICON:
					if (!config.miningShopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.NEWSPAPER_TRADER_ICON:
					if (!config.newspaperTraderIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.PET_ICON:
					if (!config.petShopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.PLATE_STORE_ICON:
					if (!config.platebodyShopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.PLATELEGS_STORE_ICON:
					if (!config.platelegsShopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.SKIRTS_STORE_ICON:
					if (!config.plateskirtShopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.BEER_STORE_ICON:
					if (!config.pubIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.ROPE_TRADER_ICON:
					if (!config.ropeTraderIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.POH_SAWMILL_ICON:
					if (!config.sawmillIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.SCIMITAR_STORE_ICON:
					if (!config.scimitarShopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.SHIELD_STORE_ICON:
					if (!config.shieldShopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.SILK_STORE_ICON:
					if (!config.silkTraderIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.SILVER_STORE_ICON:
					if (!config.silverShopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.SPEEDRUNNING_ICON:
					if (!config.speedrunningShopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.SPICE_STORE_ICON:
					if (!config.spiceShopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.STAFF_STORE_ICON:
					if (!config.staffShopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.POH_STONEMASON_ICON:
					if (!config.stonemasonIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.SWORD_STORE_ICON:
					if (!config.swordShopIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.TANNER_STORE_ICON:
					if (!config.tanneryIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.TAXIDERMIST_ICON:
					if (!config.taxidermistIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.TEA_SELLER_ICON:
					if (!config.teaTraderIcon())
					{
						tile.setGroundObject(null);
					}
					break;
				case ObjectID.WINE_TRADER_ICON:
					if (!config.wineTraderIcon())
					{
						tile.setGroundObject(null);
					}
					break;
			}
		}
	}

	private void refreshSceneGroundObjects()
	{
		final Scene scene = client.getScene();
		final Tile[][][] tiles = scene.getTiles();

		if (tiles != null)
		{
			for (Tile[][] tile : tiles)
			{
				for (Tile[] value : tile)
				{
					for (final Tile currentTile : value)
					{
						if (currentTile != null)
						{
							checkObjects(currentTile.getGroundObject(), currentTile);
						}
					}
				}
			}
		}

		// Ground Object updates but MiniMap doesn't refresh unless this is called
		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				client.setGameState(GameState.LOADING);
			}
		});
	}

	@Provides
	BetterMapsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BetterMapsConfig.class);
	}
}
