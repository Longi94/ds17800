SET version=1.0.0-SNAPSHOT

start cmd /k java -jar das-server-%version%.jar 10100

start cmd /k java -jar das-server-%version%.jar 10101

rem start cmd /k java -jar das-server-%version%.jar 10102

rem start cmd /k java -jar das-server-%version%.jar 10103

rem start cmd /k java -jar das-server-%version%.jar 10104

timeout 10

for /l %%x in (1, 1, 5) do (
    start cmd /k java -jar das-client-%version%.jar player 0
    start cmd /k java -jar das-client-%version%.jar player 1
    timeout 1
)

for /l %%x in (1, 1, 1) do (
    start cmd /k java -jar das-client-%version%.jar dragon 0
    start cmd /k java -jar das-client-%version%.jar dragon 1
    timeout 1
)
