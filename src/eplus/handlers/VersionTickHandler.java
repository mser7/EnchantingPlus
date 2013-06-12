package eplus.handlers;

import cpw.mods.fml.common.IScheduledTickHandler;
import cpw.mods.fml.common.TickType;
import net.minecraft.entity.player.EntityPlayer;

import java.util.EnumSet;

/**
 * @author Freyja
 *         Lesser GNU Public License v3 (http://www.gnu.org/licenses/lgpl.html)
 */
public class VersionTickHandler implements IScheduledTickHandler
{
    private boolean messageSent;

    @Override
    public void tickStart(EnumSet type, Object... tickData) {
        if(messageSent)
            return;

        EntityPlayer player = (EntityPlayer) tickData[0];

        if(Version.versionSeen() && Version.isVersionCheckComplete()) {
            if(Version.hasUpdated()){
                player.sendChatToPlayer(String.format("[EPLUS]: %s: %s", "Version update is available", Version.getRecommendedVersion()));
            }
        }
        messageSent = true;
    }

    @Override
    public void tickEnd(EnumSet type, Object... tickData) {
    }

    @Override
    public EnumSet ticks() {
        if (this.messageSent) {
            return EnumSet.noneOf(TickType.class);
        }
        return EnumSet.of(TickType.PLAYER);
    }

    @Override
    public String getLabel() {
        return "Enchanting Plus update message";
    }

    @Override
    public int nextTickSpacing() {
        return 100;
    }
}