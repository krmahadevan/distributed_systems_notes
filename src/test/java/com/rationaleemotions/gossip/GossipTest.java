package com.rationaleemotions.gossip;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.testng.annotations.Test;

public class GossipTest {

  private final List<String> membersInfo = Arrays.asList(
      "ironman:2222",
      "hulk:2223",
      "superman:2224",
      "batman:2225"
  );

  @Test
  public void testMethod() {
    List<Callable<Void>> tasks = membersInfo.stream()
        .map(each -> (Callable<Void>) () -> {
          new ClientImpl(each, membersInfo).start();
          return null;
        })
        .collect(Collectors.toList());
    ExecutorService service = Executors.newFixedThreadPool(tasks.size());
    List<Future<Void>> futures = tasks.stream()
        .map(service::submit)
        .collect(Collectors.toList());
    service.shutdown();
    futures.parallelStream()
        .forEach(each -> {
          try {
            each.get();
          } catch (InterruptedException e) {
            e.printStackTrace();
          } catch (ExecutionException e) {
            e.printStackTrace();
          }
        });
  }

}
