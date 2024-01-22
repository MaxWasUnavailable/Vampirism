package de.teamlapen.vampirism.util;

import de.teamlapen.vampirism.blockentity.diffuser.FogDiffuserBlockEntity;
import de.teamlapen.vampirism.blockentity.diffuser.GarlicDiffuserBlockEntity;
import net.minecraft.world.item.Item;

public class RegisterHelper extends de.teamlapen.lib.lib.util.RegisterHelper {

    public static <T extends Item> T fogDiffuser(int burnTime, T item) {
        FogDiffuserBlockEntity.registerBurnTime(item, burnTime);
        return item;
    }

    public static <T extends Item> T garlicDiffuser(int burnTime, T item) {
        GarlicDiffuserBlockEntity.registerBurnTime(item, burnTime);
        return item;
    }
}
