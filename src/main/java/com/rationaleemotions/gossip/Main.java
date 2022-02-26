package com.rationaleemotions.gossip;

public class Main {

  //https://github.com/ympons/gossip-protocol-java
  public static void main(String[] args) throws Exception {
    ClientImpl client = new ClientImpl("");
    client.start();
  }

}
