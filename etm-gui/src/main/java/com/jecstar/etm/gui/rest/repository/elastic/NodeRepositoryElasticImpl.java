package com.jecstar.etm.gui.rest.repository.elastic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthAction;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequestBuilder;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoAction;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequestBuilder;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.admin.cluster.state.ClusterStateAction;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequestBuilder;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.routing.RoutingNodes;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.monitor.jvm.JvmInfo;
import org.elasticsearch.monitor.os.OsInfo;

import com.google.common.base.Predicate;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.converter.EtmConfigurationConverterTags;
import com.jecstar.etm.core.converter.json.EtmConfigurationConverterTagsJsonImpl;
import com.jecstar.etm.gui.rest.ClusterStatus;
import com.jecstar.etm.gui.rest.ClusterStatus.ShardStatus;
import com.jecstar.etm.gui.rest.Node;
import com.jecstar.etm.gui.rest.NodeStatus;
import com.jecstar.etm.gui.rest.StatusCode;
import com.jecstar.etm.gui.rest.repository.NodeRepository;

public class NodeRepositoryElasticImpl implements NodeRepository {
	
	private Predicate<ShardRouting> isEtmIndex() {return p -> p.getIndex().startsWith("etm_event_") || p.getIndex().equals("etm_configuration");};
	
	private final String indexName = "etm_configuration";
	private final String indexType = "node";
	private final String defaultId = "default_configuration";
	
	private final EtmConfigurationConverterTags tags = new EtmConfigurationConverterTagsJsonImpl();
	private final Client elasticClient;
	private final EtmConfiguration etmConfiguration;

	public NodeRepositoryElasticImpl(Client elasticClient, EtmConfiguration etmConfiguration) {
		this.elasticClient = elasticClient;
		this.etmConfiguration = etmConfiguration;
	}
	
	@Override
	public Map<String, Object> getNodeConfiguration(String nodeName) {
		GetResponse getResponse = this.elasticClient.prepareGet(this.indexName, this.indexType, this.defaultId).get();
		if (!getResponse.isExists()) {
			return Collections.emptyMap();
		}
		Map<String, Object> statusData = getResponse.getSourceAsMap();
		if (nodeName.equals("cluster")) {
			return statusData;
		}
		statusData.remove(this.tags.getLicenseTag());
		getResponse = this.elasticClient.prepareGet(this.indexName, this.indexType,  nodeName).get();
		if (getResponse.isExists()) {
			statusData.putAll(getResponse.getSourceAsMap());
		}
		return statusData;
	}

	@Override
	public List<Node> getNodes() {
		ClusterStateResponse clusterStateResponse = new ClusterStateRequestBuilder(this.elasticClient, ClusterStateAction.INSTANCE).clear().setNodes(true).get();
		List<Node> result = new ArrayList<Node>();
		clusterStateResponse.getState().getNodes().getNodes().forEach(c -> {
			Node node = new Node();
			node.name = c.value.getName();
			node.id = c.value.getId();
			result.add(node);
		});
		Collections.sort(result, new Comparator<Node>() {
			@Override
			public int compare(Node o1, Node o2) {
				return o1.name.toLowerCase().compareTo(o2.name.toLowerCase());
			}
		});
		return result;
	}

	@Override
	public void update(String nodeName, Map<String, Object> values) {
		if ("cluster".equals(nodeName)) {
			this.elasticClient.prepareUpdate(this.indexName, this.indexType, this.defaultId)
				.setConsistencyLevel(WriteConsistencyLevel.valueOf(this.etmConfiguration.getWriteConsistency().name()))
				.setDetectNoop(true)
				.setDoc(values)
				.get();
			return;
		} else {
			GetResponse getResponse = this.elasticClient.prepareGet(this.indexName, this.indexType, this.defaultId).get();
			if (!getResponse.isExists()) {
				return;
			}
			Map<String, Object> clusterData = getResponse.getSourceAsMap();
			clusterData.remove(this.tags.getLicenseTag());
			Iterator<Map.Entry<String, Object>> entryIterator = values.entrySet().iterator();
			while(entryIterator.hasNext()) {
				Map.Entry<String, Object> entry = entryIterator.next();
			    String key = entry.getKey();
				if (clusterData.containsKey(key) && clusterData.get(key).equals(values.get(key))) {
					entryIterator.remove();
				}
			}
			if (values.size() == 0) {
				this.elasticClient.prepareDelete(this.indexName, this.indexType, nodeName)
					.setConsistencyLevel(WriteConsistencyLevel.valueOf(this.etmConfiguration.getWriteConsistency().name()))
					.get();
			} else {
				this.elasticClient.prepareIndex(this.indexName, this.indexType, nodeName)
					.setConsistencyLevel(WriteConsistencyLevel.valueOf(this.etmConfiguration.getWriteConsistency().name()))
					.setSource(values)
					.get();
				
			}
		}
	}

