package de.teamlapen.vampirism.client.render.entities;

import com.google.common.base.Predicates;
import com.mojang.blaze3d.matrix.MatrixStack;
import de.teamlapen.vampirism.client.model.MinionModel;
import de.teamlapen.vampirism.client.render.layers.HunterEquipmentLayer;
import de.teamlapen.vampirism.client.render.layers.PlayerBodyOverlayLayer;
import de.teamlapen.vampirism.entity.minion.HunterMinionEntity;
import de.teamlapen.vampirism.util.REFERENCE;
import net.minecraft.client.renderer.entity.BipedRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * There are differently looking level 0 hunters.
 * Hunter as of level 1 look all the same, but have different weapons
 */
@OnlyIn(Dist.CLIENT)
public class HunterMinionRenderer extends BipedRenderer<HunterMinionEntity, MinionModel<HunterMinionEntity>> {
    private final ResourceLocation texture = new ResourceLocation(REFERENCE.MODID, "textures/entity/hunter_base1.png");
    private final ResourceLocation[] textures = {
            new ResourceLocation(REFERENCE.MODID, "textures/entity/hunter_base2.png"),
            new ResourceLocation(REFERENCE.MODID, "textures/entity/hunter_base3.png"),
            new ResourceLocation(REFERENCE.MODID, "textures/entity/hunter_base4.png"),
            new ResourceLocation(REFERENCE.MODID, "textures/entity/hunter_base5.png")
    };
    private final ResourceLocation textureExtra = new ResourceLocation(REFERENCE.MODID, "textures/entity/hunter_extra.png");

    public HunterMinionRenderer(EntityRendererManager renderManagerIn) {
        super(renderManagerIn, new MinionModel<>(0.5f, false), 0.5F);
        this.addLayer(new PlayerBodyOverlayLayer<>(this));
        this.addLayer(new HunterEquipmentLayer<>(this, Predicates.alwaysFalse(), entityModel -> 1));
    }

    @Override
    protected void preRenderCallback(HunterMinionEntity entityIn, MatrixStack matrixStackIn, float partialTickTime) {
        float s = entityIn.getScale();
        //float off = (1 - s) * 1.95f;
        matrixStackIn.scale(s, s, s);
        //matrixStackIn.translate(0,off,0f);
    }


    //    @Override
    //    protected ResourceLocation getEntityTexture(HunterMinion entity) {
    //        int level = entity.getLevel();
    //        if (level > 0) return texture;
    //        return textures[entity.getEntityTextureType() % textures.length];
    //    }
    //
}