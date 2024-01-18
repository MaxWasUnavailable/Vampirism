package de.teamlapen.vampirism;

import de.teamlapen.lib.HelperRegistry;
import de.teamlapen.lib.lib.entity.IPlayerEventListener;
import de.teamlapen.lib.lib.network.ISyncable;
import de.teamlapen.lib.lib.util.IInitListener;
import de.teamlapen.lib.util.Color;
import de.teamlapen.lib.util.OptifineHandler;
import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.VampirismAttachments;
import de.teamlapen.vampirism.api.VampirismRegistries;
import de.teamlapen.vampirism.api.entity.player.hunter.IHunterPlayer;
import de.teamlapen.vampirism.api.entity.player.skills.SkillType;
import de.teamlapen.vampirism.api.entity.player.vampire.IVampirePlayer;
import de.teamlapen.vampirism.blockentity.*;
import de.teamlapen.vampirism.client.VampirismModClient;
import de.teamlapen.vampirism.client.renderer.VampirismClientEntityRegistry;
import de.teamlapen.vampirism.config.VampirismConfig;
import de.teamlapen.vampirism.core.*;
import de.teamlapen.vampirism.data.reloadlistener.BloodValuesReloadListener;
import de.teamlapen.vampirism.data.reloadlistener.SingleJigsawReloadListener;
import de.teamlapen.vampirism.data.reloadlistener.SkillTreeReloadListener;
import de.teamlapen.vampirism.data.reloadlistener.SundamageReloadListener;
import de.teamlapen.vampirism.entity.ExtendedCreature;
import de.teamlapen.vampirism.entity.ModEntityEventHandler;
import de.teamlapen.vampirism.entity.SundamageRegistry;
import de.teamlapen.vampirism.entity.action.ActionManagerEntity;
import de.teamlapen.vampirism.entity.converted.DefaultConvertingHandler;
import de.teamlapen.vampirism.entity.converted.VampirismEntityRegistry;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import de.teamlapen.vampirism.entity.factions.FactionRegistry;
import de.teamlapen.vampirism.entity.minion.HunterMinionEntity;
import de.teamlapen.vampirism.entity.minion.VampireMinionEntity;
import de.teamlapen.vampirism.entity.player.ModPlayerEventHandler;
import de.teamlapen.vampirism.entity.player.actions.ActionManager;
import de.teamlapen.vampirism.entity.player.hunter.HunterPlayer;
import de.teamlapen.vampirism.entity.player.skills.SkillManager;
import de.teamlapen.vampirism.entity.player.vampire.BloodVision;
import de.teamlapen.vampirism.entity.player.vampire.NightVision;
import de.teamlapen.vampirism.entity.player.vampire.VampirePlayer;
import de.teamlapen.vampirism.items.BloodBottleFluidHandler;
import de.teamlapen.vampirism.items.BloodBottleItem;
import de.teamlapen.vampirism.items.VampireRefinementItem;
import de.teamlapen.vampirism.items.crossbow.CrossbowArrowHandler;
import de.teamlapen.vampirism.misc.SettingsProvider;
import de.teamlapen.vampirism.misc.VampirismLogger;
import de.teamlapen.vampirism.mixin.ReloadableServerResourcesAccessor;
import de.teamlapen.vampirism.mixin.TagManagerAccessor;
import de.teamlapen.vampirism.modcompat.IMCHandler;
import de.teamlapen.vampirism.modcompat.terrablender.TerraBlenderCompat;
import de.teamlapen.vampirism.network.ModPacketDispatcher;
import de.teamlapen.vampirism.proxy.ClientProxy;
import de.teamlapen.vampirism.proxy.IProxy;
import de.teamlapen.vampirism.proxy.ServerProxy;
import de.teamlapen.vampirism.recipes.ExtendedBrewingRecipeRegistry;
import de.teamlapen.vampirism.sit.SitHandler;
import de.teamlapen.vampirism.util.*;
import de.teamlapen.vampirism.world.biome.OverworldModifications;
import de.teamlapen.vampirism.world.gen.VanillaStructureModifications;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.*;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Main class for Vampirism
 */
@Mod(value = REFERENCE.MODID)
public class VampirismMod {

    private static final Logger LOGGER = LogManager.getLogger();

