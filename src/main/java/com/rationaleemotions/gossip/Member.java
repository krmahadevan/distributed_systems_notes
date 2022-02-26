package com.rationaleemotions.gossip;

import java.io.Serializable;
import java.net.InetSocketAddress;
import javax.management.NotificationListener;

public class Member implements Serializable {

  private static final long serialVersionUID = 8387950590016941525L;

  /**
   * The member address in the form IP:port Similar to the toString in {@link InetSocketAddress}
   */
  private final String nickName;

  private int heartbeat;

  private final transient TimeoutTimer timeoutTimer;

  public Member(String nickName, int heartbeat, NotificationListener client, int t_cleanup) {
    this.nickName = nickName;
    this.heartbeat = heartbeat;
    this.timeoutTimer = new TimeoutTimer(t_cleanup, client, this);
  }

  public void startTimeoutTimer() {
    this.timeoutTimer.start();
  }

  public void resetTimeoutTimer() {
    this.timeoutTimer.reset();
  }

  public String getNickName() {
    return nickName;
  }

  public int getHeartbeat() {
    return heartbeat;
  }

  public void setHeartbeat(int heartbeat) {
    this.heartbeat = heartbeat;
  }

  @Override
  public String toString() {
    return "[address=" + nickName + ", heartbeat=" + heartbeat + "]";
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((nickName == null) ? 0 : nickName.hashCode());
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Member other = (Member) obj;
    if (nickName == null) {
      return other.nickName == null;
    }
    return nickName.equals(other.nickName);
  }
}
