package com.topuw.end;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class TopuwSEndUpdate implements ModInitializer {

    public static final String MOD_ID = "topuws-end-update";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Topuw's End Update initialized!");

        ModItems.initialize();
        registerEndermiteBucketInteractions();
        registerEndermiteSpawning();
    }

    private static void registerEndermiteBucketInteractions() {

        // Puste wiadro + klikniÄ™cie endermita = zĹ‚apanie.
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            ItemStack heldStack = player.getItemInHand(hand);

            if (!heldStack.is(Items.BUCKET)
                    || !(entity instanceof Endermite mite)
                    || player.isSpectator()) {
                return InteractionResult.PASS;
            }

            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }

            ItemStack filledBucket = new ItemStack(ModItems.ENDERMITE_BUCKET);

            if (player.getAbilities().instabuild) {
                // W trybie kreatywnym nie zuĹĽywamy pustego wiadra.
                if (!player.getInventory().add(filledBucket)) {
                    LOGGER.warn("Ekwipunek pełny — nie udało się dodać wiadra z endermitem.");
                }
            } else if (heldStack.getCount() == 1) {
                player.setItemInHand(hand, filledBucket);
            } else {
                heldStack.shrink(1);

                if (!player.getInventory().add(filledBucket)) {
                    LOGGER.warn("Ekwipunek pełny — nie udało się dodać wiadra z endermitem.");
                }
            }

            mite.discard();
            LOGGER.info("Endermite captured in a bucket.");

            return InteractionResult.SUCCESS;
        });

        // Wiadro z endermitem + klikniÄ™cie bloku = wypuszczenie.
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            ItemStack heldStack = player.getItemInHand(hand);

            if (!heldStack.is(ModItems.ENDERMITE_BUCKET)
                    || player.isSpectator()) {
                return InteractionResult.PASS;
            }

            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }

            BlockPos spawnPos = hitResult.getBlockPos()
                    .relative(hitResult.getDirection());

            Optional<Holder.Reference<EntityType<?>>> entry =
                    BuiltInRegistries.ENTITY_TYPE.get(
                            Identifier.fromNamespaceAndPath("minecraft", "endermite")
                    );

            if (entry.isEmpty()) {
                LOGGER.error("Cannot release Endermite: entity type not found.");
                return InteractionResult.FAIL;
            }

            Entity entity = entry.get().value().create(
                    level,
                    EntitySpawnReason.NATURAL
            );

            if (!(entity instanceof Endermite mite)) {
                LOGGER.error("Cannot create Endermite entity.");
                return InteractionResult.FAIL;
            }

            mite.setPos(
                    spawnPos.getX() + 0.5,
                    spawnPos.getY(),
                    spawnPos.getZ() + 0.5
            );

            if (!level.getBlockState(spawnPos).isAir()
                    || !level.getBlockState(spawnPos.above()).isAir()) {
                // SprĂłbuj postawiÄ‡ endermita nad klikniÄ™tym blokiem.
                spawnPos = hitResult.getBlockPos().above();

                if (!level.getBlockState(spawnPos).isAir()
                        || !level.getBlockState(spawnPos.above()).isAir()) {
                    mite.discard();
                    return InteractionResult.FAIL;
                }

                mite.setPos(
                        spawnPos.getX() + 0.5,
                        spawnPos.getY(),
                        spawnPos.getZ() + 0.5
                );
            }

            if (!level.addFreshEntity(mite)) {
                mite.discard();
                return InteractionResult.FAIL;
            }

            if (!player.getAbilities().instabuild) {
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            }

            LOGGER.info("Endermite released from a bucket.");
            return InteractionResult.SUCCESS;
        });
    }

    private static void registerEndermiteSpawning() {
        ServerTickEvents.END_LEVEL_TICK.register(level -> {
            if (!level.dimension().equals(Level.END)) {
                return;
            }

            // PrĂłba spawnu co 2 sekundy.
            if (level.getGameTime() % 40 != 0) {
                return;
            }

            for (ServerPlayer player : level.players()) {
                // Maksymalnie 4 endermity w promieniu 32 blokĂłw.
                AABB area = player.getBoundingBox().inflate(32.0);

                if (level.getEntitiesOfClass(Endermite.class, area).size() >= 4) {
                    continue;
                }

                int dx = 8 + level.getRandom().nextInt(9);
                int dz = 8 + level.getRandom().nextInt(9);

                if (level.getRandom().nextBoolean()) {
                    dx = -dx;
                }

                if (level.getRandom().nextBoolean()) {
                    dz = -dz;
                }

                int x = player.blockPosition().getX() + dx;
                int z = player.blockPosition().getZ() + dz;

                BlockPos checkPos = new BlockPos(
                        x,
                        player.blockPosition().getY(),
                        z
                );

                if (!level.hasChunkAt(checkPos)) {
                    continue;
                }

                int surfaceY = level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        x,
                        z
                );

                BlockPos spawnPos = new BlockPos(x, surfaceY, z);

                if (!level.getBlockState(spawnPos).isAir()
                        || !level.getBlockState(spawnPos.above()).isAir()
                        || !level.getBlockState(spawnPos.below()).isSolid()) {
                    continue;
                }

                Optional<Holder.Reference<EntityType<?>>> entry =
                        BuiltInRegistries.ENTITY_TYPE.get(
                                Identifier.fromNamespaceAndPath("minecraft", "endermite")
                        );

                if (entry.isEmpty()) {
                    LOGGER.error("Nie znaleziono minecraft:endermite w rejestrze!");
                    continue;
                }

                Entity entity = entry.get().value().create(
                        level,
                        EntitySpawnReason.NATURAL
                );

                if (!(entity instanceof Endermite mite)) {
                    continue;
                }

                mite.setPos(
                        spawnPos.getX() + 0.5,
                        spawnPos.getY(),
                        spawnPos.getZ() + 0.5
                );

                if (level.addFreshEntity(mite)) {
                    LOGGER.info("Spawned an Endermite in the End");
                }
            }
        });
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
