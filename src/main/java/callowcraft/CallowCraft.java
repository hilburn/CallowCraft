package callowcraft;

import java.io.File;
import java.util.List;
import java.util.Map;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

/**
 * CallowCraft Mod
 *
 * @author Charlie Paterson
 * @license DBaJ non-commercial care-free license. (https://github.com/hilburn/CallowCraft/blob/master/LICENSE.md)
 **/

@Mod(modid = "CallowCraft", name = "CallowCraft", version = "1.0.0")
public class CallowCraft {
    public static final String modID = "CallowCraft";
    public static double xpModifier = 1.0D;
    public static int radius = 1;
    public static int maxXPValue = 2048;

    @Instance("CallowCraft")
    public static CallowCraft instance = new CallowCraft();

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        config(event.getSuggestedConfigurationFile());
        MinecraftForge.EVENT_BUS.register(new xpHandler());
    }

    public static void config(File file)
    {
        Configuration config = new Configuration(file);

        Property xp = config.get("General", "xpModifier", xpModifier);
        xp.comment = "Set from 0 (disabled) to 1 (no change)";
        xpModifier = Math.max(Math.min(xp.getDouble(), 1.0D), 0.0D);

        Property rad = config.get("General", "groupingRadius", radius);
        rad.comment = "Set to 0 to disable grouping, higher values increase radius";
        radius = rad.getInt();

        Property max = config.get("General", "maxXPGrouping", maxXPValue);
        max.comment = "The maximum amount of XP a single orb will hold before no longer grouping";
        maxXPValue = max.getInt();

        config.save();
    }

    @NetworkCheckHandler
    public final boolean networkCheck(Map<String, String> remoteVersions, Side side)
    {
        if (side.isClient()) return true;
        else return remoteVersions.containsKey(modID);
    }

    public class xpHandler {
        private boolean all;
        private boolean noGroup;

        public xpHandler()
        {
            this.all = xpModifier == 0.0D;
            this.noGroup = radius == 0;
        }

        @SubscribeEvent
        public void xpHandler(EntityJoinWorldEvent e)
        {
            if (e.entity instanceof EntityXPOrb) {
                if (this.all || e.world.rand.nextDouble() > xpModifier) {
                    e.setCanceled(true);
                    return;
                }
                if (noGroup) return;
                EntityXPOrb orb = (EntityXPOrb) e.entity;
                AxisAlignedBB axisAlignedBB = AxisAlignedBB.getBoundingBox(orb.posX - radius, orb.posY - radius, orb.posZ - radius, orb.posX + radius, orb.posY + radius, orb.posZ + radius);
                List<EntityXPOrb> nearbyOrbs = (List<EntityXPOrb>) e.world.getEntitiesWithinAABB(EntityXPOrb.class, axisAlignedBB);
                int nearby = 0;
                for (EntityXPOrb nearbyOrb : nearbyOrbs) {
                    if (nearbyOrb.isDead || nearbyOrb.xpValue+orb.xpValue>maxXPValue) continue;
                    nearby++;
                    orb.posX = nearbyOrb.posX;
                    orb.posY = nearbyOrb.posY;
                    orb.posZ = nearbyOrb.posZ;
                    orb.motionX = orb.motionX*orb.xpValue + nearbyOrb.motionX*nearbyOrb.xpValue;
                    orb.motionY = orb.motionY*orb.xpValue + nearbyOrb.motionY*nearbyOrb.xpValue;
                    orb.motionZ = orb.motionZ*orb.xpValue + nearbyOrb.motionZ*nearbyOrb.xpValue;
                    orb.xpValue += nearbyOrb.getXpValue();
                    orb.motionX /= orb.xpValue;
                    orb.motionY /= orb.xpValue;
                    orb.motionZ /= orb.xpValue;
                    e.world.removeEntity(nearbyOrb);
                }
                if (nearby>0) {
                    e.setCanceled(true);
                    e.world.spawnEntityInWorld(orb);
                }
            }
        }
    }
}