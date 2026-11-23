package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class safeteleportmanager extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<List<String>> whitelist = sg.add(
            new StringListSetting.Builder()
                    .name("whitelist")
                    .description("players to ignore or accept")
                    .defaultValue(List.of("friend1", "friend2"))
                    .build()
    );

    private final Setting<Boolean> autoAccept = sg.add(
            new BoolSetting.Builder()
                    .name("auto-accept")
                    .description("accepts tpa from whitelisted players")
                    .defaultValue(false)
                    .build()
    );

    private final Setting<String> cmd = sg.add(
            new StringSetting.Builder()
                    .name("cmd")
                    .description("command used to deny tpa")
                    .defaultValue("tpdeny")
                    .build()
    );

    private final Setting<String> acceptCmd = sg.add(
            new StringSetting.Builder()
                    .name("accept-cmd")
                    .description("command used to accept tpa")
                    .defaultValue("tpaccept")
                    .visible(autoAccept::get)
                    .build()
    );

    private final Setting<Integer> delay = sg.add(
            new IntSetting.Builder()
                    .name("delay")
                    .description("ticks before responding")
                    .defaultValue(5)
                    .min(0)
                    .max(60)
                    .build()
    );

    private final Setting<Boolean> notify = sg.add(
            new BoolSetting.Builder()
                    .name("notify")
                    .description("show tpa actions in chat")
                    .defaultValue(true)
                    .build()
    );

    private static final Pattern NAME_PATTERN = Pattern.compile("\\b([a-zA-Z0-9_]{3,16})\\b");

    private int timer = 0;
    private String target = null;
    private boolean shouldAccept = false;

    public safeteleportmanager() {
        super(SeleniumAddon.CATEGORY, "tp-manager", "rejects or accepts tpa requests");
    }

    @Override
    public void onActivate() {
        timer = 0;
        target = null;
        shouldAccept = false;
    }

    @EventHandler
    private void onMessage(ReceiveMessageEvent event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        String raw = event.getMessage().getString();
        String lower = raw.toLowerCase();

        boolean isTpa = lower.contains("has requested to teleport") ||
                lower.contains("tpa") ||
                lower.contains("tp") ||
                lower.contains("/tpaccept") ||
                lower.contains("/tpdeny");

        if (!isTpa) return;

        Matcher matcher = NAME_PATTERN.matcher(raw);
        String sender = null;

        while (matcher.find()) {
            String word = matcher.group(1);
            if (!word.equalsIgnoreCase(mc.player.getName().getString()) && !isIgnored(word)) {
                sender = word;
                break;
            }
        }

        if (sender == null) return;

        boolean isWhitelisted = false;
        for (String allowed : whitelist.get()) {
            if (allowed.equalsIgnoreCase(sender)) {
                isWhitelisted = true;
                break;
            }
        }

        if (isWhitelisted) {
            if (autoAccept.get()) {
                target = sender;
                shouldAccept = true;
                timer = 0;
            }
            return;
        }

        target = sender;
        shouldAccept = false;
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.getNetworkHandler() == null || target == null) return;

        if (timer < delay.get()) {
            timer++;
            return;
        }

        if (shouldAccept) {
            String command = acceptCmd.get().startsWith("/") ? acceptCmd.get().substring(1) : acceptCmd.get();
            mc.getNetworkHandler().sendChatCommand(command + " " + target);

            if (notify.get()) {
                info("accepted tpa from " + target);
            }
        } else {
            String command = cmd.get().startsWith("/") ? cmd.get().substring(1) : cmd.get();
            mc.getNetworkHandler().sendChatCommand(command);

            if (notify.get()) {
                info("rejected tpa from " + target);
            }
        }

        target = null;
        shouldAccept = false;
        timer = 0;
    }

    private boolean isIgnored(String word) {
        String w = word.toLowerCase();
        return w.equals("tpa") || w.equals("tpahere") || w.equals("sistem") ||
                w.equals("uyari") || w.equals("bilgi") || w.equals("server") ||
                w.equals("vip") || w.equals("mvip") || w.equals("oyuncu");
    }
}
