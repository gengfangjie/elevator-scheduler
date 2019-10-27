package com.fjgeng.elevatorscheduler;

import com.fjgeng.elevatorscheduler.enums.ElevatorMark;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by gengfangjie on 2019/10/25.
 * 大楼类
 */
public class Building {
    public static Building INSTANCE = new Building();
    // 顶层楼层
    public static final int TOP_FLOOR = 20;
    // 底层楼层
    public static final int GROUND_FLOOR = 1;
    // 大楼最大容量
    public static final int CAPACITY = 10000;

    private final Scheduler scheduler;
    private final List<Elevator> elevatorList;
    private final Set<Passenger> passengerSet;

    private Building() {
        this.scheduler = new Scheduler();
        this.elevatorList = new ArrayList<>();
        this.passengerSet = new HashSet<>();
        init();
    }

    private void init() {
        List<EntranceButton> entranceButtonList = new ArrayList<>(TOP_FLOOR);
        for (int i = GROUND_FLOOR; i <= TOP_FLOOR; i++) {
            entranceButtonList.add(new EntranceButton(false, false));
        }
        for (ElevatorMark mark : ElevatorMark.values()) {
            List<Entrance> entranceList = new ArrayList<>();
            for (int i = GROUND_FLOOR; i <= TOP_FLOOR; i++) {
                entranceList.add(new Entrance(mark.getMark(), i, entranceButtonList.get(i-1)));
            }
            elevatorList.add(new Elevator(mark.getMark(), entranceList));
        }

        elevatorList.forEach(elevator -> {
            elevator.getEntranceList().forEach(entrance ->  {
                entrance.registerListener(scheduler);
                elevator.registerListener(entrance);
            });
            elevator.registerListener(scheduler);
            elevator.init();
        });

        scheduler.init();
    }

    public Entrance getEntrance(int floor, ElevatorMark mark) {
        if (floor > TOP_FLOOR || floor < 1) {
            return null;
        }
        return elevatorList.get(mark.getIndex()).getEntranceList().get(floor-1);
    }

    public Elevator getElevator(ElevatorMark mark) {
        return elevatorList.get(mark.getIndex());
    }

    public boolean registerPassenger(Passenger passenger) {
        if (passengerSet.size() >= CAPACITY) {
            System.out.println("超过大楼最大容量!");
            return false;
        } else {
            if (passengerSet.add(passenger)) {
                return true;
            } else {
                System.out.println("已存在相同id的乘客");
                return false;
            }
        }
    }

    public boolean passengerRegistered(Passenger passenger) {
        return passengerSet.contains(passenger);
    }
}
