package com.surftools.BeanstalkClientImpl;

import java.util.ArrayList;
import com.surftools.BeanstalkClientImpl.ClientImpl;
import com.surftools.BeanstalkClient.BeanstalkException;

/**
 * Contributed by cscotta, scott@phoreo.com, to identify and resolve put performance issue
 * 
 * @author cscotta
 *
 */
public class TimedPutTest {

    static String HOST = "127.0.0.1";
    static int PORT = 11300;
    static String TUBE = "taskQueue";
    static ClientImpl beanstalkClient;
    static ArrayList<Long> enqueueTimes = new ArrayList<Long>();

    public static void main(String[] args) {
        beanstalkClient = new ClientImpl(HOST, PORT);
        beanstalkClient.useTube(TUBE);

        for (int i = 0; i < 100; i++)
            enqueueTask("ddddddddddddddddddddddddddd");

        int averageTime = 0;
        for (Long time : enqueueTimes)
            averageTime += time;

        averageTime = averageTime / enqueueTimes.size();
        System.out.println("Average enqueue time: " + averageTime + "ms");
    }

    public static void enqueueTask(String task) {
        try {

            // Instrument the time taken to enqueue a message.
            Long start = System.currentTimeMillis();
            beanstalkClient.put(0, 0, 120, task.getBytes());
            long delta = System.currentTimeMillis() - start;

            enqueueTimes.add(delta);
            System.out.println("Enqueue time: " + delta + "ms");

        } catch (BeanstalkException e) {
            System.out.println("Error encoding task in queue.");
        }
    }
}
