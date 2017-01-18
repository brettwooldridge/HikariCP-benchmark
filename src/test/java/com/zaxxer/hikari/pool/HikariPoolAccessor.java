package com.zaxxer.hikari.pool;

public class HikariPoolAccessor
{
   private final HikariPool pool;

   public HikariPoolAccessor(HikariPool pool)
   {
      this.pool = pool;
   }

   public int[] getPoolStateCounts()
   {
      return pool.getPoolStateCounts();
   }
}
