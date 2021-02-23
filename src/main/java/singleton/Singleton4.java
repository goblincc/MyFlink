package singleton;

/**
 *  懒汉式（线程安全，每次调用的时候都需要同步，效率低，不推荐使用）
 */
public class Singleton4 {
    private Singleton4(){

    }
    private static Singleton4 instance;

    //加入同步代码块
    public static synchronized Singleton4 getInstance(){
        if(instance == null){
            instance = new Singleton4();
        }
        return instance;
    }
}
