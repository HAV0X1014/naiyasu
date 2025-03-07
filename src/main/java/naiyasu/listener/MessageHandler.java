package naiyasu.listener;

import naiyasu.chat.Character;
import naiyasu.chat.TactAI;
import naiyasu.chat.TypeContinuously;
import naiyasu.util.ActivityBlock;
import naiyasu.util.RandomEvent;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static naiyasu.NaiMain.*;

public class MessageHandler extends ListenerAdapter {
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent mc) {
        //this is where we will handle new messages from users coming in, largely 'active' chat logic
        //reject webhook and bot messages from causing chat responses
        if (!mc.getAuthor().isBot() || !mc.getMessage().isWebhookMessage()) {
            for (Character character : characters) {
                if (character.getChannel().equals(mc.getChannel())) {
                    //launch the chat code
                    String messageContent = mc.getMessage().getContentRaw();
                    if (messageContent.startsWith("[")) {
                        switch (messageContent) {
                            case "[forcechat" -> character.getLockout().startLockout();
                            case "[endchat" -> character.getLockout().unlock();
                            case "[randomevent" -> {
                                List<RandomEvent> validEvents = new ArrayList<>();
                                for (RandomEvent event : character.getRandomEvents()) {
                                    if (RandomEvent.isValid(event, character.getCurrentBlock())) {
                                        validEvents.add(event);
                                    }
                                }
                                character.setCurrentEvent(validEvents.get(ThreadLocalRandom.current().nextInt(validEvents.size())));
                                System.out.println(character.getCurrentEvent().getDetails());
                                mc.getMessage().reply(character.getCurrentEvent().getDetails()).queue();
                            }
                            case "[getcurrentevent" -> {
                                System.out.println(character.getCurrentEvent().getDetails());
                                mc.getMessage().reply(character.getCurrentEvent().getDetails()).queue();
                            }
                        }
                        return;
                    }
                    LocalTime now = LocalTime.now();
                    double chatChance = 0;
                    for (ActivityBlock segment : character.getActivityBlocks()) {
                        if (segment.isInBlock(now)) {
                            chatChance = segment.getChatChance() / 100;
                        }
                    }
                    double random = new Random().nextDouble();
                    if ((random < chatChance) || character.getLockout().isLocked()) {
                        if (!character.isAiRequestProcessing()) {
                            System.out.println("starting chat...");
                            character.setAiRequestProcessing(true);
                            //add random delay of 2-8 seconds before making a response
                            Thread chat = new Thread(() -> {
                                try (TypeContinuously tc = new TypeContinuously((MessageChannelUnion) character.getChannel())) {
                                    int minSeconds = 2;
                                    int maxSeconds = 8;
                                    int randomDelay = ThreadLocalRandom.current().nextInt(minSeconds, maxSeconds + 1) * 1000; // Convert to milliseconds
                                    try {
                                        Thread.sleep(randomDelay);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                    TactAI instance = new TactAI();
                                    try {
                                        instance.aiRequest(character);
                                    } finally {
                                        // **CRITICAL CHANGE: Ensure the flag is reset in a finally block**
                                        character.setAiRequestProcessing(false);
                                    }
                                }
                            });
                            chat.start();
                            //if we have chatted, prevent idle messages from being sent for the next 10-15 minutes
                            character.getLockout().startLockout();
                        }
                    }
                }
            }
        }
    }
}
