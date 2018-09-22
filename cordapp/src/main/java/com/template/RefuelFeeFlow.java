package com.template;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.*;
import net.corda.core.flows.*;

import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import net.corda.finance.contracts.asset.Cash;


import java.awt.*;
import java.security.PublicKey;
import java.util.Currency;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import static com.template.RefuelFeeContract.RF_CONTRACT_ID;
import static net.corda.core.contracts.ContractsDSL.requireThat;
import static net.corda.finance.Currencies.EUR;
import static net.corda.finance.contracts.GetBalances.getCashBalance;

public class RefuelFeeFlow {

    /**
     * The RefuelFeeFlow should setup by the Supplier
     */
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<Void> {

        private final Amount<Currency> amount;
        private final Integer num;  // the number of ProductType x should be traded
        private final String productType;
        /**
         * The progress tracker provides checkpoints indicating the progress of the flow to observers.
         */
        public Initiator(Amount<Currency> amount,Integer num, String productType) {
            this.amount = amount;
            this.num = num;
            this.productType = productType;
        }

        private final Step PREPARATION = new Step("Obtaining Obligation from vault.");
        private final Step BUILDING = new Step("Building and verifying transaction.");
        private final Step SIGNING = new Step("Signing transaction.");
        private final Step COLLECTING = new Step("Collecting counterparty signature.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final Step FINALISING = new Step("Finalising transaction.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        private final ProgressTracker progressTracker = new ProgressTracker(
                PREPARATION, BUILDING, SIGNING, COLLECTING, FINALISING
        );

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

            // Step1: We retrieve the Boxes(obligation) / Cash from vault and check
            progressTracker.setCurrentStep(PREPARATION);
            final int boxNum  = BoxManager.getBoxBalance(productType, getServiceHub());
            final Amount<Currency> cashBalance = getCashBalance(getServiceHub(), amount.getToken());

            if(boxNum< num){
                throw new FlowException(String.format(
                        "The boxes of type %s are not enough," +
                                "only %d left", productType,boxNum
                ));
            }
            if (cashBalance.getQuantity() < amount.getQuantity()) {
                throw new FlowException(String.format(
                        "Proposer has only %s but needs %s to settle.", cashBalance, amount));
            }
            // step 2: we get the counterparty identity
            CordaX500Name x500Name = CordaX500Name.parse("O=Operator,L=Cologne,C=DE");
            Party receiver = getServiceHub().getIdentityService().wellKnownPartyFromX500Name(x500Name);

            /// step 3:  Create a settle command.
            //StateAndContract outputContractAndState = new StateAndContract(outputState, RF_CONTRACT_ID);
            List<PublicKey> requiredSigners = ImmutableList.of(getOurIdentity().getOwningKey(), receiver.getOwningKey());
            final Command cmdSettle = new Command<>(new RefuelFeeContract.Commands.Settle(), requiredSigners);

            // Stage 4. Create a transaction builder. Add the settle command and input Boxes to be transfered.
            progressTracker.setCurrentStep(BUILDING);
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            final TransactionBuilder txBuilder = new TransactionBuilder();
            txBuilder.setNotary(notary);
            List<StateAndRef<Box>> boxesToSettle = BoxManager.getBoxesByType(productType, getServiceHub());

            Iterator<StateAndRef<Box>> it;
            it = boxesToSettle.iterator();
            while(it.hasNext()) {
                StateAndRef<Box> boxToSettle = it.next();
                txBuilder.addInputState(boxToSettle).addCommand(cmdSettle);
                CommandAndState boxTransfered = boxToSettle.getState().getData().withNewOwner(getOurIdentity());
                txBuilder.addOutputState(boxTransfered.getOwnableState(), RF_CONTRACT_ID);
            }

            RefuelFeeState outputState = new RefuelFeeState(getOurIdentity(),receiver,productType, num);
            txBuilder.withItems(new StateAndContract(outputState, RF_CONTRACT_ID), cmdSettle);


            // Stage 5. Get some cash from the vault and add a spend to our transaction builder.
            final List<PublicKey> cashSigningKeys = Cash.generateSpend(
                    getServiceHub(),
                    txBuilder,
                    amount,
                    receiver,
                    ImmutableSet.of()).getSecond();

            // Step 6. Verifying and signing the transaction.
            progressTracker.setCurrentStep(SIGNING);
            txBuilder.verify(getServiceHub());
            final List<PublicKey> signingKeys = new ImmutableList.Builder<PublicKey>()
                    .addAll(cashSigningKeys)
                    .add(receiver.getOwningKey())
                    .build();
            final SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder,signingKeys);

            // Step 7. Creating a session with the counterparty.
            progressTracker.setCurrentStep(COLLECTING);
            FlowSession otherpartySession = initiateFlow(receiver);
            // Obtaining the counterparty's signature.
            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(
                    signedTx, ImmutableList.of(otherpartySession), CollectSignaturesFlow.tracker()));

            // Finalising the transaction.
            subFlow(new FinalityFlow(fullySignedTx));

            return null;
        }
    }// end of the initiator

    @InitiatedBy(Initiator.class)
/**
 The flow is annotated with InitiatedBy(IOUFlow.class),
 which means that your node will invoke IOUFlowResponder.call
 when it receives a message from a instance of Initiator running on another node
 */
    public static class RefuelFeeFlowResponder extends FlowLogic<Void> {
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
                    Scanner scanner = new Scanner( System.in );
                    requireThat(require -> {
//                        ContractState output = stx.getTx().getOutputs().get(0).getData();
//                        require.using("This must be an Refuelfee transaction.", output instanceof RefuelFeeState);
//                        RefuelFeeState iou = (RefuelFeeState) output;
//                        System.out.println( "If you have received the transfer, pls enter: yes; otherwise no:\n" );
//
//                        String input =  scanner.nextLine();
//                        require.using("The Transaction is denied by the Operator: "+input,  input.equals("yes"));
                        return null;
                    });
                }
            }

            subFlow(new SignTxFlow(otherPartySession, SignTransactionFlow.Companion.tracker()));

            return null;
        }
    }
}