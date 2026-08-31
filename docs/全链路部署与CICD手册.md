# 银行微服务：全链路部署与 CI/CD 手册

本手册的目标是跑通这条链路：代码提交到应用仓库后，GitHub Actions 测试并构建镜像；随后更新 GitOps 仓库中的 K8S 镜像版本；Argo CD 监听 GitOps 仓库并同步到 Kubernetes；MySQL 保持部署在 Windows。

当前学习环境：

- Windows：IDE、MySQL、VMware、必要时的代理。
- Ubuntu VM：containerd、Kubernetes 单节点集群。
- Ubuntu 节点 IP：192.168.30.130。
- VMware NAT 网关：192.168.30.1。

真实密码、Token、secret.yaml 都不能提交 Git。

## 1. 架构全景

~~~
应用仓库 Git 提交
  ↓
GitHub Actions：测试 → Docker 构建 → 推送 GHCR
  ↓
GitHub Actions：更新 GitOps 仓库 k8s/kustomization.yaml
  ↓
Argo CD：监听 GitOps 仓库 → 同步 Kubernetes
  ↓
Kubernetes：拉取镜像并启动业务服务
  ├── api-gateway：统一入口
  ├── customer-service：客户业务
  ├── account-service：账户业务
  ├── Nacos：注册与发现
  ├── RocketMQ：预留给未来异步业务；当前开户不使用它
  └── mysql（只是名称映射）→ Windows MySQL
~~~

当前已拆成两个仓库：

- `bank-backend-springboot`：应用源码、Dockerfile、CI。
- `bank-backend-gitops`：Kubernetes 清单、环境配置、Argo CD 引导配置。

应用仓库不再保存 `k8s/` 目录；K8S 期望状态统一由 GitOps 仓库管理。

## 2. 仓库目录

| 路径 | 目的 |
| --- | --- |
| Dockerfile | customer-service 镜像构建文件。 |
| account-service/Dockerfile | account-service 镜像构建文件。 |
| api-gateway/Dockerfile | gateway 镜像构建文件。 |
| .github/workflows/ci.yml | CI 流水线。 |
| database/schema.sql | bank_dev 的客户库表。 |
| account-service/database/schema.sql | bank_account 的账户库表。 |
| argocd/bank-backend-application.yaml | Argo CD Application 引导文件，指向 GitOps 仓库。 |

Kubernetes 清单已经拆到独立仓库：

| GitOps 仓库路径 | 目的 |
| --- | --- |
| k8s | 全部 Kubernetes 清单。 |
| k8s/apps | customer、account、gateway 的 Deployment 和 Service。 |
| k8s/config | 公共配置、SIT/UAT/PRD 环境配置、Secret 模板。 |
| k8s/infra | Windows MySQL 映射、Nacos、RocketMQ。 |
| k8s/observability | 日志采集配置。 |

## 3. Kubernetes 基础安装

如果当前节点已经是 Ready，本章只用于复习或重建，不要重复执行 kubeadm init。

### 3.1 关闭 swap

~~~
sudo swapoff -a
sudo sed -i '/ swap / s/^/#/' /etc/fstab
~~~

目的：Kubernetes 默认要求关闭 swap，避免节点内存管理和 Pod 调度出现不可预期行为。  
成功标志：执行 `free -h` 时，Swap 的 used 为 0。

### 3.2 内核网络参数

~~~
sudo modprobe overlay
sudo modprobe br_netfilter
cat <<'EOF' | sudo tee /etc/modules-load.d/k8s.conf
overlay
br_netfilter
EOF
cat <<'EOF' | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-iptables = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward = 1
EOF
sudo sysctl --system
~~~

目的：允许 Linux 加载容器网络需要的内核模块，并允许 Pod 网络转发流量。`/etc/modules-load.d/k8s.conf` 用来保证重启后仍会自动加载模块。  
成功标志：输出中有 net.ipv4.ip_forward = 1。

### 3.3 containerd

~~~
sudo apt-get update
sudo apt-get install -y containerd
sudo mkdir -p /etc/containerd
containerd config default | sudo tee /etc/containerd/config.toml >/dev/null
sudo sed -i 's/SystemdCgroup = false/SystemdCgroup = true/' /etc/containerd/config.toml
sudo systemctl enable --now containerd
systemctl is-active containerd
~~~

目的：containerd 是 Kubernetes 实际运行容器的运行时。  
成功标志：最后输出 active。

### 3.4 Kubernetes 三个组件

