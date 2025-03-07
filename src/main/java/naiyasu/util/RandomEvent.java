package naiyasu.util;

public class RandomEvent {
    private String validTimes;
    private String name;
    private String details;

    /**
     * Object containing the information pertaining to a random event to be supplied to AI interactions.
     * @param validTimes
     * @param name
     * @param details
     */
    public RandomEvent(String validTimes, String name, String details) {
        this.validTimes = validTimes;
        this.name = name;
        this.details = details;
    }

    /**
     * Checks if the supplied RandomEvent's valid times are within the supplied ActivityBlock.
     * @param event The RandomEvent to check.
     * @param activityBlock The ActivityBlock to compare against.
     * @return True/False if the RandomEvent is/isn't allowed to happen in the ActivityBlock.
     */
    public static boolean isValid(RandomEvent event, ActivityBlock activityBlock) {
        if (event.validTimes.equals("ALL")) return true;
        //if the name of the activity block is within the random event's valid times, return true
        return event.validTimes.contains(activityBlock.getName());
    }

    public String getDetails() {
        return details;
    }

    public String getName() {
        return name;
    }

    public String getValidTimes() {
        return validTimes;
    }
}
