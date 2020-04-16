package orm;

import java.util.List;

//设计这一个类主要用来存放sql语句和所有的key，
//可读性高而且两个又存在着联系，故放在类中比较好
public class SQLAndKey {
    private String sql;
    private List<String> keyList;

    public SQLAndKey(String sql, List<String> keyList){
        this.sql = sql;
        this.keyList = keyList;
    }

    public String getSql() {
        return sql;
    }

    public List<String> getKeyList() {
        return keyList;
    }


}
