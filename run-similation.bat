SET version=1.0.0-SNAPSHOT

start cmd /k java -jar das-server-%version%.jar 10100 ^> server0.log

start cmd /k java -jar das-server-%version%.jar 10101 ^> server1.log

start cmd /k java -jar das-server-%version%.jar 10102 ^> server2.log

start cmd /k java -jar das-server-%version%.jar 10103 ^> server3.log

start cmd /k java -jar das-server-%version%.jar 10104 ^> server4.log

timeout 10

for /l %%x in (1, 1, 2) do (
    start cmd /k java -jar das-client-%version%.jar player 0
    timeout 5
    start cmd /k java -jar das-client-%version%.jar player 1
    timeout 5
    start cmd /k java -jar das-client-%version%.jar player 2
    timeout 5
    start cmd /k java -jar das-client-%version%.jar player 3
    timeout 5
    start cmd /k java -jar das-client-%version%.jar player 4
    timeout 5
)

for /l %%x in (1, 1, 1) do (
    start cmd /k java -jar das-client-%version%.jar dragon 0
    timeout 5
    start cmd /k java -jar das-client-%version%.jar dragon 1
    timeout 5
    start cmd /k java -jar das-client-%version%.jar dragon 2
    timeout 5
    start cmd /k java -jar das-client-%version%.jar dragon 3
    timeout 5
    start cmd /k java -jar das-client-%version%.jar dragon 4
    timeout 5
)
