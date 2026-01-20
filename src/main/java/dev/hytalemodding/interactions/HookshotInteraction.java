package dev.hytalemodding.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.npc.movement.MovementState;

import javax.annotation.Nonnull;


public class HookshotInteraction extends SimpleInteraction {

    public static final BuilderCodec<HookshotInteraction> CODEC = BuilderCodec.builder(
            HookshotInteraction.class, HookshotInteraction::new, SimpleInteraction.CODEC
    ).build();

    private final double pullSpeed = 25; //blocks per second

    public static final String INTERACTION_NAME = "HookshotInteraction";
    public Vector3d target = new Vector3d(0,90,0);

    @Override
    protected void tick0(
        boolean firstRun,
        float time,
        @Nonnull com.hypixel.hytale.protocol.InteractionType type,
        @Nonnull com.hypixel.hytale.server.core.entity.InteractionContext context,
        @Nonnull com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler cooldownHandler
    ) {
        InteractionSyncData contextState = context.getState();
        if (firstRun) {
            contextState.state = InteractionState.NotFinished;
            // Raycast
            Ref<EntityStore> ref = context.getEntity();
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();

            Transform look = TargetUtil.getLook(ref, context.getCommandBuffer());
            System.out.println(look);
            Vector3d origin = look.getPosition();
            System.out.println(origin);
            Vector3d direction = look.getDirection();
            System.out.println(direction);

            target = origin.add(direction.normalize().scale(30)); // raycast 10 blocks
            System.out.println(target);
        }

        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        Player player = commandBuffer.getComponent(context.getEntity(), Player.getComponentType());

        if (!firstRun) {
            Ref<EntityStore> ref = context.getEntity();
            pullPlayer(ref, ref.getStore(), target, ref.getStore().getExternalData().getWorld(), player, contextState);

            contextState.state = InteractionState.NotFinished;
            System.out.println(target);
        }
    }

    private void pullPlayer(Ref<EntityStore> ref, Store<EntityStore> store, Vector3d target, World world, Player player, InteractionSyncData contextState) {
        System.out.println("pullplayer called");
        world.execute(() -> {
            Vector3f playerRot = player.getReference().getStore().getComponent(player.getReference(), com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType()).getRotation();

            System.out.println("INSIDE CHUNK THREAD!");
            // Get player transform
            var transform = store.getComponent(ref, com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
            if (transform == null) return;
            System.out.println("transform was NOT NULL");

            Vector3d current = transform.getPosition();
            Vector3d toTarget = target.subtract(current);
            double distance = toTarget.length();

            if (distance < 1) {
                contextState.state = InteractionState.Finished;
                return;
            }
            System.out.println("distance not reached");

            Vector3d step = toTarget.normalize().scale(Math.min(pullSpeed, distance));

            if (player.getReference() == null) return;

            Store<EntityStore> store2 = player.getReference().getStore();


            Velocity velo = store2.getComponent(player.getReference(), Velocity.getComponentType());
            velo.addInstruction(step, null, ChangeVelocityType.Set);
        });
    }
}
