#!/bin/bash
set -e

placeholder="1"
bootstrap_key="BOOT"
redis_url="http://127.0.0.1:7379"
# TODO change it for linux hostname -I
if [[ $OSTYPE == "darwin"* ]] ; then
  ip="$(ifconfig en1 inet | tail -1 | cut -d ' ' -f 2)"
elif [[ "$OSTYPE" == "linux-gnu" ]]; then
  ip="$(hostname -I)"
else
  exit -1
fi
num_nodes_per_machine="10"
starting_port="2912"
total_nodes_key="NUM_TOTAL_NODES"
ready_nodes_key="NUM_READY_NODES"
test_id_key="TEST_NAME"
separator="-_-"
working_dir="/Users/shweelan"
clone_dir_name="simulation"
build_dir="build"
logs_dir="logs"
test_id="default"
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

  ts="$(date +"%s")"
  redis_api "SETNX" $test_id_key $ts
  redis_api "GET" $test_id_key
  test_id="$redis_api_ret"
  mkdir -p "$logs_dir/$test_id"
  for (( i = 0; i < $num_nodes_per_machine; i++ )); do
    this_port="$(($starting_port + $i))"
    echo "I will start server $test_id $ip $this_port"
    out="$logs_dir/$test_id/$this_port-log.log"
    err="$logs_dir/$test_id/$this_port-err.log"
    java -classpath "$build_dir" bn.Main $ip $this_port 2> "$err" 1> "$out" &
  done

  for job_pid in `jobs -p`; do
    wait $job_pid
    echo "JAVA process ended $job_pid"
    redis_api "DECR" $ready_nodes_key
  done
  redis_api "DECRBY" $total_nodes_key $num_nodes_per_machine
done
