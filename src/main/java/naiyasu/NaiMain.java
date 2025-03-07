package naiyasu;

import naiyasu.chat.Character;
import naiyasu.listener.MessageHandler;
import naiyasu.util.ActivityBlock;
import naiyasu.util.ConfigHandler;
import naiyasu.util.FileHandler;
import naiyasu.util.RandomEvent;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;

public class NaiMain {
    public static JDA jda;
    public static String config;
    public static User self;
    public static List<Character> characters = new ArrayList<>();
    public static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(6);

    public static void main(String[] args) throws InterruptedException {
        config = FileHandler.read(new File("ServerFiles/config.json"));
        jda = JDABuilder.createDefault(ConfigHandler.getString("Token"), EnumSet.allOf(GatewayIntent.class))
                .addEventListeners(new MessageHandler())
                .setActivity(Activity.customStatus("naiyasu started."))
                .build();
        jda.awaitReady();
        self = jda.getSelfUser();
        System.out.println("Logged in as " + jda.getSelfUser().getName());

        //for each character file in ServerFiles/Characters/, make a character object representing them and containing all of
        //their relevant info, like day segments, random events, channel id, emotions, etc.
        //each one of those objects will run their own idle message loop logic
        //im going to pass in the character's specifics here, in case it bugs out. that way it wont happen inside of the object.
        File[] characterFiles = new File("ServerFiles/Characters/").listFiles((dir,name) -> name.endsWith(".json"));
        assert characterFiles != null;

        //character object setup from JSON file and init
        for (File characterFile : characterFiles) {
            JSONObject characterJSON = new JSONObject(FileHandler.read(characterFile));

            Channel channel = jda.getTextChannelById(characterJSON.getString("ChannelID"));
            String webhookURL = characterJSON.getString("WebhookURL");
            String name = characterJSON.getString("Name");
            String description = characterJSON.getString("Description");
            List<ActivityBlock> activityBlocks = new ArrayList<>();
            //for each entry in "DaySegments"
            for (int i = 0; i < characterJSON.getJSONArray("DaySegments").length(); i++) {
                JSONObject segment = characterJSON.getJSONArray("DaySegments").getJSONObject(i);
                //make an ActivityBlock object out of the defined DaySegment in the config
                activityBlocks.add(new ActivityBlock(segment.getString("Start"),
                        segment.getString("End"),
                        segment.getDouble("MessagesPerBlock"),
                        segment.getDouble("ChatChance"),
                        segment.getString("Name")));
            }
            //iterator that just makes the JSONArray into a regular string array
            String[] emotions = Stream.iterate(0, i -> i + 1).limit(characterJSON.getJSONArray("Emotions").length())
                    .map(characterJSON.getJSONArray("Emotions")::getString).toArray(String[]::new);
            List<RandomEvent> randomEvents = new ArrayList<>();
            for (int i = 0; i < characterJSON.getJSONArray("RandomEvents").length(); i++) {
                JSONObject event = characterJSON.getJSONArray("RandomEvents").getJSONObject(i);
                randomEvents.add(new RandomEvent(event.getString("ValidTimes"),
                        event.getString("EventName"),
                        event.getString("Details")));
            }
            //finally we can actually make the freakin' object!
            //add it to the characters arraylist so we can iterate over them later
            characters.add(new Character(channel,webhookURL,name,description,activityBlocks,emotions,randomEvents));
        }
    }
}
