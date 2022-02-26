package client;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import game.ClientSession;

public class ClientDesktopLauncher {
    public static void main (String[] arg) {
        LwjglApplicationConfiguration applicationConfiguration = new LwjglApplicationConfiguration();
        applicationConfiguration.title = "ARSWA";
        applicationConfiguration.width = 1200;
        applicationConfiguration.height = 800;
        applicationConfiguration.foregroundFPS = 10;
        applicationConfiguration.backgroundFPS = 10;
        applicationConfiguration.resizable = false;

        ClientSession clientSession = new ClientSession("baseScene");

        new LwjglApplication(clientSession, applicationConfiguration);
    }
}
