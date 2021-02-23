package singleton;

/**
 *  懒汉式（静态内部类）
 */
public class Singleton6 {
    //构造器私有化
    private Singleton6(){

    }

    //静态内部类在Singleton类装载时并不会立即实例化， 在调用getInstance()时会装载SingletonInstance
    //类的静态属性只会在第一次加载时初始化， JVM帮助我们保证了线程的安全， 在类进行初始化时， 别的线程是无法进入的
    private static class SingletonInstance {
        private static final Singleton6 INSTANCE = new Singleton6();
    }

    public static Singleton6 getInstance(){
        return SingletonInstance.INSTANCE;
    }
}
