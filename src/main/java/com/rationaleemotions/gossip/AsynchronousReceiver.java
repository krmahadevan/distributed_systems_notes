package com.rationaleemotions.gossip;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.management.NotificationListener;

/**
 * This class handles the passive cycle, where this client has received an incoming message.  For
 * now, this message is always the membership list, but if you choose to gossip additional
 * information, you will need some logic to determine the incoming message.
 */
public class AsynchronousReceiver implements Runnable {

  private final Client client;
  private final AtomicBoolean keepRunning = new AtomicBoolean(true);

  public AsynchronousReceiver(Client client) {
    this.client = client;
    if (!(client instanceof NotificationListener)) {
      throw new IllegalArgumentException("Not my type");
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void run() {
    while (keepRunning.get()) {
      try {
        //XXX: be mindful of this array size for later
        byte[] buf = new byte[256];
        DatagramPacket p = new DatagramPacket(buf, buf.length);
        client.getServer().receive(p);

        // extract the member arraylist out of the packet
        // TODO: maybe abstract this out to pass just the bytes needed
        ByteArrayInputStream bais = new ByteArrayInputStream(p.getData());
        ObjectInputStream ois = new ObjectInputStream(bais);

        Object readObject = ois.readObject();
        if (readObject instanceof ArrayList<?>) {
          ArrayList<Member> list = (ArrayList<Member>) readObject;

          String text = client.getNickName() + " received the following members : " +
              list.stream()
                  .map(Objects::toString)
                  .collect(Collectors.joining("\t"));

          System.out.println(text);
          // Merge our list with the one we just received
          mergeLists(list);
        } else {
          keepRunning.set(false);
        }
      } catch (IOException e) {
        e.printStackTrace();
        keepRunning.set(false);
      } catch (ClassNotFoundException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  /**
   * Merge remote list (received from peer), and our local member list. Simply, we must update the
   * heartbeats that the remote list has with our list.  Also, some additional logic is needed to
   * make sure we have not timed out a member and then immediately received a list with that
   * member.
   */
  private void mergeLists(ArrayList<Member> remoteList) {

    synchronized (client.getDeadList()) {
      synchronized (client.getMemberList()) {
        for (Member remoteMember : remoteList) {
          if (client.getMemberList().contains(remoteMember)) {
            Member localMember = client.getMemberList().get(
                client.getMemberList().indexOf(remoteMember));

            if (remoteMember.getHeartbeat() > localMember.getHeartbeat()) {
              // update local list with latest heartbeat
              localMember.setHeartbeat(remoteMember.getHeartbeat());
              // and reset the timeout of that member
              localMember.resetTimeoutTimer();
            }
            continue;
          }
          // the local list does not contain the remote member

          // the remote member is either brand new, or a previously declared dead member
          // if its dead, check the heartbeat because it may have come back from the dead

          if (client.getDeadList().contains(remoteMember)) {
            Member localDeadMember = client.getDeadList().get(
                client.getDeadList().indexOf(remoteMember));
            if (remoteMember.getHeartbeat() > localDeadMember.getHeartbeat()) {
              // it's baa-aack
              client.getDeadList().remove(localDeadMember);
              Member newLocalMember = new Member(remoteMember.getNickName(),
                  remoteMember.getHeartbeat(), (NotificationListener) client, client.t_cleanup);
              client.getMemberList().add(newLocalMember);
              newLocalMember.startTimeoutTimer();
            } // else ignore
          } else {
            // brand spanking new member - welcome
            Member newLocalMember = new Member(remoteMember.getNickName(),
                remoteMember.getHeartbeat(), (NotificationListener) client, client.t_cleanup);
            client.getMemberList().add(newLocalMember);
            newLocalMember.startTimeoutTimer();
          }
        }
      }
    }
  }
}
