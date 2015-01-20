package com.abbiya.broadr.repositories;

/**
 * Created by seshachalam on 4/9/14.
 */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.abbiya.broadr.dao.Comment;
import com.abbiya.broadr.dao.CommentDao;
import com.abbiya.broadr.dao.DaoMaster;
import com.abbiya.broadr.dao.DaoMaster.DevOpenHelper;
import com.abbiya.broadr.dao.DaoSession;
import com.abbiya.broadr.dao.Message;
import com.abbiya.broadr.utility.Constants;

import java.util.List;

import de.greenrobot.dao.query.LazyList;

public class CommentRepo {

    private static Context context = null;
    private static DevOpenHelper helper = null;
    private static SQLiteDatabase db = null;
    private static DaoMaster daoMaster = null;
    private static DaoSession daoSession = null;
    private static CommentDao commentDao = null;

    public CommentRepo(Context context) {
        if (CommentRepo.context == null) {
            CommentRepo.context = context;
        }
        if (CommentRepo.helper == null) {
            CommentRepo.helper = new DevOpenHelper(context, Constants.SQLITE_DB_NAME, null);
        }
        if (CommentRepo.db == null) {
            CommentRepo.db = CommentRepo.helper.getWritableDatabase();
        }
        if (CommentRepo.daoMaster == null) {
            CommentRepo.daoMaster = new DaoMaster(CommentRepo.db);
        }
        if (CommentRepo.daoSession == null) {
            CommentRepo.daoSession = CommentRepo.daoMaster.newSession();
        }
        if (CommentRepo.commentDao == null) {
            CommentRepo.commentDao = CommentRepo.daoSession.getCommentDao();
        }
    }

    public Comment getComment(Long id) {
        return CommentRepo.commentDao.load(id);
    }

    public Comment getComment(String UUID) {
        return CommentRepo.commentDao.queryBuilder().where(CommentDao.Properties.Uuid.eq(UUID)).build().unique();
    }

    public List<Comment> getComments(List<String> uuids) {
        if (uuids.size() == 1) {
            return CommentRepo.commentDao.queryBuilder().where(CommentDao.Properties.Uuid.eq(uuids.get(0))).list();
        } else {
            return CommentRepo.commentDao.queryBuilder().where(CommentDao.Properties.Uuid.in(uuids)).list();
        }
    }

    public Long insertComment(Comment comment) {
        CommentRepo.commentDao.insert(comment);

        return comment.getId();
    }

    public List<Comment> getComments(Message message) {
        return message.getComments();
    }

    public Comment getLastComment(Message message) {
        return CommentRepo.commentDao.queryBuilder().where(CommentDao.Properties.Id.isNotNull()).where(CommentDao.Properties.MessageId.eq(message.getId()))
                .orderDesc(CommentDao.Properties.Id)
                .limit(1).unique();
    }

    public Comment getFirstComment(Message message) {
        return CommentRepo.commentDao.queryBuilder().where(CommentDao.Properties.Id.isNotNull()).where(CommentDao.Properties.MessageId.eq(message.getId()))
                .orderAsc(CommentDao.Properties.Id)
                .limit(1).unique();
    }

    public Long insertOrReplace(Comment comment) {
        CommentRepo.commentDao.insertOrReplace(comment);

        return comment.getId();
    }

    public Long insert(Comment comment) {
        CommentRepo.commentDao.insert(comment);

        return comment.getId();
    }

    public void insertComments(List<Comment> comments) {
        CommentRepo.commentDao.insertOrReplaceInTx(comments);
    }

    public Comment updateComment(Comment comment) {
        CommentRepo.commentDao.update(comment);

        return comment;
    }

    public List<Comment> getCommentsOfStatus(Integer... statuses) {
        return CommentRepo.commentDao.queryBuilder().where(CommentDao.Properties.Status.eq(statuses[0])).list();
//        if (statuses.length == 1) {
//            return CommentRepo.commentDao.queryBuilder().where(CommentDao.Properties.Status.eq(statuses[0])).list();
//        } else {
//            return CommentRepo.commentDao.queryBuilder().where(CommentDao.Properties.Status.in(statuses)).list();
//        }
    }

    public void deleteComment(Comment comment) {
        CommentRepo.commentDao.delete(comment);
    }

    public void deleteComments(List<Comment> comments) {
        CommentRepo.commentDao.deleteInTx(comments);
    }

    public LazyList<Comment> lazyLoadComments(Message message) {
        message.resetComments();
        return CommentRepo.commentDao.queryBuilder().where(CommentDao.Properties.MessageId.eq(message.getId())).orderDesc(CommentDao.Properties.HappenedAt).listLazy();
    }

    public long isEmpty() {
        return CommentRepo.commentDao.count();
    }

    public void close() {
        CommentRepo.daoSession.clear();
        CommentRepo.db.close();
        CommentRepo.helper.close();
    }
}
