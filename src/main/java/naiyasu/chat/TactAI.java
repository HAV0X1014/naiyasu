package naiyasu.chat;

import naiyasu.util.ConfigHandler;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.WebhookClient;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.WebhookAction;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static naiyasu.NaiMain.jda;
import static naiyasu.NaiMain.self;

public class TactAI {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    public void aiRequest(Character character) {
        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).build();
        String systemPrompt =  "You are " + character.getName() + ", and this is the description for them: " + character.getDescription() +
                " Your task is to chat with the personality of " + character.getName() + " with others in an instant messenger text chat; type in a natural, easygoing manner fitting for the conversation." +
                " You should only respond as " + character.getName() + ", do not respond as other users." +
                " Adhere to your character's personality in responses, but behaving in a natural manner takes precedent." +
                //" The current topic to keep in mind is - " + character.getCurrentEvent().getDetails() +
                "\nChat history to make a response to is below.\n";
                /*
                "You are " + character.getName() + ", using an instant messenger." +
                " Your task is to chat with the personality of " + character.getName() + " with others in an online text chat; type in a natural, easygoing manner fitting for the conversation, do not use \"action tags\"." +
                " You should only respond as " + character.getName() + ", do not respond as other users." +
                " Adhere to your character's personality in responses, but behaving in a natural manner takes precedent." +
                " The current topic to discuss is - " + character.getCurrentEvent().getDetails() +
                "\nThis is the description for " + character.getName() +": \n" + character.getDescription() +
                "\n\n> Keep in mind that this is not roleplay, it is just chatting.";

                "Engage in a natural-sounding conversation within a group chat setting, adopting the distinct personality, voice, and linguistic habits of the designated character \"" + character.getName() + "\"." +
                " As \"" + character.getName() + "\", respond authentically, using their typical phrasing and tone. Only message as \"" + character.getName() + "\", never impersonating or addressing others in the chat." +
                " When joining an existing conversation, strive to stay on-topic while also prioritizing character consistency and the current situation." +
                " If needed, introduce new topics to maintain a plausible discussion flow. Refer to the provided character description, which outlines their personality, traits, and world context, to inform your responses.";
                 */

        //get the last 30 messages in the channel, and add them to the message history.
        //this goes newest to oldest so we need to flip it around for it to make any sense.
        JSONArray messages = new JSONArray();
        //todo: local caching? repeatedly requesting messages from discord like this is wasteful.
        TextChannel channel = (TextChannel) character.getChannel();
        List<Message> m = channel.getHistory().retrievePast(10).complete();
        List<String> chatHistory = new ArrayList<>();
        for (Message message : m) {
            //if the message has no content (embed, sticker, etc)
            if (!message.getContentDisplay().isEmpty()) {
                //JSONObject reply = new JSONObject();
                //if it came from a webhook, then add it to history as assistant
                if (message.isWebhookMessage()) {
                    chatHistory.add("\n" + character.getName() + ": " + message.getContentDisplay());
                    //reply.put("role", "assistant");
                    //reply.put("content", message.getContentDisplay());
                    //messages.put(reply);
                }
                //if it came from anything other than a bot (user), add it to history as a user
                if (!message.getAuthor().isBot()) {
                    chatHistory.add("\n" + message.getAuthor().getEffectiveName() + ": " + message.getContentDisplay());
                    //reply.put("role", "user");
                    //reply.put("content", message.getAuthor().getEffectiveName() + ": " + message.getContentDisplay());
                    //messages.put(reply);
                }
            }
        }
        Collections.reverse(chatHistory);

        JSONObject systemPromptObject = new JSONObject();
        systemPromptObject.put("role","system");
        systemPromptObject.put("content",systemPrompt);
        messages.put(systemPromptObject);

        JSONObject chatHistoryObject = new JSONObject();
        chatHistoryObject.put("role","system");
        chatHistoryObject.put("content","Chat history: " + chatHistory +
                "\nKeep in mind that this is not roleplay, it is just chatting. Do not use action tags like \"*smirks*\", \"*stretches arms*\", etc. Do not make responses as other characters, and do not start your response with " + character.getName() +
                " The current topic to keep in mind is - " + character.getCurrentEvent().getDetails());
        messages.put(chatHistoryObject);

        JSONArray chronologicalMessageOrder = new JSONArray();
        //flip the whole message order around
        for (int i = messages.length() - 1; i >= 0; i--) {
            JSONObject jsonObject = messages.getJSONObject(i);
            chronologicalMessageOrder.put(jsonObject);
        }
        /*
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("HH:mm aa yyyy/MM/dd");
        String secondSystemPrompt = character.getName() + ", continue the conversation as normal with the following additional information." +
                " The current time and date are " + dateFormat.format(date) +
                ", the current day segment is " + character.getCurrentBlock().getName() +
                ". The last message was sent " + Duration.between(newestMessageTimestamp, OffsetDateTime.now()).toMinutes() + " minutes ago.";

        JSONObject secondSystemPromptObject = new JSONObject();
        secondSystemPromptObject.put("role","system");
        secondSystemPromptObject.put("content", secondSystemPrompt);

        //put the second system prompt here so it shows up as the most recent thing - always front of mind
        chronologicalMessageOrder.put(secondSystemPromptObject);
         */

        JSONObject parameters = new JSONObject();
        /*
        if (!imageData.isEmpty()) {
            parameters.put("image_data", imageData);
        }
        */
        parameters.put("max_tokens", 420);
        parameters.put("temperature", 1);
        parameters.put("top_p", .9);
        parameters.put("top_k", 30);
        parameters.put("repeat_penalty", 1);
        parameters.put("repeat_last_n", 64);
        parameters.put("messages", chronologicalMessageOrder);
        //old settings
        //top_p = .9
        //top_k = 40
        //System.out.println(parameters.toString(1));
        RequestBody requestBody = RequestBody.create(JSON, parameters.toString());
        try {
            URL url = new URL(ConfigHandler.getString("AIServerEndpoint"));     //chat endpoint allows chatting, and not just prompt continuing
            Request request = new Request.Builder().url(url).post(requestBody).addHeader("Authorization","Bearer " + ConfigHandler.getString("AIKey")).build();         //make the actual post request

            String responseContent;                                                 //declare the content-holding string
            try (Response resp = client.newCall(request).execute()) {               //send request to the server
                responseContent = resp.body().string();                             //get the returned content and put it in the respective string
            }
            JSONObject jsonObject = new JSONObject(responseContent);                //put all of the response JSON into an object
            String output = jsonObject.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
            //channel.sendMessage(output).queue();
            WebhookClient.createClient(jda, character.getWebhookURL()).sendMessage(output).queue();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}