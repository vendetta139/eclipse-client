package ru.eclipse.client.mixin;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.eclipse.client.event.EventBus;
import ru.eclipse.client.event.impl.EventReceivePacket;
import ru.eclipse.client.event.impl.EventSendPacket;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onPacketSend(Packet<?> packet, CallbackInfo ci) {
        EventSendPacket event = EventBus.post(new EventSendPacket(packet));
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onPacketReceive(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
        EventReceivePacket event = EventBus.post(new EventReceivePacket(packet));
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
