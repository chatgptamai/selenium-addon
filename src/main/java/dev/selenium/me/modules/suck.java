package dev.selenium.me.modules;

import dev.selenium.me.SeleniumAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.InputUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class suck extends Module {
    private final Random random = new Random();
    private int[] mapping = new int[]{1, 2, 3, 0};
    private boolean[] wasPhysicallyPressed = new boolean[4];

    public suck() {
        super(SeleniumAddon.CATEGORY, "suck", "");
    }

    @Override
    public void onActivate() {
        shuffleKeys();

        wasPhysicallyPressed = new boolean[4];
    }

    @Override
    public void onDeactivate() {
        if (mc.options == null) return;
        long handle = mc.getWindow().getHandle();
        mc.options.forwardKey.setPressed(isKeyDown(handle, mc.options.forwardKey.getBoundKeyTranslationKey()));
        mc.options.leftKey.setPressed(isKeyDown(handle, mc.options.leftKey.getBoundKeyTranslationKey()));
        mc.options.backKey.setPressed(isKeyDown(handle, mc.options.backKey.getBoundKeyTranslationKey()));
        mc.options.rightKey.setPressed(isKeyDown(handle, mc.options.rightKey.getBoundKeyTranslationKey()));
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;

        long handle = mc.getWindow().getHandle();

        boolean[] physical = new boolean[4];
        physical[0] = isKeyDown(handle, mc.options.forwardKey.getBoundKeyTranslationKey());
        physical[1] = isKeyDown(handle, mc.options.leftKey.getBoundKeyTranslationKey());
        physical[2] = isKeyDown(handle, mc.options.backKey.getBoundKeyTranslationKey());
        physical[3] = isKeyDown(handle, mc.options.rightKey.getBoundKeyTranslationKey());


        boolean newPressDetected = false;
        for (int i = 0; i < 4; i++) {
            if (physical[i] && !wasPhysicallyPressed[i]) {
                newPressDetected = true;
            }
            wasPhysicallyPressed[i] = physical[i];
        }

        if (newPressDetected) {
            shuffleKeys();
        }

        boolean[] virtualPressed = new boolean[4];
        for (int i = 0; i < 4; i++) {
            if (physical[i]) {
                virtualPressed[mapping[i]] = true;
            }
        }

        mc.options.forwardKey.setPressed(virtualPressed[0]);
        mc.options.leftKey.setPressed(virtualPressed[1]);
        mc.options.backKey.setPressed(virtualPressed[2]);
        mc.options.rightKey.setPressed(virtualPressed[3]);
    }

    private boolean isKeyDown(long windowHandle, String translationKey) {
        InputUtil.Key key = InputUtil.fromTranslationKey(translationKey);
        if (key.getCategory() == InputUtil.Type.KEYSYM && key.getCode() != -1) {
            return InputUtil.isKeyPressed(windowHandle, key.getCode());
        }
        return false;
    }


    private void shuffleKeys() {
        List<Integer> list = new ArrayList<>(List.of(0, 1, 2, 3));
        boolean valid = false;

        while (!valid) {
            Collections.shuffle(list, random);
            valid = true;
            for (int i = 0; i < 4; i++) {
                if (list.get(i) == i) {
                    valid = false;
                    break;
                }
            }
        }

        for (int i = 0; i < 4; i++) {
            mapping[i] = list.get(i);
        }
    }
}
