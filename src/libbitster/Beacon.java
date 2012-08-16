package libbitster;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Allows Actors to register a watch for a memo associated with a type of event
 * @author Theodore Surgent
 */
class Beacon {
  private ConcurrentHashMap<String, Set<Actor>> eventTypes;
  
  Beacon() {
    eventTypes = new ConcurrentHashMap<String, Set<Actor>>();
  }
  
  /**
   * Register to watch for an event
   * @param type The event type
   * @param actor Where the memo will be posted
   */
  public void watch(String type, Actor actor) {
    if(!eventTypes.containsKey(type)) {
      eventTypes.put(type, Collections.synchronizedSet(new HashSet<Actor>()));
    }
    
    eventTypes.get(type).add(actor);
  }
  
  /**
   * Unregister a watch for an event
   * @param type The event type
   * @param actor The Actor currently watching the event
   */
  public void ignore(String type, Actor actor) {
    if(!eventTypes.containsKey(type))
      return;
    
    Set<Actor> actors = eventTypes.get(type);
    actors.remove(actor);
    
    if(actors.size() == 0)
      eventTypes.remove(type);
  }

  /**
   * Unregister a watch for all events
   * @param actor The Actor currently watching the events
   */
  public void ignoreAll(Actor actor) {
    Set<String> keys = eventTypes.keySet();
    
    for(String key : keys) {
      eventTypes.get(key).remove(actor);
    }
  }
  
  /**
   * Signal the Beacon!
   * Post a memo to all Actors watching a type of event
   * @param type The event type
   * @param payload Data associated with the event
   * @param sender The Actor sending the event (usually `this`)
   */
  protected void signal(String type, Object payload, Actor sender) {
    Set<Actor> actors = eventTypes.get(type);
    
    if(actors != null) {
      Memo memo = new Memo(type, payload, sender);
      
      for(Actor actor : actors) {
        actor.post(memo);
      }
    }
  }
}
