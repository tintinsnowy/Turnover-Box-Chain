package com.template;
 // the Contract between Operators and Suppliers
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.util.Collections;
import java.util.List;
/**
 * Define your state object here.
 */
public class RefuelFeeState implements ContractState {
    private final Integer value;
    private final Party productSupplier;
    private final Party boxOperator;

    public RefuelFeeState(int value, Party productSupplier, Party boxOperator) {
        this.value = value;
        this.productSupplier = productSupplier;
        this.boxOperator = boxOperator;
    }
    public int getValue() {
        return value;
    }

    public Party getOperator() {
        return boxOperator;
    }

    public Party getSupplier() {
        return productSupplier;
    }

    /** The public keys of the involved parties. */
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(productSupplier, boxOperator);
    }
}
