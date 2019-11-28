# [WIP] moleculer-java-speedtest
Measure the speed of EventBus for different implementations (Moleculer vs Guava/Akka/Vert.x/Spring).  

**Internal EventBus tests:**
```
Moleculer Sync   (direct method call):   10 000 000 requests made within 998 milliseconds.  
Moleculer Async  (call via thread pool): 10 000 000 requests made within 3 296 milliseconds.  
Spring EventBus  (direct method call):   10 000 000 requests made within 1 125 milliseconds.  
Google Guava     (direct method call):   10 000 000 requests made within 1 903 milliseconds.  
Vert.x EventBus  (call via thread pool): 10 000 000 requests made within 10 004 milliseconds.  
Akka ActorSystem (call via thread pool): 10 000 000 requests made within 2 391 milliseconds.  
```
**Organized results:**

| Framework        | Type  | Events/sec |
| ---------------- | ----- | ---------- |
| Moleculer Sync   | Sync  | 10 020 040 |
| Spring EventBus  | Sync  | 8 888 889  |
| Google Guava     | Sync  | 5 254 861  |
| Akka ActorSystem | Async | 4 182 350  |
| Moleculer Async  | Async | 3 033 981  |
| Vert.x EventBus  | Async | 999 600    |

*The higher value is better - the fastest are "Moleculer Sync" then "Spring EventBus".  
The best result is 10 020 040 messages per second.*

Warm up cycles: 1 000  
Test cycles:    10 000 000

[[source of the test]](https://github.com/moleculer-java/moleculer-java-speedtest/blob/master/src/test/java/services/moleculer/speedtest/SpeedTest.java)
