package com.template;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.Issued;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.flows.*;
import net.corda.core.identity.*;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;


import javax.annotation.Nullable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

public class RechargeFlow extends FlowLogic<Void>{
    private double amount;
    private AbstractParty theParty;// the Party which deposits.
    //private Party otherParty;
    /**
     * The progress tracker provides checkpoints indicating the progress of the flow to observers.
     */

    private final ProgressTracker.Step INITIALISING = new ProgressTracker.Step("Initialising the transaction...");

    private final ProgressTracker.Step BUILDING = new ProgressTracker.Step("Building the tx...");

    private final ProgressTracker.Step SIGNING = new ProgressTracker.Step("Signing the tx...");

    private final ProgressTracker.Step VERIFY = new ProgressTracker.Step("Verifing the tx...") {

        @Nullable
        @Override
        public ProgressTracker childProgressTracker() {
            return CollectSignaturesFlow.Companion.tracker();
        }
    };

    private final ProgressTracker.Step FINALISING = new ProgressTracker.Step("Finalising the tx..."){
        @Nullable
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.Companion.tracker();
        }
    };

    private final ProgressTracker progressTracker = new ProgressTracker(
            INITIALISING, BUILDING, SIGNING, VERIFY, FINALISING
    );

    @Nullable
    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    public RechargeFlow(double amount, AbstractParty theParty) {
        this.amount = amount;
        this.theParty = theParty;
        //this.otherParty = otherParty;
    }

    @Override
    public Void call() throws FlowException {
        // We create a transaction builder.
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        final TransactionBuilder txBuilder = new TransactionBuilder();
        txBuilder.setNotary(notary);


        return null;
    }
}
