# 为什么造轮子
1. 光说不练假把式，造轮子就是要实践Raft算法。
2. 基于Raft算法的分布式KV存储，Golang，C++等语言的非常之多，Java版本非常少。

# 技术选型
1. RPC框架（Netty4）
2. 状态机&日志存储实现（Rocksdb）

# 实现功能
1. leader 选举
2. 日志复制
3. 成员变更

# 测试tiny-kv

## 测试选举
1. Idea配置5个Application启动类，并设置`SERVICE_PORT`，以及`SERVER_PORT`两个环境变量。
2. 启动5个RaftNodeBootstrap.startRaft()方法。
3. 监控日志。

```java
public class RaftNodeBootstrap {

    public static void main(String[] args) throws Throwable {
        startRaft();
    }

    public static void startRaft() throws Throwable {
        //获取Node实例
        DefaultNode node = DefaultNode.getInstance();

        //设置config信息
        String[] peerAddr = {"127.0.0.1:2781", "127.0.0.1:2782", "127.0.0.1:2783", "127.0.0.1:2784", "127.0.0.1:2785"};
        NodeConfig config = new NodeConfig();
        config.setSelfPort(Integer.valueOf(System.getenv("SERVICE_PORT")));
        config.setPeerAddrs(Arrays.asList(peerAddr));
        node.setConfig(config);

        //初始化节点
        node.init();

        //线程结束钩子,销毁node。
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                node.destroy();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }));
    }
}
```
![http://r9fyemfa9.hn-bkt.clouddn.com/idea.png](http://r9fyemfa9.hn-bkt.clouddn.com/idea.png)
![http://r9fyemfa9.hn-bkt.clouddn.com/leader.png](http://r9fyemfa9.hn-bkt.clouddn.com/leader.png)
![http://r9fyemfa9.hn-bkt.clouddn.com/follower.png](http://r9fyemfa9.hn-bkt.clouddn.com/follower.png)

## 测试KV功能

`存储uname:泰勒斯威夫特`

```java
public class TestKVStorage {

    private static RpcClient rpcClient;
    private static String addr = "127.0.0.1:2781";

    static {
        rpcClient = RpcClient.getInstance();
        rpcClient.startRpcClient(DefaultNode.getInstance());
    }

    @Before
    public void write() {
        RpcRequest<Object> request = RpcRequest.builder()
                .cmd(RpcRequest.CLIENT_REQ)
                .url(addr)
                .obj(ClientKVReq.builder()
                        .key("uname")
                        .value("泰勒斯威夫特")
                        .type(ClientKVReq.PUT)
                        .build()).build();
        try {
            Object response = rpcClient.sendSyncMsg(addr, request);
            System.out.println("写入key后，返回结果json:" + JSON.toJSONString(response));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void read() {
        RpcRequest<Object> request = RpcRequest.builder()
                .cmd(RpcRequest.CLIENT_REQ)
                .url(addr)
                .obj(ClientKVReq.builder()
                        .key("uname")
                        .type(ClientKVReq.GET)
                        .build()).build();
        try {
            Object response = rpcClient.sendSyncMsg(addr, request);
            System.out.println("读取Key后，返回结果:" + JSON.toJSONString(response));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }
}
```
