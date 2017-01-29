package org.apache.commons.dbcp2;

import java.sql.Connection;
import java.sql.SQLException;

public final class DbcpPoolAccessor extends BasicDataSource
{
   public DbcpPoolAccessor()
   {
      super();
   }

   @Override
   public Connection getConnection() throws SQLException
   {
      return super.getConnection();
   }

   public int getNumTotal()
   {
      return (int) getConnectionPool().getCreatedCount();
   }
}
