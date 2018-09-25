package com.template;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.confidential.IdentitySyncFlow;
import net.corda.core.contracts.*;
import net.corda.core.flows.*;

import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import net.corda.core.utilities.UntrustworthyData;
import net.corda.finance.contracts.asset.Cash;

import java.security.PublicKey;
import java.util.Currency;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import static com.template.RefuelFeeContract.RF_CONTRACT_ID;
import static net.corda.core.contracts.ContractsDSL.requireThat;
import static net.corda.finance.contracts.GetBalances.getCashBalance;

public class RefuelFeeFlow {

    /**
     * The RefuelFeeFlow should setup by the Operator
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

        private final Step AWAITING_PROPOSAL = new Step("Setup PROPOSAL from Supplier.");
        private final Step RECEIVING = new Step("The Supplier(Box borrower) RECEIVED!");
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
                AWAITING_PROPOSAL,RECEIVING, BUILDING, SIGNING, COLLECTING, FINALISING
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

            // step 1: we get the counterparty identity
            CordaX500Name x500Name = CordaX500Name.parse("O=Supplier,L=Dusserdorf,C=DE");
            Party receiver = getServiceHub().getIdentityService().wellKnownPartyFromX500Name(x500Name);

            // Step2: We retrieve the Box from vault and check whether it is enough
            progressTracker.setCurrentStep(AWAITING_PROPOSAL);
            final int boxNum  = BoxManager.getBoxBalance(productType, getServiceHub());
            if(boxNum< num){
                throw new FlowException(String.format(
                        "The boxes of type %s are not enough," +
                                "only %d left", productType,boxNum
                ));
            }


            // Step 3: If the Supplier can afford then send the proposal
            List<StateAndRef<Box>> boxesToSettle = BoxManager.getBoxesByType(productType, getServiceHub());
            FlowSession otherPartySession = initiateFlow(receiver);
            subFlow(new SendStateAndRefFlow(otherPartySession, boxesToSettle));
            Helper.LenderInfo hello = new Helper.LenderInfo(amount, getOurIdentity());
            otherPartySession.send(hello);

            /// step 4:  Verify the signing
            progressTracker.setCurrentStep(SIGNING);
            /*
              Sync identities to ensure we know all of the identities involved in the transaction we're about to
              be asked to sign
             */
            subFlow(new IdentitySyncFlow.Receive(otherPartySession));
            //StateAndContract outputContractAndState = new StateAndContract(outputState, RF_CONTRACT_ID);
            // Step 7. Creating a session with the counterparty.
            progressTracker.setCurrentStep(COLLECTING);
            FlowSession otherpartySession = initiateFlow(receiver);



            class  SignTxFlow extends SignTransactionFlow{
                private SignTxFlow(FlowSession otherPartySession, ProgressTracker progressTracker) {
                    super(otherPartySession, progressTracker);
                }
                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("This must be an Recharge transaction.", output instanceof Cash.State);

                        return null;
                    });
                }
            }

            // Obtaining the counterparty's signature.
//            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(
//                    signedTx, ImmutableList.of(otherpartySession), CollectSignaturesFlow.tracker()));

            // Finalising the transaction.
            //subFlow(new FinalityFlow(fullySignedTx));
          subFlow(new SignTxFlow(otherPartySession, SignTransactionFlow.Companion.tracker()));
            return null;
        }
    }// end of the initiator

    @InitiatedBy(Initiator.class)
