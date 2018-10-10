package com.template;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TypeOnlyCommandData;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.core.utilities.ProgressTracker;
import net.corda.finance.contracts.asset.CommodityContract;
import net.corda.finance.contracts.asset.Obligation;

import java.security.PublicKey;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;


public class AddBoxContract implements Contract {

    public static final String AddBox_Contract_ID = "com.template.AddBoxContract";

    // Our Create command.
    public interface Commands extends CommandData {
        class Issue extends TypeOnlyCommandData implements Commands { }
        class Transfer extends TypeOnlyCommandData implements Commands {}
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
        final Commands commandData  = command.getValue();
        final Set<PublicKey> setOfSigners = new HashSet<>(command.getSigners());

            if (commandData instanceof Commands.Issue) {
                verifyIssue(tx, setOfSigners);
            } else if (commandData instanceof Commands.Transfer) {
                verifyTransfer(tx, setOfSigners);
            } else {
                throw new IllegalArgumentException("Unrecognised command.");
            }
    }


    // This only allows one Box issuance per transaction.
    private void verifyIssue(LedgerTransaction tx, Set<PublicKey> signers){
        requireThat(check -> {
            // Constraints on the shape of the transaction.
            pt.setCurrentStep(STEP1);
            check.using("Cannot reissue a.", tx.getInputs().isEmpty());
            pt.setCurrentStep(STEP2);
            check.using("There should be one output state of type AddBoxContract.", tx.getOutputs().size() == 1);
            // should Constrains that the contractor should be the box operators
            final Box out = tx.outputsOfType(Box.class).get(0);
            final AbstractParty owner = out.getOwner();
            String name = owner.nameOrNull().getOrganisation();
            check.using("The Owner isn't operator!.but "+owner.nameOrNull().getOrganisation(),
                    name.equals("Operator"));
            pt.setCurrentStep(STEP3);
            check.using("output states are issued by a command signer", signers.contains(out.getOwner().getOwningKey()));
            return null;
        });
    }

    private void verifyTransfer(LedgerTransaction tx, Set<PublicKey> signers) {
        List<Box>  inputs = tx.inputsOfType(Box.class);
        List<Box> outputs = tx.outputsOfType(Box.class);
        requireThat(check ->{

            return null;
        });
    }


}