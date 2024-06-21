package de.dafuqs.fluidvoidfading.mixin.client;

import me.jellysquid.mods.sodium.client.model.color.*;
import me.jellysquid.mods.sodium.client.model.light.*;
import me.jellysquid.mods.sodium.client.model.light.data.*;
import me.jellysquid.mods.sodium.client.model.quad.*;
import me.jellysquid.mods.sodium.client.model.quad.properties.*;
import me.jellysquid.mods.sodium.client.render.chunk.compile.*;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.*;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.*;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.*;
import me.jellysquid.mods.sodium.client.util.*;
import me.jellysquid.mods.sodium.client.world.*;
import net.caffeinemc.mods.sodium.api.util.*;
import net.fabricmc.fabric.api.client.render.fluid.v1.*;
import net.minecraft.block.*;
import net.minecraft.client.*;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.*;
import net.minecraft.fluid.*;
import net.minecraft.registry.tag.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import org.jetbrains.annotations.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

// TODO: This mixin is currently broken and does not work and I do not know, why
// Hence the   "breaks": { "sodium": "*" } entry in the mod.json
@Pseudo
@Mixin(value = FluidRenderer.class, remap = false)
public abstract class SodiumFluidRendererMixin {
    
    @Shadow @Final private BlockPos.Mutable scratchPos;
    
    @Shadow protected abstract ColorProvider<FluidState> getColorProvider(Fluid fluid, FluidRenderHandler handler);
    
    @Shadow @Final private ModelQuadViewMutable quad;
    
    @Shadow @Final private LightPipelineProvider lighters;
    
    @Shadow @Final private QuadLightData quadLightData;
    
    @Shadow @Final private int[] quadColors;
    
    @Shadow
    private static void setVertex(ModelQuadViewMutable quad, int i, float x, float y, float z, float u, float v) {
    }
    
    @Shadow protected abstract void writeQuad(ChunkModelBuilder builder, Material material, BlockPos offset, ModelQuadView quad, ModelQuadFacing facing, boolean flip);
	
	@Shadow
	private static FluidRenderHandler getFluidRenderHandler(FluidState fluidState) {
		return null;
	}
    
    @Inject(method = "render", at = @At("TAIL"))
    public void fluidVoidFading$render(WorldSlice world, FluidState fluidState, BlockPos pos, BlockPos offset, ChunkBuildBuffers buffers, CallbackInfo cir) {
        if (pos.getY() == world.getBottomY()) {
            fluidVoidFading$renderFluidInVoid(world, fluidState, pos, offset, buffers);
        }
    }

    @Inject(method = "isSideExposed", at = @At("HEAD"), cancellable = true)
    private void fluidVoidFading$isSideExposed(BlockRenderView world, int x, int y, int z, Direction dir, float height, CallbackInfoReturnable<Boolean> cir) {
        if (dir == Direction.DOWN && y == world.getBottomY()) {
            cir.setReturnValue(false);
        }
    }
    
