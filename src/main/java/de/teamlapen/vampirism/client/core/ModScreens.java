package de.teamlapen.vampirism.client.core;

import de.teamlapen.vampirism.api.client.VIngameOverlays;
import de.teamlapen.vampirism.client.gui.*;
import de.teamlapen.vampirism.core.ModContainer;
import de.teamlapen.vampirism.inventory.container.HunterTableContainer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

@OnlyIn(Dist.CLIENT)
public class ModScreens {
    /**
     * Call on serial loading queue to avoid possible issues with parallel access
     */
    public static void registerScreensUnsafe() {
        MenuScreens.<HunterTableContainer, HunterTableScreen>register(ModContainer.HUNTER_TABLE.get(), HunterTableScreen::new);
        MenuScreens.register(ModContainer.ALCHEMICAL_CAULDRON.get(), AlchemicalCauldronScreen::new);
        MenuScreens.register(ModContainer.WEAPON_TABLE.get(), WeaponTableScreen::new);
        MenuScreens.register(ModContainer.HUNTER_TRAINER.get(), HunterTrainerScreen::new);
        MenuScreens.register(ModContainer.HUNTER_BASIC.get(), HunterBasicScreen::new);
        MenuScreens.register(ModContainer.ALTAR_INFUSION.get(), AltarInfusionScreen::new);
        MenuScreens.register(ModContainer.BLOOD_GRINDER.get(), BloodGrinderScreen::new);
        MenuScreens.register(ModContainer.MINION.get(), MinionScreen::new);
        MenuScreens.register(ModContainer.TASK_MASTER.get(), TaskBoardScreen::new);
        MenuScreens.register(ModContainer.EXTENDED_POTION_TABLE.get(), PotionTableScreen::new);
        MenuScreens.register(ModContainer.VAMPIRISM.get(), VampirismScreen::new);
    }

    public static void registerScreenOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.EXPERIENCE_BAR.id(), VIngameOverlays.FACTION_LEVEL_ID.getPath(), VIngameOverlays.FACTION_LEVEL_ELEMENT);
        event.registerAbove(VanillaGuiOverlay.BOSS_EVENT_PROGRESS.id(),VIngameOverlays.FACTION_RAID_BAR_ID.getPath(), VIngameOverlays.FACTION_RAID_BAR_ELEMENT);
        event.registerAbove(VanillaGuiOverlay.FOOD_LEVEL.id(), VIngameOverlays.BLOOD_BAR_ID.getPath(), VIngameOverlays.BLOOD_BAR_ELEMENT);
    }
}