~~~
sudo mkdir -p -m 755 /etc/apt/keyrings
curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.36/deb/Release.key | sudo gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg
echo 'deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v1.36/deb/ /' | sudo tee /etc/apt/sources.list.d/kubernetes.list
sudo apt-get update
sudo apt-get install -y kubelet kubeadm kubectl
sudo apt-mark hold kubelet kubeadm kubectl
~~~

目的：

- kubeadm：创建集群。
- kubelet：节点上管理 Pod。
- kubectl：操作集群。

### 3.5 初始化和网络插件

~~~
sudo kubeadm init --pod-network-cidr=10.244.0.0/16
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
kubectl apply -f https://github.com/flannel-io/flannel/releases/latest/download/kube-flannel.yml
kubectl taint nodes --all node-role.kubernetes.io/control-plane-
kubectl get nodes
~~~

目的：kubeadm 创建控制面；Flannel 让 Pod 能互相通信；最后一条 taint 命令允许单节点控制面也运行业务 Pod。  
成功标志：kubectl get nodes 显示 Ready。

## 4. Windows MySQL，不部署到 K8S

MySQL 继续运行在 Windows。K8S 中的 mysql 仅是 Service 加 EndpointSlice，相当于内部通讯录：

~~~
Pod → mysql:3306 → 192.168.30.1:3306 → 127.0.0.1:3306 → Windows MySQL
~~~

因此它不会拉取 MySQL 镜像，也不会创建 MySQL Pod。

### 4.1 初始化数据库

在 Windows MySQL Workbench，以 MySQL 管理员账号执行：

1. `bank-backend-gitops/k8s/infra/mysql-windows-setup.example.sql`：创建 bank_k8s 专用账号和两个数据库。
2. `bank-backend-springboot/database/schema.sql`：创建客户库表。
3. `bank-backend-springboot/account-service/database/schema.sql`：创建账户库表。

目的：每个服务有自己的库，K8S 使用专用账号而不是 root；当前开户经 Feign 同步调用账户服务。
成功标志：出现 bank_dev、bank_account 和对应表。

### 4.2 开户幂等语义

开户接口使用 `globalSerialNo` 做入口幂等。当前策略是“防重复提交”，不是“结果重放”：

- 第一次请求：写入 `t_request_record`，状态从 `PROCESSING` 变为 `SUCCESS` 或 `FAILED`。
- 重复请求且上一笔仍为 `PROCESSING`：返回 `40900`，提示“请求正在处理中，请勿重复提交”。
- 重复请求且上一笔已结束：返回 `40900`，提示“请求已处理，请勿重复提交”。
- 如果要重新开户测试，必须同时更换新的 `globalSerialNo` 和新的 `customerNo`。

目的：学习环境中明确暴露重复提交，便于观察幂等记录和跨服务一致性问题。生产系统也可以选择“成功结果重放”语义，但需要配套保存可恢复的响应快照。

### 4.3 NAT 端口转发

以 Windows 管理员 PowerShell 执行：

~~~
netsh interface portproxy add v4tov4 listenaddress=192.168.30.1 listenport=3306 connectaddress=127.0.0.1 connectport=3306
New-NetFirewallRule -DisplayName 'MySQL for VMware K8s' -Direction Inbound -Action Allow -Protocol TCP -LocalAddress 192.168.30.1 -LocalPort 3306 -RemoteAddress 192.168.30.0/24
~~~

目的：只允许 VMware NAT 网段访问 Windows MySQL，不向整个局域网开放。  
成功标志：执行 netsh interface portproxy show v4tov4 后可以看到 192.168.30.1:3306 的映射。

如果 NAT 网段改变，必须同步修改 `bank-backend-gitops/k8s/infra/mysql-external.yaml` 中的地址。

## 5. 网络代理

Ubuntu 能直接访问 GitHub、GHCR 和镜像仓库时，不需要代理。

无法直连外网时，在 Ubuntu 当前终端运行：

~~~
export http_proxy=http://192.168.30.1:7877
export https_proxy=http://192.168.30.1:7877
curl -I --max-time 10 https://github.com
~~~

目的：让 Ubuntu 的外网请求经 Windows 代理转发。  
成功标志：curl 返回 HTTP/2 200 或 HTTP/1.1 200。  
注意：export 只影响当前终端，关闭终端后失效。生产环境应使用企业代理或内部镜像仓库。

## 6. CI：GitHub Actions

文件：.github/workflows/ci.yml。

