package nsu.shserg.proxy.util;

import java.nio.ByteBuffer;

public class ObservableByteBuffer {

    public interface BufferCheck {
        void update();
    }

    private ByteBuffer buffer;
    private boolean isClose = false;
    private BufferCheck bufferCheck;

    public ObservableByteBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public void checkBuffer(){
        bufferCheck.update();
    }

    public void setBufferCheck(BufferCheck bufferCheck){
        this.bufferCheck = bufferCheck;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void close() {
        isClose = true;
    }

    public boolean isReadyToClose(){
        return buffer.remaining() == 0 && isClose;
    }
}