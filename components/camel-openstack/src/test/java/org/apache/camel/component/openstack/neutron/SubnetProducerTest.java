/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.openstack.neutron;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.camel.component.openstack.neutron.producer.SubnetProducer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.openstack4j.api.Builders;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.IPVersionType;
import org.openstack4j.model.network.Subnet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SubnetProducerTest extends NeutronProducerTestSupport {

    private Subnet dummySubnet;

    @Mock
    private Subnet testOSsubnet;

    @Before
    public void setUp() {
        producer = new SubnetProducer(endpoint, client);
        when(subnetService.create(any(Subnet.class))).thenReturn(testOSsubnet);
        when(subnetService.get(anyString())).thenReturn(testOSsubnet);

        List<Subnet> getAllList = new ArrayList<>();
        getAllList.add(testOSsubnet);
        getAllList.add(testOSsubnet);
        doReturn(getAllList).when(subnetService).list();

        dummySubnet = createSubnet();
        when(testOSsubnet.getName()).thenReturn(dummySubnet.getName());
        when(testOSsubnet.getNetworkId()).thenReturn(dummySubnet.getNetworkId());
        when(testOSsubnet.getId()).thenReturn(UUID.randomUUID().toString());
    }

    @Test
    public void createTest() throws Exception {
        msg.setHeader(NeutronConstants.OPERATION, NeutronConstants.CREATE);
        msg.setHeader(NeutronConstants.NAME, dummySubnet.getName());
        msg.setHeader(NeutronConstants.NETWORK_ID, dummySubnet.getNetworkId());
        msg.setHeader(NeutronConstants.IP_VERSION, IPVersionType.V4);

        producer.process(exchange);

        ArgumentCaptor<Subnet> captor = ArgumentCaptor.forClass(Subnet.class);
        verify(subnetService).create(captor.capture());

        assertEqualsSubnet(dummySubnet, captor.getValue());
        assertNotNull(msg.getBody(Subnet.class).getId());
    }

    @Test
    public void getTest() throws Exception {
        final String subnetID = "myNetID";
        msg.setHeader(NeutronConstants.OPERATION, NeutronConstants.GET);
        msg.setHeader(NeutronConstants.SUBNET_ID, subnetID);

        producer.process(exchange);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(subnetService).get(captor.capture());

        assertEquals(subnetID, captor.getValue());
        assertEqualsSubnet(testOSsubnet, msg.getBody(Subnet.class));
    }

    @Test
    public void getAllTest() throws Exception {
        msg.setHeader(NeutronConstants.OPERATION, NeutronConstants.GET_ALL);

        producer.process(exchange);

        final List<Subnet> result = msg.getBody(List.class);
        assertTrue(result.size() == 2);
        assertEquals(testOSsubnet, result.get(0));
    }

    @Test
    public void deleteTest() throws Exception {
        when(subnetService.delete(anyString())).thenReturn(ActionResponse.actionSuccess());
        final String subnetID = "myNetID";
        msg.setHeader(NeutronConstants.OPERATION, NeutronConstants.DELETE);
        msg.setHeader(NeutronConstants.ID, subnetID);

        producer.process(exchange);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(subnetService).delete(captor.capture());
        assertEquals(subnetID, captor.getValue());
        assertFalse(msg.isFault());

        //in case of failure
        final String failureMessage = "fail";
        when(subnetService.delete(anyString())).thenReturn(ActionResponse.actionFailed(failureMessage, 404));
        producer.process(exchange);
        assertTrue(msg.isFault());
        assertTrue(msg.getBody(String.class).contains(failureMessage));
    }

    private Subnet createSubnet() {
        return Builders.subnet()
                .name("name")
                .networkId("netId")
                .build();
    }

    private void assertEqualsSubnet(Subnet old, Subnet newSubnet) {
        assertEquals(old.getName(), newSubnet.getName());
        assertEquals(old.getNetworkId(), newSubnet.getNetworkId());
    }
}
