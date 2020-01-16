package nsu.shserg.proxy.handlers;

import nsu.shserg.proxy.util.Attachment;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class AcceptHandler extends Handler {
    private ServerSocketChannel serverSocketChannel;

    public AcceptHandler(ServerSocketChannel serverSocketChannel) {
        super(null);
        this.serverSocketChannel = serverSocketChannel;
    }

    @Override
    public void handle(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);

        Attachment attachment = new Attachment(getBuffLength());
        SocksAttachHandler attachHandler = new SocksAttachHandler(attachment);

        SelectionKey key = socketChannel.register(selectionKey.selector(), SelectionKey.OP_READ, attachHandler);
        attachment.setBufferCheck(() -> key.interestOpsOr(SelectionKey.OP_WRITE));

        System.out.println("New attachment: " + socketChannel.getRemoteAddress());
    }
}