| 触发事件 | 流水线行为 |
| --- | --- |
| push 到 main | 测试所有服务；通过后构建并推送镜像。 |
| 向 main 提交 Pull Request | 只测试，不推送镜像。 |
| 手动触发 | 重跑流水线。 |

流水线具体步骤：

1. checkout：下载源码。
2. setup-java：安装 JDK 21，缓存 Maven 依赖。
3. 对 customer、account、gateway 分别执行 mvn --batch-mode verify。
4. main 分支测试通过后登录 GHCR。
5. 使用各服务 Dockerfile 构建镜像并推送。

每张镜像同时有两个标签：

~~~
ghcr.io/xiaopanyz-max/bank-customer-service:sha-提交号
ghcr.io/xiaopanyz-max/bank-customer-service:latest
~~~

目的：SHA 标签可以精确回滚和追踪；latest 只适合本地学习。  
成功标志：GitHub Actions 页面全绿，GHCR 中有 customer、account、gateway 三个镜像包。

## 7. CD：GitOps 发布到 K8S

### 7.1 获取 GitOps 清单

在 Ubuntu 执行：

~~~
cd ~
git clone https://github.com/xiaopanyz-max/bank-backend-gitops.git
cd ~/bank-backend-gitops
ls k8s
~~~

目的：让 Ubuntu 拿到 GitOps 仓库中的 K8S 部署清单。  
成功标志：能看到 apps、infra、kustomization.yaml。

此步骤仅用于本地学习或故障排查。正常发布由 Argo CD 拉取 GitOps 仓库，人不需要登录节点执行 git clone。

### 7.2 创建 Secret

~~~
cp k8s/config/secret.example.yaml k8s/config/secret.yaml
nano k8s/config/secret.yaml
chmod 600 k8s/config/secret.yaml
~~~

填入 Windows MySQL 中 bank_k8s 账号的用户名和密码。

目的：密码交给 K8S Secret 管理；Deployment 只引用 Secret 名称。  
成功标志：文件存在，并且不显示在 git status 中。

如果 Secret 在 Windows 本机生成，可通过 SSH 上传：

~~~
scp ./k8s/config/secret.yaml aragon@192.168.30.130:/home/aragon/bank-backend-gitops/k8s/config/secret.yaml
~~~

### 7.3 发布所有服务

~~~
cd ~/bank-backend-gitops
kubectl apply -f k8s/config/secret.yaml
kubectl apply -k k8s/
kubectl get pods -n bank -w
~~~

目的：

- 第一条创建数据库密码 Secret。
- 第二条 Kustomize 一次发布 namespace、ConfigMap、SIT/UAT/PRD 环境配置、Windows MySQL 名称映射、Nacos、RocketMQ 基础设施、三个业务服务。
- 第三条持续观察 Pod 状态。

成功标志：bank 命名空间内所有 Pod 为 Running，READY 列等于容器总数。

### 7.4 验证

~~~
kubectl get svc -n bank
kubectl get endpointslice -n bank
kubectl logs -n bank deploy/api-gateway --tail=100
~~~

目的：确认 NodePort、Windows MySQL 映射和网关日志。

Windows 网关入口：

~~~
http://192.168.30.130:30080
~~~

## 8. 第二集群 cluster-b 接入步骤

第二台机器完成 `kubeadm init` 后，先不要急着部署业务，按下面顺序验收基础能力：

~~~
kubectl get nodes -o wide
kubectl get pods -A
kubectl cluster-info
~~~

目的：确认控制面、CoreDNS、Flannel 等基础组件正常。  
成功标志：节点为 Ready，`kube-system` 中核心 Pod 为 Running。

如果是单节点学习集群，需要允许控制面节点调度业务 Pod：

~~~
kubectl taint nodes --all node-role.kubernetes.io/control-plane-
~~~

接着给 cluster-b 准备和 cluster-a 等价的基础依赖：

1. 创建 `bank` 命名空间。
2. 创建访问 Windows MySQL 的 Secret。
3. 确认 MySQL、ES、代理等外部地址对 cluster-b 可达。
4. 在 GitOps 仓库新增 `overlays/sit-cluster-b`，放 cluster-b 独有的 IP、日志字段、资源和副本配置。
5. 在 cluster-b 安装 Argo CD，并创建指向 GitOps 仓库 `k8s/overlays/sit-cluster-b` 的 Application。

这样 cluster-a 和 cluster-b 共享同一套 `base`，但每个集群用自己的 overlay：

