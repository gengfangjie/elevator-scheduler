package com.fjgeng.elevatorscheduler;

import org.apache.commons.lang3.RandomUtils;

import java.util.concurrent.TimeUnit;

/**
 * Created by gengfangjie on 2019/10/25.
 * 程序入口类
 */
public class Main {

    public static void main(String[] args) {

        try {
            for (int i = 0; i < 20; i++) {
                Passenger passenger = new Passenger(i);
                passenger.register();
                passenger.takeRide(RandomUtils.nextInt(Building.GROUND_FLOOR, Building.TOP_FLOOR + 1),
                        RandomUtils.nextInt(Building.GROUND_FLOOR, Building.TOP_FLOOR + 1));
                TimeUnit.SECONDS.sleep(1);
            }
            TimeUnit.SECONDS.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
