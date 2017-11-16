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