	@Override
	public ClusterStatus getClusterStatus() {
		ClusterStatus clusterStatus = new ClusterStatus();
		ClusterHealthResponse clusterHealthResponse = new ClusterHealthRequestBuilder(this.elasticClient, ClusterHealthAction.INSTANCE).get();
		clusterStatus.clusterName = clusterHealthResponse.getClusterName();
		clusterStatus.statusCode = StatusCode.fromClusterHealthStatus(clusterHealthResponse.getStatus());
		clusterStatus.numberOfNodes =  clusterHealthResponse.getNumberOfNodes();
		clusterStatus.numberOfDataNodes = clusterHealthResponse.getNumberOfDataNodes();
		clusterStatus.numberOfActivePrimaryShards = clusterHealthResponse.getActivePrimaryShards();
		clusterStatus.numberOfActiveShards = clusterHealthResponse.getActiveShards();
		clusterStatus.numberOfRelocatingShards = clusterHealthResponse.getRelocatingShards();
		clusterStatus.numberOfInitializingShards = clusterHealthResponse.getInitializingShards();
		clusterStatus.numberOfUnassignedShards = clusterHealthResponse.getUnassignedShards();
		clusterStatus.numberOfDelayedUnassignedShards = clusterHealthResponse.getDelayedUnassignedShards();
		clusterStatus.numberOfPendingTasks = clusterHealthResponse.getNumberOfPendingTasks();
		clusterStatus.numberOfInFlightFethch = clusterHealthResponse.getNumberOfInFlightFetch();
		ClusterStateResponse stateResponse = new ClusterStateRequestBuilder(this.elasticClient, ClusterStateAction.INSTANCE).all().get();
		RoutingNodes routingNodes = stateResponse.getState().getRoutingNodes();
		List<ShardRouting> shards = routingNodes.shards(isEtmIndex());
		addShardToClusterStatus(clusterStatus, shards, stateResponse, isEtmIndex());
		addShardToClusterStatus(clusterStatus, routingNodes.unassigned(), stateResponse, isEtmIndex());
		return clusterStatus;
	}
	
	private void addShardToClusterStatus(ClusterStatus clusterStatus, Iterable<ShardRouting> shards, ClusterStateResponse stateResponse, Predicate<ShardRouting> predicate) {
		for (ShardRouting shard : shards) {
			if (!predicate.apply(shard)) {
				continue;
			}
			ShardStatus shardStatus = clusterStatus.new ShardStatus();
			shardStatus.id = shard.getId();
			shardStatus.active = shard.active();
			shardStatus.status = shard.state().name();
			if (shard.currentNodeId() != null) {
				shardStatus.node = stateResponse.getState().getNodes().get(shard.currentNodeId()).getName();
				shardStatus.nodeId = shard.currentNodeId();
			}
			if (shard.relocatingNodeId() != null) {
				shardStatus.relocatingNode = stateResponse.getState().getNodes().get(shard.relocatingNodeId()).getName();
				shardStatus.relocatingNodeId = shard.relocatingNodeId();				
			}
			shardStatus.primary = shard.primary();
			clusterStatus.addShardStatus(shard.getIndex(), shardStatus);
		}
	}

	@Override
	public NodeStatus getNodeStatus(String nodeId) {
		NodesInfoResponse nodesInfoResponse = new NodesInfoRequestBuilder(this.elasticClient, NodesInfoAction.INSTANCE)
				.all()
				.setNodesIds(nodeId)
				.get(TimeValue.timeValueMillis(10000));
		NodeStatus nodeStatus = new NodeStatus();
		if (nodesInfoResponse.getNodes() == null || nodesInfoResponse.getNodes().length < 1) {
			return nodeStatus;
		}
		NodeInfo nodeInfo = nodesInfoResponse.getNodes()[0];
		nodeStatus.id = nodeInfo.getNode().getId();
		nodeStatus.hostname = nodeInfo.getHostname();
		nodeStatus.address = nodeInfo.getTransport().getAddress().publishAddress().getAddress();
		nodeStatus.client = nodeInfo.getNode().isClientNode();
		nodeStatus.data = nodeInfo.getNode().isDataNode();
		nodeStatus.master = nodeInfo.getNode().isMasterNode();
		
		OsInfo os = nodeInfo.getOs();
		
		if (os != null) {
			nodeStatus.osAvailableProcessors =  os.getAvailableProcessors();
			nodeStatus.osRefreshInterval = os.getRefreshInterval();
//			Cpu osCpu = os.cpu();
//			if (osCpu != null) {
//				nodeStatus.osCpuCacheSize =  osCpu.getCacheSize().bytes();
//				nodeStatus.osCpuCoresPerSocket = osCpu.getCoresPerSocket();
//				nodeStatus.osCpuMhz = osCpu.getMhz();
//				nodeStatus.osCpuModel = osCpu.getModel();
//				nodeStatus.osCpuTotalCores = osCpu.getTotalCores();
//				nodeStatus.osCpuTotalSockets = osCpu.getTotalSockets();
//				nodeStatus.osCpuVendor = osCpu.getVendor();
//			}
//			Mem osMem = os.getMem();
//			if (osMem != null) {
//				nodeStatus.osMemTotal = osMem.getTotal().bytes();
//			}
//			Swap osSwap = os.getSwap();
//			if (osSwap != null) {
//				nodeStatus.osSwapTotal = osSwap.getTotal().bytes();
//			}
		}
		
		JvmInfo jvm = nodeInfo.getJvm();
		if (jvm != null) {
			nodeStatus.jvmStartTime = jvm.getStartTime();
			nodeStatus.jvmPid = jvm.getPid();
			nodeStatus.jvmName = jvm.getVmName();
			nodeStatus.jvmVendor =  jvm.getVmVendor();
			nodeStatus.jvmVersion =  jvm.getVersion() + " (" + jvm.getVmVersion() + ")";			
			org.elasticsearch.monitor.jvm.JvmInfo.Mem jvmMem = jvm.getMem();
			if (jvmMem != null) {
				nodeStatus.jvmMemDirectMax = jvmMem.getDirectMemoryMax().bytes();
				nodeStatus.jvmMemHeapInit = jvmMem.getHeapInit().bytes();
				nodeStatus.jvmMemHeapMax = jvmMem.getHeapMax().bytes();
				nodeStatus.jvmMemNonHeapInit = jvmMem.getNonHeapInit().getBytes();
				nodeStatus.jvmMemNonHeapMax = jvmMem.getNonHeapMax().getBytes();
			}
		}
		return nodeStatus;
	}

}
