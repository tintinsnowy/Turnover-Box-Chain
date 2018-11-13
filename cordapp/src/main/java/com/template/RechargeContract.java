package com.template;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TypeOnlyCommandData;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.core.utilities.ProgressTracker;
import net.corda.finance.contracts.asset.Cash;

import java.security.PublicKey;
import java.util.HashSet;
import java.util.Set;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;


public class RechargeContract implements Contract {

    public static final String Recharge_Contract_ID = "com.template.RechargeContract";

    // Our Create command.
    public interface Commands extends CommandData {
        class Issue extends TypeOnlyCommandData implements Commands { }
        class Transfer extends TypeOnlyCommandData implements Commands {}
        class Settle extends  TypeOnlyCommandData implements Commands {}
    }

    // set up some tracker
    private final ProgressTracker.Step STEP1 = new ProgressTracker.Step("step1");
    private final ProgressTracker.Step STEP2 = new ProgressTracker.Step("step2");
    private final ProgressTracker.Step STEP3 = new ProgressTracker.Step("step3");
    private final ProgressTracker pt = new ProgressTracker(
            STEP1,STEP2,STEP3
    );

    @Override
    public void verify(LedgerTransaction tx) {
        //throw new UnsupportedOperationException();
        final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
        final RechargeContract.Commands commandData  = command.getValue();
        final Set<PublicKey> setOfSigners = new HashSet<>(command.getSigners());

        if (commandData instanceof RechargeContract.Commands.Issue) {
            verifyIssue(tx, setOfSigners);
        } else if (commandData instanceof RechargeContract.Commands.Transfer) {
            verifyTransfer(tx, setOfSigners);
        } else if (commandData instanceof RechargeContract.Commands.Settle) {
            verifySettle(tx, setOfSigners);
        } else {
            throw new IllegalArgumentException("Unrecognised command.");
        }
    }

    private void verifyTransfer(LedgerTransaction tx, Set<PublicKey> signers) {
        final Cash.State out = tx.inputsOfType(Cash.State.class).get(0);
        System.out.println("\n we are confirming Cash from "+ out.getOwner());

    }

    private void verifySettle(LedgerTransaction tx, Set<PublicKey> signers) {


    }

    private void verifyIssue(LedgerTransaction tx, Set<PublicKey> signers) {

        requireThat(check -> {
            // Constraints on the shape of the transaction.
            pt.setCurrentStep(STEP1);
            check.using("Cannot reissue a.transcation", tx.getInputs().isEmpty());
            pt.setCurrentStep(STEP2);
            check.using("There should be one output state of type RechargeContract.", tx.getOutputs().size() == 1);
            // should Constrains that the contractor should be the box operators
            final Cash.State out = tx.outputsOfType(Cash.State.class).get(0);
            final AbstractParty owner = out.getOwner();
            String name = owner.nameOrNull().getOrganisation();
            check.using("The Owner shouldn't be operator!  ", !name.equals("Operator"));
            check.using("The deposited amount should be greater than 0", out.getAmount().getQuantity()>0);
            pt.setCurrentStep(STEP3);
            check.using("output states are issued by a command signer", signers.contains(out.getOwner().getOwningKey()));
            return null;
        });
    }


}