package de.teamlapen.vampirism.api.entity.player.vampire;

import de.teamlapen.vampirism.player.vampire.VampirePlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Blood stats similar to FoodStats for vampire players
 */
public interface IBloodStats {
    float LOW_SATURATION = 0.3F;
    float MEDIUM_SATURATION = 0.7F;
    float HIGH_SATURATION = 1.0F;

    /**
     * @return The maximum amount of blood
     */
    int getMaxBlood();

    /**
     * Change the maximum storeable amount of blood
     * Also caps the current blood at this level
     * Probably should not be used
     *
     * @param maxBlood Should be a even number
     */
    @Deprecated
    void setMaxBlood(int maxBlood);

    /**
     * Adds blood to the stats
     * Consider using {@link VampirePlayer#drinkBlood(int, float)} instead
     *
     * @param amount
     * @param saturationModifier
     * @return The amount which could not be added
     */
    @Deprecated
    int addBlood(int amount, float saturationModifier);

    /**
     * Removes blood from the vampires blood level
     *
     * @param a amount
     * @return whether the vampire had enough blood or not
     */
    boolean consumeBlood(int a);

    /**
     * @return The current blood level
     */
    int getBloodLevel();

    /**
     * Set the blood level is clamped between 0 and maxblood
     *
     * @param amt
     */
    void setBloodLevel(int amt);

    @SideOnly(Side.CLIENT)
    int getPrevBloodLevel();

    /**
     * @return If the player could use blood
     */
    boolean needsBlood();
}