/**
 The flow is annotated with InitiatedBy(IOUFlow.class),
 which means that your node will invoke IOUFlowResponder.call
 when it receives a message from a instance of Initiator running on another node
 */
    public static class RefuelFeeFlowResponder extends FlowLogic<SignedTransaction> {
        private final FlowSession otherPartySession;
        public RefuelFeeFlowResponder(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }
        private final Step RECEIVING = new Step("The Supplier(Box borrower) RECEIVED!");
        private final Step SIGNING = new Step("The Supplier(Box borrower) SIGNING!");
        private final Step BUILDING = new Step("The Supplier(Box borrower) BUILDING!");
        private final Step COLLECTING_SIGNATURES = new Step("The Supplier send to Operator the signed TX!");
        private final Step RECORDING = new Step("The Supplier(Box borrower) Recording!");
        private final ProgressTracker progressTracker = new ProgressTracker(
                RECEIVING , BUILDING, SIGNING, COLLECTING_SIGNATURES,RECORDING //FINALISING
        );
        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }
        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            //=========================================================================
            // STEP1> Wait for a trade request to come in from the other party.
            progressTracker.setCurrentStep(RECEIVING);
            //List<StateAndRef<Box>> boxesToSettle =  new ReceiveStateAndRefFlow(otherPartySession).call();
            List<StateAndRef<Box>> boxesToSettle =  subFlow(new ReceiveStateAndRefFlow<>(otherPartySession));
            UntrustworthyData<Helper.LenderInfo> tempInfo = otherPartySession.receive(Helper.LenderInfo.class);
            Helper.LenderInfo OperatorInfo = tempInfo.unwrap(data -> data);

            // STEP2> CONFIRMING
            System.out.println("the size of Boxes: "+ boxesToSettle.get(0).getState().getData().getProductType()+
            " is " + boxesToSettle.size());
            System.out.println("\n If it is what you want or not, pls Enter(Y/N) \n");
            Scanner scanner = new Scanner( System.in );
            String input =  scanner.nextLine();
            if(input.equalsIgnoreCase("Y"))
                throw new NotFoundException("Supplier denies the proposal "+ input);

             //STEP 3> to check whether the hassupplier has enough money
           // final Amount<Currency> cashBalance = getCashBalance(getServiceHub(), OperatorInfo.amount.getToken());
            final Amount<Currency> cashBalance = getCashBalance(getServiceHub(), OperatorInfo.amount.getToken());
            System.out.println("\n " + cashBalance.toString());
//            if (cashBalance.getQuantity() < OperatorInfo.amount.getQuantity()) {
//                throw new FlowException(String.format(
//                        "Proposer has only %s but needs %s to settle.", cashBalance, OperatorInfo.amount));
//            }

            // Stage 4. Create a transaction builder. Add the settle command and input Boxes to be transfered.
            progressTracker.setCurrentStep(BUILDING);
            List<PublicKey> requiredSigners = ImmutableList.of(getOurIdentity().getOwningKey(), otherPartySession.getCounterparty().getOwningKey());
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            final TransactionBuilder txBuilder = new TransactionBuilder();
            txBuilder.setNotary(notary);

            Iterator<StateAndRef<Box>> it;
            it = boxesToSettle.iterator();
            final Command cmdSettle = new Command<>(new RefuelFeeContract.Commands.Settle(), requiredSigners);
            while(it.hasNext()) {
                StateAndRef<Box> boxToSettle = it.next();
                txBuilder.addInputState(boxToSettle).addCommand(cmdSettle);
                CommandAndState boxTransfered = boxToSettle.getState().getData().withNewOwner(getOurIdentity());
                txBuilder.addOutputState(boxTransfered.getOwnableState(), RF_CONTRACT_ID);
            }

            RefuelFeeState outputState = new RefuelFeeState(getOurIdentity(),getOurIdentity(),
                    boxesToSettle.get(0).getState().getData().getProductType(), boxesToSettle.size());
            txBuilder.withItems(new StateAndContract(outputState, RF_CONTRACT_ID), cmdSettle);


            progressTracker.setCurrentStep(SIGNING);
            // Stage 5. Get some cash from the vault and add a spend to our transaction builder.
            //kotlin.Pair<txBuilder,cashSigningPubKeys>
            kotlin.Pair<TransactionBuilder,List<PublicKey>> temp= Cash.generateSpend(
                    getServiceHub(),
                    txBuilder,
                    OperatorInfo.amount,
                    getOurIdentity(),
                    ImmutableSet.of());
            TransactionBuilder tx = temp.component1();
            List<PublicKey> cashSigningPubKeys = temp.component2();
            SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(tx, cashSigningPubKeys);
            //currentTime = ServiceHub.clock.instant();
            //tx.setTimeWindow(currentTime, 30.seconds)

            // STEP 6: Sync up confidential identities in the transaction with our counterparty
            subFlow(new IdentitySyncFlow.Send(otherPartySession, tx.toWireTransaction(getServiceHub())));
            // Send the signed transaction to the Operator, who must then sign it themselves and commit
            // it to the ledger by sending it to the notary.
            progressTracker.setCurrentStep(COLLECTING_SIGNATURES);
            SignedTransaction operatorSignature = subFlow(new CollectSignaturesFlow(partSignedTx, ImmutableList.of(otherPartySession)));
            //SignedTransaction twiceSignedTx = operatorSignature. (partSignedTx);

            // Notarise and record the transaction.
            progressTracker.setCurrentStep(RECORDING);
            subFlow(new FinalityFlow(operatorSignature));
//            @Suspendable
//            private assembleSharedTX (StateAndRef<Box> assetForSale, tradeRequest: SellerTradeInfo, buyerAnonymousIdentity: PartyAndCertificate): SharedTx {
//                val ptx = TransactionBuilder(notary)

            return null;
        }
    }
}