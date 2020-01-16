package nsu.shserg.proxy;

public class Main {
    private final static int PORT_INDEX = 0;
    private final static int ARGS_COUNT = 1;

    public static void main(String[] args) {
        try {
            SocksProxy socksProxy = new SocksProxy(resolve(args));
            socksProxy.start();
        } catch (IllegalArgumentException exc){
            System.out.println("Wrong arguments");
            System.out.println("Usage: java -jar proxy.jar <port>");
        }
    }

    public static int resolve(String[] args) throws IllegalArgumentException{
        if(args.length != ARGS_COUNT){
            throw new IllegalArgumentException();
        }
        return Integer.parseInt(args[PORT_INDEX]);
    }
}
