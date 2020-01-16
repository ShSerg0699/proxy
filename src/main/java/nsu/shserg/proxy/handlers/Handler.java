package nsu.shserg.proxy.handlers;

import nsu.shserg.proxy.util.Attachment;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public abstract class Handler {
    private static final int BUFF_LENGTH = 65536;
    private static final int NO_REMAINING = 0;
    private Attachment attachment;

    public Handler(Attachment attachment) {
        this.attachment = attachment;
    }

    public Attachment getAttachment() {
        return attachment;
    }

    abstract public void handle(SelectionKey selectionKey) throws IOException;

    public int read(SelectionKey selectionKey) throws IOException {
        Handler handler = (Handler) selectionKey.attachment();
        SocketChannel socket = (SocketChannel) selectionKey.channel();
        Attachment attachment = handler.getAttachment();
        ByteBuffer outputBuffer = attachment.getOutput();

        if(!isReadyToRead(outputBuffer, attachment)) {
            return 0;
        }

        int readCount = socket.read(outputBuffer);

        if(readCount <= 0) {
            attachment.close();
            selectionKey.interestOps(0);
            checkConnectionClose(socket);
        }

        return readCount;
    }

    public int write(SelectionKey selectionKey) throws IOException {
        ByteBuffer inputBuffer = attachment.getInput();
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        attachment.prepareToWrite();
        socketChannel.write(inputBuffer);

        int remaining = inputBuffer.remaining();

        if(remaining == NO_REMAINING){
            selectionKey.interestOps(SelectionKey.OP_READ);
            checkAssociate(socketChannel, inputBuffer);
        } else
            attachment.setPosition();

        return remaining;
    }

    public static int getBuffLength() {
        return BUFF_LENGTH;
    }

    private boolean isReadyToRead(ByteBuffer buffer, Attachment attachment){
        return buffer.position() < BUFF_LENGTH / 2 || attachment.isSocketChanelShutDown();
    }

    private void checkConnectionClose(SocketChannel socketChannel) throws IOException {
        if(attachment.isReadyToClose()){
            System.out.println("Socket closed: " + socketChannel.getRemoteAddress());
            socketChannel.close();
            attachment.closeSocketChannel();
        }
    }

    private void checkAssociate(SocketChannel socketChannel, ByteBuffer buffer) throws IOException {
        if(attachment.isSocketChanelShutDown()){
            socketChannel.shutdownOutput();
            return;
        }
        buffer.clear();
        attachment.resetPosition();
    }
}
