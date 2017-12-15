#!/usr/bin/env bash

if [ ! $# -eq 3 ]; then
    echo "Script takes 3 parameters as input"
    echo "Usage: ./runSimulation <nrServers> <nrDragons> <nrPlayers>"
    echo "  e.g. ./runSimulation 5 20 100"
    exit 1
fi

trap "exit" INT TERM
trap "kill 0" EXIT

# clean up last run
mkdir -p logs/
rm -f logs/{SERVER,DRAGON,PLAYER}_*.out

for (( SERVER=0; SERVER<$1; SERVER+=1 )); do
    CPORT=$(( 10100 + $SERVER ))
    SPORT=$(( 20100 + $SERVER ))
    java -jar das-server/build/libs/das-server-1.0.0-SNAPSHOT.jar $CPORT $SPORT > logs/SERVER_${SERVER}.out &
done

for (( DRAGON=0; DRAGON<$2; DRAGON+=1 )); do
    java -jar das-client/build/libs/das-client-1.0.0-SNAPSHOT.jar dragon > logs/DRAGON_${DRAGON}.out &
    sleep 0.1
done

for (( PLAYER=0; PLAYER<$3; PLAYER+=1 )); do
    java -jar das-client/build/libs/das-client-1.0.0-SNAPSHOT.jar player > logs/PLAYER_${PLAYER}.out &
    sleep 0.1
done

for job in `jobs -p`
do
echo $job
    wait $job || let "FAIL+=1"
done