~~~
k8s/base
  ├─ 业务服务通用 Deployment/Service
  ├─ Nacos/RocketMQ/Fluent Bit 等通用组件
  └─ 公共 ConfigMap/Secret 模板

k8s/overlays/sit-cluster-a
  └─ cluster-a 的 IP、日志、资源、副本配置

k8s/overlays/sit-cluster-b
  └─ cluster-b 的 IP、日志、资源、副本配置
~~~

目的：两个 K8S 集群各自独立运行，但部署来源统一由 GitOps 管理。后续做双活时，只需要在入口流量层决定请求打到 cluster-a、cluster-b，应用部署方式不分叉。

cluster-b 是 4G 左右内存的副集群，但 MQ 承担部分业务功能，不能简单下线。因此 `sit-cluster-b` overlay 保留单副本 RocketMQ，同时把 Broker 压到低配模式：

~~~
JAVA_OPT_EXT=-Xms96m -Xmx128m -Xmn32m -XX:MaxDirectMemorySize=64m -XX:-AlwaysPreTouch -XX:ParallelGCThreads=1 -XX:ConcGCThreads=1
requests.cpu=50m
requests.memory=256Mi
limits.memory=512Mi
~~~

cluster-b 的 RocketMQ Broker 还通过 initContainer 在 Pod 启动前修正 hostPath 权限：

~~~
/var/lib/bank-k8s/rocketmq
/var/log/bank-k8s/rocketmq
~~~

原因：`hostPath.type=DirectoryOrCreate` 在新节点上创建的宿主机目录通常由 root 拥有。如果 RocketMQ 主容器不是 root 用户，Broker 会在很早期因为无法写 store/log 目录而退出，表现为 `CrashLoopBackOff`、`exitCode=253`，但 `kubectl logs` 里可能只有 JVM warning，宿主机日志目录也没有文件。cluster-a 没遇到这个问题，通常是因为目录曾经被手动创建/修改过权限，或者旧数据目录权限刚好可写；cluster-b 是全新节点，所以暴露了这个初始权限差异。

如果 Broker 仍然 `CrashLoopBackOff`，优先查看宿主机日志和内核 OOM 记录：

~~~
sudo find /var/log/bank-k8s/rocketmq -type f -name "*.log" -exec sh -c 'echo ==== $1; tail -120 "$1"' _ {} \;
sudo journalctl -k --no-pager | grep -i -E "oom|out of memory|killed process"
~~~

## 8. 故障排查

