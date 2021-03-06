/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012.
 * and individual contributors
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

package org.mobicents.protocols.ss7.oam.common.statistics.api;

import java.io.Serializable;


/**
* This value is supplied by CounterMediator and consumed by CounterProvider.
* This is a value of counters results for one counter and one object.
*
* @author sergey vetyutnev
*
*/
public interface SourceValueObject extends Serializable {

    /**
     * Name of an object.
     * @return
     */
    String getObjectName();

    /**
     * Long value
     * @return
     */
    long getValue();

    /**
     * First double value
     * @return
     */
    double getValueA();

    /**
     * Second double value
     * @return
     */
    double getValueB();

    /**
     * A set of complex values (a set of pairs "String" - "Long", for example a count of different ACN's)
     * @return
     */
    ComplexValue[] getComplexValue();

}
