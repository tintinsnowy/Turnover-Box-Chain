package com.template;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;
import java.util.List;

import static com.template.AddBoxContract.AddBox_Contract_ID;

/**
 * Define your flow here.
 */
//Replace TemplateFlow's definition with:
@InitiatingFlow
@StartableByRPC
public class AddBoxFlow extends FlowLogic<Void> {

    private AbstractParty owner;
    private double faceValue;
    private String productType;
    private double price;
    private Integer num;

    /**
     * The progress tracker provides checkpoints indicating the progress of the flow to observers.
     */
    private final ProgressTracker progressTracker = new ProgressTracker();

    public AddBoxFlow(AnonymousParty owner, String productType, double price, Integer num) {
        this.owner = owner;
        this.faceValue = price * num;
        //this.maturityDate = maturityDate;
        this.productType = productType;
        this.price = price;
        this.num = num;
    }


    @Override
    public Void call() throws FlowException {
        // We retrieve the notary identity from the network map.
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // We create a transaction builder.
        final TransactionBuilder txBuilder = new TransactionBuilder();
        txBuilder.setNotary(notary);

        for (int i =0; i<num; i++){

        // We create the transaction components.
        BoxState outputState = new BoxState(getOurIdentity(), productType, price);
        StateAndContract outputContractAndState = new StateAndContract(outputState, AddBoxContract.AddBox_Contract_ID);
        List<PublicKey> requiredSigners = ImmutableList.of(getOurIdentity().getOwningKey(), owner.getOwningKey());
        Command cmd = new Command<>(new AddBoxContract.Create(), requiredSigners);

        // We add the items to the builder.
        txBuilder.withItems(outputContractAndState, cmd);

        // Verifying the transaction.
        txBuilder.verify(getServiceHub());

        // Signing the transaction.
        final SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);


        // Finalising the transaction.
            //subFlow(new FinalityFlow(fullySignedTx));
        }

        return null;
    }
}
