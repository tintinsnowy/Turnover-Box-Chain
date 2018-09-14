package com.template;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.flows.*;

import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;
import java.util.List;

import static com.template.RefuelFeeContract.RF_CONTRACT_ID;
/**
 * Define your flow here.
 */
//Replace TemplateFlow's definition with:
@InitiatingFlow
@StartableByRPC
public class RefuelFeeFlow extends FlowLogic<Void> {

    private final Integer Value;
    private final Party otherParty;

    /**
     * The progress tracker provides checkpoints indicating the progress of the flow to observers.
     */
    private final ProgressTracker progressTracker = new ProgressTracker();

    public RefuelFeeFlow(Integer Value, Party otherParty) {
        this.Value = Value;
        this.otherParty = otherParty;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }
    /**
     * The flow logic is encapsulated within the call() method.
     */
    @Suspendable
    @Override
    public Void call() throws FlowException {
        // We create a transaction builder.
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        final TransactionBuilder txBuilder = new TransactionBuilder();
        txBuilder.setNotary(notary);

// We create the transaction components.
        RefuelFeeState outputState = new RefuelFeeState(Value, otherParty, getOurIdentity());
        StateAndContract outputContractAndState = new StateAndContract(outputState, RF_CONTRACT_ID);
        List<PublicKey> requiredSigners = ImmutableList.of(getOurIdentity().getOwningKey(), otherParty.getOwningKey());
        Command cmd = new Command<>(new RefuelFeeContract.Create(), requiredSigners);

// We add the items to the builder.
        txBuilder.withItems(outputContractAndState, cmd);

// Verifying the transaction.
        txBuilder.verify(getServiceHub());

// Signing the transaction.
        final SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

// Creating a session with the other party.
        FlowSession otherpartySession = initiateFlow(otherParty);;

// Obtaining the counterparty's signature.
        SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(
                signedTx, ImmutableList.of(otherpartySession), CollectSignaturesFlow.tracker()));

// Finalising the transaction.
        subFlow(new FinalityFlow(fullySignedTx));

        return null;
    }
}