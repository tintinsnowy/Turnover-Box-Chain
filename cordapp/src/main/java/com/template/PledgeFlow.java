package com.template;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.confidential.IdentitySyncFlow;
import net.corda.core.contracts.*;
import net.corda.core.flows.*;

import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import net.corda.core.utilities.UntrustworthyData;
import net.corda.finance.contracts.asset.Cash;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static com.template.AddBoxContract.AddBox_Contract_ID;
import static net.corda.core.contracts.ContractsDSL.requireThat;
import static net.corda.finance.contracts.GetBalances.getCashBalance;

public class PledgeFlow {

    /**
     * The RefuelFeeFlow should setup by the Operator
     */

    @InitiatingFlow
    @StartableByRPC
    public static class PledgeInitiator extends FlowLogic<Void> {

        private final Amount<Currency> amount;
        private final long numDemand;  // the number of ProductType x should be traded
        private final String productType;
        private final Party counterParty;

        /**
         * The progress tracker provides checkpoints indicating the progress of the flow to observers.
         */
        public PledgeInitiator(Amount<Currency> amount, long numDemand, String productType, Party counterParty) {
            this.amount = amount;
            this.numDemand = numDemand;
            this.productType = productType;
            this.counterParty = counterParty;
        }

        private final Step AWAITING_PROPOSAL = new Step("====== Setup PROPOSAL from box owners======");
        private final Step RECEIVING = new Step("======The Supplier(Box borrower) RECEIVED!======");
        private final Step BUILDING = new Step("======Building and verifying transaction.======");
        private final Step SIGNING = new Step("=======Signing transaction.=======");
        private final Step COLLECTING = new Step("=======Collecting counterparty signature.=======") {
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final Step FINALISING = new Step("=======Finalising transaction.=======") {
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

            // Step2: We retrieve the Boxes from vault and check whether they are enough
            progressTracker.setCurrentStep(AWAITING_PROPOSAL);
            final long numInStock  = BoxManager.getBoxBalance(productType, getServiceHub());
            if(numInStock< numDemand){
                throw new FlowException(String.format(
                        "The boxes of type %s are not enough," +
                                "only %d left", productType,numInStock
                ));
            }
            // Step 3: send the MSG and Boxes to Supplier to settle the Transaction
            List<StateAndRef<Box>> boxesToSettle = BoxManager.getBoxesByType(productType, getServiceHub());
            FlowSession otherPartySession = initiateFlow(counterParty);
            subFlow(new SendStateAndRefFlow(otherPartySession, boxesToSettle));
            Helper.LenderInfo hello = new Helper.LenderInfo(amount, getOurIdentity(), numDemand, numInStock);
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
            class  SignTxFlow extends SignTransactionFlow{
                private SignTxFlow(FlowSession otherPartySession, ProgressTracker progressTracker) {
                    super(otherPartySession, progressTracker);
                }
                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        System.out.println(output);
                        //require.using("This must be an Recharge transaction.", output instanceof Cash.State);
                        return null;
                    });
                }
            }

            // Obtaining the counterparty's signature.
//            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(
//                    signedTx, ImmutableList.of(otherpartySession), CollectSignaturesFlow.tracker()));

