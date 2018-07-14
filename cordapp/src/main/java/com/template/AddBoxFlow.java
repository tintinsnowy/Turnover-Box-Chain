package com.template;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Command;
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
import java.util.List;

//import com.template.AddBoxContract.*;
//import static net.corda.core.messaging.CordaRPCOps.*;

/**
 * Define your flow here.
 */
//Replace TemplateFlow's definition with:
@InitiatingFlow
@StartableByRPC
public class AddBoxFlow extends FlowLogic<Void> {

    private String productType;
    private double price;
    private Integer num;
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

    public AddBoxFlow(String productType, double price, Integer num) {
        this.productType = productType;
        this.price = price;
        this.num = num;
        //this.otherParty = otherParty;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        // We retrieve the notary identity from the network map.
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);


        for (int i =0; i<num; i++){
            // We create a transaction builder.
            final TransactionBuilder txBuilder = new TransactionBuilder();
            txBuilder.setNotary(notary);

            // Step 1 Initialisation：We create the transaction components.
            progressTracker.setCurrentStep(INITIALISING);
            BoxState outputState = new BoxState(getOurIdentity(), productType, price);
            StateAndContract outputContractAndState = new StateAndContract(outputState, AddBoxContract.AddBox_Contract_ID);
            List<PublicKey> requiredSigners =  new ArrayList<>();
            requiredSigners.add(getOurIdentity().getOwningKey());


            //Step 2 Building: we add the items to the builder.
            progressTracker.setCurrentStep(BUILDING);
            Command cmd = new Command<>(new AddBoxContract.Add.Issue(),requiredSigners);
            txBuilder.withItems(outputContractAndState,cmd);

            //Step3 Verifying the transaction.
            txBuilder.verify(getServiceHub());
            //txBuilder.toWireTransaction().toLedgerTransaction(getServiceHub()).verify();
            progressTracker.setCurrentStep(VERIFY);

            //Step 4 signing the contract
            progressTracker.setCurrentStep(SIGNING);
            final SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Finalising the transaction.
            subFlow(new FinalityFlow(signedTx));
        }

        return null;
    }
}
