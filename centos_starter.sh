sudo yum -y update
sudo yum -y install curl
sudo yum -y install git
sudo yum -y install java-1.8.0-openjdk-devel java-1.8.0-openjdk

clone_url="https://github.com/shweelan/binomial-graph-simulation.git"
clone_dir_name="simulation"
cd "$HOME"
if [ ! -d "$clone_dir_name" ] ; then
  git clone "$clone_url" "$clone_dir_name"
fi

redis_url="http://10.10.0.11:7379"
num_nodes_per_machine="4"

cd "$clone_dir_name"
chmod +x "start.sh"
/bin/bash ./start.sh $num_nodes_per_machine $redis_url > "start.log"  2>&1 &
