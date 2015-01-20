package com.abbiya.broadr.events;

import com.abbiya.broadr.dao.Board;

/**
 * Created by seshachalam on 22/11/14.
 */
public class StoredBoardEvent {
    private Board board;

    public StoredBoardEvent(Board board) {
        this.board = board;
    }

    public Board getBoard() {
        return board;
    }

    public void setBoard(Board board) {
        this.board = board;
    }

}
