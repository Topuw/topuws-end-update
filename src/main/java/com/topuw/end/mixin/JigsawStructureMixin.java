
package com.topuw.end.mixin;

import java.util.List;
import java.util.Optional;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationStub;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasBinding;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

@Mixin(JigsawStructure.class)
public abstract class JigsawStructureMixin {

    @Shadow @Final
    private Holder<StructureTemplatePool> startPool;

    @Shadow @Final
    private Optional<Identifier> startJigsawName;

    @Shadow @Final
    private int maxDepth;

    @Shadow @Final
    private boolean useExpansionHack;

    @Shadow @Final
    private JigsawStructure.MaxDistance maxDistanceFromCenter;

    @Shadow @Final
    private List<PoolAliasBinding> poolAliases;

    @Shadow @Final
    private DimensionPadding dimensionPadding;

    @Shadow @Final
    private LiquidSettings liquidSettings;

    @Inject(
        method = "findGenerationPoint",
        at = @At("HEAD"),
        cancellable = true
    )
    private void topuwsEndUpdate$placeRuinsOnEndStone(
            GenerationContext context,
            CallbackInfoReturnable<Optional<GenerationStub>> cir
    ) {
boolean isOurRuins = startPool.unwrapKey()
        .map(key -> key.identifier().toString()
                .equals("topuws-end-update:end_ruins"))
        .orElse(false);
        // Wszystkie pozostałe struktury korzystają z kodu Minecrafta.
        if (!isOurRuins) {
            return;
        }

        ChunkPos chunkPos = context.chunkPos();
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();

        // Losowy punkt początkowy, ale sprawdzamy wszystkie 256 kolumn.
        int offset = context.random().nextInt(256);

        for (int i = 0; i < 256; i++) {
            int index = (offset + i) & 255;
            int x = minX + (index & 15);
            int z = minZ + (index >> 4);

            int surfaceY = context.chunkGenerator().getBaseHeight(
                    x,
                    z,
                    Heightmap.Types.WORLD_SURFACE_WG,
                    context.heightAccessor(),
                    context.randomState()
            );

            if (surfaceY <= context.heightAccessor().getMinY()) {
                continue;
            }

            var column = context.chunkGenerator().getBaseColumn(
                    x,
                    z,
                    context.heightAccessor(),
                    context.randomState()
            );

            // Ruiny mogą rozpocząć się tylko nad End Stone.
            if (!column.getBlock(surfaceY - 1).is(Blocks.END_STONE)) {
                continue;
            }

            // surfaceY to pierwszy blok powietrza nad powierzchnią.
            BlockPos startPos = new BlockPos(x, surfaceY, z);

            Optional<GenerationStub> result = JigsawPlacement.addPieces(
                    context,
                    startPool,
                    startJigsawName,
                    maxDepth,
                    startPos,
                    useExpansionHack,
                    Optional.empty(),
                    maxDistanceFromCenter,
                    PoolAliasLookup.create(
                            poolAliases,
                            startPos,
                            context.seed()
                    ),
                    dimensionPadding,
                    liquidSettings
            );

            cir.setReturnValue(result);
            return;
        }

        // W tym chunku nie znaleziono powierzchni End Stone.
        cir.setReturnValue(Optional.empty());
    }
}
