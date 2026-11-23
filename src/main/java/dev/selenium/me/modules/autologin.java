/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class autologin extends Module {
    private final SettingGroup iwontstealurpassword = settings.createGroup("PSW");

    private final Setting<String> psw = iwontstealurpassword.add(new StringSetting.Builder()
            .name("password")
            .defaultValue("123456")
            .build()
    );

    private boolean sent = false;
    private int timer = 0;

    public autologin() {
        super(SeleniumAddon.CATEGORY, "auto loggvro", "automatically uses /login for u");
    }

    @Override
    public void onActivate() {
        sent = false;
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) {
            sent = false;
            timer = 0;
            return;
        }


        if (!sent) {
            mc.player.networkHandler.sendChatCommand("login " + psw.get());
            sent = true;
            timer = 100;
            return;
        }


        if (timer > 0) {
            timer--;
        }
    }

    public boolean isCooldownActive() {
        return timer > 0;
    }
}
