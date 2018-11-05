package com.template;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.StateAndRef;
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

import static com.template.AddBoxContract.AddBox_Contract_ID;

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

    public AddBoxFlow(String productType,Integer num) {
        this.productType = productType;
        this.num = num;
        //this.otherParty = otherParty;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        // We retrieve the notary identity from the network map.
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

     // We create a transaction builder.
        final TransactionBuilder txBuilder = new TransactionBuilder();
        txBuilder.setNotary(notary);

        // Step 1 Initialisation：We create the transaction components.
        progressTracker.setCurrentStep(INITIALISING);
        Box outputState = new Box(getOurIdentity(), productType, num);
        //StateAndContract outputContractAndState = new StateAndContract(outputState, AddBox_Contract_ID);
        List<PublicKey> requiredSigners =  new ArrayList<>();
        requiredSigners.add(getOurIdentity().getOwningKey());

        //Step 2 Building: we add the items to the builder.
        progressTracker.setCurrentStep(BUILDING);
        //Command<AddBoxContract.cmdData.Issue> cmd = new Command<>(new AddBoxContract.cmdData.Issue(),requiredSigners);
        //txBuilder.withItems(outputContractAndState,cmd);
       txBuilder.addOutputState(outputState, AddBox_Contract_ID)
               .addCommand(new AddBoxContract.Commands.Issue(),requiredSigners);
        //Step3 Verifying the transaction.
        txBuilder.verify(getServiceHub());
        //txBuilder.toWireTransaction().toLedgerTransaction(getServiceHub()).verify();
        progressTracker.setCurrentStep(VERIFY);

        //Step 4 signing the contract
        progressTracker.setCurrentStep(SIGNING);
        final SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        // Finalising the transaction.
        subFlow(new FinalityFlow(signedTx));
        //test
        final  List<StateAndRef<Box>> boxNum  = BoxManager.getBoxesByType(productType, getServiceHub());
        System.out.printf("The there should be : %d this type of Boxes\n",boxNum.get(0).component1().getData().getNum());

        return null;
    }
}
