package com.rationaleemotions.caching;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;

/**
 * References: https://programmer.help/blogs/consistency-hash-algorithm-principle-and-java-implementation.html
 */
@SuppressWarnings("unused")
@Slf4j
public class ConsistentHash {

  private static final int MAX_VIRTUAL_NODES_PER_PHYSICAL_NODE = 5;
  private final int virtualNodesPerPhysicalNode;
  private final Function<String, Integer> hashFunction;

  //Virtual node, key represents the hash value of virtual node, value represents the name of virtual node
  private final SortedMap<Integer, String> virtualNodes = new TreeMap<>();

  public ConsistentHash(List<String> servers) {
    this(servers, MAX_VIRTUAL_NODES_PER_PHYSICAL_NODE, text -> Math.abs(Objects.hashCode(text)));
  }

  public ConsistentHash(List<String> servers, int maxVirtualNodesPerPhysicalNode,
      Function<String, Integer> hashFunction) {
    this.virtualNodesPerPhysicalNode = maxVirtualNodesPerPhysicalNode;
    this.hashFunction = hashFunction;
    addServers(servers);
  }

  private List<String> virtualNodes(String server) {
    return IntStream.rangeClosed(0, virtualNodesPerPhysicalNode - 1)
        .mapToObj(i -> server + "&&VN" + i)
        .collect(Collectors.toList());
  }

  private void addServers(List<String> servers) {
    for (int i = 0; i < virtualNodesPerPhysicalNode; i++) {
      for (String server : servers) {
        String virtualNode = virtualNodes(server).get(i);
        int hash = hash(virtualNode) + i;
        log.info("Adding virtual node : {}", virtualNode);
        virtualNodes.put(hash, virtualNode);
      }
    }

  }

  private void removeVirtualNode(String server) {
    for (int i = 0; i < virtualNodesPerPhysicalNode; i++) {
      String virtualNode = server + "&&VN" + i;
      int hash = hash(virtualNode) + i;
      log.info("Deleting virtual node : {}", virtualNode);
      virtualNodes.remove(hash);
    }
  }

  public synchronized void addNewServer(String server) {
    addServers(Collections.singletonList(server));
  }

  public synchronized void removeServer(String server) {
    removeVirtualNode(server);
  }

  public int hash(String content) {
    return hashFunction.apply(content);
  }

  public String getServerFor(String key) {
    int hash = hash(key);
    SortedMap<Integer, String> subMap = virtualNodes.tailMap(hash);
    //We assume that there is no one larger than the hash value of the key and start with the
    //first node
    int i = virtualNodes.firstKey();
    if (!subMap.isEmpty()) {
      //Since we found that there are larger values than the hash value of our key, lets update
      //ourselves to point to the first key in the found sub map.
      i = subMap.firstKey();
    }
    String virtualNode = virtualNodes.get(i);
    if (virtualNode == null || virtualNode.trim().isEmpty()) {
      throw new IllegalStateException("Problem");
    }
    return virtualNode.substring(0, virtualNode.indexOf("&&VN")) + " (" + virtualNode + ")";
  }
}
