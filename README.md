# fs17800

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

### Communication types
-> (Incoming)
<- (outgoing)

* Middleware:
	-> I am client, give me server to connect;
	-> I am server, add me to server list;
	<- Server, are you still alive

* Server:
	-> I am client, here is my move
	<- Client, here is status of your move

	<- Server #i, here is my last moves, do you accept it
	-> I am Server #i, your moves are accepted/rejected

* Client:
	<- Hi middleware, I am new client, give me server to connect
	<- Server, it is my move
  

![arch](https://i.imgur.com/NWXFBIf.jpg)
