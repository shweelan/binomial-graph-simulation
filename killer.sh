kill $(ps -ax|grep 'start.sh'|grep 'http:'|awk '{print $1}')
pkill java
