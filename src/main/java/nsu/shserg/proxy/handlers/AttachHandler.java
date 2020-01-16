package nsu.shserg.proxy.handlers;

import nsu.shserg.proxy.util.Attachment;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class AttachHandler extends Handler{
    private static final int ANY_PORT = 0;

    public AttachHandler(Attachment attachment) {
        super(attachment);
    }

    @Override
    public void handle(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        Handler handler = (Handler) selectionKey.attachment();
        Attachment attachment = handler.getAttachment();

        socketChannel.finishConnect();

        selectionKey.attach(new ForwardHandler(attachment));
        selectionKey.interestOpsAnd(~SelectionKey.OP_CONNECT);
        selectionKey.interestOpsOr(SelectionKey.OP_READ);
    }

    public static SocketChannel initTargetSocket(Attachment clientAttachment, SelectionKey selectionKey,
                                                 InetSocketAddress targetAddress) throws IOException {
        SocketChannel targetSocket = SocketChannel.open();
        targetSocket.bind(new InetSocketAddress(ANY_PORT));
        targetSocket.configureBlocking(false);
        Attachment targetAttachment = new Attachment(clientAttachment.getObservableInputBuffer(),
                                                     clientAttachment.getObservableOutputBuffer());
        targetSocket.connect(targetAddress);
        AttachHandler attachHandler = new AttachHandler(targetAttachment);

        clientAttachment.setSocketChannel(targetSocket);
        targetAttachment.setSocketChannel((SocketChannel) selectionKey.channel());

        SelectionKey key = targetSocket.register(selectionKey.selector(), SelectionKey.OP_CONNECT, attachHandler);
        targetAttachment.setBufferCheck(() -> key.interestOpsOr(SelectionKey.OP_WRITE));

        return targetSocket;
    }

    public static void attachToTarget(SelectionKey clientKey, InetSocketAddress targetAddress) throws IOException {
        Handler handler = (Handler) clientKey.attachment();
        Attachment clientAttachment = handler.getAttachment();
        SocketChannel targetSocketChannel = initTargetSocket(clientAttachment, clientKey, targetAddress);

        putResponseIntoBuf(clientAttachment, targetSocketChannel);
        clientKey.interestOpsOr(SelectionKey.OP_WRITE);
        clientKey.attach(new ForwardHandler(clientAttachment));
        clientAttachment.getOutput().clear();
    }

    private static void putResponseIntoBuf(Attachment attachment, SocketChannel socketChannel) throws IOException {
        InetSocketAddress socketAddress = (InetSocketAddress) socketChannel.getLocalAddress();
        ByteBuffer inputBuff = attachment.getInput();
        int responseLength = 10;
        ByteBuffer byteBuffer = ByteBuffer.allocate(responseLength);
        byteBuffer.put((byte)0x05)
                .put((byte)0x00)
                .put((byte)0x00)
                .put((byte)0x01)
                .put(InetAddress.getLocalHost().getAddress())
                .putShort((short) socketAddress.getPort());

        byteBuffer.flip();
        inputBuff.put(byteBuffer);
    }
}
