/*
 * Copyright 2020-present Open Networking Foundation
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
package nctu.winlab.unicastdhcp;

import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_ADDED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_UPDATED;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IPv4;

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

import java.util.Dictionary;
import java.util.Properties;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.ArrayDeque;

import static org.onlab.util.Tools.get;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
public class AppComponent {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final NameConfigListener cfgListener = new NameConfigListener();
    private final ConfigFactory factory =
        new ConfigFactory<ApplicationId, NameConfig>(
            APP_SUBJECT_FACTORY, NameConfig.class, "UnicastDhcpConfig") {
            @Override
            public NameConfig createConfig() {
            return new NameConfig();
            }
        };

    private ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry cfgService;

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


    private String DHCP_deviceId = new String("of:0000000000000000");
    private String DHCP_server_PORT = new String("2");    
    



    @Activate
    protected void activate() {
        appId = coreService.registerApplication("nctu.winlab.unicastdhcp");
        packetService.addProcessor(processor, PacketProcessor.director(2));
        cfgService.addListener(cfgListener);
        cfgService.registerConfigFactory(factory);
        log.info("Started");
        requestIntercepts();
    }

    @Deactivate
    protected void deactivate() {
        cfgService.removeListener(cfgListener);
        cfgService.unregisterConfigFactory(factory);
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

            MacAddress DHCP_Mac = MacAddress.valueOf("ea:e9:78:fb:fd:04");
            
            Topo_Path.clear();
            
            ConnectPoint cp = inPkt.receivedFrom();
            
            HostId srcId = HostId.hostId(srcMac);
            HostId dstId = HostId.hostId(dstMac);
            
            IpAddress DHCP_IpAdress = IpAddress.valueOf(IPv4.toIPv4Address("10.1.11.3"));
            MacAddress MacFlood = MacAddress.valueOf("FF:FF:FF:FF:FF:FF");
            IpAddress DHCP_zero = IpAddress.valueOf(IPv4.toIPv4Address("0.0.0.0"));
            IpAddress DHCP_flood = IpAddress.valueOf(IPv4.toIPv4Address("255.255.255.255"));
                        
            for(TopologyVertex v: graph.getVertexes()){
                for(Host h: hostService.getConnectedHosts(v.deviceId())){
                    if(h.mac().equals(srcMac)){
                        srcSwitch = v;
                    }    
                }
                if(v.deviceId().equals(DeviceId.deviceId(DHCP_deviceId))){
                    dstSwitch = v;
                    for(Host h: hostService.getConnectedHosts(v.deviceId())){                        
                        if(h.location().port().toString().equals(DHCP_server_PORT)){
                            DHCP_Mac = h.mac();
                            for(IpAddress p: h.ipAddresses()){
                                DHCP_IpAdress = p;
                            }
                        } 
                    }                     
                }
            }

            if((!(srcMac.toString().equals(DHCP_Mac.toString())))){   
                ArrayList<TopologyVertex> path = findpath(graph, srcSwitch, dstSwitch);	
                for(int i = path.size() - 1; i >= 0; i--){
                    if(i == path.size() - 1){
                        PortNumber portNumber = PortNumber.portNumber(DHCP_server_PORT); 
                        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
                        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchEthSrc(srcMac).matchEthDst(MacFlood).matchIPSrc(DHCP_zero.toIpPrefix()).matchIPDst(DHCP_flood.toIpPrefix());

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

                    }
                    for(Link l: linkService.getDeviceEgressLinks(path.get(i).deviceId())){                      
                        if(i != path.size() - 1 && l.dst().deviceId().equals(path.get(i + 1).deviceId())){
                            PortNumber portNumber = l.src().port();
                           
                            TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
                            selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchEthSrc(srcMac).matchEthDst(MacFlood).matchIPSrc(DHCP_zero.toIpPrefix()).matchIPDst(DHCP_flood.toIpPrefix());

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
                        }					
                    }
                }


                for(int i = 0; i < path.size(); i++){
                    if(i == 0){
                        for(Host h: hostService.getHostsByMac(srcMac)){
                            PortNumber portNumber = h.location().port(); 
                            TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
                            selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchEthSrc(DHCP_Mac).matchEthDst(srcMac).matchIPSrc(DHCP_IpAdress.toIpPrefix());

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
                            break;
                        }
                    }
                    for(Link l: linkService.getDeviceEgressLinks(path.get(i).deviceId())){                      
                        if(i != 0 && l.dst().deviceId().equals(path.get(i - 1).deviceId())){
                            PortNumber portNumber = l.src().port();
                            
                            TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
                            selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchEthSrc(DHCP_Mac).matchEthDst(srcMac).matchIPSrc(DHCP_IpAdress.toIpPrefix());

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
                        }					
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

    private class NameConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            if ((event.type() == CONFIG_ADDED || event.type() == CONFIG_UPDATED)
                && event.configClass().equals(NameConfig.class)) {
                NameConfig config = cfgService.getConfig(appId, NameConfig.class);
                if (config != null) {
                    log.info("DHCP server is at {}", config.name());
                    DHCP_deviceId = config.name().substring(0, 19);
                    DHCP_server_PORT = config.name().substring(config.name().length() - 1);                  
                }
            }

        }
    }

}
