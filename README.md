# [WIP] moleculer-java-speedtest
Measure the speed of EventBus for different implementations (Moleculer vs Guava/Akka/Vert.x/Spring).  

**Internal EventBus tests:**
```
Moleculer Sync   (direct method call):   10 000 000 requests made within 1 287 milliseconds.  
Moleculer Async  (call via thread pool): 10 000 000 requests made within 3 257 milliseconds.  
Spring EventBus  (direct method call):   10 000 000 requests made within 1 408 milliseconds.  
Google Guava     (direct method call):   10 000 000 requests made within 2 123 milliseconds.  
Vert.x EventBus  (call via thread pool): 10 000 000 requests made within 6 705 milliseconds.  
Akka ActorSystem (call via thread pool): 10 000 000 requests made within 2 260 milliseconds.  
```
**Organized results:**

| Framework        | Type  | Events/sec |
| ---------------- | ----- | ---------- |
| Moleculer Sync   | Sync  | 7 770 008  |
| Spring EventBus  | Sync  | 7 102 273  |
| Google Guava     | Sync  | 4 710 316  |
| Akka ActorSystem | Async | 4 424 779  |
| Moleculer Async  | Async | 3 070 310  |
| Vert.x EventBus  | Async | 1 491 424  |

*The higher value is better - the fastest are "Moleculer Sync" then "Spring EventBus".  
The best result is 7 770 008 messages per second.*

Warm up cycles: 1 000  
Test cycles:    10 000 000

[[source of the test]](https://github.com/moleculer-java/moleculer-java-speedtest/blob/master/src/test/java/services/moleculer/speedtest/SpeedTest.java)
