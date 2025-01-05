package org.dynmap.bukkit.helper.v121_4;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeFog;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_21_R3.CraftWorld;
import org.dynmap.DynmapChunk;
import org.dynmap.bukkit.helper.BukkitWorld;
import org.dynmap.common.BiomeMap;
import org.dynmap.common.chunk.GenericChunk;
import org.dynmap.common.chunk.GenericChunkCache;
import org.dynmap.common.chunk.GenericMapChunkCache;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;

/**
 * Container for managing chunks - dependent upon using chunk snapshots, since rendering is off server thread
 */
public class MapChunkCache121_4 extends GenericMapChunkCache {
    private World w;
    /**
     * Construct empty cache
     */
    public MapChunkCache121_4(GenericChunkCache cc) {
        super(cc);
    }

    protected GenericChunk getLoadedChunk(DynmapChunk chunk) {
        CraftWorld cw = (CraftWorld) w;
        if (!cw.isChunkLoaded(chunk.x, chunk.z)) return null;
        Chunk c = cw.getHandle().getChunkIfLoaded(chunk.x, chunk.z);
        if (c == null || !c.q) return null;    // c.loaded
        SerializableChunkData chunkData = SerializableChunkData.a(cw.getHandle(), c); //SerializableChunkData.copyOf
        NBTTagCompound nbt = chunkData.a(); // SerializableChunkData.write
        return nbt != null ? parseChunkFromNBT(new NBT.NBTCompound(nbt)) : null;
    }

    protected GenericChunk loadChunk(DynmapChunk chunk) {
        CraftWorld cw = (CraftWorld) w;
        NBTTagCompound nbt = null;
        ChunkCoordIntPair cc = new ChunkCoordIntPair(chunk.x, chunk.z);
        GenericChunk gc = null;
        try {	// BUGBUG - convert this all to asyn properly, since now native async
            nbt = cw.getHandle().m().a.d(cc).join().get();	// WorldServer.getChunkSource().chunkMap.read(cc).join().get()
        } catch (CancellationException cx) {
        } catch (NoSuchElementException snex) {
        }
        if (nbt != null) {
            gc = parseChunkFromNBT(new NBT.NBTCompound(nbt));
        }
        return gc;
    }

    public void setChunks(BukkitWorld dw, List<DynmapChunk> chunks) {
        this.w = dw.getWorld();
        super.setChunks(dw, chunks);
    }

    @Override
    public int getFoliageColor(BiomeMap bm, int[] colormap, int x, int z) {
		return bm.<BiomeBase>getBiomeObject().map(BiomeBase::h).flatMap(BiomeFog::e).orElse(colormap[bm.biomeLookup()]); // BiomeBase::getSpecialEffects, BiomeFog::skyColor
    }

    @Override
    public int getGrassColor(BiomeMap bm, int[] colormap, int x, int z) {
        BiomeFog fog = bm.<BiomeBase>getBiomeObject().map(BiomeBase::h).orElse(null); // BiomeBase::getSpecialEffects
        if (fog == null) return colormap[bm.biomeLookup()];
        return fog.g().a(x, z, fog.f().orElse(colormap[bm.biomeLookup()])); // BiomeFog.getGrassColorModifier, BiomeFog.getGrassColorOverride
    }
}
