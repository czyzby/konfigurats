package mj.konfigurats.logic;

/**
 * A class used to schedule various events in the game. When the class'
 * timer reaches 0, the (Runnable) event is executed. ScheduledEvent can
 * be set to run with each update for over-time effects.
 * @author MJ
 */
public class ScheduledEvent {
	private final boolean triggerWithEachUpdate;
	private final Runnable event;
	private float duration;
	
	/**
	 * Schedules a new event. Event should be updated with delta time
	 * to be executed.
	 * @param event scheduled event.
	 * @param triggerWithEachUpdate true to execute event with each update.
	 * @param duration time before the event is finished (in seconds).
	 */
	public ScheduledEvent(Runnable event, boolean triggerWithEachUpdate,
		float duration) {
		this.event = event;
		this.triggerWithEachUpdate = triggerWithEachUpdate;
		this.duration = duration;
	}
	
	/**
	 * Updates the timer on the scheduled event. Will run its event when
	 * the timer reaches one (or with each update, if set to do so).
	 * @param delta time with which the event is updated.
	 * @return true when the timer reached 0.
	 */
	public boolean update(float delta) {
		duration -= delta;
		
		// If the event is finished:
		if(duration <= 0f) {
			event.run();
			return true;
		}
		
		// If the event occurs with each update:
		if(triggerWithEachUpdate) {
			event.run();
		}
		return false;
	}

}
