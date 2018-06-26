package com.template;

import com.esotericsoftware.kryo.NotNull;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.*;
import net.corda.core.crypto.NullKeys;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.finance.contracts.CommercialPaper;

import java.time.Instant;
import java.util.Currency;
import java.util.List;


public class BoxState implements OwnableState {

    private PartyAndReference issuance;
    private AbstractParty owner;
    private Amount<Issued<Currency>> faceValue;
    //private Instant maturityDate;
    private String productType;
    private double price;

    public BoxState() {
    }  // For serialization

    public BoxState(PartyAndReference issuance, AbstractParty owner, Amount<Issued<Currency>> faceValue, //, Instant maturityDate) {
                    String productType, double price ){
        this.issuance = issuance;
        this.owner = owner;
        this.faceValue = faceValue;
        //this.maturityDate = maturityDate;
        this.productType = productType;
        this.price = price;
    }

    public BoxState copy() {
        return new BoxState(this.issuance, this.owner, this.faceValue, this.productType,this.price);
    }

    public BoxState withoutOwner() {
        return new BoxState(this.issuance, new AnonymousParty(NullKeys.NullPublicKey.INSTANCE), this.faceValue,
                this.productType, this.price);
    }

    @Override
    public CommandAndState withNewOwner(AbstractParty newOwner) {
        return new CommandAndState(new CommercialPaper.Commands.Move(),
                new BoxState(this.issuance, newOwner, this.faceValue,this.productType, this.price));
    }

    public PartyAndReference getIssuance() {
        return issuance;
    }

    public AbstractParty getOwner() {
        return owner;
    }

    public Amount<Issued<Currency>> getFaceValue() {
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

        BoxState state = (BoxState) o;

        if (issuance != null ? !issuance.equals(state.issuance) : state.issuance != null) return false;
        if (owner != null ? !owner.equals(state.owner) : state.owner != null) return false;
        return !(faceValue != null ? !faceValue.equals(state.faceValue) : state.faceValue != null);
        //!(maturityDate != null ? !maturityDate.equals(state.maturityDate) : state.maturityDate != null);
    }

    @Override
    public int hashCode() {
        int result = issuance != null ? issuance.hashCode() : 0;
        result = 31 * result + (owner != null ? owner.hashCode() : 0);
        result = 31 * result + (faceValue != null ? faceValue.hashCode() : 0);
        //result = 31 * result + (maturityDate != null ? maturityDate.hashCode() : 0);
        return result;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        // an ImmutableList is a List whose contents are immutable so can’t be modified.
        return ImmutableList.of(this.owner);
    }
}