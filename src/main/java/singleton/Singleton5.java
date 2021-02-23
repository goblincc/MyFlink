package singleton;

/**
 *  懒汉式（双重检查）
 */
public class Singleton5 {
    //构造器私有化
    private Singleton5(){

    }
    //volatile 变量修改线程可见 ，轻量级同步
    private static volatile Singleton5 instance;

    public static Singleton5 getInstance(){
        if(instance == null){
            synchronized (Singleton5.class){
                if(instance == null){
                    instance = new Singleton5();
                }
            }
        }
        return instance;
    }
}
