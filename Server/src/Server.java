import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

public class Server implements Runnable{
    private Selector selector;
    private HashMap<SocketChannel,String>dataMapper;
    private InetSocketAddress socketAddress;

    public Server(String address, int port) {
        socketAddress = new InetSocketAddress(address, port); // 소켓 주소 설정
        dataMapper = new HashMap<SocketChannel, String>();
    }

    @Override
    public void run() { // 서버가 실행되면 호출된다.
        // TODO Auto-generated method stub
        try {
            selector = Selector.open(); // 소켓 셀렉터 열기
            ServerSocketChannel socketChannel = ServerSocketChannel.open(); // 서버소켓채널 열기
            socketChannel.configureBlocking(false); // 블럭킹 모드를 False로 설정한다.

            socketChannel.socket().bind(socketAddress); // 서버 주소로 소켓을 설정한다.
            socketChannel.register(selector, SelectionKey.OP_ACCEPT); // 서버셀렉터를 등록한다.

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.out.println("###Server Started!");

        while (true) {
            try {
                selector.select(); // 셀럭터로 소켓을 선택한다. 여기서 Blocking 됨.

                Iterator<?> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) { // 셀렉터가 가지고 있는 정보와 비교해봄

                    SelectionKey key = (SelectionKey) keys.next();
                    keys.remove();

                    if (!key.isValid()) { // 사용가능한 상태가 아니면 그냥 넘어감.
                        continue;
                    }

                    if (key.isAcceptable()) { // Accept가 가능한 상태라면
                        accept(key);
                    }

                    else if (key.isReadable()) { // 데이터를 읽을 수 있는 상태라면
                        readData(key);
                    }
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    private void accept(SelectionKey key) { // 전달받은 SelectionKey로 Accept를 진행
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel;

        try {
            channel = serverChannel.accept();
            channel.configureBlocking(false);
            Socket socket = channel.socket();
            SocketAddress remoteAddr = socket.getRemoteSocketAddress();
            String[] arrRemoteAddr = remoteAddr.toString().split(":");
            System.out.printf("Connected by ('%s', %s)\n", arrRemoteAddr[0].substring(1), arrRemoteAddr[1]);

            // register channel with selector for further IO
            dataMapper.put(channel, remoteAddr.toString());
            channel.register(this.selector, SelectionKey.OP_READ);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private void readData(SelectionKey key) throws IOException { // 전달받은 SelectionKey에서 데이터를 읽는다.

        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int numRead = -1;

        try {
            numRead = channel.read(buffer);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            dataMapper.remove(channel);
            channel.close();
            e.printStackTrace();
        }

        byte[] arrBytes = buffer.array();
        int indexOfNullPoint = arrBytes.length;
        for(int i=0; i<arrBytes.length; i++){
            if(arrBytes[i]=='\0'){
                indexOfNullPoint = i;
                break;
            }
        }

        byte[] slicingArrBytes = new byte[indexOfNullPoint];
        for(int i=0; i<slicingArrBytes.length; i++){
            slicingArrBytes[i] = arrBytes[i];
        }

        String[] indexAndMsg = new String(slicingArrBytes).split("\1");

        if(indexAndMsg.length==2){
            System.out.printf("Received(%s): %s\n", indexAndMsg[0], indexAndMsg[1]);
        }
        else{
            System.out.println("###error : wrong message!");
        }

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(3000);
                String responseMsg = (indexAndMsg[1].equals("ping")) ? "pong" : indexAndMsg[1];
                writeData(key, indexAndMsg[0]+"\1"+responseMsg);
                System.out.printf("Send: %s (%s)\n",responseMsg, indexAndMsg[0]);
            } catch (InterruptedException | IOException e) {
                System.out.println("###error : wrong socket read!");
            }
        });

    }

    private void writeData(SelectionKey key, String msg) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();

        channel.write(ByteBuffer.wrap((msg).getBytes()));
    }
}
