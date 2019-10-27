package com.fjgeng.elevatorscheduler;

import com.fjgeng.elevatorscheduler.enums.Direction;

/**
 * Created by gengfangjie on 2019/10/25.
 * 电梯厢状态
 */
public class ElevatorState {
    // 当前楼层
    private volatile int floor;
    private WorkingState workingState;
    private Direction direction;
    private Boolean stall;

    public ElevatorState() {
        this.floor = 10;
        this.workingState = WorkingState.Idle;
        this.direction = null;
        this.stall = true;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public WorkingState getWorkingState() {
        return workingState;
    }

    public void setWorkingState(WorkingState workingState) {
        this.workingState = workingState;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public Boolean getStall() {
        return stall;
    }

    public void setStall(Boolean stall) {
        this.stall = stall;
    }

    enum WorkingState {
        // 上行
        Up,
        // 下行
        Down,
        // 空闲
        Idle,
        // 乘客出
        Waiting_out,
        // 乘客入
        Waiting_in
    }

    @Override
    public String toString() {
        return "[" + floor + "]" + "[" + workingState + "]" + "[" + direction + "]" + "[" + stall + "]";
    }
}
