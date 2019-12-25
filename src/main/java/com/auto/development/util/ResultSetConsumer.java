package com.auto.development.util;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: TODO
 * @date 2019-05-11 13:32
 */
public interface ResultSetConsumer<T> {
    T apply(ResultSet rs) throws SQLException;
    void accept(T object);
}
