package naiyasu.util;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ActivityBlock {
    LocalTime startTime;
    LocalTime endTime;
    private double messagesPerBlock;
    double chatChance;
    String name;
    /**
     * @param startTime The time that the block starts.
     * @param endTime The time that the block ends.
     * @param messagesPerBlock The targeted amount of messages during this block.
     * @param chatChance The probability of starting a chat during this block out of 100.
     * @param name The name of this block period, like "evening", "lunch", "bedtime (sleeping)".
     */
    public ActivityBlock(String startTime, String endTime, double messagesPerBlock, double chatChance, String name) {

        this.startTime = parseLocalTime(startTime);
        this.endTime = parseLocalTime(endTime);
        this.setMessagesPerBlock(messagesPerBlock);
        this.chatChance = chatChance;
        this.name = name;
    }
    public boolean isInBlock(LocalTime currentTime) {
        if (startTime.isBefore(endTime)) {  // Normal case: start is before end
            return !currentTime.isBefore(startTime) && currentTime.isBefore(endTime);
        } else { // Handles cases where the block crosses midnight (e.g., 10 PM to 2 AM)
            return !currentTime.isBefore(startTime) || currentTime.isBefore(endTime);
        }
    }
    @Override
    public String toString() {
        return "ActivityBlock{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                ", messagesPerMinute=" + getMessagesPerBlock() +
                '}';
    }

    public static LocalTime parseLocalTime(String timeString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm"); // Allow single digit hours
        try {
            return LocalTime.parse(timeString, formatter); // Uses the default DateTimeFormatter.ISO_LOCAL_TIME which defaults to HH:mm
        }
        catch (DateTimeParseException e) {
            formatter = DateTimeFormatter.ofPattern("HH:mm"); //Try 2-digit hours
            try {
                return LocalTime.parse(timeString, formatter);
            }
            catch (DateTimeParseException e2){
                throw new DateTimeParseException("Invalid time format. Expected HH:mm or H:mm", timeString, e.getErrorIndex(), e);
            }

        }
    }

    public long getBlockDurationSeconds() {
        if (startTime.isBefore(endTime)) {
            return Duration.between(startTime,endTime).getSeconds();
        } else {
            return Duration.between(startTime, LocalTime.MAX).getSeconds() + Duration.between(LocalTime.MIN, endTime).getSeconds() + 1;
            //+1 to also include 23:59:59 to 00:00:00
        }
    }

    public double getMessagesPerBlock() {
        return messagesPerBlock;
    }

    public void setMessagesPerBlock(double messagesPerHour) {
        this.messagesPerBlock = messagesPerHour;
    }

    public double getChatChance() {
        return chatChance;
    }

    public void setChatChance(double chatChance) {
        this.chatChance = chatChance;
    }

    public String getName() {
        return name;
    }
}
