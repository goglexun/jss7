/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.protocols.ss7.m3ua.impl;


import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javolution.util.FastMap;

import org.mobicents.protocols.api.Association;
import org.mobicents.protocols.api.AssociationListener;
import org.mobicents.protocols.api.AssociationType;
import org.mobicents.protocols.api.IpChannelType;
import org.mobicents.protocols.api.Management;
import org.mobicents.protocols.api.ManagementEventListener;
import org.mobicents.protocols.api.PayloadData;
import org.mobicents.protocols.api.Server;
import org.mobicents.protocols.api.ServerListener;
import org.mobicents.protocols.ss7.m3ua.ExchangeType;
import org.mobicents.protocols.ss7.m3ua.Functionality;
import org.mobicents.protocols.ss7.m3ua.impl.fsm.FSM;
import org.mobicents.protocols.ss7.m3ua.impl.message.M3UAMessageImpl;
import org.mobicents.protocols.ss7.m3ua.impl.message.MessageFactoryImpl;
import org.mobicents.protocols.ss7.m3ua.impl.message.transfer.PayloadDataImpl;
import org.mobicents.protocols.ss7.m3ua.impl.parameter.ParameterFactoryImpl;
import org.mobicents.protocols.ss7.m3ua.impl.parameter.ProtocolDataImpl;
import org.mobicents.protocols.ss7.m3ua.message.M3UAMessage;
import org.mobicents.protocols.ss7.m3ua.message.MessageClass;
import org.mobicents.protocols.ss7.m3ua.message.MessageType;
import org.mobicents.protocols.ss7.m3ua.message.asptm.ASPActiveAck;
import org.mobicents.protocols.ss7.m3ua.message.mgmt.Notify;
import org.mobicents.protocols.ss7.m3ua.parameter.RoutingContext;
import org.mobicents.protocols.ss7.m3ua.parameter.Status;
import org.mobicents.protocols.ss7.m3ua.parameter.TrafficModeType;
import org.mobicents.protocols.ss7.mtp.Mtp3PausePrimitive;
import org.mobicents.protocols.ss7.mtp.Mtp3Primitive;
import org.mobicents.protocols.ss7.mtp.Mtp3ResumePrimitive;
import org.mobicents.protocols.ss7.mtp.Mtp3StatusPrimitive;
import org.mobicents.protocols.ss7.mtp.Mtp3TransferPrimitive;
import org.mobicents.protocols.ss7.mtp.Mtp3UserPartListener;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests the FSM of client side AS and ASP's
 * 
 * @author amit bhayani
 * 
 */
public class RemSgFSMTest {

	private ParameterFactoryImpl parmFactory = new ParameterFactoryImpl();
	private MessageFactoryImpl messageFactory = new MessageFactoryImpl();
	private M3UAManagement clientM3UAMgmt = null;
	private Mtp3UserPartListenerimpl mtp3UserPartListener = null;
	
	private Semaphore semaphore = null;

	private TransportManagement transportManagement = null;

	public RemSgFSMTest() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@BeforeMethod
	public void setUp() throws Exception {
		semaphore = new Semaphore(0);
		this.transportManagement = new TransportManagement();
		this.clientM3UAMgmt = new M3UAManagement("RemSgFSMTest");
		this.clientM3UAMgmt.setTransportManagement(this.transportManagement);
		this.mtp3UserPartListener = new Mtp3UserPartListenerimpl();
		this.clientM3UAMgmt.addMtp3UserPartListener(this.mtp3UserPartListener);
		this.clientM3UAMgmt.start();

	}

	@AfterMethod
	public void tearDown() throws Exception {
		clientM3UAMgmt.getAppServers().clear();
		clientM3UAMgmt.getAspfactories().clear();
		clientM3UAMgmt.getRoute().clear();
		clientM3UAMgmt.stop();
	}

	private AspState getAspState(FSM fsm) {
		return AspState.getState(fsm.getState().getName());
	}

	private AsState getAsState(FSM fsm) {
		return AsState.getState(fsm.getState().getName());
	}

	/**
	 * Test with RC Set
	 * @throws Exception
	 */
	@Test
	public void testSingleAspInAsWithRC() throws Exception {

		// 5.1.1. Single ASP in an Application Server ("1+0" sparing),
		this.transportManagement.addAssociation(null, 0, null, 0, "testAssoc1");

		RoutingContext rc = parmFactory.createRoutingContext(new long[] { 100 });

		// As as = rsgw.createAppServer("testas", rc, rKey, trModType);
		As as = this.clientM3UAMgmt.createAs("testas", Functionality.AS, ExchangeType.SE, null, rc, null, null);

		AspFactory localAspFactory = this.clientM3UAMgmt.createAspFactory("testasp", "testAssoc1");
		localAspFactory.start();

		Asp asp = clientM3UAMgmt.assignAspToAs("testas", "testasp");
		
		// Create Route. Adding 3 routes
		this.clientM3UAMgmt.addRoute(3, -1, -1, "testas");
		this.clientM3UAMgmt.addRoute(2, 10, -1, "testas");
		this.clientM3UAMgmt.addRoute(2, -1, -1, "testas");

		// Signal for Communication UP
		TestAssociation testAssociation = (TestAssociation) this.transportManagement.getAssociation("testAssoc1");
		testAssociation.signalCommUp();

		// Once comunication is UP, ASP_UP should have been sent.
		FSM aspLocalFSM = asp.getLocalFSM();
		assertEquals(AspState.UP_SENT, this.getAspState(aspLocalFSM));
		assertTrue(validateMessage(testAssociation, MessageClass.ASP_STATE_MAINTENANCE, MessageType.ASP_UP, -1, -1));

		// The other side will send ASP_UP_ACK and after that NTFY(AS-INACTIVE)
		M3UAMessageImpl message = messageFactory.createMessage(MessageClass.ASP_STATE_MAINTENANCE,
				MessageType.ASP_UP_ACK);
		localAspFactory.read(message);

		Notify notify = (Notify) messageFactory.createMessage(MessageClass.MANAGEMENT, MessageType.NOTIFY);
		notify.setRoutingContext(rc);
		Status status = parmFactory.createStatus(Status.STATUS_AS_State_Change, Status.INFO_AS_INACTIVE);
		notify.setStatus(status);
		localAspFactory.read(notify);

		assertEquals(AspState.ACTIVE_SENT, this.getAspState(aspLocalFSM));
		assertTrue(validateMessage(testAssociation, MessageClass.ASP_TRAFFIC_MAINTENANCE, MessageType.ASP_ACTIVE, -1,
				-1));

		FSM asPeerFSM = as.getPeerFSM();
		// also the AS should be INACTIVE now
		assertEquals(AsState.INACTIVE, this.getAsState(asPeerFSM));

		// The other side will send ASP_ACTIVE_ACK and after that
		// NTFY(AS-ACTIVE)
		ASPActiveAck aspActiveAck = (ASPActiveAck) messageFactory.createMessage(MessageClass.ASP_TRAFFIC_MAINTENANCE,
				MessageType.ASP_ACTIVE_ACK);
		aspActiveAck.setRoutingContext(rc);
		localAspFactory.read(aspActiveAck);

		notify = (Notify) messageFactory.createMessage(MessageClass.MANAGEMENT, MessageType.NOTIFY);
		notify.setRoutingContext(rc);
		status = parmFactory.createStatus(Status.STATUS_AS_State_Change, Status.INFO_AS_ACTIVE);
		notify.setStatus(status);
		localAspFactory.read(notify);

		assertEquals(AspState.ACTIVE, this.getAspState(aspLocalFSM));
		// also the AS should be ACTIVE now
		assertEquals(AsState.ACTIVE, this.getAsState(asPeerFSM));
		
		// Check if MTP3 RESUME received
		//lets wait for 2second to receive the MTP3 primitive before giving up 
		semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);
		//RESUME for DPC 3
		Mtp3Primitive mtp3Primitive = this.mtp3UserPartListener.rxMtp3PrimitivePoll();
		assertNotNull(mtp3Primitive);
		assertEquals(Mtp3Primitive.RESUME, mtp3Primitive.getType());
		assertEquals(3, mtp3Primitive.getAffectedDpc());
		
