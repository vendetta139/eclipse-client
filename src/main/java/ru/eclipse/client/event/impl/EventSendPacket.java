package ru.eclipse.client.event.impl;

import net.minecraft.network.packet.Packet;
import ru.eclipse.client.event.Event;

public class EventSendPacket extends Event {
    private Packet<?> packet;

    public EventSendPacket(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public void setPacket(Packet<?> packet) {
        this.packet = packet;
    }
}
