# 银行微服务：全链路部署与 CI/CD 手册

本手册的目标是跑通这条链路：代码提交到 GitHub 后，GitHub Actions 测试并构建镜像；Ubuntu 中的 Kubernetes 运行 Nacos、RocketMQ 和三个业务服务；MySQL 保持部署在 Windows。

当前学习环境：

- Windows：IDE、MySQL、VMware、必要时的代理。
- Ubuntu VM：containerd、Kubernetes 单节点集群。
- Ubuntu 节点 IP：192.168.30.130。
- VMware NAT 网关：192.168.30.1。

真实密码、Token、secret.yaml 都不能提交 Git。

## 1. 架构全景

~~~
Git 提交
  ↓
GitHub Actions：测试 → Docker 构建 → 推送 GHCR
  ↓
Kubernetes：拉取镜像并启动业务服务
  ├── api-gateway：统一入口
  ├── customer-service：客户业务
  ├── account-service：账户业务
  ├── Nacos：注册与发现
  ├── RocketMQ：异步消息
  └── mysql（只是名称映射）→ Windows MySQL
~~~

当前 GitHub Actions 已完成 CI。当前 K8S 发布由人工执行 kubectl apply，属于手工 CD。后续用 Argo CD 自动监听 Git 后，才是完整自动 CD。

## 2. 仓库目录

| 路径 | 目的 |
| --- | --- |
| Dockerfile | customer-service 镜像构建文件。 |
| account-service/Dockerfile | account-service 镜像构建文件。 |
| api-gateway/Dockerfile | gateway 镜像构建文件。 |
| .github/workflows/ci.yml | CI 流水线。 |
| database/schema.sql | bank_dev 的客户库表。 |
| account-service/database/schema.sql | bank_account 的账户库表。 |
| k8s | 全部 Kubernetes 清单。 |
| k8s/infra/mysql-external.yaml | Windows MySQL 的 K8S 名称映射，不会创建 MySQL Pod。 |
| k8s/config/secret.example.yaml | 数据库凭据模板，没有真实密码。 |

## 3. Kubernetes 基础安装

如果当前节点已经是 Ready，本章只用于复习或重建，不要重复执行 kubeadm init。

### 3.1 内核网络参数

~~~
sudo modprobe overlay
sudo modprobe br_netfilter
cat <<'EOF' | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-iptables = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward = 1
EOF
sudo sysctl --system
~~~

目的：允许 Pod 网络转发流量。  
成功标志：输出中有 net.ipv4.ip_forward = 1。

### 3.2 containerd

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

### 3.3 Kubernetes 三个组件

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

### 3.4 初始化和网络插件

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

1. k8s/infra/mysql-windows-setup.example.sql：创建 bank_k8s 专用账号和两个数据库。
2. database/schema.sql：创建客户库表。
3. account-service/database/schema.sql：创建账户库表。

目的：每个服务有自己的库，K8S 使用专用账号而不是 root。  
成功标志：出现 bank_dev、bank_account 和对应表。

### 4.2 NAT 端口转发

以 Windows 管理员 PowerShell 执行：

~~~
netsh interface portproxy add v4tov4 listenaddress=192.168.30.1 listenport=3306 connectaddress=127.0.0.1 connectport=3306
New-NetFirewallRule -DisplayName 'MySQL for VMware K8s' -Direction Inbound -Action Allow -Protocol TCP -LocalAddress 192.168.30.1 -LocalPort 3306 -RemoteAddress 192.168.30.0/24
~~~

目的：只允许 VMware NAT 网段访问 Windows MySQL，不向整个局域网开放。  
成功标志：执行 netsh interface portproxy show v4tov4 后可以看到 192.168.30.1:3306 的映射。

如果 NAT 网段改变，必须同步修改 k8s/infra/mysql-external.yaml 中的地址。

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

## 7. CD 第一阶段：手工发布到 K8S

### 7.1 获取清单

在 Ubuntu 执行：

~~~
cd ~
git clone https://github.com/xiaopanyz-max/bank-backend-springboot.git
cd ~/bank-backend-springboot
ls k8s
~~~

目的：让 Ubuntu 拿到 K8S 部署清单。  
成功标志：能看到 apps、infra、kustomization.yaml。

此步骤仅用于学习。生产中由 Argo CD 拉取清单，人不需要登录节点执行 git clone。

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
scp ./k8s/config/secret.yaml aragon@192.168.30.130:/home/aragon/bank-backend-springboot/k8s/config/secret.yaml
~~~

### 7.3 发布所有服务

~~~
cd ~/bank-backend-springboot
kubectl apply -f k8s/config/secret.yaml
kubectl apply -k k8s/
kubectl get pods -n bank -w
~~~

目的：

- 第一条创建数据库密码 Secret。
- 第二条 Kustomize 一次发布 namespace、ConfigMap、Windows MySQL 名称映射、Nacos、RocketMQ、三个业务服务。
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

## 8. 故障排查

| 现象 | 首先执行 | 常见原因 |
| --- | --- | --- |
| git clone 卡住 | curl -I --max-time 10 https://github.com | Ubuntu 无外网或代理中转未运行。 |
| Pod ImagePullBackOff | kubectl describe pod -n bank Pod名称 | GHCR 包私有、镜像不存在、代理问题。 |
| Pod CreateContainerConfigError | kubectl describe pod -n bank Pod名称 | bank-local-secrets 不存在或 key 错误。 |
| 服务连不上 MySQL | kubectl logs -n bank deploy/customer-service | MySQL、端口映射、防火墙或账号 Host 限制错误。 |
| 节点 NotReady | kubectl get pods -A | Flannel 未运行或网络镜像拉取失败。 |

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

推荐演进顺序：

1. 先用当前方式手工执行 kubectl apply -k k8s 跑通。
2. 安装 Argo CD，创建 Application 指向仓库 k8s 目录。
3. 将镜像从 latest 改为固定 sha-提交号 标签。
4. CI 更新清单中的镜像标签并提交。
5. Argo CD 自动同步。
6. 项目变大后拆分“应用源码仓库”和“环境部署仓库”。

## 10. 日常发布流程

~~~
改代码
  ↓
git add / commit / push
  ↓
GitHub Actions 测试并构建镜像
  ↓
当前：Ubuntu 中 git pull 后 kubectl apply -k k8s
后续：Argo CD 自动同步
~~~

生产环境还需要：多副本、滚动发布、资源限制、监控告警、私有镜像凭据、数据库备份与恢复演练。
