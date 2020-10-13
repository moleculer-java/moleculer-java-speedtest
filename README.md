## (WIP) Moleculer for Java speedtest

Measure the speed of EventBus for different implementations (Moleculer vs Guava/Akka/Vert.x/Spring).  

**Internal EventBus tests:**
```
Moleculer        (direct method call):   10 000 000 requests made within 942 milliseconds.  
Spring EventBus  (direct method call):   10 000 000 requests made within 1 156 milliseconds.  
Google Guava     (direct method call):   10 000 000 requests made within 1 864 milliseconds.  
Vert.x EventBus  (call via thread pool): 10 000 000 requests made within 8 100 milliseconds.  
Akka ActorSystem (call via thread pool): 10 000 000 requests made within 5 573 milliseconds.  
```
**Organized results:**

| Framework        | Type  | Events/sec |
| ---------------- | ----- | ---------- |
| Moleculer        | Sync  | 10 615 711 |
| Spring EventBus  | Sync  | 8 650 519  |
| Google Guava     | Sync  | 5 364 807  |
| Akka ActorSystem | Async | 1 794 366  |
| Vert.x EventBus  | Async | 1 234 568  |

*The higher value is better - the fastest are "Moleculer" then "Spring EventBus".  
The best result is 10 615 711 messages per second.*

Warm up cycles: 1 000  
Test cycles:    10 000 000

[[source of the test]](https://github.com/moleculer-java/moleculer-java-speedtest/blob/master/src/test/java/services/moleculer/speedtest/SpeedTest.java)

## Moleculer Documentation

[![Documentation](https://raw.githubusercontent.com/moleculer-java/site/master/docs/docs-button.png)](https://moleculer-java.github.io/site/introduction.html)