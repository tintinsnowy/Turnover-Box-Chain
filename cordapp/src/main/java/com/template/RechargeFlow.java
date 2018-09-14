package com.template;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.*;
import net.corda.core.flows.*;
import net.corda.core.identity.*;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.RPCOps;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;


import javax.annotation.Nullable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static net.corda.core.contracts.ContractsDSL.requireThat;
import static net.corda.finance.Currencies.EUR;

public class RechargeFlow {


// the partners submit the RechargeFlow
    public static class Initiator extends FlowLogic<Void> {
        private long amount;
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

        private final ProgressTracker.Step FINALISING = new ProgressTracker.Step("Finalising the tx...") {
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

        public Initiator(long amount, AbstractParty theParty) {
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

            // Step 1 Initialisation：We create the transaction components.
            progressTracker.setCurrentStep(INITIALISING);

            CashState outputState = new CashState(getOurIdentity(), new Amount<>(amount, EUR));
            StateAndContract outputContractAndState = new StateAndContract(outputState, RechargeContract.Recharge_Contract_ID);

            CordaX500Name x500Name = CordaX500Name.parse("O=Operator,L=Cologne,C=DE");
            // or using getPeerByLegalName
            //CordaRPCOps rpcOps = null;
            //Party receiver = rpcOps.wellKnownPartyFromX500Name(x500Name);
            Party receiver = getServiceHub().getIdentityService().wellKnownPartyFromX500Name(x500Name);
            List<PublicKey> requiredSigners = ImmutableList.of(getOurIdentity().getOwningKey(), receiver.getOwningKey());

            //Step 2 Building: we add the items to the builder.
            progressTracker.setCurrentStep(BUILDING);
            Command cmd = new Command<>(new RechargeContract.Add.Issue(), requiredSigners);
            txBuilder.withItems(outputContractAndState, cmd);

            //Step3 Verifying the transaction.
            txBuilder.verify(getServiceHub());
            //txBuilder.toWireTransaction().toLedgerTransaction(getServiceHub()).verify();
            progressTracker.setCurrentStep(VERIFY);

            //Step 4 signing the contract
            progressTracker.setCurrentStep(SIGNING);
            final SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Step 5 creating a session with the counterparty
            FlowSession otherpartySession = initiateFlow(receiver);;
            otherpartySession.send("hello the transaction num is:xxx");

            // Step 6: Obtaining the counterparty's signature.
            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(
                    signedTx, ImmutableList.of(otherpartySession), CollectSignaturesFlow.tracker()));

            // Finalising the transaction.
            subFlow(new FinalityFlow(fullySignedTx));


            return null;
        }
    }


    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<Void> {
        private FlowSession counterpartySession;

        public Responder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }
        /**
         * Define the acceptor's flow logic here.
         */
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
                        require.using("This must be an Recharge transaction.", output instanceof CashState);
                        CashState iou = (CashState) output;
                        require.using("The Recharge value can't be under 0.", iou.getValue() > 0);
                        Scanner scanner = new Scanner( System.in );
                        System.out.print( "If you have received the transfer, pls enter: yes; otherwise no" );
                        String input = scanner.nextLine();
                        require.using("The Transaction is denied by the Operator",  input.equals("yes"));
                        return null;
                    });
                }
            }

            subFlow(new SignTxFlow(counterpartySession, SignTransactionFlow.Companion.tracker()));

            return null;
        }//end of the void call()
    }

}