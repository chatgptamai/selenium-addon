package dev.selenium.me;

import dev.selenium.me.modules.*;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SeleniumAddon extends MeteorAddon {
    public static final Category CATEGORY = new Category("UXP");
    public static final Category Combat = new Category("CombatVRO");
    public static final Category Extra = new Category("Extra");
    public static final Logger LOG = LoggerFactory.getLogger("SeleniumAddon");

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
        Modules.registerCategory(Combat);
        Modules.registerCategory(Extra);
    }

    @Override
    public void onInitialize() {
        Modules modules = Modules.get();
        modules.add(new AutoFollow());
        modules.add(new autologin());
        modules.add(new MegaAura());
        modules.add(new noswing());
        modules.add(new AutoSex());
        modules.add(new test());
        modules.add(new runtospace());
        modules.add(new safeteleportmanager());
        modules.add(new Spammer());
        modules.add(new funvro());
        modules.add(new antimacetest123());
        modules.add(new AutoMace());
        modules.add(new UltraCrystal());
        modules.add(new LightningRodDupe());
        modules.add(new TickShifter());
        modules.add(new ItemNameSucker());
        modules.add(new Twerk());
        modules.add(new Beyblade());
        modules.add(new BunnyHop());
        modules.add(new suck());
        modules.add(new BackHole());
        modules.add(new headfucker());
        modules.add(new PanicMode());
        modules.add(new DrunkWalk());
        modules.add(new autoscrool());
        modules.add(new Uwuuu());
        modules.add(new Paranoia());
        modules.add(new Statue());
        modules.add(new sperm());
        modules.add(new SilentFrame());
        modules.add(new ItemFrameDupe());
        modules.add(new ReplenishPlus());
        modules.add(new macetotemsuckers());
        modules.add(new InfiniteAura());
        modules.add(new AntiPop());
        modules.add(new DeathOPTotem());
        modules.add(new eclink());
        modules.add(new sikeletonesp());
        modules.add(new ClickTP());
        modules.add(new gptroom());
        modules.add(new Fuckback());
        modules.add(new FlightVroO());
        modules.add(new AutoSex());
        modules.add(new ezvro());
        modules.add(new autodoor());
        modules.add(new craftlink());
        modules.add(new AutoLink());
    }

    @Override
    public String getPackage() {
        return "dev.selenium.me";
    }
}
