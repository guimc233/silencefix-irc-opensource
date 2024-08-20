package ltd.guimc.silencefix;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import static dev.faiths.utils.IMinecraft.mc;
import static ltd.guimc.silencefix.SilenceFixIRC.Instance;

public class SFIRCListener implements Listener {
    private MSTimer msTimer = new MSTimer();

    private final Handler<TickUpdateEvent> worldEventHandler = event -> {
        if (!msTimer.check(2000)) return;
        if (Instance.ircUser == null) return;
        Instance.sendPacket(Messages.createSetMinecraftProfile(getUUID()));
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityPlayer) {
                Instance.sendPacket(Messages.createQueryPlayer(entity.getUniqueID().toString(), 0));
            }
        }
        msTimer.reset();
    };

    @Override
    public boolean isAccessible() {
        return true;
    }

    private static String getUUID() {
        return mc.thePlayer == null ? mc.getSession().getPlayerID() : mc.thePlayer.getUniqueID().toString();
    }
}
