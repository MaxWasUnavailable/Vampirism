package de.teamlapen.vampirism.world;

import com.google.common.collect.Sets;
import de.teamlapen.lib.util.Color;
import de.teamlapen.vampirism.network.ClientboundUpdateMultiBossEventPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ServerMultiBossEvent extends MultiBossEvent {

    private final Set<ServerPlayer> players = Sets.newHashSet();
    private boolean visible = true;


    public ServerMultiBossEvent(Component nameIn, BossEvent.BossBarOverlay overlayIn, Color... entries) {
        super(Mth.createInsecureUUID(), nameIn, overlayIn, entries);
    }

    public void addPlayer(ServerPlayer player) {
        if (this.players.add(player) && this.visible) {
            player.connection.send(new ClientboundUpdateMultiBossEventPacket(ClientboundUpdateMultiBossEventPacket.OperationType.ADD, this));
        }
    }

    @Override
    public void clear() {
        super.clear();
        this.sendUpdate(ClientboundUpdateMultiBossEventPacket.OperationType.UPDATE_PROGRESS);
    }

    public @NotNull Set<ServerPlayer> getPlayers() {
        return players;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            this.visible = visible;

            for (ServerPlayer player : this.players) {
                player.connection.send(new ClientboundUpdateMultiBossEventPacket(visible ? ClientboundUpdateMultiBossEventPacket.OperationType.ADD : ClientboundUpdateMultiBossEventPacket.OperationType.REMOVE, this));
            }
        }
    }

    public void removePlayer(ServerPlayer player) {
        if (this.players.remove(player) && this.visible) {
            player.connection.send(new ClientboundUpdateMultiBossEventPacket(ClientboundUpdateMultiBossEventPacket.OperationType.REMOVE, this));
        }
    }

    @Override
    public void setColors(Color... entries) {
        super.setColors(entries);
        this.sendUpdate(ClientboundUpdateMultiBossEventPacket.OperationType.ADD);
    }

    @Override
    public void setName(Component name) {
        super.setName(name);
        this.sendUpdate(ClientboundUpdateMultiBossEventPacket.OperationType.UPDATE_NAME);
    }

    @Override
    public void setOverlay(BossEvent.BossBarOverlay overlay) {
        super.setOverlay(overlay);
        this.sendUpdate(ClientboundUpdateMultiBossEventPacket.OperationType.UPDATE_STYLE);
    }

    @Override
    public void setPercentage(Color color, float perc) {
        super.setPercentage(color, perc);
        this.sendUpdate(ClientboundUpdateMultiBossEventPacket.OperationType.UPDATE_PROGRESS);
    }

    @Override
    public void setPercentage(float... perc) {
        super.setPercentage(perc);
        this.sendUpdate(ClientboundUpdateMultiBossEventPacket.OperationType.UPDATE_PROGRESS);
    }

    private void sendUpdate(ClientboundUpdateMultiBossEventPacket.OperationType operation) {
        if (this.visible) {
            ClientboundUpdateMultiBossEventPacket packet = new ClientboundUpdateMultiBossEventPacket(operation, this);

            for (ServerPlayer player : this.players) {
                player.connection.send(packet);
            }
        }
    }
}
