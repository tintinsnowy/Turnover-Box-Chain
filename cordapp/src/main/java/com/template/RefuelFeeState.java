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
    private final Party productSupplier;
    private final Party boxOperator;
    private final String productType;
    private final Integer num;


    public RefuelFeeState( Party productSupplier, Party boxOperator,String productType, Integer num) {
        this.productSupplier = productSupplier;
        this.boxOperator = boxOperator;
        this.productType = productType;
        this.num = num;
    }
    public int getnNum() {
        return num;
    }

    public Party getOperator() {
        return boxOperator;
    }

    public Party getSupplier() {
        return productSupplier;
    }

    public String getProductType() {return  productType; }

    /** The public keys of the involved parties. */
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(productSupplier, boxOperator);
    }
}
