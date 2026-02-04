package net.loevi;

import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import net.loevi.interactions.GrapplingHookInteraction;
import net.loevi.interactions.HookshotInteraction;

import javax.annotation.Nonnull;
import java.util.logging.Level;

public class ZeldaGear extends JavaPlugin {
    private static ZeldaGear instance;

    //initialize plugin
    public ZeldaGear(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static ZeldaGear get() {
        return instance;
    }

    @Override
    protected void setup() {
        super.setup();

        this.getCodecRegistry(Interaction.CODEC)
                .register(HookshotInteraction.INTERACTION_NAME, HookshotInteraction.class, HookshotInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC)
                .register(GrapplingHookInteraction.INTERACTION_NAME, GrapplingHookInteraction.class, GrapplingHookInteraction.CODEC);


        getLogger().at(Level.INFO).log("Zelda Gear setup complete");
    }

    @Override
    protected void start() {
        getLogger().at(Level.INFO).log("Zelda Gear Started!");
    }

    @Override
    protected void shutdown() {
        super.shutdown();

        getLogger().at(Level.INFO).log("Zelda Gear Shutdown!");

        this.getCodecRegistry(Interaction.CODEC)
                .shutdown();

    }
}
