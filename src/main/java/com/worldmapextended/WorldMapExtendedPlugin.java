package com.worldmapextended;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptID;
import net.runelite.api.SpritePixels;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.VarbitChanged;
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
 * It is not yet possible to properly hide the world map icons, and a pull request has been open for > 5 years to modify
 * the runelite 'World Map' client plugin (https://github.com/runelite/runelite/pull/11599) to add this functionality to
 * the client plugin. However, with the current API, the icon sprites are able to have their offset positions defined,
 * giving the illusion of them being hidden. This plugin has therefore been generated to provide a middle-ground solution
 * between both situations and work in conjunction with the existing 'World Map' client plugin. As there is naturally
 * some level of overlap between the 'World Map' client plugin and this version, some segments of code have been re-used
 * to more effectively represent the customisations available within the client. This plugin therefore initially and
 * directly recognises the coding contributions made by a range of individuals for the client plugin.
 *
 * Moving the sprites leaves the icon in place therefore the default tooltips would still be visible when hovering over
 * their normal locations (this remains also true with the 'World Map' plugin tooltips, and is part of the reason for
 * some duplication). To alleviate this issue when considering the default tooltips, the standard tooltip generation is
 * disabled and custom tooltips are required for each icon type.
 */

@Slf4j
@PluginDescriptor(
	name = "World Map Extended",
	description = "A more customisable world map with the ability to hide icons, to be used in conjunction with the existing client plugin.",
	tags = {"worldmap", "map", "icon", "hide", "clean"}
)
public class WorldMapExtendedPlugin extends Plugin
{
	static final String CONFIG_GROUP = "worldMapExtended";

	private static final int HOLIDAY_EVENT_ICON_CATEGORY = 1119;
	private static final int OFFSET_TO_HIDE_ICON_SPRITES = 25000;
	private static final int WORLDMAP_ELEMENTS_MARKER_SCRIPT_ID = 1757;
	private static final int WORLDMAP_ELEMENTS_TOOLTIP_SCRIPT_ID = 1847;

	/**
	 * Code copied and modified from Runelite worldmap client plugin, used to generate blank icons to hold the tooltips.
	 */
	static final BufferedImage BLANK_ICON;
	static final BufferedImage BLANK_QUEST_ICON;
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

	@Inject	private Client client;
	@Inject	private ClientThread clientThread;
	@Inject	private ConfigManager configManager;
	@Inject	private PluginManager pluginManager;
	@Inject	private WorldMapExtendedConfig config;
	@Inject	private WorldMapPointManager worldMapPointManager;

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
			checkRuneliteWorldMapClientPluginSettings();
			addTooltipsToWorldMap();
			updateWorldMapIcons();
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		resetWorldMapIcons();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals(CONFIG_GROUP))
		{
			checkConfigForClashes();
			addTooltipsToWorldMap();
			updateWorldMapIcons();
		}
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired scriptPreFired) {
		if (scriptPreFired.getScriptId() == WORLDMAP_ELEMENTS_TOOLTIP_SCRIPT_ID) {
			client.setVarcIntValue(VarClientID.TOOLTIP_BUILT,1);
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
		wantedCategories.add(MapIcons.MAP_LINK.getCategory());

		if(config.agilityCoursesIcon())
		{
			wantedCategories.add(MapIcons.AGILITY_TRAINING.getCategory());
		}
		if(config.agilityShortcutsIcon())
		{
			wantedCategories.add(MapIcons.AGILITY_SHORTCUT.getCategory());
		}
		if(config.altarIcon())
		{
			wantedCategories.add(MapIcons.ALTAR.getCategory());
		}
		if(config.amuletshopIcon())
		{
			wantedCategories.add(MapIcons.AMULET_SHOP.getCategory());
		}
		if(config.anvilIcon())
		{
			wantedCategories.add(MapIcons.ANVIL.getCategory());
		}
		if(config.apothecaryIcon())
		{
			wantedCategories.add(MapIcons.APOTHECARY.getCategory());
		}
		if(config.archeryshopIcon())
		{
			wantedCategories.add(MapIcons.ARCHERY_SHOP.getCategory());
		}
		if(config.axeshopIcon())
		{
			wantedCategories.add(MapIcons.AXE_SHOP.getCategory());
		}
		if(config.bankIcon())
		{
			wantedCategories.add(MapIcons.BANK.getCategory());
		}
		if(config.bankTutorIcon())
		{
			wantedCategories.add(MapIcons.BANK_TUTOR.getCategory());
		}
		if(config.birdHouseIcon())
		{
			wantedCategories.add(MapIcons.BIRD_HOUSE_SITE.getCategory());
		}
		if(config.bondTutorIcon())
		{
			wantedCategories.add(MapIcons.BOND_TUTOR.getCategory());
		}
		if(config.bountyHunterIcon())
		{
			wantedCategories.add(MapIcons.BOUNTY_HUNTER_TRADER.getCategory());
		}
		if(config.breweryIcon())
		{
			wantedCategories.add(MapIcons.BREWERY.getCategory());
		}
		if(config.candleShopIcon())
		{
			wantedCategories.add(MapIcons.CANDLE_SHOP.getCategory());
		}
		if(config.chainbodyShopIcon())
		{
			wantedCategories.add(MapIcons.CHAINMAIL_SHOP.getCategory());
		}
		if(config.clanHubIcon())
		{
			wantedCategories.add(MapIcons.CLAN_HUB.getCategory());
		}
		if(config.clothesShopIcon())
		{
			wantedCategories.add(MapIcons.CLOTHES_SHOP.getCategory());
		}
		if(config.clueTutorIcon())
		{
			wantedCategories.add(MapIcons.CLUE_TUTOR.getCategory());
		}
		if(config.combatAchievementsIcon())
		{
			wantedCategories.add(MapIcons.COMBAT_ACHIEVEMENTS.getCategory());
		}
		if(config.combatTrainingIcon())
		{
			wantedCategories.add(MapIcons.COMBAT_TRAINING.getCategory());
		}
		if(config.combatTutorIcon())
		{
			wantedCategories.add(MapIcons.COMBAT_TUTOR.getCategory());
		}
		if(config.cookingRangeIcon())
		{
			wantedCategories.add(MapIcons.COOKING_RANGE.getCategory());
		}
		if(config.cookingTutorIcon())
		{
			wantedCategories.add(MapIcons.COOKING_TUTOR.getCategory());
		}
		if(config.craftingShopIcon())
		{
			wantedCategories.add(MapIcons.CRAFTING_SHOP.getCategory());
		}
		if(config.craftingTutorIcon())
		{
			wantedCategories.add(MapIcons.CRAFTING_TUTOR.getCategory());
		}
		if(config.dairyChurnIcon())
		{
			wantedCategories.add(MapIcons.DAIRY_CHURN.getCategory());
		}
		if(config.dairyCowIcon())
		{
			wantedCategories.add(MapIcons.DAIRY_COW.getCategory());
		}
		if(config.dangerTutorIcon())
		{
			wantedCategories.add(MapIcons.DANGER_TUTOR.getCategory());
		}
		if(config.deadmanTutorIcon())
		{
			wantedCategories.add(MapIcons.DEADMAN_TUTOR.getCategory());
		}
		if(config.deathsOfficeIcon())
		{
			wantedCategories.add(MapIcons.DEATHS_OFFICE.getCategory());
		}
		if(config.distractionAndDiversionIcon())
		{
			wantedCategories.add(MapIcons.DISTRACTION_AND_DIVERSION.getCategory());
		}
		if(config.dungeonIcon())
		{
			wantedCategories.add(MapIcons.DUNGEON.getCategory());
		}
		if(config.dyeTraderIcon())
		{
			wantedCategories.add(MapIcons.DYE_TRADER.getCategory());
		}
		if(config.estateAgentIcon())
		{
			wantedCategories.add(MapIcons.ESTATE_AGENT.getCategory());
		}
		if(config.farmingPatchIcon())
		{
			wantedCategories.add(MapIcons.FARMING_PATCH.getCategory());
		}
		if(config.farmingShopIcon())
		{
			wantedCategories.add(MapIcons.FARMING_SHOP.getCategory());
		}
		if(config.fishingShopIcon())
		{
			wantedCategories.add(MapIcons.FISHING_SHOP.getCategory());
		}
		if(config.fishingSpotIcon())
		{
			wantedCategories.add(MapIcons.FISHING_SPOT.getCategory());
		}
		if(config.fishingTutorIcon())
		{
			wantedCategories.add(MapIcons.FISHING_TUTOR.getCategory());
		}
		if(config.foodShopIcon())
		{
			wantedCategories.add(MapIcons.FOOD_SHOP.getCategory());
		}
		if(config.forestryShopIcon())
		{
			wantedCategories.add(MapIcons.FORESTRY_SHOP.getCategory());
		}
		if(config.furTraderIcon())
		{
			wantedCategories.add(MapIcons.FUR_TRADER.getCategory());
		}
		if(config.furnaceIcon())
		{
			wantedCategories.add(MapIcons.FURNACE.getCategory());
		}
		if(config.gardenSupplierIcon())
		{
			wantedCategories.add(MapIcons.GARDEN_SUPPLIER.getCategory());
		}
		if(config.gemShopIcon())
		{
			wantedCategories.add(MapIcons.GEM_SHOP.getCategory());
		}
		if(config.generalStoreIcon())
		{
			wantedCategories.add(MapIcons.GENERAL_STORE.getCategory());
		}
		if(config.grandExchangeIcon())
		{
			wantedCategories.add(MapIcons.GRAND_EXCHANGE.getCategory());
		}
		if(config.grindstoneIcon())
		{
			wantedCategories.add(MapIcons.GRINDSTONE.getCategory());
		}
		if(config.hairdresserIcon())
		{
			wantedCategories.add(MapIcons.HAIRDRESSER.getCategory());
		}
		if(config.helmetShopIcon())
		{
			wantedCategories.add(MapIcons.HELMET_SHOP.getCategory());
		}
		if(config.herbalistIcon())
		{
			wantedCategories.add(MapIcons.HERBALIST.getCategory());
		}
		if(config.holidayEventIcon())
		{
			wantedCategories.add(HOLIDAY_EVENT_ICON_CATEGORY); // treated differently to avoid needing coordinates
		}
		if(config.holidayItemTraderIcon())
		{
			wantedCategories.add(MapIcons.HOLIDAY_ITEM_TRADER.getCategory());
		}
		if(config.housePortalIcon())
		{
			wantedCategories.add(MapIcons.HOUSE_PORTAL.getCategory());
		}
		if(config.hunterShopIcon())
		{
			wantedCategories.add(MapIcons.HUNTER_SHOP.getCategory());
		}
		if(config.hunterTrainingIcon())
		{
			wantedCategories.add(MapIcons.HUNTER_TRAINING.getCategory());
		}
		if(config.hunterTutorIcon())
		{
			wantedCategories.add(MapIcons.HUNTER_TUTOR.getCategory());
		}
		if(config.ironmanTutorIcon())
		{
			wantedCategories.add(MapIcons.IRONMAN_TUTOR.getCategory());
		}
		if(config.jewelleryShopIcon())
		{
			wantedCategories.add(MapIcons.JEWELLERY_SHOP.getCategory());
		}
		if(config.junkCheckerIcon())
		{
			wantedCategories.add(MapIcons.JUNK_CHECKER.getCategory());
		}
		if(config.leaguesTutorIcon())
		{
			wantedCategories.add(MapIcons.LEAGUES_TUTOR.getCategory());
		}
		if(config.loomIcon())
		{
			wantedCategories.add(MapIcons.LOOM.getCategory());
		}
		if(config.lumbridgeGuideIcon())
		{
			wantedCategories.add(MapIcons.LUMBRIDGE_GUIDE.getCategory());
		}
		if(config.maceShopIcon())
		{
			wantedCategories.add(MapIcons.MACE_SHOP.getCategory());
		}
		if(config.magicShopIcon())
		{
			wantedCategories.add(MapIcons.MAGIC_SHOP.getCategory());
		}
		if(config.makeoverMageIcon())
		{
			wantedCategories.add(MapIcons.MAKEOVER_MAGE.getCategory());
		}
		if(config.minigameIcon())
		{
			wantedCategories.add(MapIcons.MINIGAME.getCategory());
		}
		if(config.miningShopIcon())
		{
			wantedCategories.add(MapIcons.MINING_SHOP.getCategory());
		}
		if(config.miningSiteIcon())
		{
			wantedCategories.add(MapIcons.MINING_SITE.getCategory());
		}
		if(config.miningTutorIcon())
		{
			wantedCategories.add(MapIcons.MINING_TUTOR.getCategory());
		}
		if(config.newspaperTraderIcon())
		{
			wantedCategories.add(MapIcons.NEWSPAPER_TRADER.getCategory());
		}
		if(config.petShopIcon())
		{
			wantedCategories.add(MapIcons.PET_SHOP.getCategory());
		}
		if(config.platebodyShopIcon())
		{
			wantedCategories.add(MapIcons.PLATEBODY_SHOP.getCategory());
		}
		if(config.platelegsShopIcon())
		{
			wantedCategories.add(MapIcons.PLATELEGS_SHOP.getCategory());
		}
		if(config.plateskirtShopIcon())
		{
			wantedCategories.add(MapIcons.PLATESKIRT_SHOP.getCategory());
		}
		if(config.polishingWheelIcon())
		{
			wantedCategories.add(MapIcons.POLISHING_WHEEL.getCategory());
		}
		if(config.pollBoothIcon())
		{
			wantedCategories.add(MapIcons.POLL_BOOTH.getCategory());
		}
		if(config.potteryWheelIcon())
		{
			wantedCategories.add(MapIcons.POTTERY_WHEEL.getCategory());
		}
		if(config.prayerTutorIcon())
		{
			wantedCategories.add(MapIcons.PRAYER_TUTOR.getCategory());
		}
		if(config.pricingExpertIcon())
		{
			wantedCategories.add(MapIcons.PRICING_EXPERT.getCategory());
		}
		if(config.pubIcon())
		{
			wantedCategories.add(MapIcons.PUB.getCategory());
		}
		if(config.questStartIcon())
		{
			wantedCategories.add(MapIcons.QUEST_START.getCategory());
		}
		if(config.raidIcon())
		{
			wantedCategories.add(MapIcons.RAID.getCategory());
		}
		if(config.rareTreesIcon())
		{
			wantedCategories.add(MapIcons.RARE_TREES.getCategory());
		}
		if(config.ropeTraderIcon())
		{
			wantedCategories.add(MapIcons.ROPE_TRADER.getCategory());
		}
		if(config.sandpitIcon())
		{
			wantedCategories.add(MapIcons.SANDPIT.getCategory());
		}
		if(config.sawmillIcon())
		{
			wantedCategories.add(MapIcons.SAWMILL.getCategory());
		}
		if(config.scimitarShopIcon())
		{
			wantedCategories.add(MapIcons.SCIMITAR_SHOP.getCategory());
		}
		if(config.securityTutorIcon())
		{
			wantedCategories.add(MapIcons.SECURITY_TUTOR.getCategory());
		}
		if(config.shieldShopIcon())
		{
			wantedCategories.add(MapIcons.SHIELD_SHOP.getCategory());
		}
		if(config.silkTraderIcon())
		{
			wantedCategories.add(MapIcons.SILK_TRADER.getCategory());
		}
		if(config.silverShopIcon())
		{
			wantedCategories.add(MapIcons.SILVER_SHOP.getCategory());
		}
		if(config.singingBowlIcon())
		{
			wantedCategories.add(MapIcons.SINGING_BOWL.getCategory());
		}
		if(config.slayerMasterIcon())
		{
			wantedCategories.add(MapIcons.SLAYER_MASTER.getCategory());
		}
		if(config.smithingTutorIcon())
		{
			wantedCategories.add(MapIcons.SMITHING_TUTOR.getCategory());
		}
		if(config.speedrunningShopIcon())
		{
			wantedCategories.add(MapIcons.SPEEDRUNNING_SHOP.getCategory());
		}
		if(config.spiceShopIcon())
		{
			wantedCategories.add(MapIcons.SPICE_SHOP.getCategory());
		}
		if(config.spinningWheelIcon())
		{
			wantedCategories.add(MapIcons.SPINNING_WHEEL.getCategory());
		}
		if(config.staffShopIcon())
		{
			wantedCategories.add(MapIcons.STAFF_SHOP.getCategory());
		}
		if(config.stagnantWaterSourceIcon())
		{
			wantedCategories.add(MapIcons.STAGNANT_WATER_SOURCE.getCategory());
		}
		if(config.stonemasonIcon())
		{
			wantedCategories.add(MapIcons.STONEMASON.getCategory());
		}
		if(config.swordShopIcon())
		{
			wantedCategories.add(MapIcons.SWORD_SHOP.getCategory());
		}
		if(config.tanneryIcon())
		{
			wantedCategories.add(MapIcons.TANNERY.getCategory());
		}
		if(config.taskMasterIcon())
		{
			wantedCategories.add(MapIcons.TASK_MASTER.getCategory());
		}
		if(config.taxidermistIcon())
		{
			wantedCategories.add(MapIcons.TAXIDERMIST.getCategory());
		}
		if(config.teaTraderIcon())
		{
			wantedCategories.add(MapIcons.TEA_TRADER.getCategory());
		}
		if(config.thievingActivityIcon())
		{
			wantedCategories.add(MapIcons.THIEVING_ACTIVITY.getCategory());
		}
		if(config.transportationIcon())
		{
			wantedCategories.add(MapIcons.TRANSPORTATION.getCategory());
		}
		if(config.tripHammerIcon())
		{
			wantedCategories.add(MapIcons.TRIP_HAMMER.getCategory());
		}
		if(config.valeTotemIcon())
		{
			wantedCategories.add(MapIcons.VALE_TOTEM.getCategory());
		}
		if(config.waterSourceIcon())
		{
			wantedCategories.add(MapIcons.WATER_SOURCE.getCategory());
		}
		if(config.windmillIcon())
		{
			wantedCategories.add(MapIcons.WINDMILL.getCategory());
		}
		if(config.wineTraderIcon())
		{
			wantedCategories.add(MapIcons.WINE_TRADER.getCategory());
		}
		if(config.woodcuttingStumpIcon())
		{
			wantedCategories.add(MapIcons.WOODCUTTING_STUMP.getCategory());
		}
		if(config.woodcuttingTutorIcon())
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
							tooltip = null;
							log.debug("Quest icon at location {} not yet included", mapIconLocation);
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
						for (MapIcons iconCategory : MapIcons.values())
						{
							if (mapIconCategory == iconCategory.getCategory())
							{
								tooltip = iconCategory.getDefaultTooltip();
							}
						}
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
		String worldmapClientPluginGroupName = "worldmap";
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
			worldMapTransportationTooltips = configManager.getConfiguration(worldmapClientPluginGroupName,"transportationTooltips").equals("true");
			worldMapAgilityCourseTooltips = configManager.getConfiguration(worldmapClientPluginGroupName,"agilityCourseTooltips").equals("true");
			worldMapAgilityShortcutTooltips = configManager.getConfiguration(worldmapClientPluginGroupName,"agilityShortcutTooltips").equals("true");
			worldMapMinigameTooltips = configManager.getConfiguration(worldmapClientPluginGroupName,"minigameTooltip").equals("true");
			worldMapFarmingTooltips = configManager.getConfiguration(worldmapClientPluginGroupName,"farmingpatchTooltips").equals("true");
			worldMapRareTreesTooltips = configManager.getConfiguration(worldmapClientPluginGroupName,"rareTreeTooltips").equals("true");
			worldMapMiningTooltips = configManager.getConfiguration(worldmapClientPluginGroupName,"miningSiteTooltips").equals("true");
			worldMapDungeonTooltips = configManager.getConfiguration(worldmapClientPluginGroupName,"dungeonTooltips").equals("true");
			worldMapHunterTooltips = configManager.getConfiguration(worldmapClientPluginGroupName,"hunterAreaTooltips").equals("true");
			worldMapFishingTooltips = configManager.getConfiguration(worldmapClientPluginGroupName,"fishingSpotTooltips").equals("true");
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

	@Provides
	WorldMapExtendedConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WorldMapExtendedConfig.class);
	}
}
