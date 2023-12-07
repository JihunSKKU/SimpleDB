                      THE SIMPLEDB DATABASE SYSTEM
                  General Information and Instructions
---
## 1. Task 1: Improving Buffer Manager

SimpleDB 버퍼 매니저에는 두 가지 주요 효율성 문제가 있습니다:

- 대체할 버퍼를 찾을 때 첫 번째 언핀 버퍼를 사용하며, LRU와 같은 더 지능적인 방법 대신에 사용하지 않습니다.
- 버퍼가 이미 있는지 확인하기 위해 버퍼를 순차적으로 스캔하며, 데이터 구조 (예: 맵)를 사용하여 빠르게 버퍼를 찾지 않습니다.

이러한 문제를 해결하기 위해 BufferMgr 클래스를 다음 전략을 사용하여 수정해야 합니다:

- 언핀 버퍼의 목록을 유지합니다. 대체 버퍼가 필요할 때 목록의 헤드에서 버퍼를 제거하고 사용합니다. 버퍼의 핀 카운트가 0이 되면 버퍼를 목록의 끝에 추가합니다. 이렇게 하면 LRU 대체가 구현됩니다.
- 블록을 포함한 할당된 버퍼에 대한 키로 사용되는 맵을 유지합니다. 버퍼의 내용이 null이 아닌 경우 버퍼가 할당되며, 핀이 있을 수도 없을 수도 있습니다. 버퍼는 할당되지 않은 상태로 시작되며 한 번 블록에 할당되면 영원히 할당된 상태가 됩니다. 이 맵을 사용하여 현재 블록이 현재 버퍼에 있는지 여부를 확인합니다. 버퍼가 처음 할당되면 맵에 추가되어야 합니다. 버퍼가 대체되면 맵이 변경되어야 합니다. 이전 블록에 대한 매핑은 제거되어야 하며, 새로운 블록에 대한 매핑이 추가되어야 합니다.
- 더 이상 필요하지 않은 버퍼 풀 배열을 제거합니다.

또한 Buffer 클래스를 수정하여 각 Buffer 객체가 자체 버퍼 ID를 알 수 있도록 해야 합니다. 구체적으로는 생성자에 세 번째 인수로 버퍼의 ID를 나타내는 것이며, getId() 메서드를 추가하여 해당 ID를 반환해야 합니다.

BufferMgr 클래스는 현재 상태를 표시하는 printStatus 메서드도 가져야 합니다. 상태는 할당된 맵의 각 버퍼의 ID, 블록 및 핀 상태 및 언핀 목록의 각 버퍼의 ID입니다. 다음은 4개의 버퍼가 있는 데이터베이스에 대한 메서드가 생성해야 하는 출력 예입니다.

```
Allocated Buffers:
Buffer 1: [file test, block 1] pinned
Buffer 0: [file test, block 0] unpinned
Buffer 3: [file test, block 3] pinned
Buffer 2: [file test, block 2] unpinned
Unpinned Buffers in LRU order: 2 0
```

위의 출력에서는 버퍼가 해시 맵에서 검색되었기 때문에 순보기하는 것처럼 보이지만, 실제로는 LRU 순서입니다. 대괄호 내부의 정보는 BlockId의 toString() 메서드를 호출한 결과입니다.

코드를 디버깅하는 데 도움이 되도록 TestBufMgr.java라는 테스트 프로그램이 작성되었습니다. 이 프로그램은 버퍼를 핀 및 언핀하고 가끔씩 버퍼 매니저의 printStatus 메서드를 호출합니다.

### Test 하는 방법

```bash
cd ~/SimpleDB
mvn compile
cd target
java -cp ./classes simpledb.buffer.TestBufMgr
```

### 결과