		//lets wait for 2second to receive the MTP3 primitive before giving up 
		semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);
		//RESUME for DPC 2
		mtp3Primitive = this.mtp3UserPartListener.rxMtp3PrimitivePoll();
		assertNotNull(mtp3Primitive);
		assertEquals(Mtp3Primitive.RESUME, mtp3Primitive.getType());
		assertEquals(2, mtp3Primitive.getAffectedDpc());
		
		//No more MTP3 Primitive or message
		assertNull(this.mtp3UserPartListener.rxMtp3PrimitivePoll());
		assertNull(this.mtp3UserPartListener.rxMtp3TransferPrimitivePoll());


		// Lets stop ASP Factory
		localAspFactory.stop();

		assertEquals(AspState.DOWN_SENT, this.getAspState(aspLocalFSM));
		assertTrue(validateMessage(testAssociation, MessageClass.ASP_STATE_MAINTENANCE, MessageType.ASP_DOWN, -1, -1));
		// also the AS is PENDING
		assertEquals(AsState.PENDING, this.getAsState(asPeerFSM));
		
		//lets wait for 3 seconds to receive the MTP3 primitive before giving up. We know Pending timeout is 2 secs
		semaphore.tryAcquire(3000, TimeUnit.MILLISECONDS);
		//PAUSE for DPC 3
		mtp3Primitive = this.mtp3UserPartListener.rxMtp3PrimitivePoll();
		assertNotNull(mtp3Primitive);
		assertEquals(Mtp3Primitive.PAUSE, mtp3Primitive.getType());
		assertEquals(3, mtp3Primitive.getAffectedDpc());
		
		//lets wait for 3 seconds to receive the MTP3 primitive before giving up. We know Pending timeout is 2 secs
		semaphore.tryAcquire(3000, TimeUnit.MILLISECONDS);
		//PAUSE for DPC 2
		mtp3Primitive = this.mtp3UserPartListener.rxMtp3PrimitivePoll();
		assertNotNull(mtp3Primitive);
		assertEquals(Mtp3Primitive.PAUSE, mtp3Primitive.getType());
		assertEquals(2, mtp3Primitive.getAffectedDpc());
		
		//No more MTP3 Primitive or message
		assertNull(this.mtp3UserPartListener.rxMtp3PrimitivePoll());
		assertNull(this.mtp3UserPartListener.rxMtp3TransferPrimitivePoll());

		// Make sure we don't have any more
		assertNull(testAssociation.txPoll());

	}
	
	/**
	 * Test with RC Set
	 * @throws Exception
	 */
	@Test
	public void testSingleAspInAsWithoutRC() throws Exception {

		// 5.1.1. Single ASP in an Application Server ("1+0" sparing),
		this.transportManagement.addAssociation(null, 0, null, 0, "testAssoc1");

		// As as = rsgw.createAppServer("testas", rc, rKey, trModType);
		As as = this.clientM3UAMgmt.createAs("testas", Functionality.AS, ExchangeType.SE, null, null, null, null);

		AspFactory localAspFactory = this.clientM3UAMgmt.createAspFactory("testasp", "testAssoc1");
		localAspFactory.start();

		Asp asp = clientM3UAMgmt.assignAspToAs("testas", "testasp");
		
		// Create Route
		this.clientM3UAMgmt.addRoute(2, -1, -1, "testas");

		// Signal for Communication UP
		TestAssociation testAssociation = (TestAssociation) this.transportManagement.getAssociation("testAssoc1");
		testAssociation.signalCommUp();

		// Once comunication is UP, ASP_UP should have been sent.
		FSM aspLocalFSM = asp.getLocalFSM();
		assertEquals(AspState.UP_SENT, this.getAspState(aspLocalFSM));
		assertTrue(validateMessage(testAssociation, MessageClass.ASP_STATE_MAINTENANCE, MessageType.ASP_UP, -1, -1));

		// The other side will send ASP_UP_ACK and after that NTFY(AS-INACTIVE)
		M3UAMessageImpl message = messageFactory.createMessage(MessageClass.ASP_STATE_MAINTENANCE,
				MessageType.ASP_UP_ACK);
		localAspFactory.read(message);

		Notify notify = (Notify) messageFactory.createMessage(MessageClass.MANAGEMENT, MessageType.NOTIFY);

		Status status = parmFactory.createStatus(Status.STATUS_AS_State_Change, Status.INFO_AS_INACTIVE);
		notify.setStatus(status);
		localAspFactory.read(notify);

		assertEquals(AspState.ACTIVE_SENT, this.getAspState(aspLocalFSM));
		assertTrue(validateMessage(testAssociation, MessageClass.ASP_TRAFFIC_MAINTENANCE, MessageType.ASP_ACTIVE, -1,
				-1));

		FSM asPeerFSM = as.getPeerFSM();
		// also the AS should be INACTIVE now
		assertEquals(AsState.INACTIVE, this.getAsState(asPeerFSM));

		// The other side will send ASP_ACTIVE_ACK and after that
		// NTFY(AS-ACTIVE)
		ASPActiveAck aspActiveAck = (ASPActiveAck) messageFactory.createMessage(MessageClass.ASP_TRAFFIC_MAINTENANCE,
				MessageType.ASP_ACTIVE_ACK);

		localAspFactory.read(aspActiveAck);

		notify = (Notify) messageFactory.createMessage(MessageClass.MANAGEMENT, MessageType.NOTIFY);

		status = parmFactory.createStatus(Status.STATUS_AS_State_Change, Status.INFO_AS_ACTIVE);
		notify.setStatus(status);
		localAspFactory.read(notify);

		assertEquals(AspState.ACTIVE, this.getAspState(aspLocalFSM));
		// also the AS should be ACTIVE now
		assertEquals(AsState.ACTIVE, this.getAsState(asPeerFSM));
		
		// Check if MTP3 RESUME received
		//lets wait for 2second to receive the MTP3 primitive before giving up 
		semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);
		
		Mtp3Primitive mtp3Primitive = this.mtp3UserPartListener.rxMtp3PrimitivePoll();
		assertNotNull(mtp3Primitive);
		assertEquals(Mtp3Primitive.RESUME, mtp3Primitive.getType());
		assertEquals(2, mtp3Primitive.getAffectedDpc());
		//No more MTP3 Primitive or message
		assertNull(this.mtp3UserPartListener.rxMtp3PrimitivePoll());
		assertNull(this.mtp3UserPartListener.rxMtp3TransferPrimitivePoll());
		
		//Since we didn't set the Traffic Mode while creating AS, it should now be set to loadshare as default
		assertEquals(TrafficModeType.Loadshare, as.getTrafficModeType().getMode());

		// Lets stop ASP Factory
		localAspFactory.stop();

		assertEquals(AspState.DOWN_SENT, this.getAspState(aspLocalFSM));
		assertTrue(validateMessage(testAssociation, MessageClass.ASP_STATE_MAINTENANCE, MessageType.ASP_DOWN, -1, -1));
		// also the AS is PENDING
		assertEquals(AsState.PENDING, this.getAsState(asPeerFSM));
		
		//lets wait for 3 seconds to receive the MTP3 primitive before giving up. We know Pending timeout is 2 secs
		semaphore.tryAcquire(3000, TimeUnit.MILLISECONDS);
		
		mtp3Primitive = this.mtp3UserPartListener.rxMtp3PrimitivePoll();
		assertNotNull(mtp3Primitive);
		assertEquals(Mtp3Primitive.PAUSE, mtp3Primitive.getType());
		assertEquals(2, mtp3Primitive.getAffectedDpc());
		//No more MTP3 Primitive or message
		assertNull(this.mtp3UserPartListener.rxMtp3PrimitivePoll());
		assertNull(this.mtp3UserPartListener.rxMtp3TransferPrimitivePoll());

		// Make sure we don't have any more
		assertNull(testAssociation.txPoll());

	}

	@Test
	public void testSingleAspInMultipleAs() throws Exception {
		// 5.1.1.3. Single ASP in Multiple Application Servers (Each with "1+0"
		// Sparing)
		this.transportManagement.addAssociation(null, 0, null, 0, "testAssoc1");

		// Define 1st AS
		RoutingContext rc1 = parmFactory.createRoutingContext(new long[] { 100 });

		As remAs1 = this.clientM3UAMgmt.createAs("testas1", Functionality.AS, ExchangeType.SE, null, rc1, null, null);

		// Define 2nd AS
		RoutingContext rc2 = parmFactory.createRoutingContext(new long[] { 200 });

		As remAs2 = clientM3UAMgmt.createAs("testas2", Functionality.AS, ExchangeType.SE, null, rc2, null, null);

		AspFactory aspFactory = clientM3UAMgmt.createAspFactory("testasp", "testAssoc1");
		aspFactory.start();

		// Both ASP uses same underlying M3UAChannel
		Asp remAsp1 = clientM3UAMgmt.assignAspToAs("testas1", "testasp");
		Asp remAsp2 = clientM3UAMgmt.assignAspToAs("testas2", "testasp");
		
		// Create Route
		this.clientM3UAMgmt.addRoute(2, -1, -1, "testas1");
		this.clientM3UAMgmt.addRoute(2, -1, -1, "testas2");
		
		this.clientM3UAMgmt.addRoute(3, -1, -1, "testas1");
		this.clientM3UAMgmt.addRoute(3, -1, -1, "testas2");

		// Signal for Communication UP
		TestAssociation testAssociation = (TestAssociation) this.transportManagement.getAssociation("testAssoc1");
		testAssociation.signalCommUp();

		FSM asp1LocalFSM = remAsp1.getLocalFSM();
		FSM asp2LocalFSM = remAsp2.getLocalFSM();

		assertNull(remAsp1.getPeerFSM());
		assertNull(remAsp2.getPeerFSM());

		assertEquals(AspState.UP_SENT, this.getAspState(asp1LocalFSM));
		assertEquals(AspState.UP_SENT, this.getAspState(asp2LocalFSM));
		// Once communication is UP, ASP_UP should have been sent.
		assertTrue(validateMessage(testAssociation, MessageClass.ASP_STATE_MAINTENANCE, MessageType.ASP_UP, -1, -1));

		FSM as1PeerFSM = remAs1.getPeerFSM();
		FSM as2PeerFSM = remAs2.getPeerFSM();

		assertNull(remAs1.getLocalFSM());
		assertNull(remAs2.getLocalFSM());

		// Both the AS is still DOWN
		assertEquals(AsState.DOWN, this.getAsState(as1PeerFSM));
		assertEquals(AsState.DOWN, this.getAsState(as2PeerFSM));

		// The other side will send ASP_UP_ACK and after that NTFY(AS-INACTIVE)
		// for both the AS
		M3UAMessageImpl message = messageFactory.createMessage(MessageClass.ASP_STATE_MAINTENANCE,
				MessageType.ASP_UP_ACK);
		aspFactory.read(message);
		assertEquals(AspState.ACTIVE_SENT, this.getAspState(asp1LocalFSM));
		assertEquals(AspState.ACTIVE_SENT, this.getAspState(asp2LocalFSM));

		// ASP_ACTIVE for both ASP in txQueue
		assertTrue(validateMessage(testAssociation, MessageClass.ASP_TRAFFIC_MAINTENANCE, MessageType.ASP_ACTIVE, -1,
				-1));
		assertTrue(validateMessage(testAssociation, MessageClass.ASP_TRAFFIC_MAINTENANCE, MessageType.ASP_ACTIVE, -1,
				-1));

		Notify notify = (Notify) messageFactory.createMessage(MessageClass.MANAGEMENT, MessageType.NOTIFY);
		notify.setRoutingContext(rc1);
		Status status = parmFactory.createStatus(Status.STATUS_AS_State_Change, Status.INFO_AS_INACTIVE);
		notify.setStatus(status);
		aspFactory.read(notify);
		// the AS1 should be INACTIVE now but AS2 still DOWN
		assertEquals(AsState.INACTIVE, this.getAsState(as1PeerFSM));
		assertEquals(AsState.DOWN, this.getAsState(as2PeerFSM));

		notify = (Notify) messageFactory.createMessage(MessageClass.MANAGEMENT, MessageType.NOTIFY);
		notify.setRoutingContext(rc2);// RC 200
		status = parmFactory.createStatus(Status.STATUS_AS_State_Change, Status.INFO_AS_INACTIVE);
		notify.setStatus(status);
		aspFactory.read(notify);
		// AS2 should be INACTIVE now
		assertEquals(AsState.INACTIVE, this.getAsState(as2PeerFSM));

		// The other side will send ASP_ACTIVE_ACK and after that
		// NTFY(AS-ACTIVE)
		ASPActiveAck aspActiveAck = (ASPActiveAck) messageFactory.createMessage(MessageClass.ASP_TRAFFIC_MAINTENANCE,
				MessageType.ASP_ACTIVE_ACK);
		aspActiveAck.setRoutingContext(this.parmFactory.createRoutingContext(new long[] { 100, 200 }));
		aspFactory.read(aspActiveAck);

		// Both ASP are ACTIVE now
		assertEquals(AspState.ACTIVE, this.getAspState(asp1LocalFSM));
		assertEquals(AspState.ACTIVE, this.getAspState(asp2LocalFSM));

		notify = (Notify) messageFactory.createMessage(MessageClass.MANAGEMENT, MessageType.NOTIFY);
		notify.setRoutingContext(rc1);
		status = parmFactory.createStatus(Status.STATUS_AS_State_Change, Status.INFO_AS_ACTIVE);
		notify.setStatus(status);
		aspFactory.read(notify);
		// the AS1 should be ACTIVE now but AS2 still INACTIVE
		assertEquals(AsState.ACTIVE, this.getAsState(as1PeerFSM));
		assertEquals(AsState.INACTIVE, this.getAsState(as2PeerFSM));
		
		// Check if MTP3 RESUME received
		//lets wait for 2second to receive the MTP3 primitive before giving up 
		semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);
		//RESUME for DPC 2
		Mtp3Primitive mtp3Primitive = this.mtp3UserPartListener.rxMtp3PrimitivePoll();
		assertNotNull(mtp3Primitive);
		assertEquals(Mtp3Primitive.RESUME, mtp3Primitive.getType());
		assertEquals(2, mtp3Primitive.getAffectedDpc());
		
		//lets wait for 2second to receive the MTP3 primitive before giving up 
		semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);
		//RESUME for DPC 3
		mtp3Primitive = this.mtp3UserPartListener.rxMtp3PrimitivePoll();
		assertNotNull(mtp3Primitive);
		assertEquals(Mtp3Primitive.RESUME, mtp3Primitive.getType());
		assertEquals(3, mtp3Primitive.getAffectedDpc());
		
		//No more MTP3 Primitive or message
		assertNull(this.mtp3UserPartListener.rxMtp3PrimitivePoll());
		assertNull(this.mtp3UserPartListener.rxMtp3TransferPrimitivePoll());
		
		//Check the TrafficMode for AS1
		assertEquals(TrafficModeType.Loadshare, remAs1.getTrafficModeType().getMode());

		notify = (Notify) messageFactory.createMessage(MessageClass.MANAGEMENT, MessageType.NOTIFY);
		notify.setRoutingContext(rc2);
		status = parmFactory.createStatus(Status.STATUS_AS_State_Change, Status.INFO_AS_ACTIVE);
		notify.setStatus(status);
		aspFactory.read(notify);
		// the AS2 is also ACTIVE now
		assertEquals(AsState.ACTIVE, this.getAsState(as1PeerFSM));
		assertEquals(AsState.ACTIVE, this.getAsState(as2PeerFSM));
		
		//But RESUME shouldn't have been fired
		assertNull(this.mtp3UserPartListener.rxMtp3PrimitivePoll());
		assertNull(this.mtp3UserPartListener.rxMtp3TransferPrimitivePoll());
		
		//Check the TrafficMode for AS2
		assertEquals(TrafficModeType.Loadshare, remAs2.getTrafficModeType().getMode());

		// Lets stop ASP Factory
		aspFactory.stop();

		assertEquals(AspState.DOWN_SENT, this.getAspState(asp1LocalFSM));
		assertEquals(AspState.DOWN_SENT, this.getAspState(asp2LocalFSM));
		assertTrue(validateMessage(testAssociation, MessageClass.ASP_STATE_MAINTENANCE, MessageType.ASP_DOWN, -1, -1));
		// also the both AS is PENDING
		assertEquals(AsState.PENDING, this.getAsState(as1PeerFSM));
		assertEquals(AsState.PENDING, this.getAsState(as2PeerFSM));
		
		
		//lets wait for 3 seconds to receive the MTP3 primitive before giving up. We know Pending timeout is 2 secs
		semaphore.tryAcquire(3000, TimeUnit.MILLISECONDS);
		//PAUSE for DPC 2
		mtp3Primitive = this.mtp3UserPartListener.rxMtp3PrimitivePoll();
		assertNotNull(mtp3Primitive);
		assertEquals(Mtp3Primitive.PAUSE, mtp3Primitive.getType());
		assertEquals(2, mtp3Primitive.getAffectedDpc());
		
		//lets wait for 3 seconds to receive the MTP3 primitive before giving up. We know Pending timeout is 2 secs
		semaphore.tryAcquire(3000, TimeUnit.MILLISECONDS);
		//PAUSE for DPC 3
		mtp3Primitive = this.mtp3UserPartListener.rxMtp3PrimitivePoll();
		assertNotNull(mtp3Primitive);
		assertEquals(Mtp3Primitive.PAUSE, mtp3Primitive.getType());
		assertEquals(3, mtp3Primitive.getAffectedDpc());
		
		//No more MTP3 Primitive or message
		assertNull(this.mtp3UserPartListener.rxMtp3PrimitivePoll());
		assertNull(this.mtp3UserPartListener.rxMtp3TransferPrimitivePoll());		

		// Make sure we don't have any more
		assertNull(testAssociation.txPoll());
	}

	@Test
	public void testTwoAspInAsOverride() throws Exception {
		// 5.1.2. Two ASPs in Application Server ("1+1" Sparing)

		TestAssociation testAssociation1 = (TestAssociation) this.transportManagement.addAssociation(null, 0, null, 0,
				"testAssoc1");
		TestAssociation testAssociation2 = (TestAssociation) this.transportManagement.addAssociation(null, 0, null, 0,
				"testAssoc2");

		RoutingContext rc = parmFactory.createRoutingContext(new long[] { 100 });

		TrafficModeType trModType = parmFactory.createTrafficModeType(TrafficModeType.Override);

		// As remAs = rsgw.createAppServer("testas", rc, rKey, trModType);
		As remAs = this.clientM3UAMgmt.createAs("testas", Functionality.AS, ExchangeType.SE, null, rc, null, null);

		AspFactory aspFactory1 = this.clientM3UAMgmt.createAspFactory("testasp1", "testAssoc1");
		aspFactory1.start();

		// AspFactory aspFactory2 = rsgw.createAspFactory("testasp2",
		// "127.0.0.1", 2777, "127.0.0.1", 2778);
		AspFactory aspFactory2 = this.clientM3UAMgmt.createAspFactory("testasp2", "testAssoc2");
		aspFactory2.start();

		Asp remAsp1 = clientM3UAMgmt.assignAspToAs("testas", "testasp1");
		Asp remAsp2 = clientM3UAMgmt.assignAspToAs("testas", "testasp2");
		
		// Create Route
		this.clientM3UAMgmt.addRoute(2, -1, -1, "testas");

		FSM asp1LocalFSM = remAsp1.getLocalFSM();
		FSM asp2LocalFSM = remAsp2.getLocalFSM();

		assertNull(remAsp1.getPeerFSM());
		assertNull(remAsp2.getPeerFSM());

		// Check for Communication UP for ASP1
		testAssociation1.signalCommUp();
		assertEquals(AspState.UP_SENT, this.getAspState(asp1LocalFSM));
		// ASP_UP should have been sent.
		assertTrue(validateMessage(testAssociation1, MessageClass.ASP_STATE_MAINTENANCE, MessageType.ASP_UP, -1, -1));
		// But AS is still Down
		FSM asPeerFSM = remAs.getPeerFSM();

		assertEquals(AsState.DOWN, this.getAsState(asPeerFSM));

		// Far end send ASP_UP_ACK and NTFY
		M3UAMessageImpl message = messageFactory.createMessage(MessageClass.ASP_STATE_MAINTENANCE,
				MessageType.ASP_UP_ACK);
		aspFactory1.read(message);
		assertEquals(AspState.ACTIVE_SENT, this.getAspState(asp1LocalFSM));
		assertTrue(validateMessage(testAssociation1, MessageClass.ASP_TRAFFIC_MAINTENANCE, MessageType.ASP_ACTIVE, -1,
				-1));

		Notify notify = (Notify) messageFactory.createMessage(MessageClass.MANAGEMENT, MessageType.NOTIFY);
		notify.setRoutingContext(rc);
		Status status = parmFactory.createStatus(Status.STATUS_AS_State_Change, Status.INFO_AS_INACTIVE);
		notify.setStatus(status);
		aspFactory1.read(notify);
		// the AS1 should be INACTIVE
		assertEquals(AsState.INACTIVE, this.getAsState(asPeerFSM));

		// The other side will send ASP_ACTIVE_ACK and after that
		// NTFY(AS-ACTIVE)
		ASPActiveAck aspActiveAck = (ASPActiveAck) messageFactory.createMessage(MessageClass.ASP_TRAFFIC_MAINTENANCE,
				MessageType.ASP_ACTIVE_ACK);
		aspActiveAck.setRoutingContext(rc);
		aspActiveAck.setTrafficModeType(trModType);
		aspFactory1.read(aspActiveAck);

		assertEquals(AspState.ACTIVE, this.getAspState(asp1LocalFSM));

		notify = (Notify) messageFactory.createMessage(MessageClass.MANAGEMENT, MessageType.NOTIFY);
		notify.setRoutingContext(rc);
		status = parmFactory.createStatus(Status.STATUS_AS_State_Change, Status.INFO_AS_ACTIVE);
		notify.setStatus(status);
		aspFactory1.read(notify);
		aspFactory2.read(notify);

		assertEquals(AsState.ACTIVE, this.getAsState(asPeerFSM));
		
		// Check if MTP3 RESUME received
		//lets wait for 2second to receive the MTP3 primitive before giving up 
		semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);
		
		Mtp3Primitive mtp3Primitive = this.mtp3UserPartListener.rxMtp3PrimitivePoll();
		assertNotNull(mtp3Primitive);
		assertEquals(Mtp3Primitive.RESUME, mtp3Primitive.getType());
		assertEquals(2, mtp3Primitive.getAffectedDpc());
		//No more MTP3 Primitive or message
		assertNull(this.mtp3UserPartListener.rxMtp3PrimitivePoll());
		assertNull(this.mtp3UserPartListener.rxMtp3TransferPrimitivePoll());		

		// Communication UP for ASP2
		testAssociation2.signalCommUp();
		assertEquals(AspState.UP_SENT, this.getAspState(asp2LocalFSM));
		// ASP_UP should have been sent.
		assertTrue(validateMessage(testAssociation2, MessageClass.ASP_STATE_MAINTENANCE, MessageType.ASP_UP, -1, -1));
		// Far end send ASP_UP_ACK
		message = messageFactory.createMessage(MessageClass.ASP_STATE_MAINTENANCE, MessageType.ASP_UP_ACK);
		aspFactory2.read(message);
		// ASP2 now is INACTIVE as ASP1 is still ACTIVATING
		assertEquals(AspState.INACTIVE, this.getAspState(asp2LocalFSM));

		// Bring down ASP1
		// 5.2.1. 1+1 Sparing, Withdrawal of ASP, Backup Override
		testAssociation1.signalCommLost();
		// the ASP is DOWN and AS goes in PENDING STATE
		assertEquals(AspState.DOWN, this.getAspState(asp1LocalFSM));
		assertEquals(AsState.PENDING, this.getAsState(asPeerFSM));

		// Aslo the ASP_ACTIVE for ASP2 should have been sent
		assertEquals(AspState.ACTIVE_SENT, this.getAspState(asp2LocalFSM));
		assertTrue(validateMessage(testAssociation2, MessageClass.ASP_TRAFFIC_MAINTENANCE, MessageType.ASP_ACTIVE, -1,
				-1));

		// We will not get Alternate ASP Active as this ASP's channel is dead
		// The other side will send ASP_ACTIVE_ACK and after that
		// NTFY(AS-ACTIVE)
		aspActiveAck = (ASPActiveAck) messageFactory.createMessage(MessageClass.ASP_TRAFFIC_MAINTENANCE,
				MessageType.ASP_ACTIVE_ACK);
		aspActiveAck.setRoutingContext(rc);
		aspFactory2.read(aspActiveAck);
		assertEquals(AspState.ACTIVE, this.getAspState(asp2LocalFSM));

		// We should get Notify that AS is ACTIVE
		notify = (Notify) messageFactory.createMessage(MessageClass.MANAGEMENT, MessageType.NOTIFY);
		notify.setRoutingContext(rc);
		status = parmFactory.createStatus(Status.STATUS_AS_State_Change, Status.INFO_AS_ACTIVE);
		notify.setStatus(status);
		aspFactory2.read(notify);

		assertEquals(AsState.ACTIVE, this.getAsState(asPeerFSM));

		assertNull(testAssociation1.txPoll());
		assertNull(testAssociation2.txPoll());
		
		//No more MTP3 Primitive or message
		assertNull(this.mtp3UserPartListener.rxMtp3PrimitivePoll());
		assertNull(this.mtp3UserPartListener.rxMtp3TransferPrimitivePoll());	

	}
	
	@Test
	public void testPendingQueue() throws Exception {

		TestAssociation testAssociation = (TestAssociation) this.transportManagement.addAssociation(null, 0, null, 0,
				"testAssoc");

		RoutingContext rc = parmFactory.createRoutingContext(new long[] { 100 });

		// As as = rsgw.createAppServer("testas", rc, rKey, trModType);
		As as = this.clientM3UAMgmt.createAs("testas", Functionality.AS, ExchangeType.SE, null, rc, null, null);
		FSM asPeerFSM = as.getPeerFSM();
		
		AspFactory localAspFactory = clientM3UAMgmt.createAspFactory("testasp", "testAssoc");
		localAspFactory.start();

		Asp asp = clientM3UAMgmt.assignAspToAs("testas", "testasp");
		// Create Route
		this.clientM3UAMgmt.addRoute(2, -1, -1, "testas");
		
		FSM aspLocalFSM = asp.getLocalFSM();
		
		// Check for Communication UP
		testAssociation.signalCommUp();

		// Once comunication is UP, ASP_UP should have been sent.
		assertEquals(AspState.UP_SENT, this.getAspState(aspLocalFSM));
		assertTrue(validateMessage(testAssociation, MessageClass.ASP_STATE_MAINTENANCE, MessageType.ASP_UP, -1, -1));

		// The other side will send ASP_UP_ACK and after that NTFY(AS-INACTIVE)
		M3UAMessageImpl message = messageFactory.createMessage(MessageClass.ASP_STATE_MAINTENANCE,
				MessageType.ASP_UP_ACK);
		localAspFactory.read(message);

		Notify notify = (Notify) messageFactory.createMessage(MessageClass.MANAGEMENT, MessageType.NOTIFY);
		notify.setRoutingContext(rc);
		Status status = parmFactory.createStatus(Status.STATUS_AS_State_Change, Status.INFO_AS_INACTIVE);
		notify.setStatus(status);
		localAspFactory.read(notify);

		assertEquals(AspState.ACTIVE_SENT, this.getAspState(aspLocalFSM));
		assertTrue(validateMessage(testAssociation, MessageClass.ASP_TRAFFIC_MAINTENANCE, MessageType.ASP_ACTIVE, -1,
				-1));
		// also the AS should be INACTIVE now
		assertEquals(AsState.INACTIVE, this.getAsState(asPeerFSM));

		// The other side will send ASP_ACTIVE_ACK and after that
		// NTFY(AS-ACTIVE)
		ASPActiveAck aspActiveAck = (ASPActiveAck) messageFactory.createMessage(MessageClass.ASP_TRAFFIC_MAINTENANCE,
				MessageType.ASP_ACTIVE_ACK);
		aspActiveAck.setRoutingContext(rc);
		localAspFactory.read(aspActiveAck);

		notify = (Notify) messageFactory.createMessage(MessageClass.MANAGEMENT, MessageType.NOTIFY);
		notify.setRoutingContext(rc);
		status = parmFactory.createStatus(Status.STATUS_AS_State_Change, Status.INFO_AS_ACTIVE);
		notify.setStatus(status);
		localAspFactory.read(notify);

		assertEquals(AspState.ACTIVE, this.getAspState(aspLocalFSM));
		// also the AS should be ACTIVE now
		assertEquals(AsState.ACTIVE, this.getAsState(asPeerFSM));
		
		// Check if MTP3 RESUME received
		//lets wait for 2second to receive the MTP3 primitive before giving up 
		semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);
		
		Mtp3Primitive mtp3Primitive = this.mtp3UserPartListener.rxMtp3PrimitivePoll();
		assertNotNull(mtp3Primitive);
		assertEquals(Mtp3Primitive.RESUME, mtp3Primitive.getType());
		assertEquals(2, mtp3Primitive.getAffectedDpc());
		//No more MTP3 Primitive or message
		assertNull(this.mtp3UserPartListener.rxMtp3PrimitivePoll());
		assertNull(this.mtp3UserPartListener.rxMtp3TransferPrimitivePoll());

		// Lets stop ASP Factory
		localAspFactory.stop();

		assertEquals(AspState.DOWN_SENT, this.getAspState(aspLocalFSM));
		assertTrue(validateMessage(testAssociation, MessageClass.ASP_STATE_MAINTENANCE, MessageType.ASP_DOWN, -1, -1));
		// also the AS is PENDING
		assertEquals(AsState.PENDING, this.getAsState(asPeerFSM));

		// The far end sends DOWN_ACK
		message = messageFactory.createMessage(MessageClass.ASP_STATE_MAINTENANCE, MessageType.ASP_DOWN_ACK);
		localAspFactory.read(message);

		// start the ASP Factory again
		localAspFactory.start();

		// Now lets add some PayloadData
		PayloadDataImpl payload = (PayloadDataImpl) messageFactory.createMessage(MessageClass.TRANSFER_MESSAGES,
				MessageType.PAYLOAD);
		ProtocolDataImpl p1 = (ProtocolDataImpl) parmFactory.createProtocolData(1408, 123, 3, 1, 0, 1, new byte[] { 1,
				2, 3, 4 });
		payload.setRoutingContext(rc);
		payload.setData(p1);

		as.write(payload);

		// Now again the ASP is brought up
		testAssociation.signalCommUp();

		// Once communication is UP, ASP_UP should have been sent.
		assertEquals(AspState.UP_SENT, this.getAspState(aspLocalFSM));
		assertTrue(validateMessage(testAssociation, MessageClass.ASP_STATE_MAINTENANCE, MessageType.ASP_UP, -1, -1));

		// The other side will send ASP_UP_ACK and after that NTFY(AS-INACTIVE)
		message = messageFactory.createMessage(MessageClass.ASP_STATE_MAINTENANCE, MessageType.ASP_UP_ACK);
		localAspFactory.read(message);

		notify = (Notify) messageFactory.createMessage(MessageClass.MANAGEMENT, MessageType.NOTIFY);
		notify.setRoutingContext(rc);
		status = parmFactory.createStatus(Status.STATUS_AS_State_Change, Status.INFO_AS_INACTIVE);
		notify.setStatus(status);
		localAspFactory.read(notify);

		assertEquals(AspState.ACTIVE_SENT, this.getAspState(aspLocalFSM));
		assertTrue(validateMessage(testAssociation, MessageClass.ASP_TRAFFIC_MAINTENANCE, MessageType.ASP_ACTIVE, -1,
				-1));
		// AS should still be PENDING
		assertEquals(AsState.PENDING, this.getAsState(asPeerFSM));

		// The other side will send ASP_ACTIVE_ACK and after that
		// NTFY(AS-ACTIVE)
		aspActiveAck = (ASPActiveAck) messageFactory.createMessage(MessageClass.ASP_TRAFFIC_MAINTENANCE,
				MessageType.ASP_ACTIVE_ACK);
		aspActiveAck.setRoutingContext(rc);
		localAspFactory.read(aspActiveAck);

		notify = (Notify) messageFactory.createMessage(MessageClass.MANAGEMENT, MessageType.NOTIFY);
		notify.setRoutingContext(rc);
		status = parmFactory.createStatus(Status.STATUS_AS_State_Change, Status.INFO_AS_ACTIVE);
		notify.setStatus(status);
		localAspFactory.read(notify);

		assertEquals(AspState.ACTIVE, this.getAspState(aspLocalFSM));
		// also the AS should be ACTIVE now
		assertEquals(AsState.ACTIVE, this.getAsState(asPeerFSM));

		// Also we should have PayloadData
		M3UAMessage payLoadTemp = testAssociation.txPoll();
		assertNotNull(payLoadTemp);
		assertEquals(MessageClass.TRANSFER_MESSAGES, payLoadTemp.getMessageClass());
		assertEquals(MessageType.PAYLOAD, payLoadTemp.getMessageType());
		
		//No more MTP3 Primitive or message
		assertNull(this.mtp3UserPartListener.rxMtp3PrimitivePoll());
		assertNull(this.mtp3UserPartListener.rxMtp3TransferPrimitivePoll());

		// Make sure we don't have any more
		assertNull(testAssociation.txPoll());

	}

	private boolean validateMessage(TestAssociation testAssociation, int msgClass, int msgType, int type, int info) {
		M3UAMessage message = testAssociation.txPoll();
		if (message == null) {
			return false;
		}

		if (message.getMessageClass() != msgClass || message.getMessageType() != msgType) {
			return false;
		}

		if (message.getMessageClass() == MessageClass.MANAGEMENT) {
			if (message.getMessageType() == MessageType.NOTIFY) {
				Status s = ((Notify) message).getStatus();
				if (s.getType() != type || s.getInfo() != info) {
					return false;
				} else {
					return true;
				}
			}

			// TODO take care of Error?
			return true;
		} else {
			return true;
		}

	}

	class TestAssociation implements Association {

		// Is the Association been started by management?
		private volatile boolean started = false;
		// Is the Association up (connection is established)
		protected volatile boolean up = false;
		
		private AssociationListener associationListener = null;
		private String name = null;
		private LinkedList<M3UAMessage> messageRxFromUserPart = new LinkedList<M3UAMessage>();

		TestAssociation(String name) {
			this.name = name;
		}

		M3UAMessage txPoll() {
			return messageRxFromUserPart.poll();
		}

		@Override
		public AssociationListener getAssociationListener() {
			return this.associationListener;
		}

		@Override
		public String getHostAddress() {
			return null;
		}

		@Override
		public int getHostPort() {
			return 0;
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public String getPeerAddress() {
			return null;
		}

		@Override
		public int getPeerPort() {
			return 0;
		}

		@Override
		public String getServerName() {
			return null;
		}

		@Override
		public boolean isStarted() {
			return this.started;
		}

		@Override
		public void send(PayloadData payloadData) throws Exception {
			M3UAMessage m3uaMessage = messageFactory.createSctpMessage(payloadData.getData());
			this.messageRxFromUserPart.add(m3uaMessage);
		}

		@Override
		public void setAssociationListener(AssociationListener associationListener) {
			this.associationListener = associationListener;
		}

		public void signalCommUp() {
			this.started = true;
			this.up = true;
			this.associationListener.onCommunicationUp(this,1,1);
		}

		public void signalCommLost() {
			this.up = false;
			this.associationListener.onCommunicationLost(this);
		}

		@Override
		public IpChannelType getIpChannelType() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public AssociationType getAssociationType() {
			// TODO Auto-generated method stub
			return null;
		}

		/* (non-Javadoc)
		 * @see org.mobicents.protocols.api.Association#getExtraHostAddresses()
		 */
		@Override
		public String[] getExtraHostAddresses() {
			// TODO Auto-generated method stub
			return null;
		}

		/* (non-Javadoc)
		 * @see org.mobicents.protocols.api.Association#isConnected()
		 */
		@Override
		public boolean isConnected() {
			return this.started && this.up;
		}

		@Override
		public void acceptAnonymousAssociation(AssociationListener arg0) throws Exception {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void rejectAnonymousAssociation() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void stopAnonymousAssociation() throws Exception {
			// TODO Auto-generated method stub
			
		}

	}

	class TransportManagement implements Management {

		private FastMap<String, Association> associations = new FastMap<String, Association>();

		@Override
		public Association addAssociation(String hostAddress, int hostPort, String peerAddress, int peerPort,
				String assocName) throws Exception {
			TestAssociation testAssociation = new TestAssociation(assocName);
			this.associations.put(assocName, testAssociation);
			return testAssociation;
		}

		@Override
		public Server addServer(String serverName, String hostAddress, int port) throws Exception {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Association addServerAssociation(String peerAddress, int peerPort, String serverName, String assocName)
				throws Exception {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Association getAssociation(String assocName) throws Exception {
			return this.associations.get(assocName);
		}

		@Override
		public Map<String, Association> getAssociations() {
			return associations.unmodifiable();
		}

		@Override
		public int getConnectDelay() {
			return 0;
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public List<Server> getServers() {
			return null;
		}

		@Override
		public int getWorkerThreads() {
			return 0;
		}

		@Override
		public boolean isSingleThread() {
			return false;
		}

		@Override
		public void removeAssociation(String assocName) throws Exception {

		}

		@Override
		public void removeServer(String serverName) throws Exception {

		}

		@Override
		public void setConnectDelay(int connectDelay) {

		}

		@Override
		public void setSingleThread(boolean arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void setWorkerThreads(int arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void start() throws Exception {
			// TODO Auto-generated method stub

		}

		@Override
		public void startAssociation(String arg0) throws Exception {
			// TODO Auto-generated method stub

		}

		@Override
		public void startServer(String arg0) throws Exception {
			// TODO Auto-generated method stub

		}

		@Override
		public void stop() throws Exception {
			// TODO Auto-generated method stub

		}

		@Override
		public void stopAssociation(String arg0) throws Exception {
			// TODO Auto-generated method stub

		}

		@Override
		public void stopServer(String arg0) throws Exception {
			// TODO Auto-generated method stub

		}

		@Override
		public String getPersistDir() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setPersistDir(String arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Association addAssociation(String arg0, int arg1, String arg2, int arg3, String arg4, IpChannelType arg5, String[] extraHostAddresses)
				throws Exception {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Server addServer(String arg0, String arg1, int arg2, IpChannelType arg3, String[] extraHostAddresses) throws Exception {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Association addServerAssociation(String arg0, int arg1, String arg2, String arg3, IpChannelType arg4)
				throws Exception {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void removeAllResourses() throws Exception {
			
		}

		@Override
		public void addManagementEventListener(ManagementEventListener arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Server addServer(String arg0, String arg1, int arg2, IpChannelType arg3, boolean arg4, int arg5, String[] arg6) throws Exception {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ServerListener getServerListener() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void removeManagementEventListener(ManagementEventListener arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setServerListener(ServerListener arg0) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see org.mobicents.protocols.api.Management#isStarted()
		 */
		@Override
		public boolean isStarted() {
			// TODO Auto-generated method stub
			return false;
		}
	}
	
	class Mtp3UserPartListenerimpl implements Mtp3UserPartListener {
		private LinkedList<Mtp3Primitive> mtp3Primitives = new LinkedList<Mtp3Primitive>();
		private LinkedList<Mtp3TransferPrimitive> mtp3TransferPrimitives = new LinkedList<Mtp3TransferPrimitive>();

		Mtp3Primitive rxMtp3PrimitivePoll() {
			return this.mtp3Primitives.poll();
		}

		Mtp3TransferPrimitive rxMtp3TransferPrimitivePoll() {
			return this.mtp3TransferPrimitives.poll();
		}

		@Override
		public void onMtp3PauseMessage(Mtp3PausePrimitive pause) {
			this.mtp3Primitives.add(pause);
			semaphore.release();
		}

		@Override
		public void onMtp3ResumeMessage(Mtp3ResumePrimitive resume) {
			this.mtp3Primitives.add(resume);
			semaphore.release();
		}

		@Override
		public void onMtp3StatusMessage(Mtp3StatusPrimitive status) {
			this.mtp3Primitives.add(status);
			semaphore.release();
		}

		@Override
		public void onMtp3TransferMessage(Mtp3TransferPrimitive transfer) {
			this.mtp3TransferPrimitives.add(transfer);
			semaphore.release();
		}

	}
}
