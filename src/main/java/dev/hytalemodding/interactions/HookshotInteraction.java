package dev.hytalemodding.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.meta.MetaKey;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;

import javax.annotation.Nonnull;

public class HookshotInteraction extends SimpleInteraction {

    public static final BuilderCodec<HookshotInteraction> CODEC = BuilderCodec.builder(
            HookshotInteraction.class, HookshotInteraction::new, SimpleInteraction.CODEC
    ).build();

    private final double pullSpeed = 25; //blocks per second

    public static final String INTERACTION_NAME = "HookshotInteraction";

    //set up this functions data
    public static final MetaKey<Vector3d> HIT_LOCATION = CONTEXT_META_REGISTRY.registerMetaObject(data -> null);

    @Override
    protected void tick0(
        boolean firstRun,
        float time,
        @Nonnull InteractionType type,
        @Nonnull InteractionContext context,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        float predicted_time_to_target = 1.5f;

        //prefunction setup
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        Player player = commandBuffer.getComponent(context.getEntity(), Player.getComponentType());
        Ref<EntityStore> ref = player.getReference();
        Store<EntityStore> playerStore = player.getReference().getStore();
        InteractionSyncData contextState = context.getState();

        //initial setup
        if (firstRun) {
            //raycast
            Vector3d hitLocation = TargetUtil.getTargetLocation(player.getReference(), blockId -> blockId != 0, 30.0, playerStore);

            //cancel if not hit
            if(hitLocation == null) return;

            //set data
            context.getMetaStore().putMetaObject(HIT_LOCATION, hitLocation);
        }

        //handle pulling
        contextState.state = InteractionState.NotFinished;
        Vector3d target = context.getMetaStore().getMetaObject(HIT_LOCATION);
        pullPlayer(player, target);

        //end when at target or if predicted time was run out
        Vector3d current = playerStore.getComponent(ref, TransformComponent.getComponentType()).getPosition();
        double distance = target.distanceTo(current);
        if (distance < 1.5 || time > predicted_time_to_target) {
            contextState.state = InteractionState.Finished;
        }
    }

    private void pullPlayer(Player player, Vector3d target) {
        if(player.getReference() == null) return;
        if(player.getWorld() == null) return;

        //run on world thread to modify player
        player.getWorld().execute(() -> {
            Store<EntityStore> store = player.getReference().getStore();

            // Get player transform
            var transform = store.getComponent(player.getReference(), TransformComponent.getComponentType());
            if (transform == null) return;

            //calculate difference between position and destination
            Vector3d current = transform.getPosition();
            Vector3d toTarget = target.clone().subtract(current);
            double distance = toTarget.length();
            Vector3d step = toTarget.normalize().scale(pullSpeed);

            //stop player at destination (avoids launching them off the sides)
            if (distance < 1.8) {
                step = new Vector3d(0,0,0);
            }

            //set velocity
            Velocity velo = store.getComponent(player.getReference(), Velocity.getComponentType());
            velo.addInstruction(step, null, ChangeVelocityType.Set);

        });
    }
}
