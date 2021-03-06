package learning.controls.observers.messageCounter;

import learning.messages.Message;

import java.util.Map;
import java.util.TreeMap;

import peersim.config.Configuration;
import peersim.core.Node;
import peersim.transport.Transport;

/**
 * This class represents a transport layer that can count the 
 * transit predefined type of messages.
 * @author István Hegedűs
 *
 */
public class MessageCounterTransport  implements Transport {
  private static final String PAR_TRANSPORT = "transport";
  private final int transport;
  private static final String PAR_MSGCLASS = "msgClass";
  /** @hidden */
  private final Class<? extends Message> msgClass;
  /** @hidden */
  private Map<Long,Integer> messageCounter = new TreeMap<Long,Integer>();
  private int sum = 0;

  @SuppressWarnings("unchecked")
  public MessageCounterTransport(String prefix) throws ClassNotFoundException {
    transport = Configuration.getPid(prefix + "." + PAR_TRANSPORT);
    String msgClassName = Configuration.getString(prefix + "." + PAR_MSGCLASS, "");
    if (!msgClassName.equals("")) {
      msgClass = (Class<? extends Message>) Class.forName(msgClassName);
    } else {
      msgClass = null;
    }
  }

  public Object clone() {
    return this;
  }
  
  /**
   * Resets the counter.
   */
  public void resetCounters() {
    sum = 0;
    messageCounter.clear();
  }
  
  /**
   * Returns the number of incoming messages.
   * @return number of all of the received messages
   */
  public int numberOfAllIncommingMessages() {
    return sum;
  }
  
  /**
   * Returns the mapping of nodeID and the number of received messages.
   * @return nodeID, received messages mapping
   */
  public Map<Long,Integer> numberOfIncommingMessages() {
    return messageCounter;
  }
  
  /**
   * Returns the type of the observed messages.
   * @return type of observed messages
   */
  public Class<? extends Message> getFilterClass() {
    return msgClass;
  }
  

  public void send(Node src, Node dest, Object msg, int pid) {
    // increment counters
    if (msgClass == null || msgClass.isInstance(msg)) {
      Integer c = messageCounter.get(dest.getID());
      if (c == null) {
        messageCounter.put(dest.getID(), 1);
      } else {
        messageCounter.put(dest.getID(), c + 1);
      }
      sum ++;
    }
    
    Transport t = (Transport) src.getProtocol(transport);
    t.send(src, dest, msg, pid);
  }

  public long getLatency(Node src, Node dest) {
    Transport t = (Transport) src.getProtocol(transport);
    return t.getLatency(src, dest);
  }
}
