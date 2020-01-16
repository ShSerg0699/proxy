package nsu.shserg.proxy;

import nsu.shserg.proxy.handlers.AcceptHandler;
import nsu.shserg.proxy.handlers.Handler;
import nsu.shserg.proxy.util.Attachment;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class SocksProxy {
    private final int port;

    public SocksProxy(int port) {
        this.port = port;
    }

    public void start(){
        try{
            Selector selector = Selector.open();
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(port));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, new AcceptHandler(serverSocketChannel));

            DatagramChannel datagramSocket = DatagramChannel.open();
            datagramSocket.configureBlocking(false);

            DnsService dnsService = DnsService.getInstance();
            dnsService.setSocket(datagramSocket);
            dnsService.registerSelector(selector);

            while (true) {
                selector.select();
                Set<SelectionKey> readyKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = readyKeys.iterator();
                while (iterator.hasNext()) {
                    var readyKey = iterator.next();
                    try {
                        iterator.remove();
                        if(readyKey.isValid()) {
                            Handler handler = (Handler) readyKey.attachment();

                            if (readyKey.isWritable()) {
                                handler.write(readyKey);
                            }

                            if(readyKey.isValid() && readyKey.readyOps() != SelectionKey.OP_WRITE)
                                handler.handle(readyKey);
                        }
                    } catch (IOException exception) {
                        Handler handler = (Handler) readyKey.attachment();
                        Attachment attachment = handler.getAttachment();
                        SocketChannel firstSocket = (SocketChannel) readyKey.channel();

                        try {
                            System.out.println("Socket closed: " + firstSocket.getRemoteAddress());
                            firstSocket.close();
                            attachment.closeSocketChannel();
                        } catch (ClosedChannelException cce){
                            System.out.println(cce.getLocalizedMessage());
                        }
                    } catch (CancelledKeyException exc){
                    }
                }
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}
