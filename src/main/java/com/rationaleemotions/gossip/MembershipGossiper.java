package com.rationaleemotions.gossip;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The class handles gossiping the membership list. This information is important to maintaining a
 * common state among all the nodes, and is important for detecting failures.
 */
class MembershipGossiper implements Runnable {

  private final Client client;
  private final AtomicBoolean keepRunning = new AtomicBoolean(true);

  public MembershipGossiper(Client client) {
    this.client = client;
  }

  @Override
  public void run() {
    //in ms
    int t_gossip = 5000;
    while (this.keepRunning.get()) {
      try {
        TimeUnit.MILLISECONDS.sleep(t_gossip);
        client.sendMembershipList();
      } catch (InterruptedException e) {
        // TODO: handle exception
        // This membership thread was interrupted externally, shutdown
        keepRunning.set(false);
      }
    }
  }

}
