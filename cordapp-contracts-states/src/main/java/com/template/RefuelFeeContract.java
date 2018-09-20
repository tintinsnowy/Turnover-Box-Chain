package com.template;// Add these imports:
import com.google.common.collect.ImmutableList;
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
        class Issue extends TypeOnlyCommandData implements Commands {
        }

        class Transfer extends TypeOnlyCommandData implements Commands {
        }

        class Settle extends TypeOnlyCommandData implements Commands {
        }
    }

    @Override
    public void verify(LedgerTransaction tx) {
        final CommandWithParties<RefuelFeeContract.Commands> command = requireSingleCommand(tx.getCommands(), RefuelFeeContract.Commands.class);

        requireThat(check -> {
            // Constraints on the shape of the transaction.
            check.using("No inputs should be consumed when issuing an IOU.", tx.getInputs().isEmpty());
            check.using("There should be one output state of type IOUState.", tx.getOutputs().size() == 1);

            // IOU-specific constraints.
            final CashState out = tx.outputsOfType(CashState.class).get(0);
            final AbstractParty payer = out.getOwner();
            check.using("The Refuel value must be non-negative.", out.getValue() > 0);

            //check.using("The operator shouldn't be the same as supplier", operator != supplier);
            //check.using("The Owner isn't Operator!.but "+operator.getName().getOrganisation(),
             //       operator.getName().getOrganisation().equals("Operator"));
            check.using("This isn't supplier!.but "+payer.nameOrNull().getOrganisation(),
                    payer.nameOrNull() .getOrganisation().equals("Supplier"));

            // Constraints on the signers.
//            final List<PublicKey> signers = command.getSigners();
//            check.using("There must be two signers.", signers.size() == 2);
//            check.using("The borrower and lender must be signers.", signers.containsAll(
//                    ImmutableList.of(supplier.getOwningKey(), operator.getOwningKey())));

            return null;
        });
    }
}