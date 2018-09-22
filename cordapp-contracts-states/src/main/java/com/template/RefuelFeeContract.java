package com.template;// Add these imports:

import com.template.RefuelFeeState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TypeOnlyCommandData;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.finance.contracts.asset.Cash;

import java.security.PublicKey;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

// Replace TemplateContract's definition with:
public class RefuelFeeContract implements Contract {
    public static final String RF_CONTRACT_ID = "com.template.RefuelFeeContract";

    // Our Create command.
    public interface Commands extends CommandData {

        class Settle extends TypeOnlyCommandData implements Commands {
        }
    }

    @Override
    public void verify(LedgerTransaction tx) {
        final CommandWithParties<RefuelFeeContract.Commands> cmd = requireSingleCommand(tx.getCommands(), RefuelFeeContract.Commands.class);

        requireThat(check -> {
            // Constraints on the shape of the transaction.
            check.using("No Inputs is not allowed", !tx.getInputs().isEmpty());
            final RefuelFeeState InfoHub = tx.outputsOfType(RefuelFeeState.class).get(0);
            final List<Box> input = tx.inputsOfType(Box.class);
            final List<Box> output = tx.outputsOfType(Box.class);

            check.using("the transaction is signed by the Owner of Box", cmd.getSigners().
                    contains(input.get(0).getOwner().getOwningKey()));
            check.using("the input and output state should be the same", input.size()== output.size());
            for (int i=0;i<input.size();i++){
                check.using("The Owner of Boxes used to be Operator", input.get(i).getOwner().nameOrNull().equals("Operator") );
                check.using("Now Boxes belongs to Supplier", output.get(i).getOwner().nameOrNull().equals("Supplier") );
            }

            return null;
        });
    }
}