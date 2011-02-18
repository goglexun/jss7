package org.mobicents.protocols.ss7.m3ua.impl.sg;

import org.mobicents.protocols.ss7.m3ua.M3UAProvider;
import org.mobicents.protocols.ss7.m3ua.impl.Asp;
import org.mobicents.protocols.ss7.m3ua.impl.AspFactory;
import org.mobicents.protocols.ss7.m3ua.impl.AspState;
import org.mobicents.protocols.ss7.m3ua.impl.TransitionState;

public class RemAspImpl extends Asp {

    public RemAspImpl(String name, M3UAProvider m3UAProvider, AspFactory aspFactory) {
        super(name, m3UAProvider, aspFactory);

        // Define states
        fsm.createState(AspState.DOWN.toString());
        fsm.createState(AspState.ACTIVE.toString());
        fsm.createState(AspState.INACTIVE.toString());

        fsm.setStart(AspState.DOWN.toString());
        fsm.setEnd(AspState.DOWN.toString());

        // Define Transitions

        // ******************************************************************/
        // STATE DOWN /
        // ******************************************************************/
        fsm.createTransition(TransitionState.COMM_UP, AspState.DOWN.toString(), AspState.DOWN.toString());
        fsm.createTransition(TransitionState.COMM_DOWN, AspState.DOWN.toString(), AspState.DOWN.toString());
        fsm.createTransition(TransitionState.ASP_UP, AspState.DOWN.toString(), AspState.INACTIVE.toString());
        // .setHandler(new RemAspTransDwnToInact(this, fsm));

        // If the SGP receives any other M3UA messages before an ASP Up message
        // is received (other than ASP Down; see Section 4.3.4.2), the SGP MAY
        // discard them.
        fsm.createTransition(TransitionState.DAUD, AspState.DOWN.toString(), AspState.DOWN.toString());
        fsm.createTransition(TransitionState.ASP_ACTIVE, AspState.DOWN.toString(), AspState.DOWN.toString());
        fsm.createTransition(TransitionState.ASP_INACTIVE, AspState.DOWN.toString(), AspState.DOWN.toString());
        fsm.createTransition(TransitionState.PAYLOAD, AspState.DOWN.toString(), AspState.DOWN.toString());

        fsm.createTransition(TransitionState.ASP_DOWN, AspState.DOWN.toString(), AspState.DOWN.toString());
        // TODO : For REG_REQ/DREG_REQ we don't support dynamic adding of key.
        // Handle this

        // ******************************************************************/
        // STATE INACTIVE /
        // ******************************************************************/
        fsm.createTransition(TransitionState.COMM_DOWN, AspState.INACTIVE.toString(), AspState.DOWN.toString());
        // Mo transition needed? .setHandler(new RemAspTransInactToDwn(this,
        // this.fsm));

        fsm.createTransition(TransitionState.ASP_UP, AspState.INACTIVE.toString(), AspState.INACTIVE.toString());

        fsm.createTransition(TransitionState.ASP_DOWN, AspState.INACTIVE.toString(), AspState.DOWN.toString());

        fsm.createTransition(TransitionState.ASP_ACTIVE, AspState.INACTIVE.toString(), AspState.ACTIVE.toString());

        // TODO: Ignore payload if Remote ASP is still INACTIVE. may be log it?
        fsm.createTransition(TransitionState.PAYLOAD, AspState.INACTIVE.toString(), AspState.INACTIVE.toString());

        // ******************************************************************/
        // STATE ACTIVE /
        // ******************************************************************/
        fsm.createTransition(TransitionState.COMM_DOWN, AspState.ACTIVE.toString(), AspState.DOWN.toString());

        fsm.createTransition(TransitionState.ASP_UP, AspState.ACTIVE.toString(), AspState.INACTIVE.toString());

        fsm.createTransition(TransitionState.ASP_DOWN, AspState.ACTIVE.toString(), AspState.DOWN.toString());

        fsm.createTransition(TransitionState.ASP_INACTIVE, AspState.ACTIVE.toString(), AspState.INACTIVE.toString());
        // No transition handler .setHandler(new RemAspTransActToInact(this,
        // this.fsm));

        fsm.createTransition(TransitionState.PAYLOAD, AspState.ACTIVE.toString(), AspState.ACTIVE.toString());

        // This transition will be signaled from SGW
        fsm.createTransition(TransitionState.OTHER_ALTERNATE_ASP_ACTIVE, AspState.ACTIVE.toString(), AspState.INACTIVE
                .toString());

    }

}
