package dev.selenium.me.event;

import net.minecraft.entity.player.PlayerEntity;

public class DeathEvent {
    private static final DeathEvent INSTANCE = new DeathEvent();

    public PlayerEntity player;
    public boolean isSelf;

    public static DeathEvent get(PlayerEntity player, boolean isSelf) {
        INSTANCE.player = player;
        INSTANCE.isSelf = isSelf;
        return INSTANCE;
    }
}