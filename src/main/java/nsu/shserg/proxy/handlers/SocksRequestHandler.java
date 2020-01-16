package nsu.shserg.proxy.handlers;

import nsu.shserg.proxy.util.Attachment;
import nsu.shserg.proxy.DnsService;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;

public class SocksRequestHandler extends SocksHandler {
    private static final int NO_ERROR = 0;
    private static final byte WRONG_ADDRESS_TYPE = 0x08;
    private static final byte WRONG_COMMAND = 0x07;
    private static final byte SOCKS_VERSION = 0x05;
    private static final byte NO_AUTHENTICATION = 0x00;
    private static final byte IPv4 = 0x01;
    private static final byte DOMAIN_NAME = 0x03;
    private static final byte CONNECT_COMMAND = 0x01;
    private byte version;
    private byte command;
    private byte addressType;
    private byte[] ip4Address = new byte[4];
    private String domainName;
    private short targetPort;
    private byte parseError = 0x00;

    public SocksRequestHandler(Attachment connection) {
        super(connection);
    }

    @Override
    public void handle(SelectionKey selectionKey) throws IOException {
        ByteBuffer outputBuffer = getAttachment().getOutput();
        boolean isRequestNull = false;
        read(selectionKey);
        try {
            outputBuffer.flip();
            version =outputBuffer.get();
            command = outputBuffer.get();
            if (command != CONNECT_COMMAND) {
                parseError = (WRONG_COMMAND);
            }
            outputBuffer.get();
            addressType = outputBuffer.get();
            switch (addressType) {
                case IPv4:
                    outputBuffer.get(ip4Address);
                    break;
                case DOMAIN_NAME:
                    byte nameLength = outputBuffer.get();
                    byte[] nameBytes = new byte[nameLength];
                    outputBuffer.get(nameBytes);
                    domainName = new String(nameBytes, StandardCharsets.UTF_8);
                    break;
                default:
                    parseError = WRONG_ADDRESS_TYPE;
            }
            targetPort = outputBuffer.getShort();
        } catch (BufferUnderflowException exc){
            int newStartPos = outputBuffer.limit();
            outputBuffer.clear();
            outputBuffer.position(newStartPos);
            isRequestNull = true;
        }

        if (isRequestNull) {
            return;
        }

        if(parseError != NO_ERROR){
            onError(selectionKey, parseError);
            return;
        }
        if(addressType == DOMAIN_NAME){
            DnsService dnsService = DnsService.getInstance();
            dnsService.convertName(domainName, targetPort, selectionKey);
            return;
        }
        AttachHandler.attachToTarget(selectionKey, new InetSocketAddress(InetAddress.getByAddress(ip4Address), targetPort));
    }


    public static void onError(SelectionKey selectionKey, byte error) {
        Handler handler = (Handler) selectionKey.attachment();
        Attachment attachment = handler.getAttachment();

        putErrorResponseIntoBuf(selectionKey, attachment, error);
        selectionKey.attach(new SocksErrorHandler(attachment));
    }

    public static void putErrorResponseIntoBuf(SelectionKey selectionKey, Attachment attachment,  byte error) {
        int responseLength = 10;
        ByteBuffer inputBuff = attachment.getInput();
        ByteBuffer byteBuffer = ByteBuffer.allocate(responseLength);
        byteBuffer.put(SOCKS_VERSION)
                .put(error)
                .put(NO_AUTHENTICATION);

        byteBuffer.flip();
        inputBuff.put(byteBuffer);
        attachment.getOutput().clear();
        selectionKey.interestOpsOr(SelectionKey.OP_WRITE);
    }
}
