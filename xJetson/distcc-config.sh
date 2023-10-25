#!/bin/bash

echo "subnet: $subnet"
sed -i -re "s;^ALLOWEDNETS=.*;ALLOWEDNETS=\"$subnet\";" \
  -e 's/^LISTENER=.*/LISTENER="0.0.0.0"/' \
  -e 's/^STARTDISTCC=.*/STARTDISTCC="true"/' /etc/default/distcc
