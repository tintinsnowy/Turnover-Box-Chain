package com.template;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class AddBoxContract implements Contract {

    public static final String AddBox_Contract_ID = "com.template.AddBoxContract";

    // Our Create command.
    public static class Create implements CommandData {
    }

    @Override
    public void verify(LedgerTransaction tx) {
        //throw new UnsupportedOperationException();
        final CommandWithParties<AddBoxContract.Create> command = requireSingleCommand(tx.getCommands(), AddBoxContract.Create.class);
        requireThat(check ->{
            // Constraints on the shape of the transaction.
            check.using("No inputs should be consumed when issuing an IOU.", tx.getInputs().isEmpty());
            check.using("There should be one output state of type AddBoxContract.", tx.getOutputs().size() == 1);

           // should Constrains that the contractor should be the box operators
            final BoxState out = tx.outputsOfType(BoxState.class).get(0);
            final AbstractParty owner = out.getOwner();
            check.using("The Owner isn't operator!.",  owner.nameOrNull().getCommonName() =="O=Operator,L=Cologne,C=DE"  );

            return null;
        });

    }

}