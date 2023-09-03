#!/usr/bin/sh
pkill -f starege_bot.jar
sleep 3
nohup python3.10 refresh.py > logs/nohup_refresh.log &
sleep 5
nohup java -cp starege_bot.jar MainKt > java_log.log &
