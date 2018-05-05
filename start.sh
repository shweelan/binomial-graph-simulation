#!/bin/bash
set -e

placeholder="1"
bootstrap_key="BOOT"
redis_url="http://127.0.0.1:7379"
# TODO change it for linux
ip="$(ifconfig en1 inet | tail -1 | cut -d ' ' -f 2)"
num_nodes_per_machine="5"
starting_port="2912"
total_nodes_key="NUM_TOTAL_NODES"
ready_nodes_key="NUM_READY_NODES"
separator="-_-"

redis_api_ret=""
redis_api() {
  local command="$redis_url"
  for var in "$@"; do
    command="$command/$var"
  done
  command="$command.txt"
  echo "$command"
  redis_api_ret=$(curl -s "$command")
  echo "$redis_api_ret"
}

# TODO install git and java and pull the code and build it with make

while [[ true ]]; do
  while [[ true ]]; do
    redis_api "SETEX" $ip 2 $placeholder
    redis_api "EXISTS" $bootstrap_key
    if [[ "$redis_api_ret" == "1" ]]; then
      break
    fi
    sleep 1
  done

  redis_api "INCRBY" $total_nodes_key $num_nodes_per_machine

  while [[ true ]]; do
    redis_api "EXISTS" $bootstrap_key
    if [[ "$redis_api_ret" == "0" ]]; then
      break
    fi
    sleep 1
  done

  for (( i = 0; i < $num_nodes_per_machine; i++ )); do
    echo "I will start server $(($starting_port + $i))"
  done

  for job_pid in `jobs -p`; do
    wait $job_pid
    redis_api "DECR" $ready_nodes_key
  done
  redis_api "DECRBY" $total_nodes_key $num_nodes_per_machine
done
