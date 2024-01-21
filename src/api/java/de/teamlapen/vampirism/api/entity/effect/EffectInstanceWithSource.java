package de.teamlapen.vampirism.api.entity.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * mixin interface that will be implemented in {@link MobEffectInstance}
 */
public interface EffectInstanceWithSource {

    /**
     * @return the hidden effect of the effect instance
     */
    @Nullable
    MobEffectInstance getHiddenEffect();

    /**
     * @return the source of this effect instance
     */
    @Nullable
    ResourceLocation getSource();

    /**
     * sets the source of this effect instance
     *
     * @param source the id of the source
     */
    void setSource(@Nullable ResourceLocation source);

    /**
     * @return if this effect instance has a defined source
     */
    boolean hasSource();

    /**
     * sets the duration to 1,
     * which means that the effect will be removed in the next tick and the next hidden effect is applied
     */
    void removeEffect();

    static void removePotionEffect(@NotNull LivingEntity entity, @NotNull MobEffect effect, @NotNull ResourceLocation source) {
        MobEffectInstance ins = entity.getEffect(effect);
        while (ins != null) {
            EffectInstanceWithSource insM = ((EffectInstanceWithSource) ins);
            if (insM.hasSource()) {
                if (insM.getSource().equals(source)) {
                    insM.removeEffect();
                    break;
                }
            }
            ins = insM.getHiddenEffect();
        }
    }
}
