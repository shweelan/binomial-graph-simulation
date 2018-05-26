sudo yum -y install linuxptp
sudo sh -c "echo 'OPTIONS=\"-f /etc/ptp4l.conf -i eno1 -S\"' > /etc/sysconfig/ptp4l"
sudo iptables -I INPUT -p udp -m udp --dport 319 -j ACCEPT
sudo iptables -I INPUT -p udp -m udp --dport 320 -j ACCEPT
sudo systemctl restart ptp4l.service
sudo chkconfig ptp4l on
