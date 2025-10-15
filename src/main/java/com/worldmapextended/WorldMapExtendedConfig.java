package com.worldmapextended;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(WorldMapExtendedPlugin.CONFIG_GROUP)
public interface WorldMapExtendedConfig extends Config
{

	//region Game Features
	@ConfigSection(
		name = "Game Features",
		description = "Display options for the Game Feature icons.",
		position = 1,
		closedByDefault = true
	)
	String gameFeatureSection = "gameFeatureSection";

	@ConfigItem(
		keyName = "agilityShortcutsIcon",
		name = "Show Agility Shortcut icons",
		description = "Choose whether to view or hide the icons (may clash with some 'World Maps' settings).",
		position = 1,
		section = gameFeatureSection
	)
	default boolean agilityShortcutsIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "altarIcon",
		name = "Show Altar icons",
		description = "Choose whether to view or hide the icons.",
		position = 2,
		section = gameFeatureSection
	)
	default boolean altarIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bankIcon",
		name = "Show Bank icons",
		description = "Choose whether to view or hide the icons.",
		position = 3,
		section = gameFeatureSection
	)
	default boolean bankIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "clanHubIcon",
		name = "Show Clan Hub icon",
		description = "Choose whether to view or hide the icon.",
		position = 4,
		section = gameFeatureSection
	)
	default boolean clanHubIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "combatAchievementsIcon",
		name = "Show Combat Achievements icon",
		description = "Choose whether to view or hide the icon.",
		position = 5,
		section = gameFeatureSection
	)
	default boolean combatAchievementsIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "deathsOfficeIcon",
		name = "Show Deaths Office icons",
		description = "Choose whether to view or hide the icons.",
		position = 6,
		section = gameFeatureSection
	)
	default boolean deathsOfficeIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "distractionAndDiversionIcon",
		name = "Show Distraction And Diversion icons",
		description = "Choose whether to view or hide the icons.",
		position = 7,
		section = gameFeatureSection
	)
	default boolean distractionAndDiversionIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dungeonIcon",
		name = "Show Dungeon icons",
		description = "Choose whether to view or hide the icons (may clash with some 'World Maps' settings).",
		position = 8,
		section = gameFeatureSection
	)
	default boolean dungeonIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hairdresserIcon",
		name = "Show Hairdresser icons",
		description = "Choose whether to view or hide the icons.",
		position = 9,
		section = gameFeatureSection
	)
	default boolean hairdresserIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "holidayEventIcon",
		name = "Show Holiday Event icons",
		description = "Choose whether to view or hide the icons.",
		position = 10,
		section = gameFeatureSection
	)
	default boolean holidayEventIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "housePortalIcon",
		name = "Show House Portal icons",
		description = "Choose whether to view or hide the icons.",
		position = 11,
		section = gameFeatureSection
	)
	default boolean housePortalIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "makeoverMageIcon",
		name = "Show Makeover Mage icon",
		description = "Choose whether to view or hide the icon.",
		position = 12,
		section = gameFeatureSection
	)
	default boolean makeoverMageIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "minigameIcon",
		name = "Show Minigame icons",
		description = "Choose whether to view or hide the icons (may clash with some 'World Maps' settings).",
		position = 13,
		section = gameFeatureSection
	)
	default boolean minigameIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "pollBoothIcon",
		name = "Show Poll Booth icons",
		description = "Choose whether to view or hide the icons.",
		position = 14,
		section = gameFeatureSection
	)
	default boolean pollBoothIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "questStartIcon",
		name = "Show Quest icons",
		description = "Choose whether to view or hide the icons (may clash with some 'World Maps' settings).",
		position = 15,
		section = gameFeatureSection
	)
	default boolean questStartIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "raidIcon",
		name = "Show Raid icons",
		description = "Choose whether to view or hide the icons.",
		position = 16,
		section = gameFeatureSection
	)
	default boolean raidIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "taskMasterIcon",
		name = "Show Task Master icons",
		description = "Choose whether to view or hide the icons.",
		position = 17,
		section = gameFeatureSection
	)
	default boolean taskMasterIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "transportationIcon",
		name = "Show Transportation icons",
		description = "Choose whether to view or hide the icons (may clash with some 'World Maps' settings).",
		position = 18,
		section = gameFeatureSection
	)
	default boolean transportationIcon()
	{
		return true;
	}
	//endregion

	//region Tutors
	@ConfigSection(
		name = "Tutors",
		description = "Display options for the Tutor icons.",
		position = 2,
		closedByDefault = true
	)
	String tutorSection = "tutorSection";

	@ConfigItem(
		keyName = "bankTutorIcon",
		name = "Show Bank Tutor icon",
		description = "Choose whether to view or hide the icon.",
		position = 1,
		section = tutorSection
	)
	default boolean bankTutorIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bondTutorIcon",
		name = "Show Bond Tutor icon",
		description = "Choose whether to view or hide the icon.",
		position = 2,
		section = tutorSection
	)
	default boolean bondTutorIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "clueTutorIcon",
		name = "Show Clue Tutor icon",
		description = "Choose whether to view or hide the icon.",
		position = 3,
		section = tutorSection
	)
	default boolean clueTutorIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "combatTutorIcon",
		name = "Show Combat Tutor icon",
		description = "Choose whether to view or hide the icon.",
		position = 4,
		section = tutorSection
	)
	default boolean combatTutorIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "cookingTutorIcon",
		name = "Show Cooking Tutor icon",
		description = "Choose whether to view or hide the icon.",
		position = 5,
		section = tutorSection
	)
	default boolean cookingTutorIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "craftingTutorIcon",
		name = "Show Crafting Tutor icon",
		description = "Choose whether to view or hide the icon.",
		position = 6,
		section = tutorSection
	)
	default boolean craftingTutorIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dangerTutorIcon",
		name = "Show Danger Tutor icon",
		description = "Choose whether to view or hide the icon.",
		position = 7,
		section = tutorSection
	)
	default boolean dangerTutorIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "deadmanTutorIcon",
		name = "Show Deadman Tutor icon",
		description = "Choose whether to view or hide the icon.",
		position = 8,
		section = tutorSection
	)
	default boolean deadmanTutorIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "fishingTutorIcon",
		name = "Show Fishing Tutor icon",
		description = "Choose whether to view or hide the icon.",
		position = 9,
		section = tutorSection
	)
	default boolean fishingTutorIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hunterTutorIcon",
		name = "Show Hunter Tutor icon",
		description = "Choose whether to view or hide the icon.",
		position = 10,
		section = tutorSection
	)
	default boolean hunterTutorIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ironmanTutorIcon",
		name = "Show Ironman Tutor icon",
		description = "Choose whether to view or hide the icon.",
		position = 11,
		section = tutorSection
	)
	default boolean ironmanTutorIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "leaguesTutorIcon",
		name = "Show Leagues Tutor icon",
		description = "Choose whether to view or hide the icon.",
		position = 12,
		section = tutorSection
	)
	default boolean leaguesTutorIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "lumbridgeGuideIcon",
		name = "Show Lumbridge Guide icon",
		description = "Choose whether to view or hide the icon.",
		position = 13,
		section = tutorSection
	)
	default boolean lumbridgeGuideIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "miningTutorIcon",
		name = "Show Mining Tutor icon",
		description = "Choose whether to view or hide the icon.",
		position = 14,
		section = tutorSection
	)
	default boolean miningTutorIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "prayerTutorIcon",
		name = "Show Prayer Tutor icon",
		description = "Choose whether to view or hide the icon.",
		position = 15,
		section = tutorSection
	)
	default boolean prayerTutorIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "securityTutorIcon",
		name = "Show Security Tutor icon",
		description = "Choose whether to view or hide the icon.",
		position = 16,
		section = tutorSection
	)
	default boolean securityTutorIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "smithingTutorIcon",
		name = "Show Smithing Tutor icon",
		description = "Choose whether to view or hide the icon.",
		position = 17,
		section = tutorSection
	)
	default boolean smithingTutorIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "woodcuttingTutorIcon",
		name = "Show Woodcutting Tutor icon",
		description = "Choose whether to view or hide the icon.",
		position = 18,
		section = tutorSection
	)
	default boolean woodcuttingTutorIcon()
	{
		return true;
	}
	//endregion

	//region Training
	@ConfigSection(
		name = "Training",
		description = "Display options for the Training icons.",
		position = 3,
		closedByDefault = true
	)
	String trainingSection = "trainingSection";

	@ConfigItem(
		keyName = "agilityCoursesIcon",
		name = "Show Agility Courses icons",
		description = "Choose whether to view or hide the icons (may clash with some 'World Maps' settings).",
		position = 1,
		section = trainingSection
	)
	default boolean agilityCoursesIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "anvilIcon",
		name = "Show Anvil icons",
		description = "Choose whether to view or hide the icons.",
		position = 4,
		section = trainingSection
	)
	default boolean anvilIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "birdHouseIcon",
		name = "Show Bird House icons",
		description = "Choose whether to view or hide the icons.",
		position = 4,
		section = trainingSection
	)
	default boolean birdHouseIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "breweryIcon",
		name = "Show Brewery icons",
		description = "Choose whether to view or hide the icons.",
		position = 5,
		section = trainingSection
	)
	default boolean breweryIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "combatTrainingIcon",
		name = "Show Combat Training icons",
		description = "Choose whether to view or hide the icons.",
		position = 6,
		section = trainingSection
	)
	default boolean combatTrainingIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "cookingRangeIcon",
		name = "Show Cooking Range icons",
		description = "Choose whether to view or hide the icons.",
		position = 7,
		section = trainingSection
	)
	default boolean cookingRangeIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dairyChurnIcon",
		name = "Show Dairy Churn icons",
		description = "Choose whether to view or hide the icons.",
		position = 8,
		section = trainingSection
	)
	default boolean dairyChurnIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dairyCowIcon",
		name = "Show Dairy Cow icons",
		description = "Choose whether to view or hide the icons.",
		position = 9,
		section = trainingSection
	)
	default boolean dairyCowIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "farmingPatchIcon",
		name = "Show Farming Patch icons",
		description = "Choose whether to view or hide the icons (may clash with some 'World Maps' settings).",
		position = 10,
		section = trainingSection
	)
	default boolean farmingPatchIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "fishingSpotIcon",
		name = "Show Fishing Spot icons",
		description = "Choose whether to view or hide the icons (may clash with some 'World Maps' settings).",
		position = 11,
		section = trainingSection
	)
	default boolean fishingSpotIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "furnaceIcon",
		name = "Show Furnace icons",
		description = "Choose whether to view or hide the icons.",
		position = 12,
		section = trainingSection
	)
	default boolean furnaceIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "grindstoneIcon",
		name = "Show Grindstone icons",
		description = "Choose whether to view or hide the icons.",
		position = 13,
		section = trainingSection
	)
	default boolean grindstoneIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hunterTrainingIcon",
		name = "Show Hunter Training icons",
		description = "Choose whether to view or hide the icons (may clash with some 'World Maps' settings).",
		position = 14,
		section = trainingSection
	)
	default boolean hunterTrainingIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "loomIcon",
		name = "Show Loom icons",
		description = "Choose whether to view or hide the icons.",
		position = 15,
		section = trainingSection
	)
	default boolean loomIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "miningSiteIcon",
		name = "Show Mining Site icons",
		description = "Choose whether to view or hide the icons (may clash with some 'World Maps' settings).",
		position = 16,
		section = trainingSection
	)
	default boolean miningSiteIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "polishingWheelIcon",
		name = "Show Polishing Wheel icons",
		description = "Choose whether to view or hide the icons.",
		position = 17,
		section = trainingSection
	)
	default boolean polishingWheelIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "potteryWheelIcon",
		name = "Show Pottery Wheel icons",
		description = "Choose whether to view or hide the icons.",
		position = 18,
		section = trainingSection
	)
	default boolean potteryWheelIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "rareTreesIcon",
		name = "Show Rare Trees icons",
		description = "Choose whether to view or hide the icons (may clash with some 'World Maps' settings).",
		position = 19,
		section = trainingSection
	)
	default boolean rareTreesIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sandpitIcon",
		name = "Show Sandpit icons",
		description = "Choose whether to view or hide the icons.",
		position = 20,
		section = trainingSection
	)
	default boolean sandpitIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "singingBowlIcon",
		name = "Show Singing Bowl icons",
		description = "Choose whether to view or hide the icons.",
		position = 21,
		section = trainingSection
	)
	default boolean singingBowlIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "slayerMasterIcon",
		name = "Show Slayer Master icons",
		description = "Choose whether to view or hide the icons.",
		position = 22,
		section = trainingSection
	)
	default boolean slayerMasterIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "spinningWheelIcon",
		name = "Show Spinning Wheel icons",
		description = "Choose whether to view or hide the icons.",
		position = 23,
		section = trainingSection
	)
	default boolean spinningWheelIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "stagnantWaterSourceIcon",
		name = "Show Stagnant Water Source icon",
		description = "Choose whether to view or hide the icon.",
		position = 24,
		section = trainingSection
	)
	default boolean stagnantWaterSourceIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "thievingActivityIcon",
		name = "Show Thieving Activity icons",
		description = "Choose whether to view or hide the icons.",
		position = 25,
		section = trainingSection
	)
	default boolean thievingActivityIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tripHammerIcon",
		name = "Show Trip Hammer icon",
		description = "Choose whether to view or hide the icon.",
		position = 26,
		section = trainingSection
	)
	default boolean tripHammerIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "valeTotemIcon",
		name = "Show Vale Totem icons",
		description = "Choose whether to view or hide the icons.",
		position = 27,
		section = trainingSection
	)
	default boolean valeTotemIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "waterSourceIcon",
		name = "Show Water Source icons",
		description = "Choose whether to view or hide the icons.",
		position = 28,
		section = trainingSection
	)
	default boolean waterSourceIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "windmillIcon",
		name = "Show Windmill icons",
		description = "Choose whether to view or hide the icons.",
		position = 29,
		section = trainingSection
	)
	default boolean windmillIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "woodcuttingStumpIcon",
		name = "Show Woodcutting Stump icon",
		description = "Choose whether to view or hide the icon.",
		position = 30,
		section = trainingSection
	)
	default boolean woodcuttingStumpIcon()
	{
		return true;
	}
	//endregion

	//region Shops
	@ConfigSection(
		name = "Shopping",
		description = "Display options for the Shopping icons.",
		position = 4,
		closedByDefault = true
	)
	String shoppingSection = "shoppingSection";

	@ConfigItem(
		keyName = "amuletshopIcon",
		name = "Show Amulet shop icon",
		description = "Choose whether to view or hide the icon.",
		position = 1,
		section = shoppingSection
	)
	default boolean amuletshopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "apothecaryIcon",
		name = "Show Apothecary icons",
		description = "Choose whether to view or hide the icons.",
		position = 2,
		section = shoppingSection
	)
	default boolean apothecaryIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "archeryshopIcon",
		name = "Show Archery shop icons",
		description = "Choose whether to view or hide the icons.",
		position = 3,
		section = shoppingSection
	)
	default boolean archeryshopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "axeshopIcon",
		name = "Show Axe shop icons",
		description = "Choose whether to view or hide the icons.",
		position = 4,
		section = shoppingSection
	)
	default boolean axeshopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bountyHunterIcon",
		name = "Show Bounty Hunter icon",
		description = "Choose whether to view or hide the icon.",
		position = 5,
		section = shoppingSection
	)
	default boolean bountyHunterIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "candleShopIcon",
		name = "Show Candle Shop icons",
		description = "Choose whether to view or hide the icons.",
		position = 6,
		section = shoppingSection
	)
	default boolean candleShopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "chainbodyShopIcon",
		name = "Show Chainbody Shop icon",
		description = "Choose whether to view or hide the icon.",
		position = 7,
		section = shoppingSection
	)
	default boolean chainbodyShopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "clothesShopIcon",
		name = "Show Clothes Shop icons",
		description = "Choose whether to view or hide the icons.",
		position = 8,
		section = shoppingSection
	)
	default boolean clothesShopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "craftingShopIcon",
		name = "Show Crafting Shop icons",
		description = "Choose whether to view or hide the icons.",
		position = 9,
		section = shoppingSection
	)
	default boolean craftingShopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dyeTraderIcon",
		name = "Show Dye Trader icons",
		description = "Choose whether to view or hide the icons.",
		position = 10,
		section = shoppingSection
	)
	default boolean dyeTraderIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "estateAgentIcon",
		name = "Show Estate Agent icons",
		description = "Choose whether to view or hide the icons.",
		position = 11,
		section = shoppingSection
	)
	default boolean estateAgentIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "farmingShopIcon",
		name = "Show Farming Shop icons",
		description = "Choose whether to view or hide the icons.",
		position = 12,
		section = shoppingSection
	)
	default boolean farmingShopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "fishingShopIcon",
		name = "Show Fishing Shop icons",
		description = "Choose whether to view or hide the icons.",
		position = 13,
		section = shoppingSection
	)
	default boolean fishingShopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "foodShopIcon",
		name = "Show Food Shop icons",
		description = "Choose whether to view or hide the icons.",
		position = 14,
		section = shoppingSection
	)
	default boolean foodShopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "forestryShopIcon",
		name = "Show Forestry Shop icons",
		description = "Choose whether to view or hide the icons.",
		position = 15,
		section = shoppingSection
	)
	default boolean forestryShopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "furTraderIcon",
		name = "Show Fur Trader icons",
		description = "Choose whether to view or hide the icons.",
		position = 16,
		section = shoppingSection
	)
	default boolean furTraderIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "gardenSupplierIcon",
		name = "Show Garden Supplier icons",
		description = "Choose whether to view or hide the icons.",
		position = 17,
		section = shoppingSection
	)
	default boolean gardenSupplierIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "gemShopIcon",
		name = "Show Gem Shop icons",
		description = "Choose whether to view or hide the icons.",
		position = 18,
		section = shoppingSection
	)
	default boolean gemShopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "generalStoreIcon",
		name = "Show General Store icons",
		description = "Choose whether to view or hide the icons.",
		position = 19,
		section = shoppingSection
	)
	default boolean generalStoreIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "grandExchangeIcon",
		name = "Show Grand Exchange icon",
		description = "Choose whether to view or hide the icon.",
		position = 20,
		section = shoppingSection
	)
	default boolean grandExchangeIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "helmetShopIcon",
		name = "Show Helmet Shop icons",
		description = "Choose whether to view or hide the icons.",
		position = 21,
		section = shoppingSection
	)
	default boolean helmetShopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "herbalistIcon",
		name = "Show Herbalist icons",
		description = "Choose whether to view or hide the icons.",
		position = 22,
		section = shoppingSection
	)
	default boolean herbalistIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "holidayItemTraderIcon",
		name = "Show Holiday Item Trader icons",
		description = "Choose whether to view or hide the icons.",
		position = 23,
		section = shoppingSection
	)
	default boolean holidayItemTraderIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hunterShopIcon",
		name = "Show Hunter Shop icons",
		description = "Choose whether to view or hide the icons.",
		position = 24,
		section = shoppingSection
	)
	default boolean hunterShopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "jewelleryShopIcon",
		name = "Show Jewellery Shop icon",
		description = "Choose whether to view or hide the icon.",
		position = 25,
		section = shoppingSection
	)
	default boolean jewelleryShopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "junkCheckerIcon",
		name = "Show Junk Checker icon",
		description = "Choose whether to view or hide the icon.",
		position = 26,
		section = shoppingSection
	)
	default boolean junkCheckerIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "maceShopIcon",
		name = "Show Mace Shop icons",
		description = "Choose whether to view or hide the icons.",
		position = 27,
		section = shoppingSection
	)
	default boolean maceShopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "magicShopIcon",
		name = "Show Magic Shop icons",
		description = "Choose whether to view or hide the icons.",
		position = 28,
		section = shoppingSection
	)
	default boolean magicShopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "miningShopIcon",
		name = "Show Mining Shop icons",
		description = "Choose whether to view or hide the icons.",
		position = 29,
		section = shoppingSection
	)
	default boolean miningShopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "newspaperTraderIcon",
		name = "Show Newspaper Trader icon",
		description = "Choose whether to view or hide the icon.",
		position = 30,
		section = shoppingSection
	)
	default boolean newspaperTraderIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "petShopIcon",
		name = "Show Pet Insurance Shop icon",
		description = "Choose whether to view or hide the icon.",
		position = 31,
		section = shoppingSection
	)
	default boolean petShopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "platebodyShopIcon",
		name = "Show Platebody Shop icons",
		description = "Choose whether to view or hide the icons.",
		position = 32,
		section = shoppingSection
	)
	default boolean platebodyShopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "platelegsShopIcon",
		name = "Show Platelegs Shop icon",
		description = "Choose whether to view or hide the icon.",
		position = 33,
		section = shoppingSection
	)
	default boolean platelegsShopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "plateskirtShopIcon",
		name = "Show Plateskirt Shop icon",
		description = "Choose whether to view or hide the icon.",
		position = 34,
		section = shoppingSection
	)
	default boolean plateskirtShopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "pricingExpertIcon",
		name = "Show Pricing Expert icons",
		description = "Choose whether to view or hide the icons.",
		position = 35,
		section = shoppingSection
	)
	default boolean pricingExpertIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "pubIcon",
		name = "Show Pub icons",
		description = "Choose whether to view or hide the icons.",
		position = 36,
		section = shoppingSection
	)
	default boolean pubIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ropeTraderIcon",
		name = "Show Rope Trader icon",
		description = "Choose whether to view or hide the icon.",
		position = 37,
		section = shoppingSection
	)
	default boolean ropeTraderIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sawmillIcon",
		name = "Show Sawmill icons",
		description = "Choose whether to view or hide the icons.",
		position = 38,
		section = shoppingSection
	)
	default boolean sawmillIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "scimitarShopIcon",
		name = "Show Scimitar Shop icons",
		description = "Choose whether to view or hide the icons.",
		position = 39,
		section = shoppingSection
	)
	default boolean scimitarShopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "shieldShopIcon",
		name = "Show Shield Shop icons",
		description = "Choose whether to view or hide the icons.",
		position = 40,
		section = shoppingSection
	)
	default boolean shieldShopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "silkTraderIcon",
		name = "Show Silk Trader icons",
		description = "Choose whether to view or hide the icons.",
		position = 41,
		section = shoppingSection
	)
	default boolean silkTraderIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "silverShopIcon",
		name = "Show Silver Shop icons",
		description = "Choose whether to view or hide the icons.",
		position = 42,
		section = shoppingSection
	)
	default boolean silverShopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "speedrunningShopIcon",
		name = "Show Speedrunning Shop icon",
		description = "Choose whether to view or hide the icon.",
		position = 43,
		section = shoppingSection
	)
	default boolean speedrunningShopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "spiceShopIcon",
		name = "Show Spice Shop icons",
		description = "Choose whether to view or hide the icons.",
		position = 44,
		section = shoppingSection
	)
	default boolean spiceShopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "staffShopIcon",
		name = "Show Staff Shop icons",
		description = "Choose whether to view or hide the icons.",
		position = 45,
		section = shoppingSection
	)
	default boolean staffShopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "stonemasonIcon",
		name = "Show Stonemason icons",
		description = "Choose whether to view or hide the icons.",
		position = 46,
		section = shoppingSection
	)
	default boolean stonemasonIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "swordShopIcon",
		name = "Show Sword Shop icons",
		description = "Choose whether to view or hide the icons.",
		position = 47,
		section = shoppingSection
	)
	default boolean swordShopIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tanneryIcon",
		name = "Show Tannery icons",
		description = "Choose whether to view or hide the icons.",
		position = 48,
		section = shoppingSection
	)
	default boolean tanneryIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "taxidermistIcon",
		name = "Show Taxidermist icon",
		description = "Choose whether to view or hide the icon.",
		position = 49,
		section = shoppingSection
	)
	default boolean taxidermistIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "teaTraderIcon",
		name = "Show Tea Trader icon",
		description = "Choose whether to view or hide the icon.",
		position = 50,
		section = shoppingSection
	)
	default boolean teaTraderIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "wineTraderIcon",
		name = "Show Wine Trader icons",
		description = "Choose whether to view or hide the icons.",
		position = 51,
		section = shoppingSection
	)
	default boolean wineTraderIcon()
	{
		return true;
	}
	//endregion
}