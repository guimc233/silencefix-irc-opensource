package ltd.guimc.silencefix.callback;

import dev.faiths.utils.ClientUtils;
import ltd.guimc.silencefix.SilenceFixIRC;

public class LogCallback implements SilenceFixIRC.LogCallback {
    @Override
    public void callback(String var1) {
        ClientUtils.displayChatMessage("xinxin irc log: " + var1);
    }
}
