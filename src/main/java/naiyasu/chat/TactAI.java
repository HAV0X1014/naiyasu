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
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static naiyasu.NaiMain.jda;
import static naiyasu.NaiMain.self;

public class TactAI {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    public void aiRequest(Character character) {
        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).build();
        /*
        String systemPrompt = "You are " + character.getName() + ". You will respond to user input in a short, realistic way as if you were sending a few casual texts to each other. " +
                "This chat takes place in a group chat on Discord, try to adhere to " + character.getName() + "'s personality throughout the discussion, respond only as " + character.getName() + ", and no other characters. " +
                "Use language fitting for a casual, easygoing text chat, and go with the flow of conversation. " +
                "Note, the name of the user speaking to you is enclosed in brackets, their names do not include brackets so do not use them in replies or enclose your own name in brackets. " +
                "Message history is included for you to reference, but you can make your own topic of chat if you desire to.\n" +
                character.getDescription();
         */
        String systemPrompt = "This chat happens in a group chat that you, \"" + character.getName() + "\", are in. Respond in a manner fitting and true to " + character.getName() + "'s personality," +
                " and use language they would use themselves. Only make messages from the perspective of " + character.getName() +
                " and do not make responses as anyone else in the chat. Try to go with the flow of the conversation, but the current event and character needs take precedent." +
                " The recent history of the chat is included for you to reference, but you can make your own topic of chat if desired. The personality, character, and world description for " + character.getName() +
                " is listed as follows.\n" + character.getDescription();

        JSONObject systemPromptObject = new JSONObject();
        systemPromptObject.put("role","system");
        systemPromptObject.put("content",systemPrompt);

        //get the last 30 messages in the channel, and add them to the message history.
        //this goes newest to oldest so we need to flip it around for it to make any sense.
        JSONArray messages = new JSONArray();
        //todo: local caching? repeatedly requesting messages from discord like this is wasteful.
        TextChannel channel = (TextChannel) character.getChannel();
        List<Message> m = channel.getHistory().retrievePast(30).complete();
        boolean gotNewestMessageTimestamp = false;
        OffsetDateTime newestMessageTimestamp = null;
        for (Message message : m) {
            //if the message has no content (embed, sticker, etc)
            if (!message.getContentDisplay().isEmpty()) {
                if (!gotNewestMessageTimestamp) {
                    newestMessageTimestamp = message.getTimeCreated();
                    gotNewestMessageTimestamp = true;
                }
                JSONObject reply = new JSONObject();
                //if it came from a webhook, then add it to history as assistant
                if (message.isWebhookMessage()) {
                    reply.put("role", "assistant");
                    reply.put("content", message.getContentDisplay());
                    messages.put(reply);
                }
                //if it came from anything other than a bot (user), add it to history as a user
                if (!message.getAuthor().isBot()) {
                    reply.put("role", "user");
                    reply.put("content", message.getAuthor().getEffectiveName() + ": " + message.getContentDisplay());
                    messages.put(reply);
                }
            }
        }
        messages.put(systemPromptObject);

        JSONArray chronologicalMessageOrder = new JSONArray();
        //flip the whole message order around
        for (int i = messages.length() - 1; i >= 0; i--) {
            JSONObject jsonObject = messages.getJSONObject(i);
            chronologicalMessageOrder.put(jsonObject);
        }

        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("HH:mm aa yyyy/MM/dd");
        String secondSystemPrompt = "Important chat info: Current real time and date - " + dateFormat.format(date) + ", Current day segment - " + character.getCurrentBlock().getName() +
                ", Your current event/topic to discuss while continuing the chat: " + character.getCurrentEvent().getDetails() +
                "\nThe last message in the chat was sent " + Duration.between(newestMessageTimestamp, OffsetDateTime.now()).toMinutes() + " minutes ago.\n" +
                "Remember to continue that chat by creating responses that are representative of " + character.getName() + "'s personality - don't overuse acronyms or internet slang. " +
                "Try not to repeat any phrases or words, and be sure to provide a natural chatting experience!";
        JSONObject secondSystemPromptObject = new JSONObject();
        secondSystemPromptObject.put("role","system");
        secondSystemPromptObject.put("content", secondSystemPrompt);

        //put the second system prompt here so it shows up as the most recent thing - always front of mind
        chronologicalMessageOrder.put(secondSystemPromptObject);

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
        parameters.put("repeat_penalty", 1.2);
        parameters.put("repeat_last_n", -1);
        parameters.put("messages", chronologicalMessageOrder);
        //old settings
        //top_p = .9
        //top_k = 40
        System.out.println(parameters.toString(1));
        RequestBody requestBody = RequestBody.create(JSON, parameters.toString());
        try {
            URL url = new URL(ConfigHandler.getString("AIServerEndpoint"));     //chat endpoint allows chatting, and not just prompt continuing
            Request request = new Request.Builder().url(url).post(requestBody).build();         //make the actual post request

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