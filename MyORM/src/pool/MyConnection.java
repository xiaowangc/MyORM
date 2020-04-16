package pool;

import java.sql.*;

//用到了代理模式和一个适配器模式AbstractMyConnection就是适配器
//顶级接口为Connection，真实类为JDBC4Connection(1.5版本驱动包下的), 代理类为MyConnection
public class MyConnection extends AbstractMyConnection {
    //有两个属性:Connection boolean
    //属性connection的真实身份是ConnectionImpl，通过反射可以识别出来
    private Connection connection;//connection不赋值的原因就是DriveManager.getConnection会产生异常，属性里处理不了异常
    private boolean isUsed = false;
    //这几个string类型的属性后面需要写进一个配置文件里

    private static String className = ConnectionInformation.getValue("className");
    private static String url = ConnectionInformation.getValue("url");
    private static String name = ConnectionInformation.getValue("username");;
    private static String password = ConnectionInformation.getValue("password");;



    //放在静态代码块表示只要一份就够了
    static {
        try {
            Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    //放在普通代码块中表示每次执行一次构造方法就获取一个连接，但不关闭
    {
        try {
            connection = DriverManager.getConnection(url,name,password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Statement createStatement() throws SQLException {
        return this.connection.createStatement();
    }


    public PreparedStatement prepareStatement(String s) throws SQLException {
        return this.connection.prepareStatement(s);
    }
    public void close() throws SQLException {
        this.isUsed = false;
    }






    public MyConnection(){}

    public Connection getConnection() {
        return connection;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed(boolean isUsed) {
        this.isUsed = isUsed;
    }


}
