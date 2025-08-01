package gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.production;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.Muffler;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.TAE;
import gregtech.api.enums.TierEU;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.config.MachineStats;
import gregtech.common.pollution.PollutionConfig;
import gtPlusPlus.api.recipe.GTPPRecipeMaps;
import gtPlusPlus.core.block.ModBlocks;
import gtPlusPlus.core.util.minecraft.PlayerUtils;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.base.GTPPMultiBlockBase;
import gtPlusPlus.xmod.gregtech.common.blocks.textures.TexturesGtBlock;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

public class MTEMassFabricator extends GTPPMultiBlockBase<MTEMassFabricator> implements ISurvivalConstructable {

    public static int sUUAperUUM = 1;
    public static int sUUASpeedBonus = 4;
    public static int sDurationMultiplier = 3200;

    public static String mCasingName1 = "Matter Fabricator Casing";
    public static String mCasingName2 = "Containment Casing";
    public static String mCasingName3 = "Matter Generation Coil";

    private int mMode = 0;

    private static final int MODE_SCRAP = 1;
    private static final int MODE_UU = 0;

    public static boolean sRequiresUUA = false;
    private static final FluidStack[] mUU = new FluidStack[2];
    private static final ItemStack[] mScrap = new ItemStack[2];

    private int mCasing;
    private static IStructureDefinition<MTEMassFabricator> STRUCTURE_DEFINITION = null;