    public static VampirismMod instance;
    public static final IProxy proxy = FMLEnvironment.dist == Dist.CLIENT ? VampirismModClient.getProxy() : new ServerProxy();
    public static boolean inDev = false;
    public static boolean inDataGen = false;

    private final @NotNull RegistryManager registryManager;
    private final IEventBus modBus;


    public VampirismMod(IEventBus modEventBus) {
        instance = this;
        checkEnv();

        this.modBus = modEventBus;

        this.registryManager = new RegistryManager(modEventBus);

        this.modBus.addListener(this::setup);
        this.modBus.addListener(this::enqueueIMC);
        this.modBus.addListener(this::processIMC);
        this.modBus.addListener(this::loadComplete);
        this.modBus.addListener(this::registerCapabilities);
        this.modBus.addListener(this::finalizeConfiguration);
        this.modBus.addListener(VersionUpdater::catchModVersionMismatch);
        this.modBus.register(ModPacketDispatcher.class);
        this.modBus.register(MigrationData.class);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            this.modBus.register(new VampirismModClient(modEventBus, this.registryManager));
        }

        NeoForge.EVENT_BUS.register(Permissions.class);
        NeoForge.EVENT_BUS.register(SitHandler.class);
        NeoForge.EVENT_BUS.register(new GeneralEventHandler());
        NeoForge.EVENT_BUS.addListener(this::onCommandsRegister);
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListenerEvent);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(VersionUpdater::checkVersionUpdated);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
        NeoForge.EVENT_BUS.addListener(this::onDataPackSyncEvent);

        VampirismConfig.init();
        ShapedRecipePattern.setCraftingSize(4,4);

        prepareAPI();
        this.registryManager.setupRegistries();
        this.modBus.addListener(ModItems::registerOtherCreativeTabItems);

        if (OptifineHandler.isOptifineLoaded()) {
            LOGGER.warn("Using Optifine. Expect visual glitches and reduces blood vision functionality if using shaders.");
        }
    }

    public void onAddReloadListenerEvent(@NotNull AddReloadListenerEvent event) {
        event.addListener(new BloodValuesReloadListener());
        event.addListener(new SingleJigsawReloadListener());
        event.addListener(new SundamageReloadListener(((TagManagerAccessor) ((ReloadableServerResourcesAccessor) event.getServerResources()).getTagManager()).getRegistryAccess()));
        event.addListener(new SkillTreeReloadListener(event.getConditionContext(), event.getRegistryAccess()));
    }

    public void onCommandsRegister(@NotNull RegisterCommandsEvent event) {
        ModCommands.registerCommands(event.getDispatcher(), event.getBuildContext());
    }

    private void checkEnv() {
        String launchTarget = System.getProperty("vampirism_target");
        if (launchTarget != null && launchTarget.contains("dev")) {
            inDev = true;
        }
        if (launchTarget != null && launchTarget.contains("data")) {
            inDataGen = true;
        }
    }

    @SuppressWarnings("unchecked")
    private void enqueueIMC(final @NotNull InterModEnqueueEvent event) {
        onInitStep(IInitListener.Step.ENQUEUE_IMC, event);
        HelperRegistry.registerPlayerEventReceivingCapability((AttachmentType<IPlayerEventListener>) (Object) ModAttachments.VAMPIRE_PLAYER.get(), VampirePlayer.class);
        HelperRegistry.registerPlayerEventReceivingCapability((AttachmentType<IPlayerEventListener>) (Object) ModAttachments.HUNTER_PLAYER.get(), HunterPlayer.class);
        HelperRegistry.registerSyncableEntityCapability((AttachmentType<ISyncable.ISyncableAttachment>) (Object) ModAttachments.EXTENDED_CREATURE.get(), ExtendedCreature.class);
        HelperRegistry.registerSyncablePlayerCapability((AttachmentType<ISyncable.ISyncableAttachment>) (Object) ModAttachments.VAMPIRE_PLAYER.get(), VampirePlayer.class);
        HelperRegistry.registerSyncablePlayerCapability((AttachmentType<ISyncable.ISyncableAttachment>) (Object) ModAttachments.HUNTER_PLAYER.get(), HunterPlayer.class);
        HelperRegistry.registerSyncablePlayerCapability((AttachmentType<ISyncable.ISyncableAttachment>) (Object) ModAttachments.FACTION_PLAYER_HANDLER.get(), FactionPlayerHandler.class);
    }

    private void registerCapabilities(@NotNull RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.FluidHandler.ITEM, (item, b) -> new BloodBottleFluidHandler(item, BloodBottleItem.CAPACITY), ModItems.BLOOD_BOTTLE.get());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModTiles.BLOOD_CONTAINER.get(), (o, side) -> o.getTank());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModTiles.ALTAR_INSPIRATION.get(), (o, side) -> o.getTank());
        event.registerItem(Capabilities.FluidHandler.ITEM, (item, b) -> new BloodBottleFluidHandler(item, BloodBottleItem.CAPACITY), ModItems.BLOOD_BOTTLE.get());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModTiles.GRINDER.get(), (o, side) -> o.getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModTiles.BLOOD_PEDESTAL.get(), (o, side) -> o);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModTiles.SIEVE.get(), (o, side) -> o.getTank());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModTiles.POTION_TABLE.get(), new ICapabilityProvider<>() {
            @Override
            public @Nullable IItemHandler getCapability(PotionTableBlockEntity object, Direction context) {
                return object.getCapability(object, context);
            }
        });
    }

    private void onServerStarting(@NotNull ServerAboutToStartEvent event) {
        VanillaStructureModifications.addVillageStructures(event.getServer().registryAccess());
        ((SundamageRegistry) VampirismAPI.sundamageRegistry()).initServer(event.getServer().registryAccess());
    }

    private void onServerStopped(ServerStoppedEvent event) {
        ((SundamageRegistry) VampirismAPI.sundamageRegistry()).removeServer();
    }

    private void onDataPackSyncEvent(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            ((SundamageRegistry) VampirismAPI.sundamageRegistry()).updateClient(event.getPlayer());
        } else {
            event.getPlayerList().getPlayers().forEach(player -> ((SundamageRegistry) VampirismAPI.sundamageRegistry()).updateClient(player));
        }
    }

    private void finalizeConfiguration(RegisterEvent event) {
        VampirismConfig.finalizeAndRegisterConfig(this.modBus);
    }

    /**
     * Finish API during InterModProcessEvent
     */
    private void finishAPI() {
        ((FactionRegistry) VampirismAPI.factionRegistry()).finish();
        ((VampirismEntityRegistry) VampirismAPI.entityRegistry()).finishRegistration();
    }

    private void loadComplete(final @NotNull FMLLoadCompleteEvent event) {
        onInitStep(IInitListener.Step.LOAD_COMPLETE, event);
        event.enqueueWork(OverworldModifications::addBiomesToOverworldUnsafe);
        VampirismAPI.skillManager().registerSkillType(SkillType.LEVEL);
        VampirismAPI.skillManager().registerSkillType(SkillType.LORD);
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            VampirismLogger.init();
        }
    }


    /**
     * Called during constructor to set up the API as well as VReference
     */
    private void prepareAPI() {

        VampirismAPI.setUpRegistries(new FactionRegistry(), new SundamageRegistry(), FMLEnvironment.dist == Dist.CLIENT ? new VampirismClientEntityRegistry(DefaultConvertingHandler::new) : new VampirismEntityRegistry(DefaultConvertingHandler::new), new ActionManager(), new SkillManager(), new VampireVisionRegistry(), new ActionManagerEntity(), new ExtendedBrewingRecipeRegistry(), new SettingsProvider(REFERENCE.SETTINGS_API));
        if (FMLEnvironment.dist == Dist.CLIENT) {
            proxy.setupAPIClient();
        }

        VReference.VAMPIRE_FACTION = VampirismAPI.factionRegistry()
                .createPlayableFaction(VReference.VAMPIRE_FACTION_ID, IVampirePlayer.class, () -> (AttachmentType<IVampirePlayer>)(Object) ModAttachments.VAMPIRE_PLAYER.get())
                .color(Color.MAGENTA_DARK.getRGB())
                .chatColor(ChatFormatting.DARK_PURPLE)
                .name("text.vampirism.vampire")
                .namePlural("text.vampirism.vampires")
                .hostileTowardsNeutral()
                .highestLevel(REFERENCE.HIGHEST_VAMPIRE_LEVEL)
                .lord().lordLevel(REFERENCE.HIGHEST_VAMPIRE_LORD).lordTitle(LordTitles::getVampireTitle).enableLordSkills()
                .minion(VampireMinionEntity.VampireMinionData.ID).minionData(VampireMinionEntity.VampireMinionData::new).build()
                .build()
                .village(VampireVillage::vampireVillage)
                .refinementItems(VampireRefinementItem::getItemForType)
                .addTag(Registries.BIOME, ModTags.Biomes.IS_VAMPIRE_BIOME)
                .addTag(Registries.POINT_OF_INTEREST_TYPE, ModTags.PoiTypes.IS_VAMPIRE)
                .addTag(Registries.VILLAGER_PROFESSION, ModTags.Professions.IS_VAMPIRE)
                .addTag(Registries.ENTITY_TYPE, ModTags.Entities.VAMPIRE)
                .addTag(VampirismRegistries.TASK_ID, ModTags.Tasks.IS_VAMPIRE)
                .register();
        VReference.HUNTER_FACTION = VampirismAPI.factionRegistry()
                .createPlayableFaction(VReference.HUNTER_FACTION_ID, IHunterPlayer.class, () -> (AttachmentType<IHunterPlayer>)(Object) ModAttachments.HUNTER_PLAYER.get())
                .color(Color.BLUE.getRGB())
                .chatColor(ChatFormatting.BLUE)
                .name("text.vampirism.hunter")
                .namePlural("text.vampirism.hunters")
                .highestLevel(REFERENCE.HIGHEST_HUNTER_LEVEL)
                .lord().lordLevel(REFERENCE.HIGHEST_HUNTER_LORD).lordTitle(LordTitles::getHunterTitle).enableLordSkills()
                .minion(HunterMinionEntity.HunterMinionData.ID).minionData(HunterMinionEntity.HunterMinionData::new).build()
                .build()
                .village(HunterVillage::hunterVillage)
                .addTag(Registries.BIOME, ModTags.Biomes.IS_HUNTER_BIOME)
                .addTag(Registries.POINT_OF_INTEREST_TYPE, ModTags.PoiTypes.IS_HUNTER)
                .addTag(Registries.VILLAGER_PROFESSION, ModTags.Professions.IS_HUNTER)
                .addTag(Registries.ENTITY_TYPE, ModTags.Entities.HUNTER)
                .addTag(VampirismRegistries.TASK_ID, ModTags.Tasks.IS_HUNTER)
                .register();

        VReference.vision_nightVision = VampirismAPI.vampireVisionRegistry().registerVision(new ResourceLocation(REFERENCE.MODID, "night_vision"), new NightVision());
        VReference.vision_bloodVision = VampirismAPI.vampireVisionRegistry().registerVision(new ResourceLocation(REFERENCE.MODID, "blood_vision"), new BloodVision());

        VampirismAPI.onSetupComplete();
    }

    private void processIMC(final @NotNull InterModProcessEvent event) {
        finishAPI();
        onInitStep(IInitListener.Step.PROCESS_IMC, event);
        IMCHandler.handleInterModMessage(event);
        CrossbowArrowHandler.collectCrossbowArrows();
    }

    private void setup(final @NotNull FMLCommonSetupEvent event) {
        onInitStep(IInitListener.Step.COMMON_SETUP, event);

        NeoForge.EVENT_BUS.register(new ModPlayerEventHandler());

        NeoForge.EVENT_BUS.register(new ModEntityEventHandler());
        NeoForge.EVENT_BUS.addListener(ModLootTables::onLootLoad);

        SupporterManager.init();
        VampireBookManager.getInstance().init();
        ModEntitySelectors.registerSelectors();
        event.enqueueWork(TerraBlenderCompat::registerBiomeProviderIfPresentUnsafe);
//        VanillaStructureModifications.addVillageStructures(RegistryAccess.EMPTY);

        TelemetryCollector.execute();
    }

    private void onInitStep(IInitListener.@NotNull Step step, @NotNull ParallelDispatchEvent event) {
        registryManager.onInitStep(step, event);
        proxy.onInitStep(step, event);
    }

}
