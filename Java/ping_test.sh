#!/bin/bash
printf "%s" "waiting for ServerXY ..."
while ! timeout 0.2 ping -c 1 -n 192.168.14.82 &> /dev/null
do
    printf "%c" "."
done
printf "\n%s\n"  "Server is back online"

