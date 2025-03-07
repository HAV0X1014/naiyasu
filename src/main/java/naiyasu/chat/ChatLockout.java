package naiyasu.chat;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import static naiyasu.NaiMain.jda;

public class ChatLockout {
    private final AtomicBoolean isCooldownActive = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Random random = new Random();

    private ScheduledFuture<?> cooldownTaskFuture = null; // To hold the future of the scheduled task
    private final ReentrantLock lock = new ReentrantLock(); // For synchronization

    public void startLockout() {
        lock.lock(); // Acquire the lock to protect shared state

        try {
            if (isCooldownActive.get()) {
                // Cooldown is already active, cancel the existing task
                if (cooldownTaskFuture != null) {
                    cooldownTaskFuture.cancel(false); // Cancel the previous task (do not interrupt if running)
                } else {
                    System.out.println("Cooldown active, but no task to cancel (this should not happen).");
                }
            } else {
                isCooldownActive.set(true);
                jda.getPresence().setPresence(OnlineStatus.ONLINE,false);
                jda.getPresence().setActivity(Activity.customStatus("Lets chat!"));
            }

            // Calculate random cooldown duration, somewhere between 10-15 minutes
            int cooldownSeconds = 10 * 60 + random.nextInt(5 * 60 + 1);

            // Schedule the task to set the isCooldownActive boolean to false later
            //this is run after 10-15 minutes
            cooldownTaskFuture = scheduler.schedule(() -> {
                lock.lock(); // Acquire the lock before modifying shared state
                try {
                    isCooldownActive.set(false);
                    cooldownTaskFuture = null;  // Clear the future
                    jda.getPresence().setPresence(OnlineStatus.IDLE,true);
                    jda.getPresence().setActivity(Activity.customStatus("Probably busy, out flying!"));
                } finally {
                    lock.unlock(); // Release the lock
                }
            }, cooldownSeconds, TimeUnit.SECONDS);

        } finally {
            lock.unlock(); // Ensure the lock is always released
        }
    }

    public boolean isLocked() {
        return isCooldownActive.get();
    }

    public void unlock() {
        lock.lock();
        try {
            isCooldownActive.set(false);
            cooldownTaskFuture = null;  // Clear the future
            System.out.println("[force unlocked]");
            jda.getPresence().setPresence(OnlineStatus.IDLE, true);
            jda.getPresence().setActivity(Activity.customStatus("Probably busy, out flying!"));
        } finally {
            lock.unlock();
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}


