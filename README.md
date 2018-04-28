# Binomial Graph Network Simulator


#On bash script
// Showing the operator which server is running
SETEX READY_<IP> 2 ""

// BOOTING MUST BE SETEX

while(GET BOOTING == null) {
  sleep 1
  SETEX READY_<IP> 2 ""
}

INCRBY NUM_TOTAL_NODES <MY_NUM_NODES YOU WILL SPAWN>

// WAITING THE BOOT TO START WILL HELP TO MAINTAIN FIXED NUMBER OF NODES

while(GET BOOT != null) {
  sleep 1
}

Start <MY_NUM_NODES> servers with different port for each
WAIT until java exits
FLUSHALL
LOOP


#On each server
start your listener with the port given
RPUSH LISTENERS <IP:PORT>

totalNodes = GET NUM_TOTAL_NODES
while(LLEN LISTENERS < totalNodes) {
  sleep 1
}

Nodes = LRANGE LISTENERS 0 -1
build graph for self
Get all routes from self to nodes
Set routes into listeners
INCR NUM_READY_NODES
while (GET NUM_READY_NODES < totlaNodes) {
  sleep 1
}
// HERE ALL servers are running and all the routes are calculated
start sending till timeout, or num messages achieved

LREM LISTENERS <IP:PORT> 1
while(LLEN LISTENERS > 0) {
  sleep 1
}

// HERE ALL THE SERVERS HAVE STOPPED

Stop Listener
Start bash if not started! using booting.lock to check
kill self