```bash
Pin block 0
Pin block 1
Pin block 2
Pin block 3
Pin block 4
Pin block 5
Pin block 6
Pin block 7
Allocated Buffers:
Buffer 5: [file test, block 5] pinned
Buffer 4: [file test, block 4] pinned
Buffer 7: [file test, block 7] pinned
Buffer 6: [file test, block 6] pinned
Buffer 1: [file test, block 1] pinned
Buffer 0: [file test, block 0] pinned
Buffer 3: [file test, block 3] pinned
Buffer 2: [file test, block 2] pinned
Unpinned Buffers in LRU order: 
Unpin block 2
Unpin block 0
Unpin block 5
Unpin block 4
Allocated Buffers:
Buffer 5: [file test, block 5] unpinned
Buffer 4: [file test, block 4] unpinned
Buffer 7: [file test, block 7] pinned
Buffer 6: [file test, block 6] pinned
Buffer 1: [file test, block 1] pinned
Buffer 0: [file test, block 0] unpinned
Buffer 3: [file test, block 3] pinned
Buffer 2: [file test, block 2] unpinned
Unpinned Buffers in LRU order: 2 0 5 4 
Pin block 8
Pin block 5
Pin block 7
Allocated Buffers:
Buffer 2: [file test, block 8] pinned
Buffer 5: [file test, block 5] pinned
Buffer 4: [file test, block 4] unpinned
Buffer 7: [file test, block 7] pinned
Buffer 6: [file test, block 6] pinned
Buffer 1: [file test, block 1] pinned
Buffer 0: [file test, block 0] unpinned
Buffer 3: [file test, block 3] pinned
Unpinned Buffers in LRU order: 0 4
```

## 2. Task 2: Wait-Die Scheme

SimpleDB는 현재 데드락을 감지하기 위해 타임아웃을 사용합니다. 이를 대기-죽음 전략을 사용하도록 변경하세요. 코드는 LockTable 클래스를 다음과 같이 수정해야 합니다:

- sLock, xLock 및 unLock 메서드는 트랜잭션의 ID를 인수로 가져야 합니다.
- 락 변수는 블록이 블록에 대한 락을 보유한 트랜잭션 ID의 목록으로 매핑되도록 변경해야 합니다(정수가 아니라). 배타적 락을 나타내기 위해 음수 트랜잭션 ID를 사용하세요.
- sLock 및 xLock의 while 루프를 통과할 때마다 쓰레드가 중단되어야 하는지(즉, 현재 트랜잭션보다 오래된 트랜잭션이 있는지) 확인하세요. 그렇다면 코드는 LockAbortException을 throw해야 합니다.
- 또한 Transaction 및 ConcurrencyMgr 클래스를 약간 수정하여 트랜잭션 ID가 락 매니저 메서드로 전달되도록 해야 합니다.

테스트 프로그램은 세 개의 트랜잭션을 생성합니다. 트랜잭션 A와 C는 모두 트랜잭션 B가 보유한 락이 필요합니다. 대기-죽음 스키마에 따르면 트랜잭션 A는 기다려야 하고, 트랜잭션 C는 예외를 throw해야 합니다.

올바른 구현으로 테스트 프로그램을 실행하면 다음 출력이 나타납니다:

```
new transaction: 1
Transaction A starts
Tx A: request slock block 1
Tx A: receive slock block 1
new transaction: 2
Transaction B starts
Tx B: request xlock block 2
Tx B: receive xlock block 2
new transaction: 3
Transaction C starts
Tx C: request xlock block 1
Transaction C aborts
transaction 3 rolled back
Tx A: request slock block 2
Tx B: request slock block 1
Tx B: receive slock block 1
transaction 2 committed
Transaction B commits
Tx A: receive slock block 2
transaction 1 committed
Transaction A commits
```

### Test 방법

```bash
cd ~/SimpleDB
mvn compile
cd target
java -cp ./classes simpledb.tx.concurrency.TestDeadlock
```

--- 

This document contains the following sections:
    * Release Notes
    * Server Installation
    * Running the Server
    * Running Client Programs
    * SimpleDB Limitations
    * The Organization of the Server Code


