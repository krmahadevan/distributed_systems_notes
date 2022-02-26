package com.rationaleemotions.gossip;

import java.net.DatagramSocket;
import java.util.List;

public interface Client {
  int t_cleanup = 10000;
  DatagramSocket getServer();

  List<Member> getMemberList();

  List<Member> getDeadList();

  void sendMembershipList();

  String getNickName();

  void start() throws InterruptedException;
}
