package com.rationaleemotions.gossip;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.management.Notification;
import javax.management.NotificationListener;

public class ClientImpl implements Client, NotificationListener {

  private final List<Member> memberList = new ArrayList<>();

  private final List<Member> deadList = new ArrayList<>();

  public final int t_cleanup = 10000; //in ms

  private final Random random = new Random();

  private DatagramSocket server;

//  private String myAddress;

  private final String nickName;

  private Member me;

  public ClientImpl(String nickName) throws SocketException {
    this(nickName, parseStartupMembers());
  }

  /**
   * Setup the client's lists, gossiping parameters, and parse the startup config file.
   */
  public ClientImpl(String nickName, List<String> entries) throws SocketException {
    Runtime.getRuntime().addShutdownHook(new Thread(
        () -> System.out.println("Goodbye my friends...")));

    int port = 0;
    this.nickName = nickName;

    // loop over the initial hosts, and find ourselves
    for (String entry : entries) {
      Member member = new Member(entry, 0, this, t_cleanup);

      if (entry.contains(this.nickName)) {
        // save our own Member class, so we can increment our heartbeat later
        me = member;
        port = Integer.parseInt(entry.split(":")[1]);
        System.out.println("I am " + me);
      }
      memberList.add(member);
    }

    String text = "I am " + me + ". My Members are : " +
        memberList.stream()
            .map(Member::toString)
            .collect(Collectors.joining("\t"));

    System.out.println(text);

    if (port != 0) {
      // TODO: starting the server could probably be moved to the constructor
      // of the receiver thread.
      server = new DatagramSocket(port);
    } else {
      // This is bad, so no need proceeding on
      System.err.println("Could not find myself in startup list");
      System.exit(-1);
    }
  }

  @Override
  public String getNickName() {
    return nickName;
  }

  public DatagramSocket getServer() {
    return server;
  }

  public List<Member> getMemberList() {
    return memberList;
  }

  public List<Member> getDeadList() {
    return deadList;
  }

  /**
   * In order to have some membership lists at startup, we read the IP addresses and port at a
   * newline delimited config file.
   *
   * @return List of <IP address:port> Strings
   */
  private static List<String> parseStartupMembers() {
    File startupConfig = new File("config", "startup_members");
    try {
      return Files.readAllLines(startupConfig.toPath());
    } catch (IOException e) {
      e.printStackTrace();
      return Collections.emptyList();
    }
  }

  /**
   * Performs the sending of the membership list, after we have incremented our own heartbeat.
   */
  public void sendMembershipList() {

    this.me.setHeartbeat(me.getHeartbeat() + 1);

    synchronized (this.memberList) {
      try {
        Member member = getRandomMember();
        if (member == null) {
          return;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(this.memberList);
        byte[] buf = baos.toByteArray();

        String address = member.getNickName();
        int port = Integer.parseInt(address.split(":")[1]);

        InetAddress dest = InetAddress.getLocalHost();

        String text = me.getNickName() + " gossipping with " +
            memberList.stream()
                .map(Member::toString)
                .collect(Collectors.joining("\t"));

        System.out.println(text);

        //simulate some packet loss ~25%
        int percentToSend = random.nextInt(100);
        if (percentToSend > 25) {
          DatagramSocket socket = new DatagramSocket();
          DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length, dest, port);
          socket.send(datagramPacket);
          socket.close();
        }

      } catch (IOException e1) {
        e1.printStackTrace();
      }
    }
  }

  /**
   * Find a random peer from the local membership list. Ensure that we do not select ourselves, and
   * keep trying 10 times if we do.  Therefore, in the case where this client is the only member in
   * the list, this method will return null
   *
   * @return Member random member if list is greater than 1, null otherwise
   */
  private Member getRandomMember() {
    if (memberList.isEmpty() || memberList.size() == 1) {
      System.out.println(me.getNickName() + " has no members to gossip");
      return null;
    }

    int randomNeighborIndex = random.nextInt(this.memberList.size() - 1);
    List<Member> list = new ArrayList<>(memberList);
    list.removeIf(m -> m.getNickName().equalsIgnoreCase(this.nickName));
    return list.get(randomNeighborIndex);
  }

  /**
   * Starts the client.  Specifically, start the various cycles for this protocol. Start the gossip
   * thread and start the receiver thread.
   */
  public void start() throws InterruptedException {

    // Start all timers except for me
    for (Member member : memberList) {
      if (!member.equals(me)) {
        member.startTimeoutTimer();
      }
    }

    // Start the two worker threads
    ExecutorService executor = Executors.newCachedThreadPool();
    List<Future<?>> results = Stream.of(
            //  The receiver thread is a passive player that handles
            //  merging incoming membership lists from other neighbors.
            new AsynchronousReceiver(this),
            //  The gossiper thread is an active player that
            //  selects a neighbor to share its membership list
            new MembershipGossiper(this)
        ).map(executor::submit)
        .collect(Collectors.toList());

    executor.shutdown();
    results
        .forEach(
            each -> {
              try {
                each.get();
              } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
              }
            }
        );

    // Potentially, you could kick off more threads here
    //  that could perform additional data synching

    // keep the main thread around
    while (!executor.isShutdown()) {
      TimeUnit.SECONDS.sleep(10);
    }
  }

  /**
   * All timers associated with a member will trigger this method when it goes off.  The timer will
   * go off if we have not heard from this member in
   * <code> t_cleanup </code> time.
   */
  @Override
  public void handleNotification(Notification notification, Object handback) {

    Member deadMember = (Member) notification.getUserData();

    System.out.println("Dead member detected: " + deadMember);

    synchronized (this.memberList) {
      this.memberList.remove(deadMember);
    }

    synchronized (this.deadList) {
      this.deadList.add(deadMember);
    }

  }
}
