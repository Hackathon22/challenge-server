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
        applicationConfiguration.foregroundFPS = 60;
        applicationConfiguration.backgroundFPS = 60;
        applicationConfiguration.resizable = false;

        ClientSession clientSession = new ClientSession("src/main/resources/scenes/base_scene.xml");

        new LwjglApplication(clientSession, applicationConfiguration);
    }
}
