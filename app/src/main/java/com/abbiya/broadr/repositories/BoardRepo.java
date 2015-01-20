package com.abbiya.broadr.repositories;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.abbiya.broadr.dao.Board;
import com.abbiya.broadr.dao.BoardDao;
import com.abbiya.broadr.dao.DaoMaster;
import com.abbiya.broadr.dao.DaoSession;
import com.abbiya.broadr.utility.Constants;

/**
 * Created by seshachalam on 22/11/14.
 */
public class BoardRepo {

    private static Context context = null;
    private static DaoMaster.DevOpenHelper helper = null;
    private static SQLiteDatabase db = null;
    private static DaoMaster daoMaster = null;
    private static DaoSession daoSession = null;
    private static BoardDao boardDao = null;

    public BoardRepo(Context context) {
        if (BoardRepo.context == null) {
            BoardRepo.context = context;
        }
        if (BoardRepo.helper == null) {
            BoardRepo.helper = new DaoMaster.DevOpenHelper(context, Constants.SQLITE_DB_NAME, null);
        }
        if (BoardRepo.db == null) {
            BoardRepo.db = BoardRepo.helper.getWritableDatabase();
        }
        if (BoardRepo.daoMaster == null) {
            BoardRepo.daoMaster = new DaoMaster(BoardRepo.db);
        }
        if (BoardRepo.daoSession == null) {
            BoardRepo.daoSession = BoardRepo.daoMaster.newSession();
        }
        if (BoardRepo.boardDao == null) {
            BoardRepo.boardDao = BoardRepo.daoSession.getBoardDao();
        }
    }

    public BoardDao getBoardDao() {
        return BoardRepo.boardDao;
    }

    public Board getBoard(Long id) {
        return BoardRepo.boardDao.load(id);
    }

    public Board getBoard(String name) {
        return BoardRepo.boardDao.queryBuilder().where(BoardDao.Properties.Name.eq(name)).build().unique();
    }

    public Long insertOrReplace(Board board) {
        BoardRepo.boardDao.insertOrReplace(board);

        return board.getId();
    }

    public void deleteBoard(Board board) {
        BoardRepo.boardDao.delete(board);
    }

    public void close() {
        BoardRepo.daoSession.clear();
        BoardRepo.db.close();
        BoardRepo.helper.close();
    }

}