I. Release Notes:

  This release of the SimpleDB system is Version 3.4, which was
  uploaded on March 24, 2021.  This release contains fixes a problem with the 
  file MultibufferProductScan.java from Version 3.3.

  SimpleDB is distributed in a WinZip-formatted file. This file contains
  five items:

    * The folder simpledb, which contains the server-side Java code.
    * The folder simpleclient, which contains some client-side code 
      for a SimpleDB database.
    * The folder derbyclient, which contains client-side code 
      For the Derby database, but with added features not supported
      by SimpleDB. These fies are examined in my "Database Design 
      and Implementation" text.
    * The file BookErrata.pdf, which describes how to update the code
      in my revised textbook "Database Design and Implementation" so
      that it conforms to version 3.4.
    * This document.

  The author welcomes all comments, including bug reports, suggestions
  for improvement, and anecdotal experiences.  His email address is 
  sciore@bc.edu
  

II. Installation Instructions:

  1)  Install the Java SDK.

  2) To install the SimpleDB engine, you must add the simpledb folder to 
     your classpath. To do so using Eclipse, first create a new project; 
     call it �SimpleDB Engine�. Then from the operating system, copy the 
     simpledb folder to the src folder of the project. Finally, refresh 
     the project from Eclipse, using the refresh command in the File menu.

  3) The simpleclient folder contains example programs that call the SimpleDB 
     engine. You should create a new project for them; call it �SimpleDB Clients�. 
     To ensure that the example programs can find the SimpleDB engine code, you 
     should add the SimpleDB Engine project to the build path of SimpleDB Clients. 
     Then use the operating system to copy the contents of simpleclient into the 
     src directory of SimpleDB Clients. 

  4) The derbyclient folder contains example programs that call the Derby engine. 
     This code illustrates features of JDBC that SimpleDB does not support, and
     is used in Chapter 2 of my "Database Design and Implementation" text.

III. Running the SimpleDB Server:

  You run the server code on a host machine, where it will sit and wait for 
  connections from clients. It is able to handle multiple simultaneous requests 
  from clients, each on possibly different machines. You can then run a client 
  program from any machine that is able to connect to the host machine.

  To run the SimpleDB server, run Java on the simpledb.server.StartServer class.  
  The argument to the class is the name of a folder that SimpleDB will use to 
  hold the database. If you leave out the argument, it will use "studentdb".
  If a folder with that name does not exist, then one will be created 
  automatically in the current directory.
 
  If everything is working correctly, when you run the server with a
  new database folder the following will be printed in the server's 
  window:

      creating new database
      new transaction: 1
      transaction 1 committed
      database server ready

  If you run the server with an existing database folder, the following
  will be printed instead:

      recovering existing database
      database server ready

  In either case, the server will then sit awaiting connections from
  clients.  As connections arrive, the server will print additional
  messages in its window.

  The server is implemented using RMI, on port 1099. If a registry is 
  running when the server is started, it will use that registry; 
  otherwise, it will create and run the registry itself.


IV. Running Client Code:

  SimpleDB clients can be run in embedded mode or network mode. To run a 
  client in embedded mode, use the EmbeddedDriver JDBC class with the 
  connection string "jdbc:simpledb:xyz", where xyz is the name of the database. 
  The database will be created if it does not exist, in the current directory 
  of the client program. No server is necessary

  To run a client in network mode, use the NetworkDriver class with the
  connection string "jdbc:simpledb://xyz", where xyz is the name or IP address
  of the machine running the SimpleDB server. Note that you cannot specify a
  database, because the client must use the database bound to the server.

  SimpleDB does not require a username and password, although
  it is easy enough to modify the server code to do so.

  The following list briefly describes the provided SimpleDB clients.

    * CreateStudentDB creates and populates the student database used
      by the other clients. It therefore must be the first client run 
      on a new database. 
    * StudentMajors prints a table listing the names of students and 
      their majors.
    * FindMajors asks the user for the name of a department. It then
      prints the name and graduation year of students having that major.
    * SimpleIJ repeatedly prints a prompt asking you to enter a 
      single line of text containing an SQL statement. The program then 
      executes that statement.  If the statement is a query, the output 
      table is displayed.  If the statement is an update command, then
      the number of affected records is printed. If the statement is ill
      formed, and error message will be printed. SimpleDB understands 
      only a limited subset of SQL, which is described below.
    * ChangeMajor changes the student named Amy to be a drama major.  
      It is the only client that updates the database (although you can 
      use SQLInterpreter to run update commands).

  These clients connect to the server at "localhost".  If the client is  
  to be run from a different machine than the server, then its source code 
  must be modified so that localhost is replaced by the domain name (or IP 
  address) of the server machine. 
  


