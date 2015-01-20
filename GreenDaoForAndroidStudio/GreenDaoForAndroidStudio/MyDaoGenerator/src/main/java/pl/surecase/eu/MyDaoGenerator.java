package pl.surecase.eu;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Property;
import de.greenrobot.daogenerator.Schema;
import de.greenrobot.daogenerator.ToMany;

public class MyDaoGenerator {

    public static void main(String args[]) throws Exception {

        Schema schema = new Schema(1, "broadr");

        Entity board = schema.addEntity("Board");
        board.addIdProperty();
        board.addStringProperty("name").unique();

        Entity message = schema.addEntity("Message");
        message.addIdProperty();
        message.addStringProperty("uuid").unique();
        message.addStringProperty("content"); //message text
        message.addDateProperty("happenedAt"); //createdAt
        message.addStringProperty("geoHash"); //Geo Hash
        message.addStringProperty("address"); //address
        message.addIntProperty("type"); // type of message
        message.addIntProperty("status"); //sending, sent, delivered, received
        message.addDateProperty("updatedAt"); //anytime after creating

        Property boardId = message.addLongProperty("boardId").notNull().getProperty();
        message.addToOne(board, boardId);
        ToMany boardToMessages = board.addToMany(message, boardId);
        boardToMessages.setName("messages");

        Entity comment = schema.addEntity("Comment");
        comment.addIdProperty();
        comment.addStringProperty("uuid").unique();
        comment.addStringProperty("content"); //comment text
        comment.addDateProperty("happenedAt"); //createdAt
        comment.addStringProperty("geoHash"); //Geo Hash
        comment.addStringProperty("address"); //address
        comment.addIntProperty("status"); //sending, sent, delivered, received
        comment.addDateProperty("updatedAt"); //anytime after creating

        Property messageId = comment.addLongProperty("messageId").notNull().getProperty();
        comment.addToOne(message, messageId);
        ToMany messageToComments = message.addToMany(comment, messageId);
        messageToComments.setName("comments");

        Entity location = schema.addEntity("Location");
        location.addIdProperty();
        location.addStringProperty("uuid");
        location.addStringProperty("latitude");
        location.addStringProperty("longitude");

        Entity registrationMessage = schema.addEntity("RegistrationMessage");
        registrationMessage.addIdProperty();
        registrationMessage.addStringProperty("uuid");
        registrationMessage.addStringProperty("email");

        new DaoGenerator().generateAll(schema, args[0]);
    }
}
