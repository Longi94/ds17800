#!/usr/bin/env bash

if [ ! $# -eq 3 ]; then
    echo "Script takes 3 parameters as input"
    echo "Usage: ./runSimulation <nrServers> <nrDragons> <nrPlayers>"
    echo "  e.g. ./runSimulation 5 20 100"
    exit 1
fi


# clean up last run
pkill java
mkdir -p logs/
rm logs/{SERVER,DRAGON,PLAYER}_*.out

for (( SERVER=0; SERVER<$1; SERVER+=1 )); do
    PORT=$(( 10100 + $SERVER ))
    java -jar das-server/build/libs/das-server-1.0.0-SNAPSHOT.jar $PORT > logs/SERVER_${SERVER}.out &
done

# ensure servers are up
sleep 6s

for (( DRAGON=0; DRAGON<$2; DRAGON+=1 )); do
    MOD_FIVE=$(( $DRAGON % 5 ))
    PORT=$(( 10100 + $MOD_FIVE ))
    java -jar das-client/build/libs/das-client-1.0.0-SNAPSHOT.jar dragon $PORT > logs/DRAGON_${DRAGON}.out &
done

for (( PLAYER=0; PLAYER<$3; PLAYER+=1 )); do
    MOD_FIVE=$(( $PLAYER % 5 ))
    PORT=$(( 10100 + $MOD_FIVE ))
    java -jar das-client/build/libs/das-client-1.0.0-SNAPSHOT.jar player $PORT > logs/PLAYER_${PLAYER}.out &
done
