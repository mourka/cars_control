Getting acquainted with monitors in Java. Solving a cars control problem that requires mutual exclusion when cars try to use the same object (for example, the alley) and synchronization when they arrive at the barrier.

Step4 - Alley and Barrier Synchronization with Java Monitors

Step4D - User can set up the threshold that defines how many cars are waiting at the barrier, before the cars are released. Number remains stable until the threshold is
changed again. If new threshold is smaller than the previous one, then the cars waiting currently in the barrier are released and the new threshold takes immediately place. If the new threshold is bigger than the previous one, then the cars waiting at the barrier still count up to the old threshold until they are released, and then the new threshold takes place. In the meanwhile, changes in the threshold have not any effect to it. If the barrier is set to inactive, then the cars pass through that area without stopping. The threshold value is not affected by turning on and off the barrier.

Step 4E - Optimizing the alley synchronization such that cars no. 1 and 2 can enter the alley without any delay.