package fi.dy.masa.minihud.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.data.IntBoundingBox;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.nbt.NbtStringifierPretty;
import fi.dy.masa.malilib.util.nbt.NbtStringifierSimple;
import fi.dy.masa.minihud.LiteModMiniHud;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.data.DataStorage;
import fi.dy.masa.minihud.mixin.IMixinChunkProviderServer;

public class MiscUtils
{
    private static final Random RAND = new Random();

    public static long bytesToMb(long bytes)
    {
        return bytes / 1024L / 1024L;
    }

    public static double intAverage(int[] values)
    {
        final int size = values.length;
        long sum = 0L;

        for (int i = 0; i < size; ++i)
        {
            sum += values[i];
        }

        return (double) sum / (double) values.length;
    }

    public static boolean canSlimeSpawnAt(int posX, int posZ, long worldSeed)
    {
        return canSlimeSpawnInChunk(posX >> 4, posZ >> 4, worldSeed);
    }

    public static boolean canSlimeSpawnInChunk(int chunkX, int chunkZ, long worldSeed)
    {
        long slimeSeed = 987234911L;
        long rngSeed = worldSeed +
                       (long) (chunkX * chunkX *  4987142) + (long) (chunkX * 5947611) +
                       (long) (chunkZ * chunkZ) * 4392871L + (long) (chunkZ * 389711) ^ slimeSeed;

        RAND.setSeed(rngSeed);

        return RAND.nextInt(10) == 0;
    }

    public static int getChunkUnloadBucket(int chunkX, int chunkZ)
    {
        if (Configs.Generic.CHUNK_UNLOAD_BUCKET_HASH_SIZE.getBooleanValue())
        {
            return getChunkOrder(chunkX, chunkZ, DataStorage.getInstance().getDroppedChunksHashSize());
        }
        // The old simple calculation, without knowledge of the HashSet size
        else
        {
            int longHash = Long.valueOf(ChunkPos.asLong(chunkX, chunkZ)).hashCode();
            return (longHash ^ (longHash >>> 16)) & 0xFFFF;
        }
    }

    /**
     * This method has been taken from the Carpet mod, by gnembon
     */
    public static int getCurrentHashSize(WorldServer server)
    {
        IMixinChunkProviderServer provider = (IMixinChunkProviderServer) (Object) server.getChunkProvider();

        try
        {
            Set<Long> droppedChunks = provider.getDroppedChunks();
            Field field = droppedChunks.getClass().getDeclaredField("map");
            field.setAccessible(true);

            @SuppressWarnings("unchecked")
            HashMap<Object, Object> map = (HashMap<Object, Object>) field.get(droppedChunks);
            field = map.getClass().getDeclaredField("table");
            field.setAccessible(true);

            Object[] table = (Object []) field.get(map);

            if (table == null)
            {
                return 2;
            }

            return table.length;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return -1;
    }


    /**
     * This method has been taken from the Carpet mod, by gnembon
     */
    public static int getChunkOrder(int chunkX, int chunkZ, int hashSize)
    {
        try
        {
            Method method = HashMap.class.getDeclaredMethod("hash", Object.class);
            method.setAccessible(true);

            return (Integer) method.invoke(null, Long.hashCode(ChunkPos.asLong(chunkX, chunkZ))) & (hashSize - 1);
        }
        catch (Exception e)
        {
            LiteModMiniHud.logger.error("Error while trying to get the chunk unload order");
            return -1;
        }
    }

    public static int getSpawnableChunksCount(WorldServer world)
    {
        Set<ChunkPos> eligibleChunksForSpawning = new HashSet<>();
        int chunkCount = 0;

        for (EntityPlayer player : world.playerEntities)
        {
            if (player.isSpectator() == false)
            {
                int cx = MathHelper.floor(player.posX / 16.0D);
                int cz = MathHelper.floor(player.posZ / 16.0D);
                int chunkRadius = 8;

                for (int cxOff = -chunkRadius; cxOff <= chunkRadius; ++cxOff)
                {
                    for (int czOff = -chunkRadius; czOff <= chunkRadius; ++czOff)
                    {
                        boolean edgeChunk = cxOff == -chunkRadius || cxOff == chunkRadius || czOff == -chunkRadius || czOff == chunkRadius;
                        ChunkPos chunkPos = new ChunkPos(cxOff + cx, czOff + cz);

                        if (eligibleChunksForSpawning.contains(chunkPos) == false)
                        {
                            ++chunkCount;

                            if (edgeChunk == false && world.getWorldBorder().contains(chunkPos))
                            {
                                PlayerChunkMapEntry playerchunkmapentry = world.getPlayerChunkMap().getEntry(chunkPos.x, chunkPos.z);

                                if (playerchunkmapentry != null && playerchunkmapentry.isSentToPlayers())
                                {
                                    eligibleChunksForSpawning.add(chunkPos);
                                }
                            }
                        }
                    }
                }
            }
        }

        return chunkCount;
    }

    public static void getItemTooltip(ItemStack stack, List<String> lines)
    {
        Minecraft mc = Minecraft.getMinecraft();
        boolean showPretty = Configs.Generic.ITEM_NBT_KEY_PRETTY.isHeld();
        boolean showString = Configs.Generic.ITEM_NBT_KEY_STRING.isHeld();

        // If the vanilla advanced tooltips are disabled, add them here, when showing a tooltip
        if (mc.gameSettings.advancedItemTooltips == false && (showPretty || showString))
        {
            if (stack.isItemDamaged())
            {
                lines.add(StringUtils.translate("item.durability", stack.getMaxDamage() - stack.getItemDamage(), stack.getMaxDamage()));
            }

            lines.add(GuiBase.TXT_DARK_GRAY + Item.REGISTRY.getNameForObject(stack.getItem()).toString());

            if (stack.hasTagCompound())
            {
                lines.add(GuiBase.TXT_DARK_GRAY + StringUtils.translate("item.nbt_tags", stack.getTagCompound().getKeySet().size()));
            }
        }

        NBTTagCompound tag = stack.getTagCompound();

        if (tag != null)
        {
            if (showPretty)
            {
                lines.addAll((new NbtStringifierPretty(true, GuiBase.TXT_GRAY).getNbtLines(tag)));
            }

            if (showString)
            {
                String str = (new NbtStringifierSimple(true, GuiBase.TXT_GRAY)).getNbtString(tag);
                StringUtils.splitTextToLines(lines, str, 240);
            }
        }
    }

    public static boolean isStructureWithinRange(IntBoundingBox bb, BlockPos playerPos, int maxRange)
    {
        if (playerPos.getX() < (bb.minX - maxRange) ||
            playerPos.getX() > (bb.maxX + maxRange) ||
            playerPos.getZ() < (bb.minZ - maxRange) ||
            playerPos.getZ() > (bb.maxZ + maxRange))
        {
            return false;
        }

        return true;
    }

    public static boolean isStructureWithinRange(StructureBoundingBox bb, BlockPos playerPos, int maxRange)
    {
        if (playerPos.getX() < (bb.minX - maxRange) ||
            playerPos.getX() > (bb.maxX + maxRange) ||
            playerPos.getZ() < (bb.minZ - maxRange) ||
            playerPos.getZ() > (bb.maxZ + maxRange))
        {
            return false;
        }

        return true;
    }
}