| 现象 | 首先执行 | 常见原因 |
| --- | --- | --- |
| git clone 卡住 | curl -I --max-time 10 https://github.com | Ubuntu 无外网或代理中转未运行。 |
| 安装 kubeadm 提示 `gpg: no valid OpenPGP data found`、`NO_PUBKEY`、`Unable to locate package kubeadm` | 重新下载 Kubernetes Release.key，并确认 key 文件非空 | Kubernetes 软件源 key 没下载成功，常见原因是连接被重置、代理/VPN 不稳定或写入了损坏的 key 文件。 |
| `kubeadm init` 提示 `failed to pull image registry.k8s.io/...`、`DeadlineExceeded`、`i/o timeout` | `curl -I --max-time 10 https://registry.k8s.io/v2/` 和 `curl -I --max-time 10 https://us-west2-docker.pkg.dev/v2/` | kubeadm 需要通过 containerd 拉 Kubernetes 控制面镜像；Shell 能访问 GitHub 不代表 containerd 能访问 registry.k8s.io。常见原因是 VM 网络/VPN/代理没覆盖 containerd。 |
| `kubeadm init` 在 `wait-control-plane` 阶段提示 `unable to create ClusterRoleBinding`、`context deadline exceeded` | `sudo KUBECONFIG=/etc/kubernetes/admin.conf kubectl get nodes` 和 `sudo journalctl -u kubelet -n 100 --no-pager` | 控制面静态 Pod 已经生成，但 apiserver/etcd/kubelet 启动慢或异常。4G 内存 VM 上可能只是启动超时，也可能是镜像、containerd、kubelet、swap 或端口 6443 未就绪。 |
| kubelet 日志提示 `running with swap on is not supported`、`Swap is on`、`fail-swap-on` | `free -h`、`cat /proc/swaps`、`sudo swapoff -a` | swap 没关闭或 `/etc/fstab` 中 swap 行没有注释，kubelet 会直接退出，导致 apiserver 6443 不监听。 |
| `argocd-applicationset-controller` CrashLoopBackOff，日志提示 `no matches for kind "ApplicationSet"` | `kubectl get crd | grep applicationsets` | Argo CD 的 ApplicationSet CRD 没安装或安装清单不完整。重新应用官方 install.yaml，或当前阶段不用 ApplicationSet 时先把该组件缩容为 0。 |
| Argo CD Application 提示 `ComparisonError`、`failed to list refs`、`context deadline exceeded` | `kubectl logs -n argocd deploy/argocd-repo-server --tail=100` | Argo CD 的 repo-server Pod 拉 GitHub 超时。宿主机/Ubuntu 终端能访问 GitHub，不代表 Argo CD Pod 能访问；需要给 `argocd-repo-server` 配置 HTTP/HTTPS 代理，或恢复 Pod 到 GitHub 的直连网络。 |
| Kibana 能打开但查不到日志 | `kubectl logs -n logging ds/fluent-bit --tail=120` | Fluent Bit 没把日志写进 Elasticsearch。若日志提示连接旧地址或 `tcp://<ip>:9200 timed out`，需要更新 GitOps base 里的 Fluent Bit ES Host。cluster-a 和 cluster-b 当前共用 ES 地址 `10.46.132.23:9200`。 |
| Kibana 有日志但按 `cluster` / `traceId` 查不到 | `kubectl get cm fluent-bit-config -n logging -o yaml | grep -A 5 spring_log_level` | Fluent Bit parser 没把业务日志中的 `cluster=...`、`traceId=...` 拆成 ES 字段。更新 parser 后只对新采集日志生效，旧日志不会自动回填。Kibana 精确查询优先用 `cluster.keyword`、`trace_id.keyword`。 |
| Pod ImagePullBackOff | kubectl describe pod -n bank Pod名称 | GHCR 包私有、镜像不存在、代理问题。 |
| Pod CreateContainerConfigError | kubectl describe pod -n bank Pod名称 | bank-local-secrets 不存在或 key 错误。 |
| 服务连不上 MySQL | kubectl logs -n bank deploy/customer-service | MySQL、端口映射、防火墙或账号 Host 限制错误。 |
| 节点 NotReady | kubectl get pods -A | Flannel 未运行或网络镜像拉取失败。 |

Kubernetes apt 源 key 下载失败时，先清理坏文件再重建：

~~~
sudo rm -f /etc/apt/keyrings/kubernetes-apt-keyring.gpg
sudo mkdir -p -m 755 /etc/apt/keyrings
curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.36/deb/Release.key -o /tmp/kubernetes-release.key
ls -lh /tmp/kubernetes-release.key
sudo gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg /tmp/kubernetes-release.key
echo 'deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v1.36/deb/ /' | sudo tee /etc/apt/sources.list.d/kubernetes.list
sudo apt-get update
~~~

目的：避免把网络错误页或空文件当成 GPG key 写入 apt 源，导致 Kubernetes 软件源不可用。

`kubeadm init` 拉取控制面镜像超时时，优先验证 registry 连通性：

~~~
curl -I --max-time 10 https://registry.k8s.io/v2/
curl -I --max-time 10 https://us-west2-docker.pkg.dev/v2/
~~~

如果 Shell 能访问外网，但 kubeadm/containerd 拉镜像仍超时，需要给 containerd 单独配置代理。下面的 `<WINDOWS_GATEWAY_IP>` 和 `<PROXY_PORT>` 按当前机器实际代理填写：

~~~
sudo mkdir -p /etc/systemd/system/containerd.service.d
cat <<EOF | sudo tee /etc/systemd/system/containerd.service.d/proxy.conf
[Service]
Environment="HTTP_PROXY=http://<WINDOWS_GATEWAY_IP>:<PROXY_PORT>"
Environment="HTTPS_PROXY=http://<WINDOWS_GATEWAY_IP>:<PROXY_PORT>"
Environment="NO_PROXY=127.0.0.1,localhost,10.96.0.0/12,10.244.0.0/16,10.46.132.0/24"
EOF
sudo systemctl daemon-reload
sudo systemctl restart containerd
sudo systemctl show containerd --property=Environment
~~~

配置前后都要验证 Ubuntu 能不能连上 Windows 代理端口：

~~~
ip route | awk '/default/ {print $3}'
nc -vz <WINDOWS_GATEWAY_IP> <PROXY_PORT>
curl -I --proxy http://<WINDOWS_GATEWAY_IP>:<PROXY_PORT> --max-time 10 https://registry.k8s.io/v2/
~~~