    public MTEMassFabricator(final int aID, final String aName, final String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEMassFabricator(final String aName) {
        super(aName);
    }

    @Override
    public String getMachineType() {
        return "Mass Fabricator, Recycler";
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType(getMachineType())
            .addInfo("Speed: +0% | EU Usage: 80%")
            .addInfo("Parallel: Scrap = 64 | UU = 8 * Tier")
            .addInfo("Produces UU-A, UU-M & Scrap")
            .addInfo("Change mode with screwdriver")
            .addPerfectOCInfo()
            .addPollutionAmount(getPollutionPerSecond(null))
            .beginStructureBlock(5, 4, 5, true)
            .addController("Front Center")
            .addCasingInfoMin(mCasingName3, 9, false)
            .addCasingInfoMin(mCasingName2, 24, false)
            .addCasingInfoMin(mCasingName1, 36, false)
            .addInputBus("Any Casing", 1)
            .addOutputBus("Any Casing", 1)
            .addInputHatch("Any Casing", 1)
            .addOutputHatch("Any Casing", 1)
            .addEnergyHatch("Any Casing", 1)
            .addMaintenanceHatch("Any Casing", 1)
            .addMufflerHatch("Any Casing", 1)
            .toolTipFinisher();
        return tt;
    }

    @Override
    protected IIconContainer getActiveOverlay() {
        return TexturesGtBlock.Overlay_MatterFab_Active_Animated;
    }

    @Override
    protected IIconContainer getActiveGlowOverlay() {
        return TexturesGtBlock.Overlay_MatterFab_Active_Animated_Glow;
    }

    @Override
    protected IIconContainer getInactiveOverlay() {
        return TexturesGtBlock.Overlay_MatterFab_Animated;
    }

    @Override
    protected IIconContainer getInactiveGlowOverlay() {
        return TexturesGtBlock.Overlay_MatterFab_Animated_Glow;
    }

    @Override
    protected int getCasingTextureId() {
        return TAE.GTPP_INDEX(9);
    }

    @Override
    public void onConfigLoad() {
        super.onConfigLoad();
        sDurationMultiplier = MachineStats.massFabricator.durationMultiplier;
        sUUAperUUM = MachineStats.massFabricator.UUAPerUUM;
        sUUASpeedBonus = MachineStats.massFabricator.UUASpeedBonus;
        sRequiresUUA = MachineStats.massFabricator.requiresUUA;
    }

    public static boolean sInit = false;

    public static void init() {
        if (!sInit) {
            if (mScrap[0] == null) {
                mScrap[0] = ItemList.IC2_Scrap.get(1L);
            }
            if (mScrap[1] == null) {
                mScrap[1] = ItemList.IC2_Scrapbox.get(1L);
            }
            if (mUU[0] == null) {
                mUU[0] = Materials.UUAmplifier.getFluid(100);
            }
            if (mUU[1] == null) {
                mUU[1] = Materials.UUMatter.getFluid(100);
            }
            sInit = true;
        }
    }

    @Override
    public IStructureDefinition<MTEMassFabricator> getStructureDefinition() {
        if (STRUCTURE_DEFINITION == null) {
            STRUCTURE_DEFINITION = StructureDefinition.<MTEMassFabricator>builder()
                .addShape(
                    mName,
                    transpose(
                        new String[][] { { "CCCCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC" },
                            { "CGGGC", "G---G", "G---G", "G---G", "CGGGC" },
                            { "CGGGC", "G---G", "G---G", "G---G", "CGGGC" },
                            { "CC~CC", "CHHHC", "CHHHC", "CHHHC", "CCCCC" }, }))
                .addElement(
                    'C',
                    buildHatchAdder(MTEMassFabricator.class)
                        .atLeast(InputBus, OutputBus, InputHatch, OutputHatch, Maintenance, Energy, Muffler)
                        .casingIndex(TAE.GTPP_INDEX(9))
                        .dot(1)
                        .buildAndChain(onElementPass(x -> ++x.mCasing, ofBlock(ModBlocks.blockCasingsMisc, 9))))
                .addElement('H', ofBlock(ModBlocks.blockCasingsMisc, 8))
                .addElement('G', ofBlock(ModBlocks.blockCasings3Misc, 15))
                .build();
        }
        return STRUCTURE_DEFINITION;
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(mName, stackSize, hintsOnly, 2, 3, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(mName, stackSize, 2, 3, 0, elementBudget, env, false, true);
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        mCasing = 0;
        return checkPiece(mName, 2, 3, 0) && mCasing >= 36 && checkHatch();
    }

    @Override
    public int getPollutionPerSecond(final ItemStack aStack) {
        return PollutionConfig.pollutionPerSecondMultiMassFabricator;
    }

    @Override
    public IMetaTileEntity newMetaEntity(final IGregTechTileEntity aTileEntity) {
        return new MTEMassFabricator(this.mName);
    }

    /**
     * Special Recipe Handling
     */
    @Override
    public RecipeMap<?> getRecipeMap() {
        return this.mMode == MODE_SCRAP ? RecipeMaps.recyclerRecipes : GTPPRecipeMaps.multiblockMassFabricatorRecipes;
    }

    @Nonnull
    @Override
    public Collection<RecipeMap<?>> getAvailableRecipeMaps() {
        return Arrays.asList(RecipeMaps.recyclerRecipes, GTPPRecipeMaps.multiblockMassFabricatorRecipes);
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic() {

            @NotNull
            @Override
            public CheckRecipeResult process() {
                init();
                return super.process();
            }

            @NotNull
            @Override
            protected CheckRecipeResult validateRecipe(@NotNull GTRecipe recipe) {
                if (mMode == MODE_SCRAP) {
                    if (recipe.mOutputs == null) {
                        return SimpleCheckRecipeResult.ofSuccess("no_scrap");
                    }
                }
                return CheckRecipeResultRegistry.SUCCESSFUL;
            }

            @Nonnull
            @Override
            protected Stream<GTRecipe> findRecipeMatches(@Nullable RecipeMap<?> map) {
                if (mMode == MODE_SCRAP) {
                    if (inputItems != null) {
                        for (ItemStack item : inputItems) {
                            if (item == null || item.stackSize == 0) continue;
                            ItemStack aPotentialOutput = GTModHandler
                                .getRecyclerOutput(GTUtility.copyAmount(1, item), 0);
                            GTRecipe recipe = new GTRecipe(
                                false,
                                new ItemStack[] { GTUtility.copyAmount(1, item) },
                                aPotentialOutput == null ? null : new ItemStack[] { aPotentialOutput },
                                null,
                                new int[] { 2000 },
                                null,
                                null,
                                40,
                                (int) TierEU.RECIPE_LV,
                                0);
                            return Stream.of(recipe);
                        }
                    }
                    return Stream.empty();
                }
                return super.findRecipeMatches(map);
            }
        }.setEuModifier(0.8F)
            .setMaxParallelSupplier(this::getTrueParallel);
    }

    @Override
    protected void setupProcessingLogic(ProcessingLogic logic) {
        super.setupProcessingLogic(logic);
        logic.enablePerfectOverclock();
    }

    @Override
    public int getMaxParallelRecipes() {
        return this.mMode == MODE_SCRAP ? 64 : 8 * (Math.max(1, GTUtility.getTier(getMaxInputVoltage())));
    }

    @Override
    public void onModeChangeByScrewdriver(ForgeDirection side, EntityPlayer aPlayer, float aX, float aY, float aZ) {
        int aMode = this.mMode + 1;
        if (aMode > 1) {
            this.mMode = MODE_UU;
            PlayerUtils.messagePlayer(aPlayer, "Mode [" + this.mMode + "]: Matter/AmpliFabricator");
        } else if (aMode == 1) {
            this.mMode = MODE_SCRAP;
            PlayerUtils.messagePlayer(aPlayer, "Mode [" + this.mMode + "]: Recycler");
        } else {
            this.mMode = MODE_SCRAP;
            PlayerUtils.messagePlayer(aPlayer, "Mode [" + this.mMode + "]: Recycler");
        }
        mLastRecipe = null;
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        aNBT.setInteger("mMode", mMode);
        super.saveNBTData(aNBT);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        mMode = aNBT.getInteger("mMode");
        super.loadNBTData(aNBT);
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        tag.setInteger("mode", mMode);
    }

    @Override
    public void getWailaBody(ItemStack itemStack, List<String> currentTip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        super.getWailaBody(itemStack, currentTip, accessor, config);
        final NBTTagCompound tag = accessor.getNBTData();
        currentTip.add(
            StatCollector.translateToLocal("GT5U.machines.oreprocessor1") + " "
                + EnumChatFormatting.WHITE
                + StatCollector.translateToLocal("GT5U.GTPP_MULTI_MASS_FABRICATOR.mode." + tag.getInteger("mode"))
                + EnumChatFormatting.RESET);
    }
}
