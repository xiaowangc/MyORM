package orm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Handler {


    //设计一个方法，用来解析sql语句，并提取其中的key
    SQLAndKey parseSQL(String sql) {
//        sql = "insert into wyc values(#{workerId},#{workerName},#{job},#{leaderName})";
        StringBuilder newSQL = new StringBuilder();//目的是为了存放之后的新SQL语句
        List<String> keyList = new ArrayList<>();//目的是为了存放所有解析出来的key
        //以下的操作都是在解析传进来的sql语句，并拼接完成放在newSql语句中
        while (true) {
            int left = sql.indexOf("#{");//找到#{出现的位置
            int right = sql.indexOf("}");//找到}出现的位置
            if (left != -1 && right != -1 && left < right) {//判断成一组 就获取key
                newSQL.append(sql.substring(0, left));
                newSQL.append("?");
                keyList.add(sql.substring(left + 2, right));//将中间的key添加到list中
            } else {//没有成一组
                newSQL.append(sql);//将剩下的sql拼接完
                break;
            }
            sql = sql.substring(right + 1);//原来sql的右半部分 做再次循环
        }
        return new SQLAndKey(newSQL.toString(), keyList);
    }

    //设计一个方法，将一个map集合中的value值拼接到sql语句中，参数obj就是一个map集合
    void setMap(PreparedStatement pstat, Object obj, List keyList) throws SQLException {
        Map map = (Map) obj;//先将obj强制转换为map集合
        //需要将keyList中解析出来的key遍历 去map中找寻value value赋值到SQL上进行拼接
        for (int i = 0; i < keyList.size(); i++) {
            Object value = map.get(keyList.get(i));//分析原来sql得到的#{key}
            //有key找出map集合中的value值拼接赋值
            pstat.setObject(i + 1, value);
        }
    }

    //设计一个方法，将一个domain对象拼接到sql语句中,参数obj就是一个domain对象
    void setObject(PreparedStatement pstat, Object obj, List keyList) throws SQLException {
        //obj是一个domain对象,需要利用反射将属性值拼接到pstat中
        Class clazz = obj.getClass();

        for (int i = 0; i < keyList.size(); i++) {
            try {
                //获取domain上相应的属性
                String key = (String) keyList.get(i);
                Field field = clazz.getDeclaredField(key);
                //属性对应的get方法名
                String name = "get" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
                //反射找到属性对应的get方法
                Method getMethod = clazz.getMethod(name);
                //执行get方法获取属性值
                Object value = getMethod.invoke(obj);
                //让pstat将数据拼接完整
                pstat.setObject(i + 1, value);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
    }


    //设计一个小弟 负责分析Object类型 将SQL和Object中的值拼接完整
    //  是否需要参数？ 1.pstat  2.真正的值Object  3.分析后得到的keyList
    //  obj是为了传递值的   可以一个(Integer int Float String) 可以一个domain 可以一个map
    void handleParameter(PreparedStatement pstat, Object obj, List keyList) throws SQLException {
        //先分析obj是什么类型的
        //反射看一看obj到底是什么
        Class clazz = obj.getClass();
        if (clazz == int.class || clazz == Integer.class) {
            pstat.setInt(1, (Integer) obj);
        } else if (clazz == float.class || clazz == Float.class) {
            pstat.setFloat(1, (Float) obj);
        } else if (clazz == double.class || clazz == Double.class) {
            pstat.setDouble(1, (Double) obj);
        } else if (clazz == String.class) {
            pstat.setString(1, (String) obj);
        } else if (clazz.isArray()) { //数组的话就不操作了
            //我就不操作啦    按顺序执行   我们写的SQL是按照key执行
        } else {
            if (obj instanceof Map) {
                this.setMap(pstat, obj, keyList);
            } else { //这里表示的obj是一个domain对象
                //需要将keyList中解析出来的key遍历 反射去domain对象中找属性 获取属性value 赋值到SQL上拼接
                this.setObject(pstat, obj, keyList);
            }
        }
    }


    //====================以上用来处理更新set的小弟
    //====================以下用来处理返回值get的小弟

    //设计一个小小弟 负责给下面这个方法赋值 map,结果存放在参数result中
    private void getMap(ResultSet rs, Map result) throws SQLException {
        //为一个传入的map对象赋值，因为result是引用类型，所有不用设置返回值
        //获取结果集中的全部信息(列 值)  存入map中  key value
        ResultSetMetaData rsMetaData = rs.getMetaData();
        for (int i = 1; i <= rsMetaData.getColumnCount(); i++) {
            //每次循环获取一个列名
            String columnName = rsMetaData.getColumnName(i);
            //去结果集中取值
            Object value = rs.getObject(columnName);
            //存入集合中
            result.put(columnName, value);
        }
    }

    //设计一个小小弟 负责将rs中的数据存入 domain对象result中
    //需要利用反射执行result中的setXXX方法，给所有属性赋值
    private void getObject(ResultSet rs, Object result) throws SQLException {
        Class clazz = result.getClass();
        try {
            //获取结果集中的全部信息(列 值)  存入map中  key value
            ResultSetMetaData rsMetaData = rs.getMetaData();
            for (int i = 1; i <= rsMetaData.getColumnCount(); i++) {
                //每次循环获取一个列名
                String columnName = rsMetaData.getColumnName(i);
                //去结果集中取值
                Object value = rs.getObject(columnName);
                //存入domain对象的属性中，即执行setXXX方法为属性赋值(也可以获取属性,为属性赋值)
                Field field = clazz.getDeclaredField(columnName);
                //获取属性名
                String fieldName = field.getName();
                //通过属性名拼接成相应的set方法名
                String setMethodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                //通过方法名，方法中的参数类型找到相应的方法
                Method setMethod = clazz.getMethod(setMethodName, field.getType());
                //执行set方法，为属性赋值--> value
                if(value instanceof Date){
                    value = value.toString();
                }
                setMethod.invoke(result, value);
            }
        } catch (NoSuchFieldException | NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }


    //设计一个小弟 负责分析结果集ResultSet
    //负责分析给定Class类型   确定返回值是什么类型
    //  进行组装  将结果集的值拆分出来 存入新的容器中
    //     参数?     Class  (domain map String int)
    //方法返回值都有可能是 (domain map String int)
    <T>T handleResult(ResultSet rs, Class resultType) throws SQLException {
        Object result = null;
        if (resultType == int.class || resultType == Integer.class) {
            result = rs.getInt(1);
        } else if (resultType == float.class || resultType == Float.class) {
            result = rs.getFloat(1);
        } else if (resultType == double.class || resultType == Double.class) {
            result = rs.getDouble(1);
        } else if (resultType == String.class) {
            result = rs.getString(1);
        } else { //这里resultType可能是map类，也有可能是domain类
            try {
                Constructor constructor = resultType.getConstructor();
                result = constructor.newInstance();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            if (result instanceof Map) { //属于map类或其子类
                this.getMap(rs, (Map) result);
            } else { //属于domain对象
                this.getObject(rs, result);
            }
        }
        return (T)result;
    }
}
