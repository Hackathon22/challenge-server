package client;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import game.DesktopClient;
import game.WindowlessClient;
import org.apache.commons.cli.*;

public class ClientDesktopLauncher {
    public static void main(String[] arg) throws ParseException, IllegalArgumentException {
        Options options = new Options();
        options.addRequiredOption("m", "mode", true, "The mode of the server to run (window, windowless and replay)");
        options.addOption("f", "file", true, "Path to the file where to save the AI actions or where to replay the actions.");
        options.addOption("t", "gametime", true, "Game time (in seconds) - Default: 60s");
        options.addOption("a", "aitime", true, "Time dedicated for each AI to compute. - Default: 150s");
        options.addOption("c", "cps", true, "Commands per seconds (asked to the python AI). - Default: 4 per second");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, arg);

        String mode = cmd.getOptionValue("m");
        String file = cmd.getOptionValue("f");
        String gameTime = cmd.getOptionValue("t");
        String aiTime = cmd.getOptionValue("a");
        String commandsPerSecond = cmd.getOptionValue("c");

        float actualGameTime = 0f;
        float actualAITime = 0f;
        float actualCommandsPerSeconds = 0f;

        if (gameTime != null)
            actualGameTime = Float.parseFloat(gameTime);
        else
            actualGameTime = 60f;

        if (aiTime != null)
            actualAITime = Float.parseFloat(aiTime);
        else
            actualAITime = 150f;

        if (commandsPerSecond != null)
            actualCommandsPerSeconds = Float.parseFloat(commandsPerSecond);
        else
            actualCommandsPerSeconds = 4f;

        if (mode.equals("window")) {
            LwjglApplicationConfiguration applicationConfiguration = new LwjglApplicationConfiguration();
            applicationConfiguration.title = "ARSWA";
            applicationConfiguration.width = 1200;
            applicationConfiguration.height = 800;
            applicationConfiguration.foregroundFPS = 60;
            applicationConfiguration.backgroundFPS = 60;
            applicationConfiguration.resizable = false;

            DesktopClient desktopClient = new DesktopClient(null, 90f);
            new LwjglApplication(desktopClient, applicationConfiguration);
        }
        else if (mode.equals("windowless")) {
            if (file == null) {
                throw new IllegalArgumentException("Please specify the save file path when running on windowless mode.");
            }
            if (commandsPerSecond == null)  {
            }
            WindowlessClient client = new WindowlessClient(file, actualGameTime, actualAITime, actualCommandsPerSeconds);
            client.create();
            client.play();
        }
        else if (mode.equals("replay")) {
            // TODO implement
        }
        else {
            throw new IllegalArgumentException("Unknown challenge mode: " + mode);
        }
    }
}
