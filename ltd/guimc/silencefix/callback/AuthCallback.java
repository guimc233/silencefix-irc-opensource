package ltd.guimc.silencefix.callback;

import dev.faiths.utils.ClientUtils;
import ltd.guimc.silencefix.SilenceFixIRC;

public class AuthCallback implements SilenceFixIRC.AuthCallback {
    @Override
    public void callback(String var2) {
        ClientUtils.displayChatMessage("xinxin irc auth msg: " + var2);
    }
}
