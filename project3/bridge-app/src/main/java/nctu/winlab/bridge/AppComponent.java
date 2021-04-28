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
package nctu.winlab.bridge;

import com.google.common.collect.Maps;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Host;
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

import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



import java.util.Map;
import java.util.Optional;


/**
 * Tutorial class used to help build a basic onos learning switch application.
 * This class contains the solution to the learning switch tutorial.  Change "enabled = false"
 * to "enabled = true" below, to run the solution.
 */
@Component(immediate = true, enabled = true)
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

    private PacketProcessor processor = new ReactivePacketProcessor();
	protected Map<DeviceId, Map<MacAddress, PortNumber>> macTables = Maps.newConcurrentMap();
	
    private ApplicationId appId;
	
	@Activate
    protected void activate() {
		
        appId = coreService.registerApplication("nctu.winlab.bridge");
		
		
        packetService.addProcessor(processor, PacketProcessor.director(2));
		requestIntercepts();
		
        log.info("Started");
    }
	
	@Deactivate
	protected void deactivate() {
        packetService.removeProcessor(processor);
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
			ConnectPoint cp = context.inPacket().receivedFrom();
			macTables.putIfAbsent(cp.deviceId(), Maps.newConcurrentMap());
			installRule(context);
		}
    }
	
	private void installRule(PacketContext context){
        Ethernet inPkt = context.inPacket().parsed();
		TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
		
		if (inPkt.getEtherType() != Ethernet.TYPE_ARP && inPkt.getEtherType() != Ethernet.TYPE_IPV4) {
                return;
        }
		
		ConnectPoint cp = context.inPacket().receivedFrom();
        Map<MacAddress, PortNumber> macTable = macTables.get(cp.deviceId());
		MacAddress srcMac = inPkt.getSourceMAC();
		MacAddress dstMac = inPkt.getDestinationMAC();
        macTable.put(srcMac, cp.port());
        macTables.put(cp.deviceId(),macTable);
        PortNumber outport = macTable.get(dstMac);
        // log.info("-------------------------------------");
        // log.info(cp.deviceId().toString());
        // if(outport != null){
        //     log.info(outport.toString());
        // }else{
        //     log.info("outport can't find");
        // }
		// log.info("-------------------------------------");      
		
        if (outport != null) {
			selectorBuilder.matchEthSrc(srcMac).matchEthDst(dstMac);

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(outport)
                    .build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .withSelector(selectorBuilder.build())
                    .withTreatment(treatment)
                    .withPriority(10)
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .fromApp(appId)
                    .makeTemporary(10)
                    .add();

            flowObjectiveService.forward(context.inPacket().receivedFrom().deviceId(), forwardingObjective);
            packetOut(context, outport);
		
	    }else{
            flood(context);
        }
    }
    
    private void flood(PacketContext context) {
        packetOut(context, PortNumber.FLOOD);
    }
    private void packetOut(PacketContext context, PortNumber portNumber) {
        context.treatmentBuilder().setOutput(portNumber);
		context.send();
	}
}

