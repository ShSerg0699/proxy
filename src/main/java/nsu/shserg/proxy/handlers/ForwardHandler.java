package nsu.shserg.proxy.handlers;

import nsu.shserg.proxy.util.Attachment;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public class ForwardHandler extends Handler {
    public ForwardHandler(Attachment connection) {
        super(connection);
    }

    @Override
    public void handle(SelectionKey selectionKey) throws IOException {
        Attachment attachment = ((Handler) selectionKey.attachment()).getAttachment();
        int readCount = read(selectionKey);
        if (readCount != 0){
            attachment.checkBuffer();
        }
    }

}
