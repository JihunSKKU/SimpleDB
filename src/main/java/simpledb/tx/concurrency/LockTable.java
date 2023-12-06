package simpledb.tx.concurrency;

import java.util.*;
import simpledb.file.BlockId;

/**
 * The lock table, which provides methods to lock and unlock blocks.
 * If a transaction requests a lock that causes a conflict with an
 * existing lock, then that transaction is placed on a wait list.
 * There is only one wait list for all blocks.
 * When the last lock on a block is unlocked, then all transactions
 * are removed from the wait list and rescheduled.
 * If one of those transactions discovers that the lock it is waiting for
 * is still locked, it will place itself back on the wait list.
 * @author Edward Sciore
 */
class LockTable {
   private static final long MAX_TIME = 10000; // 10 seconds
   
   private Map<BlockId, List<Integer>> locks = new HashMap<>();
   
   /**
    * Grant an SLock on the specified block.
    * If an XLock exists when the method is called,
    * then the calling thread will be placed on a wait list
    * until the lock is released.
    * If the thread remains on the wait list for a certain 
    * amount of time (currently 10 seconds),
    * then an exception is thrown.
    * @param blk a reference to the disk block
    * @param txnum the ID of the transaction 
    */
   public synchronized void sLock(BlockId blk, int txnum) {
      try {
         long timestamp = System.currentTimeMillis();
         while (hasXlock(blk) && !waitingTooLong(timestamp)) {
            checkAbort(blk, txnum);
            wait(MAX_TIME);
         }
         checkAbort(blk, txnum);

         if (hasXlock(blk))
            throw new LockAbortException();

         List<Integer> txList = locks.get(blk);
         if (txList == null) {
            txList = new ArrayList<>();
            locks.put(blk, txList);
         }
         txList.add(txnum);
      }
      catch(InterruptedException e) {
         throw new LockAbortException();
      }
   }
   
   /**
    * Grant an XLock on the specified block.
    * If a lock of any type exists when the method is called,
    * then the calling thread will be placed on a wait list
    * until the locks are released.
    * If the thread remains on the wait list for a certain 
    * amount of time (currently 10 seconds),
    * then an exception is thrown.
    * @param blk a reference to the disk block
    * @param txnum the ID of the transaction
    */
   synchronized void xLock(BlockId blk, int txnum) {
      try {
         long timestamp = System.currentTimeMillis();
         while ((hasOtherSLocks(blk) || hasXlock(blk)) && !waitingTooLong(timestamp)) {
            checkAbort(blk, txnum);
            wait(MAX_TIME);
         }
         checkAbort(blk, txnum);

         if (hasOtherSLocks(blk) || hasXlock(blk))
            throw new LockAbortException();

         List<Integer> txList = new ArrayList<>();
         txList.add(-txnum);
         locks.put(blk, txList);
      }
      catch(InterruptedException e) {
         throw new LockAbortException();
      }
   }
   
   private void checkAbort(BlockId blk, int txnum) {
      List<Integer> txList = locks.get(blk);
      if (txList != null && !txList.isEmpty() && txList.get(0) < txnum) {
         throw new LockAbortException();
      }
   }

   /**
    * Release a lock on the specified block.
    * If this lock is the last lock on that block,
    * then the waiting transactions are notified.
    * @param blk a reference to the disk block
    */
   synchronized void unlock(BlockId blk, int txnum) {
      List<Integer> txList = locks.get(blk);
      if (txList != null) {
         txList.remove(Integer.valueOf(txnum));
         if (txList.isEmpty()) {
            locks.remove(blk);
            notifyAll();
         }
      }
   }
   
   private boolean hasXlock(BlockId blk) {
      List<Integer> txList = locks.get(blk);
      return txList != null && txList.get(0) < 0;
   }

   private boolean hasOtherSLocks(BlockId blk) {
      List<Integer> txList = locks.get(blk);
      return txList != null && txList.get(0) > 1;
   }
   
   private boolean waitingTooLong(long starttime) {
      return System.currentTimeMillis() - starttime > MAX_TIME;
   }
}
