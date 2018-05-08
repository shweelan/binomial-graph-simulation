#!/bin/bash
set -e

placeholder="1"
bootstrap_key="BOOT"
redis_url="http://127.0.0.1:7379"
# TODO change it for linux hostname -I
ip="$(ifconfig en1 inet | tail -1 | cut -d ' ' -f 2)"
num_nodes_per_machine="10"
starting_port="2912"
total_nodes_key="NUM_TOTAL_NODES"
ready_nodes_key="NUM_READY_NODES"
separator="-_-"
working_dir="/Users/shweelan"
clone_dir_name="simulation"
build_dir="build"
logs_dir="logs"
clone_url="https://github.com/shweelan/binomial-graph-simulation.git"

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

# TODO install git and java

cd "$working_dir"
if [ ! -d "$clone_dir_name" ] ; then
  git clone "$clone_url" "$clone_dir_name"
fi
cd "$clone_dir_name"
git pull
rm -rf "$build_dir"
mkdir "$build_dir"
javac -Xdiags:verbose -Xlint:unchecked -g -d build/ *.java
mkdir -p "$logs_dir"

while [[ true ]]; do
  while [[ true ]]; do
    ready_str="READY_$ip"
    redis_api "SETEX" $ready_str 2 $placeholder
    redis_api "EXISTS" $bootstrap_key
    if [[ "$redis_api_ret" == "1" ]]; then
      redis_api "GET" $bootstrap_key
      n_max="$redis_api_ret"
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

  # TODO remove
  n_max="3"

  ts="$(date +"%s")"
  mkdir "$logs_dir/$ts"
  for (( i = 0; i < $num_nodes_per_machine; i++ )); do
    this_port="$(($starting_port + $i))"
    echo "I will start server $ip $this_port $n_max $ts"
    out="$logs_dir/$ts/$this_port-log.log"
    err="$logs_dir/$ts/$this_port-err.log"
    java -classpath "$build_dir" bn.Main $ip $this_port $n_max 2> "$err" 1> "$out" &
  done

  for job_pid in `jobs -p`; do
    wait $job_pid
    echo "JAVA process ended $job_pid"
    redis_api "DECR" $ready_nodes_key
  done
  redis_api "DECRBY" $total_nodes_key $num_nodes_per_machine
done