    @Unique
    private void fluidVoidFading$renderFluidInVoid(WorldSlice world, @NotNull FluidState fluidState, BlockPos blockPos, BlockPos offset, ChunkBuildBuffers buffers) {
        Fluid fluid = fluidState.getFluid();
        Material material = DefaultMaterials.forRenderLayer(RenderLayer.getTranslucent());;
        ChunkModelBuilder meshBuilder = buffers.get(material);
        if (fluid != Fluids.EMPTY) {
            int posX = blockPos.getX();
            int posY = blockPos.getY();
            int posZ = blockPos.getZ();
            
            BlockState northBlockState = world.getBlockState(blockPos.offset(Direction.NORTH));
            FluidState northFluidState = northBlockState.getFluidState();
            BlockState southBlockState = world.getBlockState(blockPos.offset(Direction.SOUTH));
            FluidState southFluidState = southBlockState.getFluidState();
            BlockState westBlockState = world.getBlockState(blockPos.offset(Direction.WEST));
            FluidState westFluidState = westBlockState.getFluidState();
            BlockState eastBlockState = world.getBlockState(blockPos.offset(Direction.EAST));
            FluidState eastFluidState = eastBlockState.getFluidState();
            
            boolean sfNorth = northFluidState.getFluid().matchesType(fluidState.getFluid());
            boolean sfSouth = southFluidState.getFluid().matchesType(fluidState.getFluid());
            boolean sfWest = westFluidState.getFluid().matchesType(fluidState.getFluid());
            boolean sfEast = eastFluidState.getFluid().matchesType(fluidState.getFluid());
    
            boolean isWater = fluidState.isIn(FluidTags.WATER);
            FluidRenderHandler handler = getFluidRenderHandler(fluidState);
            ColorProvider<FluidState> colorProvider = this.getColorProvider(fluid, handler);
            Sprite[] sprites = handler.getFluidSprites(world, blockPos, fluidState);
            float northWestHeight;
            float southWestHeight;
            float southEastHeight;
            float northEastHeight;
            float yOffset = 0.0F;
            northWestHeight = 1.0F;
            southWestHeight = 1.0F;
            southEastHeight = 1.0F;
            northEastHeight = 1.0F;
    
            ModelQuadViewMutable quad = this.quad;
            LightMode lightMode = isWater && MinecraftClient.isAmbientOcclusionEnabled() ? LightMode.SMOOTH : LightMode.FLAT;
            LightPipeline lighter = this.lighters.getLighter(lightMode);
            quad.setFlags(0);
            float c1;
            float c2;
            float x1;
            float z1;
            float x2;
            float z2;
            for (Direction dir : DirectionUtil.HORIZONTAL_DIRECTIONS) {
                switch (dir) {
                    case NORTH:
                        if (sfNorth) {
                            continue;
                        }
                
                        c1 = northWestHeight;
                        c2 = northEastHeight;
                        x1 = 0.0F;
                        x2 = 1.0F;
                        z1 = 0.001F;
                        z2 = z1;
                        break;
                    case SOUTH:
                        if (sfSouth) {
                            continue;
                        }
                
                        c1 = southEastHeight;
                        c2 = southWestHeight;
                        x1 = 1.0F;
                        x2 = 0.0F;
                        z1 = 0.999F;
                        z2 = z1;
                        break;
                    case WEST:
                        if (sfWest) {
                            continue;
                        }
                
                        c1 = southWestHeight;
                        c2 = northWestHeight;
                        x1 = 0.001F;
                        x2 = x1;
                        z1 = 1.0F;
                        z2 = 0.0F;
                        break;
                    case EAST:
                        if (!sfEast) {
                            c1 = northEastHeight;
                            c2 = southEastHeight;
                            x1 = 0.999F;
                            x2 = x1;
                            z1 = 0.0F;
                            z2 = 1.0F;
                            break;
                        }
                    default:
                        continue;
                }
        
                int adjX = posX + dir.getOffsetX();
                int adjY = posY + dir.getOffsetY();
                int adjZ = posZ + dir.getOffsetZ();
                Sprite sprite = sprites[1];
                boolean isOverlay = false;
                if (sprites.length > 2) {
                    BlockPos adjPos = this.scratchPos.set(adjX, adjY, adjZ);
                    BlockState adjBlock = world.getBlockState(adjPos);
                    if (FluidRenderHandlerRegistry.INSTANCE.isBlockTransparent(adjBlock.getBlock())) {
                        sprite = sprites[2];
                        isOverlay = true;
                    }
                }
                
                float u1 = sprite.getFrameU(0.0F);
                float u2 = sprite.getFrameU(0.5F);
                float v1 = sprite.getFrameV((1.0F - c1) * 0.5F);
                float v2 = sprite.getFrameV((1.0F - c2) * 0.5F);
                float v3 = sprite.getFrameV(0.5F);
                quad.setSprite(sprite);
                setVertex(quad, 0, x2, c2, z2, u2, v2);
                setVertex(quad, 1, x2, yOffset, z2, u2, v3);
                setVertex(quad, 2, x1, yOffset, z1, u1, v3);
                setVertex(quad, 3, x1, c1, z1, u1, v1);
                float br = dir.getAxis() == Direction.Axis.Z ? 0.8F : 0.6F;
                ModelQuadFacing facing = ModelQuadFacing.fromDirection(dir);
                
                int[] original = new int[]{this.quadColors[0], this.quadColors[1], this.quadColors[2], this.quadColors[3]};
                
                BlockPos downPos1 = offset.offset(Direction.DOWN, 1);
                this.updateQuadWithAlpha(quad, world, blockPos, lighter, dir, br, colorProvider, fluidState, original, 1.0F, 0.3F);
                this.writeQuad(meshBuilder, material, downPos1, quad, facing, false);
                if (!isOverlay) {
                    this.writeQuad(meshBuilder, material, downPos1, quad, facing.getOpposite(), true);
                }
                
                BlockPos downPos2 = offset.offset(Direction.DOWN, 2);
                this.updateQuadWithAlpha(quad, world, blockPos, lighter, dir, br, colorProvider, fluidState, original, 0.3F, 0.0F);
                this.writeQuad(meshBuilder, material, downPos2, quad, facing, false);
                if (!isOverlay) {
                    this.writeQuad(meshBuilder, material, downPos2, quad, facing.getOpposite(), true);
                }
            }
        }
    }
    
    @Unique
    private void updateQuadWithAlpha(ModelQuadView quad, WorldSlice world, BlockPos pos, LightPipeline lighter, Direction dir, float brightness, ColorProvider<FluidState> colorProvider, FluidState fluidState, int[] original, float alphaStart, float alphaEnd) {
        QuadLightData light = this.quadLightData;
        lighter.calculate(quad, pos, light, null, dir, false);
        colorProvider.getColors(world, pos, fluidState, quad, original);
        
        for(int i = 0; i < 4; ++i) {
            float alpha = i == 0 || i == 3 ? alphaStart : alphaEnd;
            this.quadColors[i] = ColorABGR.withAlpha(original[i], light.br[i] * brightness * alpha);
        }
    }

}
