public class Main {
    public static void main(String[] args) {
        Server server = new Server("localhost", 9000); // 서버 객체 생성
        server.run(); // 서버 실행
    }
}
