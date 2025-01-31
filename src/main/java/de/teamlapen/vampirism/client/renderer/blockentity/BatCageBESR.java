package de.teamlapen.vampirism.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.teamlapen.vampirism.blockentity.BatCageBlockEntity;
import de.teamlapen.vampirism.blocks.BatCageBlock;
import net.minecraft.client.model.BatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BatCageBESR extends VampirismBESR<BatCageBlockEntity> {

    private final BatModel model;
    private Bat bat;

    public BatCageBESR(BlockEntityRendererProvider.Context context) {
        model = new BatModel(context.bakeLayer(ModelLayers.BAT));
    }

    @Override
    public void render(BatCageBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        BlockState blockState = pBlockEntity.getBlockState();
        if (blockState.getValue(BatCageBlock.CONTAINS_BAT)) {
            checkBat(pBlockEntity.getLevel());
            renderBat(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, blockState.getValue(BatCageBlock.FACING));
        }
    }

    private void checkBat(Level pLevel) {
        if (bat == null) {
            this.bat = EntityType.BAT.create(pLevel);
            this.bat.setResting(true);
        }
    }

    private void renderBat(PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay, Direction direction) {
        pPoseStack.pushPose();
        pPoseStack.translate(0.5F, 0.2F, 0.5F);
        pPoseStack.mulPose(Axis.YN.rotationDegrees(90 * direction.get2DDataValue()));
        pPoseStack.scale(0.35F, 0.35F, 0.35F);
        pPoseStack.mulPose(Axis.XP.rotationDegrees(180));
        this.model.setupAnim(this.bat, 0, 0, 0, -1, -1);
        this.model.renderToBuffer(pPoseStack, pBuffer.getBuffer(this.model.renderType(new ResourceLocation("textures/entity/bat.png"))), pPackedLight, pPackedOverlay, 1, 1, 1, 1);
        pPoseStack.popPose();
    }
}