如果 `nc` 或 `curl --proxy` 提示 `Connection refused`，说明不是 Kubernetes 问题，而是 Windows 代理没有对虚拟机开放。检查 Clash、Mihomo、v2rayN 等代理软件是否开启 `Allow LAN` / `允许局域网连接`，监听地址不能只绑定 `127.0.0.1`，应允许 `0.0.0.0` 或宿主机局域网 IP；必要时放行 Windows 防火墙。

cluster-b 的 Argo CD 如果拉 GitHub 超时，需要给 repo-server 单独配置代理。下面的 `<WINDOWS_PROXY_IP>` 是 cluster-b Pod 能访问到的 Windows 代理地址：

~~~
kubectl set env deploy/argocd-repo-server -n argocd \
  HTTP_PROXY=http://<WINDOWS_PROXY_IP>:7878 \
  HTTPS_PROXY=http://<WINDOWS_PROXY_IP>:7878 \
  http_proxy=http://<WINDOWS_PROXY_IP>:7878 \
  https_proxy=http://<WINDOWS_PROXY_IP>:7878 \
  NO_PROXY=127.0.0.1,localhost,.svc,.cluster.local,10.96.0.0/12,10.244.0.0/16,kubernetes.default.svc \
  no_proxy=127.0.0.1,localhost,.svc,.cluster.local,10.96.0.0/12,10.244.0.0/16,kubernetes.default.svc

kubectl rollout status deploy/argocd-repo-server -n argocd
kubectl annotate application bank-backend -n argocd argocd.argoproj.io/refresh=hard --overwrite
~~~

目的：Argo CD 的 manifest 生成和 Git 拉取发生在 `argocd-repo-server` Pod 内，不发生在 SSH 登录的 Ubuntu 终端内。

然后先单独拉镜像，确认没问题再初始化集群：

~~~
sudo kubeadm config images pull
sudo kubeadm init --pod-network-cidr=10.244.0.0/16
~~~

目的：kubeadm 初始化集群时会先拉 apiserver、controller-manager、scheduler、etcd、pause、CoreDNS 等镜像。镜像拉不下来，集群就不会进入初始化阶段。

如果 `kubeadm init` 已经写出 `/etc/kubernetes/admin.conf`，但最后在 `wait-control-plane` 阶段超时，先不要立刻 reset。可能只是控制面启动慢，先检查 apiserver 是否已经延迟启动：

~~~
sudo ls -l /etc/kubernetes/admin.conf
sudo KUBECONFIG=/etc/kubernetes/admin.conf kubectl get nodes -o wide
sudo ss -lntp | grep 6443
sudo journalctl -u kubelet -n 100 --no-pager
~~~

如果 `kubectl get nodes` 已经能返回节点信息，说明集群控制面其实已经起来，可以继续复制 kubeconfig、安装 Flannel。只有确认 apiserver 不可用、kubelet 日志有明确错误时，再执行 `sudo kubeadm reset -f` 重新初始化。

## 9. CD 第二阶段：Argo CD

Argo CD 持续比较 Git 中的期望状态与集群实际状态，并执行同步。

~~~
提交 K8S 清单到 main
       ↓
Argo CD 发现 Git 变更
       ↓
同步到 Kubernetes
       ↓
集群状态与 Git 一致
~~~

当前 Argo CD Application 指向：

~~~yaml
repoURL: https://github.com/xiaopanyz-max/bank-backend-gitops.git
targetRevision: main
path: k8s
~~~

也就是说，Argo CD 不再监听应用源码仓库，而是监听 GitOps 仓库。应用仓库的 Actions 构建镜像后，会用 `GITOPS_REPO_TOKEN` 把新镜像 tag 写入 GitOps 仓库。

验证命令：

~~~
kubectl get applications -n argocd
kubectl get deploy -n bank -o custom-columns=SERVICE:.metadata.name,IMAGE:.spec.template.spec.containers[0].image
kubectl exec -n bank deploy/customer-service -- printenv | grep -E "APP_ENV|SPRING_PROFILES_ACTIVE|LOGGING_LEVEL"
~~~

## 10. 日常发布流程

~~~
改代码
  ↓
git add / commit / push
  ↓
GitHub Actions 测试并构建镜像
  ↓
GitHub Actions 更新 GitOps 仓库镜像 tag
  ↓
Argo CD 自动同步 GitOps 仓库到 K8S
~~~

生产环境还需要：多副本、滚动发布、资源限制、监控告警、私有镜像凭据、数据库备份与恢复演练。
