package org.apache.commons.dbcp2;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

public final class DbcpPoolAccessor extends BasicDataSource
{
   private final AtomicInteger pendingThreads;

   public DbcpPoolAccessor()
   {
      super();

      pendingThreads = new AtomicInteger();
   }

   @Override
   public Connection getConnection() throws SQLException
   {
      try {
         pendingThreads.incrementAndGet();
         return super.getConnection();
      }
      finally {
         pendingThreads.decrementAndGet();
      }
   }

   public int getNumWaiters()
   {
      return pendingThreads.get();
   }

   public int getNumTotal()
   {
      return (int) getConnectionPool().getCreatedCount();
   }
}
