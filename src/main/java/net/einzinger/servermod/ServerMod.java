package net.einzinger.servermod;

import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(ServerMod.MOD_ID)
public class ServerMod
{
    public static final String MOD_ID = "servermod";
    private static final Logger LOGGER = LogUtils.getLogger();

    private Timer timer;
    private MinecraftServer server;

    public ServerMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Timer für das Senden der Spieler-Daten initialisieren
        timer = new Timer();
        timer.schedule(new SendPlayerDataTask(), 0, 5000); // Alle 5 Sekunden(5000ms) senden
    }

    private class SendPlayerDataTask extends TimerTask {
        @Override
        public void run(){
            if(server != null){
                List<ServerPlayer> players = server.getPlayerList().getPlayers();
                for(ServerPlayer player : players){
                    String playerName = player.getName().toString();
                    int playerLevel = player.experienceLevel;

                    // JSON-Objekt aus den Daten erzeugen
                    JsonObject json = new JsonObject();
                    json.addProperty("playerName", playerName);
                    json.addProperty("playerLevel", playerLevel);

                    // Sende Daten an Julian´s PC
                    sendPlayerData(json.toString());
                }
            }
        }
    }

    private void sendPlayerData(String jsonData) {
        try {
            // URL von Julian´s PC
            URL url = new URL("http:185.253.17.65:8080");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setDoOutput(true);

            // Daten an den Server senden
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Serverantwort lesen
            int responseCode = conn.getResponseCode();
            System.out.println("Response Code: " + responseCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("HELLO from server starting");
        this.server = event.getServer(); // Speichert das MC-Server Objekt
    }
    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }

}
