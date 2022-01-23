package client;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import net.ClientSession;

public class ClientDesktopLauncher {
    public static void main (String[] arg) {
        LwjglApplicationConfiguration applicationConfiguration = new LwjglApplicationConfiguration();
        applicationConfiguration.title = "ARSWA";
        applicationConfiguration.width = 1200;
        applicationConfiguration.height = 800;
        applicationConfiguration.foregroundFPS = 60;
        applicationConfiguration.backgroundFPS = 60;

        ClientSession clientSession = new ClientSession();

        new LwjglApplication(clientSession, applicationConfiguration);
    }
}
