package dev.hytalemodding;

import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.hytalemodding.commands.ExampleCommand;
import dev.hytalemodding.events.ExampleEvent;
import dev.hytalemodding.interactions.HookshotInteraction;

import javax.annotation.Nonnull;
import java.util.logging.Level;

public class ExamplePlugin extends JavaPlugin {

    private static ExamplePlugin instance;

    public ExamplePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this; // remember to assign the instance
    }

    public static ExamplePlugin get() {
        return instance;
    }

    @Override
    protected void setup() {
        this.getCommandRegistry().registerCommand(new ExampleCommand("example", "An example command"));
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, ExampleEvent::onPlayerReady);
        this.getCodecRegistry(Interaction.CODEC)
                .register("HookshotInteraction", HookshotInteraction.class, HookshotInteraction.CODEC);
    }

    @Override
    protected void start() {
        getLogger().at(Level.INFO).log("Plugin started!");
    }

    @Override
    protected void shutdown() {
        getLogger().at(Level.INFO).log("Plugin shutting down!");
    }
}
