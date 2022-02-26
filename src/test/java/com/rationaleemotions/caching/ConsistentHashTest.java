package com.rationaleemotions.caching;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;

@Slf4j
public class ConsistentHashTest {

  private final List<String> keys = Arrays.asList(
      "apple", "bat", "cat", "dog", "mumbai", "name", "pig", "queen", "tiger", "umbrella"
  );

  @Test
  public void demoConsistentHashing() {
    List<String> servers = Arrays.asList("Solaris-Host", "OSX-Host", "Linux-Host", "Windows-Host");
    ConsistentHash consistentHash = new ConsistentHash(servers, 2,
        hashFunction());
    log.info("Initially with {} servers", servers.size());
    Collection<String> logs = generateHashes(consistentHash);
    String[] expected = new String[]{
        "[apple] with hash value [1], Routed to Node[Linux-Host (Linux-Host&&VN0)]",
        "[bat] with hash value [2], Routed to Node[Linux-Host (Linux-Host&&VN0)]",
        "[cat] with hash value [3], Routed to Node[Linux-Host (Linux-Host&&VN0)]",
        "[dog] with hash value [4], Routed to Node[Linux-Host (Linux-Host&&VN0)]",
        "[mumbai] with hash value [13], Routed to Node[Linux-Host (Linux-Host&&VN1)]",
        "[name] with hash value [14], Routed to Node[OSX-Host (OSX-Host&&VN0)]",
        "[pig] with hash value [16], Routed to Node[OSX-Host (OSX-Host&&VN1)]",
        "[queen] with hash value [17], Routed to Node[Solaris-Host (Solaris-Host&&VN0)]",
        "[tiger] with hash value [20], Routed to Node[Solaris-Host (Solaris-Host&&VN1)]",
        "[umbrella] with hash value [21], Routed to Node[Windows-Host (Windows-Host&&VN0)]"
    };
    assertThat(logs).containsExactly(expected);
    String removedServer = "Linux-Host";
    consistentHash.removeServer(removedServer);
    log.info("After removing server {}", removedServer);
    expected = new String[]{
        "[apple] with hash value [1], Routed to Node[OSX-Host (OSX-Host&&VN0)]",
        "[bat] with hash value [2], Routed to Node[OSX-Host (OSX-Host&&VN0)]",
        "[cat] with hash value [3], Routed to Node[OSX-Host (OSX-Host&&VN0)]",
        "[dog] with hash value [4], Routed to Node[OSX-Host (OSX-Host&&VN0)]",
        "[mumbai] with hash value [13], Routed to Node[OSX-Host (OSX-Host&&VN0)]",
        "[name] with hash value [14], Routed to Node[OSX-Host (OSX-Host&&VN0)]",
        "[pig] with hash value [16], Routed to Node[OSX-Host (OSX-Host&&VN1)]",
        "[queen] with hash value [17], Routed to Node[Solaris-Host (Solaris-Host&&VN0)]",
        "[tiger] with hash value [20], Routed to Node[Solaris-Host (Solaris-Host&&VN1)]",
        "[umbrella] with hash value [21], Routed to Node[Windows-Host (Windows-Host&&VN0)]"
    };
    logs = generateHashes(consistentHash);
    assertThat(logs).containsExactly(expected);
  }

  private Function<String, Integer> hashFunction() {
    return text -> {
      String character = (text.charAt(0) + "").toLowerCase();
      switch (character) {
        case "a":
          return 1;
        case "b":
          return 2;
        case "c":
          return 3;
        case "d":
          return 4;
        case "e":
          return 5;
        case "f":
          return 6;
        case "g":
          return 7;
        case "h":
          return 8;
        case "i":
          return 9;
        case "j":
          return 10;
        case "k":
          return 11;
        case "l":
          return 12;
        case "m":
          return 13;
        case "n":
          return 14;
        case "o":
          return 15;
        case "p":
          return 16;
        case "q":
          return 17;
        case "r":
          return 18;
        case "s":
          return 19;
        case "t":
          return 20;
        case "u":
          return 21;
        case "v":
          return 22;
        case "w":
          return 23;
        case "x":
          return 24;
        case "y":
          return 25;
        default:
          return 26;
      }
    };
  }

  private Collection<String> generateHashes(ConsistentHash consistentHash) {
    return keys.stream()
        .map(key -> {
          String serverKey = consistentHash.getServerFor(key);
          return ("[" + key + "] with hash value [" +
              consistentHash.hash(key) + "], Routed to Node[" + serverKey + "]");

        }).collect(Collectors.toList());
  }

}
