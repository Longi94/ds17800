# ds17800
[![Build Status](https://travis-ci.com/Longi94/ds17800.svg?token=q2uGqSPNQRpjpMzytpn6&branch=master)](https://travis-ci.com/Longi94/ds17800)

## Build
`./gradlew assemble`

## Run
Servers, dragons and players are run as Java jar files. Run one process for every server or entity with server/port as parameters.

#### Server
`java -jar das-server/build/libs/das-server-1.0.0-SNAPSHOT.jar 10100 20100`

#### Dragon
`java -jar das-client/build/libs/das-client-1.0.0-SNAPSHOT.jar dragon`

#### Player
`java -jar das-client/build/libs/das-client-1.0.0-SNAPSHOT.jar player`

## Report
* [Final report (PDF)](https://raw.githubusercontent.com/Longi94/ds17800/master/report/distributed-systems-lab_2017-12-15_18-59.pdf
)
* [LaTeX source](https://www.overleaf.com/read/vccymnszfjkz)


## Fault tolerance
* Mirrored servers, no super nodes.

## Scalability
* Middleware service for managing masters.
* Entry point for the cliets. Get information about the masters.
* Start up sleeping servers to scale performance.

## Replication
* Synchronized masters, replicated states.
* Servers multicasts the command of their clients.

## Consistency
* Synchronized masters, replicated states.
* Race conditions are resolved by timestamp or id.

### Etc.
* Start with 3 servers.
* Clients have the middleware IP.
* Server handles action delays. On client.
* TCP

## Communication types

### Middleware:
* (Incoming)I am client, give me server to connect;
* (Incoming) I am server, add me to server list;
* (Outgoing) Server, are you still alive

### Server:
* (Incoming) I am client, here is my move
* (Outgoing) Client, here is status of your move

* (Outgoing) Server #i, here is my last moves, do you accept it
* (Incoming) I am Server #i, your moves are accepted/rejected

### Client:
* (Outgoing) Hi middleware, I am new client, give me server to connect
* (Outgoing) Server, it is my move
  

![arch](https://raw.githubusercontent.com/Longi94/ds17800/master/das.jpg)
