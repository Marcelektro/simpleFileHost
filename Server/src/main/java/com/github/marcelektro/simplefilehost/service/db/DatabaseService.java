package com.github.marcelektro.simplefilehost.service.db;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseService {

    Connection getConnection() throws SQLException;
    void initialSetup() throws Exception;

}
