package com.extendedWorldmap;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.SpritePixels;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
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
 * the runelite 'World Map' client plugin (https://github.com/runelite/runelite/pull/11599). However, with the current
 * API, the icon sprites can be offset giving the illusion of them being hidden, therefore this plugin has been generated
 * to provide a middle-ground solution and work in conjunction with the existing 'World Map' client plugin. While there
 * could be a lot more features added to this plugin (hide agility shortcuts the player cannot use etc), it would either
 * need changes to the existing 'World Map' plugin or duplicate several of the features.
 *
 * Moving the sprites leaves the icon in place therefore the default tooltips would still be visible when hovering over
 * their normal locations (this is also true with the 'World Map' plugin tooltips). To alleviate this issue, the default
 * tooltip generation is disabled and custom tooltips are required for every icon type. Where specified, this plugin will
 * not override the tooltips of the client plugin (if those are preferred).
 */

@Slf4j
@PluginDescriptor(
	name = "World Map Extended",
	description = "A customisable Worldmap with ability to hide icons, to be used in conjunction with the existing 'World Map' plugin.",
	tags = {"worldmap", "map", "icon", "hide", "clean"}
)
public class WorldmapExtendedPlugin extends Plugin
{
	static final String CONFIG_GROUP = "extendedWorldmap";

	private static final int OFFSET_TO_HIDE_ICON_SPRITES = 25000;
	private static final int WORLDMAP_ELEMENTS_MARKER_SCRIPT_ID = 1757;
	private static final int WORLDMAP_ELEMENTS_TOOLTIP_SCRIPT_ID = 1847;

	/**
	 * Code copied from Runelite worldmap client plugin, used to generate blank icons to hold the tooltips.
	 */
	static final BufferedImage BLANK_ICON;
	static final BufferedImage BLANK_QUEST_ICON;
	static
	{
		// Original size of world map icons
		final int worldMapIconSize = 15;
		//A size of 17 gives us a buffer when triggering tooltips
		final int iconOffset = 1;
		final int iconBufferSize = worldMapIconSize + iconOffset * 2;
		// Quest icons are a bit bigger than regular icons
		// A size of 25 aligns the quest icons when converting the world map point to pixel coordinates
		// The new quest icons must be offset by 5, for a size of 25, to align when drawing on top of the original icon
		final int questIconOffset = 5;
		final int questIconBufferSize = worldMapIconSize + questIconOffset * 2;

		BLANK_ICON = new BufferedImage(iconBufferSize, iconBufferSize, BufferedImage.TYPE_INT_ARGB);
		BLANK_QUEST_ICON = new BufferedImage(questIconBufferSize, questIconBufferSize, BufferedImage.TYPE_INT_ARGB);
	}

	@Inject	private Client client;
	@Inject	private ClientThread clientThread;
	@Inject	private ConfigManager configManager;
	@Inject	private PluginManager pluginManager;
	@Inject	private WorldmapExtendedConfig config;
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
			checkRuneliteWorldMapClientPluginSettings();
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
			checkRuneliteWorldMapClientPluginSettings();
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
		if (scriptPostFired.getScriptId() == WORLDMAP_ELEMENTS_MARKER_SCRIPT_ID)
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
			showTooltips = getWhetherTooltipsShouldBeShown();
		}
	}

	private boolean getWhetherTooltipsShouldBeShown()
	{
		Widget worldmapToggles = client.getWidget(InterfaceID.Worldmap.TOGGLES);
		if (worldmapToggles != null)
		{
			Widget showTooltipsCrossed = worldmapToggles.getDynamicChildren()[22];
			if (showTooltipsCrossed != null)
			{
				return showTooltipsCrossed.isSelfHidden();
			}
		}
		return true;
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

		checkRuneliteWorldMapClientPluginSettings();
		showTooltips = getWhetherTooltipsShouldBeShown();
		worldMapPointManager.removeIf(MapPoint.class::isInstance);

		WorldMapRegion[][] regions = wmm.getMapRegions();
		String tooltip;
		for (WorldMapRegion[] worldMapRegions : regions)
		{
			for (WorldMapRegion region : worldMapRegions)
			{
				for (WorldMapIcon icon : region.getMapIcons())
				{
					MapElementConfig iconConfig = client.getMapElementConfig(icon.getType());
					WorldPoint iconPosition = icon.getCoordinate().dx(-1);

					switch (iconConfig.getCategory())
					{
						case MapIcons.Categories.AGILITY_COURSES:
							tooltip = showTooltips ? (worldMapAgilityCourseTooltips ? null : MapIcons.Tooltips.AGILITY_COURSES) : null;
							moveIcon(config.agilityCoursesIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.AGILITY_SHORTCUT:
							tooltip = showTooltips ? (worldMapAgilityShortcutTooltips ? null : MapIcons.Tooltips.AGILITY_SHORTCUT) : null;
							moveIcon(config.agilityShortcutsIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.ALTAR:
							tooltip = showTooltips ? MapIcons.Tooltips.ALTAR : null;
							moveIcon(config.altarIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.AMULET_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.AMULET_SHOP : null;
							moveIcon(config.amuletshopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.ANVIL:
							tooltip = showTooltips ? MapIcons.Tooltips.ANVIL : null;
							moveIcon(config.anvilIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.APOTHECARY:
							tooltip = showTooltips ? MapIcons.Tooltips.APOTHECARY : null;
							moveIcon(config.apothecaryIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.ARCHERY_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.ARCHERY_SHOP : null;
							moveIcon(config.archeryshopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.AXE_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.AXE_SHOP : null;
							moveIcon(config.axeshopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.BANK:
							tooltip = showTooltips ? MapIcons.Tooltips.BANK : null;
							moveIcon(config.bankIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.BANK_TUTOR:
							tooltip = showTooltips ? MapIcons.Tooltips.BANK_TUTOR : null;
							moveIcon(config.bankTutorIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.BIRD_HOUSE_SITE:
							tooltip = showTooltips ? MapIcons.Tooltips.BIRD_HOUSE_SITE : null;
							moveIcon(config.birdHouseIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.BOND_TUTOR:
							tooltip = showTooltips ? MapIcons.Tooltips.BOND_TUTOR : null;
							moveIcon(config.bondTutorIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.BOUNTY_HUNTER_TRADER:
							tooltip = showTooltips ? MapIcons.Tooltips.BOUNTY_HUNTER_TRADER : null;
							moveIcon(config.bountyHunterIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.BREWERY:
							tooltip = showTooltips ? MapIcons.Tooltips.BREWERY : null;
							moveIcon(config.breweryIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.CANDLE_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.CANDLE_SHOP : null;
							moveIcon(config.candleShopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.CHAINMAIL_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.CHAINMAIL_SHOP : null;
							moveIcon(config.chainbodyShopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.CLAN_HUB:
							tooltip = showTooltips ? MapIcons.Tooltips.CLAN_HUB : null;
							moveIcon(config.clanHubIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.CLOTHES_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.CLOTHES_SHOP : null;
							moveIcon(config.clothesShopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.CLUE_TUTOR:
							tooltip = showTooltips ? MapIcons.Tooltips.CLUE_TUTOR : null;
							moveIcon(config.clueTutorIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.COMBAT_ACHIEVEMENTS:
							tooltip = showTooltips ? MapIcons.Tooltips.COMBAT_ACHIEVEMENTS : null;
							moveIcon(config.combatAchievementsIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.COMBAT_TRAINING:
							tooltip = showTooltips ? MapIcons.Tooltips.COMBAT_TRAINING : null;
							moveIcon(config.combatTrainingIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.COMBAT_TUTOR:
							tooltip = showTooltips ? MapIcons.Tooltips.COMBAT_TUTOR : null;
							moveIcon(config.combatTutorIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.COOKING_RANGE:
							tooltip = showTooltips ? MapIcons.Tooltips.COOKING_RANGE : null;
							moveIcon(config.cookingRangeIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.COOKING_TUTOR:
							tooltip = showTooltips ? MapIcons.Tooltips.COOKING_TUTOR : null;
							moveIcon(config.cookingTutorIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.CRAFTING_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.CRAFTING_SHOP : null;
							moveIcon(config.craftingShopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.CRAFTING_TUTOR:
							tooltip = showTooltips ? MapIcons.Tooltips.CRAFTING_TUTOR : null;
							moveIcon(config.craftingTutorIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.DAIRY_CHURN:
							tooltip = showTooltips ? MapIcons.Tooltips.DAIRY_CHURN : null;
							moveIcon(config.dairyChurnIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.DAIRY_COW:
							tooltip = showTooltips ? MapIcons.Tooltips.DAIRY_COW : null;
							moveIcon(config.dairyCowIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.DANGER_TUTOR:
							tooltip = showTooltips ? MapIcons.Tooltips.DANGER_TUTOR : null;
							moveIcon(config.dangerTutorIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.DEADMAN_TUTOR:
							tooltip = showTooltips ? MapIcons.Tooltips.DEADMAN_TUTOR : null;
							moveIcon(config.deadmanTutorIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.DEATHS_OFFICE:
							tooltip = showTooltips ? MapIcons.Tooltips.DEATHS_OFFICE : null;
							moveIcon(config.deathsOfficeIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.DISTRACTION_AND_DIVERSION:
							tooltip = showTooltips ? MapIcons.Tooltips.DISTRACTION_AND_DIVERSION : null;
							moveIcon(config.distractionAndDiversionIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.DUNGEON:
							tooltip = showTooltips ? (worldMapDungeonTooltips ? null : MapIcons.Tooltips.DUNGEON) : null;
							moveIcon(config.dungeonIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.DYE_TRADER:
							tooltip = showTooltips ? MapIcons.Tooltips.DYE_TRADER : null;
							moveIcon(config.dyeTraderIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.ESTATE_AGENT:
							tooltip = showTooltips ? MapIcons.Tooltips.ESTATE_AGENT : null;
							moveIcon(config.estateAgentIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.FARMING_PATCH:
							tooltip = showTooltips ? (worldMapFarmingTooltips ? null : MapIcons.Tooltips.FARMING_PATCH) : null;
							moveIcon(config.farmingPatchIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.FARMING_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.FARMING_SHOP : null;
							moveIcon(config.farmingShopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.FISHING_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.FISHING_SHOP : null;
							moveIcon(config.fishingShopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.FISHING_SPOT:
							tooltip = showTooltips ? (worldMapFishingTooltips ? null : MapIcons.Tooltips.FISHING_SPOT) : null;
							moveIcon(config.fishingSpotIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.FISHING_TUTOR:
							tooltip = showTooltips ? MapIcons.Tooltips.FISHING_TUTOR : null;
							moveIcon(config.fishingTutorIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.FOOD_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.FOOD_SHOP : null;
							moveIcon(config.foodShopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.FORESTRY_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.FORESTRY_SHOP : null;
							moveIcon(config.forestryShopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.FUR_TRADER:
							tooltip = showTooltips ? MapIcons.Tooltips.FUR_TRADER : null;
							moveIcon(config.furTraderIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.FURNACE:
							tooltip = showTooltips ? MapIcons.Tooltips.FURNACE : null;
							moveIcon(config.furnaceIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.GARDEN_SUPPLIER:
							tooltip = showTooltips ? MapIcons.Tooltips.GARDEN_SUPPLIER : null;
							moveIcon(config.gardenSupplierIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.GEM_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.GEM_SHOP : null;
							moveIcon(config.gemShopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.GENERAL_STORE:
							tooltip = showTooltips ? MapIcons.Tooltips.GENERAL_STORE : null;
							moveIcon(config.generalStoreIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.GRAND_EXCHANGE:
							tooltip = showTooltips ? MapIcons.Tooltips.GRAND_EXCHANGE : null;
							moveIcon(config.grandExchangeIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.GRINDSTONE:
							tooltip = showTooltips ? MapIcons.Tooltips.GRINDSTONE : null;
							moveIcon(config.grindstoneIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.HAIRDRESSER:
							tooltip = showTooltips ? MapIcons.Tooltips.HAIRDRESSER : null;
							moveIcon(config.hairdresserIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.HELMET_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.HELMET_SHOP : null;
							moveIcon(config.helmetShopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.HERBALIST:
							tooltip = showTooltips ? MapIcons.Tooltips.HERBALIST : null;
							moveIcon(config.herbalistIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.HOLIDAY_EVENT:
							tooltip = showTooltips ? MapIcons.Tooltips.HOLIDAY_EVENT : null;
							moveIcon(config.holidayEventIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.HOLIDAY_ITEM_TRADER:
							tooltip = showTooltips ? MapIcons.Tooltips.HOLIDAY_ITEM_TRADER : null;
							moveIcon(config.holidayItemTraderIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.HOUSE_PORTAL:
							tooltip = showTooltips ? MapIcons.Tooltips.HOUSE_PORTAL : null;
							moveIcon(config.housePortalIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.HUNTER_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.HUNTER_SHOP : null;
							moveIcon(config.hunterShopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.HUNTER_TRAINING:
							tooltip = showTooltips ? (worldMapHunterTooltips ? null : MapIcons.Tooltips.HUNTER_TRAINING) : null;
							moveIcon(config.hunterTrainingIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.HUNTER_TUTOR:
							tooltip = showTooltips ? MapIcons.Tooltips.HUNTER_TUTOR : null;
							moveIcon(config.hunterTutorIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.IRONMAN_TUTOR:
							tooltip = showTooltips ? MapIcons.Tooltips.IRONMAN_TUTOR : null;
							moveIcon(config.ironmanTutorIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.JEWELLERY_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.JEWELLERY_SHOP : null;
							moveIcon(config.jewelleryShopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.JUNK_CHECKER:
							tooltip = showTooltips ? MapIcons.Tooltips.JUNK_CHECKER : null;
							moveIcon(config.junkCheckerIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.LEAGUES_TUTOR:
							tooltip = showTooltips ? MapIcons.Tooltips.LEAGUES_TUTOR : null;
							moveIcon(config.leaguesTutorIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.LOOM:
							tooltip = showTooltips ? MapIcons.Tooltips.LOOM : null;
							moveIcon(config.loomIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.LUMBRIDGE_GUIDE:
							tooltip = showTooltips ? MapIcons.Tooltips.LUMBRIDGE_GUIDE : null;
							moveIcon(config.lumbridgeGuideIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.MACE_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.MACE_SHOP : null;
							moveIcon(config.maceShopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.MAGIC_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.MAGIC_SHOP : null;
							moveIcon(config.magicShopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.MAKEOVER_MAGE:
							tooltip = showTooltips ? MapIcons.Tooltips.MAKEOVER_MAGE : null;
							moveIcon(config.makeoverMageIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.MINIGAME:
							tooltip = showTooltips ? (worldMapMinigameTooltips ? null : MapIcons.Tooltips.MINIGAME) : null;
							moveIcon(config.minigameIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.MINING_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.MINING_SHOP : null;
							moveIcon(config.miningShopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.MINING_SITE:
							tooltip = showTooltips ? (worldMapMiningTooltips ? null : MapIcons.Tooltips.MINING_SITE) : null;
							moveIcon(config.miningSiteIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.MINING_TUTOR:
							tooltip = showTooltips ? MapIcons.Tooltips.MINING_TUTOR : null;
							moveIcon(config.miningTutorIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.NEWSPAPER_TRADER:
							tooltip = showTooltips ? MapIcons.Tooltips.NEWSPAPER_TRADER : null;
							moveIcon(config.newspaperTraderIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.PET_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.PET_SHOP : null;
							moveIcon(config.petShopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.PLATEBODY_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.PLATEBODY_SHOP : null;
							moveIcon(config.platebodyShopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.PLATELEGS_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.PLATELEGS_SHOP : null;
							moveIcon(config.platelegsShopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.PLATESKIRT_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.PLATESKIRT_SHOP : null;
							moveIcon(config.plateskirtShopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.POLISHING_WHEEL:
							tooltip = showTooltips ? MapIcons.Tooltips.POLISHING_WHEEL : null;
							moveIcon(config.polishingWheelIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.POLL_BOOTH:
							tooltip = showTooltips ? MapIcons.Tooltips.POLL_BOOTH : null;
							moveIcon(config.pollBoothIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.POTTERY_WHEEL:
							tooltip = showTooltips ? MapIcons.Tooltips.POTTERY_WHEEL : null;
							moveIcon(config.potteryWheelIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.PRAYER_TUTOR:
							tooltip = showTooltips ? MapIcons.Tooltips.PRAYER_TUTOR : null;
							moveIcon(config.prayerTutorIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.PRICING_EXPERT:
							tooltip = showTooltips ? MapIcons.Tooltips.PRICING_EXPERT : null;
							moveIcon(config.pricingExpertIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.PUB:
							tooltip = showTooltips ? MapIcons.Tooltips.PUB : null;
							moveIcon(config.pubIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.QUEST_START:
							if (QuestLocationLookup.objectsToQuests.containsKey(iconPosition))
							{
								Quest q = QuestLocationLookup.objectsToQuests.get(iconPosition);
								tooltip = showTooltips ? q.getName() : null;
								moveIcon(config.questStartIcon(),iconConfig,iconPosition,tooltip,BLANK_QUEST_ICON);
							}
							else
							{
								log.debug("Quest icon at location {} not yet included", iconPosition);
							}
							break;
						case MapIcons.Categories.RAID:
							tooltip = showTooltips ? MapIcons.Tooltips.RAID : null;
							moveIcon(config.raidIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.RARE_TREES:
							tooltip = showTooltips ? (worldMapRareTreesTooltips ? null : MapIcons.Tooltips.RARE_TREES) : null;
							moveIcon(config.rareTreesIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.ROPE_TRADER:
							tooltip = showTooltips ? MapIcons.Tooltips.ROPE_TRADER : null;
							moveIcon(config.ropeTraderIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.SANDPIT:
							tooltip = showTooltips ? MapIcons.Tooltips.SANDPIT : null;
							moveIcon(config.sandpitIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.SAWMILL:
							tooltip = showTooltips ? MapIcons.Tooltips.SAWMILL : null;
							moveIcon(config.sawmillIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.SCIMITAR_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.SCIMITAR_SHOP : null;
							moveIcon(config.scimitarShopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.SECURITY_TUTOR:
							tooltip = showTooltips ? MapIcons.Tooltips.SECURITY_TUTOR : null;
							moveIcon(config.securityTutorIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.SHIELD_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.SHIELD_SHOP : null;
							moveIcon(config.shieldShopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.SILK_TRADER:
							tooltip = showTooltips ? MapIcons.Tooltips.SILK_TRADER : null;
							moveIcon(config.silkTraderIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.SILVER_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.SILVER_SHOP : null;
							moveIcon(config.silverShopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.SINGING_BOWL:
							tooltip = showTooltips ? MapIcons.Tooltips.SINGING_BOWL : null;
							moveIcon(config.singingBowlIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.SLAYER_MASTER:
							tooltip = showTooltips ? MapIcons.Tooltips.SLAYER_MASTER : null;
							moveIcon(config.slayerMasterIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.SMITHING_TUTOR:
							tooltip = showTooltips ? MapIcons.Tooltips.SMITHING_TUTOR : null;
							moveIcon(config.smithingTutorIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.SPEEDRUNNING_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.SPEEDRUNNING_SHOP : null;
							moveIcon(config.speedrunningShopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.SPICE_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.SPICE_SHOP : null;
							moveIcon(config.spiceShopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.SPINNING_WHEEL:
							tooltip = showTooltips ? MapIcons.Tooltips.SPINNING_WHEEL : null;
							moveIcon(config.spinningWheelIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.STAFF_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.STAFF_SHOP : null;
							moveIcon(config.staffShopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.STAGNANT_WATER_SOURCE:
							tooltip = showTooltips ? MapIcons.Tooltips.STAGNANT_WATER_SOURCE : null;
							moveIcon(config.stagnantWaterSourceIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.STONEMASON:
							tooltip = showTooltips ? MapIcons.Tooltips.STONEMASON : null;
							moveIcon(config.stonemasonIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.SWORD_SHOP:
							tooltip = showTooltips ? MapIcons.Tooltips.SWORD_SHOP : null;
							moveIcon(config.swordShopIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.TANNERY:
							tooltip = showTooltips ? MapIcons.Tooltips.TANNERY : null;
							moveIcon(config.tanneryIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.TASK_MASTER:
							tooltip = showTooltips ? MapIcons.Tooltips.TASK_MASTER : null;
							moveIcon(config.taskMasterIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.TAXIDERMIST:
							tooltip = showTooltips ? MapIcons.Tooltips.TAXIDERMIST : null;
							moveIcon(config.taxidermistIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.TEA_TRADER:
							tooltip = showTooltips ? MapIcons.Tooltips.TEA_TRADER : null;
							moveIcon(config.teaTraderIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.THIEVING_ACTIVITY:
							tooltip = showTooltips ? MapIcons.Tooltips.THIEVING_ACTIVITY : null;
							moveIcon(config.thievingActivityIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.TRANSPORTATION:
							tooltip = showTooltips ? (worldMapTransportationTooltips ? null : MapIcons.Tooltips.TRANSPORTATION) : null;
							moveIcon(config.transportationIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.TRIP_HAMMER:
							tooltip = showTooltips ? MapIcons.Tooltips.TRIP_HAMMER : null;
							moveIcon(config.tripHammerIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.VALE_TOTEM:
							tooltip = showTooltips ? MapIcons.Tooltips.VALE_TOTEM : null;
							moveIcon(config.valeTotemIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.WATER_SOURCE:
							tooltip = showTooltips ? MapIcons.Tooltips.WATER_SOURCE : null;
							moveIcon(config.waterSourceIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.WINDMILL:
							tooltip = showTooltips ? MapIcons.Tooltips.WINDMILL : null;
							moveIcon(config.windmillIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.WINE_TRADER:
							tooltip = showTooltips ? MapIcons.Tooltips.WINE_TRADER : null;
							moveIcon(config.wineTraderIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.WOODCUTTING_STUMP:
							tooltip = showTooltips ? MapIcons.Tooltips.WOODCUTTING_STUMP : null;
							moveIcon(config.woodcuttingStumpIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
						case MapIcons.Categories.WOODCUTTING_TUTOR:
							tooltip = showTooltips ? MapIcons.Tooltips.WOODCUTTING_TUTOR : null;
							moveIcon(config.woodcuttingTutorIcon(),iconConfig,iconPosition,tooltip, BLANK_ICON);
							break;
					}

				}
			}
		}
	}

	private void moveIcon(boolean setIconVisible, MapElementConfig iconConfig, WorldPoint iconPosition, String iconTooltip, BufferedImage iconImage)
	{
		SpritePixels iconSprite = iconConfig.getMapIcon(false); // Must be false otherwise nothing happens
		if (setIconVisible)
		{
			iconSprite.setOffsetX(0);
			iconSprite.setOffsetY(0);

			WorldMapPoint t = MapPoint.builder()
				.type(MapPoint.Type.DEFAULT)
				.worldPoint(iconPosition)
				.image(iconImage)
				.tooltip(iconTooltip)
				.build();

			worldMapPointManager.add(t);
		}
		else
		{
			iconSprite.setOffsetX(OFFSET_TO_HIDE_ICON_SPRITES);
			iconSprite.setOffsetY(OFFSET_TO_HIDE_ICON_SPRITES);
		}
	}

	private void resetWorldMapIcons()
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

		worldMapPointManager.removeIf(MapPoint.class::isInstance);

		WorldMapRegion[][] regions = wmm.getMapRegions();
		for (WorldMapRegion[] worldMapRegions : regions)
		{
			for (WorldMapRegion region : worldMapRegions)
			{
				for (WorldMapIcon icon : region.getMapIcons())
				{
					MapElementConfig iconConfig = client.getMapElementConfig(icon.getType());
					iconConfig.getMapIcon(false).setOffsetX(0);
					iconConfig.getMapIcon(false).setOffsetY(0);
				}
			}
		}
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
			}
		}

		worldMapTransportationTooltips = configManager.getConfiguration(worldmapClientPluginGroupName,"transportationTooltips").equals("true") && worldMapClientPluginEnabled;
		worldMapAgilityCourseTooltips = configManager.getConfiguration(worldmapClientPluginGroupName,"agilityCourseTooltips").equals("true") && worldMapClientPluginEnabled;
		worldMapAgilityShortcutTooltips = configManager.getConfiguration(worldmapClientPluginGroupName,"agilityShortcutTooltips").equals("true") && worldMapClientPluginEnabled;
		worldMapMinigameTooltips = configManager.getConfiguration(worldmapClientPluginGroupName,"minigameTooltip").equals("true") && worldMapClientPluginEnabled;
		worldMapFarmingTooltips = configManager.getConfiguration(worldmapClientPluginGroupName,"farmingpatchTooltips").equals("true") && worldMapClientPluginEnabled;
		worldMapRareTreesTooltips = configManager.getConfiguration(worldmapClientPluginGroupName,"rareTreeTooltips").equals("true") && worldMapClientPluginEnabled;
		worldMapMiningTooltips = configManager.getConfiguration(worldmapClientPluginGroupName,"miningSiteTooltips").equals("true") && worldMapClientPluginEnabled;
		worldMapDungeonTooltips = configManager.getConfiguration(worldmapClientPluginGroupName,"dungeonTooltips").equals("true") && worldMapClientPluginEnabled;
		worldMapHunterTooltips = configManager.getConfiguration(worldmapClientPluginGroupName,"hunterAreaTooltips").equals("true") && worldMapClientPluginEnabled;
		worldMapFishingTooltips = configManager.getConfiguration(worldmapClientPluginGroupName,"fishingSpotTooltips").equals("true") && worldMapClientPluginEnabled;
	}

	@Provides
	WorldmapExtendedConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WorldmapExtendedConfig.class);
	}
}
