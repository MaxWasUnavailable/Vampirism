package de.teamlapen.vampirism.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import de.teamlapen.vampirism.blockentity.BatCageBlockEntity;
import de.teamlapen.vampirism.blocks.BatCageBlock;
import net.minecraft.client.model.BatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.Bat;

public class BatCageBESR extends VampirismBESR<BatCageBlockEntity> {

    private final BatModel model;

    public BatCageBESR(BlockEntityRendererProvider.Context context) {
        model = new BatModel(context.bakeLayer(ModelLayers.BAT));
    }

    @Override
    public void render(BatCageBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        RenderType renderType = this.model.renderType(new ResourceLocation("textures/entity/bat.png"));
        Direction value = pBlockEntity.getBlockState().getValue(BatCageBlock.FACING);
        VertexConsumer buffer = pBuffer.getBuffer(renderType);
        pPoseStack.pushPose();
        pPoseStack.translate(0.5F, 0.2F, 0.5F);
        pPoseStack.mulPose(Axis.YN.rotationDegrees(90 * value.get2DDataValue()));
        pPoseStack.scale(0.35F, 0.35F, 0.35F);
        pPoseStack.mulPose(Axis.XP.rotationDegrees(180));
        Bat bat = EntityType.BAT.create(pBlockEntity.getLevel());
        bat.setResting(true);
        this.model.setupAnim(bat, 0, 0, 0, -1, -1);
        this.model.renderToBuffer(pPoseStack, buffer, pPackedLight, pPackedOverlay, 1, 1, 1, 1);
        pPoseStack.popPose();
    }
}
