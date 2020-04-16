package pool;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

//连接池，用来存放好多MyConnection
//连接池应该只有一份，所有是单例的吧？？double check
public class ConnectionPool {


    private List<Connection> connectionList = new ArrayList<>();
    private static int poolSize = Integer.parseInt(ConnectionInformation.getValue("poolSize"));
    private ConnectionPool(){}
    //不用方法创建对象的原因：会造成堆内存溢出，循环调用
    //不用代码块的原因：没有返回对象
    //不用构造方法的原因：不让外界随便调用创建对象
    //禁止指令重排序volatile
    private volatile static ConnectionPool connectionPool;

    //double check
    public static ConnectionPool getInstance(){
        if(connectionPool == null){//第一层check,可能两个人同时访问都为null
            synchronized (ConnectionPool.class){
                if(connectionPool == null){ //第二层check 保证真的为null再创建对象
                    connectionPool = new ConnectionPool();
                }
            }
        }
        return connectionPool;
    }

    //连接池是单例的，代码块只要一个
    {
        for(int i = 0; i < poolSize;i++){
            connectionList.add(new MyConnection());
        }
    }

    //设计一个方法，提供一个MyConnection
    //当多个人同时访问调用这个方法获取一个MyConnection时，
    //可能存在抢资源的问题，所以此时必须加锁（方法结构体加或者方法内容里面加，这两种方法的区别是性能不同）
    private Connection getMyConnection(){
        Connection result = null;
        for(int i = 0; i<connectionList.size(); i++){
            MyConnection con = (MyConnection) connectionList.get(i);
            if(!con.isUsed()){
                synchronized (con) {
                    if(!con.isUsed()){
                        con.setUsed(true);
                        result = con;
                        break;
                    }
                }
            }
        }
        return result;
    }
    //设计一个方法，排队机制
    public Connection getConnection(){
        Connection mc = this.getMyConnection();
        int count = 0;
        while(mc == null && count < Integer.parseInt(ConnectionInformation.getValue("waitTime")) * 10){  //null表示获取不到，下面再获取一次
            mc = this.getMyConnection();
            try {
                Thread.sleep(100); //每100毫秒获取一次
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            count++;
        }
        if(mc == null){
            //抛出异常
            throw new TheNullPointerException("系统繁忙，请刷新后再来");
        }
        return mc;

    }

}
