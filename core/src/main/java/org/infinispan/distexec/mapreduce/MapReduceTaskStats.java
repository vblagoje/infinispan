package org.infinispan.distexec.mapreduce;

public interface MapReduceTaskStats {

   public static String STATE_INITIALIZED = "initialized";
   public static String STATE_RUNNING = "running";
   public static String STATE_COMPLETED = "completed";

   double mapProgress();
   double reduceProgress();

   int mapsRunning();
   int reducesRunning();

   int mapsCompleted();
   int reducesCompleted();

   int failedMapAttempts();
   int failedReduceAttempts();

   long startTime();
   long elapsedTime();
   long endTime();

   String state();
   String taskId();

}
