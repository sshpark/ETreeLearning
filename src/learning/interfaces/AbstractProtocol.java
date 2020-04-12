package learning.interfaces;

import learning.DataBaseReader;
import learning.InstanceHolder;
import learning.controls.ChurnControl;
import learning.messages.ActiveThreadMessage;
import learning.messages.ModelMessage;
import learning.messages.OnlineSessionFollowerActiveThreadMessage;
import learning.topology.TopoUtil;
import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;
import peersim.transport.Transport;

import java.io.File;

/**
 * This abstract base class (ABC) is situated between the Peersim protocol interface
 * and our concrete learning protocol implementations (in the inheritance tree).
 * Basically it implements and hides the irrelevant details
 * from the viewpoint of learning protocols.
 * So in the concrete protocols we have to take care of only the learning dependent
 * code pieces.<br/>
 * Make sure you initialize well the delayMean and delayVar fields which defines the
 * length of active thread delay. These fields are used here but not initialized!<br/>
 * This implementation also adds some useful methods like getTransport, getOverlay and
 * getCurrentProtocol.
 *
 * @author Róbert Ormándi
 *
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractProtocol implements EDProtocol, Churnable, LearningProtocol {
  // compute eval accuracy
  private static final String PAR_TFILE = "trainingFile";
  private static final String PAR_EFILE = "evaluationFile";
  private static final String PAR_ECFILE = "evalForClusteringFile";
  private static final String PAR_PRETRAINROUNDS = "pretrainRounds";
  protected File tFile;
  protected File eFile;
  protected File ecFile;
  protected String readerClassName;
  protected DataBaseReader reader;
  protected InstanceHolder eval;
  protected InstanceHolder evalForClusteringSet;
  protected int pretrainRounds;
  protected long cycle;


  //active thread delay mean and variance
  /** @hidden */
  protected static final String PAR_DELAYMEAN = "delayMean";
  protected double delayMean = Double.POSITIVE_INFINITY;
  /** @hidden */
  protected static final String PAR_DELAYVAR = "delayVar";
  protected double delayVar = 1.0;
  
  // instance variable
  /** @hidden */
  protected InstanceHolder instances;
  
  // variables for modeling churn
  protected long sessionLength = ChurnControl.INIT_SESSION_LENGTH;
  protected int sessionID = 0;
  
  // state variables
  /** @hidden */
  protected Node currentNode;
  /** @hidden */
  protected int currentProtocolID = -1;
  /** @hidden */
  protected String prefix;

  protected int[][] minDelayMatrix;

  protected double r;
  
  /**
   * This method performers the deep copying of the protocol.
   */
  @Override
  public abstract Object clone();

  protected void init(String prefix) {
    this.prefix = prefix;
    delayMean = Configuration.getDouble(prefix + "." + PAR_DELAYMEAN, Double.POSITIVE_INFINITY);
    delayVar = Configuration.getDouble(prefix + "." + PAR_DELAYVAR, 1.0);
    r = Configuration.getDouble("REGULARIZATION");
    tFile = new File(Configuration.getString(prefix + "." + PAR_TFILE));
    eFile = new File(Configuration.getString(prefix + "." + PAR_EFILE));
    ecFile = new File(Configuration.getString(prefix + "." + PAR_ECFILE));
    pretrainRounds = Configuration.getInt(prefix + "." + PAR_PRETRAINROUNDS);
    readerClassName = "learning.DataBaseReader";
    try {
//      reader = DataBaseReader.createDataBaseReader(readerClassName, tFile, eFile);
      reader = DataBaseReader.createDataBaseReader(readerClassName, tFile, eFile, ecFile);
      eval = reader.getEvalSet();
      evalForClusteringSet = reader.getEvalForClusteringSet();
      cycle = 0;
    } catch (Exception ex) {
      throw new RuntimeException("Exception has occurred in InstanceLoader!", ex);
    }
  }

  public void setMinDelayMatrix(int[][] minDelayMatrix) {
    this.minDelayMatrix = minDelayMatrix;
  }

  /**
   * It is a helper method as well which supports sending message
   * to a uniform random neighbor.
   * 
   * @param message The message which will be sent. The source of the
   * message will be set before sending it.
   */
  protected void sendToRandomNeighbor(ModelMessage message) {
    message.setSource(currentNode);
    Linkable overlay = getOverlay();
    Node randomNode = overlay.getNeighbor(CommonState.r.nextInt(overlay.degree()));
    getTransport().send(currentNode, randomNode, message, currentProtocolID);
  }
  
  /**
   * It is method which makes more easer of the accessing to the transport layer of the current node.
   * 
   * @return The transform layer is returned.
   */
  protected Transport getTransport() {
    return ((Transport) currentNode.getProtocol(FastConfig.getTransport(currentProtocolID)));
  }
  
  /**
   * This method supports the accessing of the overlay of the current node.
   * 
   * @return The overlay of the current node is returned.
   */
  protected Linkable getOverlay() {
    return (Linkable) currentNode.getProtocol(FastConfig.getLinkable(currentProtocolID));
  }
  
  /**
   * This is a helper method which returns the current protocol instance.
   * Here we assume that the subclass implements the interface {@link learning.interfaces.LearningProtocol}
   * for which this helper ABC implementation was designed.<br/>
   * Implementing the interface {@link learning.interfaces.LearningProtocol} is
   * strongly suggested!
   * 
   * @return This protocol instance is returned.
   */
  protected LearningProtocol getCurrentProtocol() {
    return (LearningProtocol) currentNode.getProtocol(currentProtocolID);
  }
  
  /**
   * This is the most basic implementation of processEvent which 
   * can recognize two types of messages:
   * <ul>
   *   <li>In the case when the protocol receives message from the
   *   first type it indicates that an <i>activeThread()</i> method call
   *   has to be performed. Messages of the first type are the instances of
   *   {@link learning.messages.ActiveThreadMessage} or
   *   {@link learning.messages.OnlineSessionFollowerActiveThreadMessage}.</li>
   *   <li>In the other hand when a {@link learning.messages.ModelMessage}
   *   is received the protocol perform a <i>passiveThread(modelMessage)</i> call.</li>
   * </ul>
   * Notice that the two abstract methods here are the same as those specified in the
   * interface {@link learning.interfaces.LearningProtocol}.
   * 
   * @param currentNode Reference to the current node.
   * @param currentProtocolID ID of the current protocol.
   * @param messageObj The message as an Object.
   */
  @Override
  public void processEvent(Node currentNode, int currentProtocolID, Object messageObj) {
    // the current node and protocol fields are updated
    this.currentNode = currentNode;
    this.currentProtocolID = currentProtocolID;
    
    if ( messageObj instanceof ActiveThreadMessage || 
          (messageObj instanceof OnlineSessionFollowerActiveThreadMessage && 
          ((OnlineSessionFollowerActiveThreadMessage)messageObj).sessionID == sessionID) ) {
      
      // The received message is a valid active thread alarm => performing active thread call
      activeThread();
      
      // After the processing we set a new alarm with a delay
      if (!Double.isInfinite(delayMean)) {
        int delay = (int)(delayMean + CommonState.r.nextGaussian()*delayVar);
        delay = (delay > 0) ? delay : 1;
        // Next time of the active thread
        EDSimulator.add(delay, new OnlineSessionFollowerActiveThreadMessage(sessionID), currentNode, currentProtocolID);
      }
    } else if (messageObj instanceof ModelMessage) {
      // The received message is a model message => calling the passive thread handler
      passiveThread((ModelMessage)messageObj);
    }
  }
  
  public int getPID() {
    if (currentProtocolID < 0) {
      throw new RuntimeException("Too early request for PID!");
    }
    return currentProtocolID;
  }
  
  //----- Instance related methods -----
  
  /**
   * It returns the instances as an InstanceHolder stored by the node.
   * 
   * @return The instances stored by the node.
   */
  public InstanceHolder getInstanceHolder() {
    return instances;
  }
  
  /**
   * It sets a new set of instances for the node.
   * 
   * @param instances The new set of instances as an InstanceHolder.
   */
  public void setInstenceHolder(InstanceHolder instances) {
    this.instances = instances;
  }
  
  //----- Churnable related methods -----
  
  /**
   * Basic churn implementation which simply stores the remaining session length
   * in a field.
   * 
   * @return remaining session length
   */
  @Override
  public long getSessionLength() {
    return sessionLength;
  }
  
  /**
   * It sets a new session length.
   * 
   * @param sessionLength new session length which overwrites the original value
   */
  @Override
  public void setSessionLength(long sessionLength) {
    this.sessionLength = sessionLength;
  }

  /**
   * Session initialization simply awakes the protocol by adding an active thread event to itself
   * with delay 0.
   */
  @Override
  public void initSession(Node node, int protocol) {
    sessionID ++;
    EDSimulator.add(0, new OnlineSessionFollowerActiveThreadMessage(sessionID), node, protocol);
  }

  protected double crossEntropyLoss(double y, double[] y_pred) {
    // clipping
    double eps = 1e-8;
    int label = (int)y;
    y_pred[label] = Math.max(y_pred[label], eps);
    y_pred[label] = Math.min(y_pred[label], 1.0-eps);
    return -Math.log(y_pred[label]);
  }

}
