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

1. `bank-backend-gitops/k8s/infra/mysql-windows-setup.example.sql`：创建 bank_k8s 专用账号和两个数据库。
2. `bank-backend-springboot/database/schema.sql`：创建客户库表。
3. `bank-backend-springboot/account-service/database/schema.sql`：创建账户库表。

目的：每个服务有自己的库，K8S 使用专用账号而不是 root；当前开户经 Feign 同步调用账户服务。
成功标志：出现 bank_dev、bank_account 和对应表。

### 4.2 NAT 端口转发

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
