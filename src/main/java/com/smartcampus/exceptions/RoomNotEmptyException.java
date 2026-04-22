package com.smartcampus.exceptions;

public class RoomNotEmptyException extends RuntimeException {
    public RoomNotEmptyException() {
        super("Room still has active sensors assigned.");
    }
}
