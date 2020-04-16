package orm;

import orm.annotation.Delete;
import orm.annotation.Insert;
import orm.annotation.Select;
import orm.annotation.Update;
import pool.ConnectionPool;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SqlSession {

    //存放一个handler属性 帮我们处理SQL 参数 返回值等等
    private Handler handler = new Handler();

    //obj可能是基本数据类型，也可能是map集合，也有可能是引用数据类型，domain对象
    public void update(String sql, Object obj){
        //1.解析SQL语句
        SQLAndKey sqlAndKey = handler.parseSQL(sql);
        try {
            //2.获取连接
            Connection con = ConnectionPool.getInstance().getConnection();
            //3.获取状态参数(sql)
            PreparedStatement pstat = con.prepareStatement(sqlAndKey.getSql());
            //*4.将sql和问号信息拼接完整
            if(obj != null){
                handler.handleParameter(pstat,obj,sqlAndKey.getKeyList());
            }
            //5.执行executeUpdate()
            pstat.executeUpdate();
            //6.关闭
            pstat.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
    public void delete(String sql,Object obj){
        this.update(sql,obj);
    }
    public void insert(String sql,Object obj){
        this.update(sql,obj);
    }
    //-------------------------------------------
    public void update(String sql){
        this.update(sql,null);
    }
    public void delete(String sql){
        this.update(sql,null);
    }
    public void insert(String sql){
        this.update(sql,null);
    }

    //=======================以上是更新方法========================
    //=======================以下是查询方法========================

    //设计一个方法  可以处理任何一个表格的单条查询操作
    //需要的参数个数：3个 分别为sql语句、sql的信息、告知一个返回值类型（查询完毕后数据组装成的样子类型）
    //如果没有一个返回值类型，就不知道要放在哪个类型容器来盛放这些结果了
    //方法的返回值类型由第三个参数决定
    public <T>T selectOne(String sql,Object obj, Class resultType){

        if(this.selectAll(sql, obj, resultType).size() > 0){
            return (T) this.selectAll(sql, obj, resultType).get(0);
        }
        return null;
    }

    public <T> List<T> selectAll(String sql, Object obj, Class resultType){

        List<T> resultList = new ArrayList<>();
        //1.解析SQL语句
        SQLAndKey sqlAndKey = handler.parseSQL(sql);
        try {
            //2.获取连接
            Connection con = ConnectionPool.getInstance().getConnection();
            //3.获取状态参数(sql)
            PreparedStatement pstat = con.prepareStatement(sqlAndKey.getSql());
            //*4.将sql和问号信息拼接完整
            if(obj != null){
                handler.handleParameter(pstat,obj,sqlAndKey.getKeyList());
            }
            //5.执行executeQuery()
            ResultSet rs = pstat.executeQuery();

            //*6.将结果集中的数据拆出来  重新存入一个容器中resultType(domain map String int)
            //将结果集拆开放入一个容器中是因为后面要关闭这个结果集，需要把数据拿出来
            while(rs.next()){
                resultList.add(handler.handleResult(rs, resultType));
            }
            //6.关闭
            pstat.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultList;
    }

    //设计一个方法，用来产生dao层中dao类的代理对象
    //返回值为一个代理对象Class类型的子类，参数一个Class （必须是接口）
    public Object getMapping(Class clazz){  //WorkerDao接口
        //Proxy.newProxyInstance（？，？，？）会生成一个代理对象
        //需要三个参数：
        //  参数一：需要一个类加载器，将ClassLoader加载进来
        //  参数二：类数组 Class[],表示代理的接口是谁  通常数组就一个长度
        //  参数三：需要一个InvocationHandler  需要代理执行哪个方法
        return Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new InvocationHandler() {
            public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                //invoke有三个参数：第一个表示代理对象，
                //第二个表示需要执行的方法   第三个表示方法传递的参数（domain  String int...map）
                //1.获取方法上的注解
                Annotation an = method.getAnnotations()[0];
                //2.分析这个注解是什么类型---为了根据注解类型调用方法
                Class type = an.annotationType();
                //3.获取注解中的sql语句  执行注解的方法 获取sql
                Method valueMethod = type.getDeclaredMethod("value");
                String sql = (String)valueMethod.invoke(an);
                //4.分析方法参数(目前参数要么没有 要么就一个 String int float domain map)
                Object param = (objects==null) ? null : objects[0];
                if(type == Insert.class){
                    SqlSession.this.insert(sql,param);
                }else if(type == Update.class){
                    SqlSession.this.update(sql,param);
                }else if(type == Delete.class){
                    SqlSession.this.delete(sql,param);
                }else if(type == Select.class){ //匹配到查询方法，又分为单条查询和多条查询
                    //获取方法的返回值类型---来替代原来的resultType
                    Class methodReturnTypeClass = method.getReturnType();
                    if(methodReturnTypeClass == List.class){//是一个多条查询
                        //多条查询 获取List集合中的那个泛型
                        //获取返回值的具体类型(java.util.List<domain.Student>)
                        Type returnType = method.getGenericReturnType();
                        //Type是一个父接口 好多类型都实现了它
                        //现在上面那一行代码的效果是一个多态
                        //多态的类型还原回真实类型  ParameterizedType
                        ParameterizedType realReturnType = (ParameterizedType)returnType;
                        //继续反射这个类型中的所有泛型
                        //获取所有的泛型
                        Type[] patternTypes = realReturnType.getActualTypeArguments();
                        //我们只要第一个   集合里面的泛型类
                        Type patternType = patternTypes[0];
                        //将这个泛型类还原回Class
                        Class realPatternType = (Class)patternType;
                        //执行查询多条操作
                        return SqlSession.this.selectAll(sql,param,realPatternType);
                    }else{
                        return SqlSession.this.selectOne(sql,param,methodReturnTypeClass);
                    }
                }
                return null;
            }
        });
    }

}
