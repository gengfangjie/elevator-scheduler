package com.fjgeng.elevatorscheduler;

import com.fjgeng.elevatorscheduler.enums.Direction;

/**
 * Created by gengfangjie on 2019/10/25.
 * 乘梯指令
 */
public class RideRequest {
    private Integer floor;
    private Direction direction;

    public RideRequest(int floor, Direction direction) {
        this.floor = floor;
        this.direction = direction;
    }

    public Integer getFloor() {
        return floor;
    }

    public Direction getDirection() {
        return direction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RideRequest request = (RideRequest) o;

        if (!floor.equals(request.floor)) return false;
        return direction == request.direction;
    }

    @Override
    public int hashCode() {
        int result = floor.hashCode();
        result = 31 * result + direction.hashCode();
        return result;
    }
}
