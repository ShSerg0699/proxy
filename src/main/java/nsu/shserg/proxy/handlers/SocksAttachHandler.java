package nsu.shserg.proxy.handlers;

import nsu.shserg.proxy.util.Attachment;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public class SocksAttachHandler extends SocksHandler{
    private static final byte NO_AUTHENTICATION = 0x00;
    private static final byte SOCKS_VERSION = 0x05;
    private static final byte NO_COMPARABLE_METHOD = (byte) 0xFF;
    private byte version;
    private byte nMethods;
    private byte[] methods;

    public SocksAttachHandler(Attachment attachment) {
        super(attachment);
    }

    @Override
    public void handle(SelectionKey selectionKey) throws IOException {
        Attachment attachment = getAttachment();
        ByteBuffer outputBuffer = attachment.getOutput();
        boolean isConnectNull = false;
        read(selectionKey);

        try {
            outputBuffer.flip();
            version = outputBuffer.get();
            nMethods = outputBuffer.get();
            outputBuffer.get(methods);
        } catch (BufferUnderflowException exc){
            int newStartPos = outputBuffer.limit();
            outputBuffer.clear();
            outputBuffer.position(newStartPos);
            isConnectNull = true;
        }

        if(isConnectNull) {
            return;
        }

        ByteBuffer inputBuffer = attachment.getInput();
        if(!(version == SOCKS_VERSION && checkMethods(methods))){
            inputBuffer.put(new byte[]{ SOCKS_VERSION, NO_COMPARABLE_METHOD});
        }else {
            inputBuffer.put(new byte[]{ SOCKS_VERSION, NO_AUTHENTICATION});
        }

        selectionKey.interestOpsOr(SelectionKey.OP_WRITE);
        selectionKey.attach(new SocksRequestHandler(attachment));
        attachment.getOutput().clear();
    }

    private static boolean checkMethods(byte[] methods){
        for(byte method : methods){
            if(method == NO_AUTHENTICATION)
                return true;
        }
        return false;
    }
}
