package net.mehvahdjukaar.moonlight.platform.network.fabric;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.mehvahdjukaar.moonlight.platform.network.Message;
import net.mehvahdjukaar.moonlight.platform.network.ChannelHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class FabricChannelHandler extends ChannelHandler {

    private int id = 0;

    public FabricChannelHandler(ResourceLocation channelName) {
        super(channelName);
    }

    public static Map<Class<?>, ResourceLocation> ID_MAP = new HashMap<>();

    @Override
    public <M extends Message> void register(
            NetworkDir direction,
            Class<M> messageClass,
            Function<FriendlyByteBuf, M> decoder) {

        ResourceLocation res = new ResourceLocation(this.channelName.getNamespace(), "" + id++);
        ID_MAP.put(messageClass, res);

        if (direction == NetworkDir.PLAY_TO_SERVER) {
            ServerPlayNetworking.registerGlobalReceiver(
                    res, (server, player, h, buf, r) -> server.execute(() ->
                            decoder.apply(buf).handle(new Wrapper(player, direction))));
        } else {
            FabricClientNetwork.register(res, decoder);
        }
    }


    static class Wrapper implements Context {

        private final Player player;
        private final NetworkDir dir;

        public Wrapper(Player player, NetworkDir dir) {
            this.player = player;
            this.dir = dir;
        }

        @Override
        public NetworkDir getDirection() {
            return dir;
        }

        @Override
        public Player getSender() {
            return player;
        }
    }

    public static void sendPacket(Message message, ServerPlayer player) {
        ResourceLocation id = ID_MAP.get(message.getClass());

        FriendlyByteBuf buf = PacketByteBufs.create();
        message.writeToBuffer(buf);

        ServerPlayNetworking.send(player, id, buf);

    }
}
