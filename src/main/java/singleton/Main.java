package singleton;

public class Main {
    public static void main(String[] args) {
        Singleton6 s1 = Singleton6.getInstance();
        Singleton6 s2 = Singleton6.getInstance();
        System.out.println(s1.equals(s2));
    }
}
