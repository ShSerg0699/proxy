package nsu.shserg.proxy.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Attachment {
    private ObservableByteBuffer output;
    private ObservableByteBuffer input;
    private SocketChannel socketChannel;
    private int position = 0;

    public Attachment(ObservableByteBuffer outputBuffer, ObservableByteBuffer inputBuffer) {
        this.output = outputBuffer;
        this.input = inputBuffer;
    }

    public Attachment(int buffLength) {
        this.input = new ObservableByteBuffer(ByteBuffer.allocate(buffLength));
        this.output = new ObservableByteBuffer(ByteBuffer.allocate(buffLength));
    }

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public ByteBuffer getInput() {
        return input.getBuffer();
    }

    public ByteBuffer getOutput() {
        return output.getBuffer();
    }

    public ObservableByteBuffer getObservableOutputBuffer() {
        return output;
    }

    public ObservableByteBuffer getObservableInputBuffer() {
        return input;
    }

    public void setBufferCheck(ObservableByteBuffer.BufferCheck bufferCheck){
        input.setBufferCheck(bufferCheck);
    }

    public void checkBuffer(){
        output.checkBuffer();
    }

    public void closeSocketChannel() throws IOException {
        if(socketChannel != null) {
            System.out.println("Socket closed: " + socketChannel.getRemoteAddress());
            socketChannel.close();
        }
    }

    public void close(){
        output.close();
    }

    public boolean isSocketChanelShutDown(){
        return input.isReadyToClose();
    }

    public void prepareToWrite(){
        ByteBuffer inputBuffer = getInput();
        inputBuffer.flip();
        inputBuffer.position(position);
    }

    public boolean isReadyToClose(){
        return output.isReadyToClose() && input.isReadyToClose();
    }

    public void resetPosition() {
        this.position = 0;
    }

    public void setPosition() {
        ByteBuffer inputBuffer = getInput();
        this.position = inputBuffer.position();
        int newStartPosition = inputBuffer.limit();
        inputBuffer.clear();
        inputBuffer.position(newStartPosition);
    }
}
