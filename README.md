# CS-382
Course Assignments (Network-Centric Computing - Java)

## Assignment 1 - Part 1
Implemented a chat client system that would allow 2 clients to communicate messages across different computers. 
The clients are multithreaded, therefore, they can send and receive simultaneously.

**Usage:**  
```
> javac MultiThreadClient.java
> java MultiThreadClient <host> <portNumber>
```

## Assignment 1 - Part 2
Added a server which would keep track of all clients. Changed the protocol such that any new client would first 
have to register with the server. A client can query server for list of online users, query a specific user for 
their information (port and IP address). A client can then connect to a client without any further help from the 
server. The client is constantly pinging the server and in case the client does not ping the server for 15 seconds, 
the server lists the client as offline.

**Usage:**  
Server
```
> javac MultiServer.java
> java MultiServer <host> <portNumber>
```
Client
```
> javac MultiClient.java
> java MultiClient <host> <portNumber>
```

## Assignment 2
Implemented Chord, a peer-to-peer distributed hash table. Nodes (computers) can be dynamically added and removed. 
Nodes can put() files into the DHT and correspondingly get() files from the DHT.

**Usage:**  
```
> javac Node.java
> java Node
```
