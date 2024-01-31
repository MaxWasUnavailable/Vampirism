package de.teamlapen.vampirism.api.datamaps;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

public record ItemBlood(int blood) {
    public static final Codec<ItemBlood> NETWORK_CODEC = Codec.intRange(0, Integer.MAX_VALUE).xmap(ItemBlood::new, ItemBlood::blood);
    public static final Codec<ItemBlood> CODEC = ExtraCodecs.withAlternative(
            RecordCodecBuilder.create(inst ->
                    inst.group(
                            Codec.INT.fieldOf("blood").forGetter(ItemBlood::blood)
                    ).apply(inst, ItemBlood::new)
            ), NETWORK_CODEC);

    public ItemBlood() {
        this(0);
    }
}
