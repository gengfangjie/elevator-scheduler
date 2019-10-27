package com.fjgeng.elevatorscheduler;

import com.fjgeng.elevatorscheduler.enums.Direction;
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

    private EntranceButton entranceButton;

    public Entrance(String mark, Integer floor, EntranceButton entranceButton) {
        this.mark = mark;
        this.floor = floor;
        this.entranceButton = entranceButton;
    }

    public void buttonHit(Direction direction) {
        if (this.floor == Building.TOP_FLOOR && direction.equals(Direction.Up)) {
            System.out.println("顶层无向上按钮");
        } else if (this.floor == Building.GROUND_FLOOR && direction.equals(Direction.Down)) {
            System.out.println("一层无向下按钮");
        } else if (direction.equals(Direction.Down) && !this.entranceButton.getDownButton()) {
            this.setDownButton(true);
            this.entranceButtonListener.entranceButtonHit(this.floor, direction);
        } else if (direction.equals(Direction.Up) && !this.entranceButton.getUpButton()) {
            this.setUpButton(true);
            this.entranceButtonListener.entranceButtonHit(this.floor, direction);
        } else {
            System.out.println(String.format("电梯入口: %s 重复按钮动作", floor+mark));
        }
    }

    public void setUpButton(Boolean upButton) {
        System.out.println(String.format("电梯入口: %s 按钮Up: %s", floor+mark, upButton));
        this.entranceButton.setUpButton(upButton);
    }

    public void setDownButton(Boolean downButton) {
        System.out.println(String.format("电梯入口: %s 按钮Down: %s", floor+mark, downButton));
        this.entranceButton.setDownButton(downButton);
    }

    public void registerListener(EntranceButtonListener listener) {
        this.entranceButtonListener = listener;
    }

    @Override
    public void stateChanged(Elevator elevator) {
        if (elevator.getElevatorState().getFloor() == this.floor
                && elevator.getElevatorState().getWorkingState().equals(ElevatorState.WorkingState.Waiting_in)) {
            if (Direction.Up.equals(elevator.getElevatorState().getDirection())) {
                this.setUpButton(false);
            } else if (Direction.Down.equals(elevator.getElevatorState().getDirection())) {
                this.setDownButton(false);
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