            // Finalising the transaction.
            subFlow(new SignTxFlow(otherPartySession, SignTransactionFlow.Companion.tracker()));
            return null;
        }
    }// end of the initiator

    @InitiatedBy(PledgeInitiator.class)
    /**
 The flow is annotated with InitiatedBy(IOUFlow.class),
 which means that your node will invoke IOUFlowResponder.call
 when it receives a message from a instance of Initiator running on another node
 */
    public static class PledgeFlowResponder extends FlowLogic<SignedTransaction> {
        private final FlowSession otherPartySession;
        public PledgeFlowResponder(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }
        private final Step RECEIVING = new Step("The Box borrower RECEIVED!");
        private final Step SIGNING = new Step("The Box borrower SIGNING!");
        private final Step BUILDING = new Step("The Box borrower BUILDING!");
        private final Step COLLECTING_SIGNATURES = new Step("The proposer send to receiver the signed TX!");
        private final Step RECORDING = new Step("The Box borrower Recording!");
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
            Instant currentTime = getServiceHub().getClock().instant();

            // STEP1> Wait for a trade request to come in from the other party.
            progressTracker.setCurrentStep(RECEIVING);

            List<StateAndRef<Box>> boxesToSettle =  subFlow(new ReceiveStateAndRefFlow<>(otherPartySession));
            UntrustworthyData<Helper.LenderInfo> tempInfo = otherPartySession.receive(Helper.LenderInfo.class);
            Helper.LenderInfo OperatorInfo = tempInfo.unwrap(data -> data);

            // STEP2> CONFIRMING
            System.out.println(" the size of Boxes: "+ boxesToSettle.get(0).getState().getData().getProductType()+
                    " is " + boxesToSettle.size());

            //STEP 3> to check whether the hassupplier has enough money
            // final Amount<Currency> cashBalance = getCashBalance(getServiceHub(), OperatorInfo.amount.getToken());
            final Amount<Currency> cashBalance = getCashBalance(getServiceHub(), OperatorInfo.amount.getToken());
            System.out.println("There is " + cashBalance.toString());
            if (cashBalance.getQuantity() < OperatorInfo.amount.getQuantity()) {
                throw new FlowException(String.format(
                        "Proposer has only %s but needs %s to settle.", cashBalance, OperatorInfo.amount));
            }

            // Stage 4. Create a transaction builder. Add the settle command and input Boxes to be transfered.
            progressTracker.setCurrentStep(BUILDING);
            List<PublicKey> requiredSigners = ImmutableList.of(getOurIdentity().getOwningKey(), otherPartySession.getCounterparty().getOwningKey());
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            final TransactionBuilder txBuilder = new TransactionBuilder();
            txBuilder.setNotary(notary);

            Iterator<StateAndRef<Box>> it;
            it = boxesToSettle.iterator();
            final Command cmdSettle = new Command<>(new AddBoxContract.Commands.Transfer(), requiredSigners);
            while(it.hasNext() ) {
                StateAndRef<Box> boxToSettle = it.next();
                txBuilder.addInputState(boxToSettle);
            }
            String productType = boxesToSettle.get(0).getState().getData().getProductType();
            Box supplierState = new Box(getOurIdentity(),productType ,OperatorInfo.numDemand);
            long rest = OperatorInfo.numInStock-OperatorInfo.numDemand;
            if (rest!=0){
                Box opState = new Box(otherPartySession.getCounterparty(),productType ,rest);
                txBuilder.addOutputState(opState,AddBox_Contract_ID);
            }
            txBuilder.addOutputState(supplierState, AddBox_Contract_ID)
                    .addCommand(cmdSettle).addCommand(new Command<>(new DepositContract.Commands.Transfer(),requiredSigners));

            // Stage 5. Get some cash from the vault and add a spend to our transaction builder.
            PublicKey okey =  otherPartySession.getCounterparty().getOwningKey();
            AbstractParty to = getServiceHub().getIdentityService().partyFromKey(okey);
            final  List<PublicKey> cashSigningPubKeys =Cash.generateSpend(
                    getServiceHub(),
                    txBuilder,
                    OperatorInfo.amount,
                    getOurIdentityAndCert(),
                    to,
                    Collections.emptySet()).getSecond();

            SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder, cashSigningPubKeys);
            txBuilder.verify(getServiceHub());

            //tx.setTimeWindow(currentTime, 30.seconds)
            progressTracker.setCurrentStep(SIGNING);
            // STEP 6: Sync up confidential identities in the transaction with our counterparty
            subFlow(new IdentitySyncFlow.Send(otherPartySession, txBuilder.toWireTransaction(getServiceHub())));
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
            Instant endTime = getServiceHub().getClock().instant();
            Duration between = Duration.between(currentTime, endTime);
            String contentToAppend = "==========The process for PledgeFlow cost "+between+"=============\n";
            System.out.println(contentToAppend);

            String fileName = "D:\\ubuntu\\Turnover-Box-Chain\\Pledge.txt";
            try {
                Files.write(
                        Paths.get(fileName),
                        (between.toString()+System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
