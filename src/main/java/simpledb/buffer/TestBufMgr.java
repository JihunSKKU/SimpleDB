package simpledb.buffer;

import simpledb.server.SimpleDB;
import simpledb.file.*;
import java.util.*;

public class TestBufMgr {
   private static Map<BlockId,Buffer> buffs = new HashMap<>();
   private static BufferMgr bm;
   
   public static void main(String args[]) throws Exception {
      SimpleDB db = new SimpleDB("buffermgrtest", 400, 8); 
      bm = db.bufferMgr();    
      pinBuffer(6); pinBuffer(7); pinBuffer(8); pinBuffer(9);
      pinBuffer(10);
      bm.printStatus();
      unpinBuffer(8); unpinBuffer(9);
      bm.printStatus();
      pinBuffer(0); pinBuffer(9); pinBuffer(1);
      bm.printStatus();
      unpinBuffer(7); unpinBuffer(10); unpinBuffer(0);
      bm.printStatus();
      pinBuffer(2); pinBuffer(10); pinBuffer(3); pinBuffer(8);
      bm.printStatus();
      unpinBuffer(6); unpinBuffer(8); unpinBuffer(3);
      bm.printStatus();
      pinBuffer(3); pinBuffer(4); pinBuffer(8);
      bm.printStatus();
   }
   
   private static void pinBuffer(int i) {
      BlockId blk = new BlockId("test", i);
      Buffer buff = bm.pin(blk);
      buffs.put(blk, buff);
      System.out.println("Pin block " + i);
   }
   
   private static void unpinBuffer(int i) {
      BlockId blk = new BlockId("test", i);
      Buffer buff = buffs.remove(blk);
      bm.unpin(buff);
      System.out.println("Unpin block " + i);
   }
}
