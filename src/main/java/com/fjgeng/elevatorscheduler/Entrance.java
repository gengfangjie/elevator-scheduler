package com.fjgeng.elevatorscheduler;

import com.fjgeng.elevatorscheduler.enums.Direction;
import com.fjgeng.elevatorscheduler.enums.ElevatorMark;
import com.fjgeng.elevatorscheduler.listener.ElevatorStateListener;
import com.fjgeng.elevatorscheduler.listener.EntranceButtonListener;

/**
 * Created by gengfangjie on 2019/10/25.
 * 电梯入口类
 */
public class Entrance implements ElevatorStateListener {
    private final String mark;
    private final Integer floor;

    private EntranceButtonListener entranceButtonListener;
    // 上行按钮灯
    private Boolean upButton;
    // 下行按钮灯
    private Boolean downButton;

    public Entrance(String mark, Integer floor) {
        this.mark = mark;
        this.floor = floor;
        this.upButton = false;
        this.downButton = false;
    }

    public void buttonHit(Direction direction) {
        if (this.floor == Building.TOP_FLOOR && direction.equals(Direction.Up)) {
            System.out.println("顶层无向上按钮");
        } else if (this.floor == Building.GROUND_FLOOR && direction.equals(Direction.Down)) {
            System.out.println("一层无向下按钮");
        } else if (direction.equals(Direction.Down) && !this.downButton) {
            this.downButton = true;
            this.entranceButtonListener.entranceButtonHit(this.floor, direction);
        } else if (direction.equals(Direction.Up) && !upButton) {
            this.upButton = true;
            this.entranceButtonListener.entranceButtonHit(this.floor, direction);
        } else {
            System.out.println("重复按钮动作");
        }
    }

    public void registerListener(EntranceButtonListener listener) {
        this.entranceButtonListener = listener;
    }

    @Override
    public void stateChanged(Elevator elevator) {
        if (elevator.getElevatorState().getFloor() == this.floor
                && elevator.getElevatorState().getWorkingState().equals(ElevatorState.WorkingState.Waiting_in)) {
            if (elevator.getElevatorState().getDirection().equals(Direction.Up)) {
                this.upButton = false;
            } else if (elevator.getElevatorState().getDirection().equals(Direction.Down)) {
                this.downButton = false;
            }
        }
    }

    public Integer getFloor() {
        return floor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Entrance entrance = (Entrance) o;

        if (!mark.equals(entrance.mark)) return false;
        return floor.equals(entrance.floor);
    }

    @Override
    public int hashCode() {
        int result = mark.hashCode();
        result = 31 * result + floor.hashCode();
        return result;
    }
}
