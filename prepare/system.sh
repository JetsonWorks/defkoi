#!/bin/bash
# system configuration for DefKoi

[ "$UID" -eq 0 ] || {
  echo "You must run this as root" >&2
  exit 1
}
id 1000 >/dev/null || {
  echo "You should complete post-OEM setup first" >&2
  exit 1
}
user=$(id 1000 |cut -f2 -d\( |cut -f1 -d\))

apt update && apt upgrade -y
apt install -y curl

repoUrl=https://download.docker.com/linux/ubuntu
gpgUrl=https://download.docker.com/linux/ubuntu/gpg
grep -q $repoUrl /etc/apt/sources.list.d/* || {
  curl -fsSL $gpgUrl |apt-key add -
  echo "deb [arch=arm64] $repoUrl bionic stable" > /etc/apt/sources.list.d/Docker.list
}
apt update && apt install -y docker-ce docker-compose-plugin
usermod -aG docker $user

grep -q "default-runtime" /etc/docker/daemon.json || {
  sudo sed -ri "/^\{/ a\
  \    \"default-runtime\": \"nvidia\"," /etc/docker/daemon.json
}

systemctl get-default |grep -q "multi-user.target" || {
  read -ep "Set default runlevel to multi-user (instead of graphical)? [Y/n]: " answer
  [[ "$answer" != "N" && "$answer" != "n" ]] && systemctl set-default multi-user
}

mkdir -p /var/media /app/defkoi/log
chown -R $user:$user /var/media /app/defkoi

for (( i=5; i>0; i-- )); do
  printf "Rebooting in $i...\r"
  sleep 1
done
reboot

