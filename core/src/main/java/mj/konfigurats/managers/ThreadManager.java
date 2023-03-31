package mj.konfigurats.managers;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadManager {
	private final ExecutorService executor;
	private Timer timer;
	
	public ThreadManager() {
		executor = Executors.newSingleThreadExecutor();
		timer = new Timer();
	}
	
	public void dispose() {
		if(timer != null) {
			timer.cancel();
		}
		if(executor != null) {
			executor.shutdownNow();
		}
	}
	
	/**
	 * Schedules a runnable for the network thread.
	 * @param runnable the runnable to be executed.
	 */
	public void executeOnThread(Runnable runnable) {
		executor.execute(runnable);
	}
	
	/**
	 * Schedules a task on the timer.
	 * @param task the task to be executed.
	 * @param delay delay in milliseconds before the task is executed.
	 */
	public void executeOnTimer(TimerTask task,long delay) {
		timer.schedule(task,delay);
	}
	
	/**
	 * Cancels all scheduled timer tasks and resets the timer.
	 */
	public void cancelTimerTasks() {
		timer.cancel();
		timer = new Timer();
	}
}
