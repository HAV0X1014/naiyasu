package naiyasu.chat;

import naiyasu.util.ActivityBlock;
import naiyasu.util.RandomEvent;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static naiyasu.NaiMain.scheduler;

public class Character {
    private Channel channel;
    private String webhookURL;
    private String name;
    private String description;
    private List<ActivityBlock> activityBlocks;
    private String[] emotions;
    private List<RandomEvent> randomEvents;
    private ChatLockout lock;
    private RandomEvent currentEvent;
    private ActivityBlock currentBlock;
    private boolean aiRequestProcessing = false;

    /**
     * Object running the loops for a character interactions, as well as containing all info related to the character's config, lockout, etc.
     * @param channel JDA Channel that the character reads and sends messages from.
     * @param webhookURL The webhook URL this character will send through.
     * @param name The name of the character.
     * @param description The description of the character, including world details.
     * @param activityBlocks The ActivityBlocks this character should be active during.
     * @param emotions *Unused* the emotions this character can have, used in random events.
     * @param randomEvents The random events that can happen to this character.
     */
    public Character(Channel channel, String webhookURL, String name, String description, List<ActivityBlock> activityBlocks, String[] emotions, List<RandomEvent> randomEvents) {
        this.channel = channel;
        this.webhookURL = webhookURL;
        this.name = name;
        this.description = description;
        this.activityBlocks = activityBlocks;
        this.emotions = emotions;
        this.randomEvents = randomEvents;
        //new lockout specific to the character, not shared between characters
        this.lock = new ChatLockout();
        this.currentBlock = null;

        scheduler.scheduleAtFixedRate(() -> {
            LocalTime now = LocalTime.now();
            double messagesPerSecond = 0;
            //dont process if the lockout is active (which means that we have chatted recently)
            if (!lock.isLocked()) {
                for (ActivityBlock segment : activityBlocks) {
                    if (segment.isInBlock(now)) {
                        currentBlock = segment;
                        messagesPerSecond = segment.getMessagesPerBlock() / segment.getBlockDurationSeconds();
                    }
                }
                //Send a message based on the set target message amount. messagesPer(duration) is not exact, just a target
                //change 60.0 to 3600.0 seconds in an hour to make this messagesPerHour
                /*
                for future reference, how this maths out is that we need to divide by the length of the messagesPer(timeunit) in seconds
                so if we want to do messagesPerMinute, its divided by 60. if we want to do messagesPerHour its divided by 3600.
                the reason is because this function is called every one second, so that is how often we need to calulate a new random double.
                (unrelated) now that i think about it, this feels like an LLM's temperature setting...
                SOLVED! just do desiredMessagesPerBlock divided by blockDuration, and that will give you the messagesPerSecond you need!
                 */
                double rnd = ThreadLocalRandom.current().nextDouble();
                if (rnd < messagesPerSecond) {
                    //AI gen an idle response
                    try (TypeContinuously tc = new TypeContinuously((MessageChannelUnion) channel)) {
                        Thread idleMessage = new Thread(() -> {
                            TactAI instance = new TactAI();
                            instance.aiRequest(this);
                        });
                        idleMessage.start();
                    }
                }
            }
        }, 0, 1, TimeUnit.SECONDS); // Run every second

        //set up a scheduler for random events to be chosen, run once somewhere between 45 minutes to 2 hours
        scheduler.scheduleAtFixedRate(() -> {
            //if we get the 45 roll to be true, then try to get a random (actual) event
            if (ThreadLocalRandom.current().nextInt(100) < 45) {
                //if we are allowed to get a random event
                List<RandomEvent> validEvents = new ArrayList<>();
                for (RandomEvent event : randomEvents) {
                    if (RandomEvent.isValid(event,currentBlock)) {
                        validEvents.add(event);
                    }
                }
                currentEvent = validEvents.get(ThreadLocalRandom.current().nextInt(validEvents.size()));
            } else {
                currentEvent = new RandomEvent("ALL", "Default Event", "Continue the current chat, or create your own new topic.");
            }
        },0, ThreadLocalRandom.current().nextInt(45,61),TimeUnit.MINUTES);
    }

    public Channel getChannel() {
        return channel;
    }

    public ChatLockout getLockout() {
        return lock;
    }

    public List<ActivityBlock> getActivityBlocks() {
        return activityBlocks;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getWebhookURL() {
        return webhookURL;
    }

    public RandomEvent getCurrentEvent() {
        return currentEvent;
    }

    public ActivityBlock getCurrentBlock() {
        return currentBlock;
    }

    public synchronized boolean isAiRequestProcessing() {
        return aiRequestProcessing;
    }

    public synchronized void setAiRequestProcessing(boolean processing) {
        this.aiRequestProcessing = processing;
    }

    public List<RandomEvent> getRandomEvents() {
        return randomEvents;
    }

    public void setCurrentEvent(RandomEvent currentEvent) {
        this.currentEvent = currentEvent;
    }
}
