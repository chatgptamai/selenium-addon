package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class runtospace extends Module {
    private int a = 0;
    public runtospace(){
        super(SeleniumAddon.CATEGORY,"run-to-heaven","teleports u up");
    }
    @EventHandler
    public void onTick(TickEvent.Post event){
        if(mc.player == null || mc.world == null) return;
        if (a < 1) {
            a++;
            return;
        }
        mc.getNetworkHandler().sendChatMessage(".vclip 5");
        a = 0;
    }
}
