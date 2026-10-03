#!/usr/bin/env bash
set -euo pipefail

if [[ ${EUID} -ne 0 ]]; then
  echo "请使用 sudo 运行此脚本" >&2
  exit 1
fi

install -d -m 0755 /etc/systemd/system/docker.service.d
install -m 0644 /dev/stdin /etc/systemd/system/docker.service.d/override.conf <<'EOF'
[Service]
ExecStart=
ExecStart=/usr/bin/dockerd -H fd:// -H tcp://127.0.0.1:2375 --containerd=/run/containerd/containerd.sock
EOF

systemctl daemon-reload
systemctl restart docker
systemctl is-active --quiet docker

if ss -ltn | grep -qE '192\.168\.|0\.0\.0\.0:2375|\[::\]:2375'; then
  echo "Docker API 仍暴露在非回环地址" >&2
  exit 1
fi

echo "Docker API 已限制为虚拟机回环地址 127.0.0.1:2375"
