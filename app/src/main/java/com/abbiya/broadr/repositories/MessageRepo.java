package com.abbiya.broadr.repositories;

/**
 * Created by seshachalam on 29/8/14.
 */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.abbiya.broadr.dao.Board;
import com.abbiya.broadr.dao.DaoMaster;
import com.abbiya.broadr.dao.DaoMaster.DevOpenHelper;
import com.abbiya.broadr.dao.DaoSession;
import com.abbiya.broadr.dao.Message;
import com.abbiya.broadr.dao.MessageDao;
import com.abbiya.broadr.utility.Constants;

import java.util.List;

import de.greenrobot.dao.query.LazyList;

public class MessageRepo {

    private static Context context = null;
    private static DevOpenHelper helper = null;
    private static SQLiteDatabase db = null;
    private static DaoMaster daoMaster = null;
    private static DaoSession daoSession = null;
    private static MessageDao messageDao = null;

    public MessageRepo(Context context) {
        if (MessageRepo.context == null) {
            MessageRepo.context = context;
        }
        if (MessageRepo.helper == null) {
            MessageRepo.helper = new DevOpenHelper(context, Constants.SQLITE_DB_NAME, null);
        }
        if (MessageRepo.db == null) {
            MessageRepo.db = MessageRepo.helper.getWritableDatabase();
        }
        if (MessageRepo.daoMaster == null) {
            MessageRepo.daoMaster = new DaoMaster(MessageRepo.db);
        }
        if (MessageRepo.daoSession == null) {
            MessageRepo.daoSession = MessageRepo.daoMaster.newSession();
        }
        if (MessageRepo.messageDao == null) {
            MessageRepo.messageDao = MessageRepo.daoSession.getMessageDao();
        }
    }

    public MessageDao getMessageDao() {
        return MessageRepo.messageDao;
    }

    public void updateAllMessages(Long id, Integer status) {
        List<Message> messages = MessageRepo.messageDao.queryBuilder().where(MessageDao.Properties.Id.le(id)).list();
        for (Message message : messages) {
            int msgStatus = message.getStatus();
            if (msgStatus != Constants.SENT && msgStatus != Constants.DELIVERED && msgStatus != Constants.RECEIVED) {
                message.setStatus(status);
            }
        }
        MessageRepo.messageDao.updateInTx(messages);
    }

    public Message getMessage(Long id) {
        return MessageRepo.messageDao.load(id);
    }

    public Message getMessage(String UUID) {
        return MessageRepo.messageDao.queryBuilder().where(MessageDao.Properties.Uuid.eq(UUID)).build().unique();
    }

    public List<Message> getMessages(List<String> uuids) {
        if (uuids.size() == 1) {
            return MessageRepo.messageDao.queryBuilder().where(MessageDao.Properties.Uuid.eq(uuids.get(0))).list();
        } else {
            return MessageRepo.messageDao.queryBuilder().where(MessageDao.Properties.Uuid.in(uuids)).list();
        }
    }

    public List<Message> getMessages() {
        return MessageRepo.messageDao.loadAll();
    }

    public List<Message> getMessagesOfStatus(Integer... statuses) {
        if (statuses.length == 1) {
            return MessageRepo.messageDao.queryBuilder().where(MessageDao.Properties.Status.eq(statuses[0])).orderDesc(MessageDao.Properties.HappenedAt).list();
        } else {
            return MessageRepo.messageDao.queryBuilder().where(MessageDao.Properties.Status.in((Object[]) statuses)).orderDesc(MessageDao.Properties.HappenedAt).list();
        }
    }

    public Message getLastMessage(Board board) {
        return MessageRepo.messageDao.queryBuilder().where(MessageDao.Properties.BoardId.eq(board.getId())).where(MessageDao.Properties.Status.eq(Constants.RECEIVED))
                .orderDesc(MessageDao.Properties.HappenedAt)
                .limit(1).unique();
    }

    public Message getLastMessageOfType(Board board, boolean idOrHa) {
        return MessageRepo.messageDao.queryBuilder().where(MessageDao.Properties.BoardId.eq(board.getId()))
                .orderDesc(idOrHa == true ? MessageDao.Properties.Id : MessageDao.Properties.HappenedAt)
                .limit(1).unique();
    }

    public Message getFirstMessage(Board board) {
        return MessageRepo.messageDao.queryBuilder().where(MessageDao.Properties.BoardId.eq(board.getId())).where(MessageDao.Properties.Status.eq(Constants.RECEIVED))
                .orderAsc(MessageDao.Properties.HappenedAt)
                .limit(1).unique();
    }

    public LazyList<Message> findMessages(Board board, String constraint) {
        return MessageRepo.messageDao.queryBuilder().where(MessageDao.Properties.BoardId.eq(board.getId())).whereOr(MessageDao.Properties.Content.like("%" + constraint + "%"), MessageDao.Properties.Address.like("%" + constraint + "%")).orderDesc(MessageDao.Properties.HappenedAt).listLazy();
    }

    public LazyList<Message> lazyLoadMessages(Board board) {
        return MessageRepo.messageDao.queryBuilder().where(MessageDao.Properties.BoardId.eq(board.getId())).orderDesc(MessageDao.Properties.HappenedAt).listLazy();
    }

    public Long insertMessage(Message message) {
        MessageRepo.messageDao.insert(message);

        return message.getId();
    }

    public Long insertOrReplace(Message message) {
        MessageRepo.messageDao.insertOrReplace(message);

        return message.getId();
    }

    public void insertMessages(List<Message> messages) {
        MessageRepo.messageDao.insertOrReplaceInTx(messages);
    }

    public Message updateMessage(Message message) {
        MessageRepo.messageDao.update(message);

        return message;
    }

    public void updateMessages(List<Message> messages) {
        MessageRepo.messageDao.updateInTx(messages);
    }

    public void deleteMessage(Message message) {
        MessageRepo.messageDao.delete(message);
    }

    public void deleteMessages(List<Message> messages) {
        MessageRepo.messageDao.deleteInTx(messages);
    }

    public void deleteAllMessages() {
        MessageRepo.messageDao.deleteAll();
    }

    public long isEmpty() {
        return MessageRepo.messageDao.count();
    }

    public void close() {
        MessageRepo.daoSession.clear();
        MessageRepo.db.close();
        MessageRepo.helper.close();
    }

}
