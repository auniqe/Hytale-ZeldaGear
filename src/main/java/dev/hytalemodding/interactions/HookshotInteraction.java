package dev.hytalemodding.interactions;


import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;

public class HookshotInteraction extends SimpleInteraction {

    public static final BuilderCodec<HookshotInteraction> CODEC = BuilderCodec.builder(
            HookshotInteraction.class, HookshotInteraction::new, SimpleInteraction.CODEC
    ).build();

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    protected void firstRun(InteractionType interactionType, InteractionContext context, CooldownHandler cooldownHandler) {
        if (context.getCommandBuffer() == null) {
            context.getState().state = InteractionState.Failed;
            LOGGER.atInfo().log("CommandBuffer is null");
            return;
        }

        // Example hookshot logic
        LOGGER.atInfo().log("HookshotInteraction triggered!");
    }
}