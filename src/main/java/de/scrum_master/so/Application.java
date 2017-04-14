package de.scrum_master.so;

import org.perf4j.aop.Profiled;

public class Application {
  public static final int WAIT_MILLIS = 250;

  public static void main(String[] args) throws InterruptedException {
    doSomething();
  }

  @Profiled
  private static void doSomething() throws InterruptedException {
    System.out.print("Waiting for " + WAIT_MILLIS + " ms ... ");
    Thread.sleep(WAIT_MILLIS);
    System.out.println("done");
  }
}
