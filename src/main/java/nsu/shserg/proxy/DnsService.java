package nsu.shserg.proxy;

import nsu.shserg.proxy.handlers.AttachHandler;
import nsu.shserg.proxy.handlers.Handler;
import nsu.shserg.proxy.handlers.SocksRequestHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Map;

import org.xbill.DNS.*;

public class DnsService {
    private static final byte PORT = 53;
    private static final byte NOT_AVAILABLE_ERROR = 0x04;
    private static final int BUF_SIZE = 1024;
    private int messageId = 0;
    private DatagramChannel socket;
    private InetSocketAddress dnsServerAddress;
    private Handler dnsHandler;
    private Map<Integer, MapValue> namesForConvert = new HashMap<>();

    private static class SingletonHelper{
        private static final DnsService dnsService = new DnsService();
    }
    public static DnsService getInstance() {
        return SingletonHelper.dnsService;
    }

    private DnsService() {
        String[] dnsServers = ResolverConfig.getCurrentConfig().servers();
        this.dnsServerAddress = new InetSocketAddress(dnsServers[0], PORT);
    }

    public void setSocket(DatagramChannel socket) {
        this.socket = socket;
        initResponseHandler();
    }

    public void registerSelector(Selector selector) throws ClosedChannelException {
        socket.register(selector, SelectionKey.OP_READ, dnsHandler);
    }

    public void convertName(String name, short targetPort, SelectionKey selectionKey) throws IOException {
        try {
            MapValue mapValue = new MapValue(selectionKey, targetPort);
            Message message = getMessage(name);
            byte[] messageBytes = message.toWire();
            namesForConvert.put(message.getHeader().getID(), mapValue);
            socket.send(ByteBuffer.wrap(messageBytes), dnsServerAddress);
        } catch (TextParseException exc){
            SocksRequestHandler.onError(selectionKey, NOT_AVAILABLE_ERROR);
            exc.printStackTrace();
        }
    }
    
    private void initResponseHandler() {
        dnsHandler = new Handler(null) {
            @Override
            public void handle(SelectionKey selectionKey) throws IOException {
                ByteBuffer byteBuffer = ByteBuffer.allocate(BUF_SIZE);
                if(socket.receive(byteBuffer) == null){
                    return;
                }

                Message response = new Message(byteBuffer.flip());
                Record[] answers = response.getSectionArray(Section.ANSWER);

                int responseId = response.getHeader().getID();
                MapValue unresolvedName = namesForConvert.get(responseId);
                if(answers.length == 0){
                    SocksRequestHandler.onError(unresolvedName.selectionKey, NOT_AVAILABLE_ERROR);
                    return;
                }

                String address = answers[0].rdataToString();
                InetSocketAddress socketAddress = new InetSocketAddress(address, unresolvedName.targetPort);
                AttachHandler.attachToTarget(unresolvedName.selectionKey, socketAddress);
                namesForConvert.remove(responseId);
            }
        };
    }

    private Message getMessage(String domainName) throws TextParseException {
        Header header = new Header(messageId++);
        header.setFlag(Flags.RD);
        header.setOpcode(0);

        Message message = new Message();
        message.setHeader(header);

        Record record = Record.newRecord(new Name(domainName + "."), Type.A, DClass.IN);
        message.addRecord(record, Section.QUESTION);

        return message;
    }

    private class MapValue {
        private SelectionKey selectionKey;
        private short targetPort;

        public MapValue(SelectionKey selectionKey, short targetPort) {
            this.selectionKey = selectionKey;
            this.targetPort = targetPort;
        }
    }
}
