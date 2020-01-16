package nsu.shserg.proxy.handlers;

import nsu.shserg.proxy.util.Attachment;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class SocksErrorHandler extends Handler {
    public SocksErrorHandler(Attachment attachment) {
        super(attachment);
    }

    @Override
    public void handle(SelectionKey selectionKey) throws IOException {}

    @Override
    public int write(SelectionKey selectionKey) throws IOException {
        int remaining = super.write(selectionKey);
        if(remaining == 0){
            var socket = (SocketChannel) selectionKey.channel();
            socket.close();
        }
        return remaining;
    }
}
