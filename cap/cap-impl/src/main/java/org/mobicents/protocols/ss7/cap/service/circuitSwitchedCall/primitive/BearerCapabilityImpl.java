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

package org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.primitive;

import java.io.IOException;

import org.mobicents.protocols.asn.AsnException;
import org.mobicents.protocols.asn.AsnInputStream;
import org.mobicents.protocols.asn.AsnOutputStream;
import org.mobicents.protocols.asn.Tag;
import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.CAPParsingComponentException;
import org.mobicents.protocols.ss7.cap.api.CAPParsingComponentExceptionReason;
import org.mobicents.protocols.ss7.cap.api.isup.BearerCap;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.primitive.BearerCapability;
import org.mobicents.protocols.ss7.cap.isup.BearerCapImpl;
import org.mobicents.protocols.ss7.cap.primitives.CAPAsnPrimitive;
import org.mobicents.protocols.ss7.map.api.MAPParsingComponentException;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class BearerCapabilityImpl implements BearerCapability, CAPAsnPrimitive {

	public static final int _ID_bearerCap = 0;

	public static final String _PrimitiveName = "BearerCap";

	private BearerCap bearerCap;

	
	@Override
	public BearerCap getBearerCap() {
		return bearerCap;
	}

	
	@Override
	public int getTag() throws CAPException {
		return _ID_bearerCap;
	}

	@Override
	public int getTagClass() {
		return Tag.CLASS_CONTEXT_SPECIFIC;
	}

	@Override
	public boolean getIsPrimitive() {
		return true;
	}

	@Override
	public void decodeAll(AsnInputStream ansIS) throws CAPParsingComponentException {

		try {
			int length = ansIS.readLength();
			this._decode(ansIS, length);
		} catch (IOException e) {
			throw new CAPParsingComponentException("IOException when decoding " + _PrimitiveName + ": " + e.getMessage(), e,
					CAPParsingComponentExceptionReason.MistypedParameter);
		} catch (AsnException e) {
			throw new CAPParsingComponentException("AsnException when decoding " + _PrimitiveName + ": " + e.getMessage(), e,
					CAPParsingComponentExceptionReason.MistypedParameter);
		} catch (MAPParsingComponentException e) {
			throw new CAPParsingComponentException("MAPParsingComponentException when decoding " + _PrimitiveName + ": " + e.getMessage(), e,
					CAPParsingComponentExceptionReason.MistypedParameter);
		}
	}

	@Override
	public void decodeData(AsnInputStream ansIS, int length) throws CAPParsingComponentException {

		try {
			this._decode(ansIS, length);
		} catch (IOException e) {
			throw new CAPParsingComponentException("IOException when decoding " + _PrimitiveName + ": " + e.getMessage(), e,
					CAPParsingComponentExceptionReason.MistypedParameter);
		} catch (AsnException e) {
			throw new CAPParsingComponentException("AsnException when decoding " + _PrimitiveName + ": " + e.getMessage(), e,
					CAPParsingComponentExceptionReason.MistypedParameter);
		} catch (MAPParsingComponentException e) {
			throw new CAPParsingComponentException("MAPParsingComponentException when decoding " + _PrimitiveName + ": " + e.getMessage(), e,
					CAPParsingComponentExceptionReason.MistypedParameter);
		}
	}

	private void _decode(AsnInputStream ais, int length) throws CAPParsingComponentException, MAPParsingComponentException, IOException, AsnException {

		this.bearerCap = null;

		int tag = ais.getTag();

		if (ais.getTagClass() == Tag.CLASS_CONTEXT_SPECIFIC) {
			switch (tag) {
			case _ID_bearerCap:
				byte[] buf = ais.readOctetStringData(length);
				if (buf.length < 2 || buf.length > 11)
					throw new CAPParsingComponentException(
							"Error while decoding " + _PrimitiveName + ": bearerCap must be from 2 to 11 bytes length, found: " + buf.length,
							CAPParsingComponentExceptionReason.MistypedParameter);
				this.bearerCap = new BearerCapImpl(buf);
				break;

			default:
				throw new CAPParsingComponentException("Error while decoding " + _PrimitiveName + ": bad choice tag",
						CAPParsingComponentExceptionReason.MistypedParameter);
			}
		} else {
			throw new CAPParsingComponentException("Error while decoding " + _PrimitiveName + ": bad choice tagClass",
					CAPParsingComponentExceptionReason.MistypedParameter);
		}
	}

	@Override
	public void encodeAll(AsnOutputStream asnOs) throws CAPException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void encodeAll(AsnOutputStream asnOs, int tagClass, int tag) throws CAPException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void encodeData(AsnOutputStream asnOs) throws CAPException {
		// TODO Auto-generated method stub
		
	}
}