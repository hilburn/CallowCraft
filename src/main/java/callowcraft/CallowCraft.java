package callowcraft;

import java.io.File;
import java.util.List;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
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
 * @license BDaJ non-commercial care-free license. (https://github.com/hilburn/CallowCraft/blob/master/LICENSE.md)
 **/
@Mod(modid = "CallowCraft", name = "CallowCraft", version = "1.0.0")
public class CallowCraft {
    public static double xpMultiplier = 1.0D;
    public static int radius = 1;

    @Instance("CallowCraft")
    public static CallowCraft instance = new CallowCraft();

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        config(event.getSuggestedConfigurationFile());
        MinecraftForge.EVENT_BUS.register(new xpHandler(xpMultiplier,radius));
    }

    public static void config(File file)
    {
        Configuration config = new Configuration(file);

        Property xp = config.get("General", "xpMultiplier", xpMultiplier);
        xp.comment = "Set from 0 (disabled) to 1 (no change)";
        xpMultiplier = Math.max(Math.min(xp.getDouble(), 1.0D), 0.0D);

        Property rad = config.get("General", "groupingRadius", radius);
        rad.comment = "Set to 0 to disable grouping, higher values increase radius";
        radius = rad.getInt();

        config.save();
    }


    public class xpHandler {
        private boolean all;
        private int radius;
        private double xpMultiplier;
        private boolean noGroup;

        public xpHandler(double xpMultiplier, int radius)
        {
            this.all = xpMultiplier == 0.0D;
            this.xpMultiplier = xpMultiplier;
            this.radius = radius;
            this.noGroup = radius == 0;
        }

        @SubscribeEvent
        public void xpHandler(EntityJoinWorldEvent e)
        {
            if (!e.world.isRemote) {
                if (e.entity instanceof EntityXPOrb) {
                    if (this.all || e.world.rand.nextDouble() > xpMultiplier) {
                        e.setCanceled(true);
                        return;
                    }
                    if (noGroup) return;
                    EntityXPOrb orb = (EntityXPOrb) e.entity;
                    AxisAlignedBB axisAlignedBB = AxisAlignedBB.getBoundingBox(orb.posX - radius, orb.posY - radius, orb.posZ - radius, orb.posX + radius, orb.posY + radius, orb.posZ + radius);
                    List<EntityXPOrb> nearbyOrbs = (List<EntityXPOrb>) e.world.getEntitiesWithinAABB(EntityXPOrb.class, axisAlignedBB);
                    if (nearbyOrbs.isEmpty()) return;
                    for (EntityXPOrb nearbyOrb : nearbyOrbs) {
                        if (nearbyOrb.isDead) continue;
                        orb.xpValue += nearbyOrb.getXpValue();
                        e.world.removeEntity(nearbyOrb);
                    }
                    return;
                }
            }
        }
    }


}