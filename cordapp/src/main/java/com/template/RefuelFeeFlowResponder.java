package com.template;

// Add these imports:
import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.ContractState;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;

import static net.corda.core.contracts.ContractsDSL.requireThat;

@InitiatedBy(RefuelFeeFlow.class)
/**
 The flow is annotated with InitiatedBy(IOUFlow.class),
 which means that your node will invoke IOUFlowResponder.call
 when it receives a message from a instance of Initiator running on another node
 */
public class RefuelFeeFlowResponder extends FlowLogic<Void> {
    private final FlowSession otherPartySession;

    public RefuelFeeFlowResponder(FlowSession otherPartySession) {
        this.otherPartySession = otherPartySession;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        class SignTxFlow extends SignTransactionFlow {
            private SignTxFlow(FlowSession otherPartySession, ProgressTracker progressTracker) {
                super(otherPartySession, progressTracker);
            }

            @Override
            protected void checkTransaction(SignedTransaction stx) {
                requireThat(require -> {
                    ContractState output = stx.getTx().getOutputs().get(0).getData();
                    require.using("This must be an Refuelfee transaction.", output instanceof RefuelFeeState);
                    RefuelFeeState iou = (RefuelFeeState) output;
                    require.using("The IOU's value can't be too high.", iou.getValue() < 100);
                    return null;
                });
            }
        }

        subFlow(new SignTxFlow(otherPartySession, SignTransactionFlow.Companion.tracker()));

        return null;
    }
}