V. SimpleDB Limitations

  SimpleDB is a teaching tool. It deliberately implements a tiny subset
  of SQL and JDBC, and (for simplicity) imposes restrictions not present
  in the SQL standard.  Here we briefly indicate these restrictions.


  SimpleDB SQL
  
  A query in SimpleDB consists only of select-from-where clauses in which
  the select clause contains a list of fieldnames (without the AS 
  keyword), and the from clause contains a list of tablenames (without
  range variables).
 
  The where clause is optional.  The only Boolean operator is and.  The
  only comparison operator is equality.  Unlike standard SQL, there are
  no other comparison operators, no other Boolean operators, no arithmetic
  operators or built-in functions, and no parentheses.  Consequently,
  nested queries, aggregation, and computed values are not supported.

  Views can be created, but a view definition can be at most 100 
  characters.
 
  Because there are no range variables and no renaming, all field names in
  a query must be disjoint.  And because there are no group by or order by
  clauses, grouping and sorting are not supported.  Other restrictions:

    * The "*" abbreviation in the select clause is not supported.
    * There are no null values.
    * There are no explicit joins or outer joins in the from clause.
    * The union and except keywords are not supported.
    * Insert statements take explicit values only, not queries.
    * Update statements can have only one assignment in the set clause.


  SimpleDB JDBC
  
  SimpleDB implements only the following JDBC methods:

   Driver

      public Connection connect(String url, Properties prop);
      // The method ignores the contents of variable prop.

   Connection

      public Statement createStatement();
      public void      close();

   Statement

      public ResultSet executeQuery(String qry);
      public int       executeUpdate(String cmd);

   ResultSet

      public boolean   next();
      public int       getInt();
      public String    getString();
      public void      close();
      public ResultSetMetaData getMetaData();

   ResultSetMetaData

      public int        getColumnCount();
      public String     getColumnName(int column);
      public int        getColumnType(int column);
      public int getColumnDisplaySize(int column);


VII. The Organization of the Server Code

  SimpleDB is usable without knowing anything about what the code looks
  like. However, the entire point of the system is to make the code
  easy to read and modify.  The basic packages in SimpleDB are structured
  hierarchically, in the following order:

    * file (Manages OS files as a virtual disk.)
    * log (Manages the log.)
    * buffer (Manages a buffer pool of pages in memory that acts as a
              cache of disk blocks.)
    * tx (Implements transactions at the page level.  Does locking
          and logging.)
    * record (Implements fixed-length records inside of pages.)
    * metadata (Maintains metadata in the system catalog.)
    * query (Implements relational algebra operations. Each operation 
             has a scan class, which can be composed to create a query tree.)
    * parse (Implements the parser.)
    * plan (Implements a naive planner for SQL statements.)
    * jdbc (Implements embedded and network interfaces for JDBC.)
    * server (The place where the startup and initialization code live. 
              The class Startup contains the main method.)

  The basic server is exceptionally inefficient. The following packages
  enable more efficient query processing:

    * index (Implements static hash and btree indexes, as well as 
             extensions to the parser and planner to take advantage
             of them.)
    * materialize (Implements implementations of the relational 
                   operators materialize, sort, groupby, and mergejoin.)
    * multibuffer (Implements modifications to the sort and product 
                   operators, in order to make optimum use of available
                   buffers.)
    * opt (Implements a heuristic query optimizer)
 
  My textbook "Database Design and Implementation", recently revised 
  and published by Springer, describes these packages in considerably 
  more detail. 
   
