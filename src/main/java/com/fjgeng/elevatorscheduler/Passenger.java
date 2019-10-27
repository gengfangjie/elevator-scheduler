package com.fjgeng.elevatorscheduler;

import com.fjgeng.elevatorscheduler.enums.Direction;
import com.fjgeng.elevatorscheduler.enums.ElevatorMark;
import com.fjgeng.elevatorscheduler.listener.ElevatorStateListener;
import org.apache.commons.lang3.RandomUtils;

/**
 * Created by gengfangjie on 2019/10/25.
 * 乘客类
 * 乘客遵从非常简单的逻辑
 * 1. 想去哪个楼层直接去入口按钮
 * 2. 有同向电梯停在入口，并且电梯未满员就进去，有多个电梯时按顺序选择最近电梯，进去后发生超载主动退出
 * 3. 进入电梯后到达目的楼层就出去
 */
public class Passenger implements ElevatorStateListener {
    private final Integer id;

    // 乘客在电梯入口可获得入口操作对象
    private Entrance entrance;
    private Direction direction;
    private Integer destination;
    // 乘客进入电梯内可获得电梯操作对象
    private Elevator elevator;

    public Passenger(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void register() {
        Building.INSTANCE.registerPassenger(this);
    }

    // 乘客操作: 想去哪里
    public void takeRide(int location, int destination) {
        if (!Building.INSTANCE.passengerRegistered(this)) {
            System.out.println(String.format("用户: %s 未注册", id));
            return;
        }

        System.out.println(String.format("用户: %s 想从%s层乘电梯到%s", id, location, destination));
        if (location > Building.TOP_FLOOR || location < Building.GROUND_FLOOR
                || destination > Building.TOP_FLOOR || destination < Building.GROUND_FLOOR) {
            System.out.println("不可能的当前楼层或目的楼层，无法乘梯");
            return;
        }
        if (location == destination) {
            System.out.println("目的楼层与当前楼层相同，无法乘梯");
            return;
        }

        this.destination = destination;
        this.direction = destination > location ? Direction.Up : Direction.Down;
        hitEntranceButton(nearestEntrance(location), direction);
    }

    private Entrance nearestEntrance(int floor) {
        return Building.INSTANCE.getEntrance(floor, ElevatorMark.getByIndex(RandomUtils.nextInt(0, 4)));
    }

    // 乘客操作: 乘梯按钮
    private void hitEntranceButton(Entrance entrance, Direction direction) {
        this.entrance = entrance;
        for (ElevatorMark mark : ElevatorMark.values()) {
            Building.INSTANCE.getElevator(mark).registerListener(this);
        }
        entrance.buttonHit(direction);
    }

    /**
     * 乘客操作: 进入电梯厢，忽略进电梯耗时
     * 失去入口对象，获取当前电梯对象，注销对其余三部电梯的监听
      */
    private boolean enter(Elevator elevator) {
        if (elevator.passengerIn(this)) {
            this.entrance = null;
            this.elevator = elevator;
            for (ElevatorMark mark : ElevatorMark.values()) {
                if (!this.elevator.getMark().equals(mark.getMark())) {
                    Building.INSTANCE.getElevator(mark).removeListener(this);
                }
            }
            this.elevator.addDestination(destination);
            System.out.println(String.format("乘客: %s 已乘电梯: %s 方向: %s",
                    id, elevator.getMark(), elevator.getElevatorState().getDirection()));
            return true;
        }
        return false;
    }

    private boolean exit() {
        if (elevator.passengerOut(this)) {
            System.out.println(String.format("乘客: %s 出电梯: %s 到达楼层: %s",
                    id, elevator.getMark(), elevator.getElevatorState().getFloor()));
            this.elevator.removeListener(this);
            this.elevator = null;

            return true;
        }
        return false;
    }

    /**
     * 简单设定乘客逻辑: 电梯停靠并且方向一致则进入 电梯停靠并且到达目的地则走出
     * 因为有可能多个电梯同时到达，所以需要加锁保证线程安全
     */
    @Override
    public synchronized void stateChanged(Elevator changedElevator) {
        // 尝试乘梯
        if (this.elevator == null
                && changedElevator.getElevatorState().getFloor() == entrance.getFloor()
                && direction == changedElevator.getElevatorState().getDirection()
                && changedElevator.getElevatorState().getWorkingState() == ElevatorState.WorkingState.Waiting_in
                && !changedElevator.isFullLoad()
                && !changedElevator.isOverload()) {
            enter(changedElevator);
        }

        // 下梯
        if (this.elevator != null
                && this.destination == this.elevator.getElevatorState().getFloor()
                && this.elevator.getElevatorState().getWorkingState() == ElevatorState.WorkingState.Waiting_out) {
            exit();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Passenger passenger = (Passenger) o;

        return id.equals(passenger.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
