import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class Client {
    private SocketChannel socket = null;
    private int RequestIndex = 1;

    public void exec() throws IOException {
        open();

        CompletableFuture.runAsync(() -> {
            try {
                while(true){
                    getMesage(socket);
                }

            } catch (IOException e) {
                System.out.println("\n###error : disconnection!");
                return;
            }
        });

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.printf("Send(%d): ", RequestIndex);
            String msg = sc.next();
            sendMessage(socket, msg);
        }
    }

    private void open() throws IOException {
        SocketAddress addr = new InetSocketAddress("localhost", 9000);
        socket = SocketChannel.open(addr);
        System.out.println("###Connection Success : "+socket);
    }

    private void getMesage(SocketChannel socket) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        socket.read(buffer);

        byte[] arrBytes = buffer.array();
        int indexOfNullPoint = arrBytes.length;
        for(int i=0; i<arrBytes.length; i++){
            if(arrBytes[i]=='\0' || arrBytes[i]=='\20'){
                indexOfNullPoint = i;
                break;
            }
        }

        byte[] slicingArrBytes = new byte[indexOfNullPoint];
        for(int i=0; i<slicingArrBytes.length; i++){
            slicingArrBytes[i] = arrBytes[i];
        }

        String[] indexAndMsg = new String(slicingArrBytes).split("\1");
        System.out.printf("\nReceived(%s): %s\n", indexAndMsg[0], indexAndMsg[1]);
        System.out.printf("Send(%d): ", RequestIndex);
    }

    private void sendMessage(SocketChannel socket, String msg) throws IOException {
        byte[] bytes = ((RequestIndex++)+"\1"+msg).getBytes("utf-8");
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        buffer.put(bytes);
        buffer.clear();
        socket.write(buffer);
    }
}
