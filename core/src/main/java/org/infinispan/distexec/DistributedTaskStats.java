package org.infinispan.distexec;

public interface DistributedTaskStats {

   public static String STATE_INITIALIZED = "initialized";
   public static String STATE_RUNNING = "running";
   public static String STATE_COMPLETED = "completed";

   double progress();

   int subtasksRunning();

   int subtasksCompleted();

   int failedSubtasks();

   int failedOverSubtasks();

   long startTime();

   long elapsedTime();

   long endTime();

   String state();

   String taskId();

}
