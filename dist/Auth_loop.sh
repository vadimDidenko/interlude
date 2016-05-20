#!/bin/bash

DBHOST=localhost
USER=root
PASS=
DBNAME=l2d

while :;
do
	#mysqlcheck -h $DBHOST -u $USER --password=$PASS -s -r $DBNAME>>"log/`date +%Y-%m-%d_%H:%M:%S`-sql_check.log"
	#mysqldump -h $DBHOST -u $USER --password=$PASS $DBNAME | gzip > "backup/`date +%Y-%m-%d_%H:%M:%S`-"$DBNAME"_loginserver.gz"
	mv log/java0.log.0 "log/`date +%Y-%m-%d_%H-%M-%S`_java.log"
	mv log/stdout.log "log/`date +%Y-%m-%d_%H-%M-%S`_stdout.log"
	nice -n -2 java  -Xms32m -Xmx32m -cp lib/*: l2d.auth.L2LoginServer > log/stdout.log 2>&1
	[ $? -ne 2 ] && break
	sleep 10;
done
