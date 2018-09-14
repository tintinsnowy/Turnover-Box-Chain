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
import net.corda.finance.contracts.asset.CommodityContract;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;


public class RechargeContract implements Contract {

    public static final String Recharge_Contract_ID = "com.template.RechargeContract";

    // Our Create command.
    public static class Add implements CommandData {

        //        public static class Issue implements CommodityContract.Commands {
//            @Override
//            public boolean equals(Object obj) {
//                return obj instanceof Issue;
//            }
//        }
        public static class Issue extends TypeOnlyCommandData implements CommodityContract.Commands {}
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
        final CommandWithParties<Add.Issue> cmd = requireSingleCommand(tx.getCommands(), Add.Issue.class);
        //start AddBoxFlow productType: normaltyple, price: 10, num: 2
        requireThat(check -> {

            // Constraints on the shape of the transaction.
            pt.setCurrentStep(STEP1);
            check.using("Cannot reissue a.transcation", tx.getInputs().isEmpty());
            pt.setCurrentStep(STEP2);
            check.using("There should be one output state of type RechargeContract.", tx.getOutputs().size() == 1);
            // should Constrains that the contractor should be the box operators
            final CashState out = tx.outputsOfType(CashState.class).get(0);
            final AbstractParty owner = out.getOwner();
            String name = owner.nameOrNull().getOrganisation();
            check.using("The Owner is operator!  ", !name.equals("Operator"));
            pt.setCurrentStep(STEP3);
            check.using("output states are issued by a command signer", cmd.getSigners().contains(out.getOwner().getOwningKey()));
            return null;
        });
    }

}