# [WIP] moleculer-java-speedtest
Measure the speed of EventBus for different implementations (Moleculer vs Guava/Akka/Vert.x/Spring).

First test results (duration of one million calls - lower value is better):

| Implementation | Type  | Result |
| ---- | ----- | ------ |
| Moleculer | Sync | 130 msec |
| Spring EventBus | Sync | 171 msec |
| Guava EventBus | Sync | 225 msec |
| Vert.x EventBus | Async | 325 msec |
| Akka EventBus | Async | 328 msec |
| Moleculer | Async | 390 msec |
