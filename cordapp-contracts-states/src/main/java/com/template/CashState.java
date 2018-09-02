package com.template;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.*;
import net.corda.core.crypto.NullKeys;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.finance.contracts.CommercialPaper;

import java.util.Currency;
import java.util.List;


public class CashState implements OwnableState {

    private AbstractParty owner;
    private Amount<Currency> faceValue;
    //private Instant maturityDate;

  /*  public CashState() {
    }  // For serialization
*/
    public CashState(AbstractParty owner, Amount<Currency> faceValue//, Instant maturityDate) {
    ){
        this.owner = owner;
        this.faceValue = faceValue;
        //this.maturityDate = maturityDate;
    }

    public CashState copy() {
        return new CashState(this.owner, this.faceValue);
    }

    public CashState withoutOwner() {
        return new CashState(new AnonymousParty(NullKeys.NullPublicKey.INSTANCE), this.faceValue);
    }

    @Override
    public CommandAndState withNewOwner(AbstractParty newOwner) {
        return new CommandAndState(new CommercialPaper.Commands.Move(), new CashState(newOwner, this.faceValue));
    }
/*
    public PartyAndReference getIssuance() {
        return issuance;
    }*/

    public AbstractParty getOwner() {
        return owner;
    }

    public Amount<Currency> getFaceValue() {
        return faceValue;
    }
//
//    public Instant getMaturityDate() {
//        return maturityDate;
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CashState state = (CashState) o;

        if (owner != null ? !owner.equals(state.owner) : state.owner != null) return false;
        return !(faceValue != null ? !faceValue.equals(state.faceValue) : state.faceValue != null);
        //!(maturityDate != null ? !maturityDate.equals(state.maturityDate) : state.maturityDate != null);
    }

    @Override
    public int hashCode() {
        //int result = issuance != null ? issuance.hashCode() : 0;
        int result =  owner != null ? owner.hashCode() : 0;
        //result = 31 * result + (owner != null ? owner.hashCode() : 0);
        //result = 31 * result + (faceValue >0 ? Double.toString(faceValue).hashCode() : 0);
        //result = 31 * result + (maturityDate != null ? maturityDate.hashCode() : 0);
        return result;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        // an ImmutableList is a List whose contents are immutable so can’t be modified.
        return ImmutableList.of(this.owner);
    }
}