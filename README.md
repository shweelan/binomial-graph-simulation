# Binomial Graph Network Simulator


#On bash script
DEL STARTED_<IP>
SETEX READY_<IP> 2 ""

// BOOT MUST BE SETEX

while(GET BOOTING == null) {
  sleep 1
  SETEX READY_<IP> 2 ""
}

DEL READY_<IP>
SET STARTING_<IP> ""

INCRBY TOTAL_NODES <NUM_NODES YOU WILL START>

// WAITING THE BOOT TO START WILL HELP TO MAINTAIN FIXED NUMBER OF NODES

while(GET BOOT != null) {
  sleep 1
}

Start <NUM NODES> servers with different port for each
DEL STARTING_<IP>
SET STARTED_<IP>
kill self



#On each server
start your listener with the port given
RPUSH LISTENERS <IP:PORT>

totalNodes = GET TOTAL_NODES
while(LLEN LISTENERS < totalNodes) {
  sleep 1
}

// HERE ALL THE SERVERS STATED, WE DONT NEED THE TOTAL NUMBER OF NODES
DEL TOTAL_NODES

Nodes = LRANGE LISTENERS 0 -1
build graph for self
Get all routes from self to nodes
start sending till timeout, or num messages achieved

LREM LISTENERS <IP:PORT> 1
while(LLEN LISTENERS > 0) {
  sleep 1
}

// HERE ALL THE SERVERS HAVE STOPPED

Stop Listener
Start bash if not started! using booting.lock to check
kill self
