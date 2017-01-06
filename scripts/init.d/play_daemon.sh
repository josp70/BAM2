#!/bin/bash
#To use this script:
#1) Save this file with name of your app in /etc/init.d folder.
#2) Update the APP_NAME to the name of your application in build.sbt.
#3) Update the APP_PATH of the application to the path where you extracted your dist version/snapshot.
#4) Update the value of PORT to the port you would like your application to run on.
#5) Add this script as service using update-rc.d. (Example: sudo update-rc.d your-app defaults).
#

APP_NAME="bam2"
APP_PATH=/tmp/bam2-1.0
PID_FILE=$APP_PATH/RUNNING_PID
DAEMON=$APP_PATH/bin/$APP_NAME
PORT=9000

function stopServer() {
  echo "Stopping $APP_NAME ..."
  if [ -e $PID_FILE ]
  then
    kill -9 `cat $PID_FILE`
    rm -f $PID_FILE
  else
      echo "$APP_NAME is not running"
  fi
}

function startServer() {
        if [ -e $PID_FILE ]
        then
                echo "PID File exists at $PID_FILE"
                pid=`cat $PID_FILE`
                if ps -p $pid > /dev/null
                then
                        echo "And server is already running with PID: $pid"
                        exit 1
                else
                        rm -f $PID_FILE
                fi
        fi
        echo "Starting $APP_NAME..."
        nohup $DAEMON >/dev/null -Dhttp.port=$PORT 2>&1 &

}

function checkStatus() {
        if [ -e $PID_FILE ]
        then
                pid=`cat $PID_FILE`
                if ps -p $pid > /dev/null
                then
                        echo "$APP_NAME is running with PID: $pid"
                        exit 0
                fi
        fi
        echo "$APP_NAME is not running"
}

case $1 in
  start)
        startServer
        ;;
  restart)
        echo "Restarting $APP_NAME"
        stopServer
        startServer
        ;;
  stop)
        stopServer
        ;;
  status)
        checkStatus
        ;;
  *)
        echo "Usage: $APP_NAME {start|restart|stop|status}"
        exit 1
        ;;
esac

exit 0
