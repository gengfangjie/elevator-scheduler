package com.fjgeng.elevatorscheduler;

import java.util.List;
import java.util.TreeSet;

/**
 * Created by gengfangjie on 2019/10/25.
 * 工具类
 */
public class Utils {

    public static int getNearest(List<Integer> floorList, int currentFloor) {
        int result = currentFloor;
        int minDistance = Integer.MAX_VALUE;
        for (Integer floor : floorList) {
            if (floor == 0) continue;
            int distance = Math.abs(floor - currentFloor);
            if (distance < minDistance) {
                minDistance = distance;
                result = floor;
            }
        }
        return minDistance == Integer.MAX_VALUE ? 0 : result;
    }

    public static int getNearestHigher(TreeSet<Integer> floorSet, int currentFloor) {
        Integer higher = floorSet.higher(currentFloor);
        return higher == null ? 0 : higher;
    }

    public static int getNearestLower(TreeSet<Integer> floorSet, int currentFloor) {
        Integer lower = floorSet.lower(currentFloor);
        return lower == null ? 0 : lower;
    }

    public static int lower(int floor1, int floor2) {
        if (floor1 < floor2 && floor1 != 0) {
            return floor1;
        } else {
            return floor2;
        }
    }

}
