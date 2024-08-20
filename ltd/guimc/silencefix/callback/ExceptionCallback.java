package ltd.guimc.silencefix.callback;

import dev.faiths.utils.ClientUtils;
import ltd.guimc.silencefix.SilenceFixIRC;

public class ExceptionCallback implements SilenceFixIRC.ExceptionCallback {
    @Override
    public void callback(Throwable var1) {
        ClientUtils.displayChatMessage("xinxin irc error: ");
        var1.printStackTrace();
    }
}
