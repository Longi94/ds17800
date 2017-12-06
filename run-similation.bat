SET version=1.0.0-SNAPSHOT

start cmd /k java -jar das-server-%version%.jar 10100

start cmd /k java -jar das-server-%version%.jar 10101

start cmd /k java -jar das-server-%version%.jar 10102

start cmd /k java -jar das-server-%version%.jar 10103

start cmd /k java -jar das-server-%version%.jar 10104

timeout 10

for /l %%x in (1, 1, 4) do (
    start cmd /k java -jar das-client-%version%.jar player 0
    start cmd /k java -jar das-client-%version%.jar player 1
    start cmd /k java -jar das-client-%version%.jar player 2
    start cmd /k java -jar das-client-%version%.jar player 3
    start cmd /k java -jar das-client-%version%.jar player 4
)

for /l %%x in (1, 1, 1) do (
    start cmd /k java -jar das-client-%version%.jar dragon 0
    start cmd /k java -jar das-client-%version%.jar dragon 1
    start cmd /k java -jar das-client-%version%.jar dragon 2
    start cmd /k java -jar das-client-%version%.jar dragon 3
    start cmd /k java -jar das-client-%version%.jar dragon 4
)
