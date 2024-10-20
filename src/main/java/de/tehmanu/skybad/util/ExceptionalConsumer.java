package de.tehmanu.skybad.util;

import java.sql.SQLException;

/**
 * @author TehManu
 * @since 15.10.2024
 */
public interface ExceptionalConsumer<T> {

    void accept(T t) throws SQLException;
}
