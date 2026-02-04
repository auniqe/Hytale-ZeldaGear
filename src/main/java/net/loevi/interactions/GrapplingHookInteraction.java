package net.loevi.interactions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.meta.MetaKey;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;

import javax.annotation.Nonnull;

public class GrapplingHookInteraction extends SimpleInteraction {

    private double pullSpeed = 17; //blocks per second
    private double maxDistance = 17; // block reach

    public static final BuilderCodec<GrapplingHookInteraction> CODEC = BuilderCodec.builder(
            GrapplingHookInteraction.class, GrapplingHookInteraction::new, SimpleInteraction.CODEC
            )
        .appendInherited(
                new KeyedCodec<>("PullSpeed", Codec.DOUBLE),
                (hook, value) -> hook.pullSpeed = value,
                hook -> hook.pullSpeed,
                (hook, parent) -> hook.pullSpeed = parent.pullSpeed
                )
        .add()
        .appendInherited(
                new KeyedCodec<>("MaxDistance", Codec.DOUBLE),
                (hook, value) -> hook.maxDistance = value,
                hook -> hook.maxDistance,
                (hook, parent) -> hook.maxDistance = parent.maxDistance
                )
        .add()
        .afterDecode(hook -> {
            // any extra initialization after decoding
        })
    .build();

    public static final String INTERACTION_NAME = "GrapplingHookInteraction";

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
        Store<EntityStore> store = player.getReference().getStore();
        InteractionSyncData contextState = context.getState();
        World world = player.getWorld();
        EntityStore entityStore = world.getEntityStore();
        TransformComponent transform = entityStore.getStore().getComponent(ref, EntityModule.get().getTransformComponentType());

        //initial setup
        if (firstRun) {
            //first shot sound
            world.execute(() -> {
                SoundUtil.playSoundEvent3d(SoundEvent.getAssetMap().getIndex("SFX_WOOD_HIT"), SoundCategory.UI, transform.getPosition(), entityStore.getStore());
                SoundUtil.playSoundEvent3d(SoundEvent.getAssetMap().getIndex("SFX_COINS_LAND"), SoundCategory.UI, transform.getPosition(), entityStore.getStore());
            });

            //raycast
            Vector3d hitLocation = TargetUtil.getTargetLocation(player.getReference(), blockId -> blockId != 0, maxDistance, store);

            //cancel if not hit
            if (hitLocation == null) {
                //refund 2 stamina
                world.execute(() -> {
                    EntityStatMap statMap = (EntityStatMap) store.getComponent(player.getReference(), EntityStatMap.getComponentType());
                    if (statMap != null) {
                        statMap.addStatValue(DefaultEntityStatTypes.getStamina(), 2f);
                    }
                });

                return; //only return if hitLocation is null
            }

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
            if(clock_timer(time, 100)) {
                world.execute(() -> {
                    SoundUtil.playSoundEvent3d(SoundEvent.getAssetMap().getIndex("SFX_COINS_LAND"), SoundCategory.UI, transform.getPosition(), entityStore.getStore());

                    //tip of hookshot
                    Vector3d hook_pos = Vector3d.lerp(player_center, target, time / predicted_time_to_target);
                    ParticleUtil.spawnParticleEffect("Block_Land_Hard_Metal", hook_pos, entityStore.getStore());
                });
            }
        } else {
            pullPlayer(player, target);

            //per frame sound
            world.execute(() -> {
                SoundUtil.playSoundEvent3d(SoundEvent.getAssetMap().getIndex("SFX_LIGHT_MELEE_T2_BLOCK"), SoundCategory.UI, transform.getPosition(), entityStore.getStore());
            });
        }

        //end when at target or if predicted time was run out
        Vector3d current = store.getComponent(ref, TransformComponent.getComponentType()).getPosition().clone().add(0,1,0);
        double distance = target.distanceTo(current);
        if (distance < 1.5 || time > (2.5 * predicted_time_to_target)) {
            contextState.state = InteractionState.Finished;

            //land sound
            world.execute(() -> {
                SoundUtil.playSoundEvent3d(SoundEvent.getAssetMap().getIndex("SFX_WOOD_LAND"), SoundCategory.UI, transform.getPosition(), entityStore.getStore());
                ParticleUtil.spawnParticleEffect("Block_Land_Hard_Metal", transform.getPosition(), entityStore.getStore());
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
            player.setCurrentFallDistance(0);
        });
    }

    private boolean clock_timer(float time, int ms_per_clock) {
        return Math.ceilMod((int)time * 1000, ms_per_clock) == 0;
    }
}
