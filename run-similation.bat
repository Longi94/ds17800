SET version=1.0.0-SNAPSHOT

start cmd /k java -jar das-server-%version%.jar 10100
timeout 1
start cmd /k java -jar das-server-%version%.jar 10101
timeout 1
start cmd /k java -jar das-server-%version%.jar 10102
timeout 1
start cmd /k java -jar das-server-%version%.jar 10103
timeout 1
start cmd /k java -jar das-server-%version%.jar 10104
timeout 1

for /l %%x in (1, 1, 10) do (
    start cmd /k java -jar das-client-%version%.jar player
    timeout 1
)

for /l %%x in (1, 1, 5) do (
    start cmd /k java -jar das-client-%version%.jar dragon
    timeout 1
)