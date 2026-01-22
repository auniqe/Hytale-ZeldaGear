package dev.hytalemodding.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.meta.MetaKey;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
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
    private final double max_distance = 30.0;

    public static final String INTERACTION_NAME = "HookshotInteraction";

    //set up this functions data
    public static final MetaKey<Vector3d> HIT_LOCATION = CONTEXT_META_REGISTRY.registerMetaObject(data -> null);
    public static final MetaKey<Double> PREDICTED_TIME_TO_TARGET = CONTEXT_META_REGISTRY.registerMetaObject(data -> null);

    @Override
    protected void tick0(
        boolean firstRun,
        float time,
        @Nonnull InteractionType type,
        @Nonnull InteractionContext context,
        @Nonnull CooldownHandler cooldownHandler
    ) {

        //prefunction setup
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        Player player = commandBuffer.getComponent(context.getEntity(), Player.getComponentType());
        Ref<EntityStore> ref = player.getReference();
        Store<EntityStore> playerStore = player.getReference().getStore();
        InteractionSyncData contextState = context.getState();
        World world = player.getWorld();
        EntityStore ent_store = world.getEntityStore();
        TransformComponent transform = ent_store.getStore().getComponent(ref, EntityModule.get().getTransformComponentType());

        //initial setup
        if (firstRun) {
            //first shot sound
            world.execute(() -> {
                SoundUtil.playSoundEvent3d(SoundEvent.getAssetMap().getIndex("SFX_WOOD_HIT"), SoundCategory.UI, transform.getPosition(), ent_store.getStore());
                SoundUtil.playSoundEvent3d(SoundEvent.getAssetMap().getIndex("SFX_COINS_LAND"), SoundCategory.UI, transform.getPosition(), ent_store.getStore());
            });

            //raycast
            Vector3d hitLocation = TargetUtil.getTargetLocation(player.getReference(), blockId -> blockId != 0, max_distance, playerStore);

            //cancel if not hit
            if(hitLocation == null) return;

            //store data in meta
            context.getMetaStore().putMetaObject(HIT_LOCATION, hitLocation);
            context.getMetaStore().putMetaObject(PREDICTED_TIME_TO_TARGET, hitLocation.distanceTo(transform.getPosition()) / pullSpeed);
        }

        //handle state
        contextState.state = InteractionState.NotFinished;
        Vector3d target = context.getMetaStore().getMetaObject(HIT_LOCATION);
        double predicted_time_to_target = context.getMetaStore().getMetaObject(PREDICTED_TIME_TO_TARGET);

        //casting period
        Vector3d player_center = transform.clone().getPosition().add(0,1,0);
        if(time < predicted_time_to_target) {
            //per frame sound
            world.execute(() -> {
                SoundUtil.playSoundEvent3d(SoundEvent.getAssetMap().getIndex("SFX_COINS_LAND"), SoundCategory.UI, transform.getPosition(), ent_store.getStore());

                    //chain effect
                    for(int i = 0; i < 50; i++) {
                        double percent_traveled = (double)i / 50.0d;
                        Vector3d hook_pos = Vector3d.lerp(player_center, target, percent_traveled * (time / predicted_time_to_target));
                        ParticleUtil.spawnParticleEffect("Hookshot", hook_pos, ent_store.getStore());
                    }

                    //tip of hookshot
                    Vector3d hook_pos = Vector3d.lerp(player_center, target, time / predicted_time_to_target);
                    ParticleUtil.spawnParticleEffect("Block_Land_Hard_Metal", hook_pos, ent_store.getStore());
            });
        } else {
            pullPlayer(player, target);

            //per frame sound
            world.execute(() -> {
                SoundUtil.playSoundEvent3d(SoundEvent.getAssetMap().getIndex("SFX_LIGHT_MELEE_T2_BLOCK"), SoundCategory.UI, transform.getPosition(), ent_store.getStore());

                //chain effect
                for(int i = 0; i < 50; i++) {
                    double percent_traveled = (double)i / 50.0d;
                    Vector3d hook_pos = Vector3d.lerp(player_center, target, percent_traveled * (time / predicted_time_to_target));
                    ParticleUtil.spawnParticleEffect("Hookshot", hook_pos, ent_store.getStore());
                }

            });
        }

        //end when at target or if predicted time was run out
        Vector3d current = playerStore.getComponent(ref, TransformComponent.getComponentType()).getPosition();
        double distance = target.distanceTo(current);
        if (distance < 1.5 || time > 2 * predicted_time_to_target) {
            contextState.state = InteractionState.Finished;

            //land sound
            world.execute(() -> {
                SoundUtil.playSoundEvent3d(SoundEvent.getAssetMap().getIndex("SFX_WOOD_LAND"), SoundCategory.UI, transform.getPosition(), ent_store.getStore());
                ParticleUtil.spawnParticleEffect("Block_Land_Hard_Metal", transform.getPosition(), ent_store.getStore());
            });
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
            double speed = Math.min(pullSpeed, distance / Math.max(0.016, 1/60.0));
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
