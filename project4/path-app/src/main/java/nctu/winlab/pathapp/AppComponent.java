/*
 * Copyright 2014-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nctu.winlab.pathapp;

import com.google.common.collect.Maps;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;

import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.host.HostService;
import org.onosproject.net.PortNumber;
import org.onosproject.net.ConnectPoint;

import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;

import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.Link;

import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;

import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyVertex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.ArrayDeque;


@Component(immediate = true)
public class AppComponent {
	
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;
	
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;


	@Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;
	
	@Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowObjectiveService flowObjectiveService;
	
	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected TopologyService topologyService;
	
	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected LinkService linkService;
	

    private PacketProcessor processor = new ReactivePacketProcessor();
	
	private static ArrayList<TopologyVertex> Topo_Path = new ArrayList<TopologyVertex>();
	private TopologyVertex srcSwitch;
	private TopologyVertex dstSwitch;
	private Host srcHost;
	private Host dstHost;
	private IpAddress srcIp;
	private IpAddress dstIp;

	
    private ApplicationId appId;
	
	@Activate
    protected void activate() {
		
        appId = coreService.registerApplication("nctu.winlab.pathapp");	
        packetService.addProcessor(processor, PacketProcessor.director(2));
		requestIntercepts();
		
        log.info("Started");
    }
	
	@Deactivate
	protected void deactivate() {
        packetService.removeProcessor(processor);
		flowRuleService.removeFlowRulesById(appId);
        processor = null;
        log.info("Stopped");
	}

	private void requestIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);
	}

	
	private class ReactivePacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {

            if (context.isHandled()) {
                return;
            }
			
			Topology topo = topologyService.currentTopology();
			TopologyGraph graph = topologyService.getGraph(topo);
			
			InboundPacket inPkt = context.inPacket();
			Ethernet ethPkt = inPkt.parsed();
			MacAddress srcMac = ethPkt.getSourceMAC();
			MacAddress dstMac = ethPkt.getDestinationMAC();
			
			Topo_Path.clear();
			
            ConnectPoint cp = inPkt.receivedFrom();
            
            HostId srcId = HostId.hostId(srcMac);
            HostId dstId = HostId.hostId(dstMac);
			
			
			
			log.info("Packet-in from device of:{}",cp.deviceId());
            log.info("Start to install path from {} to {}",srcId.toString() ,dstId.toString());
			


			for(TopologyVertex v: graph.getVertexes()){
				for(Host h: hostService.getConnectedHosts(v.deviceId())){
					if(h.mac().equals(srcMac)){
						srcSwitch = v;
						srcHost = h;
					}
					if(h.mac().equals(dstMac)){
						dstSwitch = v;
						dstHost = h;
					}
				}
			}      


			for(IpAddress p: srcHost.ipAddresses()){
				srcIp = p;
			}
			for(IpAddress p: dstHost.ipAddresses()){
				dstIp = p;
			}
			

            ArrayList<TopologyVertex> path = findpath(graph, srcSwitch, dstSwitch);	

            for(int i = path.size() - 1; i >= 0; i--){
				if(i == path.size() - 1){
					for(Host h: hostService.getHostsByMac(dstMac)){
                        PortNumber portNumber = h.location().port();
						TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
                        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPSrc(srcIp.toIpPrefix()).matchIPDst(dstIp.toIpPrefix());

						TrafficTreatment treatment = DefaultTrafficTreatment.builder()
		                	.setOutput(portNumber)
							.build();

						ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
			                .withSelector(selectorBuilder.build())
			                .withTreatment(treatment)
			                .withPriority(10)
			                .withFlag(ForwardingObjective.Flag.VERSATILE)
							.makeTemporary(30)
			                .fromApp(appId)
							.add();

						flowObjectiveService.forward(path.get(i).deviceId(), forwardingObjective);

						log.info("Install flow rule on of:{}", path.get(i).deviceId());
						break;
					}
				}
				for(Link l: linkService.getDeviceEgressLinks(path.get(i).deviceId())){
					
					if(i != path.size() - 1 && l.dst().deviceId().equals(path.get(i + 1).deviceId())){
                        PortNumber portNumber = l.src().port();
                        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
                        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPSrc(srcIp.toIpPrefix()).matchIPDst(dstIp.toIpPrefix());

						TrafficTreatment treatment = DefaultTrafficTreatment.builder()
		                	.setOutput(portNumber)
							.build();

						ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
			                .withSelector(selectorBuilder.build())
			                .withTreatment(treatment)
			                .withPriority(10)
			                .withFlag(ForwardingObjective.Flag.VERSATILE)
							.makeTemporary(30)
			                .fromApp(appId)
							.add();

						flowObjectiveService.forward(l.src().deviceId(), forwardingObjective);

						log.info("Install flow rule on of:{}", path.get(i).deviceId());
					}					
				}
			}			
		}
    }
	
	
	public ArrayList<TopologyVertex> findpath(TopologyGraph graph, TopologyVertex source, TopologyVertex destination){
		
		ArrayList<TopologyVertex> visited = new ArrayList<TopologyVertex>();
		ArrayList<TopologyVertex> path = new ArrayList<TopologyVertex>();
		
		visited.add(source);
		path.add(source);
		return DFS(graph, path, visited, source, destination);
		
		
	}
	public ArrayList<TopologyVertex> DFS(TopologyGraph graph, ArrayList<TopologyVertex> path, ArrayList<TopologyVertex> visited, TopologyVertex source, TopologyVertex destination){
		
		if(path.contains(destination)){
			return path;
		}		
        TopologyVertex vertex = path.get(path.size()-1);
        
		for(TopologyEdge i: graph.getEdgesFrom(vertex)){
			
			if(!visited.contains(i.dst())){
			
				visited.add(i.dst());
				path.add(i.dst());
				
				path = DFS(graph, path, visited, source, destination);
				
				if(path.contains(destination)){
					return path;
				}	
					
				path.remove(i.dst());
			}
        }
        return path;
	}
	
}

