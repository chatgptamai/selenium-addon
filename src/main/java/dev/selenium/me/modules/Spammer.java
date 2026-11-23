package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;

import java.util.List;
import java.util.Random;

public class Spammer extends Module {
    public enum Mode {
        Singleplayer,
        Paper
    }

    private final SettingGroup sgGeneral = settings.createGroup("imthebest");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("mode")
            .defaultValue(Mode.Paper)
            .build()
    );

    private final Setting<List<String>> messages = sgGeneral.add(new StringListSetting.Builder()
            .name("messages")
            .defaultValue(List.of(
                    "Selenium kullanmayan adammıdır la?"
            ))
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .defaultValue(0)
            .min(0)
            .max(400)
            .sliderRange(0, 100)
            .build()
    );

    private final Setting<Integer> perTick = sgGeneral.add(new IntSetting.Builder()
            .name("per tick")
            .defaultValue(1)
            .min(1)
            .max(100)
            .sliderRange(1, 50)
            .build()
    );

    private final Setting<Boolean> randomOrder = sgGeneral.add(new BoolSetting.Builder()
            .name("randomize")
            .defaultValue(false)
            .build()
    );

    private final Random random = new Random();
    private int timer = 0;
    private int currentIndex = 0;

    public Spammer() {
        super(SeleniumAddon.CATEGORY, "spammer", "spam vro");
    }

    @Override
    public void onActivate() {
        timer = 0;
        currentIndex = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;
        List<String> list = messages.get();
        if (list.isEmpty()) return;

        int currentDelay = delay.get();
        if (currentDelay > 0 && timer > 0) {
            timer--;
            return;
        }

        int count = (mode.get() == Mode.Paper) ? Math.min(perTick.get(), 5) : perTick.get();
        for (int i = 0; i < count; i++) {
            String raw = randomOrder.get()
                    ? list.get(random.nextInt(list.size()))
                    : list.get(currentIndex++ % list.size());
            dispatch(raw);
        }

        timer = currentDelay;
    }

    private void dispatch(String raw) {
        if (mc.player == null || mc.world == null || raw == null || raw.isBlank()) return;

        if (raw.startsWith("/")) {
            String command = raw.substring(1).trim();
            if (!command.isEmpty()) {
                mc.execute(() -> {
                    if (mc.player != null && mc.player.networkHandler != null) {
                        mc.player.networkHandler.sendChatCommand(command);
                    }
                });
            }
        } else {
            mc.execute(() -> {
                if (mc.player != null && mc.world != null) {
                    ChatUtils.sendPlayerMsg(raw);
                }
            });
        }
    }
